package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.image.usecase.ReadImageUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.usecase.DeleteProfilePictureUseCase
import com.awan.app.core.domain.profile.usecase.GetProfileUseCase
import com.awan.app.core.domain.profile.usecase.GetUserDataUseCase
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import com.awan.app.core.domain.profile.usecase.SetDarkThemeUseCase
import com.awan.app.core.domain.profile.usecase.SetLocaleUseCase
import com.awan.app.core.domain.profile.usecase.UpdateProfilePartialUseCase
import com.awan.app.core.domain.profile.usecase.UpdateProfilePictureUseCase
import com.awan.app.core.domain.profile.usecase.UpdateSessionSettingsUseCase
import com.awan.app.core.domain.profile.usecase.UpdateSleepScheduleUseCase
import com.awan.app.core.domain.profile.usecase.UpdateTimezoneUseCase
import com.awan.app.core.model.DarkThemeConfig
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.model.sanitizeLastName
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
    private val updateProfilePictureUseCase: UpdateProfilePictureUseCase,
    private val deleteProfilePictureUseCase: DeleteProfilePictureUseCase,
    private val readImage: ReadImageUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val setLocaleUseCase: SetLocaleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getEquippedItemsUseCase: GetEquippedItemsUseCase,
    private val refreshMarketplaceUseCase: RefreshMarketplaceUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
        loadCategories()
        observeProfile()
        observePreferences()
        observeEquippedFrame()
        refreshInventory()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SetTheme -> setTheme(action.config)
            is ProfileAction.SetLanguage -> setLanguage(action.languageCode)
            ProfileAction.Refresh -> loadProfile()
            is ProfileAction.UpdateSleepSchedule -> updateSleepSchedule(action.wakeupTime, action.sleepTime)
            is ProfileAction.UpdateSessionDuration -> updateSessionDuration(action.duration)
            is ProfileAction.UpdateTimezone -> updateTimezone(action.timezone)
            is ProfileAction.UpdatePersonalInfo -> updatePersonalInfo(
                action.firstName,
                action.lastName
            )
            is ProfileAction.UpdateProfilePicture -> {
                _uiState.update { it.copy(pendingPicture = PendingPicture.Picked(action.uri), fieldError = null) }
            }
            ProfileAction.DeleteProfilePicture -> {
                _uiState.update { it.copy(pendingPicture = PendingPicture.Clear, fieldError = null) }
            }
            ProfileAction.DismissEditSheet -> {
                _uiState.update { it.copy(pendingPicture = null, fieldError = null) }
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
                        darkThemeConfig = userData.darkThemeConfig,
                        language = userData.locale
                    )
                }
            }
        }
    }

    private fun observeEquippedFrame() {
        viewModelScope.launch {
            getEquippedItemsUseCase().collectLatest { equippedItems ->
                val frameUrl = equippedItems.find { it.type == StoreItemType.FRAME }?.item?.image
                _uiState.update { it.copy(equippedFrameImageUrl = frameUrl) }
            }
        }
    }

    private fun refreshInventory() {
        viewModelScope.launch {
            try {
                refreshMarketplaceUseCase()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun setTheme(config: DarkThemeConfig) {
        viewModelScope.launch {
            setDarkThemeUseCase(config)
        }
    }

    private fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            setLocaleUseCase(languageCode)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = getCategoriesUseCase()) {
                is Result.Success -> _uiState.update { it.copy(categories = result.data) }
                else -> Unit
            }
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

    private fun updatePersonalInfo(firstName: String, lastName: String) {
        if (_uiState.value.isUpdatingField || _uiState.value.isUploadingPicture) return

        val currentProfile = _uiState.value.profile
        val currentFirst = currentProfile?.firstName?.trim().orEmpty()
        val currentLast = currentProfile?.lastName?.sanitizeLastName().orEmpty()
        val inputFirst = firstName.trim()
        val inputLast = lastName.sanitizeLastName().orEmpty()

        val isNameChanged = inputFirst != currentFirst || inputLast != currentLast
        val pending = _uiState.value.pendingPicture

        if (!isNameChanged && pending == null) {
            viewModelScope.launch {
                _events.send(ProfileEvent.UpdateSuccess)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(fieldError = null) }

            var nameError: AppError? = null
            var pictureError: AppError? = null

            // 1. Update Profile Info (Name) if changed - passing raw inputs to use case
            if (isNameChanged) {
                _uiState.update { it.copy(isUpdatingField = true) }
                when (val nameResult = updateProfilePartialUseCase(firstName = firstName, lastName = lastName)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isUpdatingField = false) }
                    }
                    is Result.Error -> {
                        nameError = nameResult.error
                        _uiState.update { it.copy(isUpdatingField = false) }
                    }
                    Result.Loading -> Unit
                }
            }

            // 2. Update Profile Picture independently if pending
            if (pending != null) {
                _uiState.update { it.copy(isUploadingPicture = true) }
                when (pending) {
                    PendingPicture.Clear -> {
                        when (val deleteResult = deleteProfilePictureUseCase()) {
                            is Result.Success -> {
                                _uiState.update { it.copy(isUploadingPicture = false, pendingPicture = null) }
                            }
                            is Result.Error -> {
                                pictureError = deleteResult.error
                                _uiState.update { it.copy(isUploadingPicture = false) }
                            }
                            Result.Loading -> Unit
                        }
                    }
                    is PendingPicture.Picked -> {
                        when (val imageResult = readImage(pending.uri)) {
                            is Result.Success -> {
                                val imageBytes = imageResult.data
                                when (val uploadResult = updateProfilePictureUseCase(imageBytes.bytes, imageBytes.mimeType)) {
                                    is Result.Success -> {
                                        _uiState.update { it.copy(isUploadingPicture = false, pendingPicture = null) }
                                    }
                                    is Result.Error -> {
                                        pictureError = uploadResult.error
                                        _uiState.update { it.copy(isUploadingPicture = false) }
                                    }
                                    Result.Loading -> Unit
                                }
                            }
                            is Result.Error -> {
                                pictureError = imageResult.error
                                _uiState.update { it.copy(isUploadingPicture = false) }
                            }
                            Result.Loading -> Unit
                        }
                    }
                }
            }

            val error = nameError ?: pictureError
            if (error != null) {
                _uiState.update { it.copy(fieldError = error.toUiText()) }
            } else {
                _uiState.update { it.copy(fieldError = null, pendingPicture = null) }
                _events.send(ProfileEvent.UpdateSuccess)
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
