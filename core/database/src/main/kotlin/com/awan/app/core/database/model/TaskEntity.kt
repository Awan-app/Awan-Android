package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            // Intentionally NO cascade — deleting a goal is an explicit operation
            // handled at the repository layer to avoid accidental data loss.
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("goalId")],
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
    /** Foreign key to the parent [GoalEntity]. */
    val goalId: String,
)
