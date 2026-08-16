package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.humanDuration
import kotlin.math.roundToInt

private val KnobSize = 32.dp
private val TrackHeight = 10.dp
private val LabelSlot = 48.dp

/** Chunky Skyward slider snapping across the preferred-focus-length stops. Tap a tick or drag the knob. */
@Composable
fun TaskLengthSlider(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val haptics = LocalHapticFeedback.current
    val count = options.size
    val index = options.indexOf(selected).coerceIn(0, count - 1)
    val density = LocalDensity.current
    val stateLabel = humanDuration(selected)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val knobPx = with(density) { KnobSize.toPx() }
        val usable = (widthPx - knobPx).coerceAtLeast(1f)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {

            fun indexFromX(x: Float): Int =
                (((x - knobPx / 2f) / usable) * (count - 1)).roundToInt().coerceIn(0, count - 1)

            // Knob and fill are derived from this one value, so they cannot drift apart mid-animation.
            val fraction by animateFloatAsState(
                targetValue = index / (count - 1f),
                animationSpec = AwanTheme.motion.settle.spec(),
                label = "sliderFraction",
            )
            val knobOffset = with(density) { (fraction * usable).toDp() }
            val filledWidth = with(density) { (knobPx / 2f + fraction * usable).toDp() }

            // The gesture lambdas outlive the composition that created them, so anything that changes
            // per-frame has to be read through rememberUpdatedState rather than captured directly.
            val currentIndex by rememberUpdatedState(index)
            val currentSelect by rememberUpdatedState(onSelect)

            // Dragging reports every pointer move; only a crossed stop is a change worth emitting.
            fun emit(x: Float) {
                val next = indexFromX(x)
                if (next != currentIndex) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    currentSelect(options[next])
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KnobSize)
                    .progressSemantics(index.toFloat(), 0f..(count - 1f), (count - 2).coerceAtLeast(0))
                    .semantics { stateDescription = stateLabel }
                    .pointerInput(usable, options) { detectTapGestures { emit(it.x) } }
                    .pointerInput(usable, options) {
                        detectHorizontalDragGestures { change, _ -> emit(change.position.x) }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(TrackHeight)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.disabledSurface),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(filledWidth.coerceAtLeast(0.dp))
                        .height(TrackHeight)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.sky),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = knobOffset)
                        .size(KnobSize)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(3.dp, colors.sky, CircleShape),
                )
            }

            // Labels share the knob's coordinate system rather than being spaced by a Row, so each
            // one is centred on the stop its knob actually lands on.
            Box(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, minutes ->
                    val centre = with(density) { (knobPx / 2f + usable * i / (count - 1)).toDp() }
                    Box(
                        modifier = Modifier
                            .offset(x = centre - LabelSlot / 2)
                            .width(LabelSlot),
                        contentAlignment = Alignment.Center,
                    ) {
                        AwanText(
                            tickLabel(minutes),
                            style = if (i == index) AwanTheme.styles.buttonCompactText else AwanTheme.styles.metaText,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun tickLabel(minutes: Int): String =
    if (minutes % 60 == 0) {
        stringResource(R.string.onboarding_tick_hours, minutes / 60)
    } else {
        stringResource(R.string.onboarding_tick_minutes, minutes)
    }
