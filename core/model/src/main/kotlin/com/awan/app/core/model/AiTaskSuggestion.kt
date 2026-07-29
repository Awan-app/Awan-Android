package com.awan.app.core.model

/**
 * What Awan proposed for a title-and-note preview. Nothing is persisted yet, so there is no id —
 * confirming it goes through the ordinary [TaskDraft] path instead. [categoryName] carries the AI's
 * own label when the backend echoes one; when it only echoes a [categoryId], the caller resolves the
 * name against its own category list.
 */
data class AiTaskSuggestion(
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val mandatory: Boolean = true,
    val estimatedPoints: Int = 0,
    val allowTaskSplitting: Boolean = false,
    val categoryId: String? = null,
    val categoryName: String? = null,
)
