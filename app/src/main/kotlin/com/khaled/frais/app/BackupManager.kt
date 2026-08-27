package com.khaled.frais.app

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.khaled.frais.FraisApp.Companion.app
import com.khaled.frais.utils.HFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream

object BackupManager {

    private val prefKeys = listOf(
        FraisData.WORKING_MODE,
        FraisData.APP_THEME,
        FraisData.FUZZY_SEARCH,
        FraisData.GRID_COLUMNS,
        FraisData.ICON_SIZE,
        FraisData.SHOW_LABELS,
        FraisData.SPACING_TYPE,
        FraisData.SHOW_TAGS,
        FraisData.SHOW_NON_LAUNCHABLE_APPS,
        FraisData.HOME_TAGS_COLLAPSED,
        FraisData.TILE_ACTION,
        FraisData.LAST_SELECTED_TAG,
        "sort_by",
        "icon_pack"
    )

    suspend fun exportData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            
            // Apps
            val appsJson = HFiles.read("${app.filesDir.path}/v1/apps.json")
            if (appsJson != null) root.put("apps", org.json.JSONArray(appsJson))
            
            // Tags
            val tagsJson = HFiles.read("${app.filesDir.path}/v1/tags.json")
            if (tagsJson != null) root.put("tags", org.json.JSONArray(tagsJson))
            
            // Prefs
            val prefs = JSONObject()
            val sp = PreferenceManager.getDefaultSharedPreferences(app)
            prefKeys.forEach { key ->
                if (sp.contains(key)) {
                    val value = sp.all[key]
                    prefs.put(key, value)
                }
            }
            root.put("prefs", prefs)
            
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(root.toString(2).toByteArray())
            }
            true
        }.getOrDefault(false)
    }

    suspend fun importData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { 
                it.bufferedReader().readText()
            } ?: return@runCatching false
            
            val root = JSONObject(jsonText)
            val dir = "${app.filesDir.path}/v1"
            if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
            
            // Restore Apps
            if (root.has("apps")) {
                HFiles.write("$dir/apps.json", root.getJSONArray("apps").toString())
            }
            
            // Restore Tags
            if (root.has("tags")) {
                HFiles.write("$dir/tags.json", root.getJSONArray("tags").toString())
            }
            
            // Restore Prefs
            if (root.has("prefs")) {
                val prefs = root.getJSONObject("prefs")
                val sp = PreferenceManager.getDefaultSharedPreferences(app)
                sp.edit().apply {
                    prefs.keys().forEach { key ->
                        val value = prefs.get(key)
                        when (value) {
                            is Boolean -> putBoolean(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Float -> putFloat(key, value)
                            is String -> putString(key, value)
                            is Double -> putFloat(key, value.toFloat())
                        }
                    }
                    apply()
                }
            }
            true
        }.getOrDefault(false)
    }
}
