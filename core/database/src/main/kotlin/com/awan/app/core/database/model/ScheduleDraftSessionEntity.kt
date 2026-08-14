package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_draft_sessions",
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
data class ScheduleDraftSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: String,
    val taskId: String,
    val taskTitle: String?,
    val zoneId: String?,
    val start: String,
    val end: String,
    val suggestionType: String?,
    val suggestionReason: String?,
    val overlapTaskTitle: String?,
    val overlapStart: String?,
    val overlapEnd: String?,
    val overlapMandatory: Boolean?,
    val overlapPoints: Int?,
    val isSelected: Boolean,
)
