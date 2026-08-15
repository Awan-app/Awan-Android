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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.pluralStringResource
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private fun LazyListState.centeredItemIndex(): Int {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) return firstVisibleItemIndex
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return items.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        kotlin.math.abs(itemCenter - viewportCenter)
    }?.index ?: firstVisibleItemIndex
}

private enum class DialogPickerTarget { START_TIME, END_TIME, DATE }

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
    modifier: Modifier = Modifier,
    editDate: LocalDate = LocalDate.now(),
    onDateChange: (LocalDate) -> Unit = {},
    onDeleteClick: (() -> Unit)? = null,
    onConfirmClose: () -> Unit = {},
    onNavigateToTaskDetails: ((String) -> Unit)? = null,
) {
    var activePickerTarget by remember { mutableStateOf<DialogPickerTarget?>(null) }

    AnimatedContent(
        targetState = activePickerTarget,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "DialogViewTransition",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        when (target) {
            null -> {
                MainSessionTaskDetailView(
                    detail = detail,
                    editDate = editDate,
                    editStartMinutes = editStartMinutes,
                    editEndMinutes = editEndMinutes,
                    editDurationMinutes = editDurationMinutes,
                    onToggleStatus = onToggleStatus,
                    onToggleLock = onToggleLock,
                    onDateChange = onDateChange,
                    onStartMinutesChange = onStartMinutesChange,
                    onEndMinutesChange = onEndMinutesChange,
                    onDurationChange = onDurationChange,
                    onOpenPicker = { activePickerTarget = it },
                    onDeleteClick = onDeleteClick,
                    onConfirmClose = onConfirmClose,
                    onNavigateToTaskDetails = onNavigateToTaskDetails,
                )
            }
            DialogPickerTarget.DATE -> {
                CustomWheelDatePickerView(
                    title = stringResource(R.string.home_date_picker_title),
                    initialDate = editDate,
                    onConfirm = { selectedDate ->
                        onDateChange(selectedDate)
                        activePickerTarget = null
                    },
                    onBack = { activePickerTarget = null },
                )
            }
            DialogPickerTarget.START_TIME, DialogPickerTarget.END_TIME -> {
                val titleText = if (target == DialogPickerTarget.START_TIME) {
                    stringResource(R.string.home_edit_label_start_time)
                } else {
                    stringResource(R.string.home_edit_label_end_time)
                }
                val initialMins = if (target == DialogPickerTarget.START_TIME) editStartMinutes else editEndMinutes

                CustomWheelTimePickerView(
                    title = titleText,
                    initialMinutes = initialMins,
                    onConfirm = { selectedMins ->
                        if (target == DialogPickerTarget.START_TIME) {
                            onStartMinutesChange(selectedMins)
                        } else {
                            onEndMinutesChange(selectedMins)
                        }
                        activePickerTarget = null
                    },
                    onBack = { activePickerTarget = null },
                )
            }
        }
    }
}

@Composable
private fun MainSessionTaskDetailView(
    detail: SessionTaskDetail,
    editDate: LocalDate,
    editStartMinutes: Int,
    editEndMinutes: Int,
    editDurationMinutes: Int,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStartMinutesChange: (Int) -> Unit,
    onEndMinutesChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onOpenPicker: (DialogPickerTarget) -> Unit,
    onDeleteClick: (() -> Unit)?,
    onConfirmClose: () -> Unit = {},
    onNavigateToTaskDetails: ((String) -> Unit)? = null,
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
            // Task Title (Clickable link to Task Details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onNavigateToTaskDetails != null) {
                            Modifier.clickable { onNavigateToTaskDetails(detail.task.id) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AwanText(
                    text = detail.task.title,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                        lineHeight = 26.sp,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (onNavigateToTaskDetails != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.home_task_details_title),
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

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

                // Interactive Live Time & Date Adjuster Card
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
                        // Date Stepper Card (Session Day / Date Selection)
                        DateStepperCard(
                            dateValue = editDate,
                            onDateChange = onDateChange,
                            onDateDisplayClick = { onOpenPicker(DialogPickerTarget.DATE) },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TimeStepperCard(
                                label = stringResource(R.string.home_edit_label_start_time),
                                minutesValue = editStartMinutes,
                                onDecrease = { onStartMinutesChange(editStartMinutes - 15) },
                                onIncrease = { onStartMinutesChange(editStartMinutes + 15) },
                                onTimeDisplayClick = { onOpenPicker(DialogPickerTarget.START_TIME) },
                                modifier = Modifier.weight(1f),
                            )

                            TimeStepperCard(
                                label = stringResource(R.string.home_edit_label_end_time),
                                minutesValue = editEndMinutes,
                                onDecrease = { onEndMinutesChange(editEndMinutes - 15) },
                                onIncrease = { onEndMinutesChange(editEndMinutes + 15) },
                                onTimeDisplayClick = { onOpenPicker(DialogPickerTarget.END_TIME) },
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
                                            text = stringResource(R.string.home_edit_duration_mins, duration),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Save Changes Button
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
    var selectedHour24 by remember { mutableIntStateOf(initialMinutes / 60) }
    var selectedMinute by remember { mutableIntStateOf(initialMinutes % 60) }

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
        derivedStateOf { hourListState.centeredItemIndex() }
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
        derivedStateOf { minuteListState.centeredItemIndex() }
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
                    contentDescription = stringResource(R.string.home_picker_back),
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
                    text = String.format(Locale.US, "%02d : %02d", hour12, selectedMinute),
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
                            text = stringResource(R.string.home_picker_am),
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
                            text = stringResource(R.string.home_picker_pm),
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
                text = stringResource(R.string.home_picker_hours),
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
                text = stringResource(R.string.home_picker_minutes),
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
                                    text = String.format(Locale.US, "%02d", h),
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
                                    text = String.format(Locale.US, "%02d", m),
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
                        text = ":${String.format(Locale.US, "%02d", presetMin)}",
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

@Composable
private fun formatSessionDateDisplay(date: LocalDate): String {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
    val formatted = date.format(pattern)
    return when (date) {
        today -> stringResource(R.string.home_date_today_format, formatted)
        today.minusDays(1) -> stringResource(R.string.home_date_yesterday_format, formatted)
        today.plusDays(1) -> stringResource(R.string.home_date_tomorrow_format, formatted)
        else -> formatted
    }
}

@Composable
private fun DateStepperCard(
    dateValue: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onDateDisplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateDisplayStr = formatSessionDateDisplay(dateValue)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AwanTheme.colors.line.copy(alpha = 0.12f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AwanText(
            text = stringResource(R.string.home_edit_label_date),
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .clickable { onDateChange(dateValue.minusDays(1)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.home_picker_previous_day),
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AwanTheme.colors.surface)
                    .border(1.dp, AwanTheme.colors.sky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onDateDisplayClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AwanText(
                        text = dateDisplayStr,
                        style = AwanTheme.typography.body.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AwanTheme.colors.sky,
                        ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .clickable { onDateChange(dateValue.plusDays(1)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.home_picker_next_day),
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val today = LocalDate.now()
        val quickDayOptions = listOf(
            Pair(stringResource(R.string.home_date_today_chip), today),
            Pair(stringResource(R.string.home_date_tomorrow_chip), today.plusDays(1)),
            Pair(pluralStringResource(R.plurals.home_date_plus_days, 2, 2), today.plusDays(2)),
            Pair(pluralStringResource(R.plurals.home_date_plus_days, 3, 3), today.plusDays(3)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            quickDayOptions.forEach { (label, optionDate) ->
                val isSelected = dateValue == optionDate
                val backgroundColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface
                val textColor = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onDateChange(optionDate) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = label,
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

@Composable
private fun CustomWheelDatePickerView(
    title: String,
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var selectedDate by remember { mutableStateOf(initialDate) }

    val minYear = 2025
    val maxYear = 2035

    var day by remember { mutableIntStateOf(selectedDate.dayOfMonth) }
    var month by remember { mutableIntStateOf(selectedDate.monthValue) }
    var year by remember { mutableIntStateOf(selectedDate.year) }

    fun updateSelectedDate(newYear: Int, newMonth: Int, newDay: Int) {
        val maxDaysInMonth = java.time.YearMonth.of(newYear, newMonth).lengthOfMonth()
        val clampedDay = newDay.coerceIn(1, maxDaysInMonth)
        year = newYear
        month = newMonth
        day = clampedDay
        selectedDate = LocalDate.of(newYear, newMonth, clampedDay)
    }

    val cycleBase = 6_000
    val initialDayIndex = cycleBase - (cycleBase % 31) + (day - 1)
    val initialMonthIndex = cycleBase - (cycleBase % 12) + (month - 1)
    val yearRange = minYear..maxYear
    val yearCount = yearRange.count()
    val initialYearIndex = cycleBase - (cycleBase % yearCount) + (year - minYear)

    val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = initialDayIndex)
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMonthIndex)
    val yearListState = rememberLazyListState(initialFirstVisibleItemIndex = initialYearIndex)

    val daySnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = dayListState)
    val monthSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = monthListState)
    val yearSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = yearListState)

    val coroutineScope = rememberCoroutineScope()

    val currentCenteredDayIndex by remember { derivedStateOf { dayListState.centeredItemIndex() } }
    LaunchedEffect(currentCenteredDayIndex) {
        val maxDays = java.time.YearMonth.of(year, month).lengthOfMonth()
        val selectedDayVal = (currentCenteredDayIndex % 31) + 1
        val validDay = selectedDayVal.coerceIn(1, maxDays)
        updateSelectedDate(year, month, validDay)
        if (validDay != selectedDayVal) {
            dayListState.scrollToItem(currentCenteredDayIndex - (selectedDayVal - validDay))
        }
    }

    val currentCenteredMonthIndex by remember { derivedStateOf { monthListState.centeredItemIndex() } }
    LaunchedEffect(currentCenteredMonthIndex) {
        val selectedMonthVal = (currentCenteredMonthIndex % 12) + 1
        val maxDays = java.time.YearMonth.of(year, selectedMonthVal).lengthOfMonth()
        val currentWheelDay = (dayListState.centeredItemIndex() % 31) + 1
        val validDay = currentWheelDay.coerceIn(1, maxDays)
        if (validDay != currentWheelDay) {
            val delta = validDay - currentWheelDay
            coroutineScope.launch {
                dayListState.scrollToItem(dayListState.centeredItemIndex() + delta)
            }
        }
        updateSelectedDate(year, selectedMonthVal, validDay)
    }

    val currentCenteredYearIndex by remember { derivedStateOf { yearListState.centeredItemIndex() } }
    LaunchedEffect(currentCenteredYearIndex) {
        val selectedYearVal = minYear + (currentCenteredYearIndex % yearCount)
        val maxDays = java.time.YearMonth.of(selectedYearVal, month).lengthOfMonth()
        val currentWheelDay = (dayListState.centeredItemIndex() % 31) + 1
        val validDay = currentWheelDay.coerceIn(1, maxDays)
        if (validDay != currentWheelDay) {
            val delta = validDay - currentWheelDay
            coroutineScope.launch {
                dayListState.scrollToItem(dayListState.centeredItemIndex() + delta)
            }
        }
        updateSelectedDate(selectedYearVal, month, validDay)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                    contentDescription = stringResource(R.string.home_picker_back),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AwanTheme.colors.sky.copy(alpha = 0.12f))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            val fullFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale)
            AwanText(
                text = selectedDate.format(fullFormatter),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.sky,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        val today = LocalDate.now()
        val quickOptions = listOf(
            Pair(stringResource(R.string.home_date_today_chip), today),
            Pair(stringResource(R.string.home_date_tomorrow_chip), today.plusDays(1)),
            Pair(pluralStringResource(R.plurals.home_date_plus_days, 2, 2), today.plusDays(2)),
            Pair(pluralStringResource(R.plurals.home_date_plus_days, 7, 7), today.plusDays(7)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickOptions.forEach { (label, optionDate) ->
                val isSelected = selectedDate == optionDate
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
                            selectedDate = optionDate
                            year = optionDate.year
                            month = optionDate.monthValue
                            day = optionDate.dayOfMonth
                            coroutineScope.launch {
                                val targetDayIdx = cycleBase - (cycleBase % 31) + (day - 1)
                                val targetMonthIdx = cycleBase - (cycleBase % 12) + (month - 1)
                                val targetYearIdx = cycleBase - (cycleBase % yearCount) + (year - minYear)
                                dayListState.scrollToItem(targetDayIdx)
                                monthListState.scrollToItem(targetMonthIdx)
                                yearListState.scrollToItem(targetYearIdx)
                            }
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = label,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary,
                        ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AwanText(
                text = stringResource(R.string.home_picker_day),
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1f),
            )
            AwanText(
                text = stringResource(R.string.home_picker_month),
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1.2f),
            )
            AwanText(
                text = stringResource(R.string.home_picker_year),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AwanTheme.colors.surface)
                .border(1.dp, AwanTheme.colors.line.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
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
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        state = dayListState,
                        flingBehavior = daySnapFlingBehavior,
                        contentPadding = PaddingValues(vertical = 64.dp),
                        modifier = Modifier.height(180.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(12_000) { index ->
                            val dVal = (index % 31) + 1
                            val maxDaysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth()
                            val isInvalidDay = dVal > maxDaysInMonth
                            val isSelected = (index == currentCenteredDayIndex) && !isInvalidDay
                            val scale = if (isSelected) 1.15f else 0.75f
                            val alpha = when {
                                isInvalidDay -> 0.12f
                                isSelected -> 1.0f
                                else -> 0.45f
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            val targetIdx = if (isInvalidDay) index - (dVal - maxDaysInMonth) else index
                                            dayListState.animateScrollToItem(targetIdx)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = String.format(Locale.US, "%02d", dVal),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 22.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = (if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary).copy(alpha = alpha),
                                    ),
                                    modifier = Modifier.scale(scale),
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        state = monthListState,
                        flingBehavior = monthSnapFlingBehavior,
                        contentPadding = PaddingValues(vertical = 64.dp),
                        modifier = Modifier.height(180.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(12_000) { index ->
                            val mVal = (index % 12) + 1
                            val monthName = java.time.Month.of(mVal).getDisplayName(TextStyle.SHORT, locale)
                            val isSelected = (index == currentCenteredMonthIndex)
                            val scale = if (isSelected) 1.15f else 0.75f
                            val alpha = if (isSelected) 1.0f else 0.45f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            monthListState.animateScrollToItem(index)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = monthName,
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 20.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = (if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary).copy(alpha = alpha),
                                    ),
                                    modifier = Modifier.scale(scale),
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        state = yearListState,
                        flingBehavior = yearSnapFlingBehavior,
                        contentPadding = PaddingValues(vertical = 64.dp),
                        modifier = Modifier.height(180.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(12_000) { index ->
                            val yVal = minYear + (index % yearCount)
                            val isSelected = (index == currentCenteredYearIndex)
                            val scale = if (isSelected) 1.15f else 0.75f
                            val alpha = if (isSelected) 1.0f else 0.45f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            yearListState.animateScrollToItem(index)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = yVal.toString(),
                                    style = AwanTheme.typography.heading.copy(
                                        fontSize = 20.sp,
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

        Spacer(modifier = Modifier.height(6.dp))

        AwanButton(
            onClick = { onConfirm(selectedDate) },
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
