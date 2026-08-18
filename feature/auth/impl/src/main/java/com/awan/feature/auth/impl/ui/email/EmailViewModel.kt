package com.awan.feature.auth.impl.ui.email

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.auth.usecase.GetLastUsedEmailUseCase
import com.awan.app.core.domain.auth.usecase.RequestOtpUseCase
import com.awan.app.core.domain.auth.usecase.SignInWithFirebaseUseCase
import com.awan.feature.auth.impl.R
import com.awan.feature.auth.impl.ui.google.GoogleSignInHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject

@HiltViewModel
class EmailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val requestOtpUseCase: RequestOtpUseCase,
    private val getLastUsedEmailUseCase: GetLastUsedEmailUseCase,
    private val signInWithFirebaseUseCase: SignInWithFirebaseUseCase,
    private val googleSignInHelper: GoogleSignInHelper,
) : ViewModel() {

    private val savedEmail: String? = savedStateHandle[KEY_EMAIL]
    private val savedRateLimitTimestamp: Long? = savedStateHandle[KEY_RATE_LIMIT_TIMESTAMP]
    private val savedRateLimitedEmail: String? = savedStateHandle[KEY_RATE_LIMIT_EMAIL]

    private var rateLimitedEmail: String? = savedRateLimitedEmail

    private val _uiState = MutableStateFlow(
        run {
            val initialEmail = savedEmail.orEmpty()
            val isValid = if (initialEmail.isNotEmpty()) EMAIL_REGEX.matches(initialEmail) else false
            val remainingCooldown = if (savedRateLimitTimestamp != null && savedRateLimitedEmail != null && savedRateLimitedEmail == initialEmail) {
                val elapsed = (System.currentTimeMillis() - savedRateLimitTimestamp) / 1000
                (RATE_LIMIT_COOLDOWN_SECONDS - elapsed).coerceAtLeast(0).toInt()
            } else {
                0
            }
            val isRateLimited = remainingCooldown > 0

            EmailUiState(
                email = initialEmail,
                isEmailValid = isValid,
                isRateLimited = isRateLimited,
                rateLimitSecondsRemaining = remainingCooldown,
            )
        }
    )
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (_uiState.value.email.isEmpty()) {
            viewModelScope.launch {
                val lastEmail = try {
                    getLastUsedEmailUseCase()
                } catch (e: GeneralSecurityException) {
                    null
                } catch (e: IOException) {
                    null
                }
                if (!lastEmail.isNullOrBlank() && _uiState.value.email.isEmpty()) {
                    onEmailChanged(lastEmail)
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        savedStateHandle[KEY_EMAIL] = email
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
                    savedStateHandle.remove<String>(KEY_RATE_LIMIT_EMAIL)
                    savedStateHandle.remove<Long>(KEY_RATE_LIMIT_TIMESTAMP)
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
                        savedStateHandle[KEY_RATE_LIMIT_EMAIL] = state.email
                        savedStateHandle[KEY_RATE_LIMIT_TIMESTAMP] = System.currentTimeMillis()
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

    fun onGoogleSignInStarted() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun onGoogleSignInCancelled() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun onGoogleSignInFailed() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = UiText.StringResource(R.string.auth_error_google_sign_in_failed),
            )
        }
    }

    fun onGoogleIdTokenReceived(googleIdToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val firebaseIdToken = googleSignInHelper.getFirebaseIdToken(googleIdToken)
                authenticateWithFirebase(firebaseIdToken)
            } catch (_: Exception) {
                onGoogleSignInFailed()
            }
        }
    }

    fun authenticateWithFirebase(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = signInWithFirebaseUseCase(idToken)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val isNewUser = result.data.user?.isNew == true
                    if (isNewUser) {
                        _events.send(EmailEvent.NavigateToOnboarding)
                    } else {
                        _events.send(EmailEvent.NavigateToHome)
                    }
                }
                is Result.Error -> {
                    val message = mapFirebaseErrorToUserMessage(result.error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = result.error is AppError.Network,
                            errorMessage = message,
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun onRateLimitExpired() {
        rateLimitedEmail = null
        savedStateHandle.remove<String>(KEY_RATE_LIMIT_EMAIL)
        savedStateHandle.remove<Long>(KEY_RATE_LIMIT_TIMESTAMP)
        _uiState.update {
            it.copy(
                isRateLimited = false,
                rateLimitSecondsRemaining = 0,
                errorMessage = null,
            )
        }
    }

    private fun mapFirebaseErrorToUserMessage(error: AppError): UiText = when (error) {
        AppError.Network -> UiText.StringResource(R.string.auth_offline_banner)
        is AppError.Server -> if (error.code == 503) {
            UiText.StringResource(R.string.auth_error_firebase_not_configured)
        } else {
            error.toUiText()
        }
        is AppError.Api -> when (error.errorCode) {
            "FIREBASE_TOKEN_INVALID" -> UiText.StringResource(R.string.auth_error_firebase_token_invalid)
            "FIREBASE_PROVIDER_NOT_ALLOWED" -> UiText.StringResource(R.string.auth_error_firebase_provider_not_allowed)
            "FIREBASE_EMAIL_MISSING" -> UiText.StringResource(R.string.auth_error_firebase_email_missing)
            "FIREBASE_EMAIL_NOT_VERIFIED" -> UiText.StringResource(R.string.auth_error_firebase_email_not_verified)
            "FIREBASE_NOT_CONFIGURED" -> UiText.StringResource(R.string.auth_error_firebase_not_configured)
            else -> error.toUiText()
        }
        else -> error.toUiText()
    }

    private fun AppError.toUserMessage(): UiText? = when (this) {
        AppError.Network -> null
        is AppError.Api -> when {
            code == HTTP_TOO_MANY_REQUESTS || errorCode == "OTP_RATE_LIMIT_EXCEEDED" -> null
            else -> toUiText()
        }
        else -> toUiText()
    }

    companion object {
        const val KEY_EMAIL = "auth_email"
        const val KEY_RATE_LIMIT_EMAIL = "auth_rate_limit_email"
        const val KEY_RATE_LIMIT_TIMESTAMP = "auth_rate_limit_timestamp"
        private const val HTTP_TOO_MANY_REQUESTS = 429
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
    data object NavigateToHome : EmailEvent
    data object NavigateToOnboarding : EmailEvent
}

