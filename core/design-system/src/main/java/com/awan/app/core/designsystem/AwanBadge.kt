package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AwanBadgeTone { Sky, Violet, Tangerine, Neutral }

/** Small sky-tinted pill for reassurances and hints; a readout, not a control — see [AwanChip] for that. */
@Composable
fun AwanBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AwanBadgeTone = AwanBadgeTone.Sky,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = AwanTheme.colors
    val (fg, bg) = when (tone) {
        AwanBadgeTone.Sky -> colors.skyPressed to colors.sky.copy(alpha = 0.12f)
        AwanBadgeTone.Violet -> colors.zoneVioletPressed to colors.zoneViolet.copy(alpha = 0.14f)
        AwanBadgeTone.Tangerine -> colors.zoneTangerinePressed to colors.zoneTangerine.copy(alpha = 0.14f)
        AwanBadgeTone.Neutral -> colors.textSecondary to colors.disabledSurface
    }
    Row(
        modifier = modifier
            .clip(AwanTheme.shapes.pill)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leadingIcon?.invoke()
        AwanBadgeText(text, fg)
    }
}

@Composable
private fun AwanBadgeText(text: String, color: Color) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = AwanTheme.typography.caption.copy(color = color),
    )
}
