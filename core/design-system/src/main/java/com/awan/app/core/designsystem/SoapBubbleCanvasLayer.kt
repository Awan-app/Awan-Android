package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun rememberSoapBubbleTime(
    cycleDurationMillis: Int,
    isReducedMotion: Boolean,
): Float {
    if (isReducedMotion) {
        return 0.18f
    }
    val transition = rememberInfiniteTransition(label = "SoapBubbleTransition")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = cycleDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "SoapBubbleTime",
    )
    return time
}

/**
 * Procedural multi-ring wave-distorted renderer for Android 12 (API 31–32)
 * achieving visual richness, color waves, and distortion across the bubble surface.
 */
@Composable
internal fun SoapBubbleCanvasLayer(
    modifier: Modifier,
    baseColor: Color = SoapBubbleStyle.DEFAULT_BASE_COLOR,
    strength: Float = SoapBubbleStyle.DEFAULT_STRENGTH,
    cycleDurationMillis: Int = SoapBubbleStyle.DEFAULT_CYCLE_DURATION_MILLIS,
    isReducedMotion: Boolean = false,
) {
    val time = rememberSoapBubbleTime(
        cycleDurationMillis = cycleDurationMillis,
        isReducedMotion = isReducedMotion,
    )

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val bubbleCenter = center

        // 1. Soft monochromatic interior core sheen with gradient breakpoints:
        // 10% opacity at center (0.0), 80% at 30% in (0.70), and full 100% at edge (1.0)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to baseColor.copy(alpha = 0.010f * strength),
                    0.70f to baseColor.copy(alpha = 0.080f * strength),
                    1.00f to baseColor.copy(alpha = 0.100f * strength),
                ),
                center = bubbleCenter,
                radius = maxRadius * 0.94f,
            ),
            radius = maxRadius * 0.94f,
            center = bubbleCenter,
        )

        // 2. Multi-band organic wave arcs with dynamic radial distortion
        repeat(SoapBubbleStyle.RingRadii.size) { ringIndex ->
            val normalizedRadius = SoapBubbleStyle.RingRadii[ringIndex]
            val strokeWidth = maxRadius * SoapBubbleStyle.RingWidths[ringIndex]

            repeat(SoapBubbleStyle.SEGMENT_COUNT) { segmentIndex ->
                val startFraction = segmentIndex.toFloat() / SoapBubbleStyle.SEGMENT_COUNT
                val angleRadians = startFraction * SoapBubbleStyle.TWO_PI

                val sample = SoapBubbleMath.sampleSoapFilm(
                    normalizedRadius = normalizedRadius,
                    angleRadians = angleRadians,
                    normalizedTime = time,
                    strength = strength,
                    baseColor = baseColor,
                )

                if (sample.alpha > SoapBubbleStyle.MIN_VISIBLE_ALPHA) {
                    val effectiveRadius = (maxRadius * sample.distortedRadius)
                        .coerceIn(maxRadius * 0.10f, maxRadius * 1.05f)

                    drawArc(
                        color = sample.color.copy(alpha = sample.alpha),
                        startAngle = startFraction * 360f,
                        sweepAngle = (360f / SoapBubbleStyle.SEGMENT_COUNT) + SoapBubbleStyle.SEGMENT_OVERLAP_DEGREES,
                        useCenter = false,
                        topLeft = Offset(
                            bubbleCenter.x - effectiveRadius,
                            bubbleCenter.y - effectiveRadius,
                        ),
                        size = Size(
                            effectiveRadius * 2f,
                            effectiveRadius * 2f,
                        ),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
        }

        // 3. Draw thin outer rim
        drawSoapBubbleRim(
            radius = maxRadius,
            strength = strength,
        )

        // 4. Draw specular highlight crescents
        drawSoapBubbleHighlights(
            radius = maxRadius,
            strength = strength,
        )
    }
}

private fun DrawScope.drawSoapBubbleRim(
    radius: Float,
    strength: Float,
) {
    val rimRadius = radius * 0.96f
    val rimWidth = (radius * 0.025f).coerceAtLeast(1f)
    drawCircle(
        color = Color.White.copy(alpha = 0.09f * strength),
        radius = rimRadius,
        center = center,
        style = Stroke(width = rimWidth),
    )
}

private fun DrawScope.drawSoapBubbleHighlights(
    radius: Float,
    strength: Float,
) {
    val highlightRadius = radius * 0.88f
    val highlightWidth = (radius * 0.05f).coerceAtLeast(1.5f)
    val bubbleCenter = center
    val topLeft = Offset(bubbleCenter.x - highlightRadius, bubbleCenter.y - highlightRadius)
    val size = Size(highlightRadius * 2f, highlightRadius * 2f)

    // Upper-left primary highlight (stronger, ~45 deg sweep centered around 225 deg / -135 deg)
    drawArc(
        color = Color.White.copy(alpha = (0.22f * strength).coerceAtMost(0.30f)),
        startAngle = 205f,
        sweepAngle = 45f,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = highlightWidth,
            cap = StrokeCap.Round,
        ),
    )

    // Lower-right secondary highlight (weaker, ~25 deg sweep centered around 45 deg)
    drawArc(
        color = Color.White.copy(alpha = (0.13f * strength).coerceAtMost(0.20f)),
        startAngle = 35f,
        sweepAngle = 25f,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = highlightWidth * 0.85f,
            cap = StrokeCap.Round,
        ),
    )
}
