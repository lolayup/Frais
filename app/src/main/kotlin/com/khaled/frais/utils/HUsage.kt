package com.khaled.frais.utils

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.khaled.frais.FraisApp.Companion.app
import androidx.core.content.getSystemService

object HUsage {
    private val manager by lazy { app.getSystemService<UsageStatsManager>()!! }

    fun isPermissionGranted(): Boolean {
        val appOps = app.getSystemService<AppOpsManager>()!!
        val mode = if (HTarget.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStats(days: Int = 7): Map<String, UsageStats> {
        if (!isPermissionGranted()) return emptyMap()
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000L * 60 * 60 * 24 * days)
        return manager.queryAndAggregateUsageStats(startTime, endTime)
    }

    fun getLastUsedTime(packageName: String, stats: Map<String, UsageStats>): Long {
        return stats[packageName]?.lastTimeUsed ?: 0L
    }

    fun getTotalForegroundTime(packageName: String, stats: Map<String, UsageStats>): Long {
        return stats[packageName]?.totalTimeInForeground ?: 0L
    }
}
