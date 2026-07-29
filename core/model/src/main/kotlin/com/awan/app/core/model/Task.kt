package com.awan.app.core.model

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int = 0,
    val status: TaskStatus = TaskStatus.UNKNOWN,
    val mandatory: Boolean = false,
    val estimatedPoints: Int = 0,
    val allowTaskSplitting: Boolean = false,
    val goalId: String? = null,
    val dependsOnTaskIds: List<String> = emptyList(),
    val category: Category? = null,
)
