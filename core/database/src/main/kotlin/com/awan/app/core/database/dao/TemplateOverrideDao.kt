package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.awan.app.core.database.model.TemplateOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateOverrideDao {

    @Upsert
    suspend fun upsertOverride(override: TemplateOverrideEntity)

    @Upsert
    suspend fun upsertOverrides(overrides: List<TemplateOverrideEntity>)

    @Query("SELECT * FROM template_overrides ORDER BY dateOfDay ASC")
    fun observeAllOverrides(): Flow<List<TemplateOverrideEntity>>

    @Query("SELECT * FROM template_overrides WHERE id = :overrideId")
    fun observeOverride(overrideId: String): Flow<TemplateOverrideEntity?>

    @Query("SELECT * FROM template_overrides WHERE id = :overrideId")
    suspend fun getOverride(overrideId: String): TemplateOverrideEntity?

    /** Find the override for a specific date (`YYYY-MM-DD`), if one exists. */
    @Query("SELECT * FROM template_overrides WHERE dateOfDay = :date LIMIT 1")
    suspend fun getOverrideForDate(date: String): TemplateOverrideEntity?

    @Query("DELETE FROM template_overrides WHERE id = :overrideId")
    suspend fun deleteOverride(overrideId: String)
}
