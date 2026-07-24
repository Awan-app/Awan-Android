package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanText

@Composable
fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        AwanText(
            text = label,
            style = if (isSelected) {
                AwanTheme.styles.buttonCompactText
            } else {
                AwanTheme.styles.bodySecondaryText
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
