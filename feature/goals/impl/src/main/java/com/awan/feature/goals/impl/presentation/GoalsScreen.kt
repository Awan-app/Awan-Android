package com.awan.feature.goals.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.model.Goal
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import com.awan.feature.goals.impl.R

@Composable
fun GoalsScreen(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            GoalsHeader()

            Spacer(modifier = Modifier.height(12.dp))

            GoalsTabRow(
                selectedTab = state.tab,
                onTabSelected = { onAction(GoalsAction.TabSelected(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = colors.sky,
                            strokeWidth = 3.dp,
                        )
                    }
                }

                state.isError -> {
                    GoalsErrorState(
                        onRetry = { onAction(GoalsAction.RetryClicked) },
                    )
                }

                else -> {
                    val goals = when (state.tab) {
                        GoalsTab.Active -> state.activeGoals
                        GoalsTab.Completed -> state.completedGoals
                    }

                    if (goals.isEmpty()) {
                        GoalsEmptyState(tab = state.tab)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(goals, key = { it.id }) { goal ->
                                GoalCard(goal = goal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AwanText(
            text = stringResource(R.string.goals_title),
            style = AwanTheme.typography.display.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textPrimary,
            ),
        )
    }
}

@Composable
private fun GoalsTabRow(
    selectedTab: GoalsTab,
    onTabSelected: (GoalsTab) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(AwanTheme.colors.surface)
            .border(1.dp, AwanTheme.colors.line, shape)
            .padding(4.dp),
    ) {
        GoalsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tabShape = RoundedCornerShape(8.dp)
            val text = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_tab_active)
                GoalsTab.Completed -> stringResource(R.string.goals_tab_completed)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(tabShape)
                    .background(if (isSelected) AwanTheme.colors.sky else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = text,
                    style = AwanTheme.typography.button.copy(
                        color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}



// ── Chevron icon (canvas-drawn to avoid dependency issues) ───────────────────
@Composable
private fun ChevronIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokePx = (2.5f * density).coerceAtLeast(2f)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.38f)
            lineTo(w * 0.5f, h * 0.63f)
            lineTo(w * 0.75f, h * 0.38f)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokePx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun GoalCard(goal: Goal) {
    val hasTasks = goal.tasks.isNotEmpty()
    var expanded by rememberSaveable(goal.id) { mutableStateOf(false) }
    val colors = AwanTheme.colors
    val cardShape = RoundedCornerShape(16.dp)

    val expandedStateText = if (expanded) {
        stringResource(R.string.goals_card_state_expanded)
    } else {
        stringResource(R.string.goals_card_state_collapsed)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, colors.line, cardShape),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (hasTasks) {
                            Modifier
                                .clickable { expanded = !expanded }
                                .semantics { stateDescription = expandedStateText }
                        } else Modifier
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AwanText(
                    text = goal.emoji,
                    style = AwanTheme.typography.heading.copy(fontSize = 24.sp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = goal.title,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (goal.totalTasks > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AwanText(
                            text = stringResource(
                                R.string.goals_progress_format,
                                goal.completedTasks,
                                goal.totalTasks,
                            ),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                            ),
                        )
                    }
                }

                if (goal.totalTasks > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AwanText(
                            text = stringResource(
                                R.string.goals_progress_percentage,
                                (goal.progress * 100).toInt(),
                            ),
                            style = AwanTheme.typography.button.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.sky,
                            ),
                        )
                        
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "chevronRotation"
                        )
                        ChevronIcon(
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            if (goal.totalTasks > 0) {
                LinearProgressIndicator(
                    progress = { goal.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = colors.sky,
                    trackColor = colors.line.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round,
                )
            }

            AnimatedVisibility(visible = hasTasks && expanded) {
                val sortedTasks = remember(goal.tasks) {
                    goal.tasks.sortedBy { it.status == TaskStatus.COMPLETED }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    HorizontalDivider(color = colors.line, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    sortedTasks.forEach { task ->
                        TaskRow(task = task)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val colors = AwanTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (isCompleted) colors.sky.copy(alpha = 0.2f) else colors.line,
                ),
        ) {
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.sky),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        AwanText(
            text = task.title,
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = if (isCompleted) colors.textSecondary else colors.textPrimary,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GoalsEmptyState(
    tab: GoalsTab,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val expression = when (tab) {
            GoalsTab.Active -> MascotExpression.Curious
            GoalsTab.Completed -> MascotExpression.Celebrate
        }
        AwanMascot(expression = expression, width = 90.dp)
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_empty_active_title)
                GoalsTab.Completed -> stringResource(R.string.goals_empty_completed_title)
            },
            style = AwanTheme.typography.heading.copy(
                fontSize = 18.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AwanText(
            text = when (tab) {
                GoalsTab.Active -> stringResource(R.string.goals_empty_active_subtitle)
                GoalsTab.Completed -> stringResource(R.string.goals_empty_completed_subtitle)
            },
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun GoalsErrorState(onRetry: () -> Unit) {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(text = "☁️", style = AwanTheme.typography.display.copy(fontSize = 48.sp))
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.goals_error_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 16.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AwanButton(
            onClick = onRetry,
            variant = AwanButtonVariant.Primary,
        ) {
            AwanText(
                text = stringResource(R.string.goals_error_retry),
                style = AwanTheme.typography.button,
            )
        }
    }
}
