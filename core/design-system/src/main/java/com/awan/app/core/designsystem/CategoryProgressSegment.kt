package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.Color

data class CategoryProgressSegment(
    val color: Color,
    val weight: Float = 1f,
    val isCompleted: Boolean = false,
)
