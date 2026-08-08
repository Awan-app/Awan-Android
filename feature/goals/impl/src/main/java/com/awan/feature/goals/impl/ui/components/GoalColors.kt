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
    return palette[index % palette.size]
}

/** Gold color used for every card on the Completed tab. */
@Composable
internal fun completedGoalColor(): Color = AwanTheme.colors.zoneSun
