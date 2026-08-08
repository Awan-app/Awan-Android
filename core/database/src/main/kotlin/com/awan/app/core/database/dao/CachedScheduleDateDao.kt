package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.awan.app.core.database.model.CachedScheduleDateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedScheduleDateDao {

    @Upsert
    suspend fun upsertCachedDate(cachedDate: CachedScheduleDateEntity)

    @Upsert
    suspend fun upsertCachedDates(cachedDates: List<CachedScheduleDateEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM cached_schedule_dates WHERE date = :date)")
    suspend fun isDateCached(date: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cached_schedule_dates WHERE date = :date)")
    fun observeIsDateCached(date: String): Flow<Boolean>

    @Query("SELECT date FROM cached_schedule_dates WHERE date >= :startDate AND date <= :endDate")
    suspend fun getCachedDatesInRange(startDate: String, endDate: String): List<String>

    @Query("DELETE FROM cached_schedule_dates")
    suspend fun clearAll()

    @Query("SELECT MIN(expiryTime) FROM cached_schedule_dates WHERE date >= :startDate AND date <= :endDate")
    suspend fun getMinExpiryTimeForRange(startDate: String, endDate: String): Long?
}
