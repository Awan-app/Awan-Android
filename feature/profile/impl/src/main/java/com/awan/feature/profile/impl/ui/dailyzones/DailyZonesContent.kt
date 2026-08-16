package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesState
import com.awan.feature.profile.impl.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyZonesContent(
    uiState: DailyZonesState,
    dayColors: Map<DayOfWeek, Color>,
    onAction: (DailyZonesAction) -> Unit,
    onNavigateToRoutineDetails: (String?, String?, String?) -> Unit,
    onCreateRoutineClick: (String?, String?, String?) -> Unit,
    onShowDatePicker: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
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
                        modifier = Modifier.clickable { onAction(DailyZonesAction.GoToToday) }
                    ) {
                        val selectedDate = uiState.selectedDate ?: LocalDate.now()
                        val dayNum = selectedDate.dayOfMonth
                        val suffix = getDayOfMonthSuffix(dayNum)
                        val formatter = DateTimeFormatter.ofPattern("MMMM d'$suffix' EEEE", Locale.ENGLISH)
                        val dateStr = selectedDate.format(formatter)
                        
                        AwanText(
                            text = dateStr,
                            style = AwanTheme.styles.bodyText.copy(
                                textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                            )
                        )
                    }

                    AwanIconButton(
                        onClick = onShowDatePicker,
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
                    selectedDays = setOf(uiState.selectedDay),
                    dayColors = dayColors,
                    onDaySelected = { onAction(DailyZonesAction.SelectDay(it)) },
                    onNextWeek = { onAction(DailyZonesAction.NextWeek) },
                    onPreviousWeek = { onAction(DailyZonesAction.PreviousWeek) },
                    referenceDate = uiState.selectedDate ?: LocalDate.now(),
                    today = LocalDate.now()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Middle Scrollable Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val currentTemplateId = uiState.currentTemplate?.id
            val currentOverrideId = uiState.currentOverride?.id
            val currentOverrideDate = uiState.currentOverride?.dateOfDay
            val routineName = uiState.currentOverride?.name 
                ?: uiState.currentTemplate?.name 
                ?: stringResource(R.string.profile_routine_default_name)
                
            RoutineSummaryCard(
                day = uiState.selectedDay,
                templateName = routineName,
                zoneCount = uiState.selectedDayZones.size,
                onEditRoutineClick = if (!uiState.isLoading && (currentTemplateId != null || currentOverrideId != null)) {
                    { onNavigateToRoutineDetails(currentTemplateId, currentOverrideId, currentOverrideDate) }
                } else null,
                isOverride = uiState.currentOverride != null
            )

            /*if (uiState.currentTemplate != null || uiState.currentOverride != null || uiState.templates.isNotEmpty()) {
                RoutinePicker(
                    templates = uiState.templates,
                    selectedTemplateId = uiState.currentTemplate?.id,
                    onTemplateSelected = { onAction(DailyZonesAction.SelectTemplate(it)) },
                    onCreateRoutineClick = onCreateRoutineClick,
                    selectedDate = uiState.selectedDate?.toString(),
                    currentOverrideName = uiState.currentOverride?.name,
                    title = stringResource(R.string.profile_routine_select)
                )
            }*/

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AwanTheme.colors.sky)
                }
            } else if (uiState.selectedDayZones.isEmpty()) {
                EmptyZonesState(
                    hasTemplate = uiState.currentTemplate != null || uiState.currentOverride != null
                )
            } else {
                DailyZonesTimeline(
                    zones = uiState.selectedDayZones
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DailyZonesBottomActions(
            hasTemplate = uiState.currentTemplate != null || (uiState.templates.any { it.daysOfWeek.contains(uiState.selectedDay) }),
            hasOverride = uiState.currentOverride != null,
            onCreateRoutineClick = onCreateRoutineClick,
            onCustomizeClick = { _, date -> 
                val templateToUse = uiState.currentTemplate ?: uiState.templates.find { it.daysOfWeek.contains(uiState.selectedDay) }
                onNavigateToRoutineDetails(templateToUse?.id, uiState.currentOverride?.id, date)
            },
            selectedDate = uiState.selectedDate?.toString()
        )
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
