package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

enum class NoticeTone { Error, Warning }

/** Friendly inline error/warning: a colored badge + a plain sentence (never bare red text). */
@Composable
fun InlineNotice(text: String, tone: NoticeTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        NoticeTone.Error -> AwanTheme.colors.destructive
        NoticeTone.Warning -> AwanTheme.colors.zoneTangerine
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.text.BasicText(
                text = "!",
                style = AwanTheme.typography.caption.copy(color = Color.White, fontWeight = FontWeight.ExtraBold),
            )
        }
        AwanText(text, style = AwanTheme.styles.captionText)
    }
}
