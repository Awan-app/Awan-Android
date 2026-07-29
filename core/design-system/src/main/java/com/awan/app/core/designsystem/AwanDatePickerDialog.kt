package com.awan.app.core.designsystem

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDate

/** Material counts in UTC-midnight millis; a [LocalDate] is exactly its epoch day in that scale. */
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Shared native date picker; takes and reports a [LocalDate]. Labels are passed in so each feature
 * keeps its own localised strings. [earliestDate] greys out everything before it.
 *
 * This and [AwanTimePickerDialog] are the only two places that know Material's pickers exist. Both
 * speak in domain values rather than picker state, so swapping either for a hand-drawn Awan
 * composable is a change to one file body and nothing else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanDatePickerDialog(
    initialDate: LocalDate,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    earliestDate: LocalDate? = null,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = remember(earliestDate) { NotBefore(earliestDate) },
    )
    val selected = state.selectedDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AwanButton(
                onClick = { selected?.let(onConfirm) },
                variant = AwanButtonVariant.Quiet,
                enabled = selected != null,
            ) {
                AwanText(confirmLabel, style = AwanTheme.styles.skipLink)
            }
        },
        dismissButton = {
            AwanButton(onClick = onDismiss, variant = AwanButtonVariant.Quiet) {
                AwanText(cancelLabel, style = AwanTheme.styles.metaText)
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class NotBefore(private val earliest: LocalDate?) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        earliest == null || utcTimeMillis >= earliest.toEpochDay() * MILLIS_PER_DAY

    override fun isSelectableYear(year: Int): Boolean =
        earliest == null || year >= earliest.year
}
