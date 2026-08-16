package com.awan.feature.profile.impl.ui.dailyzones

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
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.ui.components.CategoryPickerRow
import com.awan.feature.profile.impl.ui.components.TimeInputBox
import com.awan.feature.profile.impl.ui.components.ZoneColorPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditZoneSheet(
    zone: DailyZone?,
    availableCategories: List<Category>,
    defaultStartTime: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    onAddCategory: (String) -> Unit,
    onDelete: (DailyZone) -> Unit = {},
    canDelete: Boolean = true,
    isSaving: Boolean = false
) {
    var name by remember { mutableStateOf(zone?.name ?: "") }
    
    val initialStartTime = remember(zone, defaultStartTime) {
        zone?.startTime ?: defaultStartTime ?: "09:00:00"
    }
    
    val initialEndTime = remember(zone, initialStartTime) {
        zone?.endTime ?: run {
            val startMins = DailyZonesHelper.parseTimeToMinutes(initialStartTime) ?: 540
            DailyZonesHelper.formatMinutesToTime(startMins + 60)
        }
    }

    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var color by remember { mutableStateOf(zone?.color ?: "#2EAAFF") }
    var selectedCategoryId by remember { mutableStateOf(zone?.categoryId ?: availableCategories.firstOrNull()?.id) }
    var pendingCategoryName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(availableCategories) {
        pendingCategoryName?.let { name ->
            val newCat = availableCategories.find { it.name.equals(name, ignoreCase = true) }
            if (newCat != null) {
                selectedCategoryId = newCat.id
                pendingCategoryName = null
            }
        }
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        AwanTimePickerDialog(
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(startTime) ?: 0,
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
            initialMinutes = DailyZonesHelper.parseTimeToMinutes(endTime) ?: 0,
            confirmLabel = stringResource(R.string.profile_zone_confirm),
            cancelLabel = stringResource(R.string.profile_cancel),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { mins ->
                endTime = DailyZonesHelper.formatMinutesToTime(mins)
                showEndTimePicker = false
            }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (zone == null) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_edit),
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(onClick = onDismiss, contentDescription = stringResource(R.string.profile_close)) {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

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

            CategoryPickerRow(
                label = stringResource(R.string.profile_zone_category),
                selectedCategoryId = selectedCategoryId,
                categories = availableCategories,
                onCategorySelected = { selectedCategoryId = it },
                onAddCategory = { name ->
                    pendingCategoryName = name
                    onAddCategory(name)
                }
            )

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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanButton(
                    onClick = {
                        onConfirm(
                            DailyZone(
                                id = zone?.id,
                                name = name,
                                startTime = startTime,
                                endTime = endTime,
                                color = color,
                                categoryId = selectedCategoryId
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && selectedCategoryId != null && !isSaving,
                    isLoading = isSaving
                ) {
                    AwanText(text = if (zone == null) stringResource(R.string.profile_zone_add) else stringResource(R.string.profile_zone_save_changes))
                }

                if (zone != null) {
                    AwanButton(
                        onClick = { onDelete(zone) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = AwanButtonVariant.Destructive,
                        icon = Icons.Default.Delete,
                        enabled = !isSaving && canDelete
                    ) {
                        AwanText(text = stringResource(R.string.profile_zone_delete))
                    }
                }
            }
        }
    }
}
