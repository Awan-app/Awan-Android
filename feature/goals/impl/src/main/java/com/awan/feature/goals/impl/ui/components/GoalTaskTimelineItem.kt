package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2

@Composable
internal fun GoalTaskTimelineItem(
    task: Task,
    accentColor: Color,
    isCompleting: Boolean,
    isGoalAchieved: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    val isCompleted = task.status == TaskStatus.COMPLETED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            if (isCompleting) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(2.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else {
                CalendarStyleCheckbox(
                    isCompleted = isCompleted,
                    categoryColor = accentColor,
                    onClick = { if (!isGoalAchieved) onToggle() }
                )
            }
        }
        
        // Content Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = spacing.xl)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = task.title,
                    style = AwanTheme.typography.body.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) colors.textSecondary else colors.textPrimary,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    if (!isGoalAchieved) {
                        AwanIconButton(
                            onClick = onDelete,
                            contentDescription = stringResource(R.string.goals_dialog_delete_task_title),
                            icon = {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.line.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AwanText(
                            text = stringResource(R.string.goals_task_duration_format, task.estimatedDurationMinutes),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
            
            val taskDescription = task.description
            if (!taskDescription.isNullOrBlank()) {
                AwanText(
                    text = taskDescription,
                    style = AwanTheme.typography.caption.copy(
                        color = colors.textSecondary
                    ),
                    maxLines = 2
                )
            }
            
            // Category Chip
            task.category?.let { category ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.line.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AwanText(
                            text = category.name,
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
