package com.awan.app.core.model

import java.time.LocalDateTime

data class TaskDraft(
    val title: String,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val mandatory: Boolean = false,
    val estimatedPoints: Int = 0,
    val allowTaskSplitting: Boolean = false,
    val categoryId: String? = null,
    val goalId: String? = null,
    val startAt: LocalDateTime? = null,
    val categoryToken: String? = null,
) {
    companion object {
        const val DEFAULT_DURATION_MINUTES = 45
    }
}
