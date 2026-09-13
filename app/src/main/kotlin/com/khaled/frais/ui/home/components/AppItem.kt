package com.khaled.frais.ui.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.theme.NothingRed
import me.zhanghai.compose.preference.rememberPreferenceState

@Composable
internal fun AppItem(
    app: AppInfo,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    labelColor: Color = Color.Unspecified,
    isGlyphActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isFrozen = app.state == AppInfo.State.FROZEN
    val haptics = LocalHapticFeedback.current
    val wallpaperUri by rememberPreferenceState(FraisData.WALLPAPER_URI, "")

    Column(
        modifier = modifier
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
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(iconSize.div(3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(2.dp)
                    )
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
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = if (wallpaperUri.isNotEmpty()) androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    ) else null
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = if (labelColor == Color.Unspecified && wallpaperUri.isNotEmpty()) Color.White else labelColor
            )
        }
    }
}
