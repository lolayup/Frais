package com.khaled.frais.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.khaled.frais.R
import com.khaled.frais.app.FraisData
import com.khaled.frais.app.AppManager
import com.khaled.frais.receivers.FreezeReceiver
import com.khaled.frais.utils.*
import java.util.concurrent.TimeUnit

class FreezeNotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (!FraisData.autoFreezeNotification) return Result.success()
        if (!HUsage.isPermissionGranted()) return Result.success()

        val stats = HUsage.getUsageStats(1) // Get last 24 hours stats
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesMillis = TimeUnit.MINUTES.toMillis(15)

        val apps = HPackages.getInstalledApplications()
        apps.forEach { appInfo ->
            val pkg = appInfo.packageName
            if (pkg == applicationContext.packageName) return@forEach

            // Check if protected
            val metadata = FraisData.getMetadata(pkg)
            val isProtected = metadata.pinned || metadata.whitelisted || 
                                ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) && !metadata.isSafeToFreeze)
            
            if (isProtected) return@forEach

            // Check if frozen
            if (HPackages.isAppDisabled(pkg)) return@forEach

            // Check usage
            val lastUsed = HUsage.getLastUsedTime(pkg, stats)
            if (lastUsed > 0 && (currentTime - lastUsed) > fifteenMinutesMillis) {
                // Only suggest for apps that have a launcher activity (user apps)
                val hasLauncher = HPackages.getLauncherActivities(pkg).isNotEmpty()
                if (hasLauncher) {
                    sendNotification(pkg, appInfo.loadLabel(applicationContext.packageManager).toString())
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(packageName: String, appName: String) {
        val channelId = "freeze_suggestions"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Freeze Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Suggests freezing apps that haven't been used for a while."
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val freezeIntent = Intent(applicationContext, FreezeReceiver::class.java).apply {
            putExtra("package_name", packageName)
            putExtra("notification_id", packageName.hashCode())
        }
        val freezePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            packageName.hashCode(),
            freezeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(com.khaled.frais.R.drawable.ic_round_frozen) // Assuming this exists based on QSTileService
            .setContentTitle("Optimize System")
            .setContentText("'$appName' hasn't been used for 15 minutes. Freeze it to save resources?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "FREEZE", freezePendingIntent)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notify(packageName.hashCode(), builder.build())
                }
            } else {
                notify(packageName.hashCode(), builder.build())
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<FreezeNotificationWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FreezeNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("FreezeNotificationWork")
        }
    }
}
