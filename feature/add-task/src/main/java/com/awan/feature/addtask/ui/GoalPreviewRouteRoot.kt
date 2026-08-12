package com.awan.feature.addtask.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanConfirmDialog
import com.awan.app.core.designsystem.AwanDialog
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.app.core.designsystem.rememberSpeechRecognizer
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskEvent
import com.awan.feature.addtask.presentation.AddTaskViewModel

@Composable
fun GoalPreviewRouteRoot(
    onBack: () -> Unit,
    onNavigateToGoals: () -> Unit,
    viewModel: AddTaskViewModel = hiltViewModel(
        viewModelStoreOwner = checkNotNull(LocalActivity.current) as ComponentActivity,
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AddTaskEvent.GoalCreated -> onNavigateToGoals()
            is AddTaskEvent.TaskCreated -> onBack()
            is AddTaskEvent.AiRequested -> Unit
            AddTaskEvent.Dismissed -> onBack()
        }
    }

    BackHandler(enabled = true) {
        viewModel.onAction(AddTaskAction.DismissRequested)
    }

    val speechState = rememberSpeechRecognizer(
        onTranscript = { transcript -> viewModel.onAction(AddTaskAction.InputChanged(transcript)) },
        currentText = { viewModel.state.value.input },
        hasRequestedMicPermission = state.hasRequestedMicPermission,
        onSetMicPermissionRequested = { requested ->
            viewModel.onAction(AddTaskAction.SetMicPermissionRequested(requested))
        },
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

    if (state.showGoalSaveChoice) {
        AwanDialog(
            title = stringResource(R.string.add_task_goal_save_choice_title),
            body = stringResource(R.string.add_task_goal_save_choice_body),
            primaryLabel = stringResource(R.string.add_task_goal_save_choice_add_tasks),
            onPrimary = { viewModel.onAction(AddTaskAction.AddGoalTasks) },
            secondaryLabel = stringResource(R.string.add_task_goal_save_choice_draft),
            onSecondary = { viewModel.onAction(AddTaskAction.SaveGoalAsDraft) },
            onDismiss = { viewModel.onAction(AddTaskAction.GoalSaveChoiceDismissed) },
        )
    }

    GoalPreviewScreen(
        state = state,
        onAccept = { viewModel.onAction(AddTaskAction.AcceptGoalProposal) },
        onDismiss = { viewModel.onAction(AddTaskAction.DismissRequested) },
        onRevisionSubmit = { viewModel.onAction(AddTaskAction.Submit) },
        onRevisionChanged = { input ->
            speechState.clearError()
            viewModel.onAction(AddTaskAction.InputChanged(input))
        },
        onOptionSelected = { option ->
            speechState.clearError()
            viewModel.onAction(AddTaskAction.GoalOptionSelected(option))
        },
        onToggleMic = {
            if (speechState.isListening) speechState.stopListening()
            else speechState.startListening()
        },
        isListening = speechState.isListening,
        micAmplitude = speechState.amplitude,
        speechError = speechState.errorMessage,
        isPermissionError = speechState.isPermissionError,
    )
}
