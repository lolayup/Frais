package com.khaled.frais.utils

import android.app.AlarmManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import com.khaled.frais.FraisApp.Companion.app
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HSystemInfo {
    /**
     * Retrieves the next scheduled alarm in a human-readable format.
     * Combines AlarmManager.nextAlarmClock with a Settings fallback for robustness.
     */
    fun getNextAlarm(): String? {
        val alarmManager = app.getSystemService<AlarmManager>() ?: return null
        
        // 1. Check for official NextAlarmClock (standard Android way)
        val nextAlarm = alarmManager.nextAlarmClock
        
        // 2. Check Settings Provider as a fallback (some OEMs use this)
        @Suppress("DEPRECATION")
        val settingsAlarm = Settings.System.getString(app.contentResolver, Settings.System.NEXT_ALARM_FORMATTED)
        
        if (nextAlarm == null) {
            return if (!settingsAlarm.isNullOrBlank()) {
                // Settings might return "Mon 8:00 AM", we clean it up
                settingsAlarm.uppercase().split(" ").take(2).joinToString(" ")
            } else null
        }

        val triggerTime = nextAlarm.triggerTime
        val now = System.currentTimeMillis()
        val diff = triggerTime - now
        
        // If the alarm is in the past, it's stale
        if (diff < -5000) return null
        
        val date = Date(triggerTime)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()
        
        return when {
            diff < 60000 -> "RINGING"
            diff < 3600000 -> "${diff / 60000} MIN"
            diff < 43200000 -> timeFormat // Within 12 hours, just show time
            else -> "$dayFormat $timeFormat" // Further away, show day
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getBluetoothDevices(): List<Pair<String, Int>> {
        val bluetoothManager = app.getSystemService<BluetoothManager>() ?: return emptyList()
        val adapter = bluetoothManager.adapter ?: return emptyList()
        
        if (!adapter.isEnabled) return emptyList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (app.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
        }

        val connectedDevices = mutableListOf<BluetoothDevice>()
        runCatching { connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) }
        runCatching { connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP)) }
        runCatching { connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET)) }

        return connectedDevices.distinctBy { it.address }.map { device ->
            val batteryLevel = getBatteryLevel(device)
            (device.name ?: "DEVICE") to batteryLevel
        }
    }

    private fun getBatteryLevel(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            method.invoke(device) as Int
        } catch (e: Exception) {
            -1
        }
    }

    fun isMobileDataEnabled(): Boolean {
        return try {
            // Using Settings.Global is more robust and doesn't require READ_PHONE_STATE
            Settings.Global.getInt(app.contentResolver, "mobile_data", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = app.getSystemService<LocationManager>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager?.isLocationEnabled ?: false
            } else {
                @Suppress("DEPRECATION")
                val mode = Settings.Secure.getInt(app.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (e: Exception) {
            false
        }
    }
}
