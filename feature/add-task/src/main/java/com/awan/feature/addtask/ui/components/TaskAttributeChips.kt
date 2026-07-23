package com.awan.feature.addtask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskState
import java.time.LocalDate

/**
 * Mirrors back what the parser understood. Each chip springs in the moment its token is recognised,
 * which is the feedback that tells you the sentence was read — everything here is derived from what
 * you typed except Mandatory, the one attribute the backend has no token for.
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
        PoppingChip(visible = startAt != null) {
            if (startAt != null) {
                AwanChip(text = rememberWhenLabel(startAt, today), tone = AwanChipTone.Sky)
            }
        }
        PoppingChip(visible = startAt == null) {
            AwanChip(text = stringResource(R.string.add_task_chip_unscheduled), tone = AwanChipTone.Neutral)
        }

        val duration = state.parsed.durationMinutes
        PoppingChip(visible = duration != null) {
            if (duration != null) {
                AwanChip(text = durationLabel(duration), tone = AwanChipTone.Violet)
            }
        }

        PoppingChip(visible = state.parsed.zoneToken != null) {
            ZoneChip(state)
        }

        MandatoryChip(isMandatory = state.mandatory, onToggle = onToggleMandatory)
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

/** The one chip you can press: it squashes on toggle so the tap is felt, not just seen. */
@Composable
private fun MandatoryChip(isMandatory: Boolean, onToggle: () -> Unit) {
    val reduced = reducedMotion()
    val squash by animateFloatAsState(
        targetValue = if (isMandatory) 1f else 0.94f,
        animationSpec = if (reduced) snap() else AwanTheme.motion.playful.spec(),
        label = "mandatorySquash",
    )
    AwanChip(
        text = stringResource(
            if (isMandatory) R.string.add_task_chip_mandatory else R.string.add_task_chip_optional,
        ),
        tone = if (isMandatory) AwanChipTone.Tangerine else AwanChipTone.Neutral,
        modifier = Modifier
            .graphicsLayer {
                scaleX = squash
                scaleY = squash
            }
            .clip(AwanTheme.shapes.pill)
            .toggleable(
                value = isMandatory,
                role = Role.Switch,
                onValueChange = { onToggle() },
            ),
    )
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
