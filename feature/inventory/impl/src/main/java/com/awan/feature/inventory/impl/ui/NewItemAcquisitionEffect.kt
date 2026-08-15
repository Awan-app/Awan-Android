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
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val PARTICLE_COUNT = 8

// Rich golden gradient tokens: radiant bright yellow -> rich amber orange
private val GoldenYellow = Color(0xFFFFEB3B)
private val GoldenOrange = Color(0xFFFF8F00)

private data class ParticleTrajectory(
    val angle: Float,
    val speedMultiplier: Float,
    val lineLength: Float,
)

/**
 * First-time item acquisition animation for newly obtained inventory items.
 *
 * Requirements fulfilled:
 * 1. Particles originate from the exact horizontal & vertical center of each grid item.
 * 2. Particles are layered BEHIND their own card (so only visible once extending beyond card edges)
 *    while zIndex elevates them in front of neighboring grid items.
 * 3. Particle color is a bright yellow to deep golden-orange gradient.
 * 4. Particles radiate in independent, randomized organic trajectories (not a static star).
 * 5. Particles fade out rapidly after traveling past the card perimeter.
 * 6. Brief diagonal white shine highlights the card on impact.
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

    // Generate randomized organic particle trajectories for each item
    val trajectories = remember {
        val baseStep = (2f * PI.toFloat()) / PARTICLE_COUNT
        List(PARTICLE_COUNT) { i ->
            val jitter = (Random.nextFloat() - 0.5f) * (baseStep * 0.55f)
            val speed = 0.85f + Random.nextFloat() * 0.45f
            val length = 8f + Random.nextFloat() * 6f
            ParticleTrajectory(
                angle = i * baseStep + jitter,
                speedMultiplier = speed,
                lineLength = length,
            )
        }
    }

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

        // 3. Trigger Particle Burst & Fast Glass Shine upon slam
        coroutineScope {
            launch {
                particleProgress.snapTo(0f)
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
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
        Box(modifier = modifier) { content() }
        return
    }

    val isBurstActive = particleProgress.value > 0f && particleProgress.value < 1f
    val shape = AwanTheme.shapes.card

    Box(
        modifier = modifier
            // Elevate above neighboring grid items during particle burst
            .zIndex(if (isBurstActive) 2f else 0f)
            .graphicsLayer {
                translationY = translationYAnim.value * density
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            },
    ) {
        // LAYER 1 (BEHIND CARD): Centered Golden Particle Burst Canvas
        // Rendered behind its own card; particles emerge once extending beyond the card boundaries
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = particleProgress.value
            if (p > 0f && p < 1f) {
                // Exact horizontal and vertical center of the grid item
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxReach = size.minDimension * 0.95f
                val alpha = (1f - p).coerceIn(0f, 1f)

                trajectories.forEach { trajectory ->
                    val distance = maxReach * p * trajectory.speedMultiplier
                    val lineLenPx = trajectory.lineLength.dp.toPx()

                    val startX = center.x + cos(trajectory.angle) * distance
                    val startY = center.y + sin(trajectory.angle) * distance

                    val endX = center.x + cos(trajectory.angle) * (distance + lineLenPx)
                    val endY = center.y + sin(trajectory.angle) * (distance + lineLenPx)

                    val startPoint = Offset(startX, startY)
                    val endPoint = Offset(endX, endY)

                    // Rich yellow-to-orange golden gradient
                    val goldGradient = Brush.linearGradient(
                        colors = listOf(
                            GoldenYellow.copy(alpha = alpha),
                            GoldenOrange.copy(alpha = alpha),
                        ),
                        start = startPoint,
                        end = endPoint,
                    )

                    drawLine(
                        brush = goldGradient,
                        start = startPoint,
                        end = endPoint,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // LAYER 2 (ON TOP OF PARTICLES): Main Item Card with White Shine Overlay
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
    }
}

