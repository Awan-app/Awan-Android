package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R

@Composable
internal fun GoalProgressCard(goal: Goal, accentColor: Color) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    val lineColor = colors.line
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AwanText(
                text = stringResource(R.string.goals_progress_label),
                style = AwanTheme.typography.title.copy(
                    fontSize = 16.sp,
                    color = colors.textSecondary
                )
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                AwanText(
                    text = stringResource(R.string.goals_progress_percentage, (goal.progress * 100).toInt()),
                    style = AwanTheme.typography.title.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                )
                AwanText(
                    text = stringResource(R.string.goals_progress_format, goal.completedTasks, goal.totalTasks),
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                )
            }
            
            // Progress Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                val trackH = size.height
                val radius = trackH / 2f
                val progressWidth = size.width * goal.progress.coerceIn(0f, 1f)
                
                drawRoundRect(
                    color = lineColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                )
                
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = accentColor,
                        size = androidx.compose.ui.geometry.Size(progressWidth, trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                    )
                }
            }
        }
    }
}
