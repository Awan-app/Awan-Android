package com.awan.app.core.model

data class TaskSchedule(
    val sessions: List<TaskSession> = emptyList(),
    val unscheduledReason: String? = null,
) {
    val isScheduled: Boolean get() = sessions.isNotEmpty()
}
