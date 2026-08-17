package com.awan.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A confirmation action sheet following the Skyward design language: a title, body text, a primary
 * action button, and an optional secondary (dismiss) link beneath it.
 *
 * This is a generic design-system component — it knows nothing about features. Callers provide
 * the copy and the callbacks.
 */
@Composable
fun AwanActionSheet(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primaryVariant: AwanButtonVariant = AwanButtonVariant.Primary,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    AwanConfirmDialog(
        title = title,
        body = body,
        confirmLabel = primaryLabel,
        onConfirm = onPrimary,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmVariant = primaryVariant,
        dismissLabel = secondaryLabel,
        onDismissClick = onSecondary,
        icon = icon,
    )
}
