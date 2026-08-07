package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks dates whose task schedule has been fetched from the server.
 * Allows distinguishing between an empty scheduled day and an uncached day.
 */
@Entity(tableName = "cached_schedule_dates")
data class CachedScheduleDateEntity(
    /** Format: `YYYY-MM-DD` */
    @PrimaryKey val date: String,
    /** ISO-8601 string of when this date was last synced. */
    val lastSyncedAt: String,
)
