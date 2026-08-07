package com.awan.app.core.model

data class SessionDetailInfo(
    val id: String,
    val start: String,
    val end: String,
    val status: String,
    val locked: Boolean,
    val zoneId: String?,
    val taskId: String,
)

data class TaskDetailInfo(
    val id: String,
    val title: String,
    val description: String?,
    val estimatedDuration: Int?,
    val status: String,
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
)
