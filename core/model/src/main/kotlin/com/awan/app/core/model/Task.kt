package com.awan.app.core.model

enum class TaskStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,

    /** The backend sent a status this build doesn't know about. */
    UNKNOWN,
}

/**
 * A unit of work. Carries only what the task *is* — when it happens lives on its [TaskSession]s.
 * A task with a null [goalId] belongs to the user's Inbox.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val status: TaskStatus = TaskStatus.SCHEDULED,
    val mandatory: Boolean = true,
    val estimatedPoints: Int = 0,
    val allowTaskSplitting: Boolean = false,
    val goalId: String? = null,
    val dependsOnTaskIds: List<String> = emptyList(),
    val category: Category? = null,
)
