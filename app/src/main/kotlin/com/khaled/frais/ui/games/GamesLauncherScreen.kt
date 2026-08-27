package com.khaled.frais.ui.games

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.R
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.ui.home.HomeViewModel
import com.khaled.frais.ui.home.AppOptionsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.khaled.frais.utils.HPackages
import com.khaled.frais.utils.HStorage
import com.khaled.frais.utils.HUI
import com.khaled.frais.utils.AppIconCache
import me.zhanghai.compose.preference.rememberPreferenceState

@Composable
fun GamesLauncherScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    var selectedGameForOptions by remember { mutableStateOf<AppInfo?>(null) }

    val grainIntensity by rememberPreferenceState(FraisData.GRAIN_INTENSITY, 0.1f)
    
    val games = uiState.games

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).nothingNoise(grainIntensity)) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
                val step = 40.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(Color.Gray, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(Color.Gray, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    StatsDashboard(uiState)
                }

                val isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0

                val mostPlayed = games.sortedByDescending { it.usageTime }.take(5)
                if (mostPlayed.any { it.usageTime > 0 }) {
                    item {
                        SectionTitle("MOST PLAYED")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(mostPlayed, key = { "most_${it.packageName}" }) { game ->
                                MostPlayedGameCard(
                                    game = game,
                                    onClick = { homeViewModel.launchApp(game.packageName, context) },
                                    onLongClick = { selectedGameForOptions = game },
                                    isGlyphActive = isGlyphActive
                                )
                            }
                        }
                    }
                }

                val favorites = games.filter { it.pinned }
                if (favorites.isNotEmpty()) {
                    item {
                        SectionTitle("FAVORITES")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites, key = { "fav_${it.packageName}" }) { game ->
                                RecentGameCard(
                                    game = game,
                                    onClick = { homeViewModel.launchApp(game.packageName, context) },
                                    onLongClick = { selectedGameForOptions = game },
                                    isGlyphActive = isGlyphActive
                                )
                            }
                        }
                    }
                }

                val recentlyPlayed = games.filter { it.lastUsed > 0 }.sortedByDescending { it.lastUsed }.take(5)
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionTitle("RECENTLY PLAYED")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyPlayed, key = { "recent_${it.packageName}" }) { game ->
                                RecentGameCard(
                                    game = game,
                                    onClick = { homeViewModel.launchApp(game.packageName, context) },
                                    onLongClick = { selectedGameForOptions = game },
                                    isGlyphActive = isGlyphActive
                                )
                            }
                        }
                    }
                }

                item {
                    SectionTitle("GAME LIBRARY")
                }

                if (games.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("NO GAMES DETECTED", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    }
                } else {
                    items(games, key = { it.packageName }) { game ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            GameCard(
                                game = game,
                                maxUsageTime = uiState.favoriteGame?.usageTime ?: 0L,
                                onClick = { homeViewModel.launchApp(game.packageName, context) },
                                onLongClick = { selectedGameForOptions = game },
                                isGlyphActive = isGlyphActive
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedGameForOptions != null) {
        AppOptionsDialog(
            app = selectedGameForOptions!!,
            onDismiss = { selectedGameForOptions = null },
            onUpdate = { homeViewModel.updateFilteredApps() },
            onFreezeToggle = { app, frozen ->
                selectedGameForOptions = null
                homeViewModel.setAppFrozen(app, frozen) { success ->
                    HUI.showToast(
                        if (success) (if (frozen) "FROZEN ${app.name}" else "UNFROZEN ${app.name}")
                        else "FAILED TO ${if (frozen) "FREEZE" else "UNFREEZE"} ${app.name}"
                    )
                }
            },
            onDetails = {
                HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(it.packageName))
                selectedGameForOptions = null
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        NothingDivider()
    }
}

@Composable
fun StatsDashboard(uiState: com.khaled.frais.ui.home.HomeUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "TOTAL PLAY TIME",
                value = formatUsageTime(uiState.totalPlayTime),
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "INSTALLED",
                value = uiState.games.size.toString(),
                icon = Icons.Default.Gamepad,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "FAVORITE",
                value = uiState.favoriteGame?.name?.uppercase() ?: "NONE",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "LAST PLAYED",
                value = uiState.lastPlayedGame?.name?.uppercase() ?: "NONE",
                icon = Icons.Default.History,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    NothingCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentGameCard(game: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isGlyphActive: Boolean = false) {
    val haptics = LocalHapticFeedback.current
    NothingCard(
        modifier = Modifier
            .width(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(
                info = game.applicationInfo,
                size = 52.dp,
                grayscale = game.state == AppInfo.State.FROZEN,
                isWhitelisted = game.isWhitelisted,
                isGlyphActive = isGlyphActive
            )
            Spacer(Modifier.height(8.dp))
            Text(
                game.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MostPlayedGameCard(
    game: AppInfo, 
    onClick: () -> Unit, 
    onLongClick: () -> Unit, 
    isGlyphActive: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    var dominantColor by remember { mutableStateOf(Color.White) }
    
    LaunchedEffect(game.packageName) {
        val info = game.applicationInfo ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val bitmap = AppIconCache.getOrLoadBitmap(context, info, 0, 128)
            val colorInt = AppIconCache.getDominantColor(bitmap, game.packageName)
            dominantColor = Color(colorInt)
        }
    }

    val gradient = remember(game.packageName, dominantColor) {
        val colors = listOf(
            dominantColor, Color(0xFF1A1A1A), Color(0xFF000000)
        )
        Brush.verticalGradient(colors)
    }

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clip(MaterialTheme.shapes.small)
            .background(gradient)
            .rainDrops(color = dominantColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            AppIcon(
                info = game.applicationInfo, 
                size = 48.dp,
                grayscale = game.state == AppInfo.State.FROZEN,
                isGlyphActive = isGlyphActive
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = game.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatUsageTime(game.usageTime),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
            drawLine(Color.White, Offset(0f, 0f), Offset(20.dp.toPx(), 0f), strokeWidth = 2f)
            drawLine(Color.White, Offset(0f, 0f), Offset(0f, 20.dp.toPx()), strokeWidth = 2f)
        }
    }
}

fun formatUsageTime(time: Long): String {
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(time)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(time) % 60
    return if (hours > 0) "${hours}H ${minutes}M" else "${minutes}M"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(game: AppInfo, maxUsageTime: Long, onClick: () -> Unit, onLongClick: () -> Unit, isGlyphActive: Boolean = false) {
    val isFrozen = game.state == AppInfo.State.FROZEN
    val haptics = LocalHapticFeedback.current
    
    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    info = game.applicationInfo,
                    size = 56.dp,
                    grayscale = isFrozen,
                    isWhitelisted = game.isWhitelisted,
                    isGlyphActive = isGlyphActive
                )
                
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                ) {
                    Text(
                        text = game.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (game.lastUsed > 0) {
                            val date = remember(game.lastUsed) {
                                java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(game.lastUsed))
                            }
                            Text(
                                text = "LAST: $date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp
                            )
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        
                        Text(
                            text = HStorage.formatSize(game.storageSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (game.usageTime > 0) {
                val time = remember(game.usageTime) { formatUsageTime(game.usageTime) }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { if (maxUsageTime > 0) game.usageTime.toFloat() / maxUsageTime else 0f },
                        modifier = Modifier.weight(1f).height(2.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
