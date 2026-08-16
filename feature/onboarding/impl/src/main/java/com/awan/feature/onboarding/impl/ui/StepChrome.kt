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
    val primaryLoading: Boolean = false,
    val showBack: Boolean = true,
    val onSkip: (() -> Unit)? = null,
    val secondary: StepAction? = null,
    val mascot: MascotExpression? = null,
    val mascotWidth: Dp = 0.dp,
    /**
     * Space above the mascot. Welcome uses it to sit its mascot + greeting cluster near the middle
     * of the region; every other step leaves it at zero. It is a value rather than a layout switch
     * so the scaffold's measurement rules stay identical on every step — see [StepScaffold].
     */
    val leadingSpace: Dp = 0.dp,
)

/** The per-step configuration of the persistent chrome, resolved in one place. */
@Composable
fun stepChrome(state: OnboardingState, onAction: (OnboardingAction) -> Unit): StepChrome {
    val skip = if (state.isSubmittingTask) null else { { onAction(OnboardingAction.Skip) } }
    val next = { onAction(OnboardingAction.Next) }
    val continueLabel = stringResource(R.string.onboarding_continue)
    val showBack = !state.isSubmittingTask

    return when (state.step) {
        OnboardingStep.Welcome -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_welcome_lets_go), next),
            primaryEnabled = !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = false,
            secondary = if (state.isSubmittingTask) null else StepAction(stringResource(R.string.onboarding_welcome_skip_setup)) {
                onAction(OnboardingAction.Skip)
            },
            mascot = MascotExpression.Greet,
            mascotWidth = 190.dp,
            leadingSpace = 64.dp,
        )

        OnboardingStep.Name -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            primaryEnabled = state.canContinueName && !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = showBack,
            onSkip = skip,
            mascot = MascotExpression.Greet,
            mascotWidth = 132.dp,
        )

        OnboardingStep.DayBounds -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            primaryEnabled = state.canContinueBounds && !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = showBack,
            onSkip = skip,
        )

        OnboardingStep.Zones -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_zones_use_this), next),
            primaryEnabled = !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = showBack,
            onSkip = skip,
            secondary = if (state.isSubmittingTask) null else StepAction(stringResource(R.string.onboarding_zones_reset)) {
                onAction(OnboardingAction.UseSuggestedZones)
            },
        )

        OnboardingStep.TaskLength -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(continueLabel, next),
            primaryEnabled = !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = showBack,
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
                primaryEnabled = (landed || state.canSubmitFirstTask) && !state.isSubmittingTask,
                primaryLoading = state.isSubmittingTask,
                showBack = showBack,
                onSkip = skip,
                secondary = if (state.isSubmittingTask || skip == null) null else StepAction(stringResource(R.string.onboarding_first_task_skip), skip),
                mascot = if (landed) MascotExpression.Celebrate else MascotExpression.Curious,
                mascotWidth = 148.dp,
            )
        }

        OnboardingStep.Notifications -> StepChrome(
            progressCurrent = state.step.dotIndex,
            primary = StepAction(stringResource(R.string.onboarding_notifications_turn_on)) {
                onAction(OnboardingAction.EnableNotifications)
            },
            primaryEnabled = !state.isSubmittingTask,
            primaryLoading = state.isSubmittingTask,
            showBack = showBack,
            secondary = if (state.isSubmittingTask) null else StepAction(stringResource(R.string.onboarding_notifications_not_now), next),
            mascot = MascotExpression.Idle,
            mascotWidth = 136.dp,
        )
    }
}
