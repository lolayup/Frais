package com.khaled.frais.app

import android.content.Context
import android.content.Intent
import com.khaled.frais.BuildConfig
import com.khaled.frais.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.content.FileProvider

object AppManager {
    val lockScreen: Boolean
        get() = when {
            FraisData.workingMode.startsWith(FraisData.DHIZUKU) -> HDhizuku.lockScreen
            FraisData.workingMode.startsWith(FraisData.SHIZUKU) -> HShizuku.lockScreen
            else -> false
        }

    fun checkService(): Boolean = when {
        FraisData.workingMode.startsWith(FraisData.DHIZUKU) -> 
            com.rosan.dhizuku.api.Dhizuku.init(com.khaled.frais.FraisApp.app) && com.rosan.dhizuku.api.Dhizuku.isPermissionGranted()
        FraisData.workingMode.startsWith(FraisData.SHIZUKU) -> 
            rikka.shizuku.Shizuku.pingBinder() && 
            rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        else -> false
    }

    fun requestShizukuPermission(requestCode: Int) {
        if (rikka.shizuku.Shizuku.pingBinder()) {
            rikka.shizuku.Shizuku.requestPermission(requestCode)
        } else {
            HLog.e(Exception("Shizuku binder not available for permission request"))
        }
    }

    fun isAppFrozen(packageName: String): Boolean = HPackages.isAppDisabled(packageName)

    fun setListFrozen(frozen: Boolean, vararg appInfo: AppInfo): String? {
        val excludeMe = appInfo.filter { 
            it.packageName != BuildConfig.APPLICATION_ID && 
            (!frozen || (!it.whitelisted && (!it.isSystemApp || it.isSafeToFreeze)))
        }
        var i = 0
        var denied = false
        var name = String()
        excludeMe.forEach {
            if (setAppFrozen(it.packageName, frozen)) {
                i++
                name = it.name.toString()
            } else if (it.applicationInfo != null) {
                denied = true
            }
        }
        return if (denied && i == 0) null else if (i == 1) name else i.toString()
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean {
        if (packageName == BuildConfig.APPLICATION_ID) return false
        
        // Safety check for single app freeze
        val appInfo = HPackages.getApplicationInfoOrNull(packageName)
        if (appInfo != null && frozen) {
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) {
                val metadata = FraisData.getMetadata(packageName)
                if (!metadata.isSafeToFreeze) {
                    HUI.showToast("System app protected: $packageName")
                    return false
                }
            }
        }

        return when {
            FraisData.workingMode.startsWith(FraisData.DHIZUKU) -> HDhizuku.setAppHidden(packageName, frozen)
            FraisData.workingMode.startsWith(FraisData.SHIZUKU) -> HShizuku.setAppDisabled(packageName, frozen)
            else -> false
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        when {
            FraisData.workingMode.startsWith(FraisData.DHIZUKU) ->
                if (HDhizuku.uninstallApp(packageName)) return true

            FraisData.workingMode.startsWith(FraisData.SHIZUKU) ->
                if (HShizuku.uninstallApp(packageName)) return true
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
        return false
    }

    fun reinstallApp(packageName: String): Boolean = when {
        FraisData.workingMode.startsWith(FraisData.SHIZUKU) -> HShizuku.reinstallApp(packageName)
        else -> false
    }

    suspend fun reinstallAppFallback(packageName: String, context: Context): Boolean = withContext(Dispatchers.IO) {
        val appInfo = HPackages.getApplicationInfoOrNull(packageName) ?: return@withContext false
        val sourceFile = File(appInfo.sourceDir)
        if (!sourceFile.exists()) return@withContext false
        
        val cacheFile = File(context.cacheDir, "${packageName}.apk")
        
        runCatching {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.channel.transferTo(0, input.channel.size(), output.channel)
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            true
        }.getOrElse {
            HLog.e(it)
            false
        }
    }

    fun setHideFromLauncher(packageName: String, hide: Boolean) {
        // Feature removed as requested
    }

    suspend fun execute(command: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        when {
            FraisData.workingMode.startsWith(FraisData.SHIZUKU) -> HShizuku.execute(command)
            else -> 0 to null
        }
    }
}
