package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local representation of a time-block zone.
 *
 * Maps to [ZoneResponse] from the Awan API.
 * A zone belongs to **exactly one** parent: either a [TemplateEntity] or a
 * [TemplateOverrideEntity]. The non-owning FK column is always null.
 *
 * Resolution rule (applied at the repository layer):
 *   1. Override zones take priority over template zones for the same date.
 *   2. Template zones apply when no override exists for that date's day-of-week.
 */
@Entity(
    tableName = "zones",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TemplateOverrideEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateOverrideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId"), Index("templateOverrideId")],
)
data class ZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Start of the time block stored as `HH:mm:ss`. */
    val startTime: String,
    /** End of the time block stored as `HH:mm:ss`. */
    val endTime: String,
    /** Optional hex colour code (e.g. `#4CAF50`). */
    val color: String?,
    /**
     * FK to the parent template.
     * Null when this zone belongs to a [TemplateOverrideEntity].
     */
    val templateId: String?,
    /**
     * FK to the parent override.
     * Null when this zone belongs to a [TemplateEntity].
     */
    val templateOverrideId: String?,
)
