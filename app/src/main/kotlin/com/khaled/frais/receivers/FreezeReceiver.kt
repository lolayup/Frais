package com.khaled.frais.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.khaled.frais.app.AppManager
import com.khaled.frais.utils.HUI
import androidx.core.app.NotificationManagerCompat

class FreezeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("package_name") ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (AppManager.setAppFrozen(packageName, true)) {
            HUI.showToast("FROZEN $packageName")
        } else {
            HUI.showToast("FAILED TO FREEZE $packageName")
        }

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
