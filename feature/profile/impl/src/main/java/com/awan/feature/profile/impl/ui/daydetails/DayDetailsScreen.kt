package com.awan.feature.profile.impl.ui.daydetails

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.DayDetailsAction
import com.awan.feature.profile.impl.presentation.DayDetailsState
import com.awan.feature.profile.impl.ui.components.toColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    uiState: DayDetailsState,
    onAction: (DayDetailsAction) -> Unit,
    onBackClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editableZones by remember { mutableStateOf(uiState.effectiveZones) }
    var showZoneDialog by remember { mutableStateOf<DailyZone?>(null) }
    var isNewZone by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.effectiveZones) {
        if (!isEditing) {
            editableZones = uiState.effectiveZones
        }
    }

    if (showResetDialog && uiState.overrideId != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { AwanText(text = "Reset this day?", style = AwanTheme.styles.titleText) },
            text = { AwanText(text = "Your custom schedule will be removed and this day will use its usual weekly routine again.", style = AwanTheme.styles.bodyText) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onAction(DayDetailsAction.ResetToWeeklyRoutine(uiState.overrideId))
                }) {
                    AwanText(text = "Reset", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.destructive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    AwanText(text = "Cancel", style = AwanTheme.styles.bodyText)
                }
            },
            containerColor = AwanTheme.colors.surface
        )
    }

    if (showZoneDialog != null) {
        ZoneEditDialog(
            zone = showZoneDialog!!,
            isNew = isNewZone,
            onDismiss = { showZoneDialog = null },
            onConfirm = { zone ->
                editableZones = if (isNewZone) {
                    editableZones + zone
                } else {
                    editableZones.map { if (it == showZoneDialog) zone else it }
                }
                showZoneDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val date = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(uiState.date) } catch (e: Exception) { null }
                    val dateText = remember(date) {
                        date?.let { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(it) } ?: ""
                    }
                    AwanText(text = dateText, style = AwanTheme.styles.titleText)
                },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                    }
                },
                actions = {
                    if (uiState.isOverride && !isEditing) {
                        AwanIconButton(onClick = { showResetDialog = true }, contentDescription = "Reset") {
                            Icon(Icons.Default.Refresh, null, tint = AwanTheme.colors.sky)
                        }
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
                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AwanButton(
                            onClick = { isEditing = false; editableZones = uiState.effectiveZones },
                            modifier = Modifier.weight(1f),
                            variant = AwanButtonVariant.Secondary
                        ) {
                            AwanText(text = "Cancel")
                        }
                        AwanButton(
                            onClick = {
                                if (uiState.isOverride && uiState.overrideId != null) {
                                    onAction(DayDetailsAction.UpdateOverride(uiState.overrideId, editableZones))
                                } else {
                                    onAction(DayDetailsAction.CustomizeDay(editableZones))
                                }
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            isLoading = uiState.isSaving,
                            icon = Icons.Default.Check
                        ) {
                            AwanText(text = "Save")
                        }
                    }
                } else {
                    AwanButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = if (uiState.isOverride) Icons.Default.Edit else Icons.Default.Add
                    ) {
                        AwanText(text = if (uiState.isOverride) "Edit custom schedule" else "Customize this day")
                    }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.isOverride) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AwanTheme.colors.sky.copy(alpha = 0.1f)
                ) {
                    AwanText(
                        text = "Custom schedule",
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                AwanText(
                    text = "Using weekly routine",
                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.meta)
                )
            }

            if (isEditing) {
                AwanText(
                    text = "Make changes for this day only.",
                    style = AwanTheme.styles.metaText
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        isNewZone = true
                        showZoneDialog = DailyZone(id = null, name = "", startTime = "09:00", endTime = "10:00", color = "#2EAAFF")
                    }) {
                        Icon(Icons.Default.Add, null)
                        AwanText(text = "Add zone", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
                    }
                }
            }

            TimelineDetailed(
                zones = editableZones,
                isEditing = isEditing,
                onEditZone = { zone ->
                    isNewZone = false
                    showZoneDialog = zone
                },
                onDeleteZone = { zone ->
                    editableZones = editableZones - zone
                }
            )
        }
    }
}

@Composable
private fun TimelineDetailed(
    zones: List<DailyZone>,
    isEditing: Boolean,
    onEditZone: (DailyZone) -> Unit,
    onDeleteZone: (DailyZone) -> Unit
) {
    val sortedZones = remember(zones) { zones.sortedBy { it.startTime } }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        if (sortedZones.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                AwanText(text = "No zones scheduled", style = AwanTheme.styles.bodySecondaryText)
            }
        } else {
            sortedZones.forEachIndexed { index, zone ->
                TimelineZoneItem(
                    zone = zone,
                    isEditing = isEditing,
                    showBottomLine = index < sortedZones.size - 1,
                    onEditClick = { onEditZone(zone) },
                    onDeleteClick = { onDeleteZone(zone) }
                )
            }
        }
    }
}

@Composable
private fun TimelineZoneItem(
    zone: DailyZone,
    isEditing: Boolean,
    showBottomLine: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Timeline Column
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            AwanText(
                text = DailyZonesHelper.formatTime12h(zone.startTime),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.typography.caption.copy(fontSize = 12.sp)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky)
            )
            if (showBottomLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(AwanTheme.colors.line)
                )
            }
        }

        // Zone Card
        val zoneColor = zone.color.toColor()

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(zoneColor.copy(alpha = 0.1f))
                .border(1.dp, zoneColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name,
                    style = AwanTheme.styles.bodyText.copy(textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold))
                )
                AwanText(
                    text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                )
            }
            if (isEditing) {
                AwanIconButton(onClick = onEditClick, contentDescription = "Edit") {
                    Icon(Icons.Default.Edit, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(16.dp))
                }
                AwanIconButton(onClick = onDeleteClick, contentDescription = "Delete") {
                    Icon(Icons.Default.Delete, null, tint = AwanTheme.colors.destructive, modifier = Modifier.size(16.dp))
                }
            }
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
