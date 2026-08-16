package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R

@Composable
internal fun GoalTasksHeader(
    goal: Goal,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(top = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                text = stringResource(R.string.goals_tasks_label),
                style = AwanTheme.typography.title.copy(
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
            )
            val dependentCount = goal.tasks.count { it.dependsOnTaskIds.isNotEmpty() }
            val independentCount = goal.totalTasks - dependentCount
            val independent = stringResource(R.string.goals_independent_count, independentCount)
            val dependent = stringResource(R.string.goals_dependent_count, dependentCount)
            AwanText(
                text = stringResource(R.string.goals_tasks_summary_format, independent, dependent),
                style = AwanTheme.typography.caption.copy(
                    color = colors.textSecondary
                )
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AwanText(
                    text = goal.totalTasks.toString(),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }
        }
    }
}
