package com.awan.app.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A yes/no question asked *over* whatever raised it.
 *
 * Distinct from [AwanActionSheet] on purpose: that is a bottom sheet, and a bottom sheet cannot
 * confirm something for another bottom sheet — the first has to leave before the second arrives, so
 * the user watches their work vanish and only then gets asked whether to discard it. A dialog is its
 * own window and sits in front, which is the only arrangement where the question makes sense.
 */
@Composable
fun AwanConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmVariant: AwanButtonVariant = AwanButtonVariant.Primary,
    dismissLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            AwanButton(onClick = onConfirm, variant = confirmVariant) {
                AwanText(confirmLabel)
            }
        },
        dismissButton = dismissLabel?.let {
            {
                AwanButton(onClick = onDismiss, variant = AwanButtonVariant.Quiet) {
                    AwanText(it, style = AwanTheme.styles.skipLink)
                }
            }
        },
        containerColor = AwanTheme.colors.surface,
        title = { AwanText(title, style = AwanTheme.styles.titleText) },
        text = { AwanText(body, style = AwanTheme.styles.bodySecondaryText) },
    )
}
