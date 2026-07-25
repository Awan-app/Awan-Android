package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.presentation.EditRoutineAction
import com.awan.feature.profile.impl.presentation.EditRoutineState
import com.awan.feature.profile.impl.ui.components.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    uiState: EditRoutineState,
    onAction: (EditRoutineAction) -> Unit,
    onBackClick: () -> Unit
) {
    var showZoneDialog by remember { mutableStateOf<DailyZone?>(null) }
    var isNewZone by remember { mutableStateOf(false) }

    if (showZoneDialog != null) {
        ZoneEditDialog(
            zone = showZoneDialog!!,
            isNew = isNewZone,
            onDismiss = { showZoneDialog = null },
            onConfirm = { zone ->
                if (isNewZone) {
                    onAction(EditRoutineAction.AddZone(zone))
                } else {
                    onAction(EditRoutineAction.UpdateZone(showZoneDialog!!, zone))
                }
                showZoneDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { AwanText(text = if (uiState.templateId == null) "Create Routine" else "Edit Routine", style = AwanTheme.styles.titleText) },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AwanTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(modifier = Modifier.padding(20.dp)) {
                AwanButton(
                    onClick = { onAction(EditRoutineAction.SaveRoutine) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isSaving,
                    icon = Icons.Default.Check
                ) {
                    AwanText(text = "Save routine")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (uiState.validationError != null) {
                AwanCard(background = AwanTheme.colors.destructive.copy(alpha = 0.1f)) {
                    AwanText(
                        text = uiState.validationError,
                        style = AwanTheme.styles.errorText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Routine Name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(
                    text = "Routine name",
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                AwanTextField(
                    value = uiState.name,
                    onValueChange = { onAction(EditRoutineAction.NameChange(it)) },
                    placeholder = "e.g. Workday",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Days Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(
                    text = "Apply to days",
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val isSelected = uiState.selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface)
                                .border(1.dp, if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line, CircleShape)
                                .clickable { onAction(EditRoutineAction.ToggleDay(day)) },
                            contentAlignment = Alignment.Center
                        ) {
                            AwanText(
                                text = day.name.take(1),
                                style = AwanTheme.styles.bodyText.copy(
                                    color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Zones
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AwanText(
                        text = "Zones",
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    )
                    TextButton(onClick = { 
                        isNewZone = true
                        showZoneDialog = DailyZone(id = null, name = "", startTime = "09:00", endTime = "17:00", color = "#2EAAFF")
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AwanText(text = "Add zone", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
                    }
                }

                if (uiState.zones.isEmpty()) {
                    AwanText(text = "No zones added yet", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
                } else {
                    uiState.zones.forEach { zone ->
                        ZoneEditItem(
                            zone = zone,
                            onEdit = {
                                isNewZone = false
                                showZoneDialog = zone
                            },
                            onDelete = { onAction(EditRoutineAction.DeleteZone(zone)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneEditItem(
    zone: DailyZone,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val zoneColor = zone.color.toColor()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(zoneColor.copy(alpha = 0.1f))
            .border(1.dp, zoneColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(zoneColor))
        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                text = zone.name,
                style = AwanTheme.styles.bodyText.copy(
                    textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                )
            )
            AwanText(text = "${zone.startTime} - ${zone.endTime}", style = AwanTheme.styles.captionText)
        }
        AwanIconButton(onClick = onEdit, contentDescription = "Edit") {
            Icon(Icons.Default.Edit, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(16.dp))
        }
        AwanIconButton(onClick = onDelete, contentDescription = "Delete") {
            Icon(Icons.Default.Delete, null, tint = AwanTheme.colors.destructive, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ZoneEditDialog(
    zone: DailyZone,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit
) {
    var name by remember { mutableStateOf(zone.name) }
    var startTime by remember { mutableStateOf(zone.startTime) }
    var endTime by remember { mutableStateOf(zone.endTime) }
    var color by remember { mutableStateOf(zone.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AwanText(text = if (isNew) "Add Zone" else "Edit Zone", style = AwanTheme.styles.titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AwanTextField(value = name, onValueChange = { name = it }, placeholder = "Zone Name")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AwanTextField(value = startTime, onValueChange = { startTime = it }, placeholder = "Start (HH:mm)", modifier = Modifier.weight(1f))
                    AwanTextField(value = endTime, onValueChange = { endTime = it }, placeholder = "End (HH:mm)", modifier = Modifier.weight(1f))
                }
                // Color Picker Placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("#2EAAFF", "#FF9F2E", "#AA2EFF", "#2EFFA3", "#FF2E63").forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(hex.toColor())
                                .border(if (color == hex) 2.dp else 0.dp, AwanTheme.colors.textPrimary, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                onConfirm(zone.copy(name = name, startTime = startTime, endTime = endTime, color = color))
            }) {
                AwanText(text = "Confirm", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Cancel", style = AwanTheme.styles.bodyText)
            }
        },
        containerColor = AwanTheme.colors.surface
    )
}
