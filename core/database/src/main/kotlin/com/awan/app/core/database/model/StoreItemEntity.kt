package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_items")
data class StoreItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val image: String,
    val info: String?,
    val price: Int,
    val version: String,
    val type: String, // StoreItemType.name
    val expiryTime: Long = 0L
)
