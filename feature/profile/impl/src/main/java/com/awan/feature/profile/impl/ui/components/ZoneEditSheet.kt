package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditSheet(
    zone: DailyZone,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    onDelete: (() -> Unit)? = null,
    isNew: Boolean = false
) {
    var name by remember(zone) { mutableStateOf(zone.name) }
    var startTime by remember(zone) { mutableStateOf(zone.startTime) }
    var endTime by remember(zone) { mutableStateOf(zone.endTime) }
    var color by remember(zone) { mutableStateOf(if (zone.color.startsWith("#")) zone.color else "#2EAAFF") }

    val startMins = DailyZonesHelper.parseTimeToMinutes(startTime)
    val endMins = DailyZonesHelper.parseTimeToMinutes(endTime)

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (isNew) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_edit),
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(onClick = onDismiss, contentDescription = stringResource(R.string.profile_close)) {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = stringResource(R.string.profile_zone_name),
                    style = AwanTheme.styles.bodyText.copy(
                        color = AwanTheme.colors.textSecondary,
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontSize = 13.sp)
                    )
                )
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.profile_zone_name_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeInputBox(
                    label = stringResource(R.string.profile_zone_start_time),
                    time = startTime,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
                TimeInputBox(
                    label = stringResource(R.string.profile_zone_end_time),
                    time = endTime,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Color Selection
            ZoneColorPicker(
                selectedColor = color,
                onColorSelected = { color = it }
            )

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanButton(
                    onClick = {
                        onConfirm(zone.copy(name = name, startTime = startTime, endTime = endTime, color = color))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    AwanText(text = if (isNew) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_save_changes))
                }

                if (!isNew && onDelete != null) {
                    AwanButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        variant = AwanButtonVariant.Secondary,
                        icon = Icons.Default.Delete
                    ) {
                        AwanText(text = stringResource(R.string.profile_zone_delete))
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = startMins,
            confirmLabel = stringResource(R.string.profile_zone_confirm),
            cancelLabel = stringResource(R.string.profile_cancel),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { mins ->
                startTime = DailyZonesHelper.formatMinutesToTime(mins)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = endMins,
            confirmLabel = stringResource(R.string.profile_zone_confirm),
            cancelLabel = stringResource(R.string.profile_cancel),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { mins ->
                endTime = DailyZonesHelper.formatMinutesToTime(mins)
                showEndTimePicker = false
            }
        )
    }
}
