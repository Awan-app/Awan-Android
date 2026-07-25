package com.awan.feature.profile.impl.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun AwanConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AwanText(text = title, style = AwanTheme.styles.titleText) },
        text = { AwanText(text = text, style = AwanTheme.styles.bodyText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                AwanText(
                    text = confirmText,
                    style = AwanTheme.styles.bodyText.copy(
                        color = if (isDestructive) AwanTheme.colors.destructive else AwanTheme.colors.sky
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Cancel", style = AwanTheme.styles.bodyText)
            }
        },
        containerColor = AwanTheme.colors.surface
    )
}
