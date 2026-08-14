package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.style.styleable
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.feature.goals.impl.presentation.GoalsState
import com.awan.feature.goals.impl.ui.components.*

@Composable
fun GoalsScreen(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Top Bar ─────────────────────────────────────────────────────
            GoalsTopBar(
                inboxTaskCount = state.inboxTaskCount,
                onInboxClick = { onAction(GoalsAction.InboxClicked) }
            )

            // ── Search Bar ──────────────────────────────────────────────────
            GoalsSearchBar(
                query = state.searchQuery,
                onQueryChange = { onAction(GoalsAction.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = spacing.md),
                isFilterActive = state.isAnyFilterApplied,
                onFilterClick = { onAction(GoalsAction.FilterClicked) }
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            // ── Content area ──────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = colors.sky,
                        )
                    }
                }

                state.isError -> {
                    GoalsErrorState(onRetry = { onAction(GoalsAction.RetryClicked) })
                }

                else -> {
                    val filteredGoals = state.filteredGoals
                    
                    if (state.goals.isEmpty()) {
                        GoalsEmptyState()
                    } else if (filteredGoals.isEmpty()) {
                        // Filtered Empty State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AwanText(
                                text = "No matching results",
                                style = AwanTheme.typography.heading.copy(color = colors.textPrimary)
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            AwanText(
                                text = "Try changing or clearing your filters.",
                                style = AwanTheme.typography.body.copy(
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(spacing.lg))
                            AwanButton(
                                onClick = { onAction(GoalsAction.ClearFiltersClicked) },
                                variant = AwanButtonVariant.Secondary
                            ) {
                                AwanText("Clear Filters")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                            contentPadding = PaddingValues(bottom = 140.dp)
                        ) {
                            itemsIndexed(filteredGoals, key = { _, goal -> goal.id }) { _, goal ->
                                GoalCard(
                                    goal = goal,
                                    onClick = { onAction(GoalsAction.GoalClicked(goal.id)) },
                                    onAction = onAction,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Filter Bottom Sheet ─────────────────────────────────────────────
        if (state.isFilterSheetOpen) {
            GoalsFilterSheet(
                state = state,
                onAction = onAction,
                onDismiss = { onAction(GoalsAction.DismissFilterSheet) }
            )
        }
    }
}
