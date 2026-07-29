package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTimePicker
import com.awan.feature.profile.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                AwanText(
                    text = stringResource(R.string.profile_zone_confirm),
                    style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = stringResource(R.string.profile_cancel), style = AwanTheme.styles.bodyText)
            }
        },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AwanTimePicker(state = state)
            }
        },
        containerColor = AwanTheme.colors.background
    )
}
