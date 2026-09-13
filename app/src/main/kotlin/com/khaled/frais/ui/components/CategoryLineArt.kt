package com.khaled.frais.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CategoryLineArt(
    category: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) // Increased opacity
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val path = Path()
        val cat = category?.lowercase() ?: ""
        
        when {
            cat.contains("social") || cat.contains("communication") || cat.contains("message") || cat.contains("chat") -> {
                // Circular chat bubbles pattern
                path.moveTo(w * 0.2f, h * 0.5f)
                path.quadraticTo(w * 0.2f, h * 0.3f, w * 0.5f, h * 0.3f)
                path.quadraticTo(w * 0.8f, h * 0.3f, w * 0.8f, h * 0.5f)
                path.quadraticTo(w * 0.8f, h * 0.7f, w * 0.5f, h * 0.7f)
                path.lineTo(w * 0.3f, h * 0.85f)
                path.lineTo(w * 0.4f, h * 0.7f)
                path.lineTo(w * 0.2f, h * 0.5f)
            }
            cat.contains("media") || cat.contains("video") || cat.contains("entertainment") || cat.contains("movie") || cat.contains("music") -> {
                // Play button / film strip motif
                path.moveTo(w * 0.35f, h * 0.3f)
                path.lineTo(w * 0.75f, h * 0.5f)
                path.lineTo(w * 0.35f, h * 0.7f)
                path.close()
                
                path.moveTo(w * 0.1f, h * 0.15f)
                path.lineTo(w * 0.9f, h * 0.15f)
                path.moveTo(w * 0.1f, h * 0.85f)
                path.lineTo(w * 0.9f, h * 0.85f)
            }
            cat.contains("productivity") || cat.contains("work") || cat.contains("finance") || cat.contains("money") || cat.contains("office") -> {
                // Grid / chart lines
                path.moveTo(w * 0.2f, h * 0.25f)
                path.lineTo(w * 0.8f, h * 0.25f)
                path.moveTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.8f, h * 0.5f)
                path.moveTo(w * 0.2f, h * 0.75f)
                path.lineTo(w * 0.8f, h * 0.75f)
                
                path.moveTo(w * 0.25f, h * 0.8f)
                path.lineTo(w * 0.45f, h * 0.4f)
                path.lineTo(w * 0.65f, h * 0.6f)
                path.lineTo(w * 0.85f, h * 0.2f)
            }
            cat.contains("photography") || cat.contains("camera") || cat.contains("image") || cat.contains("photo") -> {
                // Aperture / Camera lens look
                path.addOval(androidx.compose.ui.geometry.Rect(w * 0.25f, h * 0.25f, w * 0.75f, h * 0.75f))
                for (i in 0..5) {
                    val angle = (i * 60).toDouble()
                    val rad = Math.toRadians(angle)
                    path.moveTo(
                        (w * 0.5f + Math.cos(rad) * w * 0.25f).toFloat(),
                        (h * 0.5f + Math.sin(rad) * h * 0.25f).toFloat()
                    )
                    path.lineTo(
                        (w * 0.5f + Math.cos(rad + 0.4) * w * 0.4f).toFloat(),
                        (h * 0.5f + Math.sin(rad + 0.4) * h * 0.4f).toFloat()
                    )
                }
            }
            cat.contains("shopping") || cat.contains("lifestyle") || cat.contains("buy") || cat.contains("store") -> {
                // Bag / Basket outline
                path.moveTo(w * 0.3f, h * 0.4f)
                path.lineTo(w * 0.7f, h * 0.4f)
                path.lineTo(w * 0.75f, h * 0.8f)
                path.lineTo(w * 0.25f, h * 0.8f)
                path.close()
                path.moveTo(w * 0.4f, h * 0.4f)
                path.quadraticTo(w * 0.5f, h * 0.15f, w * 0.6f, h * 0.4f)
            }
            cat.contains("game") || cat.contains("gaming") || cat.contains("play") -> {
                // Controller DPAD / Buttons cross
                path.moveTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.4f, h * 0.5f)
                path.moveTo(w * 0.3f, h * 0.4f)
                path.lineTo(w * 0.3f, h * 0.6f)
                
                path.moveTo(w * 0.65f, h * 0.45f)
                path.lineTo(w * 0.85f, h * 0.65f)
                path.moveTo(w * 0.85f, h * 0.45f)
                path.lineTo(w * 0.65f, h * 0.65f)
            }
            cat.contains("tools") || cat.contains("dev") || cat.contains("system") || cat.contains("browser") || cat.contains("utility") -> {
                // Cog / Code brackets
                path.moveTo(w * 0.3f, h * 0.35f)
                path.lineTo(w * 0.15f, h * 0.5f)
                path.lineTo(w * 0.3f, h * 0.65f)
                
                path.moveTo(w * 0.7f, h * 0.35f)
                path.lineTo(w * 0.85f, h * 0.5f)
                path.lineTo(w * 0.7f, h * 0.65f)
                
                path.moveTo(w * 0.6f, h * 0.2f)
                path.lineTo(w * 0.4f, h * 0.8f)
            }
            cat.contains("travel") || cat.contains("map") || cat.contains("navigation") || cat.contains("transport") -> {
                // Compass / Map marker look
                path.moveTo(w * 0.5f, h * 0.15f)
                path.lineTo(w * 0.75f, h * 0.5f)
                path.lineTo(w * 0.5f, h * 0.85f)
                path.lineTo(w * 0.25f, h * 0.5f)
                path.close()
                path.moveTo(w * 0.15f, h * 0.5f)
                path.lineTo(w * 0.85f, h * 0.5f)
                path.moveTo(w * 0.5f, h * 0.15f)
                path.lineTo(w * 0.5f, h * 0.85f)
            }
            cat.contains("health") || cat.contains("fitness") || cat.contains("heart") || cat.contains("medical") -> {
                // Heart / Pulse line
                path.moveTo(w * 0.1f, h * 0.6f)
                path.lineTo(w * 0.3f, h * 0.6f)
                path.lineTo(w * 0.4f, h * 0.2f)
                path.lineTo(w * 0.5f, h * 0.8f)
                path.lineTo(w * 0.6f, h * 0.5f)
                path.lineTo(w * 0.9f, h * 0.5f)
            }
            cat.contains("most used") -> {
                // Flame / Fire motif
                path.moveTo(w * 0.5f, h * 0.1f)
                path.quadraticTo(w * 0.8f, h * 0.4f, w * 0.7f, h * 0.7f)
                path.quadraticTo(w * 0.5f, h * 0.95f, w * 0.3f, h * 0.7f)
                path.quadraticTo(w * 0.2f, h * 0.4f, w * 0.5f, h * 0.1f)
                path.moveTo(w * 0.4f, h * 0.5f)
                path.quadraticTo(w * 0.5f, h * 0.6f, w * 0.6f, h * 0.5f)
            }
            else -> {
                // Geodesic / network nodes
                path.moveTo(w * 0.2f, h * 0.2f)
                path.lineTo(w * 0.4f, h * 0.35f)
                path.lineTo(w * 0.3f, h * 0.7f)
                path.lineTo(w * 0.75f, h * 0.8f)
                path.lineTo(w * 0.8f, h * 0.25f)
                path.close()
                path.moveTo(w * 0.2f, h * 0.2f)
                path.lineTo(w * 0.3f, h * 0.7f)
                path.moveTo(w * 0.4f, h * 0.35f)
                path.lineTo(w * 0.8f, h * 0.25f)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
