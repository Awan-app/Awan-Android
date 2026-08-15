package com.awan.feature.inventory.impl.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val PARTICLE_COUNT = 8
private val BrightYellow = Color(0xFFFFEE58)
private val DarkYellow = Color(0xFFF57F17)

/**
 * First-time item acquisition animation for new inventory items (seen == false).
 *
 * Sequence:
 * 1. Hover & Slam: Item slides upward slightly (-16dp, scale 1.05x), then slams down into default place.
 * 2. Golden Particle Burst: Upon slam impact, emits exactly 8 short line particles (1.5dp stroke)
 *    in a 360° radial spread with a bright-to-dark golden gradient.
 * 3. White Shine: A brief, fast white glass-like highlight sweeps diagonally across the item,
 *    leaving it in its pristine normal appearance.
 */
@Composable
fun NewItemAcquisitionEffect(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = reducedMotion()

    val translationYAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    val particleProgress = remember { Animatable(0f) }
    val shineProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect

        // Stagger entrance based on grid index
        delay((index * 80L).milliseconds)

        // 1. Hover upward (-16dp, scale 1.05)
        coroutineScope {
            launch {
                translationYAnim.animateTo(
                    targetValue = -16f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1.05f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
            }
        }

        // Slight hover pause at apex
        delay(40.milliseconds)

        // 2. Slam crashing into default place (0dp, scale 1.0)
        coroutineScope {
            launch {
                translationYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                )
            }
        }

        // 3. Trigger Particle Burst & Fast White Shine upon slam impact
        coroutineScope {
            launch {
                particleProgress.snapTo(0f)
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                )
            }
            launch {
                shineProgress.snapTo(0f)
                shineProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = LinearEasing),
                )
            }
        }
    }

    if (reduced) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val shape = AwanTheme.shapes.card

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = translationYAnim.value * density
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            },
    ) {
        // Main Item Content with White Shine Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    val t = shineProgress.value
                    if (t in 0.01f..0.99f) {
                        val sweepWidth = size.width * 0.6f
                        val startX = (size.width + sweepWidth * 2f) * t - sweepWidth
                        val shineBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f * (1f - t * 0.3f)),
                                Color.Transparent,
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + sweepWidth, size.height),
                        )
                        drawRect(brush = shineBrush, size = Size(size.width, size.height))
                    }
                },
        ) {
            content()
        }

        // 8-Particle Golden Line Burst Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = particleProgress.value
            if (p > 0f && p < 1f) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.65f
                val lineLength = 10.dp.toPx()
                val alpha = (1f - p).coerceIn(0f, 1f)

                repeat(PARTICLE_COUNT) { i ->
                    val angle = (i.toFloat() / PARTICLE_COUNT) * 2f * PI.toFloat()
                    val distance = maxRadius * p

                    val startX = center.x + cos(angle) * (distance * 0.6f)
                    val startY = center.y + sin(angle) * (distance * 0.6f)

                    val endX = center.x + cos(angle) * (distance * 0.6f + lineLength)
                    val endY = center.y + sin(angle) * (distance * 0.6f + lineLength)

                    val startOffset = Offset(startX, startY)
                    val endOffset = Offset(endX, endY)

                    val goldBrush = Brush.linearGradient(
                        colors = listOf(
                            BrightYellow.copy(alpha = alpha),
                            DarkYellow.copy(alpha = alpha),
                        ),
                        start = startOffset,
                        end = endOffset,
                    )

                    drawLine(
                        brush = goldBrush,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
