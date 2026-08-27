package com.khaled.frais.ui.home

import android.content.Intent
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaled.frais.FraisApp.Companion.app
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FilterClassifier
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.GlyphState
import com.khaled.frais.utils.HPackages
import com.khaled.frais.utils.HUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilterWithCount(
    val filter: FraisData.Tag,
    val unfrozenCount: Int,
    val actionableRunningCount: Int
)

data class HomeUiState(
    val apps: List<AppInfo> = emptyList(),
    val privateApps: List<AppInfo> = emptyList(),
    val games: List<AppInfo> = emptyList(),
    val filters: List<FilterWithCount> = emptyList(),
    val selectedFilters: Set<Int> =
        if (FraisData.lastSelectedTag != 0) {
            setOf(FraisData.lastSelectedTag)
        } else {
            emptySet()
        },
    val searchQuery: String = "",
    val searchSystemFilter: String = if (FraisData.showSystemApps) "all" else "user",
    val searchFrozenFilter: String = "all",
    val isLoading: Boolean = false,
    val isServiceRunning: Boolean = true,
    val isShizukuAvailable: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val isUsagePermissionGranted: Boolean = false,
    val isTagAreaCollapsed: Boolean = FraisData.homeTagsCollapsed,
    val totalPlayTime: Long = 0L,
    val lastPlayedGame: AppInfo? = null,
    val favoriteGame: AppInfo? = null,
    val suggestedForRemoval: List<AppInfo> = emptyList(),
    val isFreezing: Boolean = false,
    val actionableAppsCount: Int = 0,
    val actionablePrivateAppsCount: Int = 0,
    val totalUserAppsCount: Int = 0,
    val totalAppsCount: Int = 0,
    val transientGlyphState: GlyphState? = null,
    val allApps: List<AppInfo> = emptyList(),
    val mostUsedApps: List<AppInfo> = emptyList(),
    val isInitialLoad: Boolean = true
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isPrivateSpaceAuthenticated = MutableStateFlow(false)
    val isPrivateSpaceAuthenticated: StateFlow<Boolean> =
        _isPrivateSpaceAuthenticated.asStateFlow()

    private var allAppsCached: List<AppInfo> = emptyList()

    init {
        refresh()
    }

    fun setPrivateSpaceAuthenticated(authenticated: Boolean) {
        _isPrivateSpaceAuthenticated.value = authenticated
        if (!authenticated) {
            val privateAppsToFreeze = allAppsCached.filter { it.isPrivate && it.state != AppInfo.State.FROZEN }
            if (privateAppsToFreeze.isNotEmpty()) {
                setAppsFrozen(privateAppsToFreeze, true)
            }
        }
    }

    // ---------------------------------------------------------
    // REFRESH
    // ---------------------------------------------------------

    fun refresh(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(isLoading = true)
            }

            if (force || allAppsCached.isEmpty()) {

                val usageStats = HUsage.getUsageStats()
                val packageManager = app.packageManager

                /*
                 * IMPORTANT:
                 *
                 * HPackages.getInstalledApps() MUST return both
                 * user and system applications.
                 *
                 * We intentionally do NOT filter system apps here.
                 */
                val installedApps =
                    HPackages.getInstalledApps(packageManager)

                val launchIntentPackages =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

                        packageManager.queryIntentActivities(
                            Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                            android.content.pm.PackageManager.ResolveInfoFlags.of(
                                android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
                            )
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        packageManager.queryIntentActivities(
                            Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                            android.content.pm.PackageManager.GET_DISABLED_COMPONENTS
                        )
                    }
                        .map { it.activityInfo.packageName }
                        .toSet()

                /*
                 * Load application metadata in parallel.
                 */
                val loadedApps = installedApps
                    .map { info ->
                        async {

                            val label =
                                info.loadLabel(packageManager).toString()

                            AppInfo(
                                info,
                                label
                            ).apply {

                                lastUsed =
                                    HUsage.getLastUsedTime(
                                        packageName,
                                        usageStats
                                    )

                                usageTime =
                                    HUsage.getTotalForegroundTime(
                                        packageName,
                                        usageStats
                                    )

                                installTime =
                                    HPackages.getFirstInstallTime(
                                        packageName
                                    )

                                storageSize =
                                    com.khaled.frais.utils.HStorage
                                        .getAppSize(packageName)

                                isLaunchable =
                                    packageName in launchIntentPackages

                                updateIsGame()

                                /*
                                 * Initial automatic classification.
                                 */
                                autoTagIds =
                                    FilterClassifier.classify(this)
                            }
                        }
                    }
                    .awaitAll()

                /*
                 * Detect launchers and exclude them from most used.
                 * This is done after parallel loading to avoid concurrent modification issues.
                 */
                var changesMade = false
                loadedApps.forEach { appInfo ->
                    val isLauncher = appInfo.packageName.let { pkg ->
                        listOf("launcher", "home", "setupwizard", "frais").any { pkg.contains(it, ignoreCase = true) }
                    }
                    if (isLauncher && !appInfo.excludeMostUsed) {
                        appInfo.setExcludeMostUsed(true, save = false)
                        changesMade = true
                    }
                }
                if (changesMade) {
                    FraisData.saveApps()
                }

                allAppsCached = loadedApps
            }

            updateFilteredApps()
        }
    }

    // ---------------------------------------------------------
    // BUILD HOME DATA
    // ---------------------------------------------------------

    fun updateFilteredApps() {

        val allApps = allAppsCached

        /*
         * First update every application's current state.
         *
         * This updates frozen/whitelisted/manual state.
         */
        allApps.forEach { appInfo ->
            appInfo.updateState()
        }

        /*
         * MOST USED
         * Filter apps used for more than 10 hours in the last week.
         */
        val tenHoursMs = 10 * 60 * 60 * 1000L
        val mostUsedApps =
            allApps
                .filter {
                    it.usageTime >= tenHoursMs &&
                            !it.excludeMostUsed
                }
                .sortedByDescending {
                    it.usageTime
                }

        /*
         * Rebuild automatic classifications.
         *
         * System apps are NOT excluded here.
         */
        allApps.forEach { appInfo ->

            val tags =
                FilterClassifier
                    .classify(appInfo)
                    .toMutableList()

            if (appInfo in mostUsedApps) {
                tags.add(FraisData.TAG_ID_MOST_USED)
            }

            appInfo.autoTagIds = tags

            /*
             * Rebuild tagIds after changing autoTagIds.
             */
            appInfo.updateState()
        }

        val tags = FraisData.tags

        /*
         * HOME APPLICATION POOL
         *
         * Important:
         *
         * System apps remain here.
         *
         * We only remove:
         * - private apps
         * - games
         *
         * System/user filtering happens later in HomeScreen.
         */
        val apps =
            allApps.filter {
                !it.isPrivate &&
                        !it.isGame &&
                        it.isInstalled &&
                        (it.isLaunchable || FraisData.showNonLaunchableApps)
            }

        val privateApps =
            allApps.filter {
                it.isPrivate &&
                        !it.isGame &&
                        it.isInstalled &&
                        (it.isLaunchable || FraisData.showNonLaunchableApps)
            }

        val games =
            allApps.filter {
                it.isGame &&
                        it.isInstalled &&
                        (it.isLaunchable || FraisData.showNonLaunchableApps)
            }

        /*
         * FILTER COUNTS
         *
         * Counts now come from the same "apps" pool that
         * HomeScreen displays.
         *
         * This means system apps are included in the filter
         * count instead of being silently excluded.
         */
        val filtersWithCounts =
            tags.filter { it.id != FraisData.TAG_ID_MOST_USED }.map { tag ->

                val matchingApps =
                    apps.filter { appInfo ->
                        tag.id in appInfo.tagIds
                    }

                val unfrozenCount =
                    matchingApps.count { appInfo ->
                        appInfo.state != AppInfo.State.FROZEN
                    }

                val actionableRunningCount =
                    if (tag.id == FraisData.TAG_ID_MOST_USED) {

                        0

                    } else {

                        matchingApps.count { appInfo ->

                            appInfo.state != AppInfo.State.FROZEN &&
                                    !appInfo.isSystemApp &&
                                    !appInfo.isWhitelisted
                        }
                    }

                FilterWithCount(
                    filter = tag,
                    unfrozenCount = unfrozenCount,
                    actionableRunningCount = actionableRunningCount
                )
            }.let { list ->
                if (FraisData.flexibleFilters) {
                    list.sortedWith(
                        compareByDescending<FilterWithCount> { f ->
                            f.filter.id == FraisData.TAG_ID_MOST_USED && f.unfrozenCount > 0
                        }.thenByDescending { f ->
                            f.actionableRunningCount
                        }.thenByDescending { f ->
                            val matchingApps = apps.filter { f.filter.id in it.tagIds }
                            matchingApps.maxOfOrNull { it.lastUsed } ?: 0L
                        }.thenByDescending { f ->
                            val matchingApps = apps.filter { f.filter.id in it.tagIds }
                            matchingApps.sumOf { it.usageTime }
                        }
                    )
                } else {
                    list
                }
            }

        /*
         * GAME STATISTICS
         */
        val totalPlayTime =
            games.sumOf {
                it.usageTime
            }

        val lastPlayedGame =
            games
                .filter {
                    it.lastUsed > 0
                }
                .maxByOrNull {
                    it.lastUsed
                }

        val favoriteGame =
            games
                .maxByOrNull {
                    it.usageTime
                }
                ?.takeIf {
                    it.usageTime > 0
                }

        /*
         * GAME REMOVAL SUGGESTIONS
         */
        val thirtyDaysAgo =
            System.currentTimeMillis() -
                    (1000L * 60 * 60 * 24 * 30)

        val suggestedForRemoval =
            games
                .filter {

                    (
                            it.usageTime == 0L &&
                                    it.installTime < thirtyDaysAgo
                            ) ||

                            (
                                    it.lastUsed > 0 &&
                                            it.lastUsed < thirtyDaysAgo
                                    )
                }
                .sortedBy {
                    it.lastUsed
                }

        /*
         * ACTIONABLE APPS
         *
         * System apps are included in the total pool,
         * but protected system apps cannot be frozen.
         */
        val actionableAppsCount =
            apps.count { appInfo ->
                !appInfo.isWhitelisted &&
                        appInfo.state != AppInfo.State.FROZEN &&
                        (
                                !appInfo.isSystemApp ||
                                        appInfo.isSafeToFreeze
                                )
            }

        val actionablePrivateAppsCount =
            privateApps.count { appInfo ->
                appInfo.state != AppInfo.State.FROZEN
            }

        val totalUserAppsCount = allApps.count { !it.isSystemApp }
        val totalAppsCount = allApps.size

        /*
         * Publish the complete state.
         */
        _uiState.update {
            it.copy(
                apps = sortApps(apps, it.selectedFilters),

                privateApps =
                    sortApps(privateApps, it.selectedFilters),

                games =
                    sortApps(games, it.selectedFilters),

                filters =
                    filtersWithCounts,

                isLoading = false,

                isServiceRunning =
                    com.khaled.frais.app.AppManager
                        .checkService(),

                isShizukuAvailable = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                    rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.getBinder() != null
                } else true,

                isShizukuPermissionGranted = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                    rikka.shizuku.Shizuku.pingBinder() && 
                    rikka.shizuku.Shizuku.getBinder() != null && 
                    rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true,

                isUsagePermissionGranted =
                    HUsage.isPermissionGranted(),

                totalPlayTime =
                    totalPlayTime,

                lastPlayedGame =
                    lastPlayedGame,

                favoriteGame =
                    favoriteGame,

                suggestedForRemoval =
                    suggestedForRemoval,

                isTagAreaCollapsed =
                    FraisData.homeTagsCollapsed,

                searchSystemFilter = if (FraisData.showSystemApps) "all" else "user",

                actionableAppsCount =
                    actionableAppsCount,

                actionablePrivateAppsCount =
                    actionablePrivateAppsCount,

                totalUserAppsCount =
                    totalUserAppsCount,

                totalAppsCount =
                    totalAppsCount,

                allApps = allApps,

                mostUsedApps = sortApps(mostUsedApps, emptySet()),

                isInitialLoad = false
            )
        }
    }

    // ---------------------------------------------------------
    // SORTING
    // ---------------------------------------------------------

    private fun sortApps(
        apps: List<AppInfo>,
        selectedFilters: Set<Int> = emptySet()
    ): List<AppInfo> {

        return apps.sortedWith(
            compareByDescending<AppInfo> { it.pinned }
                .thenBy { it.state == AppInfo.State.FROZEN }
                .thenByDescending {
                    if (selectedFilters.isEmpty() && !it.excludeMostUsed) it.usageTime else 0L
                }
                .thenByDescending { it.usageTime }
                .thenBy { it.name.lowercase() }
        )
    }

    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    fun setSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query
            )
        }
    }

    fun setSearchSystemFilter(filter: String) {
        _uiState.update {
            it.copy(
                searchSystemFilter = filter
            )
        }
    }

    fun setSearchFrozenFilter(filter: String) {
        _uiState.update {
            it.copy(
                searchFrozenFilter = filter
            )
        }
    }

    // ---------------------------------------------------------
    // TAG MOVEMENT
    // ---------------------------------------------------------

    fun moveTag(
        tagId: Int,
        up: Boolean
    ) {

        val tags = FraisData.tags

        val index =
            tags.indexOfFirst {
                it.id == tagId
            }

        if (index == -1) return

        val targetIndex =
            if (up) {
                index - 1
            } else {
                index + 1
            }

        if (targetIndex in tags.indices) {

            val temp =
                tags[index].order

            tags[index].order =
                tags[targetIndex].order

            tags[targetIndex].order =
                temp

            FraisData.saveTags()

            tags.sortBy {
                it.order
            }

            refresh()
        }
    }

    // ---------------------------------------------------------
    // TAG SELECTION
    // ---------------------------------------------------------

    fun toggleTagSelection(tagId: Int) {

        _uiState.update { state ->

            val newSelectedFilters =
                if (tagId in state.selectedFilters) {

                    emptySet()

                } else {

                    setOf(tagId)
                }

            FraisData.lastSelectedTag =
                newSelectedFilters
                    .firstOrNull()
                    ?: 0

            state.copy(
                selectedFilters =
                    newSelectedFilters
            )
        }
    }

    // ---------------------------------------------------------
    // TAG AREA
    // ---------------------------------------------------------

    fun setTagAreaCollapsed(
        collapsed: Boolean
    ) {

        FraisData.homeTagsCollapsed =
            collapsed

        _uiState.update {
            it.copy(
                isTagAreaCollapsed =
                    collapsed
            )
        }
    }

    // ---------------------------------------------------------
    // GLYPH
    // ---------------------------------------------------------

    fun triggerTransientGlyph(
        state: GlyphState
    ) {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    transientGlyphState =
                        state
                )
            }

            delay(2000)

            _uiState.update {
                it.copy(
                    transientGlyphState =
                        null
                )
            }
        }
    }

    // ---------------------------------------------------------
    // LAUNCH APP
    // ---------------------------------------------------------

    fun launchApp(
        packageName: String,
        context: android.content.Context
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            val appInfo =
                allAppsCached.find {
                    it.packageName == packageName
                } ?: return@launch

            /*
             * Non-launchable applications go to App Info.
             */
            if (!appInfo.isLaunchable) {

                withContext(Dispatchers.Main) {

                    com.khaled.frais.utils.HUI.startActivity(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        com.khaled.frais.utils.HPackages.packageUri(
                            packageName
                        )
                    )
                }

                return@launch
            }

            /*
             * Automatically unfreeze before launching.
             */
            if (appInfo.state == AppInfo.State.FROZEN) {

                val unfrozen =
                    com.khaled.frais.app.AppManager
                        .setAppFrozen(
                            packageName,
                            false
                        )

                if (!unfrozen) {

                    withContext(Dispatchers.Main) {

                        com.khaled.frais.utils.HUI.showToast(
                            app.getString(
                                com.khaled.frais.R.string.operation_failed,
                                appInfo.name
                            )
                        )
                    }

                    return@launch
                }

                appInfo.applicationInfo?.enabled =
                    true

                refresh()
            }

            if (appInfo.muteOnLaunch) {
                withContext(Dispatchers.Main) {
                    val audioManager = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
                    com.khaled.frais.utils.HUI.showToast("VOLUME MUTED")
                }
            }

            if (appInfo.locationOnLaunch || (FraisData.smartMappingLocation && appInfo.tagIds.contains(FraisData.TAG_ID_TRAVEL))) {
                com.khaled.frais.utils.HShizuku.setLocationEnabled(true)
            }
            if (appInfo.dataOnLaunch || (FraisData.smartMappingData && appInfo.tagIds.contains(FraisData.TAG_ID_TRAVEL))) {
                com.khaled.frais.utils.HShizuku.setDataEnabled(true)
            }
            if (appInfo.batterySaverOnLaunch) {
                com.khaled.frais.utils.HShizuku.setBatterySaverEnabled(true)
            }

            /*
             * Give PackageManager a moment to see the
             * newly enabled package.
             */
            withContext(Dispatchers.Main) {

                var intent: Intent? = null

                val startTime =
                    System.currentTimeMillis()

                while (
                    intent == null &&
                    System.currentTimeMillis() -
                    startTime < 1000
                ) {

                    intent =
                        context.packageManager
                            .getLaunchIntentForPackage(
                                packageName
                            )

                    if (intent == null) {
                        delay(50)
                    }
                }

                intent?.let {
                    context.startActivity(it)
                }
                    ?: com.khaled.frais.utils.HUI.showToast(
                        com.khaled.frais.R.string.activity_not_found
                    )
            }
        }
    }

    // ---------------------------------------------------------
    // FREEZE SINGLE APP
    // ---------------------------------------------------------

    fun setAppFrozen(
        app: AppInfo,
        frozen: Boolean,
        onResult: (success: Boolean) -> Unit = {}
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            _uiState.update {
                it.copy(
                    isFreezing = true
                )
            }

            val success =
                com.khaled.frais.app.AppManager
                    .setAppFrozen(
                        app.packageName,
                        frozen
                    )

            if (success) {
                refresh()
            }

            _uiState.update {
                it.copy(
                    isFreezing = false
                )
            }

            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    // ---------------------------------------------------------
    // FREEZE MULTIPLE APPS
    // ---------------------------------------------------------

    fun setAppsFrozen(
        apps: List<AppInfo>,
        frozen: Boolean,
        onResult: (result: String?) -> Unit = {}
    ) {

        if (apps.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            _uiState.update {
                it.copy(
                    isFreezing = true
                )
            }

            val result =
                com.khaled.frais.app.AppManager
                    .setListFrozen(
                        frozen,
                        *apps.toTypedArray()
                    )

            refresh()

            _uiState.update {
                it.copy(
                    isFreezing = false
                )
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    // ---------------------------------------------------------
    // SMART CLASSIFICATION
    // ---------------------------------------------------------

    fun toggleSmartClassification(
        enabled: Boolean
    ) {

        FraisData.smartClassification =
            enabled

        refresh()
    }

    fun toggleShowSystemApps(
        show: Boolean
    ) {
        FraisData.showSystemApps = show
        refresh()
    }
}
