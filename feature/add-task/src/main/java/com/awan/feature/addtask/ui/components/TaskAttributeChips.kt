package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskState
import java.time.LocalDate

/**
 * A live readout of the sentence. Every chip here except Mandatory is derived from the typed text,
 * and the two editable ones write their choice *back* into that text — so the sentence stays the
 * one place a draft is defined, and the chips never hold state of their own.
 */
@Composable
fun TaskAttributeChips(
    state: AddTaskState,
    today: LocalDate,
    onEditTime: () -> Unit,
    onEditDuration: () -> Unit,
    onToggleMandatory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Always present: with nothing stated the task is simply for today, at no set time.
        AttributeChip(
            label = whenChipLabel(state, today),
            tone = colors.zoneSky,
            active = state.parsed.startAt != null,
            onClick = onEditTime,
        )

        AttributeChip(
            label = state.parsed.durationMinutes?.let { durationLabel(it) }
                ?: stringResource(R.string.add_task_chip_no_duration),
            tone = colors.zoneViolet,
            active = state.parsed.durationMinutes != null,
            onClick = onEditDuration,
        )

        PoppingChip(visible = state.parsed.zoneToken != null) {
            ZoneChip(state)
        }

        AttributeChip(
            label = stringResource(
                if (state.mandatory) R.string.add_task_chip_mandatory else R.string.add_task_chip_optional,
            ),
            tone = colors.zoneTangerine,
            active = state.mandatory,
            onClick = onToggleMandatory,
        )
    }
}

@Composable
private fun whenChipLabel(state: AddTaskState, today: LocalDate): String {
    val startAt = state.parsed.startAt ?: return stringResource(R.string.add_task_chip_today_no_time)
    return rememberWhenLabel(startAt, today, showTime = state.parsed.hasExplicitTime)
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
        state.resolvedZone != null -> AttributeChip(
            label = state.resolvedZone.name,
            tone = colors.zoneLavender,
        )

        state.isResolvingZone -> AttributeChip(
            label = stringResource(R.string.add_task_chip_zone_resolving),
            tone = colors.zoneLavender,
            active = false,
        )

        else -> AttributeChip(
            label = stringResource(R.string.add_task_chip_zone_unknown, token),
            tone = colors.destructive,
            active = false,
        )
    }
}
