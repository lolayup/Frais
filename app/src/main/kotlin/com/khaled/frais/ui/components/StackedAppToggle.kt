package com.khaled.frais.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun StackedAppToggle(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    items: List<@Composable () -> Unit>, // Keep for compatibility but ignore in preview
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit = {},
    backdrop: PlatformBackdrop? = null,
    categoryName: String? = null
) {
    val haptics = LocalHapticFeedback.current
    
    Box(
        modifier = modifier
            .size(size + 16.dp)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background Box for the Group
        Box(
            modifier = Modifier
                .size(size + 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            // The colorful illustrated icon taking the full space
            FraisIllustration(
                name = categoryName ?: "other",
                size = size * 0.9f
            )
        }
    }
}
