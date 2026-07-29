package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DRIFT_MILLIS = 34_000
private const val REVEAL_STAGGER_MILLIS = 70L
private const val PUFF_ALPHA = 0.9f
private const val WRAP_SPAN = 1.34f
private const val WRAP_ORIGIN = -0.17f

/** One blob of a cloud, in multiples of the cloud's own radius, relative to its centre. */
private data class Puff(val dx: Float, val dy: Float, val scale: Float)

private val PuffCluster = listOf(
    Puff(0f, 0f, 1f),
    Puff(-0.78f, 0.2f, 0.68f),
    Puff(0.8f, 0.24f, 0.6f),
    Puff(0.12f, -0.44f, 0.63f),
    Puff(-0.42f, -0.2f, 0.72f),
)

/**
 * [baseX] and [y] are fractions of the banner; [radius] is a fraction of its height. [speed]
 * multiplies the shared drift so nearer clouds outrun farther ones and the band reads as depth
 * rather than a single sliding sheet.
 */
private data class Cloud(
    val baseX: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float,
)

private val Clouds = listOf(
    Cloud(baseX = 0.08f, y = 0.66f, radius = 0.30f, speed = 1.00f, alpha = 1.00f),
    Cloud(baseX = 0.44f, y = 0.40f, radius = 0.22f, speed = 0.62f, alpha = 0.72f),
    Cloud(baseX = 0.72f, y = 0.74f, radius = 0.35f, speed = 1.28f, alpha = 0.92f),
    Cloud(baseX = 0.94f, y = 0.34f, radius = 0.18f, speed = 0.48f, alpha = 0.58f),
    Cloud(baseX = 0.26f, y = 0.86f, radius = 0.26f, speed = 1.52f, alpha = 0.80f),
)

/**
 * A drifting sky band. On first composition the clouds billow up into place one after another;
 * afterwards they drift sideways forever, wrapping off one edge and back in the other.
 *
 * Meant as the header of a surface that opens — a sheet or a dialog — with [content] (usually the
 * mascot) stacked on top of the sky. Under [reducedMotion] the clouds are drawn at rest.
 */
@Composable
fun CloudDrift(
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val reduced = reducedMotion()
    val colors = AwanTheme.colors
    val motion = AwanTheme.motion

    val drift by rememberInfiniteTransition(label = "cloudDrift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDriftPhase",
    )

    // One Animatable per cloud so they arrive in sequence; a single shared value would billow the
    // whole band as one object, which reads as a panel sliding rather than as weather.
    val reveal = remember { Clouds.map { Animatable(if (reduced) 1f else 0f) } }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        reveal.forEachIndexed { index, value ->
            delay(index * REVEAL_STAGGER_MILLIS)
            launch { value.animateTo(1f, motion.bouncy.spec()) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .semantics { hideFromAccessibility() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawSky(colors)
            Clouds.forEachIndexed { index, cloud ->
                drawCloud(
                    cloud = cloud,
                    drift = if (reduced) 0f else drift,
                    reveal = reveal[index].value,
                    color = colors.surface,
                )
            }
        }
        content()
    }
}

private fun DrawScope.drawSky(colors: AwanColors) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to colors.backgroundStart,
            0.62f to colors.skyMorning,
            1f to colors.background,
        ),
    )
}

private fun DrawScope.drawCloud(cloud: Cloud, drift: Float, reveal: Float, color: Color) {
    if (reveal <= 0f) return

    val wrapped = ((cloud.baseX + drift * cloud.speed) % WRAP_SPAN + WRAP_SPAN) % WRAP_SPAN
    val centerX = (WRAP_ORIGIN + wrapped) * size.width
    // Rises the last of its own radius into place, so the billow starts below the band's floor.
    val centerY = (cloud.y + (1f - reveal) * cloud.radius) * size.height
    val radius = cloud.radius * size.height * reveal
    val alpha = cloud.alpha * PUFF_ALPHA * reveal

    PuffCluster.forEach { puff ->
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius * puff.scale,
            center = Offset(centerX + puff.dx * radius, centerY + puff.dy * radius),
        )
    }
}
