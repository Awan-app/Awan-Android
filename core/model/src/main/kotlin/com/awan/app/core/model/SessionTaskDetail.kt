package com.awan.app.core.model

import java.time.LocalDateTime

data class SessionDetailInfo(
    val id: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: SessionStatus,
    val locked: Boolean,
    val zoneId: String?,
    val taskId: String,
)

data class TaskDetailInfo(
    val id: String,
    val title: String,
    val description: String?,
    val estimatedDuration: Int?,
    val status: TaskStatus,
    val mandatory: Boolean,
    val estimatedPoints: Int,
    val allowTaskSplitting: Boolean,
    val goalId: String?,
    val categoryName: String?,
    val dependsOnTaskIds: List<String>,
)

data class SessionTaskDetail(
    val session: SessionDetailInfo,
    val task: TaskDetailInfo,
    val relatedSessions: List<SessionDetailInfo> = emptyList(),
)
