package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Full-width segmented progress: [count] pills, the first [current] filled sky. */
@Composable
fun AwanStepProgress(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            Segment(filled = index < current)
        }
    }
}

@Composable
private fun RowScope.Segment(filled: Boolean) {
    val color by animateColorAsState(
        if (filled) AwanTheme.colors.sky else AwanTheme.colors.line,
        label = "segment",
    )
    androidx.compose.foundation.layout.Box(
        Modifier
            .weight(1f)
            .height(7.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(color),
    )
}
