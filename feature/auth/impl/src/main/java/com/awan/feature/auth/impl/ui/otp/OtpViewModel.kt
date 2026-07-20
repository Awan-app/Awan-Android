package com.awan.feature.auth.impl.ui.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.feature.auth.impl.domain.usecase.RequestOtpUseCase
import com.awan.feature.auth.impl.domain.usecase.VerifyOtpUseCase
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
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val requestOtpUseCase: RequestOtpUseCase,
) : ViewModel() {

    private var email: String = savedStateHandle["email"] ?: ""

    private val _uiState = MutableStateFlow(
        OtpUiState(email = email, resendSecondsRemaining = RESEND_COOLDOWN_SECONDS)
    )
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEmail(email: String) {
        if (email.isNotBlank() && this.email != email) {
            this.email = email
            _uiState.update { it.copy(email = email) }
        }
    }

    fun onDigitsChanged(digits: List<String>) {
        if (_uiState.value.areCellsLocked) return

        _uiState.update {
            it.copy(
                digits = digits,
                status = OtpStatus.Idle,
                errorMessage = null,
            )
        }

        val joined = digits.joinToString("")
        if (joined.length == OTP_LENGTH) {
            verifyCode(joined)
        }
    }

    fun onResendCode() {
        if (!_uiState.value.isResendEnabled && _uiState.value.status != OtpStatus.Expired &&
            _uiState.value.status != OtpStatus.Locked
        ) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResendEnabled = false,
                    resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
                    status = OtpStatus.Idle,
                    errorMessage = null,
                    digits = List(OTP_LENGTH) { "" },
                )
            }
            requestOtpUseCase(email)
        }
    }

    fun onResendTimerExpired() {
        _uiState.update { it.copy(isResendEnabled = true, resendSecondsRemaining = 0) }
    }


    private fun verifyCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = OtpStatus.Verifying) }

            when (val result = verifyOtpUseCase(email = email, code = code)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = OtpStatus.Idle) }
                    _events.send(OtpEvent.NavigateToHome)
                }
                is Result.Error -> {
                    val (newStatus, message) = result.error.toOtpStatusAndMessage()
                    _uiState.update {
                        it.copy(
                            status = newStatus,
                            errorMessage = message,
                            digits = if (newStatus == OtpStatus.Wrong) {
                                List(OTP_LENGTH) { "" }
                            } else {
                                it.digits
                            },
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun AppError.toOtpStatusAndMessage(): Pair<OtpStatus, String?> = when (this) {
        AppError.Network -> OtpStatus.Idle to "No internet connection. Check and try again."
        AppError.Timeout -> OtpStatus.Idle to "Request timed out. Please try again."
        AppError.Unauthorized -> OtpStatus.Expired to "Your code has expired. Request a new one."
        is AppError.Server -> OtpStatus.Idle to "Server error. Please try again later."
        is AppError.Api -> {
            val attemptsSuffix = remainingAttempts?.let { attempts ->
                if (attempts > 0) {
                    " ($attempts attempt${if (attempts > 1) "s" else ""} remaining)"
                } else {
                    " (No attempts remaining)"
                }
            } ?: ""

            val status = when {
                errorCode == "OTP_INVALID_CODE" || code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE -> {
                    if (remainingAttempts == 0) OtpStatus.Locked else OtpStatus.Wrong
                }
                code == HTTP_GONE -> OtpStatus.Expired
                code == HTTP_TOO_MANY_REQUESTS -> OtpStatus.Locked
                else -> OtpStatus.Idle
            }

            val baseMsg = body?.takeIf { it.isNotBlank() } ?: "Invalid OTP code."
            val fullMsg = "$baseMsg$attemptsSuffix"

            status to fullMsg
        }
        AppError.Serialization -> OtpStatus.Idle to "Unexpected server response."
        is AppError.Unknown -> OtpStatus.Idle to "Something went wrong."
    }

    private companion object {
        const val OTP_LENGTH = 6
        const val RESEND_COOLDOWN_SECONDS = 120

        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNPROCESSABLE = 422
        const val HTTP_GONE = 410
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

sealed interface OtpEvent {
    data object NavigateToHome : OtpEvent
}
