package com.khaled.frais.ui

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.ui.games.GamesLauncherScreen
import com.khaled.frais.ui.home.HomeScreen
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.private.PrivateSpaceScreen
import com.khaled.frais.features.widgets.WidgetPage
import com.khaled.frais.ui.settings.SettingsScreen
import com.khaled.frais.ui.theme.AppTheme
import com.khaled.frais.features.activity.ActiveAppViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.khaled.frais.utils.HUI

import com.khaled.frais.ui.components.nothingNoise
import com.khaled.frais.ui.components.PlatformBackdrop
import com.khaled.frais.ui.components.layerBackdrop
import com.khaled.frais.ui.components.liquidGlass
import com.khaled.frais.ui.components.rememberBackdrop
import com.khaled.frais.ui.components.LoadingOverlay

import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.GlyphLiveWidget
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.home.components.AppOptionsDialog
import com.khaled.frais.ui.home.components.GroupFloatingWidget
import com.khaled.frais.ui.home.viewmodel.GridItem
import com.khaled.frais.utils.HIcon
import com.khaled.frais.utils.HPackages
import android.provider.Settings
import androidx.compose.ui.unit.sp
import me.zhanghai.compose.preference.rememberPreferenceState

enum class SettingsState { Closed, Compact, Expanded }
enum class NavigationMode { Private, Widgets }

@Composable
fun FraisMainUI(
    homeViewModel: HomeViewModel = viewModel(),
    activeAppViewModel: ActiveAppViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val isPrivateSpaceAuthenticated by homeViewModel.isPrivateSpaceAuthenticated.collectAsState()

    DisposableEffect(Unit) {
        val permissionListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, _ ->
            homeViewModel.refresh(force = false)
        }
        val binderReceivedListener = rikka.shizuku.Shizuku.OnBinderReceivedListener {
            homeViewModel.refresh(force = false)
        }
        val binderDeadListener = rikka.shizuku.Shizuku.OnBinderDeadListener {
            homeViewModel.refresh(force = false)
        }

        rikka.shizuku.Shizuku.addRequestPermissionResultListener(permissionListener)
        rikka.shizuku.Shizuku.addBinderReceivedListener(binderReceivedListener)
        rikka.shizuku.Shizuku.addBinderDeadListener(binderDeadListener)

        val packageReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                homeViewModel.refresh(force = true)
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addAction(android.content.Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)

        onDispose {
            rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permissionListener)
            rikka.shizuku.Shizuku.removeBinderReceivedListener(binderReceivedListener)
            rikka.shizuku.Shizuku.removeBinderDeadListener(binderDeadListener)
            context.unregisterReceiver(packageReceiver)
        }
    }

    val appTheme by me.zhanghai.compose.preference.rememberPreferenceState(
        FraisData.APP_THEME, 
        FraisData.THEME_AMOLED
    )

    val grainIntensity by rememberPreferenceState(
        FraisData.GRAIN_INTENSITY,
        0.1f
    )

    val screens = listOf(
        Screen.Home,
        Screen.PrivateSpace,
        Screen.Games
    )

    val isSearchActive by homeViewModel.isSearchActive.collectAsState()
    val activeScreenIndex by homeViewModel.activeScreenIndex.collectAsState()
    val navigationMode by homeViewModel.navigationMode.collectAsState()
    val selectedGroup by homeViewModel.selectedGroup.collectAsState()
    val selectedAppForDialog by homeViewModel.selectedAppForDialog.collectAsState()

    var settingsState by remember { mutableStateOf(SettingsState.Closed) }
    
    // Dynamic Icon Logic
    LaunchedEffect(uiState.actionableAppsCount, isPrivateSpaceAuthenticated) {
        val newState = when {
            isPrivateSpaceAuthenticated -> HIcon.IconState.SECURED
            uiState.actionableAppsCount > 0 -> HIcon.IconState.AWAKE
            else -> HIcon.IconState.SLEEP
        }
        HIcon.setPendingIconState(newState)
    }

    // Clear search query when search is deactivated
    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            homeViewModel.setSearchQuery("")
        }
    }
    
    val pagerState = rememberPagerState(
        initialPage = activeScreenIndex,
        pageCount = { screens.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        homeViewModel.setActiveScreenIndex(pagerState.currentPage)
    }

    // Handle Go Home Event (System Home Button / Swipe Up)
    LaunchedEffect(Unit) {
        homeViewModel.goHomeEvent.collect {
            // Close all overlays when Home is triggered
            if (selectedAppForDialog != null) homeViewModel.setSelectedAppForDialog(null)
            if (selectedGroup != null) homeViewModel.setSelectedGroup(null)
            if (isSearchActive) homeViewModel.setSearchActive(false)
            if (settingsState != SettingsState.Closed) settingsState = SettingsState.Closed
            
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // Global Back Handling to prevent reinitialization
    BackHandler(enabled = true) {
        when {
            selectedAppForDialog != null -> homeViewModel.setSelectedAppForDialog(null)
            selectedGroup != null -> homeViewModel.setSelectedGroup(null)
            isSearchActive -> homeViewModel.setSearchActive(false)
            settingsState == SettingsState.Expanded -> settingsState = SettingsState.Compact
            settingsState == SettingsState.Compact -> settingsState = SettingsState.Closed
            pagerState.currentPage != 0 -> {
                coroutineScope.launch { pagerState.animateScrollToPage(0) }
            }
            else -> {
                // Do nothing on the home screen to prevent exiting or task switching
                // The system will handle this as a "stay on home" action
            }
        }
    }

    val currentScreen = screens[pagerState.currentPage]
    val backdrop = rememberBackdrop()

    AppTheme(appTheme = appTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. BACKDROP SOURCE: Record the background area (including Scaffold content)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                Scaffold(
                    modifier = Modifier.nothingNoise(grainIntensity),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    // Top-level Glyph
                                    GlyphLiveWidget(
                                        isLoading = uiState.isLoading,
                                        isFreezing = uiState.isFreezing,
                                        transientState = uiState.transientGlyphState,
                                        currentFilter = when {
                                            currentScreen == Screen.Home && uiState.searchQuery.isNotEmpty() -> "SEARCH RESULTS"
                                            currentScreen == Screen.Home -> uiState.filters.find { it.filter.id in uiState.selectedFilters }?.filter?.name
                                            currentScreen == Screen.Games -> "GAMING HUB"
                                            currentScreen == Screen.PrivateSpace && navigationMode == NavigationMode.Widgets -> "WIDGET HUB"
                                            currentScreen == Screen.PrivateSpace -> "SECURE AREA"
                                            else -> "SECURE AREA"
                                        },
                                        appCount = uiState.totalUserAppsCount,
                                        isPrivateAuthenticated = isPrivateSpaceAuthenticated,
                                        filterAppCount = if (currentScreen == Screen.Home) {
                                            uiState.apps.count { app ->
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
                                        } else if (currentScreen == Screen.Games) uiState.games.size else uiState.privateApps.size,
                                        actionableAppsCount = uiState.actionableAppsCount,
                                        actionablePrivateAppsCount = uiState.actionablePrivateAppsCount,
                                        totalAppCount = uiState.totalAppsCount,
                                        totalFilterCount = uiState.filters.size
                                    )
                                }
                                
                                if (currentScreen == Screen.PrivateSpace && isPrivateSpaceAuthenticated) {
                                    IconButton(
                                        onClick = { 
                                            homeViewModel.setPrivateSpaceAuthenticated(false)
                                            HUI.showToast("PRIVATE SPACE LOCKED")
                                        },
                                        modifier = Modifier.padding(end = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Lock",
                                            tint = NothingRed
                                        )
                                    }
                                }
                            }

                            NothingDivider()
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        // Main Content with Pager for swiping
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = true
                        ) { page ->
                            when (screens[page]) {
                                Screen.Home -> HomeScreen(
                                    viewModel = homeViewModel,
                                    activeAppViewModel = activeAppViewModel
                                )
                                Screen.Games -> GamesLauncherScreen(homeViewModel)
                                Screen.PrivateSpace -> {
                                    AnimatedContent(
                                        targetState = navigationMode,
                                        transitionSpec = {
                                            if (targetState == NavigationMode.Widgets) {
                                                slideInVertically { height -> height } + fadeIn() togetherWith
                                                        slideOutVertically { height -> -height } + fadeOut()
                                            } else {
                                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                                        slideOutVertically { height -> height } + fadeOut()
                                            }.using(SizeTransform(clip = false))
                                        },
                                        label = "mode_switch"
                                    ) { mode ->
                                        if (mode == NavigationMode.Private) {
                                            PrivateSpaceScreen(homeViewModel)
                                        } else {
                                            WidgetPage()
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // 2. GLASS COMPONENTS: Sit outside/above the backdrop source to avoid recursion

            // Overlay to close active widgets/popups when clicking empty space
            if (settingsState != SettingsState.Closed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (settingsState != SettingsState.Closed) settingsState = SettingsState.Closed
                            }
                        }
                )
            }
            
            // Compact Search Popup (Emerging from bottom)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
                    .zIndex(1f)
            ) {
                SearchPopup(
                    viewModel = homeViewModel,
                    onClose = { homeViewModel.setSearchActive(false) }
                )
            }

            // Automatically unlock Secured Area if a secured app is active
            LaunchedEffect(uiState.actionablePrivateAppsCount) {
                if (uiState.actionablePrivateAppsCount > 0) {
                    homeViewModel.setPrivateSpaceAuthenticated(true)
                } else if (isPrivateSpaceAuthenticated) {
                    // Only auto-lock if it was authenticated and now nothing is active
                    homeViewModel.setPrivateSpaceAuthenticated(false)
                }
            }


            // Settings Sheet (Moved outside Scaffold to allow full-screen expansion)
            SettingsSheet(
                state = settingsState,
                viewModel = homeViewModel,
                onStateChange = { settingsState = it }
            )

            // Redesigned Compact Dock (Bottom-aligned)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                CompactDock(
                    currentScreen = currentScreen,
                    navigationMode = navigationMode,
                    onNavigationModeChange = { homeViewModel.setNavigationMode(it) },
                    onScreenSelect = { screen ->
                        coroutineScope.launch {
                            val targetScreen = if (screen == Screen.Widgets) Screen.PrivateSpace else screen
                            pagerState.animateScrollToPage(screens.indexOf(targetScreen))
                        }
                    },
                    onSettingsClick = { settingsState = SettingsState.Compact },
                    onSearchClick = { 
                        homeViewModel.setSearchActive(!isSearchActive)
                    },
                    viewModel = homeViewModel,
                    isPrivateAuthenticated = isPrivateSpaceAuthenticated,
                    backdrop = backdrop
                )
            }

            // Full-screen Loading Overlay
            LoadingOverlay(isVisible = uiState.isInitialLoad)

            val iconSizePref by rememberPreferenceState(FraisData.ICON_SIZE, "64")
            val iconSize = (iconSizePref.toFloatOrNull() ?: 64f).dp
            val showLabels by rememberPreferenceState(FraisData.SHOW_LABELS, true)

            AnimatedVisibility(
                visible = selectedGroup != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                selectedGroup?.let { group ->
                    GroupFloatingWidget(
                        title = group.title,
                        apps = group.apps,
                        viewModel = homeViewModel,
                        iconSize = iconSize,
                        showLabels = showLabels,
                        onDismiss = { homeViewModel.setSelectedGroup(null) },
                        onAppLongClick = { homeViewModel.setSelectedAppForDialog(it) }
                    )
                }
            }

            if (selectedAppForDialog != null) {
                AppOptionsDialog(
                    app = selectedAppForDialog!!,
                    viewModel = homeViewModel,
                    onDismiss = { homeViewModel.setSelectedAppForDialog(null) },
                    onUpdate = { homeViewModel.updateFilteredApps() },
                    onFreezeToggle = { app, frozen ->
                        homeViewModel.setSelectedAppForDialog(null)
                        homeViewModel.setAppFrozen(app, frozen) { success ->
                            HUI.showToast(if (success) (if (frozen) "FROZEN ${app.name}" else "UNFROZEN ${app.name}") else "FAILED TO ${if (frozen) "FREEZE" else "UNFREEZE"} ${app.name}")
                        }
                    },
                    onDetails = {
                        HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(it.packageName))
                        homeViewModel.setSelectedAppForDialog(null)
                    }
                )
            }
        }
    }
}

@Composable
fun CompactDock(
    currentScreen: Screen,
    navigationMode: NavigationMode,
    onNavigationModeChange: (NavigationMode) -> Unit,
    onScreenSelect: (Screen) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel,
    isPrivateAuthenticated: Boolean,
    backdrop: PlatformBackdrop
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .liquidGlass(backdrop)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Left Button: Settings
        Surface(
            onClick = onSettingsClick,
            color = Color.Transparent,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Central Pill: Navigation
        Surface(
            color = Color.Transparent,
            shape = CircleShape,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dockSlots = listOf(Screen.Home, Screen.PrivateSpace, Screen.Games)
                val haptics = LocalHapticFeedback.current
                
                dockSlots.forEach { slot ->
                    val isSecondarySlot = slot == Screen.PrivateSpace
                    val isSelected = currentScreen == slot
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "icon_scale"
                    )
                    val jumpOffset by animateDpAsState(
                        targetValue = if (isSelected) (-4).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "icon_jump"
                    )

                    Box(
                        modifier = Modifier
                            .offset(y = jumpOffset)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .pointerInput(isSecondarySlot, navigationMode) {
                                if (isSecondarySlot) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (dragAmount < -15) { // UP
                                                if (navigationMode != NavigationMode.Widgets) {
                                                    onNavigationModeChange(NavigationMode.Widgets)
                                                }
                                                if (!isSelected) onScreenSelect(slot)
                                            } else if (dragAmount > 15) { // DOWN
                                                if (navigationMode != NavigationMode.Private) {
                                                    onNavigationModeChange(NavigationMode.Private)
                                                }
                                                if (!isSelected) onScreenSelect(slot)
                                            }
                                        }
                                    )
                                }
                            }
                            .clickable { onScreenSelect(slot) }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onScreenSelect(slot) },
                                    onLongPress = {
                                        if (isSecondarySlot && navigationMode == NavigationMode.Private) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setPrivateSpaceAuthenticated(false)
                                            HUI.showToast("Private Space Locked")
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = if (isSecondarySlot) navigationMode else slot,
                            transitionSpec = {
                                if (targetState is NavigationMode) {
                                    if (targetState == NavigationMode.Widgets) {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                                slideOutVertically { height -> -height } + fadeOut()
                                    } else {
                                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                                slideOutVertically { height -> height } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "dock_icon_mode"
                        ) { target ->
                            val icon = when (target) {
                                is NavigationMode -> if (target == NavigationMode.Private) {
                                    if (isPrivateAuthenticated) Icons.Default.LockOpen else Icons.Default.Lock
                                } else Icons.Default.Widgets
                                is Screen -> target.icon
                                else -> Icons.Default.Home
                            }
                            
                            val tint = if (isSecondarySlot && target == NavigationMode.Private && isPrivateAuthenticated) {
                                NothingRed
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = tint
                            )
                        }
                    }
                }
            }
        }

        // Right Button: Search
        Surface(
            onClick = onSearchClick,
            color = Color.Transparent,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}


@Composable
fun SearchPopup(
    viewModel: HomeViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResults by remember(uiState.allApps, uiState.searchQuery) {
        derivedStateOf {
            if (uiState.searchQuery.isEmpty()) emptyList<AppInfo>()
            else uiState.allApps.filter { app ->
                app.name.contains(uiState.searchQuery, ignoreCase = true) || 
                app.packageName.contains(uiState.searchQuery, ignoreCase = true)
            }.sortedByDescending { it.usageTime }.take(10)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        kotlinx.coroutines.delay(kotlin.time.Duration.parse("100ms"))
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 600.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (searchResults.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    items(searchResults, key = { it.packageName }) { app ->
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable {
                                    viewModel.launchApp(app.packageName, context)
                                    onClose()
                                }
                        ) {
                            AppIcon(
                                info = app.applicationInfo,
                                size = 48.dp,
                                grayscale = app.state == AppInfo.State.FROZEN
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("SEARCH APPS...") },
                trailingIcon = {
                    IconButton(onClick = { 
                        viewModel.setSearchQuery("")
                        onClose() 
                    }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                shape = MaterialTheme.shapes.extraSmall,
                singleLine = true
            )
        }
    }
}

@Composable
fun SettingsSheet(
    state: SettingsState,
    viewModel: HomeViewModel,
    onStateChange: (SettingsState) -> Unit
) {
    val grainIntensity by rememberPreferenceState(
        FraisData.GRAIN_INTENSITY,
        0.1f
    )
    
    val progress by animateFloatAsState(
        targetValue = when (state) {
            SettingsState.Closed -> 0f
            SettingsState.Compact -> 0.6f
            SettingsState.Expanded -> 1f
        },
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "progress"
    )

    if (state != SettingsState.Closed) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Vertical expansion: bottom to top
                    translationY = (1f - progress) * size.height
                    
                    // Horizontal growth from center (dock area)
                    // At progress 0: width is 40%
                    // At progress 1: width is 100%
                    val scaleX = 0.4f + (progress * 0.6f)
                    this.scaleX = scaleX
                    
                    // Pivot at the bottom center to make it grow from the dock
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .pointerInput(state) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < -20 && state == SettingsState.Compact) {
                            onStateChange(SettingsState.Expanded)
                        } else if (dragAmount.y > 20 && state == SettingsState.Expanded) {
                            onStateChange(SettingsState.Compact)
                        } else if (dragAmount.y > 50 && state == SettingsState.Compact) {
                            onStateChange(SettingsState.Closed)
                        }
                    }
                }
                .zIndex(2f),
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize().nothingNoise(grainIntensity)) {
                SettingsScreen(viewModel = viewModel)
                
                // Pull handle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .alpha(if (progress > 0.95f) 0.5f else 1f) // Fade out when full screen
                )
            }
        }
    }
}
