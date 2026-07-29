package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.feature.profile.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val logoutUseCase: LogoutUseCase,
    private val userDataRepository: UserPreferencesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
        observeProfile()
        observePreferences()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SetTheme -> setTheme(action.useDarkTheme)
            is ProfileAction.SetLanguage -> setLanguage(action.languageCode)
            ProfileAction.Refresh -> loadProfile()
            is ProfileAction.UpdateSleepSchedule -> updateSleepSchedule(action.wakeupTime, action.sleepTime)
            is ProfileAction.UpdateSessionDuration -> updateSessionDuration(action.duration)
            is ProfileAction.UpdateTimezone -> updateTimezone(action.timezone)
            is ProfileAction.UpdatePersonalInfo -> updatePersonalInfo(
                action.firstName,
                action.lastName,
                action.birthDate
            )
            ProfileAction.Logout -> logout()
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile().collectLatest { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(profile = profile) }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userDataRepository.userPreferences.collectLatest { prefs ->
                _uiState.update {
                    it.copy(
                        useDarkTheme = prefs.darkThemeEnabled,
                        language = prefs.locale
                    )
                }
            }
        }
    }

    private fun setTheme(useDarkTheme: Boolean) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeEnabled(useDarkTheme)
        }
    }

    private fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            userDataRepository.setLocale(languageCode)
        }
    }

    private fun updateSleepSchedule(wakeupTime: String, sleepTime: String) {
        executeFieldUpdate { profileRepository.updateSleepSchedule(wakeupTime, sleepTime) }
    }

    private fun updateSessionDuration(duration: Int) {
        val currentBuffer = _uiState.value.profile?.preferences?.bufferBetweenSessions ?: 5
        executeFieldUpdate { profileRepository.updateSessionSettings(duration, currentBuffer) }
    }

    private fun updateTimezone(timezone: String) {
        executeFieldUpdate { profileRepository.updateTimezone(timezone) }
    }

    private fun updatePersonalInfo(firstName: String, lastName: String, birthDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingField = true, fieldError = null) }

            val nameResult = profileRepository.updateProfilePartial(firstName = firstName, lastName = lastName)
            if (nameResult is Result.Error) {
                _uiState.update { it.copy(isUpdatingField = false, fieldError = nameResult.error.toUiText()) }
                return@launch
            }

            val birthDateResult = profileRepository.updateBirthDate(birthDate)
            if (birthDateResult is Result.Error) {
                _uiState.update { it.copy(isUpdatingField = false, fieldError = birthDateResult.error.toUiText()) }
                return@launch
            }

            if (nameResult is Result.Success && birthDateResult is Result.Success) {
                _uiState.update {
                    it.copy(
                        isUpdatingField = false,
                        profile = birthDateResult.data,
                        fieldError = null
                    )
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (logoutUseCase()) {
                is Result.Success -> _events.send(ProfileEvent.LogoutSuccess)
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.profile_error_logout)
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun executeFieldUpdate(block: suspend () -> Result<Profile>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingField = true, fieldError = null) }
            when (val result = block()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingField = false,
                            profile = result.data,
                            fieldError = null,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingField = false,
                            fieldError = result.error.toUiText(),
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = profileRepository.getProfile()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = result.data,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toUiText(),
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}
