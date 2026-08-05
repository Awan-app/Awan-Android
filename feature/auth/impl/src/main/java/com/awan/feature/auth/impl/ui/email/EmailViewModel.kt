package com.awan.feature.auth.impl.ui.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.auth.usecase.GetLastUsedEmailUseCase
import com.awan.app.core.domain.auth.usecase.RequestOtpUseCase
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
    private val getLastUsedEmailUseCase: GetLastUsedEmailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailUiState())
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var rateLimitedEmail: String? = null

    init {
        viewModelScope.launch {
            // Pre-fill is a convenience, never a reason to take the login screen down: reading it
            // hits EncryptedSharedPreferences, which throws once the Keystore key is invalidated.
            @Suppress("TooGenericExceptionCaught")
            val lastEmail = try {
                getLastUsedEmailUseCase()
            } catch (e: Exception) {
                null
            }
            if (!lastEmail.isNullOrBlank() && _uiState.value.email.isEmpty()) {
                onEmailChanged(lastEmail)
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { current ->
            val isValid = EMAIL_REGEX.matches(email)
            val isRateLimited = if (rateLimitedEmail != null && email == rateLimitedEmail) {
                current.isRateLimited
            } else {
                false
            }

            current.copy(
                email = email,
                isEmailValid = isValid,
                isRateLimited = isRateLimited,
                errorMessage = if (isRateLimited) current.errorMessage else null,
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
                    rateLimitedEmail = null
                    _uiState.update { it.copy(isLoading = false, isRateLimited = false) }
                    _events.send(EmailEvent.NavigateToOtp(state.email))
                }
                is Result.Error -> {
                    val error = result.error
                    val isRateLimited = error is AppError.Api &&
                        (error.code == HTTP_TOO_MANY_REQUESTS || error.errorCode == "OTP_RATE_LIMIT_EXCEEDED")
                    val retrySeconds = if (isRateLimited) {
                        error.retryAfterSeconds ?: RATE_LIMIT_COOLDOWN_SECONDS
                    } else 0

                    if (isRateLimited) {
                        rateLimitedEmail = state.email
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRateLimited = isRateLimited,
                            rateLimitSecondsRemaining = retrySeconds,
                            isOffline = error is AppError.Network,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun onRateLimitExpired() {
        rateLimitedEmail = null
        _uiState.update {
            it.copy(
                isRateLimited = false,
                rateLimitSecondsRemaining = 0,
                errorMessage = null,
            )
        }
    }

    private fun AppError.toUserMessage(): UiText? = when (this) {
        AppError.Network -> null // shown via isOffline banner
        is AppError.Api -> when {
            code == HTTP_TOO_MANY_REQUESTS || errorCode == "OTP_RATE_LIMIT_EXCEEDED" -> null // shown via isRateLimited banner
            else -> toUiText()
        }
        else -> toUiText()
    }

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
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
