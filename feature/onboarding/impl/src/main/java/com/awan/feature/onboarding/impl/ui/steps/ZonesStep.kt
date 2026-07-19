package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.DayPreview
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.StepScaffold
import com.awan.feature.onboarding.impl.ui.components.ZoneCard

@Composable
fun ZonesStep(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        onSkip = { onAction(OnboardingAction.Skip) },
        progressCurrent = state.step.dotIndex,
        footer = {
            AwanButton(onClick = { onAction(OnboardingAction.Next) }, modifier = Modifier.fillMaxWidth()) {
                AwanText(stringResource(R.string.onboarding_zones_use_this))
            }
            AwanButton(onClick = { onAction(OnboardingAction.UseSuggestedZones) }, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.onboarding_zones_reset), style = AwanTheme.styles.skipLink)
            }
        },
    ) {
        StepHeadline(
            stringResource(R.string.onboarding_zones_title),
            stringResource(R.string.onboarding_zones_subtitle),
        )
        DayPreview(state.dayPreview)
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs), modifier = Modifier.fillMaxWidth()) {
            state.zones.forEachIndexed { index, zone ->
                ZoneCard(
                    name = zone.name,
                    colorArgb = zone.colorArgb,
                    startMinutes = zone.startMinutes,
                    endMinutes = zone.endMinutes,
                    enabled = zone.isEnabled,
                    overlapping = zone.id in state.overlappingZoneIds,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.zones.lastIndex,
                    onToggle = { onAction(OnboardingAction.ToggleZoneEnabled(zone.id)) },
                    onMoveUp = { onAction(OnboardingAction.ReorderZone(index, index - 1)) },
                    onMoveDown = { onAction(OnboardingAction.ReorderZone(index, index + 1)) },
                    onSetStart = { onAction(OnboardingAction.EditZoneWindow(zone.id, it, zone.endMinutes)) },
                    onSetEnd = { onAction(OnboardingAction.EditZoneWindow(zone.id, zone.startMinutes, it)) },
                )
            }
        }
    }
}
