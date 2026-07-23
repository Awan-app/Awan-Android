package com.awan.feature.addtask.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskEvent
import com.awan.feature.addtask.presentation.AddTaskMode
import com.awan.feature.addtask.presentation.AddTaskState
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.components.AddTaskModeSelector
import com.awan.feature.addtask.ui.components.GoalPlaceholder
import com.awan.feature.addtask.ui.components.TaskAttributeChips
import com.awan.feature.addtask.ui.components.rememberTokenHighlight

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
        modifier = modifier,
    ) {
        AddTaskSheetContent(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = AwanTheme.spacing.lg)
                .padding(bottom = AwanTheme.spacing.xl),
        )
    }
}

@Composable
private fun AddTaskSheetContent(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
    ) {
        AddTaskModeSelector(
            selected = state.mode,
            onSelect = { onAction(AddTaskAction.ModeChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        when (state.mode) {
            AddTaskMode.TASK -> TaskForm(state = state, onAction = onAction)
            AddTaskMode.GOAL -> GoalPlaceholder()
        }
    }
}

@Composable
private fun TaskForm(
    state: AddTaskState,
    onAction: (AddTaskAction) -> Unit,
) {
    AwanTextField(
        value = state.input,
        onValueChange = { onAction(AddTaskAction.InputChanged(it)) },
        placeholder = stringResource(R.string.add_task_title_placeholder),
        contentDescriptionText = stringResource(R.string.add_task_title_content_description),
        visualTransformation = rememberTokenHighlight(state.parsed.tokens),
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next,
        modifier = Modifier.fillMaxWidth(),
    )

    AwanTextField(
        value = state.description,
        onValueChange = { onAction(AddTaskAction.DescriptionChanged(it)) },
        placeholder = stringResource(R.string.add_task_description_placeholder),
        contentDescriptionText = stringResource(R.string.add_task_description_content_description),
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth(),
    )

    AwanText(stringResource(R.string.add_task_hint), style = AwanTheme.styles.metaText)

    TaskAttributeChips(
        state = state,
        today = state.today,
        onToggleMandatory = { onAction(AddTaskAction.MandatoryToggled) },
        modifier = Modifier.fillMaxWidth(),
    )

    state.errorMessage?.let {
        AwanText(stringResource(it), style = AwanTheme.styles.errorText)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AwanButton(
            onClick = { onAction(AddTaskAction.Submit) },
            enabled = state.canSubmit,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AwanText(stringResource(R.string.add_task_submit))
        }
    }
}
