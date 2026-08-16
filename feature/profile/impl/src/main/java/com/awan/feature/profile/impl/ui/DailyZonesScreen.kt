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
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesState
import com.awan.feature.profile.impl.ui.dailyzones.DailyZonesContent
import com.awan.feature.profile.impl.ui.dailyzones.DailyZonesTopBar
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZonesScreen(
    uiState: DailyZonesState,
    onAction: (DailyZonesAction) -> Unit,
    onNavigateToRoutineDetails: (String?, String?, String?) -> Unit,
    onCreateRoutineClick: (String?, String?, String?) -> Unit,
    onBackClick: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf<DailyZone?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val colors = AwanTheme.colors
    val dayColors = remember(uiState.templates, uiState.overrides, colors) {
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
        
        // 1. Templates colors - Only for templates with zones
        uiState.templates.filter { it.zones.isNotEmpty() }.forEachIndexed { index, template ->
            val color = template.zones.firstOrNull()?.color?.toColor() 
                ?: distinctColors[index % distinctColors.size]
            template.daysOfWeek.forEach { day ->
                mapping[day] = color
            }
        }
        mapping
    }

    val specialDates = remember(uiState.overrides, colors) {
        val mapping = mutableMapOf<String, Color>()
        // 2. Overrides (Custom Days) - Only for overrides with zones
        uiState.overrides.filter { it.zones.isNotEmpty() }.forEach { override ->
            mapping[override.dateOfDay] = override.zones.firstOrNull()?.color?.toColor() ?: colors.sky
        }
        mapping
    }

    if (showDatePicker) {
        AwanDatePickerDialog(
            initialDate = uiState.selectedDate ?: LocalDate.now(),
            confirmLabel = stringResource(R.string.profile_ok),
            cancelLabel = stringResource(R.string.profile_cancel),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onAction(DailyZonesAction.DateSelected(date))
                showDatePicker = false
            }
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
            specialDates = specialDates,
            onAction = onAction,
            onNavigateToRoutineDetails = onNavigateToRoutineDetails,
            onCreateRoutineClick = onCreateRoutineClick,
            onShowDatePicker = { showDatePicker = true },
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
