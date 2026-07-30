package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SweepMillis = 2500

/** Colour samples around the ring. Enough that the banding between them isn't visible. */
private const val SweepStops = 24
private val AuraStroke = 2.dp

/**
 * A gradient stroke that turns continuously around its content — the sheet's way of saying Awan is
 * about to think, or is thinking right now.
 *
 * The stroke is drawn here rather than through [AwanStyles.textField] because `border()` inside a
 * `Style` block only takes a solid `Color`; there is no brush overload, so an animated sweep cannot
 * live in the styles table. Wrap the component instead of trying to move this into `AwanStyles`.
 */
@Composable
fun AwanAiAura(
    active: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = AwanTheme.shapes.button,
    strokeWidth: Dp = AuraStroke,
    content: @Composable () -> Unit,
) {
    val colors = AwanTheme.colors
    val reduced = reducedMotion()

    /**
     * Three colours read as a cycle, so the ring closes with no seam. The list is *not* padded with
     * a repeat of the first — [colourAt] wraps modulo the size, which is what closes it.
     */
    val palette = remember(colors) { listOf(colors.zoneViolet, colors.sky, colors.zoneTangerine) }

    val turn = when {
        reduced -> 0f
        else -> rememberInfiniteTransition(label = "aiAura").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SweepMillis, easing = LinearEasing)),
            label = "aiAuraTurn",
        ).value
    }

    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(AwanTheme.motion.emphasizedMillis),
        label = "aiAuraAlpha",
    )

    // Rebuilt per frame because the stops *are* the animation; the geometry never moves.
    val stops = remember(palette, turn) {
        Array(SweepStops + 1) { step ->
            val at = step.toFloat() / SweepStops
            at to palette.colourAt(at - turn)
        }
    }

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            if (alpha <= 0f) return@drawWithContent
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                brush = Brush.sweepGradient(*stops, center = center),
                alpha = alpha,
                style = Stroke(width = strokeWidth.toPx()),
            )
        },
    ) {
        content()
    }
}

/**
 * Samples the palette as a closed loop at [at] turns, wrapping negatives. Rotating the *stops* is
 * what makes the sweep travel: rotating the canvas instead spins the outline itself, which on a wide
 * text field swings a tall rounded rectangle across the sheet rather than tracing its edge.
 */
private fun List<Color>.colourAt(at: Float): Color {
    val wrapped = ((at % 1f) + 1f) % 1f * size
    val index = wrapped.toInt()
    return lerp(this[index % size], this[(index + 1) % size], wrapped - index)
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AwanAiAura · Light", showBackground = true)
@Composable
private fun LightAiAuraPreview() {
    AiAuraPreview(dark = false)
}

@Preview(name = "AwanAiAura · Dark", showBackground = true)
@Composable
private fun DarkAiAuraPreview() {
    AiAuraPreview(dark = true)
}

@Composable
private fun AiAuraPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            AwanAiAura(active = true) {
                AwanTextField(
                    value = "Build login page",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AwanAiAura(active = false) {
                AwanTextField(
                    value = "Aura off",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
