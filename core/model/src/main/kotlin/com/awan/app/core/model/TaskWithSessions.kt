package com.awan.app.core.model

data class TaskWithSessions(
    val task: Task,
    val sessions: List<TaskSession> = emptyList(),
) {
    val completedCount: Int
        get() = sessions.count { it.status == SessionStatus.COMPLETED }
    
    val totalCount: Int
        get() = sessions.size
    
    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}
