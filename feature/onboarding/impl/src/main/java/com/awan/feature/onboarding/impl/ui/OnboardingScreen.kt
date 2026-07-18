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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingEvent
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.presentation.OnboardingStep
import com.awan.feature.onboarding.impl.presentation.OnboardingViewModel
import com.awan.feature.onboarding.impl.ui.steps.DayBoundsStep
import com.awan.feature.onboarding.impl.ui.steps.FirstTaskStep
import com.awan.feature.onboarding.impl.ui.steps.NameStep
import com.awan.feature.onboarding.impl.ui.steps.NotificationsStep
import com.awan.feature.onboarding.impl.ui.steps.TaskLengthStep
import com.awan.feature.onboarding.impl.ui.steps.WelcomeStep
import com.awan.feature.onboarding.impl.ui.steps.ZonesStep

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
    AnimatedContent(
        targetState = state.step,
        label = "onboardingStep",
        modifier = modifier,
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val direction = if (forward) 1 else -1
            (slideInHorizontally(tween(320)) { width -> direction * width / 5 } + fadeIn(tween(320))) togetherWith
                (slideOutHorizontally(tween(220)) { width -> -direction * width / 5 } + fadeOut(tween(220)))
        },
    ) { step ->
        when (step) {
            OnboardingStep.Welcome -> WelcomeStep(onAction)
            OnboardingStep.Name -> NameStep(state, onAction)
            OnboardingStep.DayBounds -> DayBoundsStep(state, onAction)
            OnboardingStep.Zones -> ZonesStep(state, onAction)
            OnboardingStep.TaskLength -> TaskLengthStep(state, onAction)
            OnboardingStep.FirstTask -> FirstTaskStep(state, onAction)
            OnboardingStep.Notifications -> NotificationsStep(state, onAction)
        }
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
