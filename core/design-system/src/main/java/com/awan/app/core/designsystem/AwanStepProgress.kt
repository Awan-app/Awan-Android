package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Full-width segmented progress. A single spring-driven fraction is shared across all [count]
 * segments, so the fill pours from one into the next and overshoots before settling.
 */
@Composable
fun AwanStepProgress(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = current.toFloat(),
        animationSpec = AwanTheme.motion.bouncy.spec(),
        label = "stepProgress",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            Segment(index = index, justFilled = current == index + 1, progress = { progress })
        }
    }
}

@Composable
private fun RowScope.Segment(index: Int, justFilled: Boolean, progress: () -> Float) {
    val track = AwanTheme.colors.line
    val fill = AwanTheme.colors.sky
    val pulse = remember { Animatable(1f) }
    val spec = AwanTheme.motion.playful.spec<Float>()

    // Advancing again mid-pulse cancels this effect, which would strand the segment at whatever
    // scale it had reached. The else branch is the recovery: every restart drives back to rest.
    LaunchedEffect(justFilled) {
        if (justFilled) {
            pulse.animateTo(1.55f, spec)
        }
        pulse.animateTo(1f, spec)
    }

    Box(
        Modifier
            .weight(1f)
            .height(7.dp)
            .graphicsLayer { scaleY = pulse.value }
            .clip(RoundedCornerShape(99.dp))
            .background(track)
            .drawBehind {
                val fraction = (progress() - index).coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRect(color = fill, size = size.copy(width = size.width * fraction))
                }
            },
    )
}
