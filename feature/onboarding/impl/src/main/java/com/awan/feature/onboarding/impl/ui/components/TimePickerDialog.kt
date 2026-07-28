package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanTimePicker
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.feature.onboarding.impl.R

/** Shared native time picker; reports the chosen time as minutes-from-midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val safe = initialMinutes.mod(DayBounds.MINUTES_PER_DAY)
    val state = rememberTimePickerState(
        initialHour = safe / 60,
        initialMinute = safe % 60,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AwanButton(onClick = { onConfirm(state.hour * 60 + state.minute) }, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.onboarding_time_picker_set), style = AwanTheme.styles.skipLink)
            }
        },
        dismissButton = {
            AwanButton(onClick = onDismiss, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.onboarding_time_picker_cancel), style = AwanTheme.styles.metaText)
            }
        },
        text = { AwanTimePicker(state = state) },
        containerColor = AwanTheme.colors.surface,
    )
}
