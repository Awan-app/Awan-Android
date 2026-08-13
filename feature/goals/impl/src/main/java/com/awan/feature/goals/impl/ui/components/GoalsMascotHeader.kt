package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCloud
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.MascotExpression

/**
 * Header area shown at the top of the Goals screen:
 * Awan mascot centred with decorative AwanCloud shapes.
 */
@Composable
internal fun GoalsMascotHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Left decorative cloud
        AwanCloud(
            size = 80.dp,
            baseColor = Color.White.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
        // Right decorative cloud
        AwanCloud(
            size = 80.dp,
            baseColor = Color.White.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        // Main mascot
        AwanMascot(
            expression = MascotExpression.Greet,
            width = 110.dp,
        )
    }
}
