package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PARTICLE_COUNT = 18
private const val SPREAD_FRACTION = 0.55f
private const val GRAVITY = 0.35f
private const val BURST_MILLIS = 900

/**
 * A one-shot celebratory burst drawn over the content it is stacked on. Fires whenever [celebrate]
 * becomes true.
 */
@Composable
fun SparkleBurst(celebrate: Boolean, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val progress = remember { Animatable(0f) }
    val colors = AwanTheme.colors
    val palette = remember(colors) {
        listOf(colors.zoneSun, colors.zoneTangerine, colors.zoneViolet, colors.sky)
    }

    LaunchedEffect(celebrate) {
        if (celebrate && !reduced) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = BURST_MILLIS))
        }
    }

    if (reduced) return

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height / 2f)
        val reach = size.minDimension * SPREAD_FRACTION
        repeat(PARTICLE_COUNT) { i ->
            val angle = (i.toFloat() / PARTICLE_COUNT) * 2f * PI.toFloat()
            val distance = reach * t
            val drop = GRAVITY * reach * t * t
            drawCircle(
                color = palette[i % palette.size].copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.018f * (1f - t * 0.5f),
                center = Offset(
                    x = origin.x + cos(angle) * distance,
                    y = origin.y + sin(angle) * distance + drop,
                ),
            )
        }
    }
}
