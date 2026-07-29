package com.awan.app.core.designsystem

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.styleable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

/**
 * A themed TimePicker following the Awan design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanTimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
) {
    TimePicker(
        state = state,
        modifier = modifier,
        colors = TimePickerDefaults.colors(
            clockDialColor = AwanTheme.colors.background,
            clockDialSelectedContentColor = AwanTheme.colors.onSky,
            clockDialUnselectedContentColor = AwanTheme.colors.textPrimary,
            selectorColor = AwanTheme.colors.sky,
            periodSelectorBorderColor = AwanTheme.colors.line,
            periodSelectorSelectedContainerColor = AwanTheme.colors.sky.copy(alpha = 0.2f),
            periodSelectorSelectedContentColor = AwanTheme.colors.sky,
            periodSelectorUnselectedContainerColor = Color.Transparent,
            periodSelectorUnselectedContentColor = AwanTheme.colors.textSecondary,
            timeSelectorSelectedContainerColor = AwanTheme.colors.sky.copy(alpha = 0.2f),
            timeSelectorSelectedContentColor = AwanTheme.colors.sky,
            timeSelectorUnselectedContainerColor = AwanTheme.colors.background,
            timeSelectorUnselectedContentColor = AwanTheme.colors.textPrimary,
        )
    )
}

@Preview(name = "TimePicker · Light", showBackground = true)
@Composable
private fun LightTimePickerPreview() {
    TimePickerPreview(dark = false)
}

@Preview(name = "TimePicker · Dark", showBackground = true)
@Composable
private fun DarkTimePickerPreview() {
    TimePickerPreview(dark = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val state = rememberTimePickerState()
            AwanTimePicker(state = state)
        }
    }
}
