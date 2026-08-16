package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBadge
import com.awan.app.core.designsystem.AwanBadgeTone
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanDropdownMenu
import com.awan.app.core.designsystem.AwanDropdownMenuItem
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.feature.addtask.R
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

private val DurationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)

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
fun GoalPreviewProposalCard(
    proposal: GoalProposal,
    modifier: Modifier = Modifier,
    onUpdateTask: (Int, ProposedTask) -> Unit = { _, _ -> },
    onRemoveTask: (Int) -> Unit = {},
) {
    var expandedIndex by remember { mutableIntStateOf(-1) }

    AwanCard(modifier = modifier.fillMaxWidth()) {
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
                        val isExpanded = expandedIndex == index
                        GoalPreviewTaskCard(
                            index = index + 1,
                            task = task,
                            isExpanded = isExpanded,
                            onToggleExpanded = {
                                expandedIndex = if (isExpanded) -1 else index
                            },
                            onTitleChanged = { newTitle ->
                                onUpdateTask(index, task.copy(title = newTitle))
                            },
                            onDurationPicked = { newDuration ->
                                onUpdateTask(index, task.copy(estimatedDuration = newDuration))
                            },
                            onRemoved = {
                                if (expandedIndex == index) expandedIndex = -1
                                onRemoveTask(index)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalPreviewTaskCard(
    index: Int,
    task: ProposedTask,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDurationPicked: (Int) -> Unit,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    AwanCard(
        modifier = modifier.fillMaxWidth(),
        background = AwanTheme.colors.background,
        onClick = {
            haptics.performHapticFeedback(
                if (isExpanded) HapticFeedbackType.SegmentTick else HapticFeedbackType.ContextClick,
            )
            onToggleExpanded()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                if (isExpanded) {
                    AwanTextField(
                        value = task.title,
                        onValueChange = onTitleChanged,
                        placeholder = stringResource(R.string.add_task_goal_preview_task_title_placeholder),
                        contentDescriptionText = stringResource(R.string.add_task_goal_preview_task_title_placeholder),
                        textStyle = AwanTheme.styles.headingText,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    AwanText(
                        text = task.title.ifBlank { stringResource(R.string.add_task_goal_preview_task_title_placeholder) },
                        style = AwanTheme.typography.body.copy(
                            color = AwanTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                val points = task.estimatedPoints
                if (points != null && points > 0) {
                    AwanBadge(
                        text = stringResource(R.string.add_task_goal_preview_points, points),
                        tone = AwanBadgeTone.Sky,
                    )
                }

                AwanIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        onRemoved()
                    },
                    contentDescription = stringResource(R.string.add_task_goal_preview_remove_task),
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        tint = AwanTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (isExpanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                ) {
                    var durationMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        AwanChip(
                            label = task.estimatedDuration?.let { durationLabel(it) }
                                ?: stringResource(R.string.add_task_goal_preview_task_duration_select),
                            tone = AwanChipTone.Violet,
                            active = task.estimatedDuration != null,
                            onClick = { durationMenuOpen = true },
                        )
                        AwanDropdownMenu(
                            expanded = durationMenuOpen,
                            onDismissRequest = { durationMenuOpen = false },
                        ) {
                            DurationPresets.forEach { minutes ->
                                val active = minutes == task.estimatedDuration
                                AwanDropdownMenuItem(
                                    label = durationLabel(minutes),
                                    onClick = {
                                        onDurationPicked(minutes)
                                        durationMenuOpen = false
                                    },
                                    selected = active,
                                    leading = { AwanChipDot(tone = AwanChipTone.Violet, active = active) },
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    task.estimatedDuration?.let { duration ->
                        AwanBadge(
                            text = durationLabel(duration),
                            tone = AwanBadgeTone.Violet,
                        )
                    }
                }
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
