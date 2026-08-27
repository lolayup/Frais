package com.khaled.frais.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.ui.theme.NothingRed
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * FRAIS Glyph Engine
 *
 * Nothing-inspired 16x16 dot matrix.
 */
object GlyphEngine {

    const val MATRIX_SIZE = 16

    object Patterns {

        val EYES_OPEN = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000110000110000,
            0b0000110000110000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000011111100000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val EYES_CLOSED = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000110000110000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000011111100000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val ZZZ_1 = listOf(
            0b0000000000000000,
            0b0001111000000000,
            0b0000001000000000,
            0b0000010000000000,
            0b0000100000000000,
            0b0001111000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000111000,
            0b0000000000001000,
            0b0000000000010000,
            0b0000000000111000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val ZZZ_2 = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0111100000000000,
            0b0000100000000000,
            0b0001000000000000,
            0b0010000000000000,
            0b0111100000000000,
            0b0000000000000000,
            0b0000000000011100,
            0b0000000000000100,
            0b0000000000001000,
            0b0000000000011100,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val ALERT_DOT = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000110000000,
            0b0000000110000000
        )

        val SCAN_1 = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0011000011000000,
            0b0011000011000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000011111100000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val SCAN_2 = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000001100001100,
            0b0000001100001100,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000011111100000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val HAPPY_FACE = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000110000110000,
            0b0000110000110000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000110000000,
            0b0000001001000000,
            0b0000010000100000,
            0b0000010000100000,
            0b0000001111000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val SNOWFLAKE = listOf(
            0b0000000110000000,
            0b0001000110001000,
            0b0000100110010000,
            0b0000010110100000,
            0b0000001111000000,
            0b0000011111100000,
            0b0111111111111110,
            0b0111111111111110,
            0b0111111111111110,
            0b0000011111100000,
            0b0000001111000000,
            0b0000010110100000,
            0b0000100110010000,
            0b0001000110001000,
            0b0000000110000000,
            0b0000000000000000
        )

        val ADDING = listOf(
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0111111111111110,
            0b0111111111111110,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val REMOVING = listOf(
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0111111111111110,
            0b0111111111111110,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val TRASH = listOf(
            0b0000001111110000,
            0b0011111111111100,
            0b1100111111110011,
            0b1111111111111111,
            0b0011111111111100,
            0b0011101110111100,
            0b0011101110111100,
            0b0011101110111100,
            0b0011111111111100,
            0b0011111111111100,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val UPLOAD = listOf(
            0b0000000110000000,
            0b0000001111000000,
            0b0000011111100000,
            0b0000111111110000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000000000000,
            0b0111111111111110,
            0b0111111111111110,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val DOWNLOAD = listOf(
            0b0111111111111110,
            0b0111111111111110,
            0b0000000000000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000000110000000,
            0b0000111111110000,
            0b0000011111100000,
            0b0000001111000000,
            0b0000000110000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val UNLOCKED_LOCK = listOf(
            0b0000011110000000,
            0b0000100001000000,
            0b0000100000000000,
            0b0000100000000000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011100001110000,
            0b0011100001110000,
            0b0011111111110000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val LOCKED_LOCK = listOf(
            0b0000011110000000,
            0b0000100001000000,
            0b0000100001000000,
            0b0000100001000000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011111111110000,
            0b0011100001110000,
            0b0011100001110000,
            0b0011111111110000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val GAMES = listOf(
            0b0000000000000000,
            0b0000011111100000,
            0b0001111111111000,
            0b0011111111111100,
            0b0111111111111110,
            0b0111111111111110,
            0b0110001111000110,
            0b0110001111000110,
            0b0011111111111100,
            0b0001110000111000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val SOCIAL = listOf(
            0b0000000000000000,
            0b0000011110000000,
            0b0000111111000000,
            0b0000111111000000,
            0b0000011110000000,
            0b0001111111100000,
            0b0011111111110000,
            0b0011111111110000,
            0b0000000000000000,
            0b0001111000000000,
            0b0011111100000000,
            0b0011111100000000,
            0b0001111000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val MEDIA = listOf(
            0b0000000000000000,
            0b0000110000000000,
            0b0000111100000000,
            0b0000111111000000,
            0b0000111111110000,
            0b0000111111111100,
            0b0000111111111100,
            0b0000111111110000,
            0b0000111111000000,
            0b0000111100000000,
            0b0000110000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val TOOLS = listOf(
            0b0000000001100000,
            0b0000000011110000,
            0b0000000111111000,
            0b0000001111111100,
            0b0000001111111100,
            0b0000000111111000,
            0b0000000011110000,
            0b0000000111110000,
            0b0000001111100000,
            0b0000011111000000,
            0b0000111110000000,
            0b0001111100000000,
            0b0011111000000000,
            0b0111100000000000,
            0b0011000000000000,
            0b0000000000000000
        )

        val PRODUCTIVITY = listOf(
            0b0000000000000000,
            0b0000001111000000,
            0b0000010000100000,
            0b0001111111111100,
            0b0011111111111110,
            0b0011111111111110,
            0b0011111111111110,
            0b0011111111111110,
            0b0011111111111110,
            0b0011111111111110,
            0b0011111111111110,
            0b0001111111111100,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val COMMUNICATION = listOf(
            0b0000000000000000,
            0b0001111111110000,
            0b0011111111111000,
            0b0011111111111000,
            0b0011111111111000,
            0b0011111111111000,
            0b0011111111111000,
            0b0011111111111000,
            0b0001111111110000,
            0b0000111111100000,
            0b0000011111000000,
            0b0000000110000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val SYSTEM = listOf(
            0b0000000110000000,
            0b0000100110010000,
            0b0000011111100000,
            0b0001111111111000,
            0b0001110001111000,
            0b0111100000111110,
            0b0111100000111110,
            0b0001110001111000,
            0b0001111111111000,
            0b0000011111100000,
            0b0000100110010000,
            0b0000000110000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000,
            0b0000000000000000
        )

        val MOST_USED = listOf(
            0b0000000011000000,
            0b0000000111100000,
            0b0000001111100000,
            0b0000011111000000,
            0b0000011111110000,
            0b0000111111111000,
            0b0001111111111100,
            0b0011111111111110,
            0b0011111111111110,
            0b0111111111111111,
            0b0111111111111111,
            0b0011111111111110,
            0b0001111111111100,
            0b0000011111110000,
            0b0000000000000000,
            0b0000000000000000
        )
    }
}

/**
 * 16x16 Glyph matrix.
 */
@Composable
fun GlyphMatrix(
    modifier: Modifier = Modifier,
    pattern: List<Int>? = null,
    secondaryPattern: List<Int>? = null,
    accentColor: Color = Color.White,
    secondaryColor: Color = NothingRed,
    lightningProgress: Float = 0f,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "glyphPulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.45f else 1f,
        targetValue = 1f,
        animationSpec = if (isPulsing) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 900,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            infiniteRepeatable(
                animation = tween(1)
            )
        },
        label = "pulseAlpha"
    )
    
    val baseDotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    Canvas(modifier = modifier) {

        val spacing =
            size.minDimension / GlyphEngine.MATRIX_SIZE

        val dotRadius =
            spacing * 0.24f

        for (row in 0 until GlyphEngine.MATRIX_SIZE) {
            for (col in 0 until GlyphEngine.MATRIX_SIZE) {

                val center = Offset(
                    x = col * spacing + spacing / 2f,
                    y = row * spacing + spacing / 2f
                )

                drawCircle(
                    color = baseDotColor,
                    radius = dotRadius,
                    center = center
                )

                if (pattern != null && row < pattern.size) {

                    val bit =
                        (pattern[row] shr
                                (GlyphEngine.MATRIX_SIZE - 1 - col)) and 1

                    if (bit == 1) {
                        drawCircle(
                            color = accentColor.copy(
                                alpha = pulseAlpha
                            ),
                            radius = dotRadius * 1.35f,
                            center = center
                        )
                    }
                }

                if (
                    secondaryPattern != null &&
                    row < secondaryPattern.size
                ) {

                    val bit =
                        (secondaryPattern[row] shr
                                (GlyphEngine.MATRIX_SIZE - 1 - col)) and 1

                    if (bit == 1) {
                        drawCircle(
                            color = secondaryColor,
                            radius = dotRadius * 1.45f,
                            center = center
                        )
                    }
                }
            }
        }

        if (lightningProgress > 0f) {
            drawLightningEffect(
                progress = lightningProgress,
                dotRadius = dotRadius
            )
        }
    }
}

private fun DrawScope.drawLightningEffect(
    progress: Float,
    dotRadius: Float
) {
    val random = Random(
        (System.currentTimeMillis() / 120).toInt()
    )

    val points = mutableListOf<Offset>()

    val steps = 10

    var lastY = size.height / 2f

    val visibleSteps =
        (steps * progress).toInt()

    for (i in 0 until visibleSteps) {

        val x =
            (i + 1) * (size.width / steps)

        val y = (
                lastY +
                        (random.nextFloat() - 0.5f) *
                        size.height *
                        0.65f
                ).coerceIn(
                size.height * 0.15f,
                size.height * 0.85f
            )

        points += Offset(x, y)

        lastY = y
    }

    if (points.isNotEmpty()) {

        drawPoints(
            points = points,
            pointMode = PointMode.Polygon,
            color = Color.White.copy(alpha = 0.75f),
            strokeWidth = dotRadius * 2f
        )

        points.forEach { point ->

            drawCircle(
                color = Color.White,
                radius = dotRadius * 2.2f,
                center = point
            )
        }
    }
}

/**
 * FRAIS Glyph Live Widget.
 *
 * IMPORTANT:
 * appCount MUST be the number of USER apps.
 * Do not pass the total including system apps.
 *
 * filterAppCount is the number of USER apps in the
 * currently selected filter.
 */
@Composable
fun GlyphLiveWidget(
    isLoading: Boolean,
    isFreezing: Boolean,
    transientState: GlyphState?,
    currentFilter: String?,
    appCount: Int,
    actionableAppsCount: Int,
    actionablePrivateAppsCount: Int = 0,
    isPrivateAuthenticated: Boolean = false,
    totalAppCount: Int = 0,
    totalFilterCount: Int = 0,
    filterAppCount: Int = appCount,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var state by remember {
        mutableStateOf(GlyphState.IDLE)
    }

    var currentPattern by remember {
        mutableStateOf(
            GlyphEngine.Patterns.EYES_OPEN
        )
    }

    val lightningProgress =
        remember {
            Animatable(0f)
        }

    val subLabels = listOf("USER APPS", "TOTAL APPS")
    var subLabelIndex by remember { mutableStateOf(0) }
    var subLabelText by remember { mutableStateOf(subLabels[0]) }

    LaunchedEffect(currentFilter) {
        if (currentFilter == null) {
            while (true) {
                delay(Random.nextLong(5000, 10000))
                val nextIndex = (subLabelIndex + 1) % subLabels.size
                val nextLabel = subLabels[nextIndex]

                // Typewriter out
                for (i in subLabelText.length downTo 0) {
                    subLabelText = subLabelText.substring(0, i)
                    delay(30)
                }
                delay(200)
                subLabelIndex = nextIndex
                // Typewriter in
                for (i in 0..nextLabel.length) {
                    subLabelText = nextLabel.substring(0, i)
                    delay(50)
                }
            }
        } else {
            subLabelText = "IN FILTER"
        }
    }

    /*
     * Resolve state.
     */
    LaunchedEffect(
        isLoading,
        isFreezing,
        transientState,
        actionablePrivateAppsCount,
        currentFilter,
        isPrivateAuthenticated
    ) {
        val isOnSecuredPage = currentFilter?.contains("SECURE", ignoreCase = true) == true ||
                             currentFilter?.contains("PRIVATE", ignoreCase = true) == true

        state = when {
            transientState != null ->
                transientState

            isFreezing ->
                GlyphState.FREEZING

            isLoading ->
                GlyphState.SCANNING

            actionablePrivateAppsCount > 0 && isOnSecuredPage ->
                GlyphState.SECURED_ACTIVE

            else ->
                GlyphState.IDLE
        }
    }

    /*
     * Glyph animation.
     */
    LaunchedEffect(
        state,
        actionableAppsCount,
        currentFilter,
        isPrivateAuthenticated
    ) {

        while (true) {

            when (state) {

                GlyphState.IDLE -> {

                    if (currentFilter != null) {
                        currentPattern = when {
                            currentFilter.contains("GAME", ignoreCase = true) || 
                            currentFilter.contains("HUB", ignoreCase = true) -> GlyphEngine.Patterns.GAMES
                            currentFilter.contains("SOCIAL", ignoreCase = true) -> GlyphEngine.Patterns.SOCIAL
                            currentFilter.contains("MEDIA", ignoreCase = true) || currentFilter.contains("PHOTO", ignoreCase = true) -> GlyphEngine.Patterns.MEDIA
                            currentFilter.contains("TOOL", ignoreCase = true) -> GlyphEngine.Patterns.TOOLS
                            currentFilter.contains("COMM", ignoreCase = true) || currentFilter.contains("CHAT", ignoreCase = true) -> GlyphEngine.Patterns.COMMUNICATION
                            currentFilter.contains("PROD", ignoreCase = true) || currentFilter.contains("WORK", ignoreCase = true) -> GlyphEngine.Patterns.PRODUCTIVITY
                            currentFilter.contains("SYS", ignoreCase = true) || currentFilter.contains("CONFIG", ignoreCase = true) -> GlyphEngine.Patterns.SYSTEM
                            currentFilter.contains("USED", ignoreCase = true) -> GlyphEngine.Patterns.MOST_USED
                            currentFilter.contains("SECURE", ignoreCase = true) || currentFilter.contains("PRIVATE", ignoreCase = true) -> {
                                if (isPrivateAuthenticated) GlyphEngine.Patterns.UNLOCKED_LOCK
                                else GlyphEngine.Patterns.LOCKED_LOCK
                            }
                            else -> GlyphEngine.Patterns.EYES_OPEN
                        }
                        delay(1500)
                    } else if (actionableAppsCount > 0) {

                        currentPattern =
                            GlyphEngine.Patterns.EYES_OPEN

                        delay(
                            Random.nextLong(
                                2500,
                                5000
                            )
                        )

                        currentPattern =
                            GlyphEngine.Patterns.EYES_CLOSED

                        delay(140)

                        currentPattern =
                            GlyphEngine.Patterns.EYES_OPEN

                    } else {

                        currentPattern =
                            GlyphEngine.Patterns.EYES_CLOSED

                        delay(1800)

                        currentPattern =
                            GlyphEngine.Patterns.ZZZ_1

                        delay(700)

                        currentPattern =
                            GlyphEngine.Patterns.ZZZ_2

                        delay(900)
                    }
                }

                GlyphState.SCANNING -> {

                    currentPattern =
                        GlyphEngine.Patterns.SCAN_1

                    delay(350)

                    currentPattern =
                        GlyphEngine.Patterns.SCAN_2

                    delay(350)
                }

                GlyphState.FREEZING -> {

                    currentPattern =
                        GlyphEngine.Patterns.SNOWFLAKE

                    delay(900)
                }

                GlyphState.ADDING -> {

                    currentPattern =
                        GlyphEngine.Patterns.ADDING

                    delay(900)
                }

                GlyphState.REMOVING -> {

                    currentPattern =
                        GlyphEngine.Patterns.REMOVING

                    delay(900)
                }

                GlyphState.UNINSTALLING -> {

                    currentPattern =
                        GlyphEngine.Patterns.TRASH

                    delay(900)
                }

                GlyphState.BACKUP -> {

                    currentPattern =
                        GlyphEngine.Patterns.UPLOAD

                    delay(900)
                }

                GlyphState.RESTORE -> {

                    currentPattern =
                        GlyphEngine.Patterns.DOWNLOAD

                    delay(900)
                }

                GlyphState.SECURED_ACTIVE -> {

                    currentPattern =
                        if (isPrivateAuthenticated) GlyphEngine.Patterns.UNLOCKED_LOCK
                        else GlyphEngine.Patterns.LOCKED_LOCK

                    delay(1200)
                }

                GlyphState.SUCCESS -> {

                    currentPattern =
                        GlyphEngine.Patterns.HAPPY_FACE

                    delay(900)
                }

                else -> {

                    currentPattern =
                        GlyphEngine.Patterns.EYES_OPEN

                    delay(900)
                }
            }
        }
    }

    /*
     * Lightning animation.
     */
    LaunchedEffect(state) {

        if (
            state == GlyphState.FREEZING ||
            state == GlyphState.SCANNING
        ) {

            lightningProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1100,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )

        } else {

            lightningProgress.snapTo(0f)
        }
    }

    /*
     * State color.
     */
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val defaultActiveColor = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White

    val accentColor = when (state) {

        GlyphState.FREEZING ->
            Color(0xFF81D4FA)

        GlyphState.UNINSTALLING,
        GlyphState.ERROR,
        GlyphState.SECURED_ACTIVE ->
            NothingRed

        GlyphState.BACKUP,
        GlyphState.SUCCESS ->
            Color(0xFF69F0AE)

        GlyphState.RESTORE ->
            Color(0xFFFFD54F)

        else ->
            defaultActiveColor
    }

    /*
     * Count shown by the widget.
     *
     * This is ALWAYS a USER-APP count.
     *
     * When a filter is selected:
     *     filterAppCount
     *
     * When no filter is selected:
     *     appCount
     */
    val displayedCountValue = when {
        state == GlyphState.SECURED_ACTIVE -> actionablePrivateAppsCount
        currentFilter != null -> filterAppCount
        subLabelText == "TOTAL APPS" -> totalAppCount
        else -> appCount
    }.coerceAtLeast(0)

    /*
     * Selected filter label.
     */
    val displayLabel = currentFilter?.uppercase() ?: "ALL APPS"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * LARGE GLYPH
         */
        GlyphMatrix(
            modifier = Modifier.size(76.dp),
            pattern = currentPattern,
            secondaryPattern =
                if (
                    (actionableAppsCount > 0 || actionablePrivateAppsCount > 0) &&
                    state == GlyphState.IDLE
                ) {
                    GlyphEngine.Patterns.ALERT_DOT
                } else {
                    null
                },
            accentColor = accentColor,
            secondaryColor = NothingRed,
            isPulsing =
                isLoading ||
                        isFreezing,
            lightningProgress =
                lightningProgress.value
        )

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        /*
         * INFORMATION AREA
         */
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.Center
        ) {

            /*
             * FILTER / STATE LABEL
             */
            Text(
                text = when {

                    state == GlyphState.SCANNING ->
                        "SCANNING"

                    state == GlyphState.FREEZING ->
                        "OPTIMIZING"

                    state == GlyphState.ADDING ->
                        "PACKAGE LINKED"

                    state == GlyphState.REMOVING ->
                        "PACKAGE REMOVED"

                    state == GlyphState.UNINSTALLING ->
                        "UNINSTALLING"

                    state == GlyphState.BACKUP ->
                        "EXPORTING"

                    state == GlyphState.RESTORE ->
                        "IMPORTING"

                    state == GlyphState.SUCCESS ->
                        "COMPLETE"

                    state == GlyphState.ERROR ->
                        "FAILED"

                    else ->
                        displayLabel
                },
                color = if (state == GlyphState.SECURED_ACTIVE) {
                    val infiniteTransition = rememberInfiniteTransition(label = "securedRed")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    NothingRed.copy(alpha = alpha)
                } else {
                    accentColor.copy(alpha = 0.78f)
                },
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Bold,
                letterSpacing = 1.3.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(1.dp)
            )

            /*
             * LARGE APP COUNT
             */
            if (state == GlyphState.IDLE || state == GlyphState.SECURED_ACTIVE) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            displayedCountValue.toString(),
                        color =
                            if (
                                actionableAppsCount > 0 || state == GlyphState.SECURED_ACTIVE
                            ) {
                                NothingRed
                            } else {
                                Color.White
                            },
                        fontSize = 32.sp,
                        fontWeight =
                            FontWeight.ExtraBold,
                        letterSpacing =
                            (-1.4).sp,
                        lineHeight = 34.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )

                    Text(
                        text = (if (state == GlyphState.SECURED_ACTIVE) {
                            "SECURED ACTIVE"
                        } else {
                            subLabelText
                        } + " • $totalFilterCount FILTERS").uppercase(),
                        color = if (state == GlyphState.SECURED_ACTIVE) {
                            NothingRed.copy(alpha = 0.7f)
                        } else {
                            Color.Gray.copy(
                                alpha = 0.75f
                            )
                        },
                        fontSize = 10.sp,
                        fontWeight =
                            FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }

            } else {

                Text(
                    text = when {

                        isFreezing ->
                            "OPTIMIZING CORES"

                        isLoading ->
                            "INDEXING $appCount USER APPS"

                        state == GlyphState.ADDING ->
                            "DATA SYNCED"

                        state == GlyphState.REMOVING ->
                            "PACKAGE REMOVED"

                        state == GlyphState.UNINSTALLING ->
                            "PURGING DATA"

                        state == GlyphState.BACKUP ->
                            "EXPORTING DATA"

                        state == GlyphState.RESTORE ->
                            "IMPORTING DATA"

                        state == GlyphState.SUCCESS ->
                            "SYSTEM READY"

                        state == GlyphState.ERROR ->
                            "CHECK OPERATION"

                        else ->
                            "FRAIS"
                    },
                    color =
                        Color.White.copy(
                            alpha = 0.86f
                        ),
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            /*
             * BOTTOM STATUS / ACTIVE BADGE
             */
            if (state == GlyphState.IDLE) {

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                if (actionableAppsCount > 0) {

                    /*
                     * ACTIVE APPS
                     *
                     * Instead of:
                     * ● ACTIVE 03
                     *
                     * we make the status feel like
                     * a small Nothing-style indicator.
                     */
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "●",
                            color = NothingRed,
                            fontSize = 9.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text(
                            text = "ACTIVE",
                            color =
                                Color.White.copy(
                                    alpha = 0.78f
                                ),
                            fontSize = 9.sp,
                            fontWeight =
                                FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(
                            text =
                                actionableAppsCount
                                    .toString()
                                    .padStart(2, '0'),
                            color = NothingRed,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                } else {

                    Text(
                        text =
                            if (
                                currentFilter != null
                            ) {
                                "$displayedCountValue APPS IN FILTER"
                            } else {
                                "$displayedCountValue USER APPS PROTECTED"
                            },
                        color =
                            Color.Gray.copy(
                                alpha = 0.68f
                            ),
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

enum class GlyphState {
    IDLE,
    SCANNING,
    FREEZING,
    SUCCESS,
    ERROR,
    ADDING,
    REMOVING,
    UNINSTALLING,
    BACKUP,
    RESTORE,
    SECURED_ACTIVE
}