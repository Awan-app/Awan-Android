package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2

@Composable
internal fun GoalDetailsTopBar(
    title: String,
    isAchieved: Boolean,
    onBack: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    val editDescription = stringResource(R.string.goals_edit_title)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            AwanBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(spacing.sm))
            AwanText(
                text = title,
                style = AwanTheme.typography.title.copy(
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    textDecoration = if (isAchieved) TextDecoration.LineThrough else null
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isAchieved) {
            AwanButton(
                onClick = onEditClick,
                variant = AwanButtonVariant.Secondary,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = editDescription
                }
            ) {
                Icon(
                    imageVector = Lucide.Pencil,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun GoalDetailsContent(
    goal: Goal,
    isAchieved: Boolean,
    isDeleting: Boolean,
    completingTaskIds: Set<String>,
    onDeleteGoalClick: () -> Unit,
    onDeleteTaskClick: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    onTaskToggle: (String) -> Unit
) {
    val spacing = AwanTheme.spacing
    val accentColor = goalAccentColor(goal.id.hashCode())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item {
            GoalHeaderCard(goal = goal, accentColor = accentColor)
        }
        
        item {
            GoalProgressCard(goal = goal, accentColor = accentColor)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoalTasksHeader(
                    goal = goal, 
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                
                if (!isAchieved) {
                    AwanIconButton(
                        onClick = onAddTaskClick,
                        contentDescription = stringResource(R.string.goals_action_add_task),
                        icon = {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
        
        itemsIndexed(goal.tasks, key = { _, task -> task.id }) { index, task ->
            GoalTaskTimelineItem(
                task = task,
                accentColor = accentColor,
                isCompleting = completingTaskIds.contains(task.id),
                isGoalAchieved = isAchieved,
                onToggle = { onTaskToggle(task.id) },
                onDelete = { onDeleteTaskClick(task.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(spacing.xl))
            AwanButton(
                onClick = onDeleteGoalClick,
                modifier = Modifier.fillMaxWidth(),
                variant = AwanButtonVariant.Destructive,
                isLoading = isDeleting,
                icon = Lucide.Trash2
            ) {
                AwanText(text = stringResource(R.string.goals_remove_label))
            }
        }
        
        item { Spacer(modifier = Modifier.height(140.dp)) }
    }
}
