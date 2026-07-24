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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val wakeupTime: String = "07:30:00",
    val sleepTime: String = "23:00:00",
    val timezone: String = "UTC",
    val preferredSessionDuration: Int = 60,
    val schedulingType: String = "SMART",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUpdatingField: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: UiText? = null,
    val fieldErrorMessage: UiText? = null,
)

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

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _profileUpdated = MutableSharedFlow<Profile>()
    val profileUpdated: SharedFlow<Profile> = _profileUpdated.asSharedFlow()

    private var originalProfile: Profile? = null
    private var saveJob: Job? = null

    init {
        loadProfile()
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

    fun onFirstNameChange(name: String) {
        _uiState.update { it.copy(firstName = name) }
    }

    fun onLastNameChange(name: String) {
        _uiState.update { it.copy(lastName = name) }
    }

    fun onBirthDateChange(date: String) {
        _uiState.update { it.copy(birthDate = date) }
    }

    fun updateSleepSchedule(wakeup: String, sleep: String) {
        executeFieldUpdate { updateSleepScheduleUseCase(wakeup, sleep) }
    }

    fun updateTimezone(timezone: String) {
        _uiState.update { it.copy(timezone = timezone) }
        executeFieldUpdate { updateTimezoneUseCase(timezone) }
    }

    fun updateSessionDuration(duration: Int) {
        _uiState.update { it.copy(preferredSessionDuration = duration) }
        executeFieldUpdate { updateSessionSettingsUseCase(duration, originalProfile?.preferences?.bufferBetweenSessions ?: 5) }
    }

    fun updateSchedulingType(type: String) {
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

    fun saveProfile() {
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
