package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.MascotExpression

/**
 * Header area shown at the top of the Goals screen:
 * Awan mascot centred with two decorative cloud shapes on each side.
 */
@Composable
internal fun GoalsMascotHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Left decorative cloud
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(width = 72.dp, height = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.65f)),
        )
        // Right decorative cloud
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(width = 72.dp, height = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.65f)),
        )
        // Main mascot (gently floating animation built into AwanMascot)
        AwanMascot(
            expression = MascotExpression.Greet,
            width = 100.dp,
        )
    }
}
