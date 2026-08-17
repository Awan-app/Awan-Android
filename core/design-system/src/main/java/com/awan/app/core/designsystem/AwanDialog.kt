package com.awan.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A centered confirmation dialog following the Skyward design language.
 * This is a generic design-system component.
 */
@Composable
fun AwanDialog(
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
