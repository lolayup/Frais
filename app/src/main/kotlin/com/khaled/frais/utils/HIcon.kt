package com.khaled.frais.utils

import android.content.ComponentName
import android.content.pm.PackageManager
import com.khaled.frais.FraisApp.Companion.app

object HIcon {
    private const val ALIAS_SLEEP = "com.khaled.frais.ui.main.MainActivitySleep"
    private const val ALIAS_AWAKE = "com.khaled.frais.ui.main.MainActivityAwake"
    private const val ALIAS_SECURED = "com.khaled.frais.ui.main.MainActivitySecured"

    enum class IconState { SLEEP, AWAKE, SECURED }

    private var pendingState: IconState? = null

    fun setPendingIconState(state: IconState) {
        pendingState = state
    }

    fun applyPendingIconState() {
        val state = pendingState ?: return
        pendingState = null
        
        val pm = app.packageManager
        val sleepComponent = ComponentName(app, ALIAS_SLEEP)
        val awakeComponent = ComponentName(app, ALIAS_AWAKE)
        val securedComponent = ComponentName(app, ALIAS_SECURED)

        val targetComponent = when (state) {
            IconState.SLEEP -> sleepComponent
            IconState.AWAKE -> awakeComponent
            IconState.SECURED -> securedComponent
        }

        // Only switch if the target is not already enabled
        if (pm.getComponentEnabledSetting(targetComponent) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return
        }

        // Disable all first
        pm.setComponentEnabledSetting(sleepComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(awakeComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(securedComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)

        // Enable target
        pm.setComponentEnabledSetting(targetComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        
        // Note: DONT_KILL_APP might not always work as expected for launcher icons,
        // but since we call this when the app is closing/backgrounded, it's safer.
    }
}
