package com.awan.app.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Combined model for user profile and their preferences.
 * Used for offline-first data retrieval.
 */
data class UserWithPreferences(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val preferences: UserPreferencesEntity?
)
