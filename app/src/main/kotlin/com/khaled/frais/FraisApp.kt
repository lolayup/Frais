package com.khaled.frais

import android.app.Application
import android.app.UiModeManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.khaled.frais.app.FraisData
import com.khaled.frais.utils.HDhizuku
import com.khaled.frais.utils.HTarget
import com.khaled.frais.utils.HIcon

class FraisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        if (!HTarget.S) setAppTheme(FraisData.appTheme)
        if (FraisData.workingMode.startsWith(FraisData.DHIZUKU)) HDhizuku.init()
        
        if (FraisData.autoFreezeNotification) {
            com.khaled.frais.workers.FreezeNotificationWorker.schedule(this)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activeActivities = 0
            override fun onActivityStarted(activity: android.app.Activity) {
                activeActivities++
            }
            override fun onActivityStopped(activity: android.app.Activity) {
                activeActivities--
                if (activeActivities == 0) {
                    // App went to background / closed
                    HIcon.applyPendingIconState()
                }
            }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    fun setAppTheme(theme: String) {
        if (HTarget.S) getSystemService<UiModeManager>()!!.setApplicationNightMode(
            when (theme) {
                FraisData.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
                FraisData.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
        )
        else AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                FraisData.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                FraisData.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    companion object {
        lateinit var app: FraisApp private set
    }
}