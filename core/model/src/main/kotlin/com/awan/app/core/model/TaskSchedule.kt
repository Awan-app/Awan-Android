package com.awan.app.core.model

/**
 * The outcome of asking the engine to place a task. Not finding a slot is a *successful* call with
 * an empty [sessions] and a populated [unscheduledReason] — callers have to check both, or a task
 * that never landed anywhere will read as scheduled.
 */
data class TaskSchedule(
    val sessions: List<TaskSession> = emptyList(),
    val unscheduledReason: String? = null,
) {
    val isScheduled: Boolean get() = sessions.isNotEmpty() && unscheduledReason == null
}
