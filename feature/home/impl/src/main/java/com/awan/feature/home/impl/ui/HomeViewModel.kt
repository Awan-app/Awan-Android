package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.datastore.auth.AuthTokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedAuthData(
    val email: String? = null,
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class HomeUiState(
    val savedAuthData: SavedAuthData? = null,
    val isDataVisible: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authTokenProvider: AuthTokenProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun toggleShowData() {
        if (_uiState.value.isDataVisible) {
            _uiState.update { it.copy(isDataVisible = false) }
        } else {
            loadSavedAuthData()
        }
    }

    fun loadSavedAuthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = authTokenProvider.getUserEmail()
            val userId = authTokenProvider.getUserId()
            val accessToken = authTokenProvider.getAccessToken()
            val refreshToken = authTokenProvider.getRefreshToken()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isDataVisible = true,
                    savedAuthData = SavedAuthData(
                        email = email,
                        userId = userId,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                    )
                )
            }
        }
    }
}
