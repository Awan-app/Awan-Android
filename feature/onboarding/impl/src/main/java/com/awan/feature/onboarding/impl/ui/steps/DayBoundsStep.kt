package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.app.core.domain.onboarding.utils.DayBoundsValidation
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.DayTimeline
import com.awan.feature.onboarding.impl.ui.components.InlineNotice
import com.awan.feature.onboarding.impl.ui.components.NoticeTone
import com.awan.feature.onboarding.impl.ui.components.StepBody
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.WakeSleepRow

@Composable
fun DayBoundsStepBody(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepBody {
        CascadeItem(0) {
            StepHeadline(stringResource(R.string.onboarding_day_bounds_step_title), null)
        }
        CascadeItem(1, Modifier.fillMaxWidth()) {
            WakeSleepRow(
                glyph = "☀️",
                label = stringResource(R.string.onboarding_i_usually_wake_up_at),
                minutes = state.bounds.wakeMinutes,
                selected = false,
                onPick = { onAction(OnboardingAction.WakeChanged(it)) },
            )
        }
        CascadeItem(2, Modifier.fillMaxWidth()) {
            WakeSleepRow(
                glyph = "🌙",
                label = stringResource(R.string.onboarding_i_usually_sleep_at),
                minutes = state.bounds.sleepMinutes,
                selected = false,
                onPick = { onAction(OnboardingAction.SleepChanged(it)) },
            )
        }
        CascadeItem(3, Modifier.fillMaxWidth()) {
            DayTimeline(state.dayPreview, showZones = false)
        }
        when {
            state.boundsValidation == DayBoundsValidation.SameTime ->
                InlineNotice(stringResource(R.string.onboarding_bounds_same_time_error), NoticeTone.Error)

            state.showWakingWarning -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InlineNotice(
                    stringResource(R.string.onboarding_bounds_short_day_warning),
                    NoticeTone.Warning,
                    modifier = Modifier.weight(1f),
                )
                AwanButton(
                    onClick = { onAction(OnboardingAction.DismissWakingWarning) },
                    variant = AwanButtonVariant.Quiet,
                ) {
                    AwanText(stringResource(R.string.onboarding_got_it), style = AwanTheme.styles.skipLink)
                }
            }
        }
    }
}
