package com.khaled.frais.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.FraisIllustration
import com.khaled.frais.ui.components.NothingCard
import com.khaled.frais.ui.home.viewmodel.HomeViewModel

@Composable
fun GroupFloatingWidget(
    title: String?,
    apps: List<AppInfo>,
    viewModel: HomeViewModel,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    onDismiss: () -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // Dim background
            .clickable { onDismiss() }
            .zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        // High-contrast surface for the apps
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .clickable(enabled = false) {},
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface, // Solid background
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FraisIllustration(name = title ?: "group", size = 48.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = title?.uppercase() ?: "GROUP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 80.dp),
                    modifier = Modifier.heightIn(max = 400.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            iconSize = iconSize,
                            showLabel = showLabels,
                            onClick = { 
                                viewModel.launchApp(app.packageName, context)
                                onDismiss()
                            },
                            onLongClick = { onAppLongClick(app) },
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
