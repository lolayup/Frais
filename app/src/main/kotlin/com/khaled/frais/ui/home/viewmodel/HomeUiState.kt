package com.khaled.frais.ui.home.viewmodel

import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.GlyphState

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
    val hiddenApps: List<AppInfo> = emptyList(),
    val isInitialLoad: Boolean = true
)
