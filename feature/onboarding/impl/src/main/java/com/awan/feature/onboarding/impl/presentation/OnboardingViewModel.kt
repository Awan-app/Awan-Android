package com.awan.feature.onboarding.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.data.onboarding.OnboardingRepository
import com.awan.app.core.domain.onboarding.ScheduleFirstTaskUseCase
import com.awan.app.core.domain.onboarding.SuggestZoneScheduleUseCase
import com.awan.app.core.domain.onboarding.ValidateDayBounds
import com.awan.app.core.domain.onboarding.ZoneEditRules
import com.awan.app.core.domain.template.usecase.CreateWeeklyTemplateUseCase
import com.awan.app.core.data.task.CreateTaskUseCase
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.UserProfile
import com.awan.app.core.model.Zone
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
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    private val suggestZoneSchedule: SuggestZoneScheduleUseCase,
    private val scheduleFirstTask: ScheduleFirstTaskUseCase,
    private val validateDayBounds: ValidateDayBounds,
    private val createTaskUseCase: CreateTaskUseCase,
    private val createWeeklyTemplate: CreateWeeklyTemplateUseCase,
) : ViewModel() {

    private var zonesUserEdited = false
    private var isBackendOnboarded = false

    private val _state = MutableStateFlow(
        OnboardingState(zones = suggestZoneSchedule(DayBounds.Default))
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _events = Channel<OnboardingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.NameChanged ->
                _state.update { it.copy(firstName = action.firstName, lastName = action.lastName) }

            is OnboardingAction.WakeChanged -> updateBounds(_state.value.bounds.copy(wakeMinutes = normalize(action.minutes)))
            is OnboardingAction.SleepChanged -> updateBounds(_state.value.bounds.copy(sleepMinutes = normalize(action.minutes)))
            OnboardingAction.DismissWakingWarning -> _state.update { it.copy(wakingWarningDismissed = true) }

            OnboardingAction.UseSuggestedZones -> applySuggestedZones()
            is OnboardingAction.EditZoneWindow -> updateZones {
                map { zone ->
                    if (zone.id == action.zoneId) {
                        ZoneEditRules.editWindow(zone, action.startMinutes, action.endMinutes, _state.value.bounds)
                    } else {
                        zone
                    }
                }
            }
            is OnboardingAction.ReorderZone -> updateZones {
                ZoneEditRules.resequence(reordered(action.fromIndex, action.toIndex), _state.value.bounds)
            }
            is OnboardingAction.ToggleZoneEnabled -> updateZones {
                map { if (it.id == action.zoneId) it.copy(isEnabled = !it.isEnabled) else it }
            }

            is OnboardingAction.TaskLengthChanged -> _state.update { it.copy(preferredTaskLengthMinutes = action.minutes) }
            is OnboardingAction.FirstTaskTitleChanged -> _state.update { it.copy(firstTaskTitle = action.title) }
            OnboardingAction.SubmitFirstTask -> submitFirstTask()

            OnboardingAction.EnableNotifications ->
                viewModelScope.launch { _events.send(OnboardingEvent.RequestNotificationPermission) }
            is OnboardingAction.NotificationPermissionResult -> if (action.granted) finishOnboarding() else Unit
            OnboardingAction.NotificationsPermanentlyDenied ->
                _state.update { it.copy(notificationsPermanentlyDenied = true) }

            OnboardingAction.Next -> onNext()
            OnboardingAction.Skip -> onSkip()
            OnboardingAction.SkipSetup -> skipSetup()
            OnboardingAction.Back -> onBack()
        }
    }

    private fun onNext() {
        val step = _state.value.step
        when (step) {
            OnboardingStep.TaskLength -> completeOnboardingBeforeTask { goToStep(OnboardingStep.FirstTask) }
            OnboardingStep.Notifications -> finishOnboarding()
            else -> goToStep(nextStep(step))
        }
    }

    private fun onSkip() {
        when (_state.value.step) {
            OnboardingStep.DayBounds -> { zonesUserEdited = false; updateBounds(DayBounds.Default) }
            OnboardingStep.Zones -> applySuggestedZones()
            OnboardingStep.TaskLength -> {
                _state.update { it.copy(preferredTaskLengthMinutes = OnboardingData.DEFAULT_TASK_LENGTH_MINUTES) }
                completeOnboardingBeforeTask { goToStep(OnboardingStep.FirstTask) }
                return
            }
            OnboardingStep.Notifications -> { finishOnboarding(); return }
            else -> Unit
        }
        goToStep(nextStep(_state.value.step))
    }

    private fun completeOnboardingBeforeTask(onComplete: () -> Unit) {
        if (isBackendOnboarded) {
            onComplete()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingTask = true) }
            submitOnboarding()
            _state.update { it.copy(isSubmittingTask = false) }
            onComplete()
        }
    }

    private fun skipSetup() {
        viewModelScope.launch {
            submitOnboarding()
            _events.send(OnboardingEvent.NavigateHome)
        }
    }

    private fun finishOnboarding() {
        viewModelScope.launch {
            if (!isBackendOnboarded) submitOnboarding()
            _events.send(OnboardingEvent.NavigateHome)
        }
    }

    /**
     * The one backend hand-off for the whole flow: the profile/day/session settings, then the
     * zone windows as the user's weekly template. The template is gated on onboarding succeeding
     * so an expired session does not fail twice.
     */
    private suspend fun submitOnboarding() {
        val s = _state.value
        val data = OnboardingData(
            profile = UserProfile(s.trimmedFirstName, s.lastName.trim()),
            bounds = s.bounds,
            zones = s.zones,
            preferredTaskLengthMinutes = s.preferredTaskLengthMinutes,
            firstTask = s.firstTask,
        )
        if (repository.completeOnboarding(data) is Result.Success) {
            createWeeklyTemplate(s.zones)
        }
        isBackendOnboarded = true
    }

    private fun onBack() {
        val step = _state.value.step
        if (step == OnboardingStep.Welcome) {
            viewModelScope.launch { _events.send(OnboardingEvent.ExitFlow) }
        } else {
            goToStep(OnboardingStep.entries[step.ordinal - 1])
        }
    }

    private fun submitFirstTask() {
        val current = _state.value
        if (!current.canSubmitFirstTask) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingTask = true) }
            val task = scheduleFirstTask(current.firstTaskTitle, current.zones, current.preferredTaskLengthMinutes)

            // Send task creation request to backend
            createTaskUseCase(
                title = task.title,
                estimatedDurationMinutes = task.durationMinutes,
            )

            _state.update { it.copy(isSubmittingTask = false, firstTask = task, celebrateTask = true) }
        }
    }

    private fun updateBounds(newBounds: DayBounds) {
        _state.update {
            val zones = if (zonesUserEdited) it.zones else suggestZoneSchedule(newBounds)
            it.copy(
                bounds = newBounds,
                boundsValidation = validateDayBounds(newBounds),
                wakingWarningDismissed = false,
                zones = zones,
                overlappingZoneIds = ZoneEditRules.overlappingZoneIds(zones, newBounds),
            )
        }
    }

    private fun applySuggestedZones() {
        zonesUserEdited = false
        _state.update {
            val zones = suggestZoneSchedule(it.bounds)
            it.copy(zones = zones, overlappingZoneIds = ZoneEditRules.overlappingZoneIds(zones, it.bounds))
        }
    }

    private inline fun updateZones(transform: List<Zone>.() -> List<Zone>) {
        if (_state.value.step == OnboardingStep.Zones) zonesUserEdited = true
        _state.update {
            val zones = it.zones.transform()
            it.copy(zones = zones, overlappingZoneIds = ZoneEditRules.overlappingZoneIds(zones, it.bounds))
        }
    }

    private fun goToStep(step: OnboardingStep) = _state.update { it.copy(step = step) }

    private fun nextStep(step: OnboardingStep): OnboardingStep =
        OnboardingStep.entries.getOrElse(step.ordinal + 1) { step }

    private fun List<Zone>.reordered(fromIndex: Int, toIndex: Int): List<Zone> {
        if (fromIndex !in indices || toIndex !in indices) return this
        return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    private fun normalize(minutes: Int): Int = ZoneEditRules.snapToFive(minutes).mod(DayBounds.MINUTES_PER_DAY)
}
