package com.awan.feature.addtask.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanDatePickerDialog
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTimePickerDialog
import com.awan.app.core.designsystem.CascadeItem
import com.awan.app.core.designsystem.CloudDrift
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.app.core.designsystem.SparkleBurst
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskEvent
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskPicker
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.components.AddTaskModeSelector
import com.awan.feature.addtask.ui.components.GoalPlaceholder
import com.awan.feature.addtask.ui.components.TaskAttributeChips
import com.awan.feature.addtask.ui.components.rememberTokenHighlight

private val MascotWidth = 108.dp
private val SkyHeight = 116.dp
private val DragHandleWidth = 36.dp
private val DragHandleHeight = 4.dp

/**
 * Quick capture. Opened from the `+` in the bottom bar; it is deliberately not a navigation
 * destination, so dismissing it always returns to whatever screen was already showing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onTaskCreated: (String) -> Unit = {},
    viewModel: AddTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AddTaskEvent.TaskCreated -> {
                onTaskCreated(event.title)
                onDismiss()
            }

            AddTaskEvent.Dismissed -> onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AwanTheme.colors.background,
        contentColor = AwanTheme.colors.textPrimary,
        dragHandle = null,
        modifier = modifier,
    ) {
        AddTaskSheetContent(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.fillMaxWidth().imePadding(),
        )
    }

    AttributePickers(state = state, onAction = viewModel::onAction)
}

/**
 * Every picker writes its choice back into the sentence; none holds a value of its own.
 *
 * Scheduling is two steps in the order the question is asked: which day, then what time on it.
 * Confirming the day therefore says "Next", and backing out of the clock still keeps the day.
 */
@Composable
private fun AttributePickers(state: AddTaskState, onAction: (AddTaskAction) -> Unit) {
    when (state.openPicker) {
        AddTaskPicker.DATE -> AwanDatePickerDialog(
            initialDate = state.pickerInitialDate,
            earliestDate = state.today,
            confirmLabel = stringResource(R.string.add_task_picker_next),
            cancelLabel = stringResource(R.string.add_task_picker_cancel),
            onDismiss = { onAction(AddTaskAction.PickerDismissed) },
            onConfirm = { onAction(AddTaskAction.DatePicked(it)) },
        )

        AddTaskPicker.TIME -> AwanTimePickerDialog(
            initialMinutes = state.pickerInitialMinutes,
            confirmLabel = stringResource(R.string.add_task_picker_set),
            cancelLabel = stringResource(R.string.add_task_picker_cancel),
            onDismiss = { onAction(AddTaskAction.PickerDismissed) },
            onConfirm = { onAction(AddTaskAction.TimePicked(it)) },
        )

        // Length is a menu hanging off its own chip, so it lives in TaskAttributeChips.
        AddTaskPicker.DURATION, null -> Unit
    }
}

@Composable
private fun AddTaskSheetContent(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SkyHeader(state = state)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.lg)
                .padding(top = AwanTheme.spacing.md, bottom = AwanTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
        ) {
            CascadeItem(0, Modifier.fillMaxWidth()) {
                AddTaskModeSelector(
                    selected = state.mode,
                    onSelect = { onAction(AddTaskAction.ModeChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Crossfade(targetState = state.mode, label = "addTaskMode") { mode ->
                when (mode) {
                    AddTaskMode.TASK -> TaskForm(state = state, onAction = onAction)
                    AddTaskMode.GOAL -> GoalPlaceholder()
                }
            }
        }
    }
}

/**
 * The band of drifting cloud the mascot sits on. This is the first thing that moves when the sheet
 * opens, so the sheet reads as sky arriving rather than a panel appearing.
 */
@Composable
private fun SkyHeader(state: AddTaskState) {
    CloudDrift(height = SkyHeight) {
        // The sheet has no Material drag handle — the sky is the header — so this stands in for it.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = AwanTheme.spacing.sm)
                .size(width = DragHandleWidth, height = DragHandleHeight)
                .clip(AwanTheme.shapes.pill)
                .background(AwanTheme.colors.surface.copy(alpha = 0.75f)),
        )

        Box(
            modifier = Modifier.padding(bottom = AwanTheme.spacing.xxs),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = state.mascot, label = "addTaskMascot") { expression ->
                AwanMascot(expression = expression, width = MascotWidth)
            }
            Box(Modifier.size(MascotWidth)) {
                SparkleBurst(celebrate = state.isCelebrating)
            }
        }
    }
}

@Composable
private fun TaskForm(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        // The task name is the draft. It gets the heading scale and the bordered field; the note
        // below it is deliberately quieter so the two never compete for the eye.
        CascadeItem(1, Modifier.fillMaxWidth()) {
            AwanTextField(
                value = state.input,
                onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
                placeholder = stringResource(R.string.add_task_title_placeholder),
                contentDescriptionText = stringResource(R.string.add_task_title_content_description),
                textStyle = AwanTheme.styles.headingText,
                placeholderStyle = AwanTheme.styles.headingText.copy(color = AwanTheme.colors.meta),
                visualTransformation = rememberTokenHighlight(state.parsed.tokens),
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        CascadeItem(2, Modifier.fillMaxWidth()) {
            NoteField(
                value = state.description,
                onValueChange = { onAction(AddTaskAction.DescriptionChanged(it)) },
            )
        }

        CascadeItem(3) {
            AwanText(stringResource(R.string.add_task_hint), style = AwanTheme.styles.metaText)
        }

        CascadeItem(4, Modifier.fillMaxWidth()) {
            TaskAttributeChips(
                state = state,
                today = state.today,
                onEditWhen = { onAction(AddTaskAction.PickerOpened(AddTaskPicker.DATE)) },
                onEditDuration = { onAction(AddTaskAction.PickerOpened(AddTaskPicker.DURATION)) },
                onDurationPicked = { onAction(AddTaskAction.DurationPicked(it)) },
                onDurationMenuDismissed = { onAction(AddTaskAction.PickerDismissed) },
                onToggleMandatory = { onAction(AddTaskAction.MandatoryToggled) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.errorMessage?.let {
            AwanText(stringResource(it), style = AwanTheme.styles.errorText)
        }

        CascadeItem(5, Modifier.fillMaxWidth()) {
            SubmitButton(state = state, onSubmit = { onAction(AddTaskAction.Submit) })
        }
    }
}

/**
 * The description. No rim, no border, body scale, secondary ink — it reads as an annotation hanging
 * off the task name rather than a second field of equal weight.
 */
@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    val label = stringResource(R.string.add_task_description_content_description)
    val placeholder = stringResource(R.string.add_task_description_placeholder)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
        cursorBrush = SolidColor(AwanTheme.colors.sky),
        singleLine = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AwanTheme.spacing.xxs)
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) {
                    AwanText(
                        text = placeholder,
                        style = AwanTheme.styles.bodySecondaryText.copy(color = AwanTheme.colors.meta),
                    )
                }
                field()
            }
        },
    )
}

/** Pops the moment the sentence becomes submittable — the same beat the onboarding footer uses. */
@Composable
private fun SubmitButton(state: AddTaskState, onSubmit: () -> Unit) {
    val reduced = reducedMotion()
    val pop = remember { Animatable(1f) }
    val spec = AwanTheme.motion.playful.spec<Float>()

    LaunchedEffect(state.canSubmit) {
        if (state.canSubmit && !reduced) {
            pop.animateTo(1.06f, spec)
            pop.animateTo(1f, spec)
        }
    }

    AwanButton(
        onClick = onSubmit,
        enabled = state.canSubmit,
        isLoading = state.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
    ) {
        AwanText(stringResource(R.string.add_task_submit))
    }
}
