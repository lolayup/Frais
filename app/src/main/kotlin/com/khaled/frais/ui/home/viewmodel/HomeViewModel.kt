package com.khaled.frais.ui.home.viewmodel

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(
        searchQuery = savedStateHandle.get<String>("search_query") ?: ""
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isPrivateSpaceAuthenticated = MutableStateFlow(false)
    val isPrivateSpaceAuthenticated: StateFlow<Boolean> =
        _isPrivateSpaceAuthenticated.asStateFlow()

    private val _isSearchActive = MutableStateFlow(savedStateHandle.get<Boolean>("is_search_active") ?: false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _activeScreenIndex = MutableStateFlow(FraisData.activeScreen)
    val activeScreenIndex = _activeScreenIndex.asStateFlow()

    private val _navigationMode = MutableStateFlow(
        savedStateHandle.get<com.khaled.frais.ui.NavigationMode>("navigation_mode") ?: com.khaled.frais.ui.NavigationMode.Widgets
    )
    val navigationMode = _navigationMode.asStateFlow()

    private val _selectedGroup = MutableStateFlow<GridItem.Group?>(null)
    val selectedGroup = _selectedGroup.asStateFlow()

    private val _selectedAppForDialog = MutableStateFlow<AppInfo?>(null)
    val selectedAppForDialog = _selectedAppForDialog.asStateFlow()

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        savedStateHandle["is_search_active"] = active
    }

    fun setActiveScreenIndex(index: Int) {
        _activeScreenIndex.value = index
        FraisData.activeScreen = index
    }

    fun setNavigationMode(mode: com.khaled.frais.ui.NavigationMode) {
        _navigationMode.value = mode
        savedStateHandle["navigation_mode"] = mode
    }

    fun setSelectedGroup(group: GridItem.Group?) {
        _selectedGroup.value = group
    }

    fun setSelectedAppForDialog(app: AppInfo?) {
        _selectedAppForDialog.value = app
    }

    private val _goHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val goHomeEvent = _goHomeEvent.asSharedFlow()

    fun triggerGoHome() {
        _goHomeEvent.tryEmit(Unit)
    }

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

    fun refresh(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(isLoading = true)
            }

            if (force || allAppsCached.isEmpty()) {
                val usageStats = HUsage.getUsageStats()
                val packageManager = app.packageManager

                val installedApps = HPackages.getInstalledApps(packageManager)

                val launchIntentPackages =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        packageManager.queryIntentActivities(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                            android.content.pm.PackageManager.ResolveInfoFlags.of(
                                android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.queryIntentActivities(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                            android.content.pm.PackageManager.GET_DISABLED_COMPONENTS
                        )
                    }
                        .map { it.activityInfo.packageName }
                        .toSet()

                val loadedApps = installedApps
                    .map { info ->
                        async {
                            val label = info.loadLabel(packageManager).toString()
                            val description = try {
                                info.loadDescription(packageManager)?.toString()
                            } catch (e: Exception) {
                                null
                            }
                            AppInfo(info, label).apply {
                                this.description = description
                                lastUsed = HUsage.getLastUsedTime(packageName, usageStats)
                                usageTime = HUsage.getTotalForegroundTime(packageName, usageStats)
                                installTime = HPackages.getFirstInstallTime(packageName)
                                storageSize = com.khaled.frais.utils.HStorage.getAppSize(packageName)
                                isLaunchable = packageName in launchIntentPackages
                                updateIsGame()
                                autoTagIds = FilterClassifier.classify(this)
                                updateSearchRaw()
                            }
                        }
                    }
                    .awaitAll()

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

    fun updateFilteredApps() {
        val allApps = allAppsCached
        val searchQuery = _uiState.value.searchQuery
        val selectedFilters = _uiState.value.selectedFilters
        val searchSystemFilter = _uiState.value.searchSystemFilter
        val searchFrozenFilter = _uiState.value.searchFrozenFilter

        allApps.forEach { appInfo ->
            appInfo.updateState()
            // Optimization: Update search raw only if needed, but here we do it to ensure accuracy
            // In a larger app we might skip this if nothing changed.
            appInfo.updateSearchRaw()
        }

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

        allApps.forEach { appInfo ->
            val tags = FilterClassifier.classify(appInfo).toMutableList()
            if (appInfo in mostUsedApps) {
                tags.add(FraisData.TAG_ID_MOST_USED)
            }
            appInfo.autoTagIds = tags
            appInfo.updateState()
            appInfo.updateSearchRaw()
        }

        val tags = FraisData.tags

        val apps = mutableListOf<AppInfo>()
        val privateApps = mutableListOf<AppInfo>()
        val games = mutableListOf<AppInfo>()
        val hiddenApps = mutableListOf<AppInfo>()
        
        val showNonLaunchable = FraisData.showNonLaunchableApps

        allApps.forEach {
            if (it.isInstalled) {
                if (it.hiddenFromHome) {
                    hiddenApps.add(it)
                } else if (it.isLaunchable || showNonLaunchable) {
                    when {
                        it.isPrivate -> privateApps.add(it)
                        it.isGame -> games.add(it)
                        else -> apps.add(it)
                    }
                }
            }
        }

        val filterCounts = mutableMapOf<Int, Int>()
        val actionableCounts = mutableMapOf<Int, Int>()
        
        apps.forEach { appInfo ->
            appInfo.tagIds.forEach { tagId ->
                filterCounts[tagId] = (filterCounts[tagId] ?: 0) + (if (appInfo.state != AppInfo.State.FROZEN) 1 else 0)
                if (tagId != FraisData.TAG_ID_MOST_USED) {
                    if (appInfo.state != AppInfo.State.FROZEN && !appInfo.isSystemApp && !appInfo.isWhitelisted) {
                        actionableCounts[tagId] = (actionableCounts[tagId] ?: 0) + 1
                    }
                }
            }
        }

        val filtersWithCounts = tags.filter { it.id != FraisData.TAG_ID_MOST_USED }.map { tag ->
            FilterWithCount(
                filter = tag,
                unfrozenCount = filterCounts[tag.id] ?: 0,
                actionableRunningCount = actionableCounts[tag.id] ?: 0
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

        val totalPlayTime = games.sumOf { it.usageTime }
        val lastPlayedGame = games.filter { it.lastUsed > 0 }.maxByOrNull { it.lastUsed }
        val favoriteGame = games.maxByOrNull { it.usageTime }?.takeIf { it.usageTime > 0 }

        val thirtyDaysAgo = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30)
        val suggestedForRemoval =
            games
                .filter {
                    (it.usageTime == 0L && it.installTime < thirtyDaysAgo) ||
                    (it.lastUsed > 0 && it.lastUsed < thirtyDaysAgo)
                }
                .sortedBy { it.lastUsed }

        val protected = FraisData.closeAllProtectedApps
        
        val actionableAppsCount =
            (apps + games).count { appInfo ->
                appInfo.packageName !in protected &&
                !appInfo.isWhitelisted &&
                        appInfo.state != AppInfo.State.FROZEN &&
                        (!appInfo.isSystemApp || appInfo.isSafeToFreeze)
            }

        val actionablePrivateAppsCount =
            privateApps.count { appInfo ->
                appInfo.packageName !in protected &&
                !appInfo.isWhitelisted &&
                appInfo.state != AppInfo.State.FROZEN
            }

        val totalUserAppsCount = allApps.count { !it.isSystemApp }
        val totalAppsCount = allApps.size

        val runningServices = if (com.khaled.frais.app.AppManager.checkService()) {
            com.khaled.frais.utils.HShizuku.getRunningServices()
        } else emptyList()

        val filteredApps = apps.filter { app ->
            val matchesQuery = if (searchQuery.isEmpty()) true
            else {
                com.khaled.frais.utils.FuzzySearch.search(app.searchRaw, searchQuery)
            }
            
            val matchesFilters = if (searchQuery.isNotEmpty() || selectedFilters.isEmpty()) true
            else selectedFilters.any { it in app.tagIds }

            val matchesSystem = when (searchSystemFilter) {
                "user" -> !app.isSystemApp
                "system" -> app.isSystemApp
                else -> true
            }

            val matchesFrozen = when (searchFrozenFilter) {
                "frozen" -> app.state == AppInfo.State.FROZEN
                "unfrozen" -> app.state == AppInfo.State.UNFROZEN
                else -> true
            }

            matchesQuery && matchesFilters && matchesSystem && matchesFrozen
        }

        val pinnedApps = apps.filter { app ->
            if (!app.pinned) return@filter false
            if (searchQuery.isEmpty()) return@filter true
            com.khaled.frais.utils.FuzzySearch.search(app.searchRaw, searchQuery)
        }

        val mostUsed = if (selectedFilters.isEmpty() && searchQuery.isEmpty()) mostUsedApps else emptyList()
        val otherApps = filteredApps.filter { it !in pinnedApps && !it.excludeMostUsed && it !in mostUsed }

        val pinnedGridItems = calculateGridItems(pinnedApps, isPinned = true)
        val mainGridItems = calculateGridItems(otherApps, isPinned = false)

        _uiState.update {
            it.copy(
                apps = sortApps(apps, it.selectedFilters),
                privateApps = sortApps(privateApps, it.selectedFilters),
                games = sortApps(games, it.selectedFilters),
                filters = filtersWithCounts,
                isLoading = false,
                isServiceRunning = com.khaled.frais.app.AppManager.checkService(),
                isShizukuAvailable = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                    rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.getBinder() != null
                } else true,
                isShizukuPermissionGranted = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                    rikka.shizuku.Shizuku.pingBinder() && 
                    rikka.shizuku.Shizuku.getBinder() != null && 
                    rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true,
                isUsagePermissionGranted = HUsage.isPermissionGranted(),
                totalPlayTime = totalPlayTime,
                lastPlayedGame = lastPlayedGame,
                favoriteGame = favoriteGame,
                suggestedForRemoval = suggestedForRemoval,
                isTagAreaCollapsed = FraisData.homeTagsCollapsed,
                searchSystemFilter = if (FraisData.showSystemApps) "all" else "user",
                actionableAppsCount = actionableAppsCount,
                actionablePrivateAppsCount = actionablePrivateAppsCount,
                totalUserAppsCount = totalUserAppsCount,
                totalAppsCount = totalAppsCount,
                runningServices = runningServices,
                allApps = allApps,
                mostUsedApps = sortApps(mostUsedApps, emptySet()),
                hiddenApps = hiddenApps,
                pinnedGridItems = pinnedGridItems,
                mainGridItems = mainGridItems,
                isInitialLoad = false,
                searchQuery = searchQuery,
                selectedFilters = selectedFilters,
                searchFrozenFilter = searchFrozenFilter
            )
        }
    }

    private fun calculateGridItems(apps: List<AppInfo>, isPinned: Boolean): List<GridItem> {
        val list = mutableListOf<GridItem>()
        val selectedFilters = _uiState.value.selectedFilters

        val groups = apps.groupBy { app ->
            val priorityTag = app.tagIds.firstOrNull { it in selectedFilters && it != FraisData.TAG_ID_MOST_USED }
            priorityTag ?: app.tagIds.firstOrNull { it != FraisData.TAG_ID_MOST_USED } ?: FraisData.TAG_ID_OTHER
        }
        
        val processedTags = mutableSetOf<Int>()
        apps.forEach { app ->
            val tagId = app.tagIds.firstOrNull { it in selectedFilters && it != FraisData.TAG_ID_MOST_USED }
                ?: app.tagIds.firstOrNull { it != FraisData.TAG_ID_MOST_USED }
                ?: FraisData.TAG_ID_OTHER
                
            if (tagId !in processedTags) {
                val group = (groups[tagId] ?: emptyList()).sortedWith(
                    compareByDescending<AppInfo> { it.state != AppInfo.State.FROZEN }
                        .thenByDescending { it.usageTime }
                        .thenBy { it.name.lowercase() }
                )
                if (group.size >= 2) {
                    val gid = if (isPinned) "pinned_tag_$tagId" else "main_tag_$tagId"
                    val tag = FraisData.tags.find { it.id == tagId }
                    list.add(GridItem.Group(gid, group, tag?.name))
                } else {
                    group.forEach { list.add(GridItem.App(it)) }
                }
                processedTags.add(tagId)
            }
        }
        return list
    }

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

    fun setSearchQuery(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
        savedStateHandle["search_query"] = query
    }

    fun setSearchSystemFilter(filter: String) {
        _uiState.update {
            it.copy(searchSystemFilter = filter)
        }
    }

    fun setSearchFrozenFilter(filter: String) {
        _uiState.update {
            it.copy(searchFrozenFilter = filter)
        }
    }

    fun moveTag(tagId: Int, up: Boolean) {
        val tags = FraisData.tags
        val index = tags.indexOfFirst { it.id == tagId }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in tags.indices) {
            val temp = tags[index].order
            tags[index].order = tags[targetIndex].order
            tags[targetIndex].order = temp
            FraisData.saveTags()
            tags.sortBy { it.order }
            refresh()
        }
    }

    fun toggleTagSelection(tagId: Int) {
        _uiState.update { state ->
            val newSelectedFilters =
                if (tagId in state.selectedFilters) {
                    emptySet()
                } else {
                    setOf(tagId)
                }
            FraisData.lastSelectedTag = newSelectedFilters.firstOrNull() ?: 0
            state.copy(selectedFilters = newSelectedFilters)
        }
    }

    fun setTagAreaCollapsed(collapsed: Boolean) {
        FraisData.homeTagsCollapsed = collapsed
        _uiState.update {
            it.copy(isTagAreaCollapsed = collapsed)
        }
    }

    fun triggerTransientGlyph(state: GlyphState) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(transientGlyphState = state)
            }
            delay(2000)
            _uiState.update {
                it.copy(transientGlyphState = null)
            }
        }
    }

    fun launchApp(packageName: String, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appInfo = allAppsCached.find { it.packageName == packageName } ?: return@launch
            if (!appInfo.isLaunchable) {
                withContext(Dispatchers.Main) {
                    com.khaled.frais.utils.HUI.startActivity(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        com.khaled.frais.utils.HPackages.packageUri(packageName)
                    )
                }
                return@launch
            }

            if (appInfo.state == AppInfo.State.FROZEN) {
                val unfrozen = com.khaled.frais.app.AppManager.setAppFrozen(packageName, false)
                if (!unfrozen) {
                    withContext(Dispatchers.Main) {
                        com.khaled.frais.utils.HUI.showToast(
                            app.getString(com.khaled.frais.R.string.operation_failed, appInfo.name)
                        )
                    }
                    return@launch
                }
                appInfo.applicationInfo?.enabled = true
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

            if (appInfo.preventNetwork) {
                com.khaled.frais.utils.HShizuku.setAppNetworkAllowed(packageName, false)
            }

            withContext(Dispatchers.Main) {
                var intent: Intent? = null
                val startTime = System.currentTimeMillis()
                while (intent == null && System.currentTimeMillis() - startTime < 1000) {
                    intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent == null) delay(50)
                }
                intent?.let { 
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(it) 
                } ?: com.khaled.frais.utils.HUI.showToast(com.khaled.frais.R.string.activity_not_found)
            }
        }
    }

    fun setAppFrozen(app: AppInfo, frozen: Boolean, onResult: (success: Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFreezing = true) }
            val success = com.khaled.frais.app.AppManager.setAppFrozen(app.packageName, frozen)
            if (success) refresh()
            _uiState.update { it.copy(isFreezing = false) }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun setAppsFrozen(apps: List<AppInfo>, frozen: Boolean, onResult: (result: String?) -> Unit = {}) {
        if (apps.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFreezing = true) }
            val result = com.khaled.frais.app.AppManager.setListFrozen(frozen, *apps.toTypedArray())
            refresh()
            _uiState.update { it.copy(isFreezing = false) }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun toggleSmartClassification(enabled: Boolean) {
        FraisData.smartClassification = enabled
        refresh()
    }

    fun toggleShowSystemApps(show: Boolean) {
        FraisData.showSystemApps = show
        refresh()
    }
}
