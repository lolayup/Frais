package com.khaled.frais.app

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.khaled.frais.utils.HPackages

class AppInfo(
    val packageName: String,
    initialName: String,
    val applicationInfo: ApplicationInfo?,
    var lastUsed: Long = 0,
    var usageTime: Long = 0,
    var installTime: Long = 0,
    initialStorageSize: Long = 0,
    var isLaunchable: Boolean = true,
    var isGame: Boolean = false,
    var autoTagIds: List<Int> = emptyList()
) {
    constructor(info: ApplicationInfo, label: String? = null) : this(
        packageName = info.packageName,
        initialName = label ?: info.packageName,
        applicationInfo = info,
        isLaunchable = true
    )

    constructor(packageName: String) : this(
        packageName = packageName,
        initialName = packageName,
        applicationInfo = HPackages.getApplicationInfoOrNull(packageName),
        isLaunchable = true
    )

    enum class State { NOT_FOUND, UNFROZEN, FROZEN }

    private val metadata get() = FraisData.getMetadata(packageName)

    private var _name by mutableStateOf(initialName)
    var name: String
        get() = _name
        set(value) { _name = value }

    private var _storageSize by mutableStateOf(initialStorageSize)
    var storageSize: Long
        get() = _storageSize
        set(value) { _storageSize = value }

    private var _pinned by mutableStateOf(metadata.pinned)
    var pinned: Boolean
        get() = _pinned
        set(value) {
            _pinned = value
            metadata.pinned = value
            FraisData.saveApps()
        }

    private var _whitelisted by mutableStateOf(metadata.whitelisted)
    var whitelisted: Boolean
        get() = _whitelisted
        set(value) {
            _whitelisted = value
            metadata.whitelisted = value
            FraisData.saveApps()
        }

    val isWhitelisted get() = whitelisted

    private var _isPrivate by mutableStateOf(metadata.isPrivate)
    var isPrivate: Boolean
        get() = _isPrivate
        set(value) {
            _isPrivate = value
            metadata.isPrivate = value
            FraisData.saveApps()
        }

    private var _isSafeToFreeze by mutableStateOf(metadata.isSafeToFreeze)
    var isSafeToFreeze: Boolean
        get() = _isSafeToFreeze
        set(value) {
            _isSafeToFreeze = value
            metadata.isSafeToFreeze = value
            FraisData.saveApps()
        }

    private var _excludeMostUsed by mutableStateOf(metadata.excludeMostUsed)
    var excludeMostUsed: Boolean
        get() = _excludeMostUsed
        set(value) {
            setExcludeMostUsed(value, save = true)
        }

    fun setExcludeMostUsed(value: Boolean, save: Boolean = true) {
        _excludeMostUsed = value
        metadata.excludeMostUsed = value
        if (save) FraisData.saveApps()
    }

    private var _hideFromLauncher by mutableStateOf(metadata.hideFromLauncher)
    var hideFromLauncher: Boolean
        get() = _hideFromLauncher
        set(value) {
            _hideFromLauncher = value
            metadata.hideFromLauncher = value
            FraisData.saveApps()
            AppManager.setHideFromLauncher(packageName, value)
        }

    val isSystemApp: Boolean
        get() = applicationInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

    val isInstalled: Boolean
        get() = applicationInfo?.let { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 } ?: false

    private var _muteOnLaunch by mutableStateOf(metadata.muteOnLaunch)
    var muteOnLaunch: Boolean
        get() = _muteOnLaunch
        set(value) {
            _muteOnLaunch = value
            metadata.muteOnLaunch = value
            FraisData.saveApps()
        }

    private var _locationOnLaunch by mutableStateOf(metadata.locationOnLaunch)
    var locationOnLaunch: Boolean
        get() = _locationOnLaunch
        set(value) {
            _locationOnLaunch = value
            metadata.locationOnLaunch = value
            FraisData.saveApps()
        }

    private var _dataOnLaunch by mutableStateOf(metadata.dataOnLaunch)
    var dataOnLaunch: Boolean
        get() = _dataOnLaunch
        set(value) {
            _dataOnLaunch = value
            metadata.dataOnLaunch = value
            FraisData.saveApps()
        }

    private var _batterySaverOnLaunch by mutableStateOf(metadata.batterySaverOnLaunch)
    var batterySaverOnLaunch: Boolean
        get() = _batterySaverOnLaunch
        set(value) {
            _batterySaverOnLaunch = value
            metadata.batterySaverOnLaunch = value
            FraisData.saveApps()
        }

    val tagIds: List<Int>
        get() {
            val result = mutableListOf<Int>()
            
            // 1. Primary Category (Exclusive)
            val category = manualTagId ?: getBestAutoTag()
            if (category != null) result.add(category)
            
            // 2. Meta Filter (Inclusive - Most Used)
            if (autoTagIds.contains(FraisData.TAG_ID_MOST_USED) && FraisData.TAG_ID_MOST_USED !in excludedTagIds) {
                result.add(FraisData.TAG_ID_MOST_USED)
            }
            
            return result.distinct()
        }

    private fun getBestAutoTag(): Int? {
        val candidates = autoTagIds.filter { it != FraisData.TAG_ID_MOST_USED && it !in excludedTagIds }
        if (candidates.isEmpty()) return null
        
        // Specific categories have priority over generic ones
        val priority = listOf(
            FraisData.TAG_ID_GAMES,
            FraisData.TAG_ID_SOCIAL,
            FraisData.TAG_ID_COMMUNICATION,
            FraisData.TAG_ID_PRODUCTIVITY,
            FraisData.TAG_ID_MEDIA,
            FraisData.TAG_ID_PHOTOGRAPHY,
            FraisData.TAG_ID_FINANCE,
            FraisData.TAG_ID_EDUCATION,
            FraisData.TAG_ID_TOOLS,
            FraisData.TAG_ID_BROWSERS,
            FraisData.TAG_ID_SHOPPING,
            FraisData.TAG_ID_DEVELOPMENT,
            FraisData.TAG_ID_TRAVEL,
            FraisData.TAG_ID_HEALTH,
            FraisData.TAG_ID_OTHER,
            FraisData.TAG_ID_USER,
            FraisData.TAG_ID_SYSTEM
        )
        
        return priority.firstOrNull { it in candidates } ?: candidates.first()
    }

    var manualTagId: Int?
        get() = metadata.manualTagId
        set(value) {
            metadata.manualTagId = value
            FraisData.saveApps()
        }

    val excludedTagIds: MutableList<Int>
        get() = metadata.excludedTagIds

    var state: State by mutableStateOf(deriveState())
        private set

    private fun deriveState(): State = when {
        applicationInfo == null -> State.NOT_FOUND
        AppManager.isAppFrozen(packageName) -> State.FROZEN
        else -> State.UNFROZEN
    }

    fun updateState() {
        state = deriveState()
    }

    fun updateIsGame() {
        if (applicationInfo == null) {
            isGame = false
            return
        }

        val systemCategory = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            applicationInfo.category
        } else -1
        
        val systemSaysGame = systemCategory == ApplicationInfo.CATEGORY_GAME
        
        val isSystemComponent = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) || 
                                (applicationInfo.packageName == "android") || 
                                (applicationInfo.packageName.startsWith("com.android.systemui"))

        val isLauncher = applicationInfo.packageName.let { pkg ->
            listOf("launcher", "home", "setupwizard").any { pkg.contains(it, ignoreCase = true) }
        }
        
        // Extended detection
        val metaData = applicationInfo.metaData
        val metaDataGame = metaData?.getBoolean("isGame") == true || 
                           metaData?.getString("android.service.games") != null
        
        val knownGamePrefixes = listOf(
            "com.tencent.tmgp", "com.netease", "com.miHoYo", "com.supercell", 
            "com.roblox", "com.mojang", "com.epicgames", "com.valvesoftware",
            "com.activision", "com.ea.", "com.ubisoft", "com.square_enix",
            "com.bandainamcoent", "com.nintendo", "com.sega", "com.gameloft",
            "com.zynga", "com.kabam", "com.rovio", "com.playrix", "com.king",
            "com.popcap.", "com.rockstargames.", "com.nianticlabs.", "com.garena.",
            "com.playgendary.", "com.scopely.", "com.outfit7.", "com.miniclip.",
            "com.voodoo.", "com.playrix.", "com.wildlife.", "com.tfgco."
        )
        val pkgPrefixGame = knownGamePrefixes.any { applicationInfo.packageName.startsWith(it) }

        val gameKeywords = listOf(
            ".game", "game.", ".rpg", ".simulation", ".simulator", ".puzzle", 
            ".arcade", ".racing", ".battle", ".sports", ".action", ".adventure",
            ".strategy", ".casino", ".cards", ".trivia", ".board", ".word",
            ".unity", ".godot", ".libgdx", ".unreal"
        )
        val pkgKeywordGame = gameKeywords.any { applicationInfo.packageName.contains(it, ignoreCase = true) }

        isGame = (systemSaysGame || metaDataGame || pkgPrefixGame || pkgKeywordGame) && !isSystemComponent && !isLauncher
    }

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName
    override fun hashCode(): Int = packageName.hashCode()
}
