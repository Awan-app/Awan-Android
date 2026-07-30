package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

/**
 * A single task row inside an expanded goal card.
 * Shows a colored dot bullet followed by the task title.
 * Completed tasks are rendered with a strikethrough.
 */
@Composable
internal fun GoalTaskRow(
    title: String,
    isCompleted: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        // Colored dot bullet
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accentColor),
        )
        Spacer(modifier = Modifier.width(10.dp))
        AwanText(
            text = title,
            style = AwanTheme.typography.body.copy(
                fontSize = 13.5.sp,
                color = AwanTheme.colors.textSecondary,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
