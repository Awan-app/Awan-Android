package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskState
import java.time.LocalDate

/**
 * Mirrors back what the parser understood. Everything here is derived from the typed sentence
 * except Mandatory, which is the one attribute the backend has no token for.
 */
@Composable
fun TaskAttributeChips(
    state: AddTaskState,
    today: LocalDate,
    onToggleMandatory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val startAt = state.parsed.startAt
        if (startAt != null) {
            AwanChip(text = rememberWhenLabel(startAt, today), tone = AwanChipTone.Sky)
        } else {
            AwanChip(text = stringResource(R.string.add_task_chip_unscheduled), tone = AwanChipTone.Neutral)
        }

        state.parsed.durationMinutes?.let {
            AwanChip(text = durationLabel(it), tone = AwanChipTone.Violet)
        }

        ZoneChip(state)

        AwanChip(
            text = stringResource(
                if (state.mandatory) R.string.add_task_chip_mandatory else R.string.add_task_chip_optional,
            ),
            tone = if (state.mandatory) AwanChipTone.Tangerine else AwanChipTone.Neutral,
            modifier = Modifier.toggleable(
                value = state.mandatory,
                role = Role.Switch,
                onValueChange = { onToggleMandatory() },
            ),
        )
    }
}

@Composable
private fun ZoneChip(state: AddTaskState) {
    val token = state.parsed.zoneToken ?: return
    when {
        state.resolvedZone != null -> AwanChip(text = state.resolvedZone.name, tone = AwanChipTone.Tangerine)
        state.isResolvingZone -> AwanChip(
            text = stringResource(R.string.add_task_chip_zone_resolving),
            tone = AwanChipTone.Neutral,
        )

        else -> AwanChip(
            text = stringResource(R.string.add_task_chip_zone_unknown, token),
            tone = AwanChipTone.Neutral,
        )
    }
}
