package com.khaled.frais.features.widgets

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.NothingDivider

@Composable
fun WidgetStack(
    widgets: List<FraisData.WidgetMetadata>,
    onRemoveWidget: (Int) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(widgets, key = { it.appWidgetId }) { widget ->
            WidgetContainer(
                widget = widget,
                onRemove = { onRemoveWidget(widget.appWidgetId) }
            )
        }
        
        // Extra scroll space at the bottom
        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun FraisWidgetContainer(
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    }
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(8.dp), content = content)
    }
}

@Composable
fun WidgetContainer(
    widget: FraisData.WidgetMetadata,
    onRemove: () -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("REMOVE WIDGET?", style = MaterialTheme.typography.labelMedium) },
            confirmButton = {
                TextButton(onClick = { 
                    onRemove()
                    showDeleteConfirm = false
                }) {
                    Text("REMOVE", color = com.khaled.frais.ui.theme.NothingRed, style = MaterialTheme.typography.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            },
            shape = MaterialTheme.shapes.extraSmall
        )
    }

    FraisWidgetContainer(
        onLongClick = {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            showDeleteConfirm = true
        }
    ) {
        AndroidWidgetHostViewWrapper(widget)
    }
}

@Composable
fun AndroidWidgetHostViewWrapper(widget: FraisData.WidgetMetadata) {
    val providerInfo = remember(widget.appWidgetId) {
        WidgetManager.getAppWidgetInfo(widget.appWidgetId)
    }

    if (providerInfo != null) {
        AndroidView(
            factory = { context ->
                WidgetManager.createView(context, widget.appWidgetId, providerInfo).apply {
                    setAppWidget(widget.appWidgetId, providerInfo)
                }
            },
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            update = { view ->
                // Ensure the view stays updated
                view.setAppWidget(widget.appWidgetId, providerInfo)
            }
        )
    } else {
        // Fallback for missing provider
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Widget provider not found", style = MaterialTheme.typography.labelSmall)
        }
    }
}
