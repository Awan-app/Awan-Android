package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TaskCategory(
    val id: String,
    val name: String,
    val color: Color,
    val containerColor: Color,
    val borderColor: Color,
) {
    companion object {
        val Study = TaskCategory(
            id = "study",
            name = "STUDY",
            color = Color(0xFF2E8BFF),
            containerColor = Color(0xFFEFF6FF),
            borderColor = Color(0xFFBFDBFE),
        )
        val Work = TaskCategory(
            id = "work",
            name = "WORK",
            color = Color(0xFF8B5CF6),
            containerColor = Color(0xFFFAF5FF),
            borderColor = Color(0xFFE9D5FF),
        )
        val Personal = TaskCategory(
            id = "personal",
            name = "PERSONAL",
            color = Color(0xFF16A34A),
            containerColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFFBBF7D0),
        )
        val Play = TaskCategory(
            id = "play",
            name = "PLAY",
            color = Color(0xFFEA580C),
            containerColor = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFED7AA),
        )

        val defaultCategories = listOf(Study, Work, Personal, Play)
    }
}
