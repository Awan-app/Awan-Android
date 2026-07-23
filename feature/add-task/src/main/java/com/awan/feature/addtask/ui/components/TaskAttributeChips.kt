package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanDropdownMenu
import com.awan.app.core.designsystem.AwanDropdownMenuItem
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskPicker
import com.awan.feature.addtask.presentation.AddTaskState
import java.time.LocalDate

/** The lengths worth one tap; anything else is typed into the sentence directly. */
private val DurationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)

/**
 * A live readout of the sentence. Every chip here except Mandatory is derived from the typed text,
 * and the two editable ones write their choice *back* into that text — so the sentence stays the
 * one place a draft is defined, and the chips never hold state of their own.
 *
 * Wrapping rather than scrolling: the toggle is the last chip, and a chip parked off the right edge
 * is a chip nobody knows is there.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskAttributeChips(
    state: AddTaskState,
    today: LocalDate,
    onEditWhen: () -> Unit,
    onEditDuration: () -> Unit,
    onDurationPicked: (Int) -> Unit,
    onDurationMenuDismissed: () -> Unit,
    onToggleMandatory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        // Always present: with nothing stated the task is simply for today, at no set time.
        AwanChip(
            label = whenChipLabel(state, today),
            tone = colors.zoneSky,
            active = state.parsed.startAt != null,
            onClick = onEditWhen,
        )

        Box {
            AwanChip(
                label = state.parsed.durationMinutes?.let { durationLabel(it) }
                    ?: stringResource(R.string.add_task_chip_no_duration),
                tone = colors.zoneViolet,
                active = state.parsed.durationMinutes != null,
                onClick = onEditDuration,
            )

            DurationMenu(
                expanded = state.openPicker == AddTaskPicker.DURATION,
                selectedMinutes = state.parsed.durationMinutes,
                onDismiss = onDurationMenuDismissed,
                onSelect = onDurationPicked,
            )
        }

        PoppingChip(visible = state.parsed.zoneToken != null) {
            ZoneChip(state)
        }

        MandatoryToggle(mandatory = state.mandatory, onToggle = onToggleMandatory)
    }
}

@Composable
private fun whenChipLabel(state: AddTaskState, today: LocalDate): String {
    val startAt = state.parsed.startAt ?: return stringResource(R.string.add_task_chip_today_no_time)
    return rememberWhenLabel(startAt, today, showTime = state.parsed.hasExplicitTime)
}

/**
 * Lengths hang off the chip they set, not out of a dialog in the middle of the screen — the choice
 * is small enough that leaving the sentence for it costs more than it's worth.
 */
@Composable
private fun DurationMenu(
    expanded: Boolean,
    selectedMinutes: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AwanDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DurationPresets.forEach { minutes ->
            val active = minutes == selectedMinutes
            AwanDropdownMenuItem(
                label = durationLabel(minutes),
                onClick = { onSelect(minutes) },
                selected = active,
                leading = { AwanChipDot(tone = AwanTheme.colors.zoneViolet, active = active) },
            )
        }
    }
}

/** Springs in on the bouncy spec so a recognised token lands with the same weight as a nudge card. */
@Composable
private fun PoppingChip(visible: Boolean, content: @Composable () -> Unit) {
    val spec = AwanTheme.motion.bouncy.spec<Float>()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spec) + scaleIn(spec, initialScale = 0.72f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
    ) {
        content()
    }
}

@Composable
private fun ZoneChip(state: AddTaskState) {
    val token = state.parsed.zoneToken ?: return
    val colors = AwanTheme.colors
    when {
        state.resolvedZone != null -> AwanChip(
            label = state.resolvedZone.name,
            tone = colors.zoneLavender,
        )

        state.isResolvingZone -> AwanChip(
            label = stringResource(R.string.add_task_chip_zone_resolving),
            tone = colors.zoneLavender,
            active = false,
        )

        else -> AwanChip(
            label = stringResource(R.string.add_task_chip_zone_unknown, token),
            tone = colors.destructive,
            active = false,
        )
    }
}
