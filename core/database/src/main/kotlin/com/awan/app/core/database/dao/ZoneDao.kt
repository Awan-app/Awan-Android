package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.awan.app.core.database.model.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Upsert
    suspend fun upsertZone(zone: ZoneEntity)

    @Upsert
    suspend fun upsertZones(zones: List<ZoneEntity>)

    @Query("SELECT * FROM zones")
    fun observeAllZones(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE id = :zoneId")
    fun observeZone(zoneId: String): Flow<ZoneEntity?>

    @Query("SELECT * FROM zones WHERE id = :zoneId")
    suspend fun getZone(zoneId: String): ZoneEntity?

    /** All zones belonging to a template, ordered by start time. */
    @Query("SELECT * FROM zones WHERE templateId = :templateId ORDER BY startTime ASC")
    fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>>

    /** All zones belonging to a template override, ordered by start time. */
    @Query("SELECT * FROM zones WHERE templateOverrideId = :overrideId ORDER BY startTime ASC")
    fun observeZonesForOverride(overrideId: String): Flow<List<ZoneEntity>>

    /**
     * The zones in effect on a date: the date's override wins, otherwise the template that owns that
     * day-of-week (`MONDAY`…`SUNDAY`), otherwise nothing.
     *
     * One query rather than a composition of three, because Room's invalidation tracker collects the
     * tables named in the subqueries too — so this re-emits on a zone edit, a day reassignment, or a
     * new override. Resolving those with `suspend` lookups instead would produce a Flow that only
     * ever reacts to the `zones` table.
     */
    @Query(
        """
        SELECT * FROM zones
        WHERE templateOverrideId = (
                  SELECT id FROM template_overrides WHERE dateOfDay = :date LIMIT 1
              )
           OR (
                  templateId = (
                      SELECT templateId FROM template_days_of_week WHERE dayOfWeek = :dayOfWeek
                  )
                  AND NOT EXISTS (SELECT 1 FROM template_overrides WHERE dateOfDay = :date)
              )
        ORDER BY startTime ASC
        """
    )
    fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>>

    @Query("DELETE FROM zones WHERE id = :zoneId")
    suspend fun deleteZone(zoneId: String)

    @Query("DELETE FROM zones WHERE templateId = :templateId")
    suspend fun deleteZonesForTemplate(templateId: String)

    @Query("DELETE FROM zones WHERE templateOverrideId = :overrideId")
    suspend fun deleteZonesForOverride(overrideId: String)
}
