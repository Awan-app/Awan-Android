package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.StepBody
import com.awan.feature.onboarding.impl.ui.components.StepHeadline
import com.awan.feature.onboarding.impl.ui.components.ZoneReorderList
import com.awan.feature.onboarding.impl.ui.components.ZoneSheet

@Composable
fun ZonesStepBody(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    var editingZoneId by remember { mutableStateOf<String?>(null) }
    val editingZone = state.zones.firstOrNull { it.id == editingZoneId }

    StepBody {
        CascadeItem(0) {
            StepHeadline(
                stringResource(R.string.onboarding_zones_title),
                stringResource(R.string.onboarding_zones_subtitle),
            )
        }
        CascadeItem(1, Modifier.fillMaxWidth()) {
            ZoneReorderList(
                zones = state.zones,
                overlappingIds = state.overlappingZoneIds,
                onToggle = { onAction(OnboardingAction.ToggleZoneEnabled(it)) },
                onOpen = { editingZoneId = it.id },
                onReorder = { from, to -> onAction(OnboardingAction.ReorderZone(from, to)) },
            )
        }
    }

    if (editingZone != null) {
        ZoneSheet(
            zone = editingZone,
            onDismiss = { editingZoneId = null },
            onSetWindow = { start, end ->
                onAction(OnboardingAction.EditZoneWindow(editingZone.id, start, end))
            },
        )
    }
}
