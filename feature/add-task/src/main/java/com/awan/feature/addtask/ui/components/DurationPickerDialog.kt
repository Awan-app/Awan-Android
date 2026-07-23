package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R

/** The lengths worth one tap; anything else is typed into the sentence directly. */
private val Presets = listOf(15, 30, 45, 60, 90, 120, 180, 240)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPickerDialog(
    selectedMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AwanTheme.colors.surface,
        title = { AwanText(stringResource(R.string.add_task_duration_title), style = AwanTheme.styles.titleText) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
            ) {
                Presets.forEach { minutes ->
                    AttributeChip(
                        label = durationLabel(minutes),
                        tone = AwanTheme.colors.zoneViolet,
                        active = minutes == selectedMinutes,
                        onClick = { onConfirm(minutes) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AwanButton(onClick = onDismiss, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.add_task_picker_cancel), style = AwanTheme.styles.metaText)
            }
        },
    )
}
