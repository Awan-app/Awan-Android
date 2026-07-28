package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.style.Style
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

val AwanChipDotSize = 8.dp
private const val ACTIVE_TINT = 0.18f

/**
 * A value as a pressable pill: a coloured dot, the value itself, and the button family's rim — the
 * face sinks into its own shelf on press exactly like a primary button, so a chip reads as something
 * you press rather than something you read. Pass no [onClick] and it becomes a plain readout.
 *
 * [active] is the difference between a value that was actually chosen and a default standing in for
 * one, which reads as a chip tinted in its tone versus a plain surface-coloured one.
 *
 * [leading] defaults to [AwanChipDot]; callers swap it for anything that carries the same weight —
 * a rail the dot slides along, an avatar, a count.
 */
@Composable
fun AwanChip(
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    leading: @Composable () -> Unit = { AwanChipDot(tone = tone, active = active) },
    onClick: (() -> Unit)? = null,
) {
    val colors = AwanTheme.colors
    val spec = AwanTheme.motion.settle.spec<Color>()
    // Blended, never translucent: the face sits directly on its own tone-coloured rim, so an alpha
    // tint would composite straight back to the rim and swallow the label.
    val fill by animateColorAsState(
        targetValue = if (active) lerp(colors.surface, tone, ACTIVE_TINT) else colors.surface,
        animationSpec = spec,
        label = "chipFill",
    )
    val edge by animateColorAsState(
        targetValue = if (active) tone else colors.line,
        animationSpec = spec,
        label = "chipEdge",
    )
    // The tone is carried by the dot, the border and the rim; the label stays in reading ink.
    val ink by animateColorAsState(
        targetValue = if (active) colors.textPrimary else colors.textSecondary,
        animationSpec = spec,
        label = "chipInk",
    )

    val faceStyle = remember(fill, edge) {
        Style {
            background(fill)
            borderColor(edge)
        }
    }
    val rimStyle = remember(edge) { Style { background(edge) } }

    // A readout has nothing to press; clearing the semantics stops it being announced as a control.
    val staticSemantics = if (onClick == null) {
        Modifier.clearAndSetSemantics { contentDescription = label }
    } else {
        Modifier
    }

    AwanButton(
        onClick = onClick ?: {},
        modifier = staticSemantics.then(modifier),
        style = faceStyle,
        rimStyle = rimStyle,
        variant = AwanButtonVariant.Chip,
    ) {
        leading()
        Spacer(Modifier.width(AwanTheme.spacing.xs))
        AwanText(text = label, style = AwanTheme.styles.buttonCompactText.copy(color = ink))
    }
}

@Composable
fun AwanChipDot(tone: Color, active: Boolean = true) {
    val color by animateColorAsState(
        targetValue = if (active) tone else AwanTheme.colors.line,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "chipDot",
    )
    Box(
        Modifier
            .size(AwanChipDotSize)
            .clip(CircleShape)
            .background(color),
    )
}
