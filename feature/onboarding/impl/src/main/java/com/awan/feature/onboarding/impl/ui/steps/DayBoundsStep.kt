package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.onboarding.DayBoundsValidation
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.ChangeAnytimeChip
import com.awan.feature.onboarding.impl.ui.components.DayPreview
import com.awan.feature.onboarding.impl.ui.components.InlineNotice
import com.awan.feature.onboarding.impl.ui.components.NoticeTone
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.StepScaffold
import com.awan.feature.onboarding.impl.ui.components.WakeSleepRow

@Composable
fun DayBoundsStep(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        onSkip = { onAction(OnboardingAction.Skip) },
        progressCurrent = state.step.dotIndex,
        footer = {
            AwanButton(
                onClick = { onAction(OnboardingAction.Next) },
                enabled = state.canContinueBounds,
                modifier = Modifier.fillMaxWidth(),
            ) { AwanText("CONTINUE") }
        },
    ) {
        StepHeadline("When does your day begin and end?", "I only schedule inside your waking hours.")
        ChangeAnytimeChip()
        WakeSleepRow(
            glyph = "☀️",
            label = "I usually wake up at",
            minutes = state.bounds.wakeMinutes,
            selected = false,
            onPick = { onAction(OnboardingAction.WakeChanged(it)) },
        )
        WakeSleepRow(
            glyph = "🌙",
            label = "I usually sleep at",
            minutes = state.bounds.sleepMinutes,
            selected = false,
            onPick = { onAction(OnboardingAction.SleepChanged(it)) },
        )
        DayPreview(state.dayPreview, showZones = false)
        when {
            state.boundsValidation == DayBoundsValidation.SameTime ->
                InlineNotice("Wake and sleep times can't be the same.", NoticeTone.Error)
            state.showWakingWarning -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InlineNotice("That's a short waking day — sure?", NoticeTone.Warning, modifier = Modifier.weight(1f))
                AwanButton(onClick = { onAction(OnboardingAction.DismissWakingWarning) }, variant = AwanButtonVariant.Quiet) {
                    AwanText("Got it", style = AwanTheme.styles.skipLink)
                }
            }
        }
    }
}
