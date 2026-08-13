package com.awan.feature.addtask.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalFocusManager
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.GoalStep
import com.awan.feature.addtask.ui.components.durationLabel
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.MicOff
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
    speechError: String?,
    modifier: Modifier = Modifier,
    isPermissionError: Boolean = false,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
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

            AwanIconButton(
                onClick = onAccept,
                contentDescription = stringResource(R.string.add_task_goal_preview_accept_action),
                enabled = state.canAcceptGoal,
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = if (state.canAcceptGoal) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Scrollable content ───────────────────────────────────────────────
        val goalStep = state.goalStep
        val replyBlocks = state.goalReplyBlocks
        val hasProposalBlock = replyBlocks.any { it is GoalDecompositionBlock.Proposal }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.md),
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
                    horizontal = AwanTheme.spacing.md,
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
                        PreviewGoalMicButton(
                            isListening = isListening,
                            onToggleMic = onToggleMic,
                            enabled = !state.isSubmitting,
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

            val submitLabel = if (state.goalStep is GoalStep.Preview) {
                stringResource(R.string.add_task_goal_preview_revision_submit)
            } else {
                stringResource(R.string.add_task_goal_writing_continue)
            }

            AwanButton(
                onClick = onRevisionSubmit,
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                variant = AwanButtonVariant.Quiet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(submitLabel)
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
    AwanCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
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
                        PreviewTaskProposalItem(index = index + 1, task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTaskProposalItem(index: Int, task: ProposedTask) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AwanTheme.colors.background,
                shape = AwanTheme.shapes.chip,
            )
            .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AwanText(
                text = stringResource(R.string.add_task_goal_preview_task_item_title, index, task.title),
                style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textPrimary),
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                task.estimatedDuration?.let { dur ->
                    val durText = durationLabel(dur)
                    AwanText(
                        text = stringResource(R.string.add_task_goal_preview_duration, durText),
                        style = AwanTheme.styles.metaText,
                    )
                }

                task.estimatedPoints?.let { pts ->
                    AwanText(
                        text = stringResource(R.string.add_task_goal_preview_points, pts),
                        style = AwanTheme.styles.metaText,
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
    val reduced = reducedMotion()
    val scaleSpec = if (reduced) snap() else AwanTheme.motion.settle.spec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (isSelected && !reduced) 1.02f else 1.0f,
        animationSpec = scaleSpec,
        label = "mcqOptionScale",
    )

    val optionDesc = stringResource(R.string.add_task_goal_mcq_option_description, option)
    AwanCard(
        selected = isSelected,
        onClick = if (enabled) { { onOptionSelected(option) } } else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                this.selected = isSelected
                this.contentDescription = optionDesc
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
                    color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewGoalMicButton(
    isListening: Boolean,
    onToggleMic: () -> Unit,
    enabled: Boolean = true,
) {
    val reduced = reducedMotion()
    val shouldPulse = isListening && !reduced && enabled
    val pulseScale by if (shouldPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "micPulseScale",
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    val desc = stringResource(
        if (isListening) R.string.add_task_goal_mic_listening
        else R.string.add_task_goal_mic_idle,
    )

    AwanIconButton(
        onClick = onToggleMic,
        contentDescription = desc,
        enabled = enabled,
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        },
    ) {
        Icon(
            imageVector = if (isListening) Lucide.MicOff else Lucide.Mic,
            contentDescription = null,
            tint = if (isListening) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}



// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "GoalPreviewScreen · Preview step", showBackground = true)
@Composable
private fun GoalPreviewScreenPreviewStepPreview(dark: Boolean = false) {
    AwanTheme(dark = dark) {
        GoalPreviewScreen(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.Preview(
                    proposal = GoalProposal(
                        title = "Master Conversational Spanish",
                        description = "A structured plan to reach conversational level in 3 months.",
                        targetDate = "2026-10-28",
                        tasks = listOf(
                            ProposedTask(title = "Daily vocabulary drills", estimatedDuration = 20, estimatedPoints = 10),
                            ProposedTask(title = "Grammar exercises", estimatedDuration = 30, estimatedPoints = 15),
                        ),
                    ),
                ),
                goalSessionId = "session-123",
            ),
            onAccept = {},
            onDismiss = {},
            onRevisionSubmit = {},
            onRevisionChanged = {},
            onOptionSelected = {},
            onToggleMic = {},
            isListening = false,
            speechError = null,
        )
    }
}
@Preview(name = "Goal preview dark", showBackground = true)
@Composable
private fun GoalPreviewScreenDarkPreview() {
    GoalPreviewScreenPreviewStepPreview(dark = true)
}

@Preview(name = "GoalPreviewScreen · MCQ inline", showBackground = true)
@Composable
private fun GoalPreviewScreenMcqPreview() {
    AwanTheme {
        GoalPreviewScreen(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.MultipleChoice(
                    question = "Would you like to adjust the timeline?",
                    options = listOf("Keep 3 months", "Extend to 6 months", "Shorten to 6 weeks"),
                    selectedOption = "Keep 3 months",
                ),
            ),
            onAccept = {},
            onDismiss = {},
            onRevisionSubmit = {},
            onRevisionChanged = {},
            onOptionSelected = {},
            onToggleMic = {},
            isListening = false,
            speechError = null,
        )
    }
}
