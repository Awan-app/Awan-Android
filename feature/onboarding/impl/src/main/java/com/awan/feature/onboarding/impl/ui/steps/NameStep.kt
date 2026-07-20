package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.StepBody
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.timeOfDayGreeting

private const val NAME_MAX = 50

@Composable
fun NameStepBody(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepBody(arrangement = Arrangement.spacedBy(AwanTheme.spacing.md)) {
        CascadeItem(0) {
            StepHeadline(
                stringResource(R.string.onboarding_name_title),
                stringResource(R.string.onboarding_name_subtitle),
            )
        }
        CascadeItem(1) {
            AwanText(
                stringResource(
                    R.string.onboarding_name_greeting,
                    timeOfDayGreeting(),
                    state.trimmedFirstName.ifEmpty { stringResource(R.string.onboarding_name_default_friend) },
                ),
                style = AwanTheme.styles.displayText,
            )
        }
        CascadeItem(2, Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanTextField(
                    value = state.firstName,
                    onValueChange = { onAction(OnboardingAction.NameChanged(it.take(NAME_MAX), state.lastName)) },
                    placeholder = stringResource(R.string.onboarding_name_first_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                AwanTextField(
                    value = state.lastName,
                    onValueChange = { onAction(OnboardingAction.NameChanged(state.firstName, it.take(NAME_MAX))) },
                    placeholder = stringResource(R.string.onboarding_name_last_placeholder),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
