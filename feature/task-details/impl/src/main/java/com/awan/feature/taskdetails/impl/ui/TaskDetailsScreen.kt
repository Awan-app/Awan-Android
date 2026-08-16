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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanDatePickerDialog
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTimePickerDialog
import com.awan.app.core.model.Goal
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import com.awan.feature.taskdetails.impl.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    val focusManager = LocalFocusManager.current

    val wrappedOnAction: (TaskDetailsAction) -> Unit = { action ->
        if (action !is TaskDetailsAction.TitleChanged && action !is TaskDetailsAction.DescriptionChanged) {
            focusManager.clearFocus()
        }
        onAction(action)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TaskDetailsTopBar(
                title = uiState.editTitle.ifBlank { stringResource(R.string.task_details_title_placeholder) },
                hasUnsavedChanges = uiState.hasUnsavedChanges,
                isSaving = uiState.isSaving,
                onBack = {
                    focusManager.clearFocus()
                    onNavigateBack()
                },
                onSave = {
                    focusManager.clearFocus()
                    onAction(TaskDetailsAction.SaveChanges)
                },
            )

            when {
                uiState.isLoading -> LoadingContent()
                uiState.errorMessage != null && uiState.task == null -> ErrorContent(
                    message = uiState.errorMessage.asString(),
                    onRetry = { wrappedOnAction(TaskDetailsAction.Retry) },
                )
                else -> DetailsContent(
                    uiState = uiState,
                    onAction = wrappedOnAction,
                )
            }
        }

        // ── Inline Error / Success Banner ────────────────────────────────────
        uiState.errorMessage?.let { msg ->
            if (uiState.task != null) {
                InlineBanner(
                    message = msg.asString(),
                    isError = true,
                    onDismiss = { onAction(TaskDetailsAction.DismissError) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }

        // ── Picker & Action Sheets ───────────────────────────────────────────
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

        if (uiState.showAddSessionSheet) {
            AddSessionBottomSheet(
                onConfirm = { drafts -> onAction(TaskDetailsAction.AddSessions(drafts)) },
                onDismiss = { onAction(TaskDetailsAction.DismissAddSessionSheet) },
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

        uiState.sessionToDelete?.let { session ->
            DeleteSessionConfirmSheet(
                session = session,
                isDeleting = uiState.isDeletingSession,
                onConfirm = { onAction(TaskDetailsAction.ConfirmDeleteSession) },
                onCancel = { onAction(TaskDetailsAction.CancelDeleteSession) },
            )
        }
    }
}

// ── Top App Bar ──────────────────────────────────────────────────────────────

@Composable
private fun TaskDetailsTopBar(
    title: String,
    hasUnsavedChanges: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
            text = stringResource(R.string.task_details_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        if (isSaving) {
            CircularProgressIndicator(
                color = AwanTheme.colors.sky,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 8.dp),
            )
        } else {
            AwanButton(
                onClick = onSave,
                variant = if (hasUnsavedChanges) AwanButtonVariant.Primary else AwanButtonVariant.Quiet,
                enabled = hasUnsavedChanges,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                AwanText(
                    text = stringResource(R.string.task_details_save),
                    style = AwanTheme.typography.button.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
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
    val timeFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d • hh:mm a") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }

        // ── 1. Hero Overview & Key Metrics ───────────────────────────────────
        item {
            TaskOverviewHeader(
                uiState = uiState,
                onStatusChange = { newStatus ->
                    onAction(TaskDetailsAction.StatusChanged(newStatus))
                },
            )
        }

        // ── 2. Title & Description Card ──────────────────────────────────────
        item {
            SectionCard {
                SectionLabel(stringResource(R.string.task_details_section_title))
                Spacer(Modifier.height(8.dp))
                InlineTextField(
                    value = uiState.editTitle,
                    onValueChange = { onAction(TaskDetailsAction.TitleChanged(it)) },
                    placeholder = stringResource(R.string.task_details_title_placeholder),
                    textStyle = AwanTheme.typography.heading.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))

                SectionLabel(stringResource(R.string.task_details_section_description))
                Spacer(Modifier.height(8.dp))
                InlineTextField(
                    value = uiState.editDescription,
                    onValueChange = { onAction(TaskDetailsAction.DescriptionChanged(it)) },
                    placeholder = stringResource(R.string.task_details_description_placeholder),
                    textStyle = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = AwanTheme.colors.textPrimary,
                        lineHeight = 20.sp,
                    ),
                    singleLine = false,
                )
            }
        }

        // ── 3. Scheduled Sessions Management (Core Feature) ──────────────────
        item {
            SessionsSectionCard(
                sessions = uiState.sessions,
                calculatedDurationMinutes = uiState.calculatedDurationMinutes,
                formatter = timeFormatter,
                onAddSession = { onAction(TaskDetailsAction.ShowAddSessionSheet) },
                onDeleteSession = { onAction(TaskDetailsAction.RequestDeleteSession(it)) },
            )
        }

        // ── 4. Task Rules & Controls ─────────────────────────────────────────
        item {
            SectionCard {
                SectionLabel(stringResource(R.string.task_details_section_details))
                Spacer(Modifier.height(14.dp))

                ToggleRow(
                    label = stringResource(R.string.task_details_mandatory_label),
                    subtitle = stringResource(R.string.task_details_mandatory_desc),
                    icon = Icons.Outlined.Lock,
                    checked = uiState.editMandatory,
                    onCheckedChange = { onAction(TaskDetailsAction.MandatoryToggled(it)) },
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                ToggleRow(
                    label = stringResource(R.string.task_details_splitting_label),
                    subtitle = stringResource(R.string.task_details_splitting_desc),
                    icon = Icons.Outlined.LockOpen,
                    checked = uiState.editAllowSplitting,
                    onCheckedChange = { onAction(TaskDetailsAction.AllowSplittingToggled(it)) },
                )
            }
        }

        // ── 5. Goal & Hierarchy ──────────────────────────────────────────────
        uiState.task?.let { task ->
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                        ) {
                            SectionLabel(stringResource(R.string.task_details_section_goal))
                            Spacer(Modifier.height(4.dp))
                            val currentGoal = uiState.goals.find { it.id == task.goalId }
                            val goalDisplayTitle = currentGoal?.title ?: if (task.goalId.isNullOrBlank()) {
                                stringResource(R.string.task_details_inbox)
                            } else {
                                stringResource(R.string.task_details_section_goal)
                            }
                            val isInbox = task.goalId.isNullOrBlank()
                            val goalIcon = if (isInbox) Icons.Outlined.Inbox else Icons.Outlined.Flag
                            val goalIconTint = if (isInbox) AwanTheme.colors.sky else AwanTheme.colors.zoneTangerine

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = goalIcon,
                                    contentDescription = null,
                                    tint = goalIconTint,
                                    modifier = Modifier.size(16.dp),
                                )
                                AwanText(
                                    text = goalDisplayTitle,
                                    style = AwanTheme.typography.body.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AwanTheme.colors.textPrimary,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                        AwanButton(
                            onClick = { onAction(TaskDetailsAction.ShowMoveGoalPicker) },
                            variant = AwanButtonVariant.Secondary,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                                contentDescription = null,
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            AwanText(
                                text = stringResource(R.string.task_details_move_goal),
                                style = AwanTheme.typography.button.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ── 6. Prerequisites & Dependencies ──────────────────────────────────
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        SectionLabel(stringResource(R.string.task_details_section_dependencies))
                        Spacer(Modifier.height(2.dp))
                        AwanText(
                            text = if (uiState.dependencies.isEmpty()) {
                                stringResource(R.string.task_details_no_dependencies)
                            } else {
                                "${uiState.dependencies.size} prerequisite task(s)"
                            },
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                color = AwanTheme.colors.textSecondary,
                            ),
                        )
                    }
                    IconButton(
                        onClick = { onAction(TaskDetailsAction.ShowAddDependencyPicker) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AwanTheme.colors.sky.copy(alpha = 0.12f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.task_details_add_dependency),
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (uiState.dependencies.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
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

        // ── 7. Downstream Dependents (Read-only) ──────────────────────────────
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

        // ── 8. Delete / Danger Zone ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AwanTheme.colors.destructive.copy(alpha = 0.08f))
                    .clickable { onAction(TaskDetailsAction.RequestDelete) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(20.dp),
                )
                AwanText(
                    text = stringResource(R.string.task_details_delete),
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.destructive,
                    ),
                )
            }
        }

        item { Spacer(Modifier.height(28.dp).navigationBarsPadding()) }
    }
}

// ── Overview Header & Quick Metrics ──────────────────────────────────────────

@Composable
private fun TaskOverviewHeader(
    uiState: TaskDetailsUiState,
    onStatusChange: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AwanTheme.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Status & Category Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatusChip(
                status = uiState.editStatus,
                onClick = {
                    val next = when (uiState.editStatus) {
                        TaskStatus.SCHEDULED -> TaskStatus.COMPLETED
                        TaskStatus.COMPLETED -> TaskStatus.SCHEDULED
                        TaskStatus.INBOX -> TaskStatus.COMPLETED
                        TaskStatus.CANCELLED -> TaskStatus.SCHEDULED
                        else -> TaskStatus.COMPLETED
                    }
                    onStatusChange(next)
                },
            )

            uiState.task?.category?.let { cat ->
                CategoryChip(name = cat.name)
            }
        }

        // Stats Summary Grid (Calculated Duration, Session count, Points)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Duration Stat Card
            MetricStatCard(
                icon = Icons.Outlined.Timer,
                iconTint = AwanTheme.colors.sky,
                value = formatMinutes(uiState.calculatedDurationMinutes),
                label = stringResource(R.string.task_details_total_duration),
                modifier = Modifier.weight(1f),
            )

            // Sessions Stat Card
            MetricStatCard(
                icon = Icons.Outlined.CalendarMonth,
                iconTint = AwanTheme.colors.zoneTangerine,
                value = "${uiState.sessions.size}",
                label = stringResource(
                    R.string.task_details_sessions_done,
                    uiState.sessions.count { it.status == SessionStatus.COMPLETED },
                ),
                modifier = Modifier.weight(1f),
            )

            // Reward Points Stat Card (Read-only)
            MetricStatCard(
                icon = Icons.Outlined.Stars,
                iconTint = AwanTheme.colors.streakIcon,
                value = stringResource(R.string.task_details_pts, uiState.editPoints),
                label = stringResource(R.string.task_details_reward),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricStatCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp),
            )
            AwanText(
                text = label,
                style = AwanTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    color = AwanTheme.colors.textSecondary,
                ),
                maxLines = 1,
            )
        }
        AwanText(
            text = value,
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textPrimary,
            ),
        )
    }
}

// ── Sessions Section Card ────────────────────────────────────────────────────

@Composable
private fun SessionsSectionCard(
    sessions: List<TaskSession>,
    calculatedDurationMinutes: Int,
    formatter: DateTimeFormatter,
    onAddSession: () -> Unit,
    onDeleteSession: (TaskSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        // Section Header
        SectionLabel(stringResource(R.string.task_details_section_sessions))

        // Subtitle with sessions count and total duration
        Spacer(Modifier.height(2.dp))
        AwanText(
            text = stringResource(
                R.string.task_details_session_count_duration,
                sessions.size,
                formatMinutes(calculatedDurationMinutes),
            ),
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                color = AwanTheme.colors.textSecondary,
            ),
        )

        Spacer(Modifier.height(12.dp))

        val sortedSessions = remember(sessions) {
            sessions.sortedBy { it.start }
        }

        // List of Sessions + Add New Session Card
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedSessions.forEach { session ->
                SessionRow(
                    session = session,
                    formatter = formatter,
                    onDelete = { onDeleteSession(session) },
                )
            }

            // Add New Session Card (matching session card design with + icon)
            AddNewSessionCard(onClick = onAddSession)
        }
    }
}

@Composable
private fun AddNewSessionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.background.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = AwanTheme.colors.sky.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.task_details_add_session),
            tint = AwanTheme.colors.sky,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        AwanText(
            text = stringResource(R.string.task_details_add_session),
            style = AwanTheme.typography.body.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AwanTheme.colors.sky,
            ),
        )
    }
}

@Composable
private fun SessionRow(
    session: TaskSession,
    formatter: DateTimeFormatter,
    onDelete: () -> Unit,
) {
    val isCompleted = session.status == SessionStatus.COMPLETED
    val statusColor = if (isCompleted) AwanTheme.colors.zoneGreen else AwanTheme.colors.zoneTangerine
    val durationMins = java.time.Duration.between(session.start, session.end).toMinutes().toInt().coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.background)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AwanText(
                        text = formatter.format(session.start),
                        style = AwanTheme.typography.body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.textPrimary,
                        ),
                    )
                    if (session.locked) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = stringResource(R.string.task_details_fixed_slot),
                            tint = AwanTheme.colors.textSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                AwanText(
                    text = "${session.start.format(DateTimeFormatter.ofPattern("hh:mm a"))} – ${session.end.format(DateTimeFormatter.ofPattern("hh:mm a"))} ($durationMins min)",
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Status Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                AwanText(
                    text = stringResource(
                        if (isCompleted) R.string.task_details_status_completed
                        else R.string.task_details_status_scheduled,
                    ),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    ),
                )
            }

            // Delete Session X Icon Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.task_details_delete_session_cd),
                    tint = AwanTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Add Session Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSessionBottomSheet(
    onConfirm: (List<SessionDraft>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var selectedDurationMinutes by remember { mutableIntStateOf(45) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateDisplayFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val timeDisplayFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    val startDateTime = selectedDate.atTime(startHour, startMinute)
    val endDateTime = startDateTime.plusMinutes(selectedDurationMinutes.toLong())

    if (showDatePicker) {
        AwanDatePickerDialog(
            initialDate = selectedDate,
            confirmLabel = stringResource(R.string.task_details_confirm),
            cancelLabel = stringResource(R.string.task_details_cancel),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                selectedDate = it
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = startHour * 60 + startMinute,
            confirmLabel = stringResource(R.string.task_details_confirm),
            cancelLabel = stringResource(R.string.task_details_cancel),
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                startHour = minutes / 60
                startMinute = minutes % 60
                showTimePicker = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    AwanText(
                        text = stringResource(R.string.task_details_add_session_title),
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AwanTheme.colors.textPrimary,
                        ),
                    )
                    AwanText(
                        text = stringResource(R.string.task_details_add_session_subtitle),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = AwanTheme.colors.textSecondary,
                    )
                }
            }

            // 1. Date Picker Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.task_details_session_date))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isToday = selectedDate == LocalDate.now()
                    val isTomorrow = selectedDate == LocalDate.now().plusDays(1)

                    QuickSelectionChip(
                        label = stringResource(R.string.task_details_today),
                        isSelected = isToday,
                        onClick = { selectedDate = LocalDate.now() },
                    )
                    QuickSelectionChip(
                        label = stringResource(R.string.task_details_tomorrow),
                        isSelected = isTomorrow,
                        onClick = { selectedDate = LocalDate.now().plusDays(1) },
                    )
                    QuickSelectionChip(
                        label = if (!isToday && !isTomorrow) dateDisplayFormatter.format(selectedDate) else "Custom Date",
                        isSelected = !isToday && !isTomorrow,
                        onClick = { showDatePicker = true },
                    )
                }
            }

            // 2. Start Time Picker Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.task_details_session_start_time))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AwanTheme.colors.background)
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp),
                        )
                        AwanText(
                            text = LocalTime.of(startHour, startMinute).format(timeDisplayFormatter),
                            style = AwanTheme.typography.body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AwanTheme.colors.textPrimary,
                            ),
                        )
                    }
                    AwanText(
                        text = "Change Time",
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.sky,
                        ),
                    )
                }
            }

            // 3. Duration Quick Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.task_details_session_duration))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        QuickSelectionChip(
                            label = "$mins min",
                            isSelected = selectedDurationMinutes == mins,
                            onClick = { selectedDurationMinutes = mins },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 4. Live Session Summary Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AwanTheme.colors.sky.copy(alpha = 0.08f))
                    .border(1.dp, AwanTheme.colors.sky.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    AwanText(
                        text = "${selectedDate.format(dateDisplayFormatter)} • ${startDateTime.format(timeDisplayFormatter)} – ${endDateTime.format(timeDisplayFormatter)}",
                        style = AwanTheme.typography.body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.textPrimary,
                        ),
                    )
                    AwanText(
                        text = "$selectedDurationMinutes minutes focus block",
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                }
            }

            // Confirm Add Button
            AwanButton(
                onClick = {
                    val draft = SessionDraft(
                        start = startDateTime,
                        end = endDateTime,
                        zoneId = null,
                    )
                    onConfirm(listOf(draft))
                },
                variant = AwanButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(
                    text = stringResource(R.string.task_details_confirm_add_session),
                    style = AwanTheme.typography.button.copy(fontSize = 14.sp),
                )
            }
        }
    }
}

@Composable
private fun QuickSelectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.15f)
                else AwanTheme.colors.background,
            )
            .border(
                width = 1.dp,
                color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        AwanText(
            text = label,
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textPrimary,
            ),
        )
    }
}

// ── Reusable Base Components ──────────────────────────────────────────────────

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
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(AwanTheme.colors.sky),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() },
        ),
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

private data class StatusConfig(
    val bg: Color,
    val fg: Color,
    val icon: ImageVector,
    val labelRes: Int,
)

@Composable
private fun StatusChip(status: TaskStatus, onClick: () -> Unit) {
    val config = when (status) {
        TaskStatus.COMPLETED -> StatusConfig(
            bg = AwanTheme.colors.zoneGreen.copy(alpha = 0.18f),
            fg = AwanTheme.colors.zoneGreen,
            icon = Icons.Outlined.CheckCircle,
            labelRes = R.string.task_details_status_completed,
        )
        TaskStatus.SCHEDULED -> StatusConfig(
            bg = AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f),
            fg = AwanTheme.colors.zoneTangerine,
            icon = Icons.Outlined.EventAvailable,
            labelRes = R.string.task_details_status_scheduled,
        )
        TaskStatus.INBOX -> StatusConfig(
            bg = AwanTheme.colors.sky.copy(alpha = 0.15f),
            fg = AwanTheme.colors.sky,
            icon = Icons.Outlined.Inbox,
            labelRes = R.string.task_details_status_inbox,
        )
        TaskStatus.CANCELLED -> StatusConfig(
            bg = AwanTheme.colors.destructive.copy(alpha = 0.15f),
            fg = AwanTheme.colors.destructive,
            icon = Icons.Outlined.Cancel,
            labelRes = R.string.task_details_status_cancelled,
        )
        else -> StatusConfig(
            bg = AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f),
            fg = AwanTheme.colors.zoneTangerine,
            icon = Icons.Outlined.EventAvailable,
            labelRes = R.string.task_details_status_scheduled,
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(config.bg)
            .border(1.dp, config.fg.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.fg,
            modifier = Modifier.size(13.dp),
        )
        AwanText(
            text = stringResource(config.labelRes),
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = config.fg,
            ),
        )
    }
}

@Composable
private fun CategoryChip(name: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AwanTheme.colors.background)
            .border(1.dp, AwanTheme.colors.line.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = AwanTheme.colors.textSecondary,
            modifier = Modifier.size(13.dp),
        )
        AwanText(
            text = name,
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                color = AwanTheme.colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String? = null,
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AwanTheme.colors.sky,
                modifier = Modifier.size(18.dp),
            )
            Column {
                AwanText(
                    text = label,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                if (subtitle != null) {
                    AwanText(
                        text = subtitle,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            color = AwanTheme.colors.textSecondary,
                            lineHeight = 15.sp,
                        ),
                    )
                }
            }
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = AwanTheme.colors.sky,
                modifier = Modifier.size(15.dp),
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.EventAvailable,
            contentDescription = null,
            tint = AwanTheme.colors.textSecondary,
            modifier = Modifier.size(15.dp),
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

// ── Goal / Dependency / Delete Bottom Sheets ──────────────────────────────────

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
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
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.line.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
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
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(28.dp),
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
                        variant = AwanButtonVariant.Destructive,
                        isLoading = isDeleting,
                        enabled = !isDeleting,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AwanText(
                            text = stringResource(R.string.task_details_delete_cascade_confirm),
                            style = AwanTheme.typography.button,
                        )
                    }
                } else {
                    AwanButton(
                        onClick = onDeleteSimple,
                        variant = AwanButtonVariant.Destructive,
                        isLoading = isDeleting,
                        enabled = !isDeleting,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AwanText(
                            text = stringResource(R.string.task_details_delete_confirm),
                            style = AwanTheme.typography.button,
                        )
                    }
                }
                AwanButton(
                    onClick = onCancel,
                    variant = AwanButtonVariant.Secondary,
                    enabled = !isDeleting,
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
private fun DeleteSessionConfirmSheet(
    session: TaskSession,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val durationMins = java.time.Duration.between(session.start, session.end).toMinutes().toInt().coerceAtLeast(0)
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d • hh:mm a") }
    val endFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.line.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.destructive.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            AwanText(
                text = stringResource(R.string.task_details_delete_session_confirm_title),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(Modifier.height(8.dp))

            AwanText(
                text = stringResource(R.string.task_details_delete_session_confirm_body),
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.sp,
                    color = AwanTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Session Details Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AwanTheme.colors.background)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.zoneTangerine),
                )
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = formatter.format(session.start),
                        style = AwanTheme.typography.body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.textPrimary,
                        ),
                    )
                    AwanText(
                        text = "${session.start.format(DateTimeFormatter.ofPattern("hh:mm a"))} – ${endFormatter.format(session.end)} ($durationMins min)",
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AwanButton(
                    onClick = onConfirm,
                    variant = AwanButtonVariant.Destructive,
                    enabled = !isDeleting,
                    isLoading = isDeleting,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AwanText(
                        text = stringResource(R.string.task_details_delete_session_btn),
                        style = AwanTheme.typography.button,
                    )
                }

                AwanButton(
                    onClick = onCancel,
                    variant = AwanButtonVariant.Secondary,
                    enabled = !isDeleting,
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

// ── Helpers & Common UI ───────────────────────────────────────────────────────

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
private fun InlineBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isError) AwanTheme.colors.destructive.copy(alpha = 0.94f) else AwanTheme.colors.zoneGreen.copy(alpha = 0.94f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
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

private fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0 min"
    val hours = minutes / 60
    val remMins = minutes % 60
    return when {
        hours > 0 && remMins > 0 -> "${hours}h ${remMins}m"
        hours > 0 -> "${hours}h"
        else -> "${remMins} min"
    }
}
