package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.goals.impl.presentation.*
import com.awan.feature.goals.impl.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState,
    events: kotlinx.coroutines.flow.Flow<InboxEvent>,
    onAction: (InboxAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ObserveAsEvents(events) { event ->
        when (event) {
            InboxEvent.NavigateBack -> onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            InboxTopBar(onBackClick = { onAction(InboxAction.BackClicked) })

            Spacer(modifier = Modifier.height(spacing.xs))

            // Search Bar with Filter Button
            Box(modifier = Modifier.padding(horizontal = spacing.md)) {
                GoalsSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onAction(InboxAction.SearchQueryChanged(it)) },
                    onFilterClick = { onAction(InboxAction.FilterClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
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
                        InboxErrorState(onRetry = { onAction(InboxAction.RetryClicked) })
                    }

                    state.visibleTasks.isEmpty() -> {
                        InboxEmptyState()
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            items(state.visibleTasks, key = { it.id }) { task ->
                                InboxTaskCard(
                                    task = task,
                                    isExpanded = state.expandedTaskId == task.id,
                                    onExpandToggle = { onAction(InboxAction.TaskExpandToggled(task.id)) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (state.showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { onAction(InboxAction.FilterDismissed) },
                sheetState = filterSheetState,
                containerColor = colors.surface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = spacing.md)
                            .size(width = 40.dp, height = 4.dp)
                            .background(colors.line, AwanTheme.shapes.pill)
                    )
                }
            ) {
                InboxFilterSheetContent(state = state, onAction = onAction)
            }
        }
    }
}
