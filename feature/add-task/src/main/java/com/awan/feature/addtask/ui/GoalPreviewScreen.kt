package com.awan.feature.addtask.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanAiAura
import com.awan.app.core.designsystem.AwanBadge
import com.awan.app.core.designsystem.AwanBadgeTone
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanMicButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.GoalStep
import com.awan.feature.addtask.ui.components.durationLabel
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import java.time.LocalDate

@Composable
fun GoalPreviewScreen(
    state: AddTaskState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onRevisionSubmit: () -> Unit,
    onRevisionChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onToggleMic: () -> Unit,
    isListening: Boolean,
    micAmplitude: () -> Float,
    speechError: String?,
    modifier: Modifier = Modifier,
    isPermissionError: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val previewHorizontalPadding = AwanTheme.spacing.sm

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = previewHorizontalPadding, vertical = AwanTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AwanIconButton(
                onClick = onDismiss,
                contentDescription = stringResource(R.string.add_task_goal_preview_close),
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = null,
                    tint = AwanTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            AwanText(
                text = stringResource(R.string.add_task_goal_preview_title),
                style = AwanTheme.styles.titleText,
            )

            Spacer(modifier = Modifier.size(38.dp))
        }

        // ── Scrollable content ───────────────────────────────────────────────
        val goalStep = state.goalStep
        val replyBlocks = state.goalReplyBlocks
        val hasProposalBlock = replyBlocks.any { it is GoalDecompositionBlock.Proposal }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = previewHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            if (replyBlocks.isNotEmpty()) {
                itemsIndexed(replyBlocks, key = { index, _ -> index }) { _, block ->
                    when (block) {
                        is GoalDecompositionBlock.Text -> PreviewAssistantTextCard(text = block.text)
                        is GoalDecompositionBlock.Proposal -> PreviewProposalCard(proposal = block.proposal)
                        is GoalDecompositionBlock.Question -> Unit // ignored per spec
                    }
                }
                if (!hasProposalBlock && goalStep is GoalStep.Preview) {
                    item {
                        PreviewProposalCard(proposal = goalStep.proposal)
                    }
                }
            } else if (goalStep is GoalStep.Preview) {
                item {
                    PreviewProposalCard(proposal = goalStep.proposal)
                }
            }

            // Inline follow-up step content
            when (goalStep) {
                is GoalStep.MultipleChoice -> {
                    item {
                        AwanText(
                            text = goalStep.question,
                            style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                        )
                    }
                    itemsIndexed(
                        items = goalStep.options,
                        key = { index, _ -> "option_$index" },
                    ) { _, option ->
                        val isSelected = goalStep.selectedOption == option
                        PreviewMcqOptionCard(
                            option = option,
                            isSelected = isSelected,
                            enabled = !state.isSubmitting,
                            onOptionSelected = { selectedOption ->
                                focusManager.clearFocus()
                                onOptionSelected(selectedOption)
                            },
                        )
                    }
                }

                is GoalStep.WritingQuestion -> {
                    item {
                        AwanText(
                            text = goalStep.question.ifBlank {
                                stringResource(R.string.add_task_goal_writing_fallback_prompt)
                            },
                            style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                        )
                    }
                }

                else -> Unit
            }

            // Error messages at end
            val errRes = state.errorMessage
            if (errRes != null) {
                item {
                    AwanText(
                        text = stringResource(errRes),
                        style = AwanTheme.styles.errorText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }

        // ── Bottom footer ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = previewHorizontalPadding,
                    vertical = AwanTheme.spacing.sm,
                ),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        ) {
            AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                AwanTextField(
                    value = state.input,
                    onValueChange = onRevisionChanged,
                    placeholder = stringResource(R.string.add_task_goal_preview_revision_placeholder),
                    contentDescriptionText = stringResource(R.string.add_task_goal_preview_revision_description),
                    enabled = !state.isSubmitting,
                    singleLine = false,
                    isError = isPermissionError,
                    trailingContent = {
                        AwanMicButton(
                            isListening = isListening,
                            onToggle = onToggleMic,
                            enabled = !state.isSubmitting,
                            amplitude = micAmplitude,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (speechError != null) {
                AwanText(
                    text = speechError,
                    style = AwanTheme.styles.errorText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            val isPreview = state.goalStep is GoalStep.Preview
            val hasInput = state.input.isNotBlank()

            val buttonText = when {
                hasInput -> stringResource(R.string.add_task_goal_preview_revision_submit)
                isPreview -> stringResource(R.string.add_task_goal_preview_accept)
                else -> stringResource(R.string.add_task_goal_writing_continue)
            }

            val buttonOnClick = if (hasInput || !isPreview) onRevisionSubmit else onAccept
            val buttonEnabled = if (hasInput || !isPreview) state.canSubmit else state.canAcceptGoal

            AwanButton(
                onClick = buttonOnClick,
                enabled = buttonEnabled,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(buttonText)
            }
        }
    }
}

// ── Private composable helpers ────────────────────────────────────────────────

@Composable
private fun PreviewAssistantTextCard(text: String) {
    AwanCard(
        background = AwanTheme.colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AwanText(
            text = text,
            style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
        )
    }
}

@Composable
private fun PreviewProposalCard(proposal: GoalProposal) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        AwanCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                AwanText(
                    text = proposal.title,
                    style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                )

                proposal.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    AwanText(
                        text = desc,
                        style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
                    )
                }

                proposal.targetDate?.takeIf { it.isNotBlank() }?.let { dateStr ->
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
                            text = stringResource(R.string.add_task_goal_preview_target_date, dateStr),
                            style = AwanTheme.styles.metaText,
                        )
                    }
                }
            }
        }

        if (proposal.tasks.isNotEmpty()) {
            AwanText(
                text = stringResource(R.string.add_task_goal_preview_tasks_header, proposal.tasks.size),
                style = AwanTheme.typography.body.copy(
                    color = AwanTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(top = AwanTheme.spacing.xxs),
            )

            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                proposal.tasks.forEachIndexed { index, task ->
                    PreviewTaskProposalCard(index = index + 1, task = task)
                }
            }
        }
    }
}

@Composable
private fun PreviewTaskProposalCard(index: Int, task: ProposedTask) {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AwanText(
                    text = stringResource(R.string.add_task_goal_preview_task_item_title, index, task.title),
                    style = AwanTheme.styles.headingText,
                    modifier = Modifier.weight(1f),
                )
                task.estimatedPoints?.takeIf { it > 0 }?.let { pts ->
                    AwanBadge(
                        text = stringResource(R.string.add_task_goal_preview_points, pts),
                        tone = AwanBadgeTone.Sky,
                    )
                }
            }

            task.estimatedDuration?.let { dur ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                ) {
                    AwanBadge(
                        text = durationLabel(dur),
                        tone = AwanBadgeTone.Violet,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMcqOptionCard(
    option: String,
    isSelected: Boolean,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = if (reducedMotion()) snap() else tween(durationMillis = 150),
        label = "mcq_scale",
    )
    val optionDescription = stringResource(R.string.add_task_goal_mcq_option_description, option)

    AwanCard(
        onClick = { if (enabled) onOptionSelected(option) },
        background = if (isSelected) AwanTheme.colors.line else AwanTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                selected = isSelected
                contentDescription = optionDescription
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AwanText(
                text = option,
                style = AwanTheme.typography.body.copy(
                    color = if (isSelected) AwanTheme.colors.textPrimary else AwanTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(AwanTheme.colors.sky, AwanTheme.shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = "✓",
                        style = AwanTheme.styles.buttonCompactText.copy(color = AwanTheme.colors.background),
                    )
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val sampleProposal = GoalProposal(
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

@Preview(name = "GoalPreviewScreen - Proposal - Light", showBackground = true)
@Composable
private fun GoalPreviewScreenProposalLightPreview() {
    AwanTheme {
        GoalPreviewScreen(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 22),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.Preview(sampleProposal),
                goalSessionId = "sess-preview-preview",
            ),
            onAccept = {},
            onDismiss = {},
            onRevisionSubmit = {},
            onRevisionChanged = {},
            onOptionSelected = {},
            onToggleMic = {},
            isListening = false,
            micAmplitude = { 0f },
            speechError = null,
        )
    }
}

@Preview(name = "GoalPreviewScreen - Proposal - Dark", showBackground = true)
@Composable
private fun GoalPreviewScreenProposalDarkPreview() {
    AwanTheme(dark = true) {
        GoalPreviewScreen(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 22),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.Preview(sampleProposal),
                goalSessionId = "sess-preview-preview",
            ),
            onAccept = {},
            onDismiss = {},
            onRevisionSubmit = {},
            onRevisionChanged = {},
            onOptionSelected = {},
            onToggleMic = {},
            isListening = false,
            micAmplitude = { 0f },
            speechError = null,
        )
    }
}
