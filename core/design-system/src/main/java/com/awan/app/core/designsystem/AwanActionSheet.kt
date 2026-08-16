package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A confirmation action sheet following the Skyward design language: a title, body text, a primary
 * action button, and an optional secondary (dismiss) link beneath it.
 *
 * This is a generic design-system component — it knows nothing about features. Callers provide
 * the copy and the callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl)
                .padding(bottom = AwanTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                icon()
            }
            AwanText(title, style = AwanTheme.styles.titleText)
            AwanText(body, style = AwanTheme.styles.bodySecondaryText)
            AwanButton(
                onClick = onPrimary,
                variant = primaryVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(primaryLabel)
            }
            if (secondaryLabel != null) {
                AwanButton(
                    onClick = onSecondary ?: onDismiss,
                    variant = AwanButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AwanText(secondaryLabel)
                }
            }
        }
    }
}
