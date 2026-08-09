package com.awan.app.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.Lucide

private const val PULSE_MILLIS = 1_100
private val BadgeSize = 42.dp

/**
 * The "you have a free spin" badge. Only shown when a spin is actually available, so its presence
 * is the whole message — a permanently visible wheel button would say nothing.
 *
 * It breathes gently to catch the eye without demanding a tap.
 */
@Composable
fun AwanWheelBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val transition = rememberInfiniteTransition(label = "wheelBadge")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wheelBadgePulse",
    )

    val colors = AwanTheme.colors
    val description = stringResource(R.string.ds_wheel_badge)

    Box(
        modifier = modifier
            .size(BadgeSize)
            .scale(pulse)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(colors.zoneSun, colors.zoneTangerine))
            )
            .border(2.dp, colors.surface, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Gift,
            contentDescription = null,
            tint = colors.surface,
            modifier = Modifier.size(22.dp),
        )
    }
}
