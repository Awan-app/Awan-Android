package com.awan.feature.addtask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion

private val ChipBorder = 2.dp
private val DotSize = 8.dp
private const val PRESSED_SCALE = 0.95f
private const val IDLE_TINT_ALPHA = 0.10f
private const val ACTIVE_TINT_ALPHA = 0.16f

/**
 * A task attribute as a pressable rim pill: a coloured dot, its value, and a 2dp border in the same
 * tone — the same chunky surface-on-rim language as [com.awan.app.core.designsystem.AwanCard],
 * scaled down. [active] is the difference between a value the sentence actually stated and a
 * default standing in for one, which reads as a filled border versus a quiet one.
 */
@Composable
fun AttributeChip(
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val reduced = reducedMotion()
    val colors = AwanTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val border by animateColorAsState(
        targetValue = if (active) tone else colors.line,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "chipBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (active) tone.copy(alpha = ACTIVE_TINT_ALPHA) else colors.surface,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "chipFill",
    )
    val ink by animateColorAsState(
        targetValue = if (active) tone else colors.textSecondary,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "chipInk",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) PRESSED_SCALE else 1f,
        animationSpec = if (reduced) snap() else AwanTheme.motion.playful.spec(),
        label = "chipPress",
    )

    val pressModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.selectable(
            selected = active,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        )
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AwanTheme.shapes.pill)
            .background(fill)
            .border(ChipBorder, border, AwanTheme.shapes.pill)
            .then(pressModifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        Box(
            Modifier
                .size(DotSize)
                .clip(CircleShape)
                .background(if (active) tone else colors.line.copy(alpha = 1f - IDLE_TINT_ALPHA)),
        )
        AwanText(text = label, style = AwanTheme.styles.buttonCompactText.copy(color = ink))
    }
}
