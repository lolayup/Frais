package com.khaled.frais.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.ui.theme.NothingRed
import kotlinx.coroutines.delay

@Composable
fun LoadingOverlay(
    isVisible: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = alpha))
                .nothingNoise(0.1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var loadingPattern by remember { mutableStateOf(GlyphEngine.Patterns.SCAN_1) }
                
                LaunchedEffect(Unit) {
                    var count = 0
                    while (true) {
                        loadingPattern = when (count % 4) {
                            0 -> GlyphEngine.Patterns.SCAN_1
                            1 -> GlyphEngine.Patterns.SCAN_2
                            2 -> GlyphEngine.Patterns.EYES_OPEN
                            else -> GlyphEngine.Patterns.EYES_CLOSED
                        }
                        count++
                        delay(250)
                    }
                }

                GlyphMatrix(
                    modifier = Modifier.size(140.dp),
                    pattern = loadingPattern,
                    isPulsing = true,
                    accentColor = Color.White
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "FRAIS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    color = Color.White.copy(alpha = alpha)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val infiniteTransition = rememberInfiniteTransition(label = "loadingText")
                val textAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "textAlpha"
                )
                
                Text(
                    text = "INITIALIZING CORE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = NothingRed.copy(alpha = alpha * textAlpha),
                    modifier = Modifier.offset(y = ((1f - alpha) * 20).dp)
                )
            }
        }
    }
}
