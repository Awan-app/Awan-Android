package com.awan.app.core.model

data class Goal(
    val id: String,
    val title: String,
    val targetDate: String? = null,
    val description: String? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val isInbox: Boolean = false,
    val emoji: String? = null,
    val tasks: List<Task> = emptyList(),
) {
    val totalTasks: Int
        get() = tasks.size

    val completedTasks: Int
        get() = tasks.count { it.status == TaskStatus.COMPLETED }

    val progress: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks

    val isCompleted: Boolean
        get() = status == GoalStatus.ACHIEVED
}
