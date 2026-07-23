package com.awan.feature.home.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.usecase.GetUserUseCase
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.auth.usecase.RefreshUserDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val isDataVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val refreshUserDataUseCase: RefreshUserDataUseCase,
    private val logoutUseCase: LogoutUseCase,
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
            val user = getUserUseCase()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isDataVisible = true,
                    user = user,
                )
            }
        }
    }

    fun refreshUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = refreshUserDataUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to refresh user data",
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (logoutUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to logout",
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}
