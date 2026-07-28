package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DailyZone
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
class DayDetailsViewModel @Inject constructor(
    private val zonesRepository: ZonesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailsState())
    val uiState: StateFlow<DayDetailsState> = _uiState.asStateFlow()

    fun onAction(action: DayDetailsAction) {
        when (action) {
            is DayDetailsAction.LoadDayDetails -> loadDayDetails(action.date)
            is DayDetailsAction.CustomizeDay -> customizeDay(action.zones)
            is DayDetailsAction.UpdateOverride -> updateOverride(action.overrideId, action.zones)
            is DayDetailsAction.ResetToWeeklyRoutine -> resetToWeeklyRoutine(action.overrideId)
        }
    }

    private fun loadDayDetails(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, date = date, error = null) }

            // Get effective zones
            val zonesResult = zonesRepository.getEffectiveZones(date)

            // Check if there's an override for this date
            val overridesResult = zonesRepository.getOverrides()
            val override = if (overridesResult is Result.Success) {
                overridesResult.data.find { it.dateOfDay == date }
            } else null

            _uiState.update { state ->
                val sortedZones = if (zonesResult is Result.Success) {
                    zonesResult.data.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
                } else {
                    state.effectiveZones
                }

                state.copy(
                    isLoading = false,
                    effectiveZones = sortedZones,
                    isOverride = override != null,
                    overrideId = override?.id,
                    error = if (zonesResult is Result.Error) DailyZonesHelper.zonesErrorToUiText(zonesResult.error) else null
                )
            }
        }
    }

    private fun customizeDay(zones: List<DailyZone>) {
        val date = _uiState.value.date
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = zonesRepository.createOverride(date, zones)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadDayDetails(date)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun updateOverride(overrideId: String, zones: List<DailyZone>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = zonesRepository.updateOverrideZones(overrideId, zones)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadDayDetails(_uiState.value.date)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun resetToWeeklyRoutine(overrideId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = zonesRepository.deleteOverride(overrideId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    loadDayDetails(_uiState.value.date)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = DailyZonesHelper.zonesErrorToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }
}