package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the `daysOfWeek` set from [TemplateResponse] — one row per weekday, globally.
 *
 * The server enforces that each calendar day belongs to AT MOST ONE template
 * (`DAY_ALREADY_ASSIGNED` error if violated). This is mirrored here by making
 * [dayOfWeek] the sole primary key: the table holds at most 7 rows, one per
 * day of the week, each pointing to its owning template.
 *
 * [dayOfWeek] values: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`,
 *                     `FRIDAY`, `SATURDAY`, `SUNDAY`.
 */
@Entity(
    tableName = "template_days_of_week",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId")],
)
data class TemplateDayOfWeekEntity(
    /** Global PK — at most 7 rows, one per weekday. */
    @PrimaryKey val dayOfWeek: String,
    /** The template that owns this day. */
    val templateId: String,
)
