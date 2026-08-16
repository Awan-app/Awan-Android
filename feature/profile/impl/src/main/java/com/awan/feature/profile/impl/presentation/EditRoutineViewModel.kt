package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.category.usecase.CreateCategoryUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.usecase.CreateOverrideUseCase
import com.awan.app.core.domain.zones.usecase.CreateWeeklyTemplateUseCase
import com.awan.app.core.domain.zones.usecase.DeleteOverrideUseCase
import com.awan.app.core.domain.zones.usecase.DeleteWeeklyTemplateUseCase
import com.awan.app.core.domain.zones.usecase.GetOverridesUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplateUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplatesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideUseCase
import com.awan.app.core.domain.zones.usecase.UpdateOverrideZonesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateTemplateZonesUseCase
import com.awan.app.core.domain.zones.usecase.UpdateWeeklyTemplateUseCase
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.helpers.ProfileErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EditRoutineViewModel @Inject constructor(
    private val getWeeklyTemplatesUseCase: GetWeeklyTemplatesUseCase,
    private val getWeeklyTemplateUseCase: GetWeeklyTemplateUseCase,
    private val getOverridesUseCase: GetOverridesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val createWeeklyTemplateUseCase: CreateWeeklyTemplateUseCase,
    private val updateWeeklyTemplateUseCase: UpdateWeeklyTemplateUseCase,
    private val updateTemplateZonesUseCase: UpdateTemplateZonesUseCase,
    private val deleteWeeklyTemplateUseCase: DeleteWeeklyTemplateUseCase,
    private val createOverrideUseCase: CreateOverrideUseCase,
    private val updateOverrideUseCase: UpdateOverrideUseCase,
    private val updateOverrideZonesUseCase: UpdateOverrideZonesUseCase,
    private val deleteOverrideUseCase: DeleteOverrideUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRoutineState(isLoading = true))
    val uiState: StateFlow<EditRoutineState> = _uiState.asStateFlow()

    private val _events = Channel<EditRoutineEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: EditRoutineAction) {
        when (action) {
            is EditRoutineAction.LoadTemplate -> loadTemplate(action.templateId, action.overrideId, action.date)
            is EditRoutineAction.NameChange -> onNameChange(action.name)
            is EditRoutineAction.ToggleDay -> toggleDay(action.day)
            is EditRoutineAction.AddZone -> addZone(action.zone)
            is EditRoutineAction.UpdateZone -> updateZone(action.oldZone, action.newZone)
            is EditRoutineAction.DeleteZone -> deleteZone(action.zone)
            is EditRoutineAction.ReorderZones -> reorderZones(action.from, action.to)
            is EditRoutineAction.CreateCategory -> createCategory(action.name)
            is EditRoutineAction.ToggleTodayOnly -> {
                if (_uiState.value.canToggleTodayOnly) {
                    _uiState.update { it.copy(isTodayOnly = action.isTodayOnly, validationError = null) }
                } else {
                    _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_daily_zones_error_no_base_routine)) }
                }
            }
            is EditRoutineAction.DateChange -> onDateChange(action.date)
            EditRoutineAction.SaveRoutine -> saveRoutine()
            EditRoutineAction.DeleteRoutine -> deleteRoutine()
        }
    }

    private fun onDateChange(date: String) {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        val dayOfWeek = DailyZonesHelper.getCurrentDay(parsedDate)
        _uiState.update { 
            it.copy(
                date = date, 
                dates = it.dates + date,
                selectedDays = it.selectedDays + dayOfWeek,
                validationError = null, 
                error = null
            )
        }
    }

    private fun loadTemplate(templateId: String?, overrideId: String?, date: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true, 
                templateId = templateId, 
                overrideId = overrideId, 
                date = date,
                dates = if (date != null) setOf(date) else emptySet()
            ) }

            // 1. Fetch ALL templates, overrides, categories
            val templatesDeferred = async { getWeeklyTemplatesUseCase() }
            val overridesDeferred = async { getOverridesUseCase() }
            val categoriesDeferred = async { getCategoriesUseCase() }

            val templatesResult = templatesDeferred.await()
            val overridesResult = overridesDeferred.await()
            val categoriesResult = categoriesDeferred.await()

            val allTemplates = (templatesResult as? Result.Success)?.data ?: emptyList()
            val allOverrides = (overridesResult as? Result.Success)?.data ?: emptyList()
            val categories = (categoriesResult as? Result.Success)?.data ?: emptyList()
            
            val otherAssigned = allTemplates
                .asSequence()
                .filter { it.id != templateId }
                .flatMap { it.daysOfWeek }
                .toSet()

            val canToggleTodayOnly = if (date != null) {
                runCatching { LocalDate.parse(date) }.getOrNull()?.let {
                    DailyZonesHelper.hasTemplateForDay(allTemplates, DailyZonesHelper.getCurrentDay(it))
                } ?: false
            } else {
                false
            }

            if (templateId == null || (date != null && overrideId == null)) {
                // Check if there's an override for this date or ID
                val override = overrideId?.let { id -> allOverrides.find { it.id == id } }
                    ?: DailyZonesHelper.findOverrideForDate(allOverrides, date)
                
                if (override != null) {
                    // EDIT OVERRIDE MODE
                    val effectiveDate = date ?: override.dateOfDay
                    val sortedZones = override.zones.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
                    _uiState.update { it.copy(
                        isLoading = false,
                        overrideId = override.id,
                        name = override.name ?: "",
                        date = effectiveDate,
                        dates = setOf(effectiveDate),
                        isTodayOnly = true,
                        canToggleTodayOnly = canToggleTodayOnly,
                        selectedDays = setOf(DailyZonesHelper.getCurrentDay(LocalDate.parse(effectiveDate))),
                        assignedDays = otherAssigned,
                        zones = sortedZones,
                        availableCategories = categories
                    ) }
                } else if (templateId != null && date != null) {
                    // CUSTOMIZE FROM TEMPLATE MODE
                    val template = allTemplates.find { it.id == templateId }
                    val sortedZones = template?.zones?.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 } ?: emptyList()
                    _uiState.update { it.copy(
                        isLoading = false,
                        templateId = templateId,
                        overrideId = null,
                        name = template?.name ?: "",
                        date = date,
                        dates = if (date != null) setOf(date) else emptySet(),
                        isTodayOnly = true,
                        canToggleTodayOnly = canToggleTodayOnly,
                        selectedDays = setOf(DailyZonesHelper.getCurrentDay(LocalDate.parse(date))),
                        assignedDays = otherAssigned,
                        zones = sortedZones,
                        availableCategories = categories
                    ) }
                } else {
                    // CREATE MODE
                    val initialSelectedDays = if (date != null) {
                        runCatching { LocalDate.parse(date) }.getOrNull()?.let {
                            setOf(DailyZonesHelper.getCurrentDay(it))
                        } ?: emptySet()
                    } else {
                        emptySet()
                    }

                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            templateId = null,
                            overrideId = null,
                            name = "",
                            isTodayOnly = false, // Must be a template if no base exists
                            canToggleTodayOnly = canToggleTodayOnly,
                            selectedDays = initialSelectedDays,
                            date = date,
                            dates = if (date != null) setOf(date) else emptySet(),
                            assignedDays = otherAssigned,
                            zones = emptyList(),
                            availableCategories = categories
                        ) 
                    }
                }
            } else {
                // EDIT MODE: Load specific template and calculate assigned days excluding this one
                when (val result = getWeeklyTemplateUseCase(templateId)) {
                    is Result.Success -> {
                        val template = result.data
                        val sortedZones = template.zones.sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
                        _uiState.update { it.copy(
                            isLoading = false,
                            name = template.name,
                            selectedDays = template.daysOfWeek.toSet(),
                            assignedDays = otherAssigned,
                            canToggleTodayOnly = false, // When editing a template proper, it's always recurring
                            zones = sortedZones,
                            availableCategories = categories
                        ) }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            assignedDays = otherAssigned,
                            error = ProfileErrorMapper.mapToUiText(result.error),
                            availableCategories = categories
                        ) }
                    }
                    Result.Loading -> Unit
                }
            }
        }
    }

    private fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, validationError = null, error = null) }
    }

    private fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            if (state.assignedDays.contains(day)) return@update state
            val newDays = if (state.selectedDays.contains(day)) {
                if (state.isTodayOnly && state.selectedDays.size <= 1) return@update state
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }

            var newDates = state.dates
            if (state.isTodayOnly && state.date != null) {
                val reference = LocalDate.parse(state.date)
                val currentWeekStart = reference.minusDays((reference.dayOfWeek.value.toLong() - 1))
                val targetDate = currentWeekStart.plusDays(day.ordinal.toLong()).toString()
                
                newDates = if (state.selectedDays.contains(day)) {
                    state.dates - targetDate
                } else {
                    state.dates + targetDate
                }
            }

            state.copy(selectedDays = newDays, dates = newDates, validationError = null, error = null)
        }
    }

    private fun addZone(zone: DailyZone) {
        _uiState.update { state ->
            val updatedZones = (state.zones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
            state.copy(zones = updatedZones, validationError = null, error = null)
        }
    }

    private fun updateZone(oldZone: DailyZone, newZone: DailyZone) {
        _uiState.update { state ->
            val updatedZones = state.zones.map { if (it == oldZone) newZone else it }
                .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) ?: 0 }
            state.copy(zones = updatedZones, validationError = null, error = null)
        }
    }

    private fun deleteZone(zone: DailyZone) {
        _uiState.update { it.copy(zones = it.zones - zone, validationError = null, error = null) }
    }

    private fun reorderZones(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.zones.toMutableList()
            if ((from !in list.indices) || (to !in list.indices)) return@update state
            
            val item = list.removeAt(from)
            list.add(to, item)
            
            // Recalculate times to stay sequential
            val updated = mutableListOf<DailyZone>()
            list.forEachIndexed { index, zone ->
                val startMins = DailyZonesHelper.parseTimeToMinutes(zone.startTime) ?: 540
                val endMins = DailyZonesHelper.parseTimeToMinutes(zone.endTime) ?: 600
                
                // Ensure duration is positive and at least 15 minutes to avoid negative/zero times
                val duration = (endMins - startMins).coerceAtLeast(15)
                
                val newStart = if (index == 0) "09:00:00" else updated[index - 1].endTime
                val newStartMins = DailyZonesHelper.parseTimeToMinutes(newStart) ?: 540
                
                // Ensure total minutes don't exceed a day
                val newEndMins = newStartMins + duration
                if (newEndMins > 1439) return@update state
                val newEnd = DailyZonesHelper.formatMinutesToTime(newEndMins)

                updated.add(zone.copy(startTime = newStart, endTime = newEnd))
            }
            state.copy(zones = updated)
        }
    }

    private fun saveRoutine() {
        val state = _uiState.value
        
        if (state.name.isBlank()) {
            _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_validation_routine_name_empty)) }
            return
        }
        if (state.selectedDays.isEmpty()) {
            _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_validation_select_day)) }
            return
        }
        
        if (state.zones.isEmpty()) {
            _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_validation_at_least_one_zone)) }
            return
        }
        
        state.zones.forEach { zone ->
            if (zone.name.isBlank()) {
                _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_validation_zone_name_empty)) }
                return
            }
            val startMins = DailyZonesHelper.parseTimeToMinutes(zone.startTime)
            val endMins = DailyZonesHelper.parseTimeToMinutes(zone.endTime)
            
            if (startMins == null || endMins == null || startMins >= endMins) {
                _uiState.update { it.copy(
                    validationError = UiText.StringResource(
                        R.string.profile_validation_time_order,
                        zone.name
                    )
                ) }
                return
            }
        }

        if (DailyZonesHelper.hasOverlappingZones(state.zones)) {
            _uiState.update { it.copy(validationError = UiText.StringResource(R.string.profile_daily_zones_error_overlap)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val stateNow = _uiState.value
            val templateId = stateNow.templateId
            val overrideId = stateNow.overrideId

            val result = if (stateNow.isTodayOnly && stateNow.dates.isNotEmpty()) {
                if (overrideId != null && stateNow.dates.size == 1) {
                    // 1. Update existing override
                    val updateNameRes = updateOverrideUseCase(overrideId, stateNow.name, stateNow.dates.first())
                    if (updateNameRes is Result.Error) {
                        _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(updateNameRes.error)) }
                        return@launch
                    }
                    updateOverrideZonesUseCase(overrideId, stateNow.zones).map { Unit }
                } else {
                    // 2. Create new overrides for all selected dates
                    var lastResult: Result<Unit> = Result.Success(Unit)
                    stateNow.dates.forEach { dateStr ->
                        val res = createOverrideUseCase(dateStr, stateNow.zones, stateNow.name).map { Unit }
                        if (res is Result.Error) lastResult = res
                    }
                    lastResult
                }
            } else {
                if (templateId != null) {
                    // 3. Update existing weekly template
                    val updateNameRes = updateWeeklyTemplateUseCase(templateId, stateNow.name, stateNow.selectedDays.toList())
                    if (updateNameRes is Result.Error) {
                        _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(updateNameRes.error)) }
                        return@launch
                    }
                    updateTemplateZonesUseCase(templateId, stateNow.zones).map { Unit }
                } else {
                    // 4. Create new weekly template
                    createWeeklyTemplateUseCase(stateNow.name, stateNow.selectedDays.toList(), stateNow.zones).map { Unit }
                }
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditRoutineEvent.SaveSuccess)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(result.error)) }
                }
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

    private fun deleteRoutine() {
        val templateId = _uiState.value.templateId
        val overrideId = _uiState.value.overrideId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            val result = when {
                templateId != null -> deleteWeeklyTemplateUseCase(templateId)
                overrideId != null -> deleteOverrideUseCase(overrideId)
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditRoutineEvent.DeleteSuccess)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = ProfileErrorMapper.mapToUiText(result.error)) }
                }
                Result.Loading -> Unit
            }
        }
    }
}
