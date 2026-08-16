package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.GoalStatus
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalSearchType
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.feature.goals.impl.presentation.GoalsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalsFilterSheet(
    state: GoalsState,
    onAction: (GoalsAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.md)
                    .size(width = 40.dp, height = 4.dp)
                    .background(colors.line, CircleShape)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            AwanText(
                text = stringResource(R.string.goals_filter_title),
                style = AwanTheme.typography.title
            )

            // Status Filter
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AwanText(
                    text = stringResource(R.string.goals_filter_status),
                    style = AwanTheme.typography.heading.copy(fontSize = 16.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    AwanChip(
                        label = stringResource(R.string.goals_filter_all),
                        active = state.pendingFilters.status == null,
                        tone = if (state.pendingFilters.status == null) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingStatusFilterChanged(null)) }
                    )
                    AwanChip(
                        label = stringResource(R.string.goals_filter_active),
                        active = state.pendingFilters.status == GoalStatus.ACTIVE,
                        tone = if (state.pendingFilters.status == GoalStatus.ACTIVE) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingStatusFilterChanged(GoalStatus.ACTIVE)) }
                    )
                    AwanChip(
                        label = stringResource(R.string.goals_filter_completed),
                        active = state.pendingFilters.status == GoalStatus.ACHIEVED,
                        tone = if (state.pendingFilters.status == GoalStatus.ACHIEVED) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingStatusFilterChanged(GoalStatus.ACHIEVED)) }
                    )
                }
            }

            // Type Filter
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AwanText(
                    text = stringResource(R.string.goals_filter_type),
                    style = AwanTheme.typography.heading.copy(fontSize = 16.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    AwanChip(
                        label = stringResource(R.string.goals_filter_all),
                        active = state.pendingFilters.type == GoalSearchType.ALL,
                        tone = if (state.pendingFilters.type == GoalSearchType.ALL) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingTypeFilterChanged(GoalSearchType.ALL)) }
                    )
                    AwanChip(
                        label = stringResource(R.string.goals_filter_tasks),
                        active = state.pendingFilters.type == GoalSearchType.TASKS,
                        tone = if (state.pendingFilters.type == GoalSearchType.TASKS) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingTypeFilterChanged(GoalSearchType.TASKS)) }
                    )
                    AwanChip(
                        label = stringResource(R.string.goals_filter_goals),
                        active = state.pendingFilters.type == GoalSearchType.GOALS,
                        tone = if (state.pendingFilters.type == GoalSearchType.GOALS) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(GoalsAction.PendingTypeFilterChanged(GoalSearchType.GOALS)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                AwanButton(
                    onClick = { onAction(GoalsAction.ResetFiltersClicked) },
                    modifier = Modifier.weight(1f),
                    variant = AwanButtonVariant.Secondary
                ) {
                    AwanText(stringResource(R.string.goals_filter_reset))
                }
                AwanButton(
                    onClick = { onAction(GoalsAction.ApplyFiltersClicked) },
                    modifier = Modifier.weight(1f),
                    variant = AwanButtonVariant.Primary
                ) {
                    AwanText(stringResource(R.string.goals_filter_apply))
                }
            }
        }
    }
}
