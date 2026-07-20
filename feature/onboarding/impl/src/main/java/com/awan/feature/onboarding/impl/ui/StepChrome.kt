package com.awan.feature.onboarding.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.presentation.OnboardingStep

@Immutable
data class StepAction(val label: String, val onClick: () -> Unit)

@Immutable
data class StepChrome(
    val progressCurrent: Int,
    val primary: StepAction,
    val primaryEnabled: Boolean = true,
    val showBack: Boolean = true,
    val onSkip: (() -> Unit)? = null,
    val secondary: StepAction? = null,
    val mascot: MascotExpression? = null,
    val mascotWidth: Dp = 0.dp,
    val centeredContent: Boolean = false,
)

/** The per-step configuration of the persistent chrome, resolved in one place. */
@Composable
fun stepChrome(state: OnboardingState, onAction: (OnboardingAction) -> Unit): StepChrome {
    val skip = { onAction(OnboardingAction.Skip) }
    val next = { onAction(OnboardingAction.Next) }
    val continueLabel = stringResource(R.string.onboarding_continue)

    return when (state.step) {
        OnboardingStep.Welcome -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_welcome_lets_go), next),
            showBack = false,
            secondary = StepAction(stringResource(R.string.onboarding_welcome_skip_setup), skip),
            mascot = MascotExpression.Greet,
            mascotWidth = 190.dp,
            centeredContent = true,
        )

        OnboardingStep.Name -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            primaryEnabled = state.canContinueName,
            onSkip = skip,
            mascot = MascotExpression.Greet,
            mascotWidth = 132.dp,
        )

        OnboardingStep.DayBounds -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            primaryEnabled = state.canContinueBounds,
            onSkip = skip,
        )

        OnboardingStep.Zones -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_zones_use_this), next),
            onSkip = skip,
            secondary = StepAction(stringResource(R.string.onboarding_zones_reset)) {
                onAction(OnboardingAction.UseSuggestedZones)
            },
        )

        OnboardingStep.TaskLength -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            onSkip = skip,
        )

        OnboardingStep.FirstTask -> {
            val landed = state.firstTask != null
            StepChrome(
                progressCurrent = state.step.dotIndex,
                primary = StepAction(
                    label = when {
                        state.isSubmittingTask -> stringResource(R.string.onboarding_first_task_scheduling)
                        landed -> continueLabel
                        else -> stringResource(R.string.onboarding_first_task_add)
                    },
                    onClick = {
                        onAction(if (landed) OnboardingAction.Next else OnboardingAction.SubmitFirstTask)
                    },
                ),
                primaryEnabled = landed || state.canSubmitFirstTask,
                onSkip = skip,
                secondary = StepAction(stringResource(R.string.onboarding_first_task_skip), skip),
                mascot = if (landed) MascotExpression.Celebrate else MascotExpression.Curious,
                mascotWidth = 148.dp,
            )
        }

        OnboardingStep.Notifications -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_notifications_turn_on)) {
                onAction(OnboardingAction.EnableNotifications)
            },
            secondary = StepAction(stringResource(R.string.onboarding_notifications_not_now), next),
            mascot = MascotExpression.Idle,
            mascotWidth = 136.dp,
        )
    }
}
