package com.khaled.frais.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import kotlin.random.Random
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

typealias PlatformBackdrop = LayerBackdrop

fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = this.nativeBackdrop(backdrop)

@Composable
fun rememberBackdrop(color: Color = Color.Transparent): PlatformBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }

fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    luminance: Float = 0.5f,
    shape: Shape = CircleShape,
    borderColor: Color? = null
): Modifier = 
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminance * 2f - 1f).let { sign(it) * it * it }
            vibrancy()
            colorControls(
                brightness = 0.05f,
                contrast = 1f,
                saturation = 1.7f, // Slightly higher saturation for better "random color" look
            )
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            lens(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        shape = { shape },
        onDrawSurface = {
            val darken = lerp(0.15f, 0.45f, ((luminance - 0.3f) / 0.5f).coerceIn(0f, 1f))
            drawRect(Color.Black.copy(alpha = darken))
            
            // Refined Specular Highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                blendMode = BlendMode.Screen
            )

            // Colored Glow Border
            if (borderColor != null) {
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    brush = Brush.linearGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.6f),
                            borderColor.copy(alpha = 0.2f),
                            borderColor.copy(alpha = 0.4f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // Subtle interior tint
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = borderColor.copy(alpha = 0.08f)
                )
            } else {
                // Subtle base border
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }
        },
    )

fun Modifier.rainDrops(
    color: Color = Color.White,
    count: Int = 10
): Modifier = composed {
    val randomPositions = remember {
        List(count) { 
            val relX = Random.nextFloat()
            val relY = Random.nextFloat()
            val offset = Random.nextInt(0, 5000)
            Triple(relX, relY, offset)
        }
    }

    this.drawWithContent {
        drawContent()
        
        val time = System.currentTimeMillis()
        
        randomPositions.forEach { (relX, relY, offset) ->
            val duration = 5000L
            val progress = ((time + offset) % duration).toFloat() / duration
            
            // Subtle water drop
            val x = relX * size.width
            val y = (relY * size.height + progress * 40.dp.toPx()) % size.height
            val alpha = (1f - progress) * 0.3f
            
            // Drop head
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 1.2.dp.toPx(),
                center = Offset(x, y)
            )
            
            // Very subtle tail/trail
            drawLine(
                color = color.copy(alpha = alpha * 0.5f),
                start = Offset(x, y),
                end = Offset(x, y - 6.dp.toPx() * (1f - progress)),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

fun Modifier.nothingNoise(alpha: Float? = null): Modifier = this.drawWithContent {
    drawContent()
    // Use system time for dynamic grain
    val seed = (System.currentTimeMillis() / 80).toInt() 
    val random = Random(seed)
    val points = mutableListOf<Offset>()
    val density = 0.0005f
    val numPoints = (size.width * size.height * density).toInt().coerceIn(1000, 5000)
    
    val grainAlpha = alpha ?: com.khaled.frais.app.FraisData.grainIntensity

    for (i in 0 until numPoints) {
        points.add(Offset(random.nextFloat() * size.width, random.nextFloat() * size.height))
    }
    
    drawPoints(
        points = points,
        pointMode = PointMode.Points,
        color = Color.White.copy(alpha = grainAlpha),
        strokeWidth = 1.5f,
        blendMode = BlendMode.Screen
    )
}

fun Modifier.nothingDots(color: Color = Color.White.copy(alpha = 0.05f)): Modifier = this.drawWithContent {
    val dotSpacing = 24.dp.toPx()
    val dotRadius = 1.dp.toPx()
    
    // Draw dot matrix background
    for (x in 0 until (size.width / dotSpacing).toInt() + 1) {
        for (y in 0 until (size.height / dotSpacing).toInt() + 1) {
            drawCircle(
                color = color,
                radius = dotRadius,
                center = Offset(x * dotSpacing, y * dotSpacing)
            )
        }
    }
    drawContent()
}

@Composable
fun NothingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        NothingDivider()
    }
}

@Composable
fun NothingDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.extraSmall,
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.5.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color.Transparent
            ),
            content = content
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraSmall,
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.5.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color.Transparent
            ),
            content = content
        )
    }
}

@Composable
fun NothingBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
