package com.awan.feature.goals.impl.ui.components

import androidx.compose.ui.graphics.Color

/** Accent colors — each goal card gets a distinct hue, assigned by list index. */
internal val GoalColorPalette = listOf(
    Color(0xFF7A64FF), // violet / purple
    Color(0xFFFF9838), // tangerine / orange
    Color(0xFF2EAAFF), // sky / blue
    Color(0xFFFF6F91), // coral / pink
    Color(0xFFFFC233), // sun / yellow
    Color(0xFF9A7BFF), // lavender
)

internal fun goalAccentColor(index: Int): Color =
    GoalColorPalette[index % GoalColorPalette.size]

/** Gold color used for every card on the Completed tab. */
internal val CompletedGoalColor = Color(0xFFFFC233)
