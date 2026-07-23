package com.awan.feature.addtask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskMode

/** The Task | Goal switch at the top of the sheet. */
@Composable
fun AddTaskModeSelector(
    selected: AddTaskMode,
    onSelect: (AddTaskMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(AwanTheme.shapes.pill)
            .background(AwanTheme.colors.disabledSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

@Composable
private fun RowScope.ModeSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val motion = AwanTheme.motion
    val background by animateColorAsState(
        targetValue = if (isSelected) AwanTheme.colors.surface else AwanTheme.colors.disabledSurface,
        animationSpec = motion.settle.spec(),
        label = "modeSegmentBackground",
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) AwanTheme.colors.textPrimary else AwanTheme.colors.textSecondary,
        animationSpec = motion.settle.spec(),
        label = "modeSegmentContent",
    )
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(AwanTheme.shapes.pill)
            .background(background)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AwanText(text = label, style = AwanTheme.styles.buttonCompactText.copy(color = content))
    }
}
