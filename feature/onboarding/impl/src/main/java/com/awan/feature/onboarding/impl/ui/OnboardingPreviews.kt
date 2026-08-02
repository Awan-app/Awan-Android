package com.awan.feature.onboarding.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.onboarding.model.FirstTask
import com.awan.app.core.domain.onboarding.usecase.SuggestZoneScheduleUseCase
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.presentation.OnboardingStep

private val previewState = OnboardingState(
    firstName = "Sam",
    zones = SuggestZoneScheduleUseCase()(DayBounds.Default),
    firstTaskTitle = "Draft the project brief",
)

@Composable
private fun PreviewStep(step: OnboardingStep, state: OnboardingState = previewState) {
    AwanTheme { OnboardingScreen(state = state.copy(step = step), onAction = {}) }
}

@Preview(name = "Welcome") @Composable private fun WelcomePreview() = PreviewStep(OnboardingStep.Welcome)

@Preview(name = "Name") @Composable private fun NamePreview() = PreviewStep(OnboardingStep.Name)

@Preview(name = "Day bounds") @Composable private fun DayBoundsPreview() = PreviewStep(OnboardingStep.DayBounds)

@Preview(name = "Zones") @Composable private fun ZonesPreview() = PreviewStep(OnboardingStep.Zones)

@Preview(name = "Zones · RTL", locale = "ar")
@Composable
private fun ZonesRtlPreview() = PreviewStep(OnboardingStep.Zones)

@Preview(name = "Day bounds · RTL", locale = "ar")
@Composable
private fun DayBoundsRtlPreview() = PreviewStep(OnboardingStep.DayBounds)

@Preview(name = "Task length") @Composable private fun TaskLengthPreview() = PreviewStep(OnboardingStep.TaskLength)

@Preview(name = "First task · celebrating")
@Composable
private fun FirstTaskPreview() = PreviewStep(
    OnboardingStep.FirstTask,
    previewState.copy(
        firstTask = FirstTask("t", "Draft the project brief", "study", 450, 60),
        celebrateTask = true,
    ),
)

@Preview(name = "Notifications") @Composable private fun NotificationsPreview() = PreviewStep(OnboardingStep.Notifications)
