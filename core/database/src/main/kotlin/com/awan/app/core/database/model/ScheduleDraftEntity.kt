package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_drafts",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ScheduleDraftEntity(
    @PrimaryKey val goalId: String,
    val state: String,
)

