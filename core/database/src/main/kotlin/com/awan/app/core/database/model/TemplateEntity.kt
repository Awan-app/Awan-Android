package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of a weekly schedule template.
 *
 * Maps to [TemplateResponse] from the Awan API.
 * The `daysOfWeek` set is stored separately in [TemplateDayOfWeekEntity] to
 * avoid a type converter and to support indexed queries by day.
 * Zones belonging to this template are stored in [ZoneEntity] with
 * [ZoneEntity.templateId] set.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
)
