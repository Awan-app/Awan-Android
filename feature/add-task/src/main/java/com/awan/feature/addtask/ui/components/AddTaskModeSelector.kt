package com.awan.feature.addtask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskMode

private val TrackPadding = 4.dp
private val TrackHeight = 46.dp

/**
 * The Task | Goal switch. A single pill slides between the two halves on the settle spring rather
 * than each half changing colour, so the selection feels like one object moving.
 */
@Composable
fun AddTaskModeSelector(
    selected: AddTaskMode,
    onSelect: (AddTaskMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val slide by animateFloatAsState(
        targetValue = if (selected == AddTaskMode.TASK) 0f else 1f,
        animationSpec = if (reduced) snap() else AwanTheme.motion.settle.spec(),
        label = "modeSlide",
    )

    BoxWithConstraints(
        modifier = modifier
            .height(TrackHeight)
            .clip(AwanTheme.shapes.pill)
            .background(AwanTheme.colors.disabledSurface)
            .padding(TrackPadding),
    ) {
        val halfWidth = (maxWidth - TrackPadding * 2) / 2

        Box(
            modifier = Modifier
                .offset { IntOffset(x = (halfWidth * slide).roundToPx(), y = 0) }
                .width(halfWidth)
                .fillMaxHeight()
                .clip(AwanTheme.shapes.pill)
                .background(AwanTheme.colors.surface),
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            ModeSegment(
                label = stringResource(R.string.add_task_mode_task),
                isSelected = selected == AddTaskMode.TASK,
                onClick = { onSelect(AddTaskMode.TASK) },
            )
            ModeSegment(
                label = stringResource(R.string.add_task_mode_goal),
                isSelected = selected == AddTaskMode.GOAL,
                onClick = { onSelect(AddTaskMode.GOAL) },
            )
        }
    }
}

@Composable
private fun RowScope.ModeSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val content by animateColorAsState(
        targetValue = if (isSelected) AwanTheme.colors.textPrimary else AwanTheme.colors.textSecondary,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "modeSegmentContent",
    )
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(AwanTheme.shapes.pill)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AwanText(text = label, style = AwanTheme.styles.buttonCompactText.copy(color = content))
    }
}
