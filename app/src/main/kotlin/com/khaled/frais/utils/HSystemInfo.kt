package com.khaled.frais.utils

import android.app.AlarmManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.core.content.getSystemService
import com.khaled.frais.FraisApp.Companion.app
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HSystemInfo {
    fun getNextAlarm(): String? {
        val alarmManager = app.getSystemService<AlarmManager>() ?: return null
        val nextAlarm = alarmManager.nextAlarmClock ?: return null
        val date = Date(nextAlarm.triggerTime)
        return SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(date)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getBluetoothDevices(): List<Pair<String, Int>> {
        val bluetoothManager = app.getSystemService<BluetoothManager>() ?: return emptyList()
        val adapter = bluetoothManager.adapter ?: return emptyList()
        
        if (!adapter.isEnabled) return emptyList()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (app.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
        }

        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        return connectedDevices.map { device ->
            val batteryLevel = getBatteryLevel(device)
            device.name to batteryLevel
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
}
