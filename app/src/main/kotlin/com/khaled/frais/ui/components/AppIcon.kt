package com.khaled.frais.ui.components

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khaled.frais.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.khaled.frais.BuildConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color as UiColor

@Composable
fun AppIcon(
    info: ApplicationInfo?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    grayscale: Boolean = false,
    isWhitelisted: Boolean = false,
    isGlyphActive: Boolean = false
) {
    val isFrais = info?.packageName == BuildConfig.APPLICATION_ID
    
    if (isFrais && isGlyphActive) {
        GlyphMatrix(
            modifier = modifier.size(size),
            pattern = GlyphEngine.Patterns.EYES_OPEN,
            secondaryPattern = GlyphEngine.Patterns.ALERT_DOT,
            isPulsing = true
        )
    } else {
        val context = LocalContext.current
        val density = LocalDensity.current
        val pixelSize = with(density) { size.roundToPx() }
        
        var bitmap by remember(info?.packageName, pixelSize) { mutableStateOf<Bitmap?>(null) }
        
        LaunchedEffect(info?.packageName, pixelSize) {
            if (info == null) {
                bitmap = null
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                bitmap = AppIconCache.getOrLoadBitmap(context, info, 0, pixelSize)
            }
        }
        
        // Desaturate AND dim frozen icons so the frozen state is unmistakable at a glance,
        // not just a subtle saturation shift that can look identical to unfrozen for
        // already-monochrome icons.
        val colorFilter = remember(grayscale) {
            if (grayscale) {
                ColorFilter.colorMatrix(ColorMatrix().apply {
                    setToSaturation(0f)
                })
            } else null
        }

        Box(contentAlignment = Alignment.BottomEnd, modifier = modifier.size(size)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (grayscale) 0.6f else 1f),
                    colorFilter = colorFilter
                )
            }

            if (grayscale) {
                Box(
                    modifier = Modifier
                        .size(size / 2.6f)
                        .align(Alignment.TopStart)
                        .background(UiColor.Black.copy(alpha = 0.55f), CircleShape)
                        .padding(size / 22),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = "Frozen",
                        tint = UiColor.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isWhitelisted) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Whitelisted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size / 3)
                )
            }
        }
    }
}
