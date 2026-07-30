package com.awan.feature.addtask.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanConfirmDialog
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskEvent
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.components.rememberSpeechRecognizer

@Composable
fun GoalPreviewRouteRoot(
    viewModel: AddTaskViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AddTaskEvent.GoalCreated -> onBack()
            is AddTaskEvent.TaskCreated -> onBack()
            AddTaskEvent.Dismissed -> onBack()
        }
    }

    BackHandler(enabled = true) {
        viewModel.onAction(AddTaskAction.DismissRequested)
    }

    val speechState = rememberSpeechRecognizer(
        onTranscript = { transcript -> viewModel.onAction(AddTaskAction.InputChanged(transcript)) },
    )

    LaunchedEffect(state.isSubmitting) {
        if (state.isSubmitting && speechState.isListening) {
            speechState.stopListening()
        }
    }

    if (state.showDiscardConfirm) {
        AwanConfirmDialog(
            title = stringResource(R.string.add_task_discard_title),
            body = stringResource(R.string.add_task_discard_body),
            confirmLabel = stringResource(R.string.add_task_discard_confirm),
            confirmVariant = AwanButtonVariant.Destructive,
            onConfirm = { viewModel.onAction(AddTaskAction.DiscardConfirmed) },
            dismissLabel = stringResource(R.string.add_task_discard_cancel),
            onDismiss = { viewModel.onAction(AddTaskAction.DiscardCancelled) },
        )
    }

    GoalPreviewScreen(
        state = state,
        onAccept = { viewModel.onAction(AddTaskAction.AcceptGoalProposal) },
        onDismiss = { viewModel.onAction(AddTaskAction.DismissRequested) },
        onRevisionSubmit = { viewModel.onAction(AddTaskAction.Submit) },
        onRevisionChanged = { viewModel.onAction(AddTaskAction.InputChanged(it)) },
        onOptionSelected = { viewModel.onAction(AddTaskAction.GoalOptionSelected(it)) },
        onToggleMic = {
            if (speechState.isListening) speechState.stopListening()
            else speechState.startListening()
        },
        isListening = speechState.isListening,
        speechError = speechState.errorMessage,
        isPermissionError = speechState.isPermissionError,
    )
}
