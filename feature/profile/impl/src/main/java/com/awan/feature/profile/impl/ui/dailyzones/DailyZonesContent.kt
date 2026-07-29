package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesState
import com.awan.feature.profile.impl.ui.components.*

@Composable
fun DailyZonesContent(
    uiState: DailyZonesState,
    dayColors: Map<DayOfWeek, Color>,
    onAction: (DailyZonesAction) -> Unit,
    onNavigateToRoutineDetails: (String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onAddZoneClick: () -> Unit,
    onEditZone: (DailyZone) -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            DaySelector(
                selectedDays = setOf(uiState.selectedDay),
                dayColors = dayColors,
                onDaySelected = { onAction(DailyZonesAction.SelectDay(it)) }
            )
        }

        val currentTemplateId = uiState.currentTemplate?.id
        RoutineSummaryCard(
            day = uiState.selectedDay,
            templateName = uiState.currentTemplate?.name ?: "Default",
            zoneCount = uiState.selectedDayZones.size,
            onEditRoutineClick = if (!uiState.isLoading && currentTemplateId != null) {
                { onNavigateToRoutineDetails(currentTemplateId) }
            } else null
        )

        RoutinePicker(
            templates = uiState.templates,
            selectedTemplateId = uiState.currentTemplate?.id,
            onTemplateSelected = { onAction(DailyZonesAction.SelectTemplate(it)) },
            onCreateRoutineClick = onCreateRoutineClick
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.isLoading -> {
                    Box(
                        Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AwanTheme.colors.sky)
                    }
                }

                uiState.selectedDayZones.isEmpty() -> {
                    EmptyZonesState(
                        hasTemplate = uiState.currentTemplate != null
                    )
                }

                else -> {
                    DailyZonesTimeline(
                        zones = uiState.selectedDayZones,
                        onEditZone = onEditZone
                    )
                }
            }
        }

        DailyZonesBottomActions(
            hasTemplate = uiState.currentTemplate != null,
            isSaving = uiState.isSaving,
            onAddZoneClick = onAddZoneClick,
            onCreateRoutineClick = onCreateRoutineClick
        )
    }
}
