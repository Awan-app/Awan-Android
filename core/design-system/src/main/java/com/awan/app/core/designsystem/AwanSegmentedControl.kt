package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val TrackPadding = 4.dp
private val SegmentGap = 4.dp

/** Long enough for a state round-trip; short enough that a refused selection snaps back at once. */
private val SelectionSettle = 250.milliseconds

/**
 * A row of keys on a sunken track. The selected one is held down on its rim — the same latched
 * press [AwanDisclosure] uses — so the choice reads as a physical key that stays depressed rather
 * than a highlight that moved.
 *
 * Built on [AwanButton] rather than hand-rolled: the rim measure policy, the diagonal press sink
 * and its RTL mirroring, and the segment-tick haptic all come with the Chip variant for free.
 *
 * ```
 * AwanSegmentedControl(
 *     options = AddTaskMode.entries,
 *     selected = state.mode,
 *     onSelect = { onAction(AddTaskAction.ModeChanged(it)) },
 *     label = { stringResource(if (it == AddTaskMode.TASK) R.string.task else R.string.goal) },
 * )
 * ```
 */
@Composable
fun <T> AwanSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    /**
     * The tapped segment latches on the release frame instead of waiting for [selected] to come
     * back through the state round-trip. Without this the finger lifts, the press sink releases,
     * and the segment springs back to raised for the frames it takes the new selection to arrive.
     */
    var pending by remember { mutableStateOf<T?>(null) }
    val shown = pending ?: selected

    LaunchedEffect(pending, selected) {
        // Cleared once the real state agrees, or abandoned if it never does — a rejected selection
        // must not leave the control showing a choice that was refused.
        if (pending != null && pending != selected) delay(SelectionSettle)
        pending = null
    }

    Row(
        modifier = modifier
            .clip(AwanTheme.shapes.button)
            .background(AwanTheme.colors.disabledSurface)
            .padding(TrackPadding),
        horizontalArrangement = Arrangement.spacedBy(SegmentGap),
    ) {
        options.forEach { option ->
            val isSelected = option == shown
            Segment(
                text = label(option),
                isSelected = isSelected,
                enabled = enabled,
                onClick = {
                    if (!isSelected) {
                        pending = option
                        onSelect(option)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spec = AwanTheme.motion.settle.spec<Color>()

    val face by animateColorAsState(
        targetValue = if (isSelected) colors.sky else colors.surface,
        animationSpec = spec,
        label = "segmentFace",
    )
    /**
     * The selected segment's border matches its own face, so it disappears. A contrasting ring
     * around a sunk face is indistinguishable from a rim under a raised one — the segment measures
     * as pressed but reads as raised. Only the unselected segments keep a visible edge, and their
     * rim is then the only rim on the track.
     */
    val edge by animateColorAsState(
        targetValue = if (isSelected) colors.sky else colors.line,
        animationSpec = spec,
        label = "segmentEdge",
    )
    val rim by animateColorAsState(
        targetValue = if (isSelected) colors.skyPressed else colors.line,
        animationSpec = spec,
        label = "segmentRim",
    )
    val ink by animateColorAsState(
        targetValue = if (isSelected) colors.onSky else colors.textSecondary,
        animationSpec = spec,
        label = "segmentInk",
    )
    // A finger on a segment darkens it, on top of the sink. Without this the only feedback is 4dp
    // of travel, which on the already-sunk selected segment is no feedback at all.
    val activeFace = if (isSelected) colors.skyPressed else colors.disabledSurface

    val shape = AwanTheme.shapes.chip
    val rimStyle = remember(rim, shape) { Style { background(rim); shape(shape) } }
    val faceStyle = remember(face, edge, ink, activeFace, shape) {
        Style {
            background(face)
            borderColor(edge)
            contentColor(ink)
            shape(shape)
            pressed { background(activeFace) }
        }
    }

    AwanButton(
        onClick = onClick,
        // AwanButton defaults to Role.Button; a segment is one of a set, and only the set knows that.
        modifier = modifier.semantics {
            selected = isSelected
        },
        style = faceStyle,
        rimStyle = rimStyle,
        variant = AwanButtonVariant.Chip,
        enabled = enabled,
        latchedPressed = isSelected,
        role = Role.Tab,
    ) {
        // Chip's LocalContentColor is fixed to textSecondary, so the label carries its own colour.
        AwanText(text = text, style = AwanTheme.styles.buttonCompactText.copy(color = ink))
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AwanSegmentedControl · Light", showBackground = true)
@Composable
private fun LightSegmentedControlPreview() {
    SegmentedControlPreview(dark = false)
}

@Preview(name = "AwanSegmentedControl · Dark", showBackground = true)
@Composable
private fun DarkSegmentedControlPreview() {
    SegmentedControlPreview(dark = true)
}

@Composable
private fun SegmentedControlPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AwanSegmentedControl(
                options = listOf("Task", "Goal"),
                selected = "Task",
                onSelect = {},
                label = { it },
                modifier = Modifier.fillMaxWidth(),
            )
            AwanSegmentedControl(
                options = listOf("Active  2", "Completed  7"),
                selected = "Completed  7",
                onSelect = {},
                label = { it },
                modifier = Modifier.fillMaxWidth(),
            )
            AwanSegmentedControl(
                options = listOf("One", "Two", "Three"),
                selected = "Two",
                onSelect = {},
                label = { it },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
