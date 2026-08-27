package com.khaled.frais.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.R
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.utils.*
import com.khaled.frais.ui.settings.SettingsItem
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.rememberPreferenceState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedAppForDialog by remember { mutableStateOf<AppInfo?>(null) }
    var editingFilter by remember { mutableStateOf<FraisData.Tag?>(null) }

    val gridColumnsPref by rememberPreferenceState(FraisData.GRID_COLUMNS, "4")
    val iconSizePref by rememberPreferenceState(FraisData.ICON_SIZE, "64")
    val showLabelsPref by rememberPreferenceState(FraisData.SHOW_LABELS, true)
    val spacingTypePref by rememberPreferenceState(FraisData.SPACING_TYPE, "comfortable")
    val grainIntensityPref by rememberPreferenceState(FraisData.GRAIN_INTENSITY, 0.1f)
    val showPulseDotPref by rememberPreferenceState(FraisData.SHOW_PULSE_DOT, true)
    var isFavoritesCollapsed by rememberPreferenceState(FraisData.HOME_FAVORITES_COLLAPSED, false)
    var isMostUsedCollapsed by rememberPreferenceState("home_most_used_collapsed", false)
    val showSystemAppsPref by rememberPreferenceState(FraisData.SHOW_SYSTEM_APPS, false)

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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        AnimatedVisibility(visible = !uiState.isServiceRunning) {
                            NothingCard(
                                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 8.dp),
                                onClick = { 
                                    if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                                        if (!uiState.isShizukuPermissionGranted) {
                                            com.khaled.frais.app.AppManager.requestShizukuPermission(1001)
                                        } else {
                                            viewModel.refresh(force = false)
                                        }
                                    } else {
                                        viewModel.refresh(force = false)
                                    }
                                }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, tint = NothingRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU) && !uiState.isShizukuPermissionGranted) {
                                            "SHIZUKU PERMISSION REQUIRED. TAP TO REQUEST."
                                        } else {
                                            "SERVICE DISCONNECTED. TAP TO RETRY."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NothingRed
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showUsageWarning) {
                            NothingCard(
                                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 8.dp),
                                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timeline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("GRANT USAGE ACCESS FOR DATA ANALYSIS.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

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
                                        showPulseDot = showPulseDotPref,
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFavoritesCollapsed = !isFavoritesCollapsed }
                                    .padding(16.dp, 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "FAVORITES (${pinnedApps.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = if (isFavoritesCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                NothingDivider()
                            }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMostUsedCollapsed = !isMostUsedCollapsed }
                                    .padding(16.dp, 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "MOST USED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = if (isMostUsedCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                NothingDivider()
                            }
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
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.padding(16.dp, 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val activeFilterNames = uiState.filters.filter { it.filter.id in uiState.selectedFilters }.map { it.filter.name.uppercase() }
                                    val title = if (activeFilterNames.isEmpty()) "APPLICATIONS" else activeFilterNames.joinToString(" + ")
                                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                    
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
                                Spacer(Modifier.height(4.dp))
                                NothingDivider()
                            }
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

@Composable
private fun FilterItem(
    filterWithCount: FilterWithCount,
    isSelected: Boolean,
    showPulseDot: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val filter = filterWithCount.filter
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showFilterMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showFilterMenu = true
                }
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraSmall,
            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(filter.icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${filter.name.uppercase()} (${filterWithCount.unfrozenCount})",
                    style = MaterialTheme.typography.labelSmall
                )

                if (showPulseDot && filterWithCount.actionableRunningCount > 0) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NothingRed.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.background).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
        ) {
            DropdownMenuItem(
                text = { Text("EDIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                onClick = {
                    showFilterMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
            )
            if (!filter.isBuiltIn) {
                DropdownMenuItem(
                    text = { Text("REMOVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NothingRed) },
                    onClick = {
                        showFilterMenu = false
                        onRemove()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = NothingRed) }
                )
            }
        }
    }
}

@Composable
internal fun AppItem(
    app: AppInfo,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    labelColor: Color = Color.Unspecified,
    isGlyphActive: Boolean = false
) {
    val isFrozen = app.state == AppInfo.State.FROZEN
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 8.dp)
            .alpha(if (isFrozen) 0.5f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIcon(
                info = app.applicationInfo,
                size = iconSize,
                grayscale = isFrozen,
                isWhitelisted = app.isWhitelisted,
                isGlyphActive = isGlyphActive
            )
            
            if (app.isSystemApp) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).size(iconSize.div(3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Shield,
                            null,
                            tint = Color.White,
                            modifier = Modifier.padding(2.dp)
                        )
                        if (!app.isSafeToFreeze) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(NothingRed, androidx.compose.foundation.shape.CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                            )
                        }
                    }
                }
            }
        }
        
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = labelColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppOptionsDialog(
    app: AppInfo,
    viewModel: HomeViewModel = viewModel(),
    onDismiss: () -> Unit,
    onUpdate: () -> Unit = {},
    onFreezeToggle: (AppInfo, Boolean) -> Unit,
    onDetails: (AppInfo) -> Unit
) {
    val frozen = app.state == AppInfo.State.FROZEN
    var showTagPicker by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    info = app.applicationInfo,
                    size = 40.dp,
                    grayscale = frozen,
                    isWhitelisted = app.isWhitelisted
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        text = {
            Column {
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Info Section
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VER: ${HPackages.getVersionName(app.packageName)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                    Text("SIZE: ${HStorage.formatSize(app.storageSize)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
                
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    val buttonModifier = Modifier.weight(1f).height(48.dp)
                    
                    if (!app.isSystemApp || app.isSafeToFreeze) {
                        CompactOptionButton(
                            icon = if (frozen) Icons.Default.PlayArrow else Icons.Default.AcUnit,
                            label = if (frozen) "UNFREEZE" else "FREEZE",
                            color = if (frozen) MaterialTheme.colorScheme.primary else NothingRed,
                            onClick = { onFreezeToggle(app, !frozen) },
                            modifier = buttonModifier
                        )
                    }

                    if (!app.isPrivate) {
                        CompactOptionButton(
                            icon = Icons.Default.PushPin,
                            label = if (app.pinned) "UNPIN" else "PIN",
                            color = if (app.pinned) MaterialTheme.colorScheme.primary else Color.Gray,
                            onClick = { app.pinned = !app.pinned; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )

                        CompactOptionButton(
                            icon = Icons.Default.Shield,
                            label = if (app.whitelisted) "UNTRUST" else "TRUST",
                            color = if (app.whitelisted) MaterialTheme.colorScheme.primary else Color.Gray,
                            onClick = { app.whitelisted = !app.whitelisted; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )

                        CompactOptionButton(
                            icon = Icons.Default.FilterList,
                            label = "FILTERS",
                            onClick = { showTagPicker = true },
                            modifier = buttonModifier
                        )
                    }

                    CompactOptionButton(
                        icon = Icons.Default.SettingsSuggest,
                        label = "CONDITIONS",
                        onClick = { showConditions = true },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = if (app.isPrivate) Icons.Default.Visibility else Icons.Default.Lock,
                        label = if (app.isPrivate) "RESTORE" else "SECURE",
                        color = if (app.isPrivate) MaterialTheme.colorScheme.primary else NothingRed,
                        onClick = { app.isPrivate = !app.isPrivate; onUpdate(); onDismiss() },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = if (app.excludeMostUsed) Icons.Default.HistoryToggleOff else Icons.Default.History,
                        label = if (app.excludeMostUsed) "RESTORE MOST" else "HIDE FROM MOST",
                        color = if (app.excludeMostUsed) NothingRed else Color.Gray,
                        onClick = { app.excludeMostUsed = !app.excludeMostUsed; onUpdate(); onDismiss() },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = Icons.Default.Info,
                        label = "DETAILS",
                        onClick = { onDetails(app) },
                        modifier = buttonModifier
                    )

                    if (app.isSystemApp) {
                        CompactOptionButton(
                            icon = Icons.Default.VerifiedUser,
                            label = if (app.isSafeToFreeze) "REVOKE" else "SAFE",
                            color = if (app.isSafeToFreeze) NothingRed else MaterialTheme.colorScheme.primary,
                            onClick = { app.isSafeToFreeze = !app.isSafeToFreeze; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )
                    }

                    CompactOptionButton(
                        icon = Icons.Default.Refresh,
                        label = "REINSTALL",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismiss()
                            coroutineScope.launch {
                                val success = com.khaled.frais.app.AppManager.reinstallAppFallback(app.packageName, context)
                                if (!success) HUI.showToast("REINSTALL FAILED")
                            }
                        },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = Icons.Default.Delete,
                        label = "UNINSTALL",
                        color = NothingRed,
                        onClick = {
                            showUninstallConfirm = true
                        },
                        modifier = buttonModifier
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )

    if (showUninstallConfirm) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = false },
            title = { Text("UNINSTALL", fontWeight = FontWeight.Bold) },
            text = { Text("ARE YOU SURE YOU WANT TO UNINSTALL '${app.name.uppercase()}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUninstallConfirm = false
                        onDismiss()
                        viewModel.triggerTransientGlyph(GlyphState.UNINSTALLING)
                        com.khaled.frais.app.AppManager.uninstallApp(app.packageName)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NothingRed)
                ) { Text("UNINSTALL") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallConfirm = false }) { Text("CANCEL") }
            },
            shape = MaterialTheme.shapes.extraSmall
        )
    }

    if (showTagPicker) {
        TagPickerDialog(app = app, onDismiss = { showTagPicker = false; onUpdate() })
    }

    if (showConditions) {
        AppConditionsDialog(app = app, onDismiss = { showConditions = false; onUpdate() })
    }
}

@Composable
fun AppConditionsDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text("LAUNCH CONDITIONS", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsItem(
                    title = "MUTE ON LAUNCH",
                    description = "SILENCE MEDIA VOLUME"
                ) {
                    Switch(checked = app.muteOnLaunch, onCheckedChange = { app.muteOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "LOCATION ON LAUNCH",
                    description = "AUTO-ENABLE GPS"
                ) {
                    Switch(checked = app.locationOnLaunch, onCheckedChange = { app.locationOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "DATA ON LAUNCH",
                    description = "AUTO-ENABLE MOBILE DATA"
                ) {
                    Switch(checked = app.dataOnLaunch, onCheckedChange = { app.dataOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "BATTERY SAVER",
                    description = "TOGGLE LOW POWER MODE"
                ) {
                    Switch(checked = app.batterySaverOnLaunch, onCheckedChange = { app.batterySaverOnLaunch = it; onUpdate() })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}

@Composable
fun CompactOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun TagPickerDialog(app: AppInfo, onDismiss: () -> Unit) {
    val filters = FraisData.tags

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text("MANAGE FILTERS", fontWeight = FontWeight.Bold) },
        text = {
            if (filters.isEmpty()) {
                Text("No filters created yet.")
            } else {
                LazyColumn {
                    items(filters) { filter ->
                        var isChecked by remember { mutableStateOf(filter.id == app.manualTagId) }
                        ListItem(
                            headlineContent = { Text(filter.name.uppercase(), style = MaterialTheme.typography.labelMedium) },
                            trailingContent = {
                                RadioButton(
                                    selected = isChecked,
                                    onClick = { 
                                        if (isChecked) app.manualTagId = null
                                        else app.manualTagId = filter.id
                                        onDismiss()
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (isChecked) app.manualTagId = null
                                else app.manualTagId = filter.id
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}

@Composable
fun FilterEditDialog(
    filter: FraisData.Tag,
    apps: List<AppInfo>,
    viewModel: HomeViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(filter.name) }
    var icon by remember { mutableStateOf(filter.icon) }
    var searchQuery by remember { mutableStateOf("") }
    
    var refreshKey by remember { mutableStateOf(0) }
    val appsInFilter = remember(apps, filter.id, refreshKey) {
        apps.filter { filter.id in it.tagIds }
    }
    
    val searchableApps = remember(apps, filter.id, searchQuery, refreshKey) {
        if (searchQuery.length < 2) emptyList()
        else apps.filter { it.name.contains(searchQuery, ignoreCase = true) && filter.id !in it.tagIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EDIT FILTER", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!filter.isBuiltIn) {
                    IconButton(onClick = { 
                        FraisData.deleteTag(filter.id)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, null, tint = NothingRed)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!filter.isBuiltIn) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("NAME") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("ICON (EMOJI)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
                
                NothingDivider()
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ADD APPS...", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    singleLine = true
                )

                if (searchableApps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn {
                            items(searchableApps) { app ->
                                ListItem(
                                    headlineContent = { Text(app.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                    leadingContent = { AppIcon(info = app.applicationInfo, size = 32.dp) },
                                    trailingContent = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable {
                                        app.manualTagId = filter.id
                                        app.excludedTagIds.remove(filter.id)
                                        FraisData.saveApps()
                                        refreshKey++
                                        searchQuery = ""
                                        viewModel.triggerTransientGlyph(GlyphState.ADDING)
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (appsInFilter.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("CURRENT APPS (${appsInFilter.size})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 250.dp)) {
                        LazyColumn {
                            items(appsInFilter) { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(info = app.applicationInfo, size = 32.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = app.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            app.manualTagId = null
                                            if (filter.id !in app.excludedTagIds) {
                                                app.excludedTagIds.add(filter.id)
                                            }
                                            FraisData.saveApps()
                                            refreshKey++
                                            viewModel.triggerTransientGlyph(GlyphState.REMOVING)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = NothingRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!filter.isBuiltIn) {
                    filter.name = name
                    filter.icon = icon
                    FraisData.updateTag(filter)
                }
                onDismiss()
            }) { Text("DONE") }
        }
    )
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, scale: Float = 1f) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(scale)
    )
}
