package com.khaled.frais.app

import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.khaled.frais.BuildConfig
import com.khaled.frais.FraisApp.Companion.app
import com.khaled.frais.utils.HFiles
import com.khaled.frais.utils.HPackages
import org.json.JSONArray
import org.json.JSONObject

object FraisData {
    const val URL_GITHUB = "https://github.com/khaled0528/Frais"
    const val VERSION = "1.7"
    private const val KEY_ID = "id"
    private const val KEY_PINNED = "pinned"
    private const val KEY_WHITELISTED = "whitelisted"
    private const val KEY_PRIVATE = "private"
    private const val KEY_SAFE_TO_FREEZE = "safe_to_freeze"
    private const val KEY_EXCLUDE_MOST_USED = "exclude_most_used"
    private const val KEY_HIDE_FROM_LAUNCHER = "hide_from_launcher"
    private const val KEY_MUTE_ON_LAUNCH = "mute_on_launch"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    private const val SORT_BY = "sort_by"
    const val SORT_NAME = "name"
    const val SORT_INSTALL = "install"
    const val SORT_UPDATE = "update"
    const val SORT_USAGE_TIME = "usage_time"
    const val SORT_LAST_USED = "last_used"
    
    const val SHIZUKU = "shizuku_"
    const val DHIZUKU = "dhizuku_"
    const val STOP = "stop"
    const val DISABLE = "disable"
    const val HIDE = "hide"
    const val SUSPEND = "suspend"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = SHIZUKU + SUSPEND
    const val MODE_SHIZUKU_STOP = SHIZUKU + STOP
    const val MODE_SHIZUKU_DISABLE = SHIZUKU + DISABLE
    const val MODE_SHIZUKU_HIDE = SHIZUKU + HIDE
    const val MODE_SHIZUKU_SUSPEND = SHIZUKU + SUSPEND
    const val MODE_DHIZUKU_HIDE = DHIZUKU + HIDE
    const val MODE_DHIZUKU_SUSPEND = DHIZUKU + SUSPEND

    val WORKING_MODE_VALUES = listOf(
        MODE_SHIZUKU_STOP,
        MODE_SHIZUKU_DISABLE,
        MODE_SHIZUKU_SUSPEND,
        MODE_DHIZUKU_SUSPEND
    )
    
    const val APP_THEME = "app_theme"
    const val FOLLOW_SYSTEM = "follow_system"
    const val THEME_LIGHT = "theme_light"
    const val THEME_DARK = "theme_dark"
    const val THEME_AMOLED = "theme_amoled"
    val APP_THEME_VALUES = listOf(FOLLOW_SYSTEM, THEME_LIGHT, THEME_DARK, THEME_AMOLED)
    const val FUZZY_SEARCH = "fuzzy_search"
    const val SMART_CLASSIFICATION = "smart_classification"
    const val FLEXIBLE_FILTERS = "flexible_filters"

    const val GRID_COLUMNS = "grid_columns_f"
    const val ICON_SIZE = "icon_size_f"
    const val SHOW_LABELS = "show_labels"
    const val SPACING_TYPE = "spacing_type"
    const val SHOW_TAGS = "show_tags"
    const val SHOW_SYSTEM_APPS = "show_system_apps"
    const val SHOW_NON_LAUNCHABLE_APPS = "show_non_launchable_apps"
    const val HOME_TAGS_COLLAPSED = "home_tags_collapsed"
    const val HOME_FAVORITES_COLLAPSED = "home_favorites_collapsed"
    const val SHOW_PULSE_DOT = "show_pulse_dot"
    const val GRAIN_INTENSITY = "grain_intensity"
    const val AUTO_FREEZE_NOTIFICATION = "auto_freeze_notification"
    const val SMART_MAPPING_LOCATION = "smart_mapping_location"
    const val SMART_MAPPING_DATA = "smart_mapping_data"

    const val ACTION_NONE = "none"

    const val TILE_ACTION = "tile_action"
    val TILE_ACTION_VALUES = listOf(
        ACTION_NONE
    )

    const val LAST_SELECTED_TAG = "last_selected_tag"

    const val SETTINGS_FILTERS_EXPANDED = "settings_filters_expanded"
    const val SETTINGS_SMART_EXPANDED = "settings_smart_expanded"
    const val SETTINGS_APPEARANCE_EXPANDED = "settings_appearance_expanded"
    const val SETTINGS_DATA_EXPANDED = "settings_data_expanded"
    const val SETTINGS_CORE_EXPANDED = "settings_core_expanded"
    const val SETTINGS_EXPANDED_FILTER = "settings_expanded_filter"
    const val DELETED_TAGS = "deleted_tags"

    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(app) }
    var sortBy: String
        get() = sp.getString(SORT_BY, SORT_NAME) ?: SORT_NAME
        set(value) = sp.edit { putString(SORT_BY, value) }
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)!!
    val appTheme get() = sp.getString(APP_THEME, THEME_AMOLED)!!
    val fuzzySearch get() = sp.getBoolean(FUZZY_SEARCH, true)
    var smartClassification
        get() = sp.getBoolean(SMART_CLASSIFICATION, true)
        set(value) = sp.edit { putBoolean(SMART_CLASSIFICATION, value) }
    var flexibleFilters
        get() = sp.getBoolean(FLEXIBLE_FILTERS, true)
        set(value) = sp.edit { putBoolean(FLEXIBLE_FILTERS, value) }
    val tileAction get() = sp.getString(TILE_ACTION, ACTION_NONE)!!
    val iconPack get() = sp.getString("icon_pack", ACTION_NONE)!!
    
    var showPulseDot
        get() = sp.getBoolean(SHOW_PULSE_DOT, true)
        set(value) = sp.edit { putBoolean(SHOW_PULSE_DOT, value) }

    var grainIntensity
        get() = sp.getFloat(GRAIN_INTENSITY, 0.1f)
        set(value) = sp.edit { putFloat(GRAIN_INTENSITY, value) }

    var autoFreezeNotification
        get() = sp.getBoolean(AUTO_FREEZE_NOTIFICATION, false)
        set(value) = sp.edit { putBoolean(AUTO_FREEZE_NOTIFICATION, value) }

    var smartMappingLocation
        get() = sp.getBoolean(SMART_MAPPING_LOCATION, true)
        set(value) = sp.edit { putBoolean(SMART_MAPPING_LOCATION, value) }

    var smartMappingData
        get() = sp.getBoolean(SMART_MAPPING_DATA, true)
        set(value) = sp.edit { putBoolean(SMART_MAPPING_DATA, value) }

    var lastSelectedTag: Int
        get() = sp.getInt(LAST_SELECTED_TAG, TAG_ID_MOST_USED)
        set(value) = sp.edit { putInt(LAST_SELECTED_TAG, value) }

    val gridColumns get() = sp.getFloat(GRID_COLUMNS, 4f).toInt()
    val iconSize get() = sp.getFloat(ICON_SIZE, 64f)
    val showLabels get() = sp.getBoolean(SHOW_LABELS, true)
    val spacingType get() = sp.getString(SPACING_TYPE, "comfortable")!!

    var showTags
        get() = sp.getString(SHOW_TAGS, "always") ?: "always"
        set(value) = sp.edit { putString(SHOW_TAGS, value) }

    var showNonLaunchableApps
        get() = sp.getBoolean(SHOW_NON_LAUNCHABLE_APPS, false)
        set(value) = sp.edit { putBoolean(SHOW_NON_LAUNCHABLE_APPS, value) }

    var showSystemApps
        get() = sp.getBoolean(SHOW_SYSTEM_APPS, false)
        set(value) = sp.edit { putBoolean(SHOW_SYSTEM_APPS, value) }

    var homeTagsCollapsed
        get() = sp.getBoolean(HOME_TAGS_COLLAPSED, false)
        set(value) = sp.edit { putBoolean(HOME_TAGS_COLLAPSED, value) }

    var homeFavoritesCollapsed
        get() = sp.getBoolean(HOME_FAVORITES_COLLAPSED, false)
        set(value) = sp.edit { putBoolean(HOME_FAVORITES_COLLAPSED, value) }

    private val dir = "${app.filesDir.path}/v1"
    private val appsPath = "$dir/apps.json"
    private val tagsPath = "$dir/tags.json"
    private val widgetsPath = "$dir/widgets.json"

    data class Tag(
        val id: Int,
        var name: String,
        var icon: String = "🏷️",
        var color: Int? = null,
        var isBuiltIn: Boolean = false,
        var isEnabled: Boolean = true,
        var order: Int = 0
    )

    data class WidgetMetadata(
        val appWidgetId: Int,
        val provider: String,
        var order: Int = 0
    )
    
    data class AppMetadata(
        val packageName: String,
        var pinned: Boolean = false,
        var whitelisted: Boolean = false,
        var isPrivate: Boolean = false,
        var isSafeToFreeze: Boolean = false,
        var excludeMostUsed: Boolean = false,
        var hideFromLauncher: Boolean = false,
        var muteOnLaunch: Boolean = false,
        var locationOnLaunch: Boolean = false,
        var dataOnLaunch: Boolean = false,
        var batterySaverOnLaunch: Boolean = false,
        var manualTagId: Int? = null,
        val excludedTagIds: MutableList<Int> = java.util.Collections.synchronizedList(mutableListOf())
    )

    private val metadataMap: java.util.concurrent.ConcurrentHashMap<String, AppMetadata> by lazy {
        java.util.concurrent.ConcurrentHashMap<String, AppMetadata>().apply {
            runCatching {
                val jsonText = HFiles.read(appsPath)
                if (jsonText.isNullOrEmpty()) return@apply
                val json = JSONArray(jsonText)
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val pkg = obj.getString(KEY_PACKAGE)
                    
                    put(pkg, AppMetadata(
                        packageName = pkg,
                        pinned = obj.optBoolean(KEY_PINNED),
                        whitelisted = obj.optBoolean(KEY_WHITELISTED),
                        isPrivate = obj.optBoolean(KEY_PRIVATE),
                        isSafeToFreeze = obj.optBoolean(KEY_SAFE_TO_FREEZE),
                        excludeMostUsed = obj.optBoolean(KEY_EXCLUDE_MOST_USED),
                        hideFromLauncher = obj.optBoolean(KEY_HIDE_FROM_LAUNCHER),
                        muteOnLaunch = obj.optBoolean(KEY_MUTE_ON_LAUNCH),
                        locationOnLaunch = obj.optBoolean("location_on_launch"),
                        dataOnLaunch = obj.optBoolean("data_on_launch"),
                        batterySaverOnLaunch = obj.optBoolean("battery_saver_on_launch"),
                        manualTagId = if (obj.has("manualTagId")) {
                            val id = obj.getInt("manualTagId")
                            if (id == -1) null else id
                        } else {
                            obj.optJSONArray("tagIds")?.let {
                                if (it.length() > 0) it.getInt(0) else null
                            }
                        },
                        excludedTagIds = java.util.Collections.synchronizedList(obj.optJSONArray("excludedTagIds")?.let {
                            MutableList(it.length()) { index -> it.getInt(index) }
                        } ?: mutableListOf())
                    ))
                }
            }
        }
    }

    fun getMetadata(packageName: String): AppMetadata {
        return metadataMap.getOrPut(packageName) { AppMetadata(packageName) }
    }

    @Synchronized
    fun saveApps() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        val list = metadataMap.values.toList()
        HFiles.write(appsPath, JSONArray().run {
            list.forEach {
                if (it.pinned || it.whitelisted || it.isPrivate || it.isSafeToFreeze || it.excludeMostUsed || it.hideFromLauncher || it.muteOnLaunch || it.locationOnLaunch || it.dataOnLaunch || it.manualTagId != null || it.excludedTagIds.isNotEmpty()) {
                    put(
                        JSONObject()
                            .put(KEY_PACKAGE, it.packageName)
                            .put(KEY_PINNED, it.pinned)
                            .put(KEY_WHITELISTED, it.whitelisted)
                            .put(KEY_PRIVATE, it.isPrivate)
                            .put(KEY_SAFE_TO_FREEZE, it.isSafeToFreeze)
                            .put(KEY_EXCLUDE_MOST_USED, it.excludeMostUsed)
                            .put(KEY_HIDE_FROM_LAUNCHER, it.hideFromLauncher)
                            .put(KEY_MUTE_ON_LAUNCH, it.muteOnLaunch)
                            .put("location_on_launch", it.locationOnLaunch)
                            .put("data_on_launch", it.dataOnLaunch)
                            .put("battery_saver_on_launch", it.batterySaverOnLaunch)
                            .put("manualTagId", it.manualTagId ?: -1)
                            .put("excludedTagIds", JSONArray(synchronized(it.excludedTagIds) { it.excludedTagIds.toList() }))
                    )
                }
            }
            toString()
        })
    }

    const val TAG_ID_MOST_USED = -15
    const val TAG_ID_GAMES = -1
    const val TAG_ID_SOCIAL = -2
    const val TAG_ID_COMMUNICATION = -3
    const val TAG_ID_PRODUCTIVITY = -4
    const val TAG_ID_MEDIA = -5
    const val TAG_ID_PHOTOGRAPHY = -6
    const val TAG_ID_FINANCE = -7
    const val TAG_ID_EDUCATION = -8
    const val TAG_ID_TOOLS = -9
    const val TAG_ID_BROWSERS = -10
    const val TAG_ID_SHOPPING = -11
    const val TAG_ID_DEVELOPMENT = -16
    const val TAG_ID_TRAVEL = -17
    const val TAG_ID_HEALTH = -18
    const val TAG_ID_SYSTEM = -12
    const val TAG_ID_USER = -13
    const val TAG_ID_OTHER = -14

    private val builtInTags = listOf(
        Tag(TAG_ID_MOST_USED, "Most Used", "🔥", isBuiltIn = true, order = -1),
        Tag(TAG_ID_GAMES, "Games", "🎮", isBuiltIn = true),
        Tag(TAG_ID_SOCIAL, "Social", "👥", isBuiltIn = true),
        Tag(TAG_ID_COMMUNICATION, "Communication", "💬", isBuiltIn = true),
        Tag(TAG_ID_PRODUCTIVITY, "Productivity", "💼", isBuiltIn = true),
        Tag(TAG_ID_MEDIA, "Media", "🎬", isBuiltIn = true),
        Tag(TAG_ID_PHOTOGRAPHY, "Photography", "📷", isBuiltIn = true),
        Tag(TAG_ID_FINANCE, "Finance", "💰", isBuiltIn = true),
        Tag(TAG_ID_EDUCATION, "Education", "🎓", isBuiltIn = true),
        Tag(TAG_ID_TOOLS, "Tools", "🔧", isBuiltIn = true),
        Tag(TAG_ID_BROWSERS, "Browsers", "🌐", isBuiltIn = true),
        Tag(TAG_ID_SHOPPING, "Shopping", "🛍️", isBuiltIn = true),
        Tag(TAG_ID_DEVELOPMENT, "Development", "💻", isBuiltIn = true),
        Tag(TAG_ID_TRAVEL, "Travel", "✈️", isBuiltIn = true),
        Tag(TAG_ID_HEALTH, "Health", "❤️", isBuiltIn = true),
        Tag(TAG_ID_SYSTEM, "System", "⚙️", isBuiltIn = true),
        Tag(TAG_ID_USER, "User Apps", "👤", isBuiltIn = true),
        Tag(TAG_ID_OTHER, "Other", "📦", isBuiltIn = true)
    )

    val tags: MutableList<Tag> by lazy {
        val deletedIds = sp.getStringSet(DELETED_TAGS, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        java.util.Collections.synchronizedList(mutableListOf<Tag>().apply {
            addAll(builtInTags.filter { it.id !in deletedIds })
            runCatching {
                val jsonText = HFiles.read(tagsPath)
                if (jsonText.isNullOrEmpty()) return@apply
                val json = JSONArray(jsonText)
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val id = obj.getInt(KEY_ID)
                    if (id in deletedIds) continue

                    val name = obj.getString("name")
                    val icon = obj.optString("icon", "🏷️")
                    val color = obj.optInt("color").takeIf { obj.has("color") }
                    val isEnabled = obj.optBoolean("isEnabled", true)
                    val order = obj.optInt("order", 0)

                    val existing = find { it.id == id }
                    if (existing != null) {
                        existing.name = name
                        existing.icon = icon
                        existing.color = color
                        existing.isEnabled = isEnabled
                        existing.order = order
                    } else {
                        add(Tag(id, name, icon, color, isBuiltIn = false, isEnabled, order))
                    }
                }
            }
            sortBy { it.order }
        })
    }

    val widgets: MutableList<WidgetMetadata> by lazy {
        java.util.Collections.synchronizedList(mutableListOf<WidgetMetadata>().apply {
            runCatching {
                val jsonText = HFiles.read(widgetsPath)
                if (jsonText.isNullOrEmpty()) return@apply
                val json = JSONArray(jsonText)
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    add(WidgetMetadata(
                        appWidgetId = obj.getInt("appWidgetId"),
                        provider = obj.getString("provider"),
                        order = obj.optInt("order", i)
                    ))
                }
            }
            sortBy { it.order }
        })
    }

    @Synchronized
    fun saveWidgets() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        val list = synchronized(widgets) { widgets.toList() }
        HFiles.write(widgetsPath, JSONArray().run {
            list.forEach {
                put(JSONObject()
                    .put("appWidgetId", it.appWidgetId)
                    .put("provider", it.provider)
                    .put("order", it.order))
            }
            toString()
        })
    }

    @Synchronized
    fun saveTags() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        val list = synchronized(tags) { tags.toList() }
        HFiles.write(tagsPath, JSONArray().run {
            list.forEach {
                put(JSONObject()
                    .put(KEY_ID, it.id)
                    .put("name", it.name)
                    .put("icon", it.icon)
                    .put("isEnabled", it.isEnabled)
                    .put("order", it.order)
                    .apply {
                        it.color?.let { c -> put("color", c) }
                    })
            }
            toString()
        })
    }

    @Synchronized
    fun addTag(name: String, icon: String = "🏷️", color: Int? = null) {
        val nextId = (synchronized(tags) { tags.maxOfOrNull { it.id } } ?: 0) + 1
        tags.add(Tag(nextId, name, icon, color))
        saveTags()
    }

    @Synchronized
    fun deleteTag(id: Int) {
        tags.removeAll { it.id == id }
        val deletedIds = sp.getStringSet(DELETED_TAGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.add(id.toString())
        sp.edit { putStringSet(DELETED_TAGS, deletedIds) }

        saveTags()
        val metadataList = synchronized(metadataMap) { metadataMap.values.toList() }
        metadataList.forEach { 
            if (it.manualTagId == id) it.manualTagId = null
        }
        saveApps()
    }

    @Synchronized
    fun updateTag(tag: Tag) {
        val index = synchronized(tags) { tags.indexOfFirst { it.id == tag.id } }
        if (index != -1) {
            tags[index] = tag
            saveTags()
        }
    }

    private val tagColors = listOf(
        0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
        0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFDCE775.toInt(),
        0xFFFFF176.toInt(), 0xFFFFD54F.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt()
    )

    fun getTagColor(tag: Tag): Int {
        return tag.color ?: tagColors[Math.abs(tag.name.hashCode()) % tagColors.size]
    }
}
