package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.image.usecase.ReadImageUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.usecase.DeleteProfilePictureUseCase
import com.awan.app.core.domain.profile.usecase.GetProfileUseCase
import com.awan.app.core.domain.profile.usecase.GetUserDataUseCase
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import com.awan.app.core.domain.profile.usecase.SetDarkThemeUseCase
import com.awan.app.core.domain.profile.usecase.SetLocaleUseCase
import com.awan.app.core.domain.profile.usecase.UpdateBirthDateUseCase
import com.awan.app.core.domain.profile.usecase.UpdateProfilePartialUseCase
import com.awan.app.core.domain.profile.usecase.UpdateProfilePictureUseCase
import com.awan.app.core.domain.profile.usecase.UpdateSessionSettingsUseCase
import com.awan.app.core.domain.profile.usecase.UpdateSleepScheduleUseCase
import com.awan.app.core.domain.profile.usecase.UpdateTimezoneUseCase
import com.awan.feature.profile.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val updateSleepScheduleUseCase: UpdateSleepScheduleUseCase,
    private val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase,
    private val updateTimezoneUseCase: UpdateTimezoneUseCase,
    private val updateProfilePartialUseCase: UpdateProfilePartialUseCase,
    private val updateBirthDateUseCase: UpdateBirthDateUseCase,
    private val updateProfilePictureUseCase: UpdateProfilePictureUseCase,
    private val deleteProfilePictureUseCase: DeleteProfilePictureUseCase,
    private val readImage: ReadImageUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val setLocaleUseCase: SetLocaleUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
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
            is ProfileAction.UpdateProfilePicture -> {
                _uiState.update { it.copy(pendingProfilePictureUri = action.uri, fieldError = null) }
            }
            ProfileAction.DeleteProfilePicture -> {
                _uiState.update { it.copy(pendingProfilePictureUri = "delete", fieldError = null) }
            }
            ProfileAction.Logout -> logout()
        }
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
            getUserDataUseCase().collectLatest { userData ->
                _uiState.update {
                    it.copy(
                        useDarkTheme = userData.darkThemeEnabled,
                        language = userData.locale
                    )
                }
            }
        }
    }

    private fun setTheme(useDarkTheme: Boolean) {
        viewModelScope.launch {
            setDarkThemeUseCase(useDarkTheme)
        }
    }

    private fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            setLocaleUseCase(languageCode)
        }
    }

    private fun updateSleepSchedule(wakeupTime: String, sleepTime: String) {
        executeFieldUpdate { updateSleepScheduleUseCase(wakeupTime, sleepTime) }
    }

    private fun updateSessionDuration(duration: Int) {
        val currentBuffer = _uiState.value.profile?.preferences?.bufferBetweenSessions ?: 5
        executeFieldUpdate { updateSessionSettingsUseCase(duration, currentBuffer) }
    }

    private fun updateTimezone(timezone: String) {
        executeFieldUpdate { updateTimezoneUseCase(timezone) }
    }

    private fun updatePersonalInfo(firstName: String, lastName: String, birthDate: String) {
        if (_uiState.value.isUpdatingField) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingField = true, fieldError = null) }

            // 1. Update Profile Info (Name & BirthDate)
            val nameResult = updateProfilePartialUseCase(firstName = firstName, lastName = lastName)
            if (nameResult is Result.Error) {
                _uiState.update { it.copy(isUpdatingField = false, fieldError = nameResult.error.toUiText()) }
                return@launch
            }

            val birthDateResult = updateBirthDateUseCase(birthDate)
            if (birthDateResult is Result.Error) {
                _uiState.update { it.copy(isUpdatingField = false, fieldError = birthDateResult.error.toUiText()) }
                return@launch
            }

            val pendingUri = _uiState.value.pendingProfilePictureUri
            if (pendingUri != null) {
                _uiState.update { it.copy(isUploadingPicture = true) }
                if (pendingUri == "delete") {
                    val deleteResult = deleteProfilePictureUseCase()
                    if (deleteResult is Result.Error) {
                        _uiState.update {
                            it.copy(
                                isUpdatingField = false,
                                isUploadingPicture = false,
                                fieldError = deleteResult.error.toUiText()
                            )
                        }
                        return@launch
                    }
                } else {
                    val imageResult = readImage(pendingUri)
                    if (imageResult is Result.Error) {
                        _uiState.update {
                            it.copy(
                                isUpdatingField = false,
                                isUploadingPicture = false,
                                fieldError = imageResult.error.toUiText()
                            )
                        }
                        return@launch
                    }

                    val imageBytes = (imageResult as Result.Success).data
                    val uploadResult = updateProfilePictureUseCase(imageBytes.bytes, imageBytes.mimeType)
                    if (uploadResult is Result.Error) {
                        _uiState.update {
                            it.copy(
                                isUpdatingField = false,
                                isUploadingPicture = false,
                                fieldError = uploadResult.error.toUiText()
                            )
                        }
                        return@launch
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isUpdatingField = false,
                    isUploadingPicture = false,
                    pendingProfilePictureUri = null,
                    fieldError = null
                )
            }
            _events.send(ProfileEvent.UpdateSuccess)
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
