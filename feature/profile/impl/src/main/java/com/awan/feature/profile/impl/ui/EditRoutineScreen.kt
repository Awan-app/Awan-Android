package com.awan.feature.profile.impl.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.EditRoutineAction
import com.awan.feature.profile.impl.presentation.EditRoutineState
import com.awan.feature.profile.impl.ui.components.DailyZoneReorderList
import com.awan.feature.profile.impl.ui.components.DaySelector
import com.awan.feature.profile.impl.ui.components.ZoneEditSheet

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
            title = stringResource(R.string.profile_routine_delete_confirm_title),
            body = stringResource(R.string.profile_routine_delete_confirm_message),
            primaryLabel = stringResource(R.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(EditRoutineAction.DeleteRoutine)
                showDeleteConfirm = false
            },
            secondaryLabel = stringResource(R.string.profile_cancel),
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
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AwanText(
                            text = if (uiState.templateId == null) stringResource(R.string.profile_routine_create) else stringResource(R.string.profile_routine_edit),
                            style = AwanTheme.styles.titleText
                        )
                        AwanText(
                            text = stringResource(R.string.profile_routine_summary_subtitle),
                            style = AwanTheme.styles.metaText
                        )
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        AwanIconButton(onClick = onBackClick, contentDescription = stringResource(R.string.profile_back)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                        }
                    }
                },
                actions = {
                    if (uiState.templateId != null) {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            AwanIconButton(
                                onClick = { showDeleteConfirm = true },
                                contentDescription = stringResource(R.string.profile_routine_delete)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = AwanTheme.colors.destructive)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background)
            )
        },
        containerColor = AwanTheme.colors.background,
        bottomBar = {
            Box(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                AwanButton(
                    onClick = { onAction(EditRoutineAction.SaveRoutine) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isSaving,
                    icon = Icons.Default.Check
                ) {
                    AwanText(text = stringResource(R.string.profile_routine_save))
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
                    text = stringResource(R.string.profile_routine_name),
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                AwanTextField(
                    value = uiState.name,
                    onValueChange = { onAction(EditRoutineAction.NameChange(it)) },
                    placeholder = stringResource(R.string.profile_routine_name_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Days Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(
                    text = stringResource(R.string.profile_routine_apply_to_days),
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                DaySelector(
                    selectedDays = uiState.selectedDays,
                    assignedDays = uiState.assignedDays,
                    onDaySelected = { onAction(EditRoutineAction.ToggleDay(it)) },
                    showTodayIndicator = false // Don't show "today" dot in routine creator
                )
            }

            // Zones
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AwanText(
                        text = stringResource(R.string.profile_routine_zones),
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
                        AwanText(text = stringResource(R.string.profile_routine_add_zone), style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
                    }
                }

                if (uiState.zones.isEmpty()) {
                    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float_edit")
                    val animY by infiniteTransition.animateFloat(
                        initialValue = -6f,
                        targetValue = 6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "float"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer { translationY = animY },
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AwanText(
                            text = stringResource(R.string.profile_routine_no_zones),
                            style = AwanTheme.styles.titleText.copy(
                                textStyle = AwanTheme.styles.titleText.textStyle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AwanText(
                            text = stringResource(R.string.profile_routine_no_zones_hint),
                            style = AwanTheme.styles.bodyText.copy(
                                color = AwanTheme.colors.textSecondary,
                                textStyle = AwanTheme.styles.bodyText.textStyle.copy(textAlign = TextAlign.Center)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
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
