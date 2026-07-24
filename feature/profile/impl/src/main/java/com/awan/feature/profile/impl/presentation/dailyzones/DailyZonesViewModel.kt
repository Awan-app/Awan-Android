package com.awan.feature.profile.impl.presentation.dailyzones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.usecase.GetProfileUseCase
import com.awan.app.core.domain.zones.usecase.*
import com.awan.app.core.model.DailyZone
import com.awan.app.core.model.DayOfWeek
import com.awan.app.core.model.WeeklyTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyZonesUiState(
    val templates: List<WeeklyTemplate> = emptyList(),
    val effectiveZones: List<DailyZone> = emptyList(),
    val profile: Profile? = null,
    val selectedDate: Date = Date(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isOperationLoading: Boolean = false,
    val operationError: UiText? = null
)

@HiltViewModel
class DailyZonesViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val getEffectiveZonesUseCase: GetEffectiveZonesUseCase,
    private val createWeeklyTemplateUseCase: CreateWeeklyTemplateUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val deleteWeeklyTemplateUseCase: DeleteWeeklyTemplateUseCase,
    private val createTemplateOverrideUseCase: CreateTemplateOverrideUseCase,
    private val updateOverrideZonesUseCase: UpdateOverrideZonesUseCase,
    private val deleteTemplateOverrideUseCase: DeleteTemplateOverrideUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyZonesUiState())
    val uiState: StateFlow<DailyZonesUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val profileResult = getProfileUseCase()
            if (profileResult is Result.Success) {
                _uiState.update { it.copy(profile = profileResult.data) }
            }

            loadTemplatesAndEffectiveZones()
        }
    }

    private suspend fun loadTemplatesAndEffectiveZones() {
        val templatesResult = getWeeklyTemplatesUseCase()
        val date = dateFormatter.format(_uiState.value.selectedDate)
        val zonesResult = getEffectiveZonesUseCase(date)

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                templates = if (templatesResult is Result.Success) templatesResult.data else state.templates,
                effectiveZones = if (zonesResult is Result.Success) zonesResult.data else state.effectiveZones,
                error = if (templatesResult is Result.Error) templatesResult.error.toUiText() 
                        else if (zonesResult is Result.Error) zonesResult.error.toUiText() 
                        else null
            )
        }
    }

    fun onDateSelected(date: Date) {
        _uiState.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val zonesResult = getEffectiveZonesUseCase(dateFormatter.format(date))
            _uiState.update { it.copy(
                isLoading = false,
                effectiveZones = if (zonesResult is Result.Success) zonesResult.data else it.effectiveZones,
                error = if (zonesResult is Result.Error) zonesResult.error.toUiText() else null
            ) }
        }
    }

    fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>) {
        executeOperation { createWeeklyTemplateUseCase(name, daysOfWeek, zones) }
    }

    fun updateTemplateZones(templateId: String, zones: List<DailyZone>) {
        executeOperation { updateTemplateZonesUseCase(templateId, zones) }
    }

    fun deleteTemplate(templateId: String) {
        executeOperation { deleteWeeklyTemplateUseCase(templateId) }
    }

    fun createOverride(date: Date, zones: List<DailyZone>) {
        executeOperation { createTemplateOverrideUseCase(dateFormatter.format(date), zones) }
    }

    fun updateOverride(overrideId: String, zones: List<DailyZone>) {
        executeOperation { updateOverrideZonesUseCase(overrideId, zones) }
    }

    fun deleteOverride(overrideId: String) {
        executeOperation { deleteTemplateOverrideUseCase(overrideId) }
    }

    private fun executeOperation(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationLoading = true, operationError = null) }
            when (val result = block()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isOperationLoading = false) }
                    loadTemplatesAndEffectiveZones()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isOperationLoading = false, operationError = result.error.toUiText()) }
                }
                Result.Loading -> Unit
            }
        }
    }
}
