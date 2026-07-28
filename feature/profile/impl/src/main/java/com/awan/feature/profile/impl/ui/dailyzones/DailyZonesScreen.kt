package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesState
import com.awan.feature.profile.impl.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZonesScreen(
    uiState: DailyZonesState,
    onAction: (DailyZonesAction) -> Unit,
    /** Navigate to DayDetailsRoute(date) — called from Customize a Day picker and when user taps a day */
    onNavigateToDayDetails: (String) -> Unit,
    /** Navigate to RoutineDetailsRoute(templateId) — called from Edit Routine */
    onNavigateToRoutineDetails: (String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var showAddZoneSheet by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DailyZone?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<DailyZone?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showCustomizeDaySheet by remember { mutableStateOf(false) }

    // Compute which days of THIS week have overrides — for indicator dots
    val overriddenDays = remember(uiState.overrides) {
        uiState.overrides.mapNotNull { DailyZonesHelper.dateStringToDayOfWeek(it.dateOfDay) }.toSet()
    }

    // ── Add / Edit Zone Sheet ──────────────────────────────
    if (showAddZoneSheet) {
        AddEditZoneSheet(
            zone = editingZone,
            templateName = uiState.currentTemplate?.name ?: "Default",
            onDismiss = {
                showAddZoneSheet = false
                editingZone = null
            },
            onConfirm = { zone ->
                if (editingZone == null) {
                    onAction(DailyZonesAction.AddZone(zone))
                } else {
                    onAction(DailyZonesAction.UpdateZone(zone))
                }
                showAddZoneSheet = false
                editingZone = null
            },
            isSaving = uiState.isSaving
        )
    }

    // ── Delete Confirmation ────────────────────────────────
    if (showDeleteConfirm != null) {
        AwanDialog(
            title = stringResource(R.string.profile_daily_zones_delete_zone_title),
            body = stringResource(R.string.profile_daily_zones_delete_zone_message),
            primaryLabel = "Delete",
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(DailyZonesAction.DeleteZone(showDeleteConfirm!!))
                showDeleteConfirm = null
            },
            secondaryLabel = "Cancel",
            onSecondary = { showDeleteConfirm = null },
            onDismiss = { showDeleteConfirm = null }
        )
    }

    // ── Reset to Default Confirmation ──────────────────────
    if (showResetConfirm) {
        val dayLabel = DailyZonesHelper.displayName(uiState.selectedDay)
        AwanDialog(
            title = stringResource(R.string.profile_daily_zones_reset_day_title, dayLabel),
            body = stringResource(R.string.profile_daily_zones_reset_day_message, dayLabel),
            primaryLabel = "Reset",
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(DailyZonesAction.ResetDay)
                showResetConfirm = false
            },
            secondaryLabel = "Cancel",
            onSecondary = { showResetConfirm = false },
            onDismiss = { showResetConfirm = false }
        )
    }

    // ── Customize a Day — Day Picker Sheet ─────────────────
    if (showCustomizeDaySheet) {
        CustomizeDayPickerSheet(
            overriddenDays = overriddenDays,
            onDaySelected = { day ->
                showCustomizeDaySheet = false
                onNavigateToDayDetails(DailyZonesHelper.getDateForDay(day))
            },
            onDismiss = { showCustomizeDaySheet = false }
        )
    }

    // ── Main Scaffold ──────────────────────────────────────
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AwanText(text = "Daily zones", style = AwanTheme.styles.titleText)
                        AwanText(
                            text = "Shape your week your way.",
                            style = AwanTheme.styles.metaText
                        )
                    }
                },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = AwanTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Icon(
                            painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AwanTheme.colors.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AwanTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Weekly Day Selector ────────────────────────
            val currentTemplateColor = uiState.currentTemplate?.zones?.firstOrNull()?.color?.toColor() ?: AwanTheme.colors.sky
            val currentTemplateDays = uiState.currentTemplate?.daysOfWeek ?: emptyList()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AwanTheme.colors.surface,
                border = BorderStroke(1.dp, AwanTheme.colors.line)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    DaySelector(
                        selectedDay = uiState.selectedDay,
                        overriddenDays = overriddenDays,
                        templateDays = currentTemplateDays,
                        templateColor = currentTemplateColor,
                        onDaySelected = { onAction(DailyZonesAction.SelectDay(it)) }
                    )
                }
            }

            // ── Routine Picker (Horizontal List) ───────────
            if (uiState.templates.isNotEmpty()) {
                RoutinePicker(
                    templates = uiState.templates,
                    selectedTemplateId = uiState.currentTemplate?.id,
                    onTemplateSelected = { onAction(DailyZonesAction.SelectTemplate(it)) }
                )
            }

            // ── Routine Summary Card ───────────────────────
            val currentTemplateId = uiState.currentTemplate?.id
            RoutineSummaryCard(
                day = uiState.selectedDay,
                templateName = if (uiState.currentOverride != null) {
                    stringResource(R.string.profile_daily_zones_custom_schedule)
                } else {
                    uiState.currentTemplate?.name ?: "Default"
                },
                zoneCount = uiState.selectedDayZones.size,
                isOverride = uiState.currentOverride != null,
                onResetClick = { showResetConfirm = true },
                templates = uiState.templates,
                onTemplateSelected = { onAction(DailyZonesAction.SelectTemplate(it)) },
                onCreateRoutineClick = onCreateRoutineClick,
                onEditRoutineClick = if (!uiState.isLoading && currentTemplateId != null && uiState.currentOverride == null) {
                    { onNavigateToRoutineDetails(currentTemplateId) }
                } else null,
                onCustomizeDayClick = if (uiState.currentOverride == null) {
                    { showCustomizeDaySheet = true }
                } else null
            )

            // ── Zone List / Loading / Empty ────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AwanTheme.colors.sky)
                        }
                    }
                    uiState.selectedDayZones.isEmpty() -> {
                        EmptyZonesState(onAddZoneClick = { showAddZoneSheet = true })
                    }
                    else -> {
                        DailyZonesTimeline(
                            zones = uiState.selectedDayZones,
                            onReorder = { from, to -> onAction(DailyZonesAction.ReorderZones(from, to)) },
                            onEditZone = { zone ->
                                editingZone = zone
                                showAddZoneSheet = true
                            },
                            onDeleteZone = { zone ->
                                showDeleteConfirm = zone
                            }
                        )
                    }
                }
            }

            // ── Bottom Actions ─────────────────────────────
            Column(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary: Create Routine
                AwanButton(
                    onClick = onCreateRoutineClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AwanButtonVariant.Secondary,
                    icon = Icons.Default.DateRange,
                    enabled = !uiState.isSaving
                ) {
                    AwanText(text = "Create New Routine")
                }

                // Primary: Add zone
                AwanButton(
                    onClick = { showAddZoneSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Add,
                    enabled = !uiState.isSaving
                ) {
                    AwanText(text = stringResource(R.string.profile_daily_zones_add_zone))
                }
            }
        }
    }

    // ── Error Snackbar (overlay) ───────────────────────────
    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AwanErrorSnackbar(
                message = uiState.error.asString(),
                onDismiss = { onAction(DailyZonesAction.ClearError) }
            )
        }
    }
}

@Composable
fun AwanErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AwanTheme.colors.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = AwanTheme.colors.destructive)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(text = message, style = AwanTheme.styles.bodyText)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
fun EmptyZonesState(onAddZoneClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CalendarToday,
            null,
            modifier = Modifier.size(64.dp),
            tint = AwanTheme.colors.line
        )
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.profile_daily_zones_no_zones),
            style = AwanTheme.styles.titleText
        )
        AwanText(
            text = stringResource(R.string.profile_daily_zones_no_zones_hint),
            style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(24.dp))
        AwanButton(
            onClick = onAddZoneClick,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Add
        ) {
            AwanText(text = stringResource(R.string.profile_daily_zones_add_zone))
        }
    }
}
