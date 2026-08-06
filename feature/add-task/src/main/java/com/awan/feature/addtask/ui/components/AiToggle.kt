package com.awan.feature.addtask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanAiAura
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R
import kotlin.math.abs
import kotlin.math.max

private val RailWidth = 48.dp
private val RailHeight = 28.dp
private val ThumbSize = 22.dp
private val ThumbInset = 3.dp
private val SparkTile = 40.dp

private const val TwinkleMillis = 2200

/**
 * Handing the task to Awan is an offer, not an attribute — so it is a card that states what it does,
 * sitting above the form it replaces, rather than another pill in the chip row where it read as one
 * more toggle among four and nobody found it.
 *
 * Switched on it wears the same rotating sweep as the text field below. That is the whole point of
 * reusing [AwanAiAura] here: the card and the field are one state, not two things that happen to be
 * violet.
 */
@Composable
fun AiToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val description = stringResource(R.string.add_task_ai_switch_content_description)

    // Blend, never alpha: a translucent fill over the card's tone-coloured rim reads as the rim.
    val surface by animateColorAsState(
        targetValue = when {
            enabled -> lerp(colors.surface, colors.zoneViolet, 0.10f)
            else -> colors.surface
        },
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "aiCardSurface",
    )

    AwanAiAura(
        active = enabled,
        shape = AwanTheme.shapes.card,
        modifier = modifier,
    ) {
        AwanCard(
            modifier = Modifier.semantics {
                role = Role.Switch
                contentDescription = description
                toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off
            },
            background = surface,
            onClick = onToggle,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
            ) {
                AiSparkTile(lit = enabled)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
                ) {
                    AwanText(
                        stringResource(R.string.add_task_ai_switch),
                        style = AwanTheme.styles.headingText,
                    )
                    AwanText(
                        stringResource(R.string.add_task_ai_switch_body),
                        style = AwanTheme.styles.bodySecondaryText,
                    )
                }
                AiSwitchTrack(on = enabled)
            }
        }
    }
}

/** The four-point spark, on its own tile so it reads as the card's mark rather than a bullet. */
@Composable
private fun AiSparkTile(lit: Boolean) {
    val colors = AwanTheme.colors
    val reduced = reducedMotion()

    val tile by animateColorAsState(
        targetValue = when {
            lit -> lerp(colors.surface, colors.zoneViolet, 0.22f)
            else -> lerp(colors.surface, colors.line, 0.55f)
        },
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "aiSparkTile",
    )
    val ink by animateColorAsState(
        targetValue = if (lit) colors.zoneVioletPressed else colors.meta,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "aiSparkInk",
    )

    // One cycle drives both sparks in opposite phase, so they alternate rather than pulse together.
    val cycle = when {
        reduced || !lit -> 0f
        else -> rememberInfiniteTransition(label = "aiSpark").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(TwinkleMillis, easing = LinearEasing)),
            label = "aiSparkCycle",
        ).value
    }

    Box(
        modifier = Modifier
            .size(SparkTile)
            .clip(AwanTheme.shapes.chip)
            .background(tile)
            .semantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(SparkTile)) {
            val unit = size.minDimension / 2f
            drawSpark(
                centre = Offset(size.width * 0.44f, size.height * 0.46f),
                radius = unit * 0.62f * breathe(cycle, phase = 0f),
                colour = ink,
            )
            drawSpark(
                centre = Offset(size.width * 0.72f, size.height * 0.72f),
                radius = unit * 0.28f * breathe(cycle, phase = 0.5f),
                colour = ink,
            )
        }
    }
}

/** 1.0 → 1.18 → 1.0 over one cycle, offset by [phase] turns. */
private fun breathe(cycle: Float, phase: Float): Float {
    val t = (cycle + phase) % 1f
    return 1f + 0.18f * (1f - abs(t * 2f - 1f))
}

/**
 * The concave four-point star everyone reads as "AI". Each arm's control point sits at the centre,
 * which is what pulls the edges in — a straight-line star of the same radius reads as a compass rose.
 */
private fun DrawScope.drawSpark(
    centre: Offset,
    radius: Float,
    colour: Color,
) {
    val r = max(radius, 0f)
    if (r == 0f) return
    val path = Path().apply {
        moveTo(centre.x, centre.y - r)
        quadraticTo(centre.x, centre.y, centre.x + r, centre.y)
        quadraticTo(centre.x, centre.y, centre.x, centre.y + r)
        quadraticTo(centre.x, centre.y, centre.x - r, centre.y)
        quadraticTo(centre.x, centre.y, centre.x, centre.y - r)
        close()
    }
    drawPath(path, colour)
}

/** The rail from [MandatoryToggle], grown to the size a card-level control deserves. */
@Composable
private fun AiSwitchTrack(on: Boolean) {
    val colors = AwanTheme.colors
    val reduced = reducedMotion()
    val travel = RailWidth - ThumbInset * 2 - ThumbSize

    val offset by animateDpAsState(
        targetValue = if (on) ThumbInset + travel else ThumbInset,
        animationSpec = if (reduced) snap() else AwanTheme.motion.bouncy.spec(),
        label = "aiThumb",
    )
    val rail by animateColorAsState(
        targetValue = if (on) colors.zoneViolet else colors.line,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "aiRail",
    )
    val thumb by animateColorAsState(
        targetValue = if (on) colors.surface else colors.meta,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "aiThumbColor",
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
                .offset { IntOffset(x = offset.roundToPx(), y = 0) }
                .size(ThumbSize)
                .clip(AwanTheme.shapes.pill)
                .background(thumb),
        )
    }
}
