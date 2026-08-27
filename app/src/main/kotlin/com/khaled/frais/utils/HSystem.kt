package com.khaled.frais.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.getSystemService

object HSystem {
    fun isInteractive(context: Context): Boolean {
        val powerManger = context.getSystemService<PowerManager>()!!
        return powerManger.isInteractive
    }

    fun isCharging(context: Context): Boolean {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @Suppress("SameParameterValue")
    private fun checkOp(context: Context, op: String): Boolean {
        val opsManager = context.getSystemService<AppOpsManager>()!!
        val result = if (HTarget.Q) {
            opsManager.unsafeCheckOp(op, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            opsManager.checkOp(op, android.os.Process.myUid(), context.packageName)
        }
        return result == AppOpsManager.MODE_ALLOWED
    }

    fun checkOpUsageStats(context: Context): Boolean =
        checkOp(context, AppOpsManager.OPSTR_GET_USAGE_STATS)
}
