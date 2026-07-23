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

    @Query("DELETE FROM zones WHERE id = :zoneId")
    suspend fun deleteZone(zoneId: String)

    @Query("DELETE FROM zones WHERE templateId = :templateId")
    suspend fun deleteZonesForTemplate(templateId: String)

    @Query("DELETE FROM zones WHERE templateOverrideId = :overrideId")
    suspend fun deleteZonesForOverride(overrideId: String)
}
