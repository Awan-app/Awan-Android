package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local representation of a scheduled task session.
 *
 * Maps to [SessionDto] from the Awan API.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("taskId"),
        Index("zoneId"),
        Index("date"),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val zoneId: String?,
    /** Format: `YYYY-MM-DD` */
    val date: String,
    /** Format: `HH:mm:ss` */
    val startTime: String,
    /** Format: `HH:mm:ss` */
    val endTime: String,
    /** One of: `SCHEDULED`, `COMPLETED`, `CANCELLED` */
    val status: String,
    val locked: Boolean,
)
