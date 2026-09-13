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
import androidx.activity.compose.BackHandler
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.features.activity.ActiveAppViewModel
import com.khaled.frais.features.activity.ActiveAppsWidget
import com.khaled.frais.features.widgets.WidgetStack
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.home.components.*
import com.khaled.frais.ui.home.viewmodel.GridItem
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
    var editingFilter by remember { mutableStateOf<FraisData.Tag?>(null) }

    val gridColumnsPref by rememberPreferenceState(FraisData.GRID_COLUMNS, "4")
    val iconSizePref by rememberPreferenceState(FraisData.ICON_SIZE, "64")
    val iconSize = (iconSizePref.toFloatOrNull() ?: 64f).dp
    val showLabelsPref by rememberPreferenceState(FraisData.SHOW_LABELS, true)
    val showFilterLabelsPref by rememberPreferenceState(FraisData.SHOW_FILTER_LABELS, true)
    val spacingTypePref by rememberPreferenceState(FraisData.SPACING_TYPE, "comfortable")
    val grainIntensityPref by rememberPreferenceState(FraisData.GRAIN_INTENSITY, 0.1f)
    var isFavoritesCollapsed by rememberPreferenceState(FraisData.HOME_FAVORITES_COLLAPSED, false)
    var isMostUsedCollapsed by rememberPreferenceState("home_most_used_collapsed", false)

    val gridState = rememberLazyGridState()

    fun editGroup(item: GridItem.Group) {
        val tagId = when {
            item.id.startsWith("pinned_tag_") -> item.id.substringAfter("pinned_tag_").toIntOrNull()
            item.id.startsWith("main_tag_") -> item.id.substringAfter("main_tag_").toIntOrNull()
            item.id.startsWith("tag_") -> item.id.substringAfter("tag_").toIntOrNull()
            else -> null
        }
        
        if (tagId != null) {
            val tag = FraisData.tags.find { it.id == tagId }
            if (tag != null) {
                editingFilter = tag
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().nothingNoise(grainIntensityPref).nothingDots()) {
        val gridColumns = gridColumnsPref.toIntOrNull() ?: 4
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
                bottom = 200.dp
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

                if (uiState.pinnedGridItems.isEmpty() && uiState.mainGridItems.isEmpty()) {
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
                    if (uiState.pinnedGridItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CategoryHeader(
                                title = "FAVORITES (${uiState.pinnedGridItems.size})",
                                isCollapsed = isFavoritesCollapsed,
                                onToggle = { isFavoritesCollapsed = !isFavoritesCollapsed }
                            )
                        }
                        
                        if (!isFavoritesCollapsed) {
                            items(uiState.pinnedGridItems, key = { item ->
                                when (item) {
                                    is GridItem.App -> "pinned_${item.app.packageName}"
                                    is GridItem.Group -> "pinned_group_${item.id}"
                                }
                            }) { item ->
                                when (item) {
                                    is GridItem.App -> {
                                        AppItem(
                                            app = item.app,
                                            iconSize = iconSize,
                                            showLabel = showLabels,
                                            onClick = { viewModel.launchApp(item.app.packageName, context) },
                                            onLongClick = { viewModel.setSelectedAppForDialog(item.app) },
                                            labelColor = NothingRed,
                                            isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                        )
                                    }
                                    is GridItem.Group -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            StackedAppToggle(
                                                items = item.apps.take(3).map { app ->
                                                    @Composable {
                                                        AppIcon(
                                                            info = app.applicationInfo,
                                                            size = iconSize,
                                                            grayscale = app.state == AppInfo.State.FROZEN
                                                        )
                                                    }
                                                },
                                                isExpanded = false,
                                                onToggle = { viewModel.setSelectedGroup(item) },
                                                onLongClick = { editGroup(item) },
                                                size = iconSize,
                                                categoryName = item.title
                                            )
                                            item.title?.let {
                                                Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }
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
                                    onLongClick = { viewModel.setSelectedAppForDialog(app) },
                                    isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
                        }
                    }

                    if (uiState.mainGridItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val activeFilterNames = uiState.filters.filter { it.filter.id in uiState.selectedFilters }.map { it.filter.name.uppercase() }
                            val title = if (activeFilterNames.isEmpty()) "MAIN APPLICATIONS" else activeFilterNames.joinToString(" + ")
                            
                            CategoryHeader(
                                title = title,
                                isCollapsed = false,
                                onToggle = {},
                                showSystemToggle = false,
                                showExpandIcon = false
                            )
                        }

                        items(uiState.mainGridItems, key = { item ->
                            when (item) {
                                is GridItem.App -> "main_${item.app.packageName}"
                                is GridItem.Group -> "main_group_${item.id}"
                            }
                        }) { item ->
                            when (item) {
                                is GridItem.App -> {
                                    AppItem(
                                        app = item.app,
                                        iconSize = iconSize,
                                        showLabel = showLabels,
                                        onClick = { viewModel.launchApp(item.app.packageName, context) },
                                        onLongClick = { viewModel.setSelectedAppForDialog(item.app) },
                                        isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                                    )
                                }
                                is GridItem.Group -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        StackedAppToggle(
                                            items = item.apps.take(3).map { app ->
                                                @Composable {
                                                    AppIcon(
                                                        info = app.applicationInfo,
                                                        size = iconSize,
                                                        grayscale = app.state == AppInfo.State.FROZEN
                                                    )
                                                }
                                            },
                                            isExpanded = false,
                                            onToggle = { viewModel.setSelectedGroup(item) },
                                            onLongClick = { editGroup(item) },
                                            size = iconSize,
                                            categoryName = item.title
                                        )
                                        item.title?.let {
                                            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                        }
                                    }
                                }
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

        if (editingFilter != null) {
            FilterEditDialog(
                filter = editingFilter!!,
                apps = uiState.apps,
                viewModel = viewModel,
                onDismiss = { editingFilter = null; viewModel.refresh(force = false) }
            )
        }
    }
}
