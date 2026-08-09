package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.StoreItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Upsert suspend fun upsertStoreItems(items: List<StoreItemEntity>)
    @Query("SELECT * FROM store_items") fun observeStoreItems(): Flow<List<StoreItemEntity>>
    @Query("SELECT * FROM store_items WHERE type = :type") fun observeStoreItemsByType(type: String): Flow<List<StoreItemEntity>>
    @Query("DELETE FROM store_items") suspend fun deleteAllStoreItems()

    @Upsert suspend fun upsertOwnedItems(items: List<OwnedItemEntity>)
    @Query("SELECT * FROM owned_items") fun observeOwnedItems(): Flow<List<OwnedItemEntity>>
    @Query("DELETE FROM owned_items") suspend fun deleteAllOwnedItems()

    @Upsert suspend fun upsertEquippedItems(items: List<EquippedItemEntity>)
    @Query("SELECT * FROM equipped_items") fun observeEquippedItems(): Flow<List<EquippedItemEntity>>
    @Query("DELETE FROM equipped_items") suspend fun deleteAllEquippedItems()
    @Query("DELETE FROM equipped_items WHERE type = :type") suspend fun deleteEquippedItemByType(type: String)

    @Transaction
    suspend fun replaceStoreItems(items: List<StoreItemEntity>) {
        deleteAllStoreItems()
        upsertStoreItems(items)
    }

    @Transaction
    suspend fun replaceOwnedItems(items: List<OwnedItemEntity>) {
        deleteAllOwnedItems()
        upsertOwnedItems(items)
    }

    @Transaction
    suspend fun replaceEquippedItems(items: List<EquippedItemEntity>) {
        deleteAllEquippedItems()
        upsertEquippedItems(items)
    }

    @Query("SELECT MIN(expiryTime) FROM store_items") suspend fun getMinExpiryTime(): Long?
}
