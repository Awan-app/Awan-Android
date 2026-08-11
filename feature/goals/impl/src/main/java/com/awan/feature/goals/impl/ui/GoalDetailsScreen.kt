package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalDetailsAction
import com.awan.feature.goals.impl.presentation.GoalDetailsState
import com.awan.feature.goals.impl.ui.components.goalAccentColor
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tag

@Composable
fun GoalDetailsScreen(
    state: GoalDetailsState,
    onAction: (GoalDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwanTheme.colors

    Scaffold(
        topBar = {
            GoalDetailsTopBar(
                title = state.goal?.title ?: "",
                onBack = { onAction(GoalDetailsAction.Back) }
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.sky
                    )
                }
                state.goal != null -> {
                    GoalDetailsContent(goal = state.goal)
                }
                state.error != null -> {
                    AwanText(
                        text = state.error,
                        modifier = Modifier.align(Alignment.Center),
                        style = AwanTheme.typography.body
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalDetailsTopBar(
    title: String,
    onBack: () -> Unit
) {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundStart, colors.background)
                )
            )
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            AwanCloudsHorizon(
                modifier = Modifier.fillMaxSize()
            )
            AwanMascot(
                expression = MascotExpression.Curious,
                width = 110.dp,
                blinkEnabled = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AwanBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AwanText(
                text = title,
                style = AwanTheme.typography.title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoalDetailsContent(
    goal: Goal
) {
    val accentColor = goalAccentColor(0) // Default for now

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GoalHeaderCard(goal = goal)
        }
        
        item {
            GoalProgressCard(goal = goal, accentColor = accentColor)
        }
        
        item {
            GoalTasksHeader(goal = goal)
        }
        
        itemsIndexed(goal.tasks, key = { _, task -> task.id }) { index, task ->
            GoalTaskTimelineItem(
                task = task,
                index = index + 1,
                isLast = index == goal.tasks.lastIndex,
                accentColor = accentColor
            )
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun GoalHeaderCard(goal: Goal) {
    val colors = AwanTheme.colors
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AwanText(
                    text = goal.title,
                    style = AwanTheme.typography.title.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.sky.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AwanText(
                        text = stringResource(R.string.goals_status_active),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.sky
                        )
                    )
                }
            }
            
            val description = goal.description
            if (!description.isNullOrBlank()) {
                AwanText(
                    text = description,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                )
            }
            
            val targetDate = goal.targetDate
            if (!targetDate.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Calendar,
                        contentDescription = null,
                        tint = colors.sky,
                        modifier = Modifier.size(16.dp)
                    )
                    AwanText(
                        text = targetDate,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(goal: Goal, accentColor: Color) {
    val colors = AwanTheme.colors
    val lineColor = colors.line
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AwanText(
                text = stringResource(R.string.goals_progress_label),
                style = AwanTheme.typography.title.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun GoalTasksHeader(goal: Goal) {
    val colors = AwanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            AwanText(
                text = stringResource(R.string.goals_tasks_label),
                style = AwanTheme.typography.title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
            )
            val independent = stringResource(R.string.goals_independent_count, 1) // TODO: real logic
            val dependent = stringResource(R.string.goals_dependent_count, goal.totalTasks - 1)
            AwanText(
                text = stringResource(R.string.goals_tasks_summary_format, independent, dependent),
                style = AwanTheme.typography.caption.copy(
                    color = colors.textSecondary
                )
            )
        }
        
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.sky.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AwanText(
                text = goal.totalTasks.toString(),
                style = AwanTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.sky
                )
            )
        }
    }
}

@Composable
private fun GoalTaskTimelineItem(
    task: Task,
    index: Int,
    isLast: Boolean,
    accentColor: Color
) {
    val colors = AwanTheme.colors
    val isCompleted = task.status == TaskStatus.COMPLETED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) accentColor else colors.surface)
                    .border(1.dp, if (isCompleted) accentColor else colors.line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AwanText(
                    text = index.toString(),
                    style = AwanTheme.typography.caption.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.White else colors.textSecondary
                    )
                )
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp) // Adjust based on content
                        .background(colors.line)
                )
            }
        }
        
        // Content Column
        Column(
            modifier = Modifier.weight(1f).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        color = colors.ink,
                        textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                // Duration Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.line.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    AwanText(
                        text = stringResource(R.string.goals_task_duration_format, task.estimatedDurationMinutes),
                        style = AwanTheme.typography.caption.copy(fontSize = 10.sp)
                    )
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.line.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AwanText(
                            text = category.name,
                            style = AwanTheme.typography.caption.copy(fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
