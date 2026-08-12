package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.feature.addtask.R
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide

@Composable
fun GoalPreviewSummaryCard(
    text: String,
    expanded: Boolean,
    expandLabel: String,
    collapseLabel: String,
    onToggleExpanded: () -> Unit,
) {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        background = AwanTheme.colors.surface,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            AwanText(
                text = text,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                style = AwanTheme.styles.bodySecondaryText,
            )
            AwanButton(
                onClick = onToggleExpanded,
                variant = AwanButtonVariant.Quiet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(if (expanded) collapseLabel else expandLabel)
            }
        }
    }
}

@Composable
fun GoalPreviewProposalCard(proposal: GoalProposal) {
    AwanCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
            AwanText(
                text = proposal.title,
                style = AwanTheme.styles.headingText,
            )

            proposal.description?.takeIf { it.isNotBlank() }?.let { description ->
                AwanText(description, style = AwanTheme.styles.bodySecondaryText)
            }

            proposal.targetDate?.takeIf { it.isNotBlank() }?.let { targetDate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
                ) {
                    Icon(
                        imageVector = Lucide.Calendar,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(16.dp),
                    )
                    AwanText(
                        text = stringResource(R.string.add_task_goal_preview_target_date, targetDate),
                        style = AwanTheme.styles.metaText,
                    )
                }
            }

            if (proposal.tasks.isNotEmpty()) {
                AwanText(
                    text = stringResource(R.string.add_task_goal_preview_tasks_header, proposal.tasks.size),
                    style = AwanTheme.typography.body.copy(
                        color = AwanTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                    proposal.tasks.forEachIndexed { index, task ->
                        GoalPreviewTaskRow(index = index + 1, task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPreviewTaskRow(index: Int, task: ProposedTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AwanTheme.colors.background, AwanTheme.shapes.chip)
            .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(AwanTheme.spacing.md)
                .background(AwanTheme.colors.sky, AwanTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            AwanText(
                text = index.toString(),
                style = AwanTheme.styles.buttonCompactText.copy(color = AwanTheme.colors.background),
            )
        }

        AwanText(
            text = task.title,
            style = AwanTheme.typography.body.copy(
                color = AwanTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            task.estimatedDuration?.let { duration ->
                AwanText(
                    text = stringResource(
                        R.string.add_task_goal_preview_duration,
                        durationLabel(duration),
                    ),
                    style = AwanTheme.styles.metaText,
                )
            }
            task.estimatedPoints?.let { points ->
                AwanText(
                    text = stringResource(R.string.add_task_goal_preview_points, points),
                    style = AwanTheme.styles.metaText,
                )
            }
        }
    }
}

private val previewProposal = GoalProposal(
    title = "Host a dinner party for four friends",
    description = "Plan, shop for, and cook a relaxed home dinner for four friends.",
    targetDate = "2026-08-15",
    tasks = listOf(
        ProposedTask("Pick the menu and write the shopping list", 45, 5),
        ProposedTask("Send invites and confirm the guest count", 20, 3),
        ProposedTask("Shop for all groceries and drinks", 60, 4),
        ProposedTask("Cook and serve dinner", null, null),
    ),
)

@Preview(name = "Goal preview cards - light", showBackground = true)
@Composable
private fun GoalPreviewCardsLightPreview() {
    AwanTheme {
        Column(
            modifier = Modifier.padding(AwanTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
        ) {
            GoalPreviewSummaryCard(
                text = "Here is your dinner party plan with four clear tasks, all leading up to Saturday evening.",
                expanded = false,
                expandLabel = stringResource(R.string.add_task_goal_preview_read_more),
                collapseLabel = stringResource(R.string.add_task_goal_preview_show_less),
                onToggleExpanded = {},
            )
            GoalPreviewProposalCard(previewProposal)
        }
    }
}

@Preview(name = "Goal preview cards - dark", showBackground = true)
@Composable
private fun GoalPreviewCardsDarkPreview() {
    AwanTheme(dark = true) {
        Column(
            modifier = Modifier.padding(AwanTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
        ) {
            GoalPreviewSummaryCard(
                text = "Here is your dinner party plan with four clear tasks, all leading up to Saturday evening.",
                expanded = false,
                expandLabel = stringResource(R.string.add_task_goal_preview_read_more),
                collapseLabel = stringResource(R.string.add_task_goal_preview_show_less),
                onToggleExpanded = {},
            )
            GoalPreviewProposalCard(previewProposal)
        }
    }
}
