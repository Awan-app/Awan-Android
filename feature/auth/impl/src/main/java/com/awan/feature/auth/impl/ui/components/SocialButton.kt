package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText

@Composable
fun SocialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    socialType: AwanButtonVariant = AwanButtonVariant.Google,
    contentDescription: String = text,
) {
    AwanButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        variant = socialType,
        icon = if (painter != null) {
            {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            null
        }
    ) {
        AwanText(text = text)
    }
}
