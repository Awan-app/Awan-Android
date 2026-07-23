package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User scheduling and session preferences.
 *
 * Maps to the `preferences` object embedded in [UserProfileResponse].
 * Stored as a separate table with a 1-to-1 relationship to [UserEntity]
 * so each preference group can be updated atomically without touching the
 * user row.
 */
@Entity(
    tableName = "user_preferences",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class UserPreferencesEntity(
    /** Foreign key and primary key — 1 row per user. */
    @PrimaryKey val userId: String,
    /** IANA timezone string (e.g. "Africa/Cairo"). */
    val timezone: String,
    /** Preferred work-session length in minutes. */
    val preferredSessionDuration: Int,
    /** Break duration between sessions in minutes. */
    val bufferBetweenSessions: Int,
    /** Wake-up time stored as `HH:mm:ss` string. */
    val wakeupTime: String,
    /** Bed time stored as `HH:mm:ss` string. */
    val sleepTime: String,
    /**
     * Task-scheduling algorithm.
     * One of: `BALANCED`, `EASIEST_FIRST`, `HARDEST_FIRST`.
     */
    val schedulingType: String,
)
