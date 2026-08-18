package com.awan.feature.auth.impl.ui.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.auth.usecase.RequestOtpUseCase
import com.awan.app.core.domain.auth.usecase.VerifyOtpUseCase
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
    private val savedStateHandle: SavedStateHandle,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val requestOtpUseCase: RequestOtpUseCase,
) : ViewModel() {

    private var email: String = savedStateHandle[KEY_EMAIL] ?: savedStateHandle["email"] ?: ""

    private val _uiState = MutableStateFlow(
        run {
            val savedDigits: ArrayList<String>? = savedStateHandle[KEY_DIGITS]
            val savedStatusName: String? = savedStateHandle[KEY_STATUS]
            val savedSentTimestamp: Long? = savedStateHandle[KEY_SENT_TIMESTAMP]

            val remainingCooldown = if (savedSentTimestamp != null) {
                val elapsed = (System.currentTimeMillis() - savedSentTimestamp) / 1000
                (RESEND_COOLDOWN_SECONDS - elapsed).coerceAtLeast(0).toInt()
            } else {
                RESEND_COOLDOWN_SECONDS
            }
            val isResendEnabled = remainingCooldown == 0
            val initialStatus = savedStatusName?.let { name ->
                runCatching { OtpStatus.valueOf(name) }.getOrNull()
            }?.let { status ->
                if (status == OtpStatus.Verifying) OtpStatus.Idle else status
            } ?: OtpStatus.Idle

            OtpUiState(
                email = email,
                digits = savedDigits ?: List(OTP_LENGTH) { "" },
                status = initialStatus,
                errorMessage = null,
                isResendEnabled = isResendEnabled,
                resendSecondsRemaining = remainingCooldown,
            )
        }
    )
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEmail(email: String) {
        if (email.isNotBlank()) {
            val isSameEmail = this.email == email
            val hasSavedTimestamp = savedStateHandle.contains(KEY_SENT_TIMESTAMP)

            if (isSameEmail && hasSavedTimestamp) {
                // State was already restored from SavedStateHandle across process death
                return
            }

            this.email = email
            val now = System.currentTimeMillis()
            val initialDigits = List(OTP_LENGTH) { "" }

            savedStateHandle[KEY_EMAIL] = email
            savedStateHandle[KEY_DIGITS] = ArrayList(initialDigits)
            savedStateHandle[KEY_STATUS] = OtpStatus.Idle.name
            savedStateHandle[KEY_SENT_TIMESTAMP] = now

            _uiState.value = OtpUiState(
                email = email,
                digits = initialDigits,
                status = OtpStatus.Idle,
                errorMessage = null,
                isResendEnabled = false,
                resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
            )
        }
    }

    fun onDigitsChanged(digits: List<String>) {
        if (_uiState.value.areCellsLocked) return

        savedStateHandle[KEY_DIGITS] = ArrayList(digits)
        savedStateHandle[KEY_STATUS] = OtpStatus.Idle.name

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

        val now = System.currentTimeMillis()
        val emptyDigits = List(OTP_LENGTH) { "" }

        savedStateHandle[KEY_SENT_TIMESTAMP] = now
        savedStateHandle[KEY_DIGITS] = ArrayList(emptyDigits)
        savedStateHandle[KEY_STATUS] = OtpStatus.Idle.name

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResendEnabled = false,
                    resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
                    status = OtpStatus.Idle,
                    errorMessage = null,
                    digits = emptyDigits,
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
                    clearSavedState()
                    _uiState.update { it.copy(status = OtpStatus.Idle) }
                    val isNewUser = result.data.user?.isNew == true
                    if (isNewUser) {
                        _events.send(OtpEvent.NavigateToOnboarding)
                    } else {
                        _events.send(OtpEvent.NavigateToHome)
                    }
                }
                is Result.Error -> {
                    val (newStatus, message) = result.error.toOtpStatusAndMessage()
                    savedStateHandle[KEY_STATUS] = newStatus.name
                    val updatedDigits = if (newStatus == OtpStatus.Wrong) {
                        List(OTP_LENGTH) { "" }
                    } else {
                        _uiState.value.digits
                    }
                    savedStateHandle[KEY_DIGITS] = ArrayList(updatedDigits)

                    _uiState.update {
                        it.copy(
                            status = newStatus,
                            errorMessage = message,
                            digits = updatedDigits,
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearSavedState() {
        savedStateHandle.remove<String>(KEY_EMAIL)
        savedStateHandle.remove<ArrayList<String>>(KEY_DIGITS)
        savedStateHandle.remove<String>(KEY_STATUS)
        savedStateHandle.remove<Long>(KEY_SENT_TIMESTAMP)
    }

    private fun AppError.toOtpStatusAndMessage(): Pair<OtpStatus, UiText?> = when (this) {
        AppError.Network -> OtpStatus.Idle to toUiText()
        AppError.Timeout -> OtpStatus.Idle to toUiText()
        AppError.Unauthorized -> OtpStatus.Expired to toUiText()
        is AppError.Server -> OtpStatus.Idle to toUiText()
        is AppError.Api -> {
            val status = when {
                errorCode == "OTP_LOCKED" || code == HTTP_TOO_MANY_REQUESTS || remainingAttempts == 0 -> OtpStatus.Locked
                errorCode == "OTP_INVALID_CODE" || code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE -> OtpStatus.Wrong
                code == HTTP_GONE -> OtpStatus.Expired
                else -> OtpStatus.Idle
            }

            status to toUiText()
        }
        AppError.Serialization -> OtpStatus.Idle to toUiText()
        is AppError.Unknown -> OtpStatus.Idle to toUiText()
        else -> OtpStatus.Idle to toUiText()
    }

    companion object {
        const val KEY_EMAIL = "otp_email"
        const val KEY_DIGITS = "otp_digits"
        const val KEY_STATUS = "otp_status"
        const val KEY_SENT_TIMESTAMP = "otp_sent_timestamp"

        const val OTP_LENGTH = 6
        const val RESEND_COOLDOWN_SECONDS = 120

        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_UNPROCESSABLE = 422
        private const val HTTP_GONE = 410
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

sealed interface OtpEvent {
    data object NavigateToHome : OtpEvent
    data object NavigateToOnboarding : OtpEvent
}
