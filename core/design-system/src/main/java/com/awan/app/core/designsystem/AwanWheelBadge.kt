package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.Lucide

private const val PULSE_MILLIS = 1_000
private const val FLOAT_MILLIS = 1_600
private val BadgeSize = 42.dp

/**
 * Premium 2D Gold Daily Gift Badge.
 *
 * Rendered continuously on screen:
 * - Active (`hasFreeSpin == true`): Shiny metallic gold/coin gradient with glowing aura & floating wobble.
 * - Claimed (`hasFreeSpin == false`): Muted/disabled metallic gift styling (keeps Gift icon!), still clickable to view today's claimed status overlay.
 */
@Composable
fun AwanWheelBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasFreeSpin: Boolean = true,
) {
    val reduced = reducedMotion()
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val transition = rememberInfiniteTransition(label = "wheelBadge")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced || !hasFreeSpin) 1f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wheelBadgePulse",
    )

    val floatY by transition.animateFloat(
        initialValue = -2f,
        targetValue = if (reduced || !hasFreeSpin) 0f else 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(FLOAT_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wheelBadgeFloat",
    )

    val colors = AwanTheme.colors
    val description = stringResource(R.string.ds_wheel_badge)

    val pressScale = if (isPressed) 0.92f else 1f
    val pressOffset = if (isPressed) 2.dp else 0.dp

    // Premium Gold Gradient for Active State vs Muted Metallic Gold-Grey Gradient for Claimed State
    val badgeGradient = if (hasFreeSpin) {
        Brush.verticalGradient(
            listOf(
                colors.pointsSurface,
                colors.pointsIcon,
                colors.streakIcon,
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                colors.disabledContent,
                colors.meta,
            )
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, (floatY.dp + pressOffset).roundToPx()) }
            .scale(pressScale)
            .size(BadgeSize + 8.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // Outer Glowing Gold Aura Pulse Ring (Active only)
        if (hasFreeSpin) {
            Box(
                modifier = Modifier
                    .size(BadgeSize * pulse)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    colors.pointsIcon.copy(alpha = 0.5f),
                                    Color.Transparent,
                                )
                            )
                        )
                    }
            )
        }

        // 2D Tactile Gold Gift Badge Body
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .clip(CircleShape)
                .background(if (hasFreeSpin) colors.ink else colors.meta)
                .padding(bottom = 2.5.dp)
                .clip(CircleShape)
                .background(badgeGradient)
                .border(
                    width = 2.dp,
                    color = if (hasFreeSpin) colors.pointsSurface else colors.line,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptics.performHapticFeedback(
                            if (hasFreeSpin) HapticFeedbackType.Confirm else HapticFeedbackType.ContextClick
                        )
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Gift,
                contentDescription = null,
                tint = if (hasFreeSpin) colors.ink else colors.meta,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
