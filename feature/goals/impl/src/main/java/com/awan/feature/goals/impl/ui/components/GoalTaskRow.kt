package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskStatus

/**
 * A single task row inside an expanded goal card.
 * Shows a colored dot bullet followed by the task title.
 * Completed tasks are rendered with a strikethrough.
 */
@Composable
internal fun GoalTaskRow(
    tws: TaskWithSessions,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val task = tws.task
    val isCompleted = task.status == TaskStatus.COMPLETED
    val colors = AwanTheme.colors
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored dot bullet
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) colors.line else accentColor),
            )
            Spacer(modifier = Modifier.width(10.dp))
            AwanText(
                text = task.title,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = if (isCompleted) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (!isCompleted && tws.totalCount > 0) {
                AwanText(
                    text = "${(tws.progress * 100).toInt()}%",
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }
        }

        // Mini progress bar for the task (if it's not already completed)
        if (!isCompleted && tws.totalCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 18.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                ) {
                    val trackH = size.height
                    val radius = trackH / 2f
                    val progressWidth = size.width * tws.progress.coerceIn(0f, 1f)

                    drawRoundRect(
                        color = colors.line.copy(alpha = 0.5f),
                        cornerRadius = CornerRadius(radius)
                    )

                    if (progressWidth > 0f) {
                        drawRoundRect(
                            color = accentColor,
                            size = Size(progressWidth, trackH),
                            cornerRadius = CornerRadius(radius)
                        )
                    }
                }
            }
        }
    }
}
