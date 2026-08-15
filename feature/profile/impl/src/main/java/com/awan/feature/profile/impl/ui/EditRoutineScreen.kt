package com.awan.feature.profile.impl.ui

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var showZoneDeleteConfirm by remember { mutableStateOf<DailyZone?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.validationError, uiState.error) {
        val error = (uiState.validationError ?: uiState.error)?.asString(context)
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
                startTime = uiState.zones.lastOrNull()?.endTime ?: "09:00:00",
                endTime = uiState.zones.lastOrNull()?.endTime?.let {
                    DailyZonesHelper.parseTimeToMinutes(it)?.let { minutes ->
                        DailyZonesHelper.formatMinutesToTime(minutes + 60)
                    }
                } ?: "10:00:00",
                color = "#2EAAFF",
                categoryId = uiState.availableCategories.firstOrNull()?.id
            ),
            availableCategories = uiState.availableCategories,
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
            onAddCategory = { name ->
                onAction(EditRoutineAction.CreateCategory(name))
            },
            onDelete = editingZone?.let { zone ->
                {
                    showZoneDeleteConfirm = zone
                    showZoneSheet = false
                }
            },
            canDelete = uiState.zones.size > 1
        )
    }

    if (showZoneDeleteConfirm != null) {
        AwanDialog(
            title = stringResource(R.string.profile_daily_zones_delete_zone_title),
            body = stringResource(R.string.profile_daily_zones_delete_zone_message),
            primaryLabel = stringResource(R.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(EditRoutineAction.DeleteZone(showZoneDeleteConfirm!!))
                showZoneDeleteConfirm = null
                editingZone = null
            },
            secondaryLabel = stringResource(R.string.profile_cancel),
            onSecondary = {
                showZoneDeleteConfirm = null
                editingZone = null
            },
            onDismiss = {
                showZoneDeleteConfirm = null
                editingZone = null
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

    if (showDatePicker) {
        AwanDatePickerDialog(
            initialDate = runCatching { LocalDate.parse(uiState.date) }.getOrDefault(LocalDate.now()),
            confirmLabel = stringResource(R.string.profile_ok),
            cancelLabel = stringResource(R.string.profile_cancel),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onAction(EditRoutineAction.DateChange(date.toString()))
                showDatePicker = false
            }
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
                        val isEdit = if (uiState.isLoading) uiState.templateId != null 
                                     else uiState.templateId != null || uiState.overrideId != null
                        
                        AwanText(
                            text = if (isEdit) stringResource(R.string.profile_routine_edit)
                            else stringResource(R.string.profile_routine_create),
                            style = AwanTheme.styles.titleText
                        )
                        
                        if (!uiState.isLoading) {
                            AwanText(
                                text = if (uiState.overrideId != null)
                                    stringResource(R.string.profile_routine_summary_subtitle_today)
                                else stringResource(R.string.profile_routine_summary_subtitle),
                                style = AwanTheme.styles.metaText
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        AwanIconButton(
                            onClick = onBackClick,
                            contentDescription = stringResource(R.string.profile_back)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = AwanTheme.colors.textPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (!uiState.isLoading && (uiState.templateId != null || uiState.overrideId != null)) {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            AwanIconButton(
                                onClick = { showDeleteConfirm = true },
                                contentDescription = stringResource(R.string.profile_routine_delete)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = AwanTheme.colors.destructive
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        bottomBar = {
            if (!uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
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
        },
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(AwanTheme.colors.backgroundStart, AwanTheme.colors.background)
            )
        )
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AwanTheme.colors.sky)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Routine Name
                AwanCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
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
                }

                // Apply to Today Only (Checkbox) - Show if we have a date context
                if (uiState.date != null) {
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        onClick = {
                            if (uiState.overrideId == null && uiState.templateId != null) {
                                onAction(EditRoutineAction.ToggleTodayOnly(!uiState.isTodayOnly))
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = uiState.isTodayOnly,
                                onCheckedChange = { onAction(EditRoutineAction.ToggleTodayOnly(it)) },
                                enabled = uiState.overrideId == null && uiState.templateId != null,
                                colors = CheckboxDefaults.colors(checkedColor = AwanTheme.colors.sky)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                AwanText(
                                    text = stringResource(R.string.profile_routine_apply_to_today_only),
                                    style = AwanTheme.styles.bodyText.copy(
                                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                )
                                if (uiState.templateId == null && uiState.overrideId == null) {
                                    AwanText(
                                        text = stringResource(R.string.profile_daily_zones_customize_day_hint),
                                        style = AwanTheme.styles.captionText.copy(
                                            color = AwanTheme.colors.textSecondary
                                        )
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { showDatePicker = true }
                                ) {
                                    AwanText(
                                        text = uiState.date!!,
                                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = AwanTheme.colors.sky,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Days Selection
                if (!uiState.isTodayOnly) {
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.clickable { onAction(EditRoutineAction.DateChange(LocalDate.now().toString())) }
                                ) {
                                    val referenceDate = runCatching { LocalDate.parse(uiState.date) }.getOrDefault(LocalDate.now())
                                    val dayNum = referenceDate.dayOfMonth
                                    val suffix = getDayOfMonthSuffix(dayNum)
                                    val formatter = DateTimeFormatter.ofPattern("MMMM d'$suffix' EEEE", Locale.ENGLISH)
                                    val dateStr = referenceDate.format(formatter)
                                    
                                    AwanText(
                                        text = dateStr,
                                        style = AwanTheme.styles.bodyText.copy(
                                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                                        )
                                    )
                                }

                                AwanIconButton(
                                    onClick = { showDatePicker = true },
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = AwanTheme.colors.sky,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DaySelector(
                                selectedDays = uiState.selectedDays,
                                assignedDays = uiState.assignedDays,
                                onDaySelected = { onAction(EditRoutineAction.ToggleDay(it)) },
                                onNextWeek = { 
                                    val current = runCatching { LocalDate.parse(uiState.date) }.getOrDefault(LocalDate.now())
                                    onAction(EditRoutineAction.DateChange(current.plusWeeks(1).toString()))
                                },
                                onPreviousWeek = { 
                                    val current = runCatching { LocalDate.parse(uiState.date) }.getOrDefault(LocalDate.now())
                                    onAction(EditRoutineAction.DateChange(current.minusWeeks(1).toString()))
                                },
                                showTodayIndicator = false,
                                referenceDate = runCatching { LocalDate.parse(uiState.date) }.getOrDefault(LocalDate.now()),
                                today = LocalDate.now()
                            )
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
                            AwanText(
                                text = stringResource(R.string.profile_routine_add_zone),
                                style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky)
                            )
                        }
                    }

                    if (uiState.zones.isEmpty()) {
                        val infiniteTransition =
                            rememberInfiniteTransition(label = "mascot_float_edit")
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
}

private fun getDayOfMonthSuffix(n: Int): String {
    if (n in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
