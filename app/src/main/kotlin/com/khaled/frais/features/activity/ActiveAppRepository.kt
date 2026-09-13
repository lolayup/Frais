package com.khaled.frais.features.activity

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveAppRepository(private val context: Context) {

    suspend fun getActiveApps(allApps: List<AppInfo>): List<ActiveApp> = withContext(Dispatchers.IO) {
        val protected = FraisData.closeAllProtectedApps
        
        allApps.filter { appInfo ->
            // القواعد المطلوبة: غير محمي + غير مجمد
            appInfo.packageName !in protected &&
            !appInfo.isWhitelisted &&
            appInfo.state != AppInfo.State.FROZEN &&
            (appInfo.isLaunchable || FraisData.showNonLaunchableApps) &&
            (!appInfo.isSystemApp || appInfo.isSafeToFreeze)
        }.sortedByDescending { it.lastUsed }.mapNotNull { appInfo ->
            val pkg = appInfo.packageName
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
