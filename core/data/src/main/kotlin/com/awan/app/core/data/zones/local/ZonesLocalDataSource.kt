package com.awan.app.core.data.zones.local

import androidx.room.withTransaction
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only writer of `templates`, `template_days_of_week`, `template_overrides` and `zones`.
 * Everything else reads them. One writer is what makes "nothing deleted elsewhere survives here"
 * a property of a function rather than something every call site has to remember.
 */
interface ZonesLocalDataSource {

    /**
     * Replaces the whole zone model with what the server returned. Both endpoints return complete
     * lists, so the whole table is the authoritative scope.
     *
     * Callers must have both responses in hand: a partial replace would delete the half it could
     * not refetch.
     */
    suspend fun replaceAll(
        templates: List<WeeklyTemplateDto>,
        overrides: List<TemplateOverrideDto>,
        expiryTime: Long,
    )
}

@Singleton
class ZonesLocalDataSourceImpl @Inject constructor(
    private val database: AwanDatabase,
    private val templateDao: TemplateDao,
    private val templateOverrideDao: TemplateOverrideDao,
    private val zoneDao: ZoneDao,
) : ZonesLocalDataSource {

    override suspend fun replaceAll(
        templates: List<WeeklyTemplateDto>,
        overrides: List<TemplateOverrideDto>,
        expiryTime: Long,
    ) = database.withTransaction {
        // Zones and day assignments CASCADE from their parents, so these two deletes clear every
        // table involved — including the rows the server no longer returns.
        templateDao.deleteAllTemplates()
        templateOverrideDao.deleteAllOverrides()

        templateDao.upsertTemplates(
            templates.map { TemplateEntity(id = it.id, name = it.name, expiryTime = expiryTime) }
        )
        templateDao.upsertDays(
            templates.flatMap { template ->
                template.daysOfWeek.map { TemplateDayOfWeekEntity(dayOfWeek = it, templateId = template.id) }
            }
        )
        templateOverrideDao.upsertOverrides(
            overrides.map { TemplateOverrideEntity(id = it.id, name = it.name, dateOfDay = it.dateOfDay) }
        )

        val templateZones = templates.flatMap { template ->
            template.zones.mapNotNull { it.toEntity(templateId = template.id) }
        }
        val overrideZones = overrides.flatMap { override ->
            override.zones.mapNotNull { it.toEntity(overrideId = override.id) }
        }
        zoneDao.upsertZones(templateZones + overrideZones)
    }
}

private fun com.awan.app.core.network.dto.zone.ZoneDto.toEntity(
    templateId: String? = null,
    overrideId: String? = null,
): ZoneEntity? = id?.let {
    ZoneEntity(
        id = it,
        name = name,
        startTime = startTime,
        endTime = endTime,
        color = color,
        templateId = templateId,
        templateOverrideId = overrideId,
    )
}
