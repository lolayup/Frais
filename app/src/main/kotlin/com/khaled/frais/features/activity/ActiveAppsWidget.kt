package com.khaled.frais.features.activity

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DragValue { Center, Dismissed }

@Composable
fun ActiveAppsWidget(
    homeViewModel: HomeViewModel,
    activeAppViewModel: ActiveAppViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val activeApps by activeAppViewModel.activeApps.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showProtectedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.allApps) {
        if (uiState.allApps.isNotEmpty()) {
            activeAppViewModel.startMonitoring(uiState.allApps)
        }
    }

    AnimatedVisibility(
        visible = activeApps.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ACTIVITIES",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Surface(
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            val protected = FraisData.closeAllProtectedApps
                            val toClose = activeApps
                                .map { it.appInfo }
                                .filter { it.packageName !in protected }
                            
                            if (toClose.isNotEmpty()) {
                                homeViewModel.setAppsFrozen(toClose, true)
                            }
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showProtectedDialog = true
                        }
                    ),
                    color = NothingRed.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = NothingRed)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "CLOSE ALL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                }
            }
            
            com.khaled.frais.ui.components.NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeApps, key = { it.appInfo.packageName }) { activeApp ->
                    ActiveAppWindow(
                        activeApp = activeApp,
                        onClick = { homeViewModel.launchApp(activeApp.appInfo.packageName, context) },
                        onSwipeUp = { homeViewModel.setAppFrozen(activeApp.appInfo, true) }
                    )
                }
            }
        }
    }

    if (showProtectedDialog) {
        ProtectedAppsDialog(
            allApps = uiState.allApps,
            onDismiss = { showProtectedDialog = false }
        )
    }
}

@Composable
fun ActiveAppWindow(
    activeApp: ActiveApp,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    val backgroundColor = activeApp.dominantColor
    val isLight = backgroundColor.luminance() > 0.5f
    val borderColor = if (isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f)
    val density = LocalDensity.current
    val decaySpec = rememberSplineBasedDecay<Float>()

    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = decaySpec
        )
    }

    SideEffect {
        anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                DragValue.Center at 0f
                DragValue.Dismissed at with(density) { -200.dp.toPx() }
            }
        )
    }

    LaunchedEffect(anchoredDraggableState.currentValue) {
        if (anchoredDraggableState.currentValue == DragValue.Dismissed) {
            onSwipeUp()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(110.dp)
            .offset {
                IntOffset(
                    x = 0,
                    y = anchoredDraggableState.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0
                )
            }
            .anchoredDraggable(
                state = anchoredDraggableState,
                orientation = Orientation.Vertical
            )
            .alpha(
                if (anchoredDraggableState.offset.isNaN()) 1f 
                else (1f - (abs(anchoredDraggableState.offset) / with(density) { 200.dp.toPx() })).coerceIn(0f, 1f)
            )
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f),
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColor.copy(alpha = 0.3f),
                                    backgroundColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(backgroundColor.copy(alpha = 0.2f))
                        .align(Alignment.TopCenter)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        info = activeApp.appInfo.applicationInfo,
                        size = 48.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = activeApp.appInfo.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ProtectedAppsDialog(
    allApps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    var protectedApps by remember { mutableStateOf(FraisData.closeAllProtectedApps) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allApps, searchQuery) {
        allApps.filter { it.isLaunchable && (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)) }
            .sortedBy { it.name.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PROTECTED FROM CLOSE ALL", style = MaterialTheme.typography.labelMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("SEARCH...", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    singleLine = true
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn {
                        items(filteredApps) { app ->
                            val isProtected = app.packageName in protectedApps
                            ListItem(
                                headlineContent = { Text(app.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                leadingContent = { AppIcon(info = app.applicationInfo, size = 32.dp) },
                                trailingContent = {
                                    Checkbox(
                                        checked = isProtected,
                                        onCheckedChange = { checked ->
                                            val newSet = protectedApps.toMutableSet()
                                            if (checked) newSet.add(app.packageName) else newSet.remove(app.packageName)
                                            protectedApps = newSet
                                            FraisData.closeAllProtectedApps = newSet
                                        }
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val newSet = protectedApps.toMutableSet()
                                    if (!isProtected) newSet.add(app.packageName) else newSet.remove(app.packageName)
                                    protectedApps = newSet
                                    FraisData.closeAllProtectedApps = newSet
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        },
        shape = MaterialTheme.shapes.extraSmall
    )
}
