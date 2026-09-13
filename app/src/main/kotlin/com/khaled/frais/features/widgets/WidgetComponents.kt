package com.khaled.frais.features.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.theme.NothingRed

@Composable
fun WidgetStack(
    widgets: List<FraisData.WidgetMetadata>,
    onRemoveWidget: (Int) -> Unit,
    onResizeWidget: (Int, Int) -> Unit = { _, _ -> },
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
                onRemove = { onRemoveWidget(widget.appWidgetId) },
                onResize = { height -> onResizeWidget(widget.appWidgetId, height) }
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
    onRemove: () -> Unit,
    onResize: (Int) -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showOptions by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("WIDGET OPTIONS", style = MaterialTheme.typography.labelMedium) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("RESIZE", style = MaterialTheme.typography.labelSmall) },
                        leadingContent = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = Color.Transparent) }, // Spacer
                        modifier = Modifier.clickable {
                            isResizing = true
                            showOptions = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = NothingRed) },
                        leadingContent = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = NothingRed) },
                        modifier = Modifier.clickable {
                            onRemove()
                            showOptions = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptions = false }) {
                    Text("CLOSE", style = MaterialTheme.typography.labelSmall)
                }
            },
            shape = MaterialTheme.shapes.extraSmall
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        FraisWidgetContainer(
            onLongClick = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                showOptions = true
            },
            modifier = Modifier.then(
                if (isResizing) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                else Modifier
            )
        ) {
            AndroidWidgetHostViewWrapper(widget)
        }

        if (isResizing) {
            // Resize Handle at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .offset(y = 12.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val currentHeightDp = if (widget.height > 0) widget.height else 100
                                val newHeight = (currentHeightDp + (dragAmount / density.density).toInt()).coerceAtLeast(40)
                                onResize(newHeight)
                            },
                            onDragEnd = {
                                isResizing = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(40.dp, 8.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}

@Composable
fun AndroidWidgetHostViewWrapper(widget: FraisData.WidgetMetadata) {
    val providerInfo = remember(widget.appWidgetId) {
        WidgetManager.getAppWidgetInfo(widget.appWidgetId)
    }

    if (providerInfo != null) {
        val density = LocalDensity.current
        val heightModifier = if (widget.height > 0) {
            Modifier.height(widget.height.dp)
        } else {
            Modifier.wrapContentHeight()
        }

        AndroidView(
            factory = { context ->
                WidgetManager.createView(context, widget.appWidgetId, providerInfo).apply {
                    setAppWidget(widget.appWidgetId, providerInfo)
                }
            },
            modifier = Modifier.fillMaxWidth().then(heightModifier),
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
