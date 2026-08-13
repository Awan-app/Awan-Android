package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.awan.app.core.designsystem.AwanMicButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.app.core.designsystem.rememberSpeechRecognizer
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.GoalStep
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import java.time.LocalDate

@Composable
fun GoalForm(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentInput by rememberUpdatedState(state.input)
    val speechState = rememberSpeechRecognizer(
        onTranscript = { transcript -> onAction(AddTaskAction.InputChanged(transcript)) },
        currentText = { currentInput },
        hasRequestedMicPermission = state.hasRequestedMicPermission,
        onSetMicPermissionRequested = { requested ->
            onAction(AddTaskAction.SetMicPermissionRequested(requested))
        },
    )

    LaunchedEffect(state.isSubmitting) {
        if (state.isSubmitting && speechState.isListening) {
            speechState.stopListening()
        }
    }

    val handleAction: (AddTaskAction) -> Unit = { action ->
        if (action is AddTaskAction.InputChanged || action is AddTaskAction.GoalOptionSelected) {
            speechState.clearError()
        }
        onAction(action)
    }

    GoalFormContent(
        state = state,
        onAction = handleAction,
        isListening = speechState.isListening,
        onToggleMic = {
            if (speechState.isListening) {
                speechState.stopListening()
            } else {
                speechState.startListening()
            }
        },
        speechError = speechState.errorMessage,
        isPermissionError = speechState.isPermissionError,
        modifier = modifier,
    )
}

@Composable
fun GoalFormContent(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    speechError: String?,
    modifier: Modifier = Modifier,
    isPermissionError: Boolean = false,
) {
    val isReduced = reducedMotion()
    val standardMillis = AwanTheme.motion.standardMillis
    val fastMillis = AwanTheme.motion.fastMillis

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
    ) {
        AnimatedContent(
            targetState = state.goalStep,
            contentKey = { step -> step::class },
            transitionSpec = {
                if (isReduced) {
                    fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                } else {
                    (fadeIn(animationSpec = tween(standardMillis)) +
                        slideInVertically(animationSpec = tween(standardMillis)) { it / 8 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(fastMillis)) +
                                slideOutVertically(animationSpec = tween(fastMillis)) { -it / 8 }
                        )
                }
            },
            label = "goalStepTransition",
        ) { step ->
            when (step) {
                GoalStep.Initial -> InitialStepContent(
                    state = state,
                    onAction = onAction,
                    isListening = isListening,
                    onToggleMic = onToggleMic,
                    speechError = speechError,
                    isPermissionError = isPermissionError,
                )

                is GoalStep.MultipleChoice -> MultipleChoiceStepContent(
                    step = step,
                    state = state,
                    onAction = onAction,
                    isListening = isListening,
                    onToggleMic = onToggleMic,
                    speechError = speechError,
                    isPermissionError = isPermissionError,
                )

                is GoalStep.WritingQuestion -> WritingStepContent(
                    step = step,
                    state = state,
                    onAction = onAction,
                    isListening = isListening,
                    onToggleMic = onToggleMic,
                    speechError = speechError,
                    isPermissionError = isPermissionError,
                )

                is GoalStep.Preview -> PreviewStepContent(
                    step = step,
                    state = state,
                    onAction = onAction,
                    isListening = isListening,
                    onToggleMic = onToggleMic,
                    speechError = speechError,
                    isPermissionError = isPermissionError,
                )
            }
        }

        val errRes = state.errorMessage
        if (errRes != null) {
            AwanText(
                text = stringResource(errRes),
                style = AwanTheme.styles.errorText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun InitialStepContent(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    speechError: String?,
    isPermissionError: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        CascadeItem(1, Modifier.fillMaxWidth()) {
            AwanText(
                text = stringResource(R.string.add_task_goal_initial_title),
                style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
            )
        }

        CascadeItem(2, Modifier.fillMaxWidth()) {
            AwanText(
                text = stringResource(R.string.add_task_goal_initial_subtitle),
                style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
            )
        }

        CascadeItem(3, Modifier.fillMaxWidth()) {
            Column {
                AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    AwanTextField(
                        value = state.input,
                        onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
                        placeholder = stringResource(R.string.add_task_goal_input_placeholder),
                        contentDescriptionText = stringResource(R.string.add_task_goal_input_description),
                        enabled = !state.isSubmitting,
                        singleLine = false,
                        isError = isPermissionError,
                        trailingContent = {
                            AwanMicButton(
                                isListening = isListening,
                                onToggle = onToggleMic,
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
                            .padding(top = AwanTheme.spacing.xxs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }

        CascadeItem(4, Modifier.fillMaxWidth()) {
            AwanButton(
                onClick = { onAction(AddTaskAction.Submit) },
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(stringResource(R.string.add_task_goal_submit_initial))
            }
        }
    }
}

@Composable
private fun MultipleChoiceStepContent(
    step: GoalStep.MultipleChoice,
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    speechError: String?,
    isPermissionError: Boolean,
) {
    val reduced = reducedMotion()
    val replyBlocks = state.goalReplyBlocks

    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        if (replyBlocks.isNotEmpty()) {
            var questionRendered = false
            replyBlocks.forEachIndexed { index, block ->
                when (block) {
                    is GoalDecompositionBlock.Text -> {
                        CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                            AssistantTextCard(text = block.text)
                        }
                    }

                    is GoalDecompositionBlock.Question -> {
                        if (!questionRendered) {
                            questionRendered = true
                            CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                                AwanText(
                                    text = block.text,
                                    style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                                )
                            }
                        }
                    }

                    is GoalDecompositionBlock.Proposal -> Unit
                }
            }
            if (!questionRendered) {
                CascadeItem(replyBlocks.size + 1, Modifier.fillMaxWidth()) {
                    AwanText(
                        text = step.question,
                        style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                    )
                }
            }
        } else {
            CascadeItem(1, Modifier.fillMaxWidth()) {
                AwanText(
                    text = step.question,
                    style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                )
            }
        }

        val baseIndex = if (replyBlocks.isNotEmpty()) replyBlocks.size + 2 else 2
        step.options.forEachIndexed { index, option ->
            val isSelected = step.selectedOption == option
            val scaleSpec = if (reduced) snap() else AwanTheme.motion.settle.spec<Float>()
            val scale by animateFloatAsState(
                targetValue = if (isSelected && !reduced) 1.02f else 1.0f,
                animationSpec = scaleSpec,
                label = "mcqOptionScale",
            )

            CascadeItem(baseIndex + index, Modifier.fillMaxWidth()) {
                val optionDesc = stringResource(R.string.add_task_goal_mcq_option_description, option)
                AwanCard(
                    selected = isSelected,
                    onClick = if (state.isSubmitting) null else { { onAction(AddTaskAction.GoalOptionSelected(option)) } },
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
        }

        CascadeItem(baseIndex + step.options.size, Modifier.fillMaxWidth()) {
            Column {
                AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    AwanTextField(
                        value = state.input,
                        onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
                        placeholder = stringResource(R.string.add_task_goal_mcq_custom_placeholder),
                        contentDescriptionText = stringResource(R.string.add_task_goal_mcq_custom_description),
                        enabled = !state.isSubmitting,
                        singleLine = false,
                        isError = isPermissionError,
                        trailingContent = {
                            AwanMicButton(
                                isListening = isListening,
                                onToggle = onToggleMic,
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
                            .padding(top = AwanTheme.spacing.xxs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }

        CascadeItem(baseIndex + step.options.size + 1, Modifier.fillMaxWidth()) {
            AwanButton(
                onClick = { onAction(AddTaskAction.Submit) },
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(stringResource(R.string.add_task_goal_mcq_continue))
            }
        }
    }
}

@Composable
private fun WritingStepContent(
    step: GoalStep.WritingQuestion,
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    speechError: String?,
    isPermissionError: Boolean,
) {
    val replyBlocks = state.goalReplyBlocks

    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        if (replyBlocks.isNotEmpty()) {
            var questionRendered = false
            replyBlocks.forEachIndexed { index, block ->
                when (block) {
                    is GoalDecompositionBlock.Text -> {
                        CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                            AssistantTextCard(text = block.text)
                        }
                    }

                    is GoalDecompositionBlock.Question -> {
                        if (!questionRendered && block.text.isNotBlank()) {
                            questionRendered = true
                            CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                                AwanText(
                                    text = block.text,
                                    style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                                )
                            }
                        }
                    }

                    is GoalDecompositionBlock.Proposal -> Unit
                }
            }
            if (!questionRendered) {
                val titleText = step.question.ifBlank {
                    stringResource(R.string.add_task_goal_writing_fallback_prompt)
                }
                CascadeItem(replyBlocks.size + 1, Modifier.fillMaxWidth()) {
                    AwanText(
                        text = titleText,
                        style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                    )
                }
            }
        } else {
            val titleText = step.question.ifBlank {
                stringResource(R.string.add_task_goal_writing_fallback_prompt)
            }
            CascadeItem(1, Modifier.fillMaxWidth()) {
                AwanText(
                    text = titleText,
                    style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                )
            }
        }

        val baseIndex = if (replyBlocks.isNotEmpty()) replyBlocks.size + 2 else 2

        CascadeItem(baseIndex, Modifier.fillMaxWidth()) {
            Column {
                AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    AwanTextField(
                        value = state.input,
                        onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
                        placeholder = stringResource(R.string.add_task_goal_writing_placeholder),
                        contentDescriptionText = stringResource(R.string.add_task_goal_writing_description),
                        enabled = !state.isSubmitting,
                        singleLine = false,
                        isError = isPermissionError,
                        trailingContent = {
                            AwanMicButton(
                                isListening = isListening,
                                onToggle = onToggleMic,
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
                            .padding(top = AwanTheme.spacing.xxs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }

        CascadeItem(baseIndex + 1, Modifier.fillMaxWidth()) {
            AwanButton(
                onClick = { onAction(AddTaskAction.Submit) },
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(stringResource(R.string.add_task_goal_writing_continue))
            }
        }
    }
}

@Composable
private fun PreviewStepContent(
    step: GoalStep.Preview,
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    speechError: String?,
    isPermissionError: Boolean,
) {
    val replyBlocks = state.goalReplyBlocks

    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        if (replyBlocks.isNotEmpty()) {
            var proposalRendered = false
            replyBlocks.forEachIndexed { index, block ->
                when (block) {
                    is GoalDecompositionBlock.Text -> {
                        CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                            AssistantTextCard(text = block.text)
                        }
                    }

                    is GoalDecompositionBlock.Proposal -> {
                        if (!proposalRendered) {
                            proposalRendered = true
                            CascadeItem(index + 1, Modifier.fillMaxWidth()) {
                                ProposalCard(proposal = block.proposal)
                            }
                        }
                    }

                    is GoalDecompositionBlock.Question -> Unit
                }
            }
            if (!proposalRendered) {
                CascadeItem(replyBlocks.size + 1, Modifier.fillMaxWidth()) {
                    ProposalCard(proposal = step.proposal)
                }
            }
        } else {
            CascadeItem(1, Modifier.fillMaxWidth()) {
                ProposalCard(proposal = step.proposal)
            }
        }

        val baseIndex = if (replyBlocks.isNotEmpty()) replyBlocks.size + 2 else 2

        CascadeItem(baseIndex, Modifier.fillMaxWidth()) {
            AwanButton(
                onClick = { onAction(AddTaskAction.AcceptGoalProposal) },
                enabled = state.canAcceptGoal,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(stringResource(R.string.add_task_goal_preview_accept))
            }
        }

        CascadeItem(baseIndex + 1, Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    AwanTextField(
                        value = state.input,
                        onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
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
                            .padding(top = AwanTheme.spacing.xxs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                AwanButton(
                    onClick = { onAction(AddTaskAction.Submit) },
                    enabled = state.canSubmit,
                    isLoading = state.isSubmitting,
                    variant = AwanButtonVariant.Quiet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AwanText(stringResource(R.string.add_task_goal_preview_revision_submit))
                }
            }
        }
    }
}

@Composable
private fun AssistantTextCard(text: String) {
    AwanCard(
        background = AwanTheme.colors.surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AwanText(
            text = text,
            style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
        )
    }
}

@Composable
private fun ProposalCard(proposal: GoalProposal) {
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
                        TaskProposalItem(index = index + 1, task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskProposalItem(index: Int, task: ProposedTask) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AwanTheme.colors.line.copy(alpha = 0.3f),
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

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "GoalForm · 1. Initial", showBackground = true)
@Composable
private fun GoalFormInitialPreview() {
    AwanTheme {
        GoalFormContent(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.Initial,
                input = "Learn Spanish for a trip",
            ),
            onAction = {},
            isListening = false,
            onToggleMic = {},
            speechError = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "GoalForm · 2. MultipleChoice", showBackground = true)
@Composable
private fun GoalFormMcqPreview() {
    AwanTheme {
        GoalFormContent(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.MultipleChoice(
                    question = "What is your main focus?",
                    options = listOf("Conversational speaking", "Reading & grammar", "Business vocabulary"),
                    selectedOption = "Conversational speaking",
                ),
            ),
            onAction = {},
            isListening = false,
            onToggleMic = {},
            speechError = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "GoalForm · 3. WritingQuestion", showBackground = true)
@Composable
private fun GoalFormWritingPreview() {
    AwanTheme {
        GoalFormContent(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.WritingQuestion(question = "How many hours per week can you dedicate?"),
                input = "5 hours every weekend",
            ),
            onAction = {},
            isListening = false,
            onToggleMic = {},
            speechError = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "GoalForm · 4. Preview", showBackground = true)
@Composable
private fun GoalFormPreviewStatePreview() {
    AwanTheme {
        GoalFormContent(
            state = AddTaskState(
                today = LocalDate.of(2026, 7, 28),
                mode = AddTaskMode.GOAL,
                goalStep = GoalStep.Preview(
                    proposal = GoalProposal(
                        title = "Master Conversational Spanish",
                        description = "Targeted practice for trip to Spain",
                        targetDate = "2026-09-01",
                        tasks = listOf(
                            ProposedTask("Daily vocabulary review", estimatedDuration = 30, estimatedPoints = 10),
                            ProposedTask("Practice with language exchange partner", estimatedDuration = 60, estimatedPoints = 25),
                        ),
                    ),
                ),
                goalSessionId = "session-123",
            ),
            onAction = {},
            isListening = false,
            onToggleMic = {},
            speechError = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}
