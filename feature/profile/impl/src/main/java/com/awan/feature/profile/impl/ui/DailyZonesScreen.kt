package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesState
import com.awan.feature.profile.impl.ui.dailyzones.AddEditZoneSheet
import com.awan.feature.profile.impl.ui.dailyzones.DailyZonesContent
import com.awan.feature.profile.impl.ui.dailyzones.DailyZonesTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZonesScreen(
    uiState: DailyZonesState,
    onAction: (DailyZonesAction) -> Unit,
    onNavigateToRoutineDetails: (String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var showAddZoneSheet by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DailyZone?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<DailyZone?>(null) }

    val colors = AwanTheme.colors
    val dayColors = remember(uiState.templates, colors) {
        val mapping = mutableMapOf<DayOfWeek, Color>()
        val distinctColors = listOf(
            colors.zoneMelon,
            colors.zoneBlue,
            colors.zonePurple,
            colors.zonePink,
            colors.zoneGreen,
            colors.zoneYellow,
            colors.zoneOrange,
            colors.zoneRed,
            colors.zoneCyan,
            colors.zoneGray
        )
        
        uiState.templates.forEachIndexed { index, template ->
            val color = template.zones.firstOrNull()?.color?.toColor() 
                ?: distinctColors[index % distinctColors.size]
            template.daysOfWeek.forEach { day ->
                mapping[day] = color
            }
        }
        mapping
    }

    if (showAddZoneSheet) {
        AddEditZoneSheet(
            zone = editingZone,
            availableCategories = uiState.availableCategories,
            defaultStartTime = uiState.selectedDayZones.lastOrNull()?.endTime,
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
            onDelete = { zone ->
                showAddZoneSheet = false
                editingZone = null
                showDeleteConfirm = zone
            },
            canDelete = uiState.selectedDayZones.size > 1,
            isSaving = uiState.isSaving
        )
    }

    if (showDeleteConfirm != null) {
        AwanDialog(
            title = stringResource(R.string.profile_daily_zones_delete_zone_title),
            body = stringResource(R.string.profile_daily_zones_delete_zone_message),
            primaryLabel = stringResource(R.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(DailyZonesAction.DeleteZone(showDeleteConfirm!!))
                showDeleteConfirm = null
            },
            secondaryLabel = stringResource(R.string.profile_cancel),
            onSecondary = { showDeleteConfirm = null },
            onDismiss = { showDeleteConfirm = null }
        )
    }

    Scaffold(
        topBar = { DailyZonesTopBar(onBackClick = onBackClick) },
        containerColor = AwanTheme.colors.background
    ) { padding ->
        DailyZonesContent(
            uiState = uiState,
            dayColors = dayColors,
            onAction = onAction,
            onNavigateToRoutineDetails = onNavigateToRoutineDetails,
            onCreateRoutineClick = onCreateRoutineClick,
            onAddZoneClick = { showAddZoneSheet = true },
            onEditZone = { zone ->
                editingZone = zone
                showAddZoneSheet = true
            },
            padding = padding
        )
    }

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
