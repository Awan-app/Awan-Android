package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun LivingTimePointer(
    pointerY: Dp,
    currentTimeFormatted: String,
    activePointerColor: Color = AwanTheme.colors.sky,
    pulseScale: Float = 1f,
    pulseAlpha: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val trackXDp = 52.dp

    val liveColor by animateColorAsState(
        targetValue = activePointerColor,
        animationSpec = tween(durationMillis = 300),
        label = "livePointerColor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "liveCirclePulse")

    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auraScale",
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auraAlpha",
    )

    val dotPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulseScale",
    )

    Box(modifier = modifier.fillMaxWidth().zIndex(50f)) {
        Box(
            modifier = Modifier
                .offset(x = trackXDp - 10.dp, y = pointerY - 10.dp)
                .size(20.dp)
                .graphicsLayer {
                    scaleX = auraScale
                    scaleY = auraScale
                    alpha = auraAlpha
                }
                .clip(CircleShape)
                .background(liveColor)
                .zIndex(50f),
        )

        Box(
            modifier = Modifier
                .offset(x = trackXDp - 6.dp, y = pointerY - 6.dp)
                .size(12.dp)
                .graphicsLayer {
                    scaleX = dotPulseScale
                    scaleY = dotPulseScale
                }
                .shadow(4.dp, CircleShape, spotColor = liveColor)
                .clip(CircleShape)
                .background(liveColor)
                .border(2.dp, Color.White, CircleShape)
                .zIndex(52f),
        )
    }
}
