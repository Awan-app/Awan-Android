package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog

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
    Dialog(onDismissRequest = onDismiss) {
        AwanCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(AwanTheme.spacing.md),
            contentPadding = PaddingValues(AwanTheme.spacing.xl)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
            ) {
                if (icon != null) {
                    Box(modifier = Modifier.padding(bottom = AwanTheme.spacing.xs)) {
                        icon()
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)
                ) {
                    AwanText(
                        text = title,
                        style = AwanTheme.styles.titleText,
                        textAlign = TextAlign.Center
                    )
                    AwanText(
                        text = body,
                        style = AwanTheme.styles.bodySecondaryText,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(AwanTheme.spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
                ) {
                    if (secondaryLabel != null) {
                        AwanButton(
                            onClick = onSecondary ?: onDismiss,
                            modifier = Modifier.weight(1f),
                            variant = AwanButtonVariant.Quiet
                        ) {
                            AwanText(secondaryLabel)
                        }
                    }
                    AwanButton(
                        onClick = onPrimary,
                        modifier = Modifier.weight(1f),
                        variant = primaryVariant
                    ) {
                        AwanText(primaryLabel)
                    }
                }
            }
        }
    }
}
