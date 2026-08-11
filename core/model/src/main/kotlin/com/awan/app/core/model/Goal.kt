package com.awan.app.core.model

data class Goal(
    val id: String,
    val title: String,
    val description: String? = null,
    val emoji: String,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val tasks: List<Task> = emptyList(),
    val targetDate: String? = null,
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
