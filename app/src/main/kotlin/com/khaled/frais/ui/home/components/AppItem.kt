package com.khaled.frais.ui.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.theme.NothingRed

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
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                    .background(NothingRed, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                            )
                        }
                    }
                }
            }

            if (app.preventNetwork) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.TopStart).size(iconSize.div(3.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.WifiOff,
                            null,
                            tint = Color.White,
                            modifier = Modifier.padding(2.dp)
                        )
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
