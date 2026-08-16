package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_draft_unscheduled_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleDraftEntity::class,
            parentColumns = ["goalId"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("goalId"),
    ],
)
data class ScheduleDraftUnscheduledTaskEntity(
    @PrimaryKey val taskId: String,
    val goalId: String,
    val taskTitle: String,
    val message: String,
)
