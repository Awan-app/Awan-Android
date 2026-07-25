package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfilePartialUseCase: UpdateProfilePartialUseCase,
    private val updateBirthDateUseCase: UpdateBirthDateUseCase,
    private val updateSleepScheduleUseCase: UpdateSleepScheduleUseCase,
    private val updateTimezoneUseCase: UpdateTimezoneUseCase,
    private val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase,
    private val updateSchedulingTypeUseCase: UpdateSchedulingTypeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileState())
    val uiState: StateFlow<EditProfileState> = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>()
    val events = _events.receiveAsFlow()

    private val _profileUpdated = MutableSharedFlow<Profile>()
    val profileUpdated: SharedFlow<Profile> = _profileUpdated.asSharedFlow()

    private var originalProfile: Profile? = null
    private var saveJob: Job? = null

    init {
        loadProfile()
    }

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.FirstNameChange -> onFirstNameChange(action.name)
            is EditProfileAction.LastNameChange -> onLastNameChange(action.name)
            is EditProfileAction.BirthDateChange -> onBirthDateChange(action.date)
            is EditProfileAction.UpdateSleepSchedule -> updateSleepSchedule(action.wakeup, action.sleep)
            is EditProfileAction.UpdateTimezone -> updateTimezone(action.timezone)
            is EditProfileAction.UpdateSessionDuration -> updateSessionDuration(action.duration)
            is EditProfileAction.UpdateSchedulingType -> updateSchedulingType(action.type)
            EditProfileAction.Save -> saveProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProfileUseCase()) {
                is Result.Success -> {
                    originalProfile = result.data
                    updateStateFromProfile(result.data)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toUiText()
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun updateStateFromProfile(profile: Profile) {
        _uiState.update {
            it.copy(
                firstName = profile.firstName,
                lastName = profile.lastName,
                birthDate = profile.birthDate ?: "",
                wakeupTime = profile.preferences?.wakeupTime ?: "07:30:00",
                sleepTime = profile.preferences?.sleepTime ?: "23:00:00",
                timezone = profile.preferences?.timezone ?: "UTC",
                preferredSessionDuration = profile.preferences?.preferredSessionDuration ?: 60,
                schedulingType = profile.preferences?.schedulingType ?: "SMART"
            )
        }
    }

    private fun onFirstNameChange(name: String) {
        _uiState.update { it.copy(firstName = name) }
    }

    private fun onLastNameChange(name: String) {
        _uiState.update { it.copy(lastName = name) }
    }

    private fun onBirthDateChange(date: String) {
        _uiState.update { it.copy(birthDate = date) }
    }

    private fun updateSleepSchedule(wakeup: String, sleep: String) {
        executeFieldUpdate { updateSleepScheduleUseCase(wakeup, sleep) }
    }

    private fun updateTimezone(timezone: String) {
        _uiState.update { it.copy(timezone = timezone) }
        executeFieldUpdate { updateTimezoneUseCase(timezone) }
    }

    private fun updateSessionDuration(duration: Int) {
        _uiState.update { it.copy(preferredSessionDuration = duration) }
        executeFieldUpdate { updateSessionSettingsUseCase(duration, originalProfile?.preferences?.bufferBetweenSessions ?: 5) }
    }

    private fun updateSchedulingType(type: String) {
        _uiState.update { it.copy(schedulingType = type) }
        executeFieldUpdate { updateSchedulingTypeUseCase(type) }
    }

    private fun executeFieldUpdate(block: suspend () -> Result<Profile>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingField = true, fieldErrorMessage = null) }
            when (val result = block()) {
                is Result.Success -> {
                    originalProfile = result.data
                    updateStateFromProfile(result.data)
                    _uiState.update { it.copy(isUpdatingField = false) }
                    _profileUpdated.emit(result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingField = false,
                            fieldErrorMessage = result.error.toUiText()
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun saveProfile() {
        if (_uiState.value.isSaving) return

        val currentState = _uiState.value
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            
            val nameChanged = currentState.firstName != originalProfile?.firstName || 
                            currentState.lastName != originalProfile?.lastName
            val birthDateChanged = currentState.birthDate != originalProfile?.birthDate

            var success = true
            var lastUpdatedProfile: Profile? = originalProfile
            var error: UiText? = null

            if (nameChanged) {
                val result = updateProfilePartialUseCase(
                    firstName = currentState.firstName,
                    lastName = currentState.lastName
                )
                when (result) {
                    is Result.Success -> {
                        lastUpdatedProfile = result.data
                    }
                    is Result.Error -> {
                        success = false
                        error = result.error.toUiText()
                    }
                    Result.Loading -> Unit
                }
            }

            if (success && birthDateChanged && currentState.birthDate.isNotBlank()) {
                val result = updateBirthDateUseCase(currentState.birthDate)
                when (result) {
                    is Result.Success -> {
                        lastUpdatedProfile = result.data
                    }
                    is Result.Error -> {
                        success = false
                        error = result.error.toUiText()
                    }
                    Result.Loading -> Unit
                }
            }

            if (success) {
                lastUpdatedProfile?.let { updated ->
                    originalProfile = updated
                    _profileUpdated.emit(updated)
                }
                _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                _events.send(EditProfileEvent.SaveSuccess)
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error
                    )
                }
            }
        }
    }
}
