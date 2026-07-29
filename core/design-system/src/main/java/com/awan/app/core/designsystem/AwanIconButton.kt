package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Small rim square icon button (e.g. the onboarding back chevron). */
@Composable
fun AwanIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: HapticFeedbackType? = awanButtonHaptic(AwanButtonVariant.Secondary),
    icon: @Composable () -> Unit,
) {
    val colors = AwanTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val hapticFeedback = LocalHapticFeedback.current
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(semanticsModifier)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    haptic?.let(hapticFeedback::performHapticFeedback)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(38.dp)) {
            Box(Modifier.matchParentSize().clip(shape).background(colors.line))
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = AwanCardRimDepth)
                    .clip(shape)
                    .background(colors.surface)
                    .border(2.dp, colors.line, shape),
                contentAlignment = Alignment.Center,
                content = { icon() },
            )
        }
    }
}
