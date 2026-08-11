package com.awan.feature.goals.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.feature.goals.impl.presentation.GoalsState
import com.awan.feature.goals.impl.ui.components.GoalCard
import com.awan.feature.goals.impl.ui.components.GoalsSearchBar
import com.awan.feature.goals.impl.ui.components.goalAccentColor

@Composable
fun GoalsScreen(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors

    var showAllActive by remember { mutableStateOf(false) }
    var showAllAchieved by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Search Bar ──────────────────────────────────────────────────
            GoalsSearchBar(
                query = state.searchQuery,
                onQueryChange = { onAction(GoalsAction.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
                onFilterClick = null // Only Inbox has filters for now
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Content area ──────────────────────────────────────────────────
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
                    GoalsErrorState(onRetry = { onAction(GoalsAction.RetryClicked) })
                }

                else -> {
                    val filteredGoals = state.filteredGoals
                    val activeGoals = filteredGoals.filter { it.status == GoalStatus.ACTIVE }
                    val achievedGoals = filteredGoals.filter { it.status == GoalStatus.ACHIEVED }

                    if (filteredGoals.isEmpty()) {
                        GoalsEmptyState()
                    } else {
                        val activeTitle = stringResource(R.string.goals_status_active)
                        val achievedTitle = stringResource(R.string.goals_status_completed)
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            goalSection(
                                title = activeTitle,
                                goals = activeGoals,
                                isExpanded = showAllActive,
                                onToggleExpand = { showAllActive = !showAllActive },
                                onGoalClick = { onAction(GoalsAction.GoalClicked(it)) }
                            )

                            item { Spacer(modifier = Modifier.height(8.dp)) }

                            goalSection(
                                title = achievedTitle,
                                goals = achievedGoals,
                                isExpanded = showAllAchieved,
                                onToggleExpand = { showAllAchieved = !showAllAchieved },
                                onGoalClick = { onAction(GoalsAction.GoalClicked(it)) }
                            )

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // ── Floating Clouds ─────────────────────────────────────────────
        com.awan.app.core.designsystem.AwanCloudsHorizon(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

private fun LazyListScope.goalSection(
    title: String,
    goals: List<Goal>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onGoalClick: (String) -> Unit,
) {
    if (goals.isEmpty()) return

    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            AwanText(
                text = title,
                style = AwanTheme.typography.title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.ink
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                AwanText(
                    text = goals.size.toString(),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.sky
                    )
                )
            }
        }
    }

    val visibleGoals = if (isExpanded) goals else goals.take(2)

    itemsIndexed(visibleGoals, key = { _, goal -> goal.id }) { index, goal ->
        GoalCard(
            goal = goal,
            accentColor = goalAccentColor(index),
            onClick = { onGoalClick(goal.id) },
            modifier = Modifier.animateItem()
        )
    }

    if (goals.size > 2) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AwanButton(
                    onClick = onToggleExpand,
                    variant = AwanButtonVariant.Quiet,
                ) {
                    AwanText(
                        text = if (isExpanded) "Show Less" else "Show More",
                        style = AwanTheme.typography.button.copy(fontSize = 13.sp)
                    )
                }
            }
        }
    }
}
