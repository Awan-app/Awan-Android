package com.awan.feature.addtask.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.awan.feature.addtask.ui.components.GoalPreviewProposalCard
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
    onUpdateProposedTask: (Int, ProposedTask) -> Unit = { _, _ -> },
    onRemoveProposedTask: (Int) -> Unit = {},
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = previewHorizontalPadding, vertical = AwanTheme.spacing.xs),
        ) {
            AwanIconButton(
                onClick = onDismiss,
                contentDescription = stringResource(R.string.add_task_goal_preview_close),
                modifier = Modifier.align(Alignment.CenterStart),
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
                modifier = Modifier.align(Alignment.Center),
            )
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
                        is GoalDecompositionBlock.Proposal -> GoalPreviewProposalCard(
                            proposal = block.proposal,
                            onUpdateTask = onUpdateProposedTask,
                            onRemoveTask = onRemoveProposedTask,
                        )
                        is GoalDecompositionBlock.Question -> Unit // ignored per spec
                    }
                }
                if (!hasProposalBlock && goalStep is GoalStep.Preview) {
                    item {
                        GoalPreviewProposalCard(
                            proposal = goalStep.proposal,
                            onUpdateTask = onUpdateProposedTask,
                            onRemoveTask = onRemoveProposedTask,
                        )
                    }
                }
            } else if (goalStep is GoalStep.Preview) {
                item {
                    GoalPreviewProposalCard(
                        proposal = goalStep.proposal,
                        onUpdateTask = onUpdateProposedTask,
                        onRemoveTask = onRemoveProposedTask,
                    )
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

            val (buttonLabel, buttonAction, buttonEnabled, buttonVariant) = when {
                isPreview && hasInput -> {
                    Quad(
                        stringResource(R.string.add_task_goal_preview_revision_submit),
                        onRevisionSubmit,
                        state.canSubmit,
                        AwanButtonVariant.Primary,
                    )
                }
                isPreview -> {
                    Quad(
                        stringResource(R.string.add_task_goal_preview_approve_plan),
                        onAccept,
                        state.canAcceptGoal,
                        AwanButtonVariant.Primary,
                    )
                }
                else -> {
                    Quad(
                        stringResource(R.string.add_task_goal_writing_continue),
                        onRevisionSubmit,
                        state.canSubmit,
                        AwanButtonVariant.Primary,
                    )
                }
            }

            AwanButton(
                onClick = buttonAction,
                enabled = buttonEnabled,
                isLoading = state.isSubmitting,
                variant = buttonVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(buttonLabel)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
