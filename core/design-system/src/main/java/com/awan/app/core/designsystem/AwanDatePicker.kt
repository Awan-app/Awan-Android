package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.style.styleable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

/**
 * Returns the default [DatePickerColors] for the Awan design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun awanDatePickerColors(): DatePickerColors {
    return DatePickerDefaults.colors(
        containerColor = AwanTheme.colors.surface,
        titleContentColor = AwanTheme.colors.textPrimary,
        headlineContentColor = AwanTheme.colors.textPrimary,
        weekdayContentColor = AwanTheme.colors.textSecondary,
        subheadContentColor = AwanTheme.colors.textSecondary,
        navigationContentColor = AwanTheme.colors.textPrimary,
        yearContentColor = AwanTheme.colors.textSecondary,
        disabledYearContentColor = AwanTheme.colors.disabledContent,
        selectedYearContentColor = AwanTheme.colors.onSky,
        disabledSelectedYearContentColor = AwanTheme.colors.onSky.copy(alpha = 0.38f),
        selectedYearContainerColor = AwanTheme.colors.sky,
        disabledSelectedYearContainerColor = AwanTheme.colors.sky.copy(alpha = 0.38f),
        dayContentColor = AwanTheme.colors.textPrimary,
        disabledDayContentColor = AwanTheme.colors.disabledContent,
        selectedDayContentColor = AwanTheme.colors.onSky,
        disabledSelectedDayContentColor = AwanTheme.colors.onSky.copy(alpha = 0.38f),
        selectedDayContainerColor = AwanTheme.colors.sky,
        disabledSelectedDayContainerColor = AwanTheme.colors.sky.copy(alpha = 0.38f),
        todayContentColor = AwanTheme.colors.sky,
        todayDateBorderColor = AwanTheme.colors.sky,
        dayInSelectionRangeContentColor = AwanTheme.colors.onSky,
        dayInSelectionRangeContainerColor = AwanTheme.colors.sky.copy(alpha = 0.2f),
        dividerColor = AwanTheme.colors.line
    )
}

/**
 * A themed DatePicker following the Awan design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanDatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    showModeToggle: Boolean = true,
) {
    DatePicker(
        state = state,
        modifier = modifier,
        showModeToggle = showModeToggle,
        colors = awanDatePickerColors()
    )
}

/**
 * A themed DatePickerDialog following the Awan design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = AwanTheme.shapes.card,
    containerColor: Color = AwanTheme.colors.surface,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        shape = shape,
        colors = DatePickerDefaults.colors(
            containerColor = containerColor,
        ),
        tonalElevation = tonalElevation,
        properties = properties,
        content = content
    )
}

@Preview(name = "DatePicker · Light", showBackground = true)
@Composable
private fun LightDatePickerPreview() {
    DatePickerPreview(dark = false)
}

@Preview(name = "DatePicker · Dark", showBackground = true)
@Composable
private fun DarkDatePickerPreview() {
    DatePickerPreview(dark = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp)
        ) {
            val state = rememberDatePickerState()
            AwanDatePicker(state = state)
        }
    }
}
