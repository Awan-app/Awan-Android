package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.onboarding.impl.ui.components.TimePickerDialog as AwanTimePickerDialog
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.ui.components.ZoneCardBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditZoneSheet(
    zone: DailyZone?,
    templateName: String = "Default",
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    isSaving: Boolean = false
) {
    var name by remember { mutableStateOf(zone?.name ?: "") }
    var startTime by remember { mutableStateOf(zone?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(zone?.endTime ?: "10:00") }
    var color by remember { mutableStateOf(zone?.color ?: "#2EAAFF") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(startTime),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { mins ->
                startTime = DailyZonesHelper.formatMinutesToTime(mins)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(endTime),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { mins ->
                endTime = DailyZonesHelper.formatMinutesToTime(mins)
                showEndTimePicker = false
            }
        )
    }

    val previewZone = DailyZone(
        id = zone?.id,
        name = name,
        startTime = startTime,
        endTime = endTime,
        color = color
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AwanText(
                text = if (zone == null) "Add zone" else "Edit zone",
                style = AwanTheme.styles.titleText
            )

            // Zone Name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(text = "Zone name", style = AwanTheme.styles.bodyText)
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Focus"
                )
            }

            // Start / End Time
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AwanText(text = "Start time", style = AwanTheme.styles.bodyText)
                    TimeField(
                        time = DailyZonesHelper.formatTime12h(startTime),
                        onClick = { showStartTimePicker = true }
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AwanText(text = "End time", style = AwanTheme.styles.bodyText)
                    TimeField(
                        time = DailyZonesHelper.formatTime12h(endTime),
                        onClick = { showEndTimePicker = true }
                    )
                }
            }

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(text = "Color", style = AwanTheme.styles.bodyText)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        "#FF6F91", "#7A64FF", "#2EAAFF",
                        "#FF9838", "#FFC233", "#9A7BFF"
                    ).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (color == hex) 2.dp else 0.dp,
                                    color = if (color == hex) AwanTheme.colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = hex }
                        )
                    }
                }
            }

            // ── Live Preview ───────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(
                    text = stringResource(R.string.profile_daily_zones_zone_preview),
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                ZoneCardBody(
                    zone = previewZone,
                    modifier = Modifier.fillMaxWidth()
                )
                // Routine hint text
                AwanText(
                    text = if (zone == null) {
                        stringResource(R.string.profile_daily_zones_zone_will_be_added)
                    } else {
                        stringResource(R.string.profile_daily_zones_zone_in_default)
                    },
                    style = AwanTheme.styles.captionText.copy(
                        color = AwanTheme.colors.textSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Confirm Button
            AwanButton(
                onClick = {
                    onConfirm(
                        DailyZone(
                            id = zone?.id,
                            name = name,
                            startTime = startTime,
                            endTime = endTime,
                            color = color
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving,
                isLoading = isSaving
            ) {
                AwanText(text = if (zone == null) "Add zone" else "Save changes")
            }

            // Cancel
            AwanButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = AwanButtonVariant.Secondary,
                enabled = !isSaving
            ) {
                AwanText(text = "Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDayPickerSheet(
    overriddenDays: Set<DayOfWeek> = emptySet(),
    onDaySelected: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AwanTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AwanText(
                text = "Customize a specific day",
                style = AwanTheme.styles.titleText
            )
            AwanText(
                text = "Make changes for one day without changing your routine.",
                style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DayOfWeek.entries.forEach { day ->
                val hasOverride = day in overriddenDays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(day) }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AwanText(
                        text = DailyZonesHelper.displayName(day),
                        style = AwanTheme.styles.bodyText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasOverride) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AwanTheme.colors.sky.copy(alpha = 0.1f)
                            ) {
                                AwanText(
                                    text = stringResource(R.string.profile_daily_zones_custom_schedule),
                                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AwanTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (day != DayOfWeek.SUNDAY) {
                    HorizontalDivider(
                        color = AwanTheme.colors.line,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun TimeField(time: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.background)
            .border(1.5.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AwanText(text = time, style = AwanTheme.styles.bodyText)
    }
}
