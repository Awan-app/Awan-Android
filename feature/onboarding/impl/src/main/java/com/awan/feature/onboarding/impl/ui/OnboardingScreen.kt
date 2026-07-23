package com.awan.feature.onboarding.impl.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingEvent
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.presentation.OnboardingStep
import com.awan.feature.onboarding.impl.presentation.OnboardingViewModel
import com.awan.app.core.designsystem.AwanActionSheet
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.components.StepScaffold
import com.awan.feature.onboarding.impl.ui.steps.DayBoundsStepBody
import com.awan.feature.onboarding.impl.ui.steps.FirstTaskStepBody
import com.awan.feature.onboarding.impl.ui.steps.NameStepBody
import com.awan.feature.onboarding.impl.ui.steps.NotificationsStepBody
import com.awan.feature.onboarding.impl.ui.steps.TaskLengthStepBody
import com.awan.feature.onboarding.impl.ui.steps.WelcomeStepBody
import com.awan.feature.onboarding.impl.ui.steps.ZonesStepBody

@Composable
fun OnboardingRoot(
    onComplete: () -> Unit,
    onExit: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onAction(OnboardingAction.NotificationPermissionResult(granted))
        if (!granted && context.findActivity()?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, NOTIFICATION_PERMISSION) } == true) {
            viewModel.onAction(OnboardingAction.NotificationsPermanentlyDenied)
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.NavigateHome -> onComplete()
            OnboardingEvent.ExitFlow -> onExit()
            OnboardingEvent.RequestNotificationPermission -> when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                    viewModel.onAction(OnboardingAction.NotificationPermissionResult(true))
                ContextCompat.checkSelfPermission(context, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED ->
                    viewModel.onAction(OnboardingAction.NotificationPermissionResult(true))
                state.notificationsPermanentlyDenied -> context.openAppSettings()
                else -> permissionLauncher.launch(NOTIFICATION_PERMISSION)
            }
        }
    }

    OnboardingScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onAction(OnboardingAction.Back) }
    val exitMillis = AwanTheme.motion.standardMillis

    // No step-change haptic here: AwanButton fires its own on the press that caused the step change.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.celebrateTask) {
        if (state.celebrateTask) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    var showSkipConfirm by remember { mutableStateOf(false) }

    // The Welcome step's secondary "Skip setup" button opens a confirmation sheet instead of
    // skipping immediately. Other steps pass Skip through to the ViewModel unchanged.
    val wrappedAction: (OnboardingAction) -> Unit = { action ->
        if (action == OnboardingAction.Skip && state.step == OnboardingStep.Welcome) {
            showSkipConfirm = true
        } else {
            onAction(action)
        }
    }

    StepScaffold(
        chrome = stepChrome(state, wrappedAction),
        onBack = { onAction(OnboardingAction.Back) },
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = state.step,
            label = "onboardingStep",
            transitionSpec = {
                // The scaffold gives every step the same bounded slot, so there is no size to
                // transform — leaving the default in would animate a difference that isn't there.
                (
                    fadeIn(tween(exitMillis)) togetherWith
                        fadeOut(tween(exitMillis)) + slideOutVertically(tween(exitMillis)) { it / 12 }
                    ) using null
            },
        ) { step ->
            when (step) {
                OnboardingStep.Welcome -> WelcomeStepBody()
                OnboardingStep.Name -> NameStepBody(state, onAction)
                OnboardingStep.DayBounds -> DayBoundsStepBody(state, onAction)
                OnboardingStep.Zones -> ZonesStepBody(state, onAction)
                OnboardingStep.TaskLength -> TaskLengthStepBody(state, onAction)
                OnboardingStep.FirstTask -> FirstTaskStepBody(state, onAction)
                OnboardingStep.Notifications -> NotificationsStepBody(state, onAction)
            }
        }
    }

    if (showSkipConfirm) {
        AwanActionSheet(
            title = stringResource(R.string.onboarding_skip_confirm_title),
            body = stringResource(R.string.onboarding_skip_confirm_body),
            icon = { AwanMascot(expression = com.awan.app.core.designsystem.MascotExpression.Curious) },
            primaryLabel = stringResource(R.string.onboarding_skip_confirm_cancel),
            onPrimary = { showSkipConfirm = false },
            secondaryLabel = stringResource(R.string.onboarding_skip_confirm_action),
            onSecondary = {
                showSkipConfirm = false
                onAction(OnboardingAction.SkipSetup)
            },
            onDismiss = { showSkipConfirm = false },
        )
    }
}

private const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
