package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.ChangeAnytimeChip
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.StepScaffold
import com.awan.feature.onboarding.impl.ui.components.TaskLengthSlider
import com.awan.feature.onboarding.impl.ui.humanDuration

@Composable
fun TaskLengthStep(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        onSkip = { onAction(OnboardingAction.Skip) },
        progressCurrent = state.step.dotIndex,
        footer = {
            AwanButton(onClick = { onAction(OnboardingAction.Next) }, modifier = Modifier.fillMaxWidth()) {
                AwanText("CONTINUE")
            }
        },
    ) {
        StepHeadline("How long do you like to focus in one go?")
        ChangeAnytimeChip()
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
        ) {
            AwanText(humanDuration(state.preferredTaskLengthMinutes), style = AwanTheme.styles.displayText)
            AwanText("preferred focus block", style = AwanTheme.styles.metaText)
        }
        Box(Modifier.fillMaxWidth().padding(top = AwanTheme.spacing.sm)) {
            TaskLengthSlider(
                options = OnboardingState.TASK_LENGTH_OPTIONS,
                selected = state.preferredTaskLengthMinutes,
                onSelect = { onAction(OnboardingAction.TaskLengthChanged(it)) },
            )
        }
        AwanCard(modifier = Modifier.fillMaxWidth(), background = AwanTheme.colors.background) {
            AwanText(
                "Longer means fewer, deeper blocks; shorter means more, lighter ones. " +
                    "Anything longer gets split into linked sessions.",
                style = AwanTheme.styles.bodySecondaryText,
            )
        }
    }
}
