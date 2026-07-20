package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.StepBody
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.TaskLengthSlider
import com.awan.feature.onboarding.impl.ui.humanDuration

@Composable
fun TaskLengthStepBody(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepBody {
        CascadeItem(0) {
            StepHeadline(stringResource(R.string.onboarding_task_length_title))
        }
        CascadeItem(1, Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
            ) {
                RollingDuration(state.preferredTaskLengthMinutes)
                AwanText(
                    stringResource(R.string.onboarding_task_length_caption),
                    style = AwanTheme.styles.metaText,
                )
            }
        }
        CascadeItem(2, Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(top = AwanTheme.spacing.sm)) {
                TaskLengthSlider(
                    options = OnboardingState.TASK_LENGTH_OPTIONS,
                    selected = state.preferredTaskLengthMinutes,
                    onSelect = { onAction(OnboardingAction.TaskLengthChanged(it)) },
                )
            }
        }
        CascadeItem(3, Modifier.fillMaxWidth()) {
            AwanCard(modifier = Modifier.fillMaxWidth(), background = AwanTheme.colors.background) {
                AwanText(
                    stringResource(R.string.onboarding_task_length_hint),
                    style = AwanTheme.styles.bodySecondaryText,
                )
            }
        }
    }
}

@Composable
private fun RollingDuration(minutes: Int) {
    AnimatedContent(
        targetState = minutes,
        transitionSpec = {
            val up = targetState > initialState
            val direction = if (up) 1 else -1
            (slideInVertically { h -> direction * h } + fadeIn()) togetherWith
                (slideOutVertically { h -> -direction * h } + fadeOut())
        },
        label = "taskLength",
    ) { value ->
        AwanText(humanDuration(value), style = AwanTheme.styles.displayText)
    }
}
