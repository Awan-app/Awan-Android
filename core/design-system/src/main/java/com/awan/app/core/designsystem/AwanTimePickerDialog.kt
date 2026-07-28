package com.awan.app.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/**
 * Shared native time picker; reports the chosen time as minutes-from-midnight. Labels are passed in
 * so each feature keeps its own localised strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanTimePickerDialog(
    initialMinutes: Int,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val safe = initialMinutes.mod(MINUTES_PER_DAY)
    val state = rememberTimePickerState(
        initialHour = safe / MINUTES_PER_HOUR,
        initialMinute = safe % MINUTES_PER_HOUR,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AwanButton(
                onClick = { onConfirm(state.hour * MINUTES_PER_HOUR + state.minute) },
                variant = AwanButtonVariant.Quiet,
            ) {
                AwanText(confirmLabel, style = AwanTheme.styles.skipLink)
            }
        },
        dismissButton = {
            AwanButton(onClick = onDismiss, variant = AwanButtonVariant.Quiet) {
                AwanText(cancelLabel, style = AwanTheme.styles.metaText)
            }
        },
        containerColor = AwanTheme.colors.surface,
        text = { TimePicker(state = state) },
    )
}
