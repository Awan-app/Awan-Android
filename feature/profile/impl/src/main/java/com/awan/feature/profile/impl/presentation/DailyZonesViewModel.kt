package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.usecase.DeleteWeeklyTemplateUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyZonesViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val deleteWeeklyTemplateUseCase: DeleteWeeklyTemplateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyZonesState(selectedDay = DailyZonesHelper.getCurrentDay()))
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
            DailyZonesAction.ClearError -> clearError()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getWeeklyTemplatesUseCase()) {
                is Result.Success -> {
                    val templates = result.data
                    _uiState.update { state ->
                        val defaultTemplate = templates.find { it.name.equals("Default", ignoreCase = true) }
                            ?: templates.find { it.name.equals("My Week", ignoreCase = true) }
                            ?: templates.firstOrNull()

                        state.copy(
                            isLoading = false,
                            templates = templates,
                            selectedTemplateId = state.selectedTemplateId ?: defaultTemplate?.id
                        )
                    }
                    updateSelectedDayData()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = DailyZonesHelper.zonesErrorToUiText(result.error)
                    ) }
                }
                Result.Loading -> Unit
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

        val template = state.templates.find { it.id == state.selectedTemplateId }
            ?: state.templates.find { it.daysOfWeek.contains(state.selectedDay) }

        val zones = (template?.zones ?: emptyList())
            .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }

        _uiState.update { it.copy(
            selectedDayZones = zones,
            currentTemplate = template
        ) }
    }

    private fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = deleteWeeklyTemplateUseCase(templateId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, selectedTemplateId = null) }
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
        val zones = _uiState.value.selectedDayZones.toMutableList()
        val item = zones.removeAt(fromIndex)
        zones.add(toIndex, item)
        _uiState.update { it.copy(selectedDayZones = zones) }
        saveZones(zones)
    }

    private fun addZone(zone: DailyZone) {
        val currentZones = _uiState.value.selectedDayZones
        if (DailyZonesHelper.isOverlapping(zone, currentZones)) {
            _uiState.update { it.copy(error = UiText.DynamicString("Zones cannot overlap")) }
            return
        }

        val newZones = (currentZones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
        _uiState.update { it.copy(selectedDayZones = newZones) }
        saveZones(newZones)
    }

    private fun updateZone(zone: DailyZone) {
        val currentZones = _uiState.value.selectedDayZones
        val otherZones = currentZones.filter { it.id != zone.id }
        if (DailyZonesHelper.isOverlapping(zone, otherZones)) {
            _uiState.update { it.copy(error = UiText.DynamicString("Zones cannot overlap")) }
            return
        }

        val newZones = currentZones.map { if (it.id == zone.id) zone else it }
            .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
        _uiState.update { it.copy(selectedDayZones = newZones) }
        saveZones(newZones)
    }

    private fun deleteZone(zone: DailyZone) {
        val newZones = _uiState.value.selectedDayZones.filter { it.id != zone.id }
        _uiState.update { it.copy(selectedDayZones = newZones) }
        saveZones(newZones)
    }

    private fun saveZones(zones: List<DailyZone>) {
        val templateId = _uiState.value.currentTemplate?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = updateTemplateZonesUseCase(templateId, zones)
            when (result) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false) }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
