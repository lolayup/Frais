package com.khaled.frais.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
fun FraisIllustration(
    name: String,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val cat = name.lowercase()
            
            when {
                cat.contains("social") || cat.contains("communication") -> {
                    // Friendly chat bubble - Purple/Blue
                    val mainColor = Color(0xFF6C5CE7)
                    val bubblePath = Path().apply {
                        moveTo(w * 0.2f, h * 0.3f)
                        quadraticTo(w * 0.2f, h * 0.15f, w * 0.4f, h * 0.15f)
                        lineTo(w * 0.7f, h * 0.15f)
                        quadraticTo(w * 0.9f, h * 0.15f, w * 0.9f, h * 0.3f)
                        lineTo(w * 0.9f, h * 0.6f)
                        quadraticTo(w * 0.9f, h * 0.75f, w * 0.7f, h * 0.75f)
                        lineTo(w * 0.5f, h * 0.75f)
                        lineTo(w * 0.3f, h * 0.9f)
                        lineTo(w * 0.35f, h * 0.75f)
                        lineTo(w * 0.2f, h * 0.75f)
                        quadraticTo(w * 0.1f, h * 0.75f, w * 0.1f, h * 0.6f)
                        close()
                    }
                    drawPath(bubblePath, mainColor)
                    drawCircle(Color.White.copy(alpha = 0.4f), radius = w * 0.05f, center = center)
                }
                cat.contains("media") || cat.contains("video") || cat.contains("entertainment") -> {
                    // Movie slate / Play button motif - Red
                    val mainColor = Color(0xFFFF7675)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.6f))
                    val playPath = Path().apply {
                        moveTo(w * 0.4f, h * 0.35f)
                        lineTo(w * 0.65f, h * 0.5f)
                        lineTo(w * 0.4f, h * 0.65f)
                        close()
                    }
                    drawPath(playPath, Color.White)
                }
                cat.contains("productivity") || cat.contains("work") -> {
                    // Stylized checkmark document - Teal
                    val mainColor = Color(0xFF00CEC9)
                    val docPath = Path().apply {
                        moveTo(w * 0.2f, h * 0.1f)
                        lineTo(w * 0.6f, h * 0.1f)
                        lineTo(w * 0.8f, h * 0.3f)
                        lineTo(w * 0.8f, h * 0.9f)
                        lineTo(w * 0.2f, h * 0.9f)
                        close()
                    }
                    drawPath(docPath, mainColor)
                    val checkPath = Path().apply {
                        moveTo(w * 0.35f, h * 0.55f)
                        lineTo(w * 0.45f, h * 0.65f)
                        lineTo(w * 0.65f, h * 0.45f)
                    }
                    drawPath(checkPath, Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                }
                cat.contains("finance") || cat.contains("money") -> {
                    // Golden coin - Yellow/Gold
                    val mainColor = Color(0xFFFDCB6E)
                    drawCircle(mainColor, radius = w * 0.4f, center = center)
                    drawCircle(Color.White.copy(alpha = 0.5f), radius = w * 0.25f, center = center, style = Stroke(width = w * 0.05f))
                }
                cat.contains("photography") || cat.contains("camera") -> {
                    // Simple camera - Mint
                    val mainColor = Color(0xFF55E6C1)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.5f))
                    drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.15f, center = center)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.1f))
                }
                cat.contains("tools") || cat.contains("utility") -> {
                    // Modern wrench/cog - Gray
                    val mainColor = Color(0xFF636E72)
                    drawCircle(mainColor, radius = w * 0.25f, center = center)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.4f))
                }
                cat.contains("shopping") -> {
                    // Shopping bag - Pink
                    val mainColor = Color(0xFFE84393)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f))
                    val handlePath = Path().apply {
                        moveTo(w * 0.35f, h * 0.3f)
                        quadraticTo(w * 0.5f, h * 0.05f, w * 0.65f, h * 0.3f)
                    }
                    drawPath(handlePath, mainColor, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                }
                cat.contains("travel") || cat.contains("transport") -> {
                    // Hot air balloon or Plane look - Blue
                    val mainColor = Color(0xFF0984E3)
                    drawCircle(mainColor, radius = w * 0.3f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f))
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.8f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.1f))
                }
                cat.contains("health") || cat.contains("fitness") -> {
                    // Heart - Red
                    val mainColor = Color(0xFFD63031)
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.35f)
                        cubicTo(w * 0.5f, h * 0.1f, w * 0.15f, h * 0.1f, w * 0.15f, h * 0.4f)
                        cubicTo(w * 0.15f, h * 0.7f, w * 0.5f, h * 0.9f, w * 0.5f, h * 0.9f)
                        cubicTo(w * 0.5f, h * 0.9f, w * 0.85f, h * 0.7f, w * 0.85f, h * 0.4f)
                        cubicTo(w * 0.85f, h * 0.1f, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.35f)
                        close()
                    }
                    drawPath(path, mainColor)
                }
                cat.contains("games") -> {
                    // Controller - Indigo
                    val mainColor = Color(0xFF6C5CE7)
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.4f)
                        lineTo(w * 0.8f, h * 0.4f)
                        quadraticTo(w * 0.95f, h * 0.4f, w * 0.95f, h * 0.6f)
                        lineTo(w * 0.85f, h * 0.85f)
                        quadraticTo(w * 0.75f, h * 0.9f, w * 0.65f, h * 0.75f)
                        lineTo(w * 0.35f, h * 0.75f)
                        lineTo(w * 0.15f, h * 0.85f)
                        quadraticTo(w * 0.05f, h * 0.9f, w * 0.05f, h * 0.6f)
                        lineTo(w * 0.05f, h * 0.4f)
                        close()
                    }
                    drawPath(path, mainColor)
                }
                cat.contains("education") -> {
                    // Graduation cap - Indigo
                    val mainColor = Color(0xFF2D3436)
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.2f)
                        lineTo(w * 0.9f, h * 0.4f)
                        lineTo(w * 0.5f, h * 0.6f)
                        lineTo(w * 0.1f, h * 0.4f)
                        close()
                    }
                    drawPath(path, mainColor)
                    drawRect(mainColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f))
                }
                cat.contains("browser") -> {
                    // Globe - Blue
                    val mainColor = Color(0xFF74B9FF)
                    drawCircle(mainColor, radius = w * 0.4f, center = center)
                    drawRect(Color.White.copy(alpha = 0.5f), topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.48f), size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.04f))
                }
                cat.contains("dev") -> {
                    // Brackets - Dark Gray
                    val mainColor = Color(0xFF2D3436)
                    val path = Path().apply {
                        moveTo(w * 0.3f, h * 0.3f)
                        lineTo(w * 0.15f, h * 0.5f)
                        lineTo(w * 0.3f, h * 0.7f)
                        
                        moveTo(w * 0.7f, h * 0.3f)
                        lineTo(w * 0.85f, h * 0.5f)
                        lineTo(w * 0.7f, h * 0.7f)
                    }
                    drawPath(path, mainColor, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
                }
                cat.contains("most used") -> {
                    // Flame - Orange
                    val mainColor = Color(0xFFE17055)
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        quadraticTo(w * 0.8f, h * 0.5f, w * 0.7f, h * 0.8f)
                        quadraticTo(w * 0.5f, h * 0.95f, w * 0.3f, h * 0.8f)
                        quadraticTo(w * 0.2f, h * 0.5f, w * 0.5f, h * 0.1f)
                        close()
                    }
                    drawPath(path, mainColor)
                }
                else -> {
                    // Default playful shape - Circle with a square
                    drawCircle(Color(0xFF81ECEC), radius = w * 0.35f, center = center)
                    drawRect(Color.White.copy(alpha = 0.5f), topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.35f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.3f))
                }
            }
        }
    }
}
