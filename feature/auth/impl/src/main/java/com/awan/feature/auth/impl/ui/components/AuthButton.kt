package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText

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
        enabled = enabled,
        isLoading = isLoading,
    ) {
        AwanText(text = if (isLoading) loadingText else text)
    }
}
