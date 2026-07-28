package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import java.util.Locale

@Composable
fun ZoneTimelineItem(
    zone: DailyZone,
    isLast: Boolean,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            AwanText(
                text = DailyZonesHelper.formatTime12h(zone.startTime),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.styles.captionText.textStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(AwanTheme.colors.line)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(zone.color.toColor())
                        .align(Alignment.TopCenter)
                        .border(1.5.dp, AwanTheme.colors.background, CircleShape)
                )
            }

            if (isLast) {
                AwanText(
                    text = DailyZonesHelper.formatTime12h(zone.endTime),
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                )
            }
        }

        // Zone card body
        ZoneCardBody(
            zone = zone,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
fun ZoneCardBody(
    zone: DailyZone,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val zoneColor = zone.color.toColor()
    AwanCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        background = zoneColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(zoneColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name.ifBlank { "New Zone" },
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (zone.name.isBlank()) AwanTheme.colors.textSecondary else AwanTheme.colors.textPrimary
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AwanTheme.colors.textSecondary
                    )
                    AwanText(
                        text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                    )
                }
            }

            Row {
                if (onEdit != null) {
                    AwanIconButton(
                        onClick = onEdit,
                        contentDescription = "Edit"
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onDelete != null) {
                    AwanIconButton(
                        onClick = onDelete,
                        contentDescription = "Delete"
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = AwanTheme.colors.destructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditSheet(
    zone: DailyZone,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    isNew: Boolean = false
) {
    var name by remember(zone) { mutableStateOf(zone.name) }
    var startTime by remember(zone) { mutableStateOf(zone.startTime) }
    var endTime by remember(zone) { mutableStateOf(zone.endTime) }
    var color by remember(zone) { mutableStateOf(if (zone.color.startsWith("#")) zone.color else "#2EAAFF") }

    val startMins = DailyZonesHelper.parseTimeToMinutes(startTime)
    val endMins = DailyZonesHelper.parseTimeToMinutes(endTime)

    val startState = rememberTimePickerState(
        initialHour = (startMins / 60).coerceIn(0, 23),
        initialMinute = (startMins % 60).coerceIn(0, 59)
    )
    val endState = rememberTimePickerState(
        initialHour = (endMins / 60).coerceIn(0, 23),
        initialMinute = (endMins % 60).coerceIn(0, 59)
    )

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (isNew) "Add Zone" else "Edit Zone",
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(onClick = onDismiss, contentDescription = "Close") {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(text = "Zone Name", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Deep Work, Workout",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeInputBox(
                    label = "Start Time",
                    time = startTime,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
                TimeInputBox(
                    label = "End Time",
                    time = endTime,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(text = "Label Color", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("#2EAAFF", "#FF9F2E", "#AA2EFF", "#2EFFA3", "#FF2E63", "#FFEB3B", "#4CAF50").forEach { hex ->
                        val isSelected = color.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(hex.toColor())
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) AwanTheme.colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = hex }
                        )
                    }
                }
            }

            AwanButton(
                onClick = {
                    onConfirm(zone.copy(name = name, startTime = startTime, endTime = endTime, color = color))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                AwanText(text = if (isNew) "Add Zone" else "Save Changes")
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            state = startState,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format(Locale.US, "%02d:%02d", startState.hour, startState.minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            state = endState,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format(Locale.US, "%02d:%02d", endState.hour, endState.minute)
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimeInputBox(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AwanText(text = label, style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp)),
            color = AwanTheme.colors.surface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccessTime, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(20.dp))
                AwanText(text = DailyZonesHelper.formatTime12h(time), style = AwanTheme.styles.bodyText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                AwanText(text = "Confirm", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Cancel", style = AwanTheme.styles.bodyText)
            }
        },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AwanTimePicker(state = state)
            }
        },
        containerColor = AwanTheme.colors.background
    )
}

@Composable
fun ZoneDragHandle(color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

fun String?.toColor(): Color {
    if (this == null || this.isBlank()) return Color.Transparent
    return try {
        val colorString = if (!this.startsWith("#")) "#$this" else this
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Transparent
    }
}
