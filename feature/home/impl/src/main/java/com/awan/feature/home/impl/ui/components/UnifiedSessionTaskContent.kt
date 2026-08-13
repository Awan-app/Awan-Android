package com.awan.feature.home.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.formatTime
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskStatus
import com.awan.feature.home.impl.R
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch

private enum class TimePickerTarget { START_TIME, END_TIME }

@Composable
internal fun UnifiedSessionTaskContent(
    detail: SessionTaskDetail,
    editStartMinutes: Int,
    editEndMinutes: Int,
    editDurationMinutes: Int,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onStartMinutesChange: (Int) -> Unit,
    onEndMinutesChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onConfirmClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeTimePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

    AnimatedContent(
        targetState = activeTimePickerTarget,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "DialogViewTransition",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        if (target == null) {
            MainSessionTaskDetailView(
                detail = detail,
                editStartMinutes = editStartMinutes,
                editEndMinutes = editEndMinutes,
                editDurationMinutes = editDurationMinutes,
                onToggleStatus = onToggleStatus,
                onToggleLock = onToggleLock,
                onStartMinutesChange = onStartMinutesChange,
                onEndMinutesChange = onEndMinutesChange,
                onDurationChange = onDurationChange,
                onOpenTimePicker = { activeTimePickerTarget = it },
                onDeleteClick = onDeleteClick,
                onConfirmClose = onConfirmClose,
            )
        } else {
            val titleText = if (target == TimePickerTarget.START_TIME) {
                stringResource(R.string.home_edit_label_start_time)
            } else {
                stringResource(R.string.home_edit_label_end_time)
            }
            val initialMins = if (target == TimePickerTarget.START_TIME) editStartMinutes else editEndMinutes

            CustomWheelTimePickerView(
                title = titleText,
                initialMinutes = initialMins,
                onConfirm = { selectedMins ->
                    if (target == TimePickerTarget.START_TIME) {
                        onStartMinutesChange(selectedMins)
                    } else {
                        onEndMinutesChange(selectedMins)
                    }
                    activeTimePickerTarget = null
                },
                onBack = { activeTimePickerTarget = null },
            )
        }
    }
}

@Composable
private fun MainSessionTaskDetailView(
    detail: SessionTaskDetail,
    editStartMinutes: Int,
    editEndMinutes: Int,
    editDurationMinutes: Int,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onStartMinutesChange: (Int) -> Unit,
    onEndMinutesChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onOpenTimePicker: (TimePickerTarget) -> Unit,
    onDeleteClick: (() -> Unit)?,
    onConfirmClose: () -> Unit = {},
) {
    val isCompleted = detail.session.status == SessionStatus.COMPLETED ||
        detail.task.status == TaskStatus.COMPLETED
    val isLocked = detail.session.locked

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Top Header Bar (Category Chip, Compact Points Badge & Delete Button) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val defaultCategory = stringResource(R.string.home_task_default_category)
                val categoryName = (detail.task.categoryName ?: defaultCategory).uppercase()

                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AwanTheme.colors.sky.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    AwanText(
                        text = categoryName,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AwanTheme.colors.sky,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                }

                // Compact Points Badge
                if (detail.task.estimatedPoints > 0) {
                    val badgeShape = CircleShape
                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(AwanTheme.colors.pointsSurface)
                            .border(1.dp, AwanTheme.colors.pointsIcon.copy(alpha = 0.25f), badgeShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Lucide.Coins,
                                contentDescription = null,
                                tint = AwanTheme.colors.pointsIcon,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AwanText(
                                text = stringResource(R.string.home_task_points_value, detail.task.estimatedPoints),
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AwanTheme.colors.pointsIcon,
                                ),
                            )
                        }
                    }
                }
            }

            if (onDeleteClick != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.destructive.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = AwanTheme.colors.destructive.copy(alpha = 0.20f),
                            shape = CircleShape,
                        )
                        .clickable(onClick = onDeleteClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.home_action_delete),
                        tint = AwanTheme.colors.destructive,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Task Title
            AwanText(
                text = detail.task.title,
                style = AwanTheme.typography.heading.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                    lineHeight = 26.sp,
                ),
            )

            // Task Description (if present)
            detail.task.description?.takeIf { it.isNotBlank() }?.let { desc ->
                AwanText(
                    text = desc,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = AwanTheme.colors.textSecondary,
                        lineHeight = 20.sp,
                    ),
                )
            }

            HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.4f))

            // --- SESSION DETAILS & LIVE TIME CONTROLS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AwanText(
                        text = stringResource(R.string.home_session_details_title).uppercase(),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AwanTheme.colors.textSecondary,
                            letterSpacing = 0.8.sp,
                        ),
                    )

                    // Modern Session Status & Locked Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Session Status Chip
                        val chipStyle = when (detail.session.status) {
                            SessionStatus.COMPLETED -> StatusChipStyle(
                                stringResource(R.string.home_status_completed),
                                AwanTheme.colors.success.copy(alpha = 0.15f),
                                AwanTheme.colors.success,
                                Icons.Default.CheckCircle,
                            )
                            SessionStatus.IN_PROGRESS -> StatusChipStyle(
                                stringResource(R.string.home_status_in_progress),
                                AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f),
                                AwanTheme.colors.zoneTangerine,
                                Icons.Default.PlayArrow,
                            )
                            else -> StatusChipStyle(
                                stringResource(R.string.home_status_scheduled),
                                AwanTheme.colors.sky.copy(alpha = 0.15f),
                                AwanTheme.colors.sky,
                                Icons.Default.Schedule,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(chipStyle.bg)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = chipStyle.icon,
                                    contentDescription = null,
                                    tint = chipStyle.fg,
                                    modifier = Modifier.size(12.dp),
                                )
                                AwanText(
                                    text = chipStyle.text,
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipStyle.fg,
                                    ),
                                )
                            }
                        }

                        // Session Locked Chip
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isLocked) AwanTheme.colors.sky.copy(alpha = 0.15f)
                                    else AwanTheme.colors.line.copy(alpha = 0.3f)
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isLocked) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
                                    modifier = Modifier.size(12.dp),
                                )
                                AwanText(
                                    text = if (isLocked) stringResource(R.string.home_session_locked_yes)
                                    else stringResource(R.string.home_session_locked_no),
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLocked) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
                                    ),
                                )
                            }
                        }
                    }
                }

                // Interactive Live Time Adjuster Card (Start Time & End Time)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AwanTheme.colors.surface)
                        .border(
                            width = 1.dp,
                            color = AwanTheme.colors.line.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TimeStepperCard(
                                label = stringResource(R.string.home_edit_label_start_time),
                                minutesValue = editStartMinutes,
                                onDecrease = { onStartMinutesChange(editStartMinutes - 15) },
                                onIncrease = { onStartMinutesChange(editStartMinutes + 15) },
                                onTimeDisplayClick = { onOpenTimePicker(TimePickerTarget.START_TIME) },
                                modifier = Modifier.weight(1f),
                            )

                            TimeStepperCard(
                                label = stringResource(R.string.home_edit_label_end_time),
                                minutesValue = editEndMinutes,
                                onDecrease = { onEndMinutesChange(editEndMinutes - 15) },
                                onIncrease = { onEndMinutesChange(editEndMinutes + 15) },
                                onTimeDisplayClick = { onOpenTimePicker(TimePickerTarget.END_TIME) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Duration Chips Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AwanText(
                                text = stringResource(R.string.home_edit_label_duration),
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AwanTheme.colors.textSecondary,
                                ),
                            )
                            val durationOptions = listOf(15, 30, 45, 60, 90, 120)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                durationOptions.forEach { duration ->
                                    val isSelected = editDurationMinutes == duration
                                    val backgroundColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface
                                    val textColor = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(backgroundColor)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                            .clickable { onDurationChange(duration) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AwanText(
                                            text = "${duration}m",
                                            style = AwanTheme.typography.caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                textAlign = TextAlign.Center,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Action Buttons Bar (Save Changes & Lock/Unlock using AwanButton design system) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Save Changes Button (closes the bottom sheet dialog on click)
            AwanButton(
                onClick = onConfirmClose,
                variant = AwanButtonVariant.Primary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                AwanText(
                    text = stringResource(R.string.home_action_save_changes),
                    style = AwanTheme.typography.button.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Lock / Unlock Button
            AwanButton(
                onClick = onToggleLock,
                variant = if (isLocked) AwanButtonVariant.Primary else AwanButtonVariant.Secondary,
                icon = {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                AwanText(
                    text = if (isLocked) stringResource(R.string.home_action_unlock)
                    else stringResource(R.string.home_action_lock),
                    style = AwanTheme.typography.button.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TimeStepperCard(
    label: String,
    minutesValue: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onTimeDisplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedStr = formatTime(minutesValue)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AwanTheme.colors.line.copy(alpha = 0.12f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AwanText(
            text = label,
            style = AwanTheme.typography.caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AwanTheme.colors.textSecondary,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AwanTheme.colors.surface)
                    .border(1.dp, AwanTheme.colors.sky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onTimeDisplayClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = formattedStr,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AwanTheme.colors.sky,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomWheelTimePickerView(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedHour24 by remember { mutableStateOf(initialMinutes / 60) }
    var selectedMinute by remember { mutableStateOf(initialMinutes % 60) }

    val isAm = selectedHour24 < 12
    val hour12 = when {
        selectedHour24 == 0 -> 12
        selectedHour24 > 12 -> selectedHour24 - 12
        else -> selectedHour24
    }

    // Infinite Wheel Cycle Math (12,000 items centered at 6,000)
    val cycleBase = 6_000
    val initialHourIndex = cycleBase - (cycleBase % 12) + (hour12 - 1)
    val initialMinuteIndex = cycleBase - (cycleBase % 60) + selectedMinute

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = initialHourIndex)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinuteIndex)

    val hourSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minuteSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = minuteListState)

    val currentCenteredHourIndex by remember {
        derivedStateOf { hourListState.firstVisibleItemIndex }
    }
    LaunchedEffect(currentCenteredHourIndex) {
        val selectedHourVal = (currentCenteredHourIndex % 12) + 1
        selectedHour24 = if (isAm) {
            if (selectedHourVal == 12) 0 else selectedHourVal
        } else {
            if (selectedHourVal == 12) 12 else selectedHourVal + 12
        }
    }

    val currentCenteredMinuteIndex by remember {
        derivedStateOf { minuteListState.firstVisibleItemIndex }
    }
    LaunchedEffect(currentCenteredMinuteIndex) {
        selectedMinute = currentCenteredMinuteIndex % 60
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header Bar with Back Arrow and Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.surface)
                    .border(1.dp, AwanTheme.colors.line.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }

            AwanText(
                text = title,
                style = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        // Digital Time Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AwanTheme.colors.sky.copy(alpha = 0.12f))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                AwanText(
                    text = String.format("%02d : %02d", hour12, selectedMinute),
                    style = AwanTheme.typography.display.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AwanTheme.colors.sky,
                    ),
                )
                Spacer(modifier = Modifier.width(16.dp))
                // AM / PM Toggle Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AwanTheme.colors.surface)
                        .border(1.dp, AwanTheme.colors.sky.copy(alpha = 0.4f), CircleShape)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isAm) AwanTheme.colors.sky else Color.Transparent)
                            .clickable {
                                if (!isAm) {
                                    selectedHour24 -= 12
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AwanText(
                            text = "AM",
                            style = AwanTheme.typography.button.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAm) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isAm) AwanTheme.colors.sky else Color.Transparent)
                            .clickable {
                                if (isAm) {
                                    selectedHour24 += 12
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AwanText(
                            text = "PM",
                            style = AwanTheme.typography.button.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isAm) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                            ),
                        )
                    }
                }
            }
        }

        // Wheel Labels Header (HOURS : MINUTES)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AwanText(
                text = "HOURS",
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(32.dp))
            AwanText(
                text = "MINUTES",
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1f),
            )
        }

        // Smooth Scroll Wheels Box with Strict Centering & Snap Fling Behavior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AwanTheme.colors.surface)
                .border(1.dp, AwanTheme.colors.line.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Strict Selection Highlight Band (Centered at 180.dp box height, height 52.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AwanTheme.colors.sky.copy(alpha = 0.12f)),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hours Wheel
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    LazyColumn(
                        state = hourListState,
                        flingBehavior = hourSnapFlingBehavior,
                        contentPadding = PaddingValues(vertical = 64.dp),
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(12_000) { index ->
                            val h = (index % 12) + 1
                            val isSelected = (index == currentCenteredHourIndex)
                            val scale = if (isSelected) 1.15f else 0.75f
                            val alpha = if (isSelected) 1.0f else 0.45f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            hourListState.animateScrollToItem(index)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = String.format("%02d", h),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 24.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = (if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary).copy(alpha = alpha),
                                    ),
                                    modifier = Modifier.scale(scale),
                                )
                            }
                        }
                    }
                }

                // Separator Colon
                AwanText(
                    text = ":",
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.sky,
                    ),
                )

                // Minutes Wheel
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    LazyColumn(
                        state = minuteListState,
                        flingBehavior = minuteSnapFlingBehavior,
                        contentPadding = PaddingValues(vertical = 64.dp),
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(12_000) { index ->
                            val m = index % 60
                            val isSelected = (index == currentCenteredMinuteIndex)
                            val scale = if (isSelected) 1.15f else 0.75f
                            val alpha = if (isSelected) 1.0f else 0.45f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            minuteListState.animateScrollToItem(index)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = String.format("%02d", m),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 24.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = (if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary).copy(alpha = alpha),
                                    ),
                                    modifier = Modifier.scale(scale),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Minute Preset Chips (:00, :15, :30, :45)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0, 15, 30, 45).forEach { presetMin ->
                val isSelected = selectedMinute == presetMin
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface)
                        .border(
                            1.dp,
                            if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable {
                            val targetIndex = currentCenteredMinuteIndex - (currentCenteredMinuteIndex % 60) + presetMin
                            coroutineScope.launch {
                                minuteListState.animateScrollToItem(targetIndex)
                            }
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = ":${String.format("%02d", presetMin)}",
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Confirm Button (Returns to Main Dialog View with confirmed time)
        AwanButton(
            onClick = {
                val totalMinutes = selectedHour24 * 60 + selectedMinute
                onConfirm(totalMinutes)
            },
            variant = AwanButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AwanText(
                text = stringResource(R.string.home_action_save),
                style = AwanTheme.typography.button.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

private data class StatusChipStyle(
    val text: String,
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
