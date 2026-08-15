@file:Suppress("NewApi")

package com.awan.feature.taskdetails.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import com.awan.feature.taskdetails.impl.R
import java.time.format.DateTimeFormatter

// ── Root Composable (collects state) ────────────────────────────────────────

@Composable
fun TaskDetailsRoot(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskDeleted by viewModel.taskDeletedEvent.collectAsStateWithLifecycle()

    LaunchedEffect(taskId) {
        viewModel.initTaskId(taskId)
    }

    LaunchedEffect(taskDeleted) {
        if (taskDeleted) {
            viewModel.onTaskDeletedEventConsumed()
            onNavigateBack()
        }
    }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !showUnsavedChangesDialog) {
        if (uiState.hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    if (showUnsavedChangesDialog) {
        UnsavedChangesBottomSheet(
            onKeepEditing = { showUnsavedChangesDialog = false },
            onDiscard = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            },
            onSave = {
                showUnsavedChangesDialog = false
                viewModel.onAction(TaskDetailsAction.SaveChanges)
            },
        )
    }

    TaskDetailsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = {
            if (uiState.hasUnsavedChanges) showUnsavedChangesDialog = true
            else onNavigateBack()
        },
    )
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
internal fun TaskDetailsScreen(
    uiState: TaskDetailsUiState,
    onAction: (TaskDetailsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top App Bar ──────────────────────────────────────────────────
            TaskDetailsTopBar(
                title = uiState.editTitle.ifBlank { stringResource(R.string.task_details_title_placeholder) },
                isSaving = uiState.isSaving,
                onBack = onNavigateBack,
                onSave = { onAction(TaskDetailsAction.SaveChanges) },
            )

            when {
                uiState.isLoading -> LoadingContent()
                uiState.errorMessage != null && uiState.task == null -> ErrorContent(
                    message = uiState.errorMessage.asString(),
                    onRetry = { onAction(TaskDetailsAction.Retry) },
                )
                else -> DetailsContent(
                    uiState = uiState,
                    onAction = onAction,
                )
            }
        }

        // ── Inline Error Toast ───────────────────────────────────────────────
        uiState.errorMessage?.let { msg ->
            if (uiState.task != null) {
                InlineErrorBanner(
                    message = msg.asString(),
                    onDismiss = { onAction(TaskDetailsAction.DismissError) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }

        // ── Picker Sheets ───────────────────────────────────────────────────
        if (uiState.showMoveGoalPicker) {
            GoalPickerSheet(
                goals = uiState.goals,
                currentGoalId = uiState.task?.goalId,
                onGoalSelected = { onAction(TaskDetailsAction.MoveToGoal(it)) },
                onDismiss = { onAction(TaskDetailsAction.DismissMoveGoalPicker) },
            )
        }

        if (uiState.showAddDependencyPicker && uiState.task != null) {
            AddDependencySheet(
                availableTasks = uiState.availableDependencyTasks(emptyList()),
                onTaskSelected = { onAction(TaskDetailsAction.AddDependency(it)) },
                onDismiss = { onAction(TaskDetailsAction.DismissAddDependencyPicker) },
            )
        }

        if (uiState.showDeleteConfirm) {
            DeleteConfirmSheet(
                isDeleting = uiState.isDeleting,
                showCascadeOption = uiState.showDeleteCascadeOption,
                onDeleteSimple = { onAction(TaskDetailsAction.ConfirmDelete(false)) },
                onDeleteCascade = { onAction(TaskDetailsAction.ConfirmDelete(true)) },
                onCancel = { onAction(TaskDetailsAction.CancelDelete) },
            )
        }
    }
}

// ── Top App Bar ──────────────────────────────────────────────────────────────

@Composable
private fun TaskDetailsTopBar(
    title: String,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.task_details_back),
                tint = AwanTheme.colors.textPrimary,
            )
        }
        AwanText(
            text = title,
            style = AwanTheme.typography.heading.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = AwanTheme.colors.textPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        if (isSaving) {
            CircularProgressIndicator(
                color = AwanTheme.colors.sky,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp).padding(end = 8.dp),
            )
        } else {
            AwanButton(
                onClick = onSave,
                variant = AwanButtonVariant.Primary,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                AwanText(
                    text = stringResource(R.string.task_details_save),
                    style = AwanTheme.typography.button.copy(fontSize = 13.sp),
                )
            }
        }
    }
}

// ── Details Content (Scrollable) ─────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(
    uiState: TaskDetailsUiState,
    onAction: (TaskDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // ── Status & Category header ─────────────────────────────────────────
        uiState.task?.let { task ->
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(
                        status = uiState.editStatus,
                        onClick = {
                            // cycle through statuses
                            val next = when (uiState.editStatus) {
                                TaskStatus.SCHEDULED -> TaskStatus.COMPLETED
                                TaskStatus.COMPLETED -> TaskStatus.SCHEDULED
                                else -> TaskStatus.SCHEDULED
                            }
                            onAction(TaskDetailsAction.StatusChanged(next))
                        },
                    )
                    task.category?.let { cat ->
                        InfoChip(text = "📁 ${cat.name}")
                    }
                    PointsBadge(points = uiState.editPoints)
                }
            }
        }

        // ── Title ────────────────────────────────────────────────────────────
        item {
            SectionCard {
                SectionLabel(stringResource(R.string.task_details_section_title))
                Spacer(Modifier.height(8.dp))
                InlineTextField(
                    value = uiState.editTitle,
                    onValueChange = { onAction(TaskDetailsAction.TitleChanged(it)) },
                    placeholder = stringResource(R.string.task_details_title_placeholder),
                    textStyle = AwanTheme.typography.heading.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
            }
        }

        // ── Description ──────────────────────────────────────────────────────
        item {
            SectionCard {
                SectionLabel(stringResource(R.string.task_details_section_description))
                Spacer(Modifier.height(8.dp))
                InlineTextField(
                    value = uiState.editDescription,
                    onValueChange = { onAction(TaskDetailsAction.DescriptionChanged(it)) },
                    placeholder = stringResource(R.string.task_details_description_placeholder),
                    textStyle = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = AwanTheme.colors.textPrimary,
                    ),
                    singleLine = false,
                )
            }
        }

        // ── Details Card ─────────────────────────────────────────────────────
        item {
            SectionCard {
                SectionLabel(stringResource(R.string.task_details_section_details))
                Spacer(Modifier.height(12.dp))

                // Duration stepper
                StepperRow(
                    label = stringResource(R.string.task_details_duration_label),
                    valueText = "${uiState.editDuration} ${stringResource(R.string.task_details_minutes)}",
                    onDecrement = {
                        if (uiState.editDuration > 15) onAction(TaskDetailsAction.DurationChanged(uiState.editDuration - 15))
                    },
                    onIncrement = {
                        onAction(TaskDetailsAction.DurationChanged(uiState.editDuration + 15))
                    },
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // Points stepper
                StepperRow(
                    label = stringResource(R.string.task_details_points_label),
                    valueText = "${uiState.editPoints} ⭐",
                    onDecrement = {
                        if (uiState.editPoints > 0) onAction(TaskDetailsAction.PointsChanged(uiState.editPoints - 5))
                    },
                    onIncrement = {
                        onAction(TaskDetailsAction.PointsChanged(uiState.editPoints + 5))
                    },
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // Mandatory toggle
                ToggleRow(
                    label = stringResource(R.string.task_details_mandatory_label),
                    icon = Icons.Outlined.Lock,
                    checked = uiState.editMandatory,
                    onCheckedChange = { onAction(TaskDetailsAction.MandatoryToggled(it)) },
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // Allow Splitting toggle
                ToggleRow(
                    label = stringResource(R.string.task_details_splitting_label),
                    icon = Icons.Outlined.LockOpen,
                    checked = uiState.editAllowSplitting,
                    onCheckedChange = { onAction(TaskDetailsAction.AllowSplittingToggled(it)) },
                )
            }
        }

        // ── Goal Assignment ──────────────────────────────────────────────────
        uiState.task?.let { task ->
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionLabel(stringResource(R.string.task_details_section_goal))
                        AwanButton(
                            onClick = { onAction(TaskDetailsAction.ShowMoveGoalPicker) },
                            variant = AwanButtonVariant.Secondary,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                                contentDescription = null,
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            AwanText(
                                text = stringResource(R.string.task_details_move_goal),
                                style = AwanTheme.typography.button.copy(fontSize = 12.sp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AwanText(
                        text = task.goalId ?: stringResource(R.string.task_details_inbox),
                        style = AwanTheme.typography.body.copy(
                            fontSize = 14.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                }
            }
        }

        // ── Dependencies ─────────────────────────────────────────────────────
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionLabel(stringResource(R.string.task_details_section_dependencies))
                    IconButton(
                        onClick = { onAction(TaskDetailsAction.ShowAddDependencyPicker) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.task_details_add_dependency),
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (uiState.dependencies.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    AwanText(
                        text = stringResource(R.string.task_details_no_dependencies),
                        style = AwanTheme.typography.body.copy(
                            fontSize = 13.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.dependencies.forEach { dep ->
                            DependencyRow(
                                task = dep,
                                onRemove = { onAction(TaskDetailsAction.RemoveDependency(dep.id)) },
                            )
                        }
                    }
                }
            }
        }

        // ── Dependents (read-only) ────────────────────────────────────────────
        if (uiState.dependents.isNotEmpty()) {
            item {
                SectionCard {
                    SectionLabel(stringResource(R.string.task_details_section_dependents))
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.dependents.forEach { dep ->
                            DependentChip(task = dep)
                        }
                    }
                }
            }
        }

        // ── Sessions ─────────────────────────────────────────────────────────
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionLabel(stringResource(R.string.task_details_section_sessions))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Schedule with AI button
                        IconButton(
                            onClick = { onAction(TaskDetailsAction.ScheduleWithAi) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            if (uiState.isScheduling) {
                                CircularProgressIndicator(
                                    color = AwanTheme.colors.sky,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Psychology,
                                    contentDescription = stringResource(R.string.task_details_schedule_ai),
                                    tint = AwanTheme.colors.sky,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        // Add session manually
                        IconButton(
                            onClick = { onAction(TaskDetailsAction.ShowAddSessionSheet) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.task_details_add_session),
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                if (uiState.sessions.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    AwanText(
                        text = stringResource(R.string.task_details_no_sessions),
                        style = AwanTheme.typography.body.copy(
                            fontSize = 13.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.sessions.forEach { session ->
                            SessionRow(session = session, formatter = timeFormatter)
                        }
                    }
                }
            }
        }

        // ── Delete ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AwanTheme.colors.destructive.copy(alpha = 0.08f))
                    .clickable { onAction(TaskDetailsAction.RequestDelete) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(22.dp),
                )
                AwanText(
                    text = stringResource(R.string.task_details_delete),
                    style = AwanTheme.typography.body.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.destructive,
                    ),
                )
            }
        }

        item { Spacer(Modifier.height(32.dp).navigationBarsPadding()) }
    }
}

// ── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AwanTheme.colors.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    AwanText(
        text = text.uppercase(),
        style = AwanTheme.typography.caption.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AwanTheme.colors.textSecondary,
            letterSpacing = 0.8.sp,
        ),
    )
}

@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(AwanTheme.colors.sky),
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    AwanText(
                        text = placeholder,
                        style = textStyle.copy(color = AwanTheme.colors.textSecondary.copy(alpha = 0.5f)),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun StatusChip(status: TaskStatus, onClick: () -> Unit) {
    val (bg, fg, label) = when (status) {
        TaskStatus.COMPLETED -> Triple(AwanTheme.colors.zoneGreen.copy(alpha = 0.18f), AwanTheme.colors.zoneGreen, "✅ Completed")
        TaskStatus.SCHEDULED -> Triple(AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f), AwanTheme.colors.zoneTangerine, "📅 Scheduled")
        TaskStatus.INBOX     -> Triple(AwanTheme.colors.sky.copy(alpha = 0.15f), AwanTheme.colors.sky, "📥 Inbox")
        TaskStatus.CANCELLED -> Triple(AwanTheme.colors.destructive.copy(alpha = 0.15f), AwanTheme.colors.destructive, "❌ Cancelled")
        else                 -> Triple(AwanTheme.colors.line.copy(alpha = 0.15f), AwanTheme.colors.textSecondary, "— Unknown")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        AwanText(
            text = label,
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            ),
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AwanTheme.colors.surface)
            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        AwanText(
            text = text,
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                color = AwanTheme.colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun PointsBadge(points: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AwanTheme.colors.streakSurface.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        AwanText(
            text = "⭐ $points pts",
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AwanTheme.colors.streakIcon,
            ),
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AwanText(
            text = label,
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = AwanTheme.colors.textPrimary,
            ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.15f))
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(text = "−", style = AwanTheme.typography.body.copy(color = AwanTheme.colors.sky, fontWeight = FontWeight.Bold))
            }
            AwanText(
                text = valueText,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AwanTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.width(80.dp),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.15f))
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(text = "+", style = AwanTheme.typography.body.copy(color = AwanTheme.colors.sky, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            AwanText(
                text = label,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AwanTheme.colors.sky,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AwanTheme.colors.line,
            ),
        )
    }
}

@Composable
private fun DependencyRow(task: Task, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AwanTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = AwanTheme.colors.sky,
                modifier = Modifier.size(14.dp),
            )
            AwanText(
                text = task.title,
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.sp,
                    color = AwanTheme.colors.textPrimary,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun DependentChip(task: Task) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AwanTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.EventAvailable,
            contentDescription = null,
            tint = AwanTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
        AwanText(
            text = task.title,
            style = AwanTheme.typography.body.copy(
                fontSize = 13.sp,
                color = AwanTheme.colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun SessionRow(
    session: TaskSession,
    formatter: DateTimeFormatter,
) {
    val statusColor = when (session.status) {
        com.awan.app.core.model.SessionStatus.COMPLETED -> AwanTheme.colors.zoneGreen
        else -> AwanTheme.colors.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AwanTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Column {
                AwanText(
                    text = formatter.format(session.start),
                    style = AwanTheme.typography.body.copy(
                        fontSize = 13.sp,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                AwanText(
                    text = "→ ${formatter.format(session.end)}",
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
            }
        }
    }
}

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalPickerSheet(
    goals: List<Goal>,
    currentGoalId: String?,
    onGoalSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            AwanText(
                text = stringResource(R.string.task_details_pick_goal),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
            Spacer(Modifier.height(16.dp))
            if (goals.isEmpty()) {
                AwanText(
                    text = stringResource(R.string.task_details_no_goals),
                    style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goals) { goal ->
                        val isSelected = goal.id == currentGoalId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.12f)
                                    else AwanTheme.colors.background,
                                )
                                .clickable { onGoalSelected(goal.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AwanText(text = goal.emoji, style = AwanTheme.typography.body.copy(fontSize = 20.sp))
                            AwanText(
                                text = goal.title,
                                style = AwanTheme.typography.body.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = AwanTheme.colors.textPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AwanTheme.colors.sky,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDependencySheet(
    availableTasks: List<Task>,
    onTaskSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            AwanText(
                text = stringResource(R.string.task_details_pick_dependency),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
            Spacer(Modifier.height(4.dp))
            AwanText(
                text = stringResource(R.string.task_details_pick_dependency_subtitle),
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.sp,
                    color = AwanTheme.colors.textSecondary,
                ),
            )
            Spacer(Modifier.height(16.dp))
            if (availableTasks.isEmpty()) {
                AwanText(
                    text = stringResource(R.string.task_details_no_available_deps),
                    style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTasks) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AwanTheme.colors.background)
                                .clickable { onTaskSelected(task.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TaskAlt,
                                contentDescription = null,
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(16.dp),
                            )
                            AwanText(
                                text = task.title,
                                style = AwanTheme.typography.body.copy(
                                    fontSize = 14.sp,
                                    color = AwanTheme.colors.textPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmSheet(
    isDeleting: Boolean,
    showCascadeOption: Boolean,
    onDeleteSimple: () -> Unit,
    onDeleteCascade: () -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.destructive.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(26.dp),
                )
            }
            AwanText(
                text = stringResource(
                    if (showCascadeOption) R.string.task_details_delete_cascade_title
                    else R.string.task_details_delete_confirm_title
                ),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )
            AwanText(
                text = stringResource(
                    if (showCascadeOption) R.string.task_details_delete_cascade_body
                    else R.string.task_details_delete_confirm_body
                ),
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = AwanTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                ),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showCascadeOption) {
                    AwanButton(
                        onClick = onDeleteCascade,
                        variant = AwanButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AwanText(
                            text = stringResource(R.string.task_details_delete_cascade_confirm),
                            style = AwanTheme.typography.button.copy(color = AwanTheme.colors.destructive),
                        )
                    }
                } else {
                    AwanButton(
                        onClick = onDeleteSimple,
                        variant = AwanButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDeleting,
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                color = AwanTheme.colors.destructive,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            AwanText(
                                text = stringResource(R.string.task_details_delete_confirm),
                                style = AwanTheme.typography.button.copy(color = AwanTheme.colors.destructive),
                            )
                        }
                    }
                }
                AwanButton(
                    onClick = onCancel,
                    variant = AwanButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AwanText(
                        text = stringResource(R.string.task_details_cancel),
                        style = AwanTheme.typography.button,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnsavedChangesBottomSheet(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onKeepEditing,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AwanText(
                text = stringResource(R.string.task_details_unsaved_title),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AwanButton(onClick = onSave, variant = AwanButtonVariant.Primary, modifier = Modifier.fillMaxWidth()) {
                    AwanText(text = stringResource(R.string.task_details_unsaved_save), style = AwanTheme.typography.button)
                }
                AwanButton(onClick = onKeepEditing, variant = AwanButtonVariant.Secondary, modifier = Modifier.fillMaxWidth()) {
                    AwanText(text = stringResource(R.string.task_details_unsaved_keep_editing), style = AwanTheme.typography.button)
                }
                AwanButton(onClick = onDiscard, variant = AwanButtonVariant.Secondary, modifier = Modifier.fillMaxWidth()) {
                    AwanText(
                        text = stringResource(R.string.task_details_unsaved_discard),
                        style = AwanTheme.typography.button.copy(color = AwanTheme.colors.destructive),
                    )
                }
            }
        }
    }
}

// ── Loading / Error ───────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AwanTheme.colors.sky,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(16.dp))
            AwanText(
                text = stringResource(R.string.task_details_loading),
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = AwanTheme.colors.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AwanText(text = "☁️", style = AwanTheme.typography.display.copy(fontSize = 48.sp))
            AwanText(
                text = message,
                style = AwanTheme.typography.body.copy(
                    fontSize = 15.sp,
                    color = AwanTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
            )
            AwanButton(onClick = onRetry, variant = AwanButtonVariant.Primary) {
                AwanText(text = stringResource(R.string.task_details_retry), style = AwanTheme.typography.button)
            }
        }
    }
}

@Composable
private fun InlineErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.destructive.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AwanText(
                text = message,
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.sp,
                    color = Color.White,
                ),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}


