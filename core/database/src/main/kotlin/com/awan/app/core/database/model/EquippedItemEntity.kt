package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipped_items")
data class EquippedItemEntity(
    @PrimaryKey val type: String,
    val itemId: String,
    val equippedAt: String,
    val expiryTime: Long = 0L
)
