package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.feature.goals.impl.presentation.GoalsState
import com.awan.feature.goals.impl.presentation.GoalsTab
import com.awan.feature.goals.impl.ui.components.GoalCard
import com.awan.feature.goals.impl.ui.components.GoalsMascotHeader
import com.awan.feature.goals.impl.ui.components.GoalsTabRow
import com.awan.feature.goals.impl.ui.components.completedGoalColor
import com.awan.feature.goals.impl.ui.components.goalAccentColor

@Composable
fun GoalsScreen(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AwanTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .statusBarsPadding(),
    ) {
        // ── Mascot header ─────────────────────────────────────────────────
        GoalsMascotHeader()

        Spacer(modifier = Modifier.height(spacing.md))

        // ── Tab row ───────────────────────────────────────────────────────
        GoalsTabRow(
            selectedTab = state.tab,
            activeCount = state.activeGoals.size,
            completedCount = state.completedGoals.size,
            onTabSelected = { onAction(GoalsAction.TabSelected(it)) },
            modifier = Modifier.padding(horizontal = spacing.xl)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        // ── Content area ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = AwanTheme.colors.sky,
                        strokeWidth = 3.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.isError -> {
                    GoalsErrorState(onRetry = { onAction(GoalsAction.RetryClicked) })
                }

                else -> {
                    val goals = when (state.tab) {
                        GoalsTab.Active -> state.activeGoals
                        GoalsTab.Completed -> state.completedGoals
                    }
                    val isCompletedTab = state.tab == GoalsTab.Completed

                    if (goals.isEmpty()) {
                        GoalsEmptyState(tab = state.tab)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = spacing.xl,
                                vertical = spacing.md
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                                GoalCard(
                                    goal = goal,
                                    accentColor = if (isCompletedTab) completedGoalColor()
                                    else goalAccentColor(index),
                                    isCompleted = isCompletedTab,
                                )
                            }
                            item { Spacer(modifier = Modifier.height(112.dp)) }
                        }
                    }
                }
            }
        }
    }
}

