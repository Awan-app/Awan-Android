package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

/** The segmented minute picker used for reminder lead time and snooze length. */
@Composable
fun MinutesOption(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
    ) {
        AwanText(
            text = label,
            style = when {
                !enabled -> AwanTheme.styles.bodySecondaryText.copy(color = AwanTheme.colors.meta)
                isSelected -> AwanTheme.styles.buttonCompactText
                else -> AwanTheme.styles.bodySecondaryText
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
