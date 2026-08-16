package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.category.usecase.CreateCategoryUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.usecase.GetOverridesUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideZonesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.helpers.ProfileErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyZonesViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val getOverridesUseCase: GetOverridesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val updateOverrideZonesUseCase: UpdateOverrideZonesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DailyZonesState(
            selectedDay = DailyZonesHelper.getCurrentDay(LocalDate.now()),
            selectedDate = LocalDate.now()
        )
    )
    val uiState: StateFlow<DailyZonesState> = _uiState.asStateFlow()

    fun onAction(action: DailyZonesAction) {
        when (action) {
            DailyZonesAction.LoadData -> loadData()
            is DailyZonesAction.SelectDay -> selectDay(action.day)
            is DailyZonesAction.SelectTemplate -> selectTemplate(action.templateId)
            is DailyZonesAction.AddZone -> addZone(action.zone)
            is DailyZonesAction.UpdateZone -> updateZone(action.zone)
            is DailyZonesAction.DeleteZone -> deleteZone(action.zone)
            is DailyZonesAction.CreateCategory -> createCategory(action.name)
            DailyZonesAction.ClearError -> clearError()
            DailyZonesAction.NextWeek -> shiftWeek(1)
            DailyZonesAction.PreviousWeek -> shiftWeek(-1)
            DailyZonesAction.GoToToday -> goToToday()
            is DailyZonesAction.DateSelected -> selectDate(action.date)
        }
    }

    private fun shiftWeek(weeks: Int) {
        _uiState.update { state ->
            val newDate = (state.selectedDate ?: LocalDate.now()).plusWeeks(weeks.toLong())
            state.copy(
                selectedDate = newDate,
                selectedDay = DailyZonesHelper.getCurrentDay(newDate)
            )
        }
        updateSelectedDayData()
    }

    private fun goToToday() {
        val today = LocalDate.now()
        _uiState.update { state ->
            state.copy(
                selectedDate = today,
                selectedDay = DailyZonesHelper.getCurrentDay(today)
            )
        }
        updateSelectedDayData()
    }

    private fun selectDate(date: LocalDate) {
        _uiState.update { state ->
            state.copy(
                selectedDate = date,
                selectedDay = DailyZonesHelper.getCurrentDay(date)
            )
        }
        updateSelectedDayData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val templatesDeferred = async { getWeeklyTemplatesUseCase() }
            val overridesDeferred = async { getOverridesUseCase() }
            val categoriesDeferred = async { getCategoriesUseCase() }

            val templatesResult = templatesDeferred.await()
            val overridesResult = overridesDeferred.await()
            val categoriesResult = categoriesDeferred.await()

            if (templatesResult is Result.Success && 
                overridesResult is Result.Success && 
                categoriesResult is Result.Success) {
                
                val templates = templatesResult.data
                val overrides = overridesResult.data
                val categories = categoriesResult.data

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        templates = templates,
                        overrides = overrides,
                        availableCategories = categories
                    )
                }
                updateSelectedDayData()
            } else {
                val error = (templatesResult as? Result.Error)?.error
                    ?: (overridesResult as? Result.Error)?.error
                    ?: (categoriesResult as? Result.Error)?.error

                _uiState.update { it.copy(
                    isLoading = false,
                    error = error?.let { ProfileErrorMapper.mapToUiText(it) }
                ) }
            }
        }
    }

    private fun selectDay(day: DayOfWeek) {
        _uiState.update { state ->
            val reference = state.selectedDate ?: LocalDate.now()
            // Find the date of the given day in the same week as the currently visible date
            val currentWeekStart = reference.minusDays((reference.dayOfWeek.value.toLong() - 1))
            val date = currentWeekStart.plusDays(day.ordinal.toLong())
            
            state.copy(selectedDay = day, selectedDate = date, selectedTemplateId = null)
        }
        updateSelectedDayData()
    }

    private fun selectTemplate(templateId: String) {
        _uiState.update { it.copy(selectedTemplateId = templateId) }
        updateSelectedDayData()
    }

    private fun updateSelectedDayData() {
        val state = _uiState.value
        val selectedDateStr = state.selectedDate?.toString()

        // 1. Check for override for this specific date
        val override = DailyZonesHelper.findOverrideForDate(state.overrides, selectedDateStr)
        
        if (override != null) {
            val zones = override.zones.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
            _uiState.update { it.copy(
                selectedDayZones = zones,
                currentTemplate = null,
                currentOverride = override
            ) }
            return
        }

        // 2. Fallback to template (either explicitly selected or for the day of week)
        val template = state.templates.find { it.id == state.selectedTemplateId }
            ?: state.templates.find { it.daysOfWeek.contains(state.selectedDay) && it.zones.isNotEmpty() }

        val zones = (template?.zones ?: emptyList())
            .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }

        _uiState.update { it.copy(
            selectedDayZones = zones,
            currentTemplate = template,
            currentOverride = null
        ) }
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
        saveZones(newZones)
    }

    private fun saveZones(zones: List<DailyZone>) {
        if (zones.isEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.profile_validation_at_least_one_zone)) }
            return
        }
        val state = _uiState.value
        val templateId = state.currentTemplate?.id
        val overrideId = state.currentOverride?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val result = when {
                templateId != null -> updateTemplateZonesUseCase(templateId, zones)
                overrideId != null -> updateOverrideZonesUseCase(overrideId, zones)
                else -> {
                    _uiState.update { it.copy(isSaving = false, error = UiText.StringResource(R.string.profile_daily_zones_error_generic)) }
                    return@launch
                }
            }

            when (result) {
                is Result.Success -> {
                    val updatedZones = result.data.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
                    _uiState.update { state ->
                        if (templateId != null) {
                            val updatedTemplates = state.templates.map { 
                                if (it.id == templateId) it.copy(zones = result.data) else it
                            }
                            state.copy(
                                isSaving = false,
                                selectedDayZones = updatedZones,
                                templates = updatedTemplates,
                                currentTemplate = updatedTemplates.find { it.id == templateId }
                            )
                        } else {
                            val updatedOverrides = state.overrides.map {
                                if (it.id == overrideId) it.copy(zones = result.data) else it
                            }
                            state.copy(
                                isSaving = false,
                                selectedDayZones = updatedZones,
                                overrides = updatedOverrides,
                                currentOverride = updatedOverrides.find { it.id == overrideId }
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(result.error)) }
                Result.Loading -> Unit
            }
        }
    }

    private fun createCategory(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = createCategoryUseCase(name)) {
                is Result.Success -> {
                    when (val catResult = getCategoriesUseCase()) {
                        is Result.Success -> {
                            _uiState.update { it.copy(isSaving = false, availableCategories = catResult.data) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(catResult.error)) }
                        }
                        Result.Loading -> Unit
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
