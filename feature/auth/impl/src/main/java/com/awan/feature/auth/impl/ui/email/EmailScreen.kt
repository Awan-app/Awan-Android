package com.awan.feature.auth.impl.ui.email

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotState
import com.awan.app.core.designsystem.R as DesignR
import com.awan.feature.auth.impl.R
import com.awan.feature.auth.impl.ui.components.AuthButton
import com.awan.feature.auth.impl.ui.components.AuthDivider
import com.awan.feature.auth.impl.ui.components.AuthEmailField
import com.awan.feature.auth.impl.ui.components.AuthScreenLayout
import com.awan.feature.auth.impl.ui.components.SocialButton
import com.awan.feature.auth.impl.ui.components.rememberCountdownTimerState
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun EmailRouteScreen(
    onNext: (email: String) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: EmailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleGoogleSignInResult(task)
        } else {
            viewModel.onGoogleSignInCancelledOrFailed()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is EmailEvent.NavigateToOtp -> onNext(event.email)
                EmailEvent.NavigateToHome -> onNavigateToHome()
                EmailEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    EmailScreen(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onContinue = viewModel::onSendCode,
        onRateLimitExpired = viewModel::onRateLimitExpired,
        onSignInWithGoogle = {
            viewModel.onSignInWithGoogle(context) { intent ->
                googleSignInLauncher.launch(intent)
            }
        },
    )
}

enum class EmailErrorBanner { None, RateLimited, Offline }

@Composable
fun EmailScreen(
    state: EmailUiState,
    onEmailChanged: (String) -> Unit,
    onContinue: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
    onRateLimitExpired: () -> Unit = {},
) {
    val mascotState = when {
        state.isLoading -> MascotState.Loading
        state.errorMessage != null -> MascotState.Sad
        state.email.isNotEmpty() -> MascotState.Typing
        else -> MascotState.Idle
    }

    val rateLimitTimer = rememberCountdownTimerState(
        initialSeconds = state.rateLimitSecondsRemaining,
        key = state.isRateLimited,
        onExpired = onRateLimitExpired,
    )

    val bannerState = when {
        state.isRateLimited -> EmailErrorBanner.RateLimited
        state.isOffline -> EmailErrorBanner.Offline
        else -> EmailErrorBanner.None
    }

    AuthScreenLayout(
        modifier = modifier,
        title = stringResource(R.string.auth_title),
        subtitle = stringResource(R.string.auth_subtitle),
        mascotState = mascotState,
        content = {
            AwanText(
                text = stringResource(R.string.auth_email_label),
                style = AwanTheme.styles.captionText,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(6.dp))

            AuthEmailField(
                value = state.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage != null,
                errorMessage = state.errorMessage,
                enabled = !state.isLoading,
                onDone = onContinue,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = bannerState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "email-error-banner",
            ) { targetBanner ->
                when (targetBanner) {
                    EmailErrorBanner.None -> Unit
                    EmailErrorBanner.RateLimited -> {
                        Column {
                            AwanText(
                                text = stringResource(R.string.auth_rate_limited_banner, rateLimitTimer.formattedTime),
                                style = AwanTheme.styles.errorText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Rate limited. Wait ${rateLimitTimer.formattedTime}"
                                    },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    EmailErrorBanner.Offline -> {
                        Column {
                            AwanText(
                                text = stringResource(R.string.auth_offline_banner),
                                style = AwanTheme.styles.captionText,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            val buttonText = if (state.isRateLimited) {
                stringResource(R.string.auth_send_code_timer, rateLimitTimer.formattedTime)
            } else {
                stringResource(R.string.auth_send_code)
            }

            AuthButton(
                text = buttonText,
                onClick = onContinue,
                enabled = state.canSubmit,
                isLoading = state.isLoading,
            )
        },
        bottomContent = {
            AuthDivider()

            Spacer(modifier = Modifier.height(20.dp))

            SocialButton(
                text = stringResource(R.string.auth_continue_with_google),
                onClick = onSignInWithGoogle,
                enabled = !state.isLoading,
                painter = painterResource(DesignR.drawable.ic_google),
                socialType = AwanButtonVariant.Google,
                contentDescription = stringResource(R.string.auth_continue_with_google),
            )
        }
    )
}
