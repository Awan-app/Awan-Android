package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.GoalStatus
import com.awan.feature.goals.impl.presentation.GoalDetailsAction
import com.awan.feature.goals.impl.presentation.GoalDetailsEvent
import com.awan.feature.goals.impl.presentation.GoalDetailsState
import com.awan.feature.goals.impl.ui.components.*
import com.awan.feature.addtask.ui.AddTaskSheet
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.presentation.AddTaskAction
import com.awan.feature.addtask.presentation.AddTaskEvent
import kotlinx.coroutines.flow.Flow

@Composable
fun GoalDetailsScreen(
    state: GoalDetailsState,
    events: Flow<GoalDetailsEvent>,
    onAction: (GoalDetailsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AwanTheme.colors
    val context = LocalContext.current
    var goalToDelete by remember { mutableStateOf<String?>(null) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }

    ObserveAsEvents(events) { event ->
        when (event) {
            GoalDetailsEvent.NavigateBack -> onNavigateBack()
            is GoalDetailsEvent.ShowError -> {
                AwanToastManager.showToast(
                    message = event.message.asString(context)
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            val isAchieved = state.goal?.status == GoalStatus.ACHIEVED
            GoalDetailsTopBar(
                title = state.goal?.title ?: "",
                isAchieved = isAchieved,
                onBack = { onAction(GoalDetailsAction.Back) },
                onEditClick = { onAction(GoalDetailsAction.EditClicked) }
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.sky
                    )
                }
                state.goal != null -> {
                    val isAchieved = state.goal.status == GoalStatus.ACHIEVED
                    GoalDetailsContent(
                        goal = state.goal,
                        isAchieved = isAchieved,
                        isDeleting = state.isDeleting,
                        completingTaskIds = state.completingTaskIds,
                        onDeleteGoalClick = { goalToDelete = state.goal.id },
                        onDeleteTaskClick = { taskToDelete = it },
                        onAddTaskClick = { onAction(GoalDetailsAction.AddTaskClicked) },
                        onTaskToggle = { onAction(GoalDetailsAction.TaskChecked(it)) }
                    )
                }
                state.error != null -> {
                    AwanText(
                        text = state.error.asString(),
                        modifier = Modifier.align(Alignment.Center),
                        style = AwanTheme.typography.body
                    )
                }
            }
        }

        // Dialogs
        goalToDelete?.let { goalId ->
            AwanConfirmDialog(
                title = "Delete Goal",
                body = "Are you sure you want to delete this goal? All associated tasks will also be removed.",
                confirmLabel = "Delete",
                confirmVariant = AwanButtonVariant.Destructive,
                dismissLabel = "Cancel",
                onConfirm = {
                    onAction(GoalDetailsAction.DeleteClicked)
                    goalToDelete = null
                },
                onDismiss = { goalToDelete = null }
            )
        }

        taskToDelete?.let { taskId ->
            AwanConfirmDialog(
                title = "Delete Task",
                body = "Are you sure you want to delete this task?",
                confirmLabel = "Delete",
                confirmVariant = AwanButtonVariant.Destructive,
                dismissLabel = "Cancel",
                onConfirm = {
                    onAction(GoalDetailsAction.DeleteTaskClicked(taskId))
                    taskToDelete = null
                },
                onDismiss = { taskToDelete = null }
            )
        }

        if (state.showEditSheet && state.goal != null) {
            GoalEditSheet(
                goal = state.goal,
                isSaving = state.isUpdating,
                onDismiss = { onAction(GoalDetailsAction.EditDismissed) },
                onConfirm = { title, description, status, targetDate ->
                    onAction(GoalDetailsAction.GoalUpdated(title, description, status, targetDate))
                }
            )
        }

        if (state.showAddTaskSheet && state.goal != null) {
            val addTaskViewModel: AddTaskViewModel = hiltViewModel()
            
            LaunchedEffect(state.goal.id) {
                addTaskViewModel.onAction(AddTaskAction.Initialize(goalId = state.goal.id))
            }

            ObserveAsEvents(addTaskViewModel.events) { event ->
                when (event) {
                    is AddTaskEvent.TaskCreated -> {
                        onAction(GoalDetailsAction.AddTaskDismissed)
                        onAction(GoalDetailsAction.Retry)
                    }
                    AddTaskEvent.Dismissed -> onAction(GoalDetailsAction.AddTaskDismissed)
                    else -> {}
                }
            }

            AddTaskSheet(
                onDismiss = { onAction(GoalDetailsAction.AddTaskDismissed) },
                viewModel = addTaskViewModel
            )
        }
    }
}
