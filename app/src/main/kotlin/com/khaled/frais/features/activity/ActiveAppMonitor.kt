package com.khaled.frais.features.activity

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.khaled.frais.utils.HUsage

object ActiveAppMonitor {

    private fun isInputMethod(context: Context, packageName: String): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.inputMethodList.any { it.packageName == packageName }
    }

    fun getActiveBackgroundPackages(context: Context): List<String> {
        // Try Shizuku first for high accuracy if available
        if (com.khaled.frais.app.AppManager.checkService()) {
            val shizukuActive = getActivePackagesViaShizuku()
            if (shizukuActive.isNotEmpty()) {
                val foreground = getForegroundPackageViaUsageStats(context)
                return shizukuActive.filter { pkg ->
                    (pkg != foreground) && 
                    (pkg != context.packageName) && 
                    !isInputMethod(context, pkg)
                }
            }
        }

        // Fallback to UsageStats heuristic
        return getActivePackagesViaUsageStats(context)
    }

    private fun getActivePackagesViaShizuku(): List<String> {
        return runCatching {
            val am = com.khaled.frais.utils.HShizuku.asInterface("android.app.IActivityManager", Context.ACTIVITY_SERVICE)

            val processes = am::class.java.getMethod("getRunningAppProcesses").invoke(am) as List<*>
            processes.mapNotNull {
                val info = it as android.app.ActivityManager.RunningAppProcessInfo
                // IMPORTANCE_PERCEPTIBLE (230) includes background music/services user can perceive
                if (info.importance <= 230) {
                    info.pkgList?.firstOrNull()
                } else null
            }.distinct()
        }.getOrElse { emptyList() }
    }

    private fun getForegroundPackageViaUsageStats(context: Context): String? {
        if (!HUsage.isPermissionGranted()) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 2
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var foreground: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foreground = event.packageName
            }
        }
        return foreground
    }

    private fun getActivePackagesViaUsageStats(context: Context): List<String> {
        if (!HUsage.isPermissionGranted()) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 10 // Last 10 minutes

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        val lastEventTime = mutableMapOf<String, Long>()
        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            lastEventTime[pkg] = event.timeStamp
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackage = pkg
            }
        }

        return lastEventTime.keys.filter { pkg ->
            pkg != foregroundPackage && 
            pkg != context.packageName &&
            !isInputMethod(context, pkg) &&
            (endTime - (lastEventTime[pkg] ?: 0L)) < 1000 * 60 * 5
        }.sortedByDescending { lastEventTime[it] }
    }
}
