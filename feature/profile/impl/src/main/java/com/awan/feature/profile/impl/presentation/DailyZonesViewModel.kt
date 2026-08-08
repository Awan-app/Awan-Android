// Claude
package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.usecase.GetTemplateOverridesUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideZonesUseCase
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DailyZonesViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val getTemplateOverridesUseCase: GetTemplateOverridesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val updateOverrideZonesUseCase: UpdateOverrideZonesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DailyZonesState(selectedDay = DailyZonesHelper.getCurrentDay()))
    val uiState: StateFlow<DailyZonesState> = _uiState.asStateFlow()

    fun onAction(action: DailyZonesAction) {
        when (action) {
            DailyZonesAction.LoadData -> loadData()
            is DailyZonesAction.SelectDay -> selectDay(action.day)
            is DailyZonesAction.SelectTemplate -> selectTemplate(action.templateId)
            is DailyZonesAction.AddZone -> addZone(action.zone)
            is DailyZonesAction.UpdateZone -> updateZone(action.zone)
            is DailyZonesAction.DeleteZone -> deleteZone(action.zone)
            DailyZonesAction.ClearError -> clearError()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val templatesDeferred = async { getWeeklyTemplatesUseCase() }
            val overridesDeferred = async { getTemplateOverridesUseCase() }
            val categoriesDeferred = async { getCategoriesUseCase() }

            val templatesResult = templatesDeferred.await()
            val overridesResult = overridesDeferred.await()
            val categoriesResult = categoriesDeferred.await()

            if (templatesResult is Result.Success && overridesResult is Result.Success && categoriesResult is Result.Success) {
                val templates = templatesResult.data
                val overrides = overridesResult.data
                val categories = categoriesResult.data

                _uiState.update { state ->
                    val defaultTemplate = templates.find { it.name.equals("Default", ignoreCase = true) }
                        ?: templates.find { it.name.equals("My Week", ignoreCase = true) }
                        ?: templates.firstOrNull()

                    state.copy(
                        isLoading = false,
                        templates = templates,
                        overrides = overrides,
                        availableCategories = categories,
                        selectedTemplateId = state.selectedTemplateId ?: defaultTemplate?.id
                    )
                }
                updateSelectedDayData()
            } else {
                val error = (templatesResult as? Result.Error)?.error
                    ?: (overridesResult as? Result.Error)?.error
                    ?: (categoriesResult as? Result.Error)?.error

                _uiState.update { it.copy(
                    isLoading = false,
                    error = error?.let { DailyZonesHelper.zonesErrorToUiText(it) }
                ) }
            }
        }
    }

    private fun selectDay(day: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = day, selectedTemplateId = null) }
        updateSelectedDayData()
    }

    private fun selectTemplate(templateId: String) {
        _uiState.update { it.copy(selectedTemplateId = templateId) }
        updateSelectedDayData()
    }

    private fun updateSelectedDayData() {
        val state = _uiState.value

        // 1. Resolution Order: Override for the date > Template matching day of week
        // Note: For simplicity in the routine list, we map selectedDay to the next occurrence of that day
        val targetDate = getNextDateForDay(state.selectedDay)
        val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val override = state.overrides.find { it.dateOfDay == dateStr }
        val template = state.templates.find { it.id == state.selectedTemplateId }
            ?: state.templates.find { it.daysOfWeek.contains(state.selectedDay) }

        val zones = when {
            override != null -> override.zones
            template != null -> template.zones
            else -> emptyList()
        }.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }

        _uiState.update { it.copy(
            selectedDayZones = zones,
            currentTemplate = template,
            currentOverride = override
        ) }
    }

    private fun getNextDateForDay(day: DayOfWeek): LocalDate {
        var date = LocalDate.now()
        while (DailyZonesHelper.getCurrentDay(date) != day) {
            date = date.plusDays(1)
        }
        return date
    }

    private fun addZone(zone: DailyZone) {
        val currentZones = _uiState.value.selectedDayZones
        if (DailyZonesHelper.isOverlapping(zone, currentZones)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_daily_zones_error_overlap)) }
            return
        }

        val newZones = (currentZones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
        saveZones(newZones)
    }

    private fun updateZone(zone: DailyZone) {
        val currentZones = _uiState.value.selectedDayZones
        val otherZones = currentZones.filter { it.id != zone.id }
        if (DailyZonesHelper.isOverlapping(zone, otherZones)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_daily_zones_error_overlap)) }
            return
        }

        val newZones = currentZones.map { if (it.id == zone.id) zone else it }
            .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
        saveZones(newZones)
    }

    private fun deleteZone(zone: DailyZone) {
        val currentZones = _uiState.value.selectedDayZones
        if (currentZones.size <= 1) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_validation_at_least_one_zone)) }
            return
        }
        val newZones = currentZones.filter { it.id != zone.id }
        _uiState.update { it.copy(selectedDayZones = newZones) }
        saveZones(newZones)
    }

    private fun saveZones(zones: List<DailyZone>) {
        if (zones.isEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_validation_at_least_one_zone)) }
            return
        }
        val state = _uiState.value
        val overrideId = state.currentOverride?.id
        val templateId = state.currentTemplate?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val result = when {
                overrideId != null -> updateOverrideZonesUseCase(overrideId, zones)
                templateId != null -> updateTemplateZonesUseCase(templateId, zones)
                else -> {
                    _uiState.update { it.copy(isSaving = false, error = UiText.StringResource(R.string.profile_daily_zones_error_generic)) }
                    return@launch
                }
            }

            when (result) {
                is Result.Success -> {
                    val updatedZones = result.data.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
                    _uiState.update { state ->
                        if (overrideId != null) {
                            val updatedOverrides = state.overrides.map { 
                                if (it.id == overrideId) it.copy(zones = result.data) else it
                            }
                            state.copy(
                                isSaving = false,
                                selectedDayZones = updatedZones,
                                overrides = updatedOverrides,
                                currentOverride = updatedOverrides.find { it.id == overrideId }
                            )
                        } else {
                            val updatedTemplates = state.templates.map { 
                                if (it.id == templateId) it.copy(zones = result.data) else it
                            }
                            state.copy(
                                isSaving = false,
                                selectedDayZones = updatedZones,
                                templates = updatedTemplates,
                                currentTemplate = updatedTemplates.find { it.id == templateId }
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
