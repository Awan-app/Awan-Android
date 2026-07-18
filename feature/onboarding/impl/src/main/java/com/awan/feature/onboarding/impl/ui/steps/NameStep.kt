package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.ChangeAnytimeChip
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.StepScaffold
import com.awan.feature.onboarding.impl.ui.timeOfDayGreeting

private const val NAME_MAX = 50

@Composable
fun NameStep(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        onSkip = { onAction(OnboardingAction.Skip) },
        progressCurrent = state.step.dotIndex,
        footer = {
            AwanButton(
                onClick = { onAction(OnboardingAction.Next) },
                enabled = state.canContinueName,
                modifier = Modifier.fillMaxWidth(),
            ) { AwanText("CONTINUE") }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AwanMascot(MascotExpression.Greet, width = 132.dp)
        }
        StepHeadline("What should I call you?", "So I can greet you properly each morning.")
        ChangeAnytimeChip()
        Spacer(Modifier.height(AwanTheme.spacing.xs))
        AwanText(
            "${timeOfDayGreeting()}, ${state.trimmedFirstName.ifEmpty { "friend" }}",
            style = AwanTheme.styles.displayText,
        )
        Spacer(Modifier.height(AwanTheme.spacing.xs))
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm), modifier = Modifier.fillMaxWidth()) {
            AwanTextField(
                value = state.firstName,
                onValueChange = { onAction(OnboardingAction.NameChanged(it.take(NAME_MAX), state.lastName)) },
                placeholder = "First name",
                modifier = Modifier.fillMaxWidth(),
            )
            AwanTextField(
                value = state.lastName,
                onValueChange = { onAction(OnboardingAction.NameChanged(state.firstName, it.take(NAME_MAX))) },
                placeholder = "Last name (optional)",
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
