package com.khaled.frais.features.activity

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.khaled.frais.app.AppInfo
import com.khaled.frais.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveAppRepository(private val context: Context) {

    suspend fun getActiveApps(allApps: List<AppInfo>): List<ActiveApp> = withContext(Dispatchers.IO) {
        val activePackages = ActiveAppMonitor.getActiveBackgroundPackages(context)
        
        activePackages.mapNotNull { pkg ->
            val appInfo = allApps.find { it.packageName == pkg } ?: return@mapNotNull null
            
            val icon = appInfo.applicationInfo?.let { 
                AppIconCache.getOrLoadBitmap(context, it, 0, 100) 
            }
            val color = icon?.let { 
                Color(AppIconCache.getDominantColor(it, pkg)) 
            } ?: Color.DarkGray
            
            ActiveApp(
                appInfo = appInfo,
                lastUsedTime = appInfo.lastUsed,
                dominantColor = color
            )
        }
    }
}
