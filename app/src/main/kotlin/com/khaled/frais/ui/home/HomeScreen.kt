package com.khaled.frais.ui.home

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.features.activity.ActiveAppViewModel
import com.khaled.frais.features.activity.ActiveAppsWidget
import com.khaled.frais.features.widgets.WidgetStack
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.home.components.*
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.utils.*
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.rememberPreferenceState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    activeAppViewModel: ActiveAppViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedAppForDialog by remember { mutableStateOf<AppInfo?>(null) }
    var editingFilter by remember { mutableStateOf<FraisData.Tag?>(null) }

    val gridColumnsPref by rememberPreferenceState(FraisData.GRID_COLUMNS, "4")
    val iconSizePref by rememberPreferenceState(FraisData.ICON_SIZE, "64")
    val showLabelsPref by rememberPreferenceState(FraisData.SHOW_LABELS, true)
    val showFilterLabelsPref by rememberPreferenceState(FraisData.SHOW_FILTER_LABELS, true)
    val spacingTypePref by rememberPreferenceState(FraisData.SPACING_TYPE, "comfortable")
    val grainIntensityPref by rememberPreferenceState(FraisData.GRAIN_INTENSITY, 0.1f)
    var isFavoritesCollapsed by rememberPreferenceState(FraisData.HOME_FAVORITES_COLLAPSED, false)
    var isMostUsedCollapsed by rememberPreferenceState("home_most_used_collapsed", false)
    val showSystemAppsPref by rememberPreferenceState(FraisData.SHOW_SYSTEM_APPS, false)
    val hideFiltersPref by rememberPreferenceState(FraisData.HIDE_FILTERS, false)
    val groupByCategoryPref by rememberPreferenceState(FraisData.GROUP_BY_CATEGORY, false)

    var collapsedCategories by remember { mutableStateOf(FraisData.collapsedCategories) }

    val filteredApps by remember(uiState.apps, uiState.searchQuery, uiState.selectedFilters, uiState.searchSystemFilter, uiState.searchFrozenFilter) {
        derivedStateOf {
            uiState.apps.filter { app ->
                val matchesQuery = if (uiState.searchQuery.isEmpty()) true
                else app.name.contains(uiState.searchQuery, ignoreCase = true) || app.packageName.contains(uiState.searchQuery, ignoreCase = true)
                
                val matchesFilters = if (uiState.searchQuery.isNotEmpty() || uiState.selectedFilters.isEmpty()) true
                else uiState.selectedFilters.any { it in app.tagIds }

                val matchesSystem = when (uiState.searchSystemFilter) {
                    "user" -> !app.isSystemApp
                    "system" -> app.isSystemApp
                    else -> true
                }

                val matchesFrozen = when (uiState.searchFrozenFilter) {
                    "frozen" -> app.state == AppInfo.State.FROZEN
                    "unfrozen" -> app.state == AppInfo.State.UNFROZEN
                    else -> true
                }

                matchesQuery && matchesFilters && matchesSystem && matchesFrozen
            }
        }
    }

    val pinnedApps by remember(uiState.apps, uiState.searchQuery) {
        derivedStateOf {
            uiState.apps.filter { app ->
                app.pinned && (uiState.searchQuery.isEmpty() ||
                        app.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        app.packageName.contains(uiState.searchQuery, ignoreCase = true))
            }
        }
    }
    
    val otherApps by remember(filteredApps, pinnedApps, uiState.selectedFilters, uiState.searchQuery, uiState.mostUsedApps) {
        derivedStateOf {
            val mostUsed = if (uiState.selectedFilters.isEmpty() && uiState.searchQuery.isEmpty()) uiState.mostUsedApps else emptyList()
            filteredApps.filter { it !in pinnedApps && it !in mostUsed }
        }
    }

    val groupedApps by remember(otherApps, uiState.filters, groupByCategoryPref) {
        derivedStateOf {
            if (!groupByCategoryPref) emptyMap<FraisData.Tag, List<AppInfo>>()
            else {
                val groups = mutableMapOf<Int, MutableList<AppInfo>>()
                otherApps.forEach { app ->
                    val tagId = app.tagIds.firstOrNull { it != FraisData.TAG_ID_MOST_USED } ?: FraisData.TAG_ID_OTHER
                    groups.getOrPut(tagId) { mutableListOf() }.add(app)
                }
                
                uiState.filters.map { it.filter }
                    .filter { it.id in groups.keys }
                    .associateWith { groups[it.id]!! }
            }
        }
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(uiState.apps) {
        if (uiState.apps.isNotEmpty() && gridState.firstVisibleItemIndex == 0) {
            gridState.scrollToItem(2)
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().nothingNoise(grainIntensityPref).nothingDots()) {
            val gridColumns = gridColumnsPref.toIntOrNull() ?: 4
            val iconSize = (iconSizePref.toFloatOrNull() ?: 64f).dp
            val showLabels = showLabelsPref
            val itemSpacing = when (spacingTypePref) {
                "compact" -> 2.dp
                "spacious" -> 16.dp
                else -> 8.dp
            }

            var showUsageWarning by remember { mutableStateOf(false) }
            LaunchedEffect(uiState.isUsagePermissionGranted) {
                if (!uiState.isUsagePermissionGranted) {
                    kotlinx.coroutines.delay(1000)
                    showUsageWarning = true
                } else {
                    showUsageWarning = false
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. ACTIVE APPS
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ActiveAppsWidget(
                        homeViewModel = viewModel,
                        activeAppViewModel = activeAppViewModel
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    StatusCards(
                        uiState = uiState,
                        viewModel = viewModel,
                        context = context,
                        showUsageWarning = showUsageWarning
                    )
                }

                if (!hideFiltersPref) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("FILTERS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            
                            if (uiState.filters.isNotEmpty()) {
                                LazyHorizontalStaggeredGrid(
                                    rows = StaggeredGridCells.Fixed(2),
                                    modifier = Modifier.fillMaxWidth().height(88.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalItemSpacing = 8.dp,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.filters.filter { it.filter.isEnabled }, key = { "full_${it.filter.id}" }) { filterWithCount ->
                                        FilterItem(
                                            filterWithCount = filterWithCount,
                                            isSelected = filterWithCount.filter.id in uiState.selectedFilters,
                                            showPulseDot = true,
                                            showLabel = showFilterLabelsPref,
                                            onClick = { viewModel.toggleTagSelection(filterWithCount.filter.id) },
                                            onEdit = { editingFilter = filterWithCount.filter },
                                            onRemove = {
                                                FraisData.deleteTag(filterWithCount.filter.id)
                                                viewModel.refresh()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (pinnedApps.isEmpty() && otherApps.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                Text(text = "NO APPLICATIONS FOUND", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    if (pinnedApps.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CategoryHeader(
                                title = "FAVORITES (${pinnedApps.size})",
                                isCollapsed = isFavoritesCollapsed,
                                onToggle = { isFavoritesCollapsed = !isFavoritesCollapsed }
                            )
                        }
                        
                        if (!isFavoritesCollapsed) {
                            items(pinnedApps, key = { "pinned_${it.packageName}" }) { app ->
                                AppItem(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = showLabels,
                                    onClick = { viewModel.launchApp(app.packageName, context) },
                                    onLongClick = { selectedAppForDialog = app },
                                    labelColor = NothingRed,
                                    isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
                        }
                    }

                    if (uiState.selectedFilters.isEmpty() && uiState.searchQuery.isEmpty() && uiState.mostUsedApps.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CategoryHeader(
                                title = "MOST USED",
                                isCollapsed = isMostUsedCollapsed,
                                onToggle = { isMostUsedCollapsed = !isMostUsedCollapsed }
                            )
                        }

                        if (!isMostUsedCollapsed) {
                            items(uiState.mostUsedApps, key = { "most_${it.packageName}" }) { app ->
                                AppItem(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = showLabels,
                                    onClick = { viewModel.launchApp(app.packageName, context) },
                                    onLongClick = { selectedAppForDialog = app },
                                    isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
                        }
                    }

                    if (otherApps.isNotEmpty()) {
                        if (groupByCategoryPref && uiState.selectedFilters.isEmpty() && uiState.searchQuery.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isShowingSystem = uiState.searchSystemFilter == "all"
                                    IconButton(
                                        onClick = { viewModel.toggleShowSystemApps(!isShowingSystem) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isShowingSystem) Icons.Default.Dns else Icons.Default.Circle,
                                            contentDescription = "System Apps",
                                            tint = if (isShowingSystem) NothingRed else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            groupedApps.forEach { (tag, apps) ->
                                val tagIdStr = tag.id.toString()
                                val isCollapsed = tagIdStr in collapsedCategories

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    CategoryHeader(
                                        title = "${tag.icon} ${tag.name}",
                                        isCollapsed = isCollapsed,
                                        onToggle = {
                                            val newSet = collapsedCategories.toMutableSet()
                                            if (isCollapsed) newSet.remove(tagIdStr) else newSet.add(tagIdStr)
                                            collapsedCategories = newSet
                                            FraisData.collapsedCategories = newSet
                                        }
                                    )
                                }
                                
                                if (!isCollapsed) {
                                    items(apps, key = { "${tag.id}_${it.packageName}" }) { app ->
                                        AppItem(
                                            app = app,
                                            iconSize = iconSize,
                                            showLabel = showLabels,
                                            onClick = { viewModel.launchApp(app.packageName, context) },
                                            onLongClick = { selectedAppForDialog = app },
                                            isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                        )
                                    }
                                }
                                
                                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val activeFilterNames = uiState.filters.filter { it.filter.id in uiState.selectedFilters }.map { it.filter.name.uppercase() }
                                val title = if (activeFilterNames.isEmpty()) "MAIN APPLICATIONS" else activeFilterNames.joinToString(" + ")
                                val isShowingSystem = uiState.searchSystemFilter == "all"
                                
                                CategoryHeader(
                                    title = title,
                                    isCollapsed = false,
                                    onToggle = {}, // Header is static in non-grouped mode for now
                                    showSystemToggle = true,
                                    isShowingSystem = isShowingSystem,
                                    onSystemToggle = { viewModel.toggleShowSystemApps(!isShowingSystem) }
                                )
                            }

                            items(otherApps, key = { it.packageName }) { app ->
                                AppItem(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = showLabels,
                                    onClick = { viewModel.launchApp(app.packageName, context) },
                                    onLongClick = { selectedAppForDialog = app },
                                    isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                )
                            }
                        }
                    }
                }
                
                // 3. OTHER EXISTING WIDGETS
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val widgets by com.khaled.frais.features.widgets.WidgetManager.widgetsState.collectAsState()
                    if (widgets.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 32.dp)) {
                            NothingSectionHeader(
                                text = "OTHER WIDGETS",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            WidgetStack(
                                widgets = widgets,
                                onRemoveWidget = { com.khaled.frais.features.widgets.WidgetManager.deleteAppWidgetId(it) },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 1000.dp)
                            )
                        }
                    }
                }
                
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(32.dp)) }
            }
        }

        if (editingFilter != null) {
            FilterEditDialog(
                filter = editingFilter!!,
                apps = uiState.apps,
                viewModel = viewModel,
                onDismiss = { editingFilter = null; viewModel.refresh(force = false) }
            )
        }

        if (selectedAppForDialog != null) {
            AppOptionsDialog(
                app = selectedAppForDialog!!,
                viewModel = viewModel,
                onDismiss = { selectedAppForDialog = null },
                onUpdate = { viewModel.updateFilteredApps() },
                onFreezeToggle = { app, frozen ->
                    selectedAppForDialog = null
                    viewModel.setAppFrozen(app, frozen) { success ->
                        HUI.showToast(if (success) (if (frozen) "FROZEN ${app.name}" else "UNFROZEN ${app.name}") else "FAILED TO ${if (frozen) "FREEZE" else "UNFREEZE"} ${app.name}")
                    }
                },
                onDetails = {
                    HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(it.packageName))
                    selectedAppForDialog = null
                }
            )
        }
    }
}
