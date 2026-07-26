package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val useDarkTheme: Boolean = false,
    val language: String = "en",
    val isUpdatingField: Boolean = false,
    val fieldError: UiText? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val updateSleepScheduleUseCase: UpdateSleepScheduleUseCase,
    private val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase,
    private val updateTimezoneUseCase: UpdateTimezoneUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userDataRepository: UserPreferencesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeProfile()
        observePreferences()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            observeProfileUseCase().collectLatest { profile ->
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

    fun setTheme(useDarkTheme: Boolean) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeEnabled(useDarkTheme)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            userDataRepository.setLocale(languageCode)
        }
    }

    fun refresh() {
        loadProfile()
    }

    fun updateSleepSchedule(wakeupTime: String, sleepTime: String) {
        executeFieldUpdate { updateSleepScheduleUseCase(wakeupTime, sleepTime) }
    }

    fun updateSessionDuration(duration: Int) {
        val currentBuffer = _uiState.value.profile?.preferences?.bufferBetweenSessions ?: 5
        executeFieldUpdate { updateSessionSettingsUseCase(duration, currentBuffer) }
    }

    fun updateTimezone(timezone: String) {
        executeFieldUpdate { updateTimezoneUseCase(timezone) }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (logoutUseCase()) {
                is Result.Success -> onSuccess()
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(com.awan.feature.profile.impl.R.string.profile_error_logout)
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
            when (val result = getProfileUseCase()) {
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
