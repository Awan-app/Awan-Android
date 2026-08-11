package com.awan.feature.goals.impl.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
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

            // ── Goals Header ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AwanText(
                    text = stringResource(R.string.goals_title_count),
                    style = AwanTheme.typography.title.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                )
                
                if (state.goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colors.sky.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AwanText(
                            text = state.goals.size.toString(),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.sky
                            )
                        )
                    }
                }
            }

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
                    val goals = state.filteredGoals

                    if (goals.isEmpty()) {
                        GoalsEmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                                GoalCard(
                                    goal = goal,
                                    accentColor = goalAccentColor(index),
                                    onClick = { onAction(GoalsAction.GoalClicked(goal.id)) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
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
