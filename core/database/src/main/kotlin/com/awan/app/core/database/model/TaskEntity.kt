package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local representation of a task.
 *
 * Maps to [TaskInfoResponse] from the Awan API.
 * Task-to-task dependencies are modelled separately in [TaskDependencyEntity]
 * to avoid storing a variable-length list inside this row.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("goalId"),
        Index("categoryId"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    /** Estimated minutes needed to complete this task. */
    val estimatedDuration: Int,
    /**
     * Task lifecycle status.
     * One of: `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
     */
    val status: String,
    val mandatory: Boolean,
    val estimatedPoints: Int,
    val allowTaskSplitting: Boolean,
    /** Optional reference to the parent [GoalEntity]. Null for Inbox tasks. */
    val goalId: String? = null,
    /** Optional reference to [CategoryEntity]. */
    val categoryId: String? = null,
    val completedAt: String? = null,
    val expiryTime: Long = 0L,
)
