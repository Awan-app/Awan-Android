package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class TimelineZoneBlock(
    val id: String,
    val category: TaskCategory,
    val startHour: Int,
    val endHour: Int,
    val isCollapsed: Boolean = false,
)

data class TimelineTaskItem(
    val id: String,
    val title: String,
    val startMinutes: Int,
    val durationMinutes: Int,
    val category: TaskCategory,
    val status: TaskStatus,
    val points: Int? = null,
)

@Composable
fun AwanScheduleTimeline(
    zones: List<TimelineZoneBlock>,
    tasks: List<TimelineTaskItem>,
    currentTimeFormatted: String = "",
    currentTimeMinutes: Int = 0,
    startHour: Int = 0,
    endHour: Int = 24,
    hourHeightDp: Int = 64,
    intervalMinutes: Int = 15,
    onToggleZoneCollapse: (zoneId: String) -> Unit = {},
    onTaskStatusToggle: (taskId: String) -> Unit = {},
    onTaskMoved: (taskId: String, newStartMinutes: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val totalHours = (endHour - startHour).coerceAtLeast(1)
    val totalHeightDp = (totalHours * hourHeightDp).dp

    val scrollState = rememberScrollState()

    // Auto-scroll timeline to current system time position on initial launch
    LaunchedEffect(currentTimeMinutes) {
        if (currentTimeMinutes > 0) {
            val targetOffsetY = (((currentTimeMinutes - startHour * 60) * hourHeightDp) / 60) - (2 * hourHeightDp)
            scrollState.animateScrollTo(targetOffsetY.coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp),
        ) {
            // Left Column: Time Axis (52.dp) with Hour labels aligned to exact pixel Y offsets
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(totalHeightDp),
            ) {
                // 1. Hour Labels rendered at exact Y offset matching zone start times
                for (hour in startHour until endHour) {
                    val hourTopY = ((hour - startHour) * hourHeightDp).dp
                    val amPmLabel = when {
                        hour == 0 -> "12 AM"
                        hour == 12 -> "12 PM"
                        hour > 12 -> "${hour - 12} PM"
                        else -> "$hour AM"
                    }
                    AwanText(
                        text = amPmLabel,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                        ),
                        modifier = Modifier.offset(y = hourTopY),
                    )
                }

                // 2. Merged 3D Current Time Pointer Badge (Small, sleek badge directly on time axis)
                if (currentTimeFormatted.isNotBlank() && currentTimeMinutes in (startHour * 60)..(endHour * 60)) {
                    val totalMinutesInRange = (totalHours * 60).coerceAtLeast(1)
                    val elapsedMinutes = (currentTimeMinutes - startHour * 60).coerceIn(0, totalMinutesInRange)
                    val pointerProgress = elapsedMinutes.toFloat() / totalMinutesInRange
                    val pointerOffsetY = totalHeightDp * pointerProgress

                    val badgeShape = RoundedCornerShape(99.dp)
                    Box(
                        modifier = Modifier
                            .offset(y = pointerOffsetY - 10.dp)
                            .zIndex(30f)
                            .shadow(4.dp, badgeShape, spotColor = Color(0xFF2563EB))
                            .clip(badgeShape)
                            .background(Color(0xFF2563EB))
                            .border(1.5.dp, Color(0xFF93C5FD), badgeShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color.White, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            AwanText(
                                text = currentTimeFormatted,
                                style = AwanTheme.typography.heading.copy(
                                    fontSize = 10.sp,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Main Content: Zones placed at exact time offsets matching hour labels
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeightDp),
            ) {
                zones.forEach { zone ->
                    val zoneStartMinutes = zone.startHour * 60
                    val zoneEndMinutes = zone.endHour * 60
                    val zoneTopOffsetY = (((zoneStartMinutes - startHour * 60) * hourHeightDp) / 60f).dp
                    val zoneDurationHours = (zone.endHour - zone.startHour).coerceAtLeast(1)
                    val zoneHeightDp = (zoneDurationHours * hourHeightDp).dp

                    val zoneTasks = tasks.filter { task ->
                        task.category.id == zone.category.id ||
                            (task.startMinutes >= zoneStartMinutes && task.startMinutes < zoneEndMinutes)
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = zoneTopOffsetY)
                            .fillMaxWidth()
                            .height(zoneHeightDp),
                    ) {
                        ZoneContainer(
                            zone = zone,
                            tasks = zoneTasks,
                            startHourMinutes = zoneStartMinutes,
                            endHourMinutes = zoneEndMinutes,
                            hourHeightDp = hourHeightDp,
                            intervalMinutes = intervalMinutes,
                            onToggleCollapse = { onToggleZoneCollapse(zone.id) },
                            onTaskStatusToggle = onTaskStatusToggle,
                            onTaskMoved = onTaskMoved,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneContainer(
    zone: TimelineZoneBlock,
    tasks: List<TimelineTaskItem>,
    startHourMinutes: Int,
    endHourMinutes: Int,
    hourHeightDp: Int,
    intervalMinutes: Int,
    onToggleCollapse: () -> Unit,
    onTaskStatusToggle: (String) -> Unit,
    onTaskMoved: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = shape,
                spotColor = zone.category.color.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(zone.category.containerColor)
            .border(1.5.dp, zone.category.borderColor, shape)
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Zone Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleCollapse,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(zone.category.color, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AwanText(
                        text = zone.category.name,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 13.5.sp,
                            color = zone.category.color,
                        ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AwanText(
                        text = "(${formatHourTo12h(zone.startHour)} - ${formatHourTo12h(zone.endHour)})",
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                        ),
                    )
                }

                AwanText(
                    text = if (zone.isCollapsed) "▼" else "▲",
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.5.sp,
                        color = zone.category.color,
                    ),
                )
            }

            // Tasks List inside Zone
            AnimatedVisibility(
                visible = !zone.isCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AwanText(
                                text = "No tasks in this zone",
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8),
                                ),
                            )
                        }
                    } else {
                        tasks.forEach { task ->
                            StrictIntervalDraggableTaskWrapper(
                                task = task,
                                startHourMinutes = startHourMinutes,
                                endHourMinutes = endHourMinutes,
                                hourHeightDp = hourHeightDp,
                                intervalMinutes = intervalMinutes,
                                onTaskStatusToggle = { onTaskStatusToggle(task.id) },
                                onTaskMoved = { newStartMins -> onTaskMoved(task.id, newStartMins) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrictIntervalDraggableTaskWrapper(
    task: TimelineTaskItem,
    startHourMinutes: Int,
    endHourMinutes: Int,
    hourHeightDp: Int,
    intervalMinutes: Int,
    onTaskStatusToggle: () -> Unit,
    onTaskMoved: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .fillMaxWidth()
            .zIndex(if (dragOffsetY.value != 0f) 10f else 1f)
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragEnd = {
                        val rawDeltaMinutes = (dragOffsetY.value / hourHeightDp) * 60f
                        val snappedDeltaMinutes = ((rawDeltaMinutes / intervalMinutes).roundToInt() * intervalMinutes)
                        val newStartMinutes = (task.startMinutes + snappedDeltaMinutes)
                            .coerceIn(startHourMinutes, endHourMinutes - task.durationMinutes)

                        coroutineScope.launch {
                            dragOffsetY.animateTo(
                                0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                        }
                        if (newStartMinutes != task.startMinutes) {
                            onTaskMoved(newStartMinutes)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                        }
                    },
                )
            },
    ) {
        AwanScheduleTaskCard(
            title = task.title,
            timeRange = formatMinutesToRange(task.startMinutes, task.durationMinutes),
            category = task.category,
            status = task.status,
            points = task.points,
            onStatusToggle = onTaskStatusToggle,
        )
    }
}

private fun formatHourTo12h(hour: Int): String {
    val modHour = hour % 24
    val amPm = if (modHour >= 12 && modHour < 24) "PM" else "AM"
    val hour12 = when {
        modHour == 0 -> 12
        modHour > 12 -> modHour - 12
        else -> modHour
    }
    return "$hour12 $amPm"
}

private fun formatMinutesToRange(startMinutes: Int, durationMinutes: Int): String {
    val endMinutes = startMinutes + durationMinutes
    return "${formatTime(startMinutes)} - ${formatTime(endMinutes)}"
}

private fun formatTime(minutes: Int): String {
    val totalMins = minutes.mod(24 * 60)
    val hour24 = totalMins / 60
    val mins = totalMins % 60
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(java.util.Locale.US, "%d:%02d %s", hour12, mins, amPm)
}
