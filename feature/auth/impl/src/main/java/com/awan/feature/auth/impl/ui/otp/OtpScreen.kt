package com.awan.feature.auth.impl.ui.otp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotState
import com.awan.feature.auth.impl.ui.components.AuthButton
import com.awan.feature.auth.impl.ui.components.AuthFooter
import com.awan.feature.auth.impl.ui.components.AuthScreenLayout
import com.awan.feature.auth.impl.ui.components.CountdownTimer
import com.awan.feature.auth.impl.ui.components.EmailDisplay
import com.awan.feature.auth.impl.ui.components.ErrorMessage
import com.awan.feature.auth.impl.ui.components.OtpField
import com.awan.feature.auth.impl.ui.components.rememberCountdownTimerState

@Composable
fun OtpRouteScreen(
    email: String = "",
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onUseDifferentEmail: () -> Unit = {},
    viewModel: OtpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(email) {
        viewModel.setEmail(email)
    }

    // Consume one-time navigation events.
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is OtpEvent.NavigateToHome -> onNext()
            }
        }
    }

    OtpScreen(
        state = state,
        onDigitsChanged = viewModel::onDigitsChanged,
        onBack = onBack,
        onResend = viewModel::onResendCode,
        onUseDifferentEmail = onUseDifferentEmail,
    )
}

@Composable
fun OtpScreen(
    state: OtpUiState,
    onDigitsChanged: (List<String>) -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mascotState = when (state.status) {
        OtpStatus.Idle -> MascotState.Thinking
        OtpStatus.Verifying -> MascotState.Loading
        OtpStatus.Wrong -> MascotState.Sad
        OtpStatus.Expired -> MascotState.Error
        OtpStatus.Locked -> MascotState.Error
    }

    val resendTimer = rememberCountdownTimerState(
        initialSeconds = state.resendSecondsRemaining,
    )

    AuthScreenLayout(
        modifier = modifier,
        title = "Check your inbox!",
        subtitle = "",
        mascotState = mascotState,
        topActionContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate back"
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = AwanTheme.colors.textPrimary,
                    )
                }
            }
        },
        formContent = {
            EmailDisplay(
                email = state.email,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))

            OtpField(
                digits = state.digits,
                onDigitsChange = onDigitsChanged,
                status = state.status,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = state.status == OtpStatus.Verifying,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics { contentDescription = "Verifying your code" },
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AwanTheme.colors.sky,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    AwanText(
                        text = "Verifying you in...",
                        style = AwanTheme.styles.captionText,
                    )
                }
            }

            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.errorMessage?.let { message ->
                    ErrorMessage(message = message)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = state.status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "otp-action-area",
            ) { status ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (status) {
                        OtpStatus.Locked -> {
                            AuthButton(
                                text = "REQUEST A NEW CODE",
                                onClick = onResend,
                            )
                        }
                        OtpStatus.Expired -> {
                            AwanButton(
                                onClick = onResend,
                                variant = AwanButtonVariant.Quiet,
                            ) {
                                AwanText("RESEND CODE")
                            }
                        }
                        else -> {
                            CountdownTimer(
                                state = resendTimer,
                                onResend = onResend,
                                isResendEnabled = state.isResendEnabled,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AwanButton(
                onClick = onUseDifferentEmail,
                variant = AwanButtonVariant.Quiet,
                modifier = Modifier.semantics {
                    contentDescription = "Use a different email address"
                },
            ) {
                AwanText("Use a different email")
            }
        },
    )
}
