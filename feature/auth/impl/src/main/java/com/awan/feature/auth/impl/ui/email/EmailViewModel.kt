package com.awan.feature.auth.impl.ui.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.feature.auth.impl.domain.usecase.RequestOtpUseCase
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
class EmailViewModel @Inject constructor(
    private val requestOtpUseCase: RequestOtpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailUiState())
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                isEmailValid = EMAIL_REGEX.matches(email),
                errorMessage = null, // clear inline error on new input
            )
        }
    }

    fun onSendCode() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = requestOtpUseCase(state.email)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(EmailEvent.NavigateToOtp(state.email))
                }
                is Result.Error -> {
                    val error = result.error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRateLimited = error is AppError.Api &&
                                error.code == HTTP_TOO_MANY_REQUESTS,
                            rateLimitSecondsRemaining = if (
                                error is AppError.Api &&
                                error.code == HTTP_TOO_MANY_REQUESTS
                            ) RATE_LIMIT_COOLDOWN_SECONDS else 0,
                            isOffline = error is AppError.Network,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun AppError.toUserMessage(): String? = when (this) {
        AppError.Network -> null // shown via isOffline banner
        AppError.Timeout -> "Request timed out. Check your connection."
        AppError.Unauthorized -> "Request not authorized. Please contact support."
        is AppError.Server -> "Server error. Please try again later."
        is AppError.Api -> when (code) {
            HTTP_TOO_MANY_REQUESTS -> null // shown via isRateLimited banner
            HTTP_NOT_FOUND -> "This email isn't registered."
            else -> body ?: "Something went wrong."
        }
        AppError.Serialization -> "Unexpected response from server."
        is AppError.Unknown -> "Something went wrong."
    }

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_NOT_FOUND = 404
        const val RATE_LIMIT_COOLDOWN_SECONDS = 60

        val EMAIL_REGEX = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
        )
    }
}

sealed interface EmailEvent {
    data class NavigateToOtp(val email: String) : EmailEvent
}
