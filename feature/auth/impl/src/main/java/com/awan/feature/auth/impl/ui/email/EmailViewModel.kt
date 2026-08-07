package com.awan.feature.auth.impl.ui.email

import android.content.Context
import android.content.Intent
import android.util.Log
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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
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
    private val requestOtpUseCase: RequestOtpUseCase,
    private val getLastUsedEmailUseCase: GetLastUsedEmailUseCase,
    private val signInWithFirebaseUseCase: SignInWithFirebaseUseCase,
    private val googleSignInHelper: GoogleSignInHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailUiState())
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var rateLimitedEmail: String? = null

    init {
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

    fun onSignInWithGoogle(context: Context, onLaunchIntent: (Intent) -> Unit) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val intentResult = googleSignInHelper.getGoogleSignInIntent(context)
            intentResult.fold(
                onSuccess = { intent ->
                    onLaunchIntent(intent)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.auth_error_google_sign_in_failed),
                        )
                    }
                }
            )
        }
    }

    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                val googleIdToken = account?.idToken
                    ?: throw IllegalStateException("Google ID token is null.")

                val firebaseIdToken = googleSignInHelper.getFirebaseIdToken(googleIdToken)
                authenticateWithFirebase(firebaseIdToken)
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
                val isCancelled = e.statusCode == CommonStatusCodes.CANCELED || e.statusCode == SIGN_IN_CANCELLED_STATUS_CODE
                if (isCancelled) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.auth_error_google_sign_in_failed),
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google/Firebase sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.StringResource(R.string.auth_error_google_sign_in_failed),
                    )
                }
            }
        }
    }

    fun onGoogleSignInCancelledOrFailed() {
        _uiState.update { it.copy(isLoading = false) }
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
        is AppError.Api -> when (error.errorCode) {
            "FIREBASE_TOKEN_INVALID" -> UiText.StringResource(R.string.auth_error_firebase_token_invalid)
            "FIREBASE_PROVIDER_NOT_ALLOWED" -> UiText.StringResource(R.string.auth_error_firebase_provider_not_allowed)
            "FIREBASE_EMAIL_MISSING" -> UiText.StringResource(R.string.auth_error_firebase_email_missing)
            "FIREBASE_EMAIL_NOT_VERIFIED" -> UiText.StringResource(R.string.auth_error_firebase_email_not_verified)
            "FIREBASE_NOT_CONFIGURED" -> UiText.StringResource(R.string.auth_error_firebase_not_configured)
            else -> when (error.code) {
                503 -> UiText.StringResource(R.string.auth_error_firebase_not_configured)
                else -> error.toUiText()
            }
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

    private companion object {
        private const val TAG = "EmailViewModel"
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val RATE_LIMIT_COOLDOWN_SECONDS = 60
        private const val SIGN_IN_CANCELLED_STATUS_CODE = 12501

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
