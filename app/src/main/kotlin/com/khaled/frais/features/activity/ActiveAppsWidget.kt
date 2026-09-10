package com.khaled.frais.features.activity

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.components.NothingSectionHeader
import com.khaled.frais.ui.home.HomeViewModel

@Composable
fun ActiveAppsWidget(
    homeViewModel: HomeViewModel,
    activeAppViewModel: ActiveAppViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val activeApps by activeAppViewModel.activeApps.collectAsState()
    val context = LocalContext.current

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
            NothingSectionHeader(
                text = "ACTIVE APPS",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeApps, key = { it.appInfo.packageName }) { activeApp ->
                    ActiveAppWindow(
                        activeApp = activeApp,
                        onClick = { homeViewModel.launchApp(activeApp.appInfo.packageName, context) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveAppWindow(
    activeApp: ActiveApp,
    onClick: () -> Unit
) {
    val backgroundColor = activeApp.dominantColor
    val isLight = backgroundColor.luminance() > 0.5f
    val contentColor = if (isLight) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(100.dp)
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, backgroundColor.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Window-like background decoration (subtle)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor.copy(alpha = 0.1f))
                )
                
                AppIcon(
                    info = activeApp.appInfo.applicationInfo,
                    size = 48.dp
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = activeApp.appInfo.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
