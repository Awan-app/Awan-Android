package com.awan.feature.profile.impl.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.EditRoutineAction
import com.awan.feature.profile.impl.presentation.EditRoutineState
import com.awan.feature.profile.impl.ui.components.DailyZoneReorderList
import com.awan.feature.profile.impl.ui.components.ZoneEditSheet
import com.awan.feature.profile.impl.ui.dailyzones.AwanErrorSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    uiState: EditRoutineState,
    onAction: (EditRoutineAction) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showZoneSheet by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DailyZone?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.validationError, uiState.error) {
        val error = uiState.validationError ?: uiState.error?.asString(context)
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showZoneSheet) {
        ZoneEditSheet(
            zone = editingZone ?: DailyZone(
                id = null,
                name = "",
                startTime = uiState.zones.lastOrNull()?.endTime ?: "09:00",
                endTime = uiState.zones.lastOrNull()?.endTime?.let { 
                    DailyZonesHelper.formatMinutesToTime(DailyZonesHelper.parseTimeToMinutes(it) + 60)
                } ?: "10:00",
                color = "#2EAAFF"
            ),
            isNew = editingZone == null,
            onDismiss = {
                showZoneSheet = false
                editingZone = null
            },
            onConfirm = { zone ->
                if (editingZone == null) {
                    onAction(EditRoutineAction.AddZone(zone))
                } else {
                    onAction(EditRoutineAction.UpdateZone(editingZone!!, zone))
                }
                showZoneSheet = false
                editingZone = null
            },
            onDelete = editingZone?.let { zone ->
                {
                    onAction(EditRoutineAction.DeleteZone(zone))
                    showZoneSheet = false
                    editingZone = null
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AwanDialog(
            title = "Delete Routine",
            body = "Are you sure you want to delete this routine? This action cannot be undone.",
            primaryLabel = "Delete",
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(EditRoutineAction.DeleteRoutine)
                showDeleteConfirm = false
            },
            secondaryLabel = "Cancel",
            onSecondary = { showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AwanErrorSnackbar(
                    message = data.visuals.message,
                    onDismiss = { data.dismiss() }
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AwanText(
                            text = if (uiState.templateId == null) "Create Routine" else "Edit Routine",
                            style = AwanTheme.styles.titleText
                        )
                        AwanText(
                            text = "Templates for your recurring schedule",
                            style = AwanTheme.styles.metaText
                        )
                    }
                },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                    }
                },
                actions = {
                    if (uiState.templateId != null) {
                        AwanIconButton(
                            onClick = { showDeleteConfirm = true },
                            contentDescription = "Delete Routine"
                        ) {
                            Icon(Icons.Default.Delete, null, tint = AwanTheme.colors.destructive)
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
                AwanButton(
                    onClick = { onAction(EditRoutineAction.SaveRoutine) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isSaving,
                    icon = Icons.Default.Check
                ) {
                    AwanText(text = "Save Routine")
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Routine Name
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(
                    text = "Routine Name",
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                AwanTextField(
                    value = uiState.name,
                    onValueChange = { onAction(EditRoutineAction.NameChange(it)) },
                    placeholder = "e.g. Workday, Weekend, Vacation",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Days Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(
                    text = "Apply to Days",
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAction(EditRoutineAction.ToggleDay(day)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AwanText(
                                    text = DailyZonesHelper.abbreviation(day).take(1),
                                    style = AwanTheme.styles.bodyText.copy(
                                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        ),
                                        color = if (isSelected) Color.White else AwanTheme.colors.textPrimary
                                    )
                                )
                            }
                            AwanText(
                                text = DailyZonesHelper.abbreviation(day),
                                style = AwanTheme.styles.captionText.copy(
                                    textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp),
                                    color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary
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
                        editingZone = null
                        showZoneSheet = true
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AwanText(text = "Add Zone", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
                    }
                }

                if (uiState.zones.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AwanTheme.colors.surface,
                        border = BorderStroke(1.dp, AwanTheme.colors.line)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = AwanTheme.colors.line, modifier = Modifier.size(48.dp))
                            AwanText(text = "No zones yet", style = AwanTheme.styles.bodySecondaryText)
                        }
                    }
                } else {
                    DailyZoneReorderList(
                        zones = uiState.zones,
                        onOpen = { zone ->
                            editingZone = zone
                            showZoneSheet = true
                        },
                        onReorder = { from, to ->
                            onAction(EditRoutineAction.ReorderZones(from, to))
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
