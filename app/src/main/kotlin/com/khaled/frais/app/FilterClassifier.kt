package com.khaled.frais.app

import android.content.pm.ApplicationInfo
import com.khaled.frais.utils.HPackages

object FilterClassifier {
    init {
        System.loadLibrary("frais-engine")
    }

    private external fun nativeClassify(packageName: String, label: String, category: Int): IntArray

    fun classify(appInfo: AppInfo): List<Int> {
        val info = appInfo.applicationInfo ?: return listOf(FraisData.TAG_ID_OTHER)
        val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            info.category
        } else {
            -1
        }
        
        val nativeTags = nativeClassify(appInfo.packageName, appInfo.name, category).toList()
        
        // Special mapping logic for Kotlin-side only checks (permissions)
        val finalTags = nativeTags.toMutableList()
        
        if (isMapping(info, appInfo.packageName, appInfo.name.lowercase())) {
            if (FraisData.TAG_ID_TRAVEL !in finalTags) finalTags.add(FraisData.TAG_ID_TRAVEL)
        }
        
        return finalTags.distinct()
    }

    private fun isMapping(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_MAPS) return true
        }
        val keywords = listOf("map", "navigation", "gps", "waze", "uber", "lyft", "grab", "taxi", "tracker")
        if (keywords.any { pkg.contains(it) || label.contains(it) }) return true
        
        // Also check for location permissions
        return HPackages.hasPermission(pkg, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
