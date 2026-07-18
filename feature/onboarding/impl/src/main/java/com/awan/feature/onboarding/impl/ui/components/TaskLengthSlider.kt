package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import kotlin.math.roundToInt

/** Chunky Skyward slider snapping across the preferred-focus-length stops. Tap a tick or drag the knob. */
@Composable
fun TaskLengthSlider(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val count = options.size
    val index = options.indexOf(selected).coerceIn(0, count - 1)
    val density = LocalDensity.current
    val knob = 32.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(knob)) {
            val widthPx = with(density) { maxWidth.toPx() }
            val knobPx = with(density) { knob.toPx() }
            val usable = (widthPx - knobPx).coerceAtLeast(1f)

            fun indexFromX(x: Float): Int =
                (((x - knobPx / 2f) / usable) * (count - 1)).roundToInt().coerceIn(0, count - 1)

            val fraction = index / (count - 1f)
            val knobOffset by animateDpAsState(with(density) { (fraction * usable).toDp() }, label = "knob")
            val filledWidth = with(density) { (knobPx / 2f + fraction * usable).toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(count) { detectTapGestures { onSelect(options[indexFromX(it.x)]) } }
                    .pointerInput(count) {
                        detectHorizontalDragGestures { change, _ -> onSelect(options[indexFromX(change.position.x)]) }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.disabledSurface),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(filledWidth)
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.sky),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = knobOffset)
                        .size(knob)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(3.dp, colors.sky, CircleShape),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            options.forEachIndexed { i, minutes ->
                AwanText(
                    tickLabel(minutes),
                    style = if (i == index) AwanTheme.styles.buttonCompactText else AwanTheme.styles.metaText,
                )
            }
        }
    }
}

private fun tickLabel(minutes: Int): String = when (minutes) {
    30 -> "30m"
    180 -> "3h"
    else -> minutes.toString()
}
