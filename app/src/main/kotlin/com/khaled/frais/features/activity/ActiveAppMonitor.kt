package com.khaled.frais.features.activity

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.khaled.frais.FraisApp
import com.khaled.frais.utils.HUsage

object ActiveAppMonitor {

    fun getActiveBackgroundPackages(context: Context): List<String> {
        if (!HUsage.isPermissionGranted()) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 15 // Last 15 minutes

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        val lastEventTime = mutableMapOf<String, Long>()
        val lastEventType = mutableMapOf<String, Int>()
        
        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            
            lastEventTime[pkg] = event.timeStamp
            lastEventType[pkg] = event.eventType

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackage = pkg
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND && pkg == foregroundPackage) {
                foregroundPackage = null
            }
        }

        // Active background packages:
        // 1. Had a recent event.
        // 2. Are not currently in foreground.
        // 3. Last event was either MOVE_TO_BACKGROUND or some activity happened after MOVE_TO_BACKGROUND?
        // Actually, if an app is playing music, it might not have frequent MOVE_TO_BACKGROUND events but it's active.
        
        // Simplified heuristic for "Active Background":
        // Apps that were in foreground recently (last 10 mins) and are not the current foreground app.
        
        val activePackages = lastEventTime.keys.filter { pkg ->
            pkg != foregroundPackage && 
            pkg != context.packageName &&
            (endTime - (lastEventTime[pkg] ?: 0L)) < 1000 * 60 * 5 // Last 5 mins activity
        }.sortedByDescending { lastEventTime[it] }

        return activePackages
    }
}
