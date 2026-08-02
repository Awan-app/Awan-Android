package com.awan.feature.addtask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanChipDotSize
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R

private val RailWidth = 26.dp
private val RailHeight = 12.dp
private val ThumbInset = 2.dp

/**
 * Whether the engine may move this task off the day, as the one chip in the row that visibly moves.
 *
 * Every other chip wears a static dot. This one's dot sits on a rail and slides across when tapped,
 * so the affordance is built from the row's own parts rather than bolted on — same pill, same rim,
 * same dot, one of them free to travel. The label carries the state in words, which is also exactly
 * what a screen reader announces, so what is seen and what is spoken cannot drift apart.
 */
@Composable
fun MandatoryToggle(
    mandatory: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AwanChip(
        label = stringResource(
            if (mandatory) R.string.add_task_chip_mandatory else R.string.add_task_chip_optional,
        ),
        tone = AwanChipTone.Tangerine,
        modifier = modifier.semantics { role = Role.Switch },
        active = mandatory,
        leading = { DotOnARail(travelled = mandatory, tone = AwanTheme.colors.zoneTangerine) },
        onClick = onToggle,
    )
}

/** The row's own dot, freed to travel. Shared by every chip in the sheet that behaves as a switch. */
@Composable
internal fun DotOnARail(travelled: Boolean, tone: Color) {
    val colors = AwanTheme.colors
    val reduced = reducedMotion()
    val travel = RailWidth - ThumbInset * 2 - AwanChipDotSize

    val offset by animateDpAsState(
        targetValue = if (travelled) ThumbInset + travel else ThumbInset,
        animationSpec = if (reduced) snap() else AwanTheme.motion.bouncy.spec(),
        label = "railThumb",
    )
    val rail by animateColorAsState(
        targetValue = if (travelled) tone else colors.line,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "railTrack",
    )
    val thumb by animateColorAsState(
        targetValue = if (travelled) colors.surface else colors.meta,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "railDot",
    )

    Box(
        modifier = Modifier
            .size(width = RailWidth, height = RailHeight)
            .clip(AwanTheme.shapes.pill)
            .background(rail),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(AwanChipDotSize)
                .clip(AwanTheme.shapes.pill)
                .background(thumb),
        )
    }
}
