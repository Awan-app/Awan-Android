package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.*

@Composable
internal fun InboxTopBar(
    onBackClick: () -> Unit
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AwanBackButton(onClick = onBackClick)
        
        Spacer(modifier = Modifier.width(spacing.md))
        
        AwanText(
            text = stringResource(R.string.inbox_title),
            style = AwanTheme.typography.title.copy(
                fontSize = 20.sp,
                color = colors.textPrimary
            )
        )
    }
}

@Composable
internal fun InboxFilterSheetContent(
    state: InboxUiState,
    onAction: (InboxAction) -> Unit
) {
    val spacing = AwanTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            AwanText(
                text = stringResource(R.string.inbox_filters_title),
                style = AwanTheme.styles.headingText
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AwanText(
                    text = stringResource(R.string.inbox_filter_task_status),
                    style = AwanTheme.styles.captionText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    InboxTaskDisplayStatus.entries.forEach { status ->
                        val isSelected = status in state.activeStatusFilters
                        val labelRes = when (status) {
                            InboxTaskDisplayStatus.Drafted -> R.string.inbox_status_drafted
                            InboxTaskDisplayStatus.Active -> R.string.inbox_status_active
                            InboxTaskDisplayStatus.Completed -> R.string.inbox_status_completed
                            InboxTaskDisplayStatus.Cancelled -> R.string.inbox_status_cancelled
                        }
                        AwanChip(
                            label = stringResource(labelRes),
                            active = isSelected,
                            tone = if (isSelected) AwanChipTone.Sky else AwanChipTone.Neutral,
                            onClick = { onAction(InboxAction.StatusFilterToggled(status)) }
                        )
                    }
                }
            }

            AwanButton(
                onClick = { onAction(InboxAction.FilterDismissed) },
                modifier = Modifier.fillMaxWidth()
            ) {
                AwanText(stringResource(R.string.inbox_filter_show_results))
            }
        }
    }
}
