package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.ui.components.TimeInputBox
import com.awan.feature.profile.impl.ui.components.ZoneColorPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditZoneSheet(
    zone: DailyZone?,
    templateName: String = "Default",
    defaultStartTime: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    isSaving: Boolean = false
) {
    var name by remember { mutableStateOf(zone?.name ?: "") }
    
    val initialStartTime = remember(zone, defaultStartTime) {
        zone?.startTime ?: defaultStartTime ?: "09:00"
    }
    
    val initialEndTime = remember(zone, initialStartTime) {
        zone?.endTime ?: run {
            val startMins = DailyZonesHelper.parseTimeToMinutes(initialStartTime)
            DailyZonesHelper.formatMinutesToTime(startMins + 60)
        }
    }

    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var color by remember { mutableStateOf(zone?.color ?: "#2EAAFF") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(startTime),
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
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(endTime),
            confirmLabel = stringResource(R.string.profile_zone_confirm),
            cancelLabel = stringResource(R.string.profile_cancel),
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AwanText(
                text = if (zone == null) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_edit),
                style = AwanTheme.styles.titleText
            )

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
                    placeholder = stringResource(R.string.profile_zone_name_placeholder)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            ZoneColorPicker(
                selectedColor = color,
                onColorSelected = { color = it }
            )

            AddEditZonePreview(
                previewZone = previewZone,
                isEdit = zone != null
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    AwanText(text = if (zone == null) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_save_changes))
                }

                AwanButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AwanButtonVariant.Secondary,
                    enabled = !isSaving
                ) {
                    AwanText(text = stringResource(R.string.profile_cancel))
                }
            }
        }
    }
}
