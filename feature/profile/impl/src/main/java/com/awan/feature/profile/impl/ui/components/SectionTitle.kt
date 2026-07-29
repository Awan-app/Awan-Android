package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanText

@Composable
fun SectionTitle(title: String) {
    AwanText(
        text = title,
        style = AwanTheme.styles.captionText.copy(
            color = AwanTheme.colors.meta,
            textStyle = AwanTheme.typography.caption.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
