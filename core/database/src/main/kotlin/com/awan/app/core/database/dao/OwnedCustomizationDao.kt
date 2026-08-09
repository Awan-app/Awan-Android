package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.OwnedCustomizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnedCustomizationDao {
    @Query("SELECT * FROM owned_customizations WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<OwnedCustomizationEntity>>

    @Upsert
    suspend fun upsertAll(customizations: List<OwnedCustomizationEntity>)

    @Query("DELETE FROM owned_customizations WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Query(
        "UPDATE owned_customizations SET isEquipped = CASE WHEN itemId = :itemId THEN 1 ELSE 0 END " +
            "WHERE userId = :userId AND type = :type",
    )
    suspend fun setEquipped(userId: String, type: String, itemId: String)

    @Transaction
    suspend fun replaceForUser(userId: String, customizations: List<OwnedCustomizationEntity>) {
        deleteForUser(userId)
        if (customizations.isNotEmpty()) upsertAll(customizations)
    }
}
