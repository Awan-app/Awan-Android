package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.feature.goals.impl.presentation.GoalsState
import com.awan.feature.goals.impl.presentation.GoalsTab
import com.awan.feature.goals.impl.ui.components.CompletedGoalColor
import com.awan.feature.goals.impl.ui.components.GoalCard
import com.awan.feature.goals.impl.ui.components.GoalsMascotHeader
import com.awan.feature.goals.impl.ui.components.GoalsTabRow
import com.awan.feature.goals.impl.ui.components.goalAccentColor

/**
 * Root Goals screen.
 *
 * Assembles the mascot header, tab row, and the goal list (or empty / error state).
 * All sub-composables live in [com.awan.feature.goals.impl.ui.components].
 */
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFCFEEFF),
                        Color(0xFFE8F5FF),
                        Color(0xFFF4FAFF),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = "My Goals",
                    style = AwanTheme.typography.title.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.ink,
                    ),
                )
            }

            // ── Mascot header ─────────────────────────────────────────────────
            GoalsMascotHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Tab row ───────────────────────────────────────────────────────
            GoalsTabRow(
                selectedTab = state.tab,
                activeCount = state.activeGoals.size,
                completedCount = state.completedGoals.size,
                onTabSelected = { onAction(GoalsAction.TabSelected(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    val goals = when (state.tab) {
                        GoalsTab.Active -> state.activeGoals
                        GoalsTab.Completed -> state.completedGoals
                    }
                    val isCompletedTab = state.tab == GoalsTab.Completed

                    if (goals.isEmpty()) {
                        GoalsEmptyState(tab = state.tab)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                                GoalCard(
                                    goal = goal,
                                    accentColor = if (isCompletedTab) CompletedGoalColor
                                                  else goalAccentColor(index),
                                    isCompleted = isCompletedTab,
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}
