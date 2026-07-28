package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.usecase.CreateTemplateOverrideUseCase
import com.awan.app.core.domain.zones.usecase.DeleteTemplateOverrideUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideZonesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.copy
import kotlin.plus

@HiltViewModel
class DailyZonesViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val createTemplateOverrideUseCase: CreateTemplateOverrideUseCase,
    private val updateOverrideZonesUseCase: UpdateOverrideZonesUseCase,
    private val deleteTemplateOverrideUseCase: DeleteTemplateOverrideUseCase,
    private val zonesRepository: ZonesRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DailyZonesState(selectedDay = DailyZonesHelper.getTodayDayOfWeek()))
    val uiState: StateFlow<DailyZonesState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onAction(action: DailyZonesAction) {
        when (action) {
            DailyZonesAction.LoadData -> loadData()
            is DailyZonesAction.SelectDay -> selectDay(action.day)
            is DailyZonesAction.SelectTemplate -> selectTemplate(action.templateId)
            is DailyZonesAction.DeleteTemplate -> deleteTemplate(action.templateId)
            is DailyZonesAction.ReorderZones -> reorderZones(action.fromIndex, action.toIndex)
            is DailyZonesAction.AddZone -> addZone(action.zone)
            is DailyZonesAction.UpdateZone -> updateZone(action.zone)
            is DailyZonesAction.DeleteZone -> deleteZone(action.zone)
            DailyZonesAction.CustomizeDay -> customizeDay()
            DailyZonesAction.ResetDay -> resetDay()
            DailyZonesAction.ClearError -> clearError()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val templatesResult = getWeeklyTemplatesUseCase()
            val overridesResult = zonesRepository.getOverrides()

            if (templatesResult is Result.Success && overridesResult is Result.Success) {
                val templates = templatesResult.data
                val overrides = overridesResult.data

                _uiState.update { state ->
                    val defaultTemplate = templates.find { it.name.equals("Default", ignoreCase = true) }
                        ?: templates.find { it.name.equals("My Week", ignoreCase = true) }
                        ?: templates.firstOrNull()
                    val newSelectedTemplateId = state.selectedTemplateId ?: defaultTemplate?.id

                    state.copy(
                        isLoading = false,
                        templates = templates,
                        overrides = overrides,
                        selectedTemplateId = newSelectedTemplateId
                    )
                }
                updateSelectedDayData()
            } else {
                val error = (templatesResult as? Result.Error)?.error
                    ?: (overridesResult as? Result.Error)?.error
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error?.let { DailyZonesHelper.zonesErrorToUiText(it) } ?: UiText.DynamicString("Failed to load data")
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
        val date = DailyZonesHelper.getDateForDayOfWeek(state.selectedDay)

        val override = state.overrides.find { it.dateOfDay == date }

        // If a template is explicitly selected, use it. Otherwise find the one for the day.
        val template = state.templates.find { it.id == state.selectedTemplateId }
            ?: state.templates.find { it.daysOfWeek.contains(state.selectedDay) }

        val zones = (override?.zones ?: template?.zones ?: emptyList())
            .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }

        _uiState.update { it.copy(
            selectedDayZones = zones,
            currentOverride = override,
            currentTemplate = template
        ) }
    }

    private fun deleteTemplate(templateId: String) {
        val template = _uiState.value.templates.find { it.id == templateId }
        if (template?.name?.equals("Default", ignoreCase = true) == true) {
            _uiState.update { it.copy(error = UiText.DynamicString("Cannot delete Default routine")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = zonesRepository.deleteTemplate(templateId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(selectedTemplateId = null) }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun reorderZones(fromIndex: Int, toIndex: Int) {
        val currentZones = _uiState.value.selectedDayZones.toMutableList()
        if (fromIndex !in currentZones.indices || toIndex !in currentZones.indices) return

        val movedZone = currentZones.removeAt(fromIndex)
        currentZones.add(toIndex, movedZone)

        saveZones(currentZones)
    }

    private fun addZone(zone: DailyZone) {
        val startMins = DailyZonesHelper.parseTimeToMinutes(zone.startTime)
        val endMins = DailyZonesHelper.parseTimeToMinutes(zone.endTime)
        if (startMins >= endMins) {
            _uiState.update { it.copy(error = UiText.DynamicString("Start time must be before end time")) }
            return
        }

        if (DailyZonesHelper.isOverlapping(zone, _uiState.value.selectedDayZones)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_daily_zones_error_overlap)) }
            return
        }

        val updatedZones = (_uiState.value.selectedDayZones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
        saveZones(updatedZones)
    }

    private fun updateZone(zone: DailyZone) {
        val startMins = DailyZonesHelper.parseTimeToMinutes(zone.startTime)
        val endMins = DailyZonesHelper.parseTimeToMinutes(zone.endTime)
        if (startMins >= endMins) {
            _uiState.update { it.copy(error = UiText.DynamicString("Start time must be before end time")) }
            return
        }

        val updatedZones = _uiState.value.selectedDayZones.map {
            if (it.id != null && it.id == zone.id) zone else if (it.id == null && it.name == zone.name) zone else it
        }.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }

        saveZones(updatedZones)
    }

    private fun deleteZone(zone: DailyZone) {
        val updatedZones = _uiState.value.selectedDayZones.filterNot {
            if (it.id != null) it.id == zone.id else it.name == zone.name
        }
        saveZones(updatedZones)
    }

    private fun saveZones(zones: List<DailyZone>) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val result = when {
                state.currentOverride != null -> {
                    updateOverrideZonesUseCase(state.currentOverride.id, zones)
                }
                state.currentTemplate != null -> {
                    updateTemplateZonesUseCase(state.currentTemplate.id, zones)
                }
                else -> Result.Error(AppError.Unknown())
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun customizeDay() {
        val state = _uiState.value
        if (state.currentOverride != null) return

        val date = DailyZonesHelper.getDateForDayOfWeek(state.selectedDay)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = createTemplateOverrideUseCase(date, state.selectedDayZones)

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadData()
                }
                is Result.Error -> {
                    val appError = result.error
                    val msg = if (appError is AppError.Api &&
                        appError.errorCode == "DAY_ALREADY_ASSIGNED"
                    ) {
                        UiText.StringResource(R.string.profile_daily_zones_error_day_assigned)
                    } else {
                        DailyZonesHelper.zonesErrorToUiText(appError)
                    }
                    _uiState.update { it.copy(isSaving = false, error = msg) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun resetDay() {
        val state = _uiState.value
        val override = state.currentOverride ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = deleteTemplateOverrideUseCase(override.id)

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}