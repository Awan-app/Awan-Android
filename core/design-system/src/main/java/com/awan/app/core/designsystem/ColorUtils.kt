package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

fun String?.toColor(): Color {
    if (this.isNullOrBlank()) return Color.Transparent

    return try {
        val colorString = if (!this.startsWith("#")) "#$this" else this
        Color(colorString.toColorInt())
    } catch (e: Exception) {
        Color.Transparent
    }
}

fun Color.toHexString(): String {
    return String.format(
        "#%02X%02X%02X",
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}
