package com.awan.feature.goals.impl.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.awan.app.core.designsystem.AwanTheme

/** Accent colors — each goal card gets a distinct hue, assigned by list index. */
@Composable
internal fun goalAccentColors(): List<Color> = with(AwanTheme.colors) {
    listOf(
        zoneViolet,    // violet / purple
        zoneTangerine, // tangerine / orange
        zoneSky,       // sky / blue
        zoneCoral,     // coral / pink
        zoneSun,       // sun / yellow
        zoneLavender,  // lavender
    )
}

@Composable
internal fun goalAccentColor(index: Int): Color {
    val palette = goalAccentColors()
    if (palette.isEmpty()) return Color.Transparent
    val safeIndex = (index % palette.size).let { if (it < 0) it + palette.size else it }
    return palette[safeIndex]
}
