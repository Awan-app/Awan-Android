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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

val AwanChipDotSize = 8.dp

/** Small sky-tinted pill for reassurances / hints (optionally with a leading icon). */
@Composable
fun AwanChip(
    label: String,
    tone: AwanChipTone,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    leading: @Composable () -> Unit = { AwanChipDot(tone = tone, active = active) },
    onClick: (() -> Unit)? = null,
) {
    val colors = AwanTheme.colors
    val (ink, fill, edge) = when (tone) {
        AwanChipTone.Sky -> Triple(colors.skyPressed, colors.sky.copy(alpha = 0.12f), colors.sky.copy(alpha = 0.24f))
        AwanChipTone.Violet -> Triple(colors.zoneViolet, colors.zoneViolet.copy(alpha = 0.14f), colors.zoneViolet.copy(alpha = 0.28f))
        AwanChipTone.Tangerine -> Triple(colors.zoneTangerine, colors.zoneTangerine.copy(alpha = 0.14f), colors.zoneTangerine.copy(alpha = 0.28f))
        AwanChipTone.Neutral -> Triple(colors.textSecondary, colors.disabledSurface, colors.line)
    }

    val rimStyle = remember(edge) { Style { background(edge) } }
    val faceStyle = remember(ink, fill, edge) {
        Style {
            background(fill)
            borderColor(edge)
            contentColor(ink)
        }
    }

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
fun AwanChipDot(tone: AwanChipTone, active: Boolean = true) {
    val colors = AwanTheme.colors
    val dotColor = when (tone) {
        AwanChipTone.Sky -> colors.skyPressed
        AwanChipTone.Violet -> colors.zoneViolet
        AwanChipTone.Tangerine -> colors.zoneTangerine
        AwanChipTone.Neutral -> colors.textSecondary
    }

    val color by animateColorAsState(
        targetValue = if (active) dotColor else colors.line,
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
