package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus

// ─── Task display status ─────────────────────────────────────────────────────

enum class InboxTaskDisplayStatus {
    Drafted,
    Active,
    Completed,
    Cancelled,
}

fun Task.deriveDisplayStatus(): InboxTaskDisplayStatus = when (status) {
    TaskStatus.INBOX, TaskStatus.UNKNOWN -> InboxTaskDisplayStatus.Drafted
    TaskStatus.SCHEDULED -> InboxTaskDisplayStatus.Active
    TaskStatus.COMPLETED -> InboxTaskDisplayStatus.Completed
    TaskStatus.CANCELLED -> InboxTaskDisplayStatus.Cancelled
    TaskStatus.TELEPORTED -> InboxTaskDisplayStatus.Active
}

// ─── UI model ─────────────────────────────────────────────────────────────────

data class InboxTaskUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val displayStatus: InboxTaskDisplayStatus = InboxTaskDisplayStatus.Drafted,
)
