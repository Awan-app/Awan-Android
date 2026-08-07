package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of a task category.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String? = null,
    val icon: String? = null,
)
