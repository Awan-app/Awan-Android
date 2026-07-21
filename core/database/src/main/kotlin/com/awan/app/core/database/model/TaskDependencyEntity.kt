package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for task-to-task prerequisite relationships.
 *
 * Each row encodes "[taskId] depends on [dependsOnTaskId]", i.e.
 * [dependsOnTaskId] must be completed before [taskId] can start.
 *
 * Both tasks must belong to the same goal (enforced by the server;
 * the client mirrors the constraint via repository-level validation).
 *
 * The server returns the dependency list as `dependsOnTaskIds: UUID[]` inside
 * [TaskInfoResponse] — that flat list is expanded into individual rows here.
 */
@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["taskId", "dependsOnTaskId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("dependsOnTaskId")],
)
data class TaskDependencyEntity(
    /** The task that has a prerequisite. */
    val taskId: String,
    /** The task that must be completed first. */
    val dependsOnTaskId: String,
)
