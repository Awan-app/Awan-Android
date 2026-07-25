package com.awan.feature.goals.impl.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.model.Goal
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import androidx.compose.material3.CircularProgressIndicator

// ── Entry composable (stateful) ───────────────────────────────────────────────

@Composable
fun GoalsRoute(
    viewModel: GoalsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GoalsScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen (stateless) ────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
) {
    val colors = AwanTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Sky header ────────────────────────────────────────────────────────
        GoalsHeader()

        // ── Tab row ───────────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        GoalsTabRow(
            selected = state.tab,
            activeCount = state.activeGoals.size,
            completedCount = state.completedGoals.size,
            onSelect = { onAction(GoalsAction.TabSelected(it)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ── Goal list ─────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            },
            label = "goalsTab",
        ) { tab ->
             if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AwanTheme.colors.zoneSky)
        }
    } else if (state.tab == GoalsTab.Active && state.activeGoals.isEmpty()) {
        GoalsEmptyState(tab = state.tab)
    } else if (state.tab == GoalsTab.Completed && state.completedGoals.isEmpty()) {
        GoalsEmptyState(tab = state.tab)
    } else {
        GoalsList(
            goals = if (state.tab == GoalsTab.Active) state.activeGoals else state.completedGoals,
            tab = state.tab,
        )
    }
        }
    }
}

// ── Sky header with mascot ────────────────────────────────────────────────────

@Composable
private fun GoalsHeader() {
    val colors = AwanTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.backgroundStart, colors.background),
                ),
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Decorative side clouds (simple white circles)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 24.dp)
                .size(width = 80.dp, height = 50.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 16.dp)
                .size(width = 70.dp, height = 44.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.55f)),
        )

        // Mascot
        AwanMascot(
            expression = MascotExpression.Greet,
            width = 110.dp,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Screen title
        Text(
            text = "My Goals",
            style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
        )
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────────

@Composable
private fun GoalsTabRow(
    selected: GoalsTab,
    activeCount: Int,
    completedCount: Int,
    onSelect: (GoalsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(colors.line)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        enumValues<GoalsTab>().forEach { tab ->
            val isSelected = tab == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) colors.surface else Color.Transparent,
                label = "tabBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                label = "tabText",
            )
            val label = when (tab) {
                GoalsTab.Active -> "Active  $activeCount"
                GoalsTab.Completed -> "Done  $completedCount"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(tab) },
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = AwanTheme.typography.body.copy(color = textColor),
                )
            }
        }
    }
}

// ── Goals list ────────────────────────────────────────────────────────────────

@Composable
private fun GoalsList(
    goals: List<Goal>,
    tab: GoalsTab,
) {
    if (goals.isEmpty()) {
        GoalsEmptyState(tab)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(goals, key = { it.id }) { goal ->
            GoalCard(goal = goal, isCompleted = tab == GoalsTab.Completed)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goal: Goal,
    isCompleted: Boolean,
) {
    val colors = AwanTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }

    val progressAnim by animateFloatAsState(
        targetValue = goal.progress,
        animationSpec = tween(600),
        label = "goalProgress",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "chevron",
    )

    // Zone accent color — rotates through the palette by goal id
    val accentColor = when (goal.id.hashCode().mod(6)) {
        0 -> colors.zoneSky
        1 -> colors.zoneViolet
        2 -> colors.zoneTangerine
        3 -> colors.zoneCoral
        4 -> colors.zoneSun
        else -> colors.zoneLavender
    }

    // Tasks sorted: incomplete first, completed at the bottom (Todoist / Things 3 convention)
    val sortedTasks = remember(goal.tasks) {
        goal.tasks.sortedBy { it.status == TaskStatus.COMPLETED }
    }

    AwanCard(modifier = Modifier.fillMaxWidth()) {
        // ── Title row + chevron ───────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
        ) {
            Text(text = goal.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = goal.title,
                style = AwanTheme.typography.heading.copy(
                    color = if (isCompleted) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            ChevronIcon(
                tint = colors.meta,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Progress bar ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = if (isCompleted) colors.zoneSun else accentColor,
                trackColor = colors.line,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${(goal.progress * 100).toInt()}%",
                style = AwanTheme.typography.caption.copy(color = colors.textSecondary),
            )
        }

        // Task count summary
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${goal.completedTasks} of ${goal.totalTasks} tasks done",
            style = AwanTheme.typography.caption.copy(color = colors.meta),
        )

        // ── Collapsible task list ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(150)),
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.line, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                sortedTasks.forEach { task ->
                    TaskRow(task = task, accentColor = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Single task row ───────────────────────────────────────────────────────────

@Composable
private fun TaskRow(
    task: Task,
    accentColor: Color,
) {
    val isTaskCompleted = task.status == TaskStatus.COMPLETED
    val colors = AwanTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Circular checkbox indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (isTaskCompleted) accentColor.copy(alpha = 0.18f)
                    else colors.line,
                ),
        ) {
            if (isTaskCompleted) {
                // Filled dot for done
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(accentColor),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = task.title,
            style = AwanTheme.typography.body.copy(
                color = if (isTaskCompleted) colors.textSecondary else colors.textPrimary,
                textDecoration = if (isTaskCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Chevron icon (canvas-drawn, no material-icons dep) ───────────────────────

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
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.8f, h * 0.35f)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokePx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────


@Composable
private fun GoalsEmptyState(tab: GoalsTab) {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 90.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (tab) {
                GoalsTab.Active -> "No active goals yet"
                GoalsTab.Completed -> "No completed goals yet"
            },
            style = AwanTheme.typography.heading.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (tab) {
                GoalsTab.Active -> "Tap + to set your first goal!"
                GoalsTab.Completed -> "Finish an active goal to see it here."
            },
            style = AwanTheme.typography.body.copy(color = colors.textSecondary),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF4FAFF)
@Composable
private fun GoalsScreenPreview() {
    AwanTheme {
        GoalsScreen(
            state = GoalsState(
                isLoading = false,
                tab = GoalsTab.Active,
                activeGoals = emptyList(),
                completedGoals = emptyList(),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4FAFF)
@Composable
private fun GoalsScreenCompletedPreview() {
    AwanTheme {
        GoalsScreen(
            state = GoalsState(
                isLoading = false,
                tab = GoalsTab.Completed,
                activeGoals = emptyList(),
                completedGoals = emptyList(),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4FAFF)
@Composable
private fun GoalsEmptyStatePreview() {
    AwanTheme {
        GoalsScreen(
            state = GoalsState(
                isLoading = false,
                tab = GoalsTab.Active,
                activeGoals = emptyList(),
                completedGoals = emptyList(),
            ),
            onAction = {},
        )
    }
}
