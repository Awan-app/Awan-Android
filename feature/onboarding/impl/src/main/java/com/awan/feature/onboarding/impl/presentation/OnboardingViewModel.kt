package com.awan.feature.onboarding.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.onboarding.usecase.AssignDefaultCategoriesUseCase
import com.awan.app.core.domain.onboarding.usecase.SuggestZoneScheduleUseCase
import com.awan.app.core.domain.onboarding.utils.ValidateDayBounds
import com.awan.app.core.domain.onboarding.utils.ZoneEditRules
import com.awan.app.core.domain.task.usecase.CreateAndScheduleFirstTaskUseCase
import com.awan.feature.onboarding.impl.R
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.onboarding.usecase.CompleteOnboardingUseCase
import com.awan.app.core.domain.profile.model.UserProfile
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val suggestZoneSchedule: SuggestZoneScheduleUseCase,
    private val validateDayBounds: ValidateDayBounds,
    private val createAndScheduleFirstTask: CreateAndScheduleFirstTaskUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val assignDefaultCategories: AssignDefaultCategoriesUseCase,
) : ViewModel() {

    private var zonesUserEdited = false
    private var isBackendOnboarded = false

    private val _state = MutableStateFlow(
        OnboardingState(zones = suggestZoneSchedule(DayBounds.Default))
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _events = Channel<OnboardingEvent>()
    val events = _events.receiveAsFlow()

    private var categoriesLoad: Job = loadCategories()

    /**
     * The backend rejects a zone without a category, so the zones step needs the user's own list
     * before it can produce a saveable day. A failure is not fatal: the step still works, the sheet
     * says there are no categories, and the repository skips the template rather than 422-ing.
     */
    private fun loadCategories(): Job =
        viewModelScope.launch {
            val categories = (getCategories() as? Result.Success)?.data ?: return@launch
            _state.update {
                it.copy(
                    availableCategories = categories,
                    zones = assignDefaultCategories(it.zones, categories),
                )
            }
        }

    /**
     * Skipping the setup reaches the submit before the initial load lands, and the repository drops a
     * zone that has no category — the skipping user would silently get no zone template at all. So the
     * hand-off waits for the load, and retries it once if it has not produced anything yet.
     */
    private suspend fun awaitZoneCategories() {
        categoriesLoad.join()
        if (_state.value.availableCategories.isEmpty()) {
            categoriesLoad = loadCategories()
            categoriesLoad.join()
        }
    }

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
            is OnboardingAction.ZoneCategoryPicked -> updateZones {
                map { if (it.id == action.zoneId) it.copy(categoryId = action.categoryId) else it }
            }

            is OnboardingAction.TaskLengthChanged -> _state.update { it.copy(preferredTaskLengthMinutes = action.minutes) }
            is OnboardingAction.FirstTaskTitleChanged -> _state.update { it.copy(firstTaskTitle = action.title) }
            OnboardingAction.SubmitFirstTask -> submitFirstTask()

            OnboardingAction.EnableNotifications ->
                viewModelScope.launch { _events.send(OnboardingEvent.RequestNotificationPermission) }
            // The permission is optional; either answer still has to finish the account setup.
            is OnboardingAction.NotificationPermissionResult -> finishOnboarding()
            OnboardingAction.NotificationsPermanentlyDenied ->
                _state.update { it.copy(notificationsPermanentlyDenied = true) }

            OnboardingAction.Next -> onNext()
            OnboardingAction.Skip -> onSkip()
            OnboardingAction.SkipSetup -> finishOnboarding()
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

    private fun completeOnboardingBeforeTask(onComplete: () -> Unit) =
        submitting { if (submitOnboarding()) onComplete() }

    private fun finishOnboarding() =
        submitting { if (submitOnboarding()) _events.send(OnboardingEvent.NavigateHome) }

    private inline fun submitting(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingTask = true, setupError = null) }
            block()
            _state.update { it.copy(isSubmittingTask = false) }
        }
    }

    /**
     * The one backend hand-off for the whole flow: the profile/day/session settings, then the
     * zone windows as the user's weekly template. The template is gated on onboarding succeeding
     * so an expired session does not fail twice.
     *
     * Each half remembers its own success, so a later exit path retries only what has not landed.
     * Nothing re-enters onboarding after it is left — a swallowed failure here would leave the
     * account permanently half-configured, so a failure blocks the exit instead of navigating on.
     */
    private suspend fun submitOnboarding(): Boolean {
        if (!isBackendOnboarded) {
            awaitZoneCategories()
            val s = _state.value
            val data = OnboardingData(
                profile = UserProfile(s.trimmedFirstName, s.lastName.trim()),
                bounds = s.bounds,
                zones = s.zones,
                preferredTaskLengthMinutes = s.preferredTaskLengthMinutes,
                firstTask = s.firstTask,
            )
            when (val result = completeOnboarding(data)) {
                is Result.Success -> isBackendOnboarded = true
                is Result.Error -> return failSetup(result.error)
                Result.Loading -> return false
            }
        }
        return true
    }

    private fun failSetup(error: AppError): Boolean {
        _state.update { it.copy(setupError = error.toUiText()) }
        return false
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
            _state.update { it.copy(isSubmittingTask = true, firstTaskError = null) }

            when (val result = createAndScheduleFirstTask(current.firstTaskTitle)) {
                is Result.Success -> {
                    val task = result.data
                    _state.update {
                        it.copy(
                            isSubmittingTask = false,
                            firstTask = task,
                            celebrateTask = task != null,
                            firstTaskError = if (task == null) {
                                UiText.StringResource(R.string.onboarding_first_task_unscheduled)
                            } else {
                                null
                            },
                        )
                    }
                }

                is Result.Error -> _state.update {
                    it.copy(isSubmittingTask = false, firstTaskError = result.error.toUiText())
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun updateBounds(newBounds: DayBounds) {
        _state.update {
            val zones = if (zonesUserEdited) {
                it.zones
            } else {
                assignDefaultCategories(suggestZoneSchedule(newBounds), it.availableCategories)
            }
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
            val zones = assignDefaultCategories(suggestZoneSchedule(it.bounds), it.availableCategories)
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
