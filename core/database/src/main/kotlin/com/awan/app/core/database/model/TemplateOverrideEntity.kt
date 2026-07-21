package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of a one-off date override for the schedule.
 *
 * Maps to [TemplateOverrideResponse] from the Awan API.
 * When both a [TemplateEntity] and a [TemplateOverrideEntity] match a given
 * date, the override takes priority (server rule, mirrored locally).
 * Zones for this override are stored in [ZoneEntity] with
 * [ZoneEntity.templateOverrideId] set.
 */
@Entity(tableName = "template_overrides")
data class TemplateOverrideEntity(
    @PrimaryKey val id: String,
    /** Optional display name for the override (e.g. "Holiday Schedule"). */
    val name: String?,
    /** The specific date this override applies to, stored as `YYYY-MM-DD`. */
    val dateOfDay: String,
)
