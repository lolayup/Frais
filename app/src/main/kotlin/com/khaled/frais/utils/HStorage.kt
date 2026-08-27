package com.khaled.frais.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.khaled.frais.FraisApp.Companion.app
import java.io.File
import java.util.UUID

object HStorage {
    fun getAppSize(packageName: String): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageStatsManager = app.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = app.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                
                // Use default storage UUID
                val uuid = StorageManager.UUID_DEFAULT
                val user = Process.myUserHandle()
                
                val stats = storageStatsManager.queryStatsForPackage(uuid, packageName, user)
                return stats.appBytes + stats.dataBytes + stats.cacheBytes
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fallback or old API
        try {
            val packageInfo = app.packageManager.getPackageInfo(packageName, 0)
            val info = packageInfo.applicationInfo
            if (info != null) {
                val file = File(info.sourceDir)
                return file.length()
            }
        } catch (e: Exception) {
            // Ignore
        }
        return 0L
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
