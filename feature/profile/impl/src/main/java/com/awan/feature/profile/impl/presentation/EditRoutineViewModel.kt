package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.repository.ZonesRepository
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

@HiltViewModel
class EditRoutineViewModel @Inject constructor(
    private val zonesRepository: ZonesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRoutineState())
    val uiState: StateFlow<EditRoutineState> = _uiState.asStateFlow()

    private val _events = Channel<EditRoutineEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: EditRoutineAction) {
        when (action) {
            is EditRoutineAction.LoadTemplate -> loadTemplate(action.templateId)
            is EditRoutineAction.NameChange -> onNameChange(action.name)
            is EditRoutineAction.ToggleDay -> toggleDay(action.day)
            is EditRoutineAction.AddZone -> addZone(action.zone)
            is EditRoutineAction.UpdateZone -> updateZone(action.oldZone, action.newZone)
            is EditRoutineAction.DeleteZone -> deleteZone(action.zone)
            EditRoutineAction.SaveRoutine -> saveRoutine()
        }
    }

    private fun loadTemplate(templateId: String?) {
        if (templateId == null) {
            _uiState.update { EditRoutineState() }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, templateId = templateId) }
            when (val result = zonesRepository.getTemplate(templateId)) {
                is Result.Success -> {
                    val template = result.data
                    val sortedZones = template.zones.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
                    _uiState.update { it.copy(
                        isLoading = false,
                        name = template.name,
                        selectedDays = template.daysOfWeek.toSet(),
                        zones = sortedZones
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, validationError = null, error = null) }
    }

    private fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            val newDays = if (state.selectedDays.contains(day)) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(selectedDays = newDays, validationError = null, error = null)
        }
    }

    private fun addZone(zone: DailyZone) {
        _uiState.update { state ->
            val updatedZones = (state.zones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
            state.copy(zones = updatedZones, validationError = null, error = null)
        }
    }

    private fun updateZone(oldZone: DailyZone, newZone: DailyZone) {
        _uiState.update { state ->
            val updatedZones = state.zones.map { if (it == oldZone) newZone else it }
                .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
            state.copy(zones = updatedZones, validationError = null, error = null)
        }
    }

    private fun deleteZone(zone: DailyZone) {
        _uiState.update { it.copy(zones = it.zones - zone, validationError = null, error = null) }
    }

    private fun saveRoutine() {
        val state = _uiState.value

        // Local Validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(validationError = "Routine name cannot be empty") }
            return
        }
        if (state.selectedDays.isEmpty()) {
            _uiState.update { it.copy(validationError = "Select at least one day") }
            return
        }

        // Zone Validation
        state.zones.forEach { zone ->
            if (zone.name.isBlank()) {
                _uiState.update { it.copy(validationError = "Zone name cannot be empty") }
                return
            }
            val startMins = DailyZonesHelper.parseTimeToMinutes(zone.startTime)
            val endMins = DailyZonesHelper.parseTimeToMinutes(zone.endTime)
            
            if (startMins >= endMins) {
                _uiState.update { it.copy(validationError = "Start time must be before end time for ${zone.name}") }
                return
            }
        }

        // Overlap Validation
        if (DailyZonesHelper.hasOverlappingZones(state.zones)) {
            _uiState.update { it.copy(validationError = "Zones cannot overlap") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val templateId = state.templateId
            val result = if (templateId == null) {
                zonesRepository.createTemplate(state.name, state.selectedDays.toList(), state.zones)
            } else {
                // Update Template Name and Days first, then Zones
                // The API for updateTemplate only takes name and days
                val updateRes = zonesRepository.updateTemplate(templateId, state.name, state.selectedDays.toList())
                if (updateRes is Result.Success) {
                    zonesRepository.updateTemplateZones(templateId, state.zones)
                } else {
                    updateRes
                }
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditRoutineEvent.SaveSuccess)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }
}