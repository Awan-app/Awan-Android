package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val boughtAt: String,
    val expiryTime: Long = 0L,
    val isSeen: Boolean = false,
)

