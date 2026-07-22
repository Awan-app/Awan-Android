package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "SENDING...",
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
) {
    val effectiveEnabled = enabled && !isLoading
    val semanticsModifier = Modifier.semantics {
        stateDescription = when {
            isLoading -> "Loading"
            !enabled -> "Disabled"
            else -> "Enabled"
        }
    }

    AwanButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(semanticsModifier),
        variant = variant,
        enabled = effectiveEnabled,
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AwanTheme.colors.onFilledControl,
                    strokeWidth = 2.dp,
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(AwanTheme.spacing.xs),
                )
                AwanText(text = loadingText)
            }
        } else {
            AwanText(text = text)
        }
    }
}
