package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanBadge
import com.awan.app.core.designsystem.AwanBadgeTone
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.app.core.designsystem.SparkleBurst
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.Zone
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.StepBody
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.formatClock

private const val TITLE_MAX = 140

@Composable
fun FirstTaskStepBody(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    val landed = state.firstTask != null
    StepBody {
        CascadeItem(0) {
            StepHeadline(stringResource(R.string.onboarding_first_task_title))
        }
        CascadeItem(1, Modifier.fillMaxWidth()) {
            AwanTextField(
                value = state.firstTaskTitle,
                onValueChange = { onAction(OnboardingAction.FirstTaskTitleChanged(it.take(TITLE_MAX))) },
                placeholder = stringResource(R.string.onboarding_first_task_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CascadeItem(2) {
            AwanText(stringResource(R.string.onboarding_first_task_examples), style = AwanTheme.styles.metaText)
        }

        AnimatedVisibility(
            visible = landed,
            enter = fadeIn() + slideInVertically(AwanTheme.motion.bouncy.spec()) { it / 2 },
        ) {
            val task = state.firstTask
            val zone = state.zones.firstOrNull { it.id == task?.zoneId }
            if (task != null && zone != null) {
                Box(Modifier.fillMaxWidth()) {
                    LandedTaskCard(task, zone)
                    SparkleBurst(celebrate = state.celebrateTask)
                }
            }
        }
    }
}

@Composable
private fun LandedTaskCard(task: FirstTask, zone: Zone) {
    val zoneColor = Color(zone.colorArgb)
    AwanCard(modifier = Modifier.fillMaxWidth()) {
        AwanText(stringResource(R.string.onboarding_first_task_lands_label), style = AwanTheme.styles.metaText)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        ) {
            AwanText(zone.name, style = AwanTheme.styles.headingText)
            AwanText(
                stringResource(
                    R.string.onboarding_time_range,
                    formatClock(zone.startMinutes),
                    formatClock(zone.endMinutes),
                ),
                style = AwanTheme.styles.metaText,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AwanTheme.spacing.xs)
                .clip(AwanTheme.shapes.card)
                .background(AwanTheme.colors.surface)
                .border(2.dp, zoneColor, AwanTheme.shapes.card)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, AwanTheme.colors.line, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(task.title, style = AwanTheme.styles.bodyText)
                AwanText(
                    stringResource(R.string.onboarding_first_task_duration_zone, task.durationMinutes, zone.name),
                    style = AwanTheme.styles.metaText,
                )
            }
            AwanBadge(text = stringResource(R.string.onboarding_first_task_new_badge), tone = AwanBadgeTone.Violet)
        }
    }
}
