package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of the authenticated user.
 *
 * Maps to [UserProfileResponse] and [UserProgressResponse] from the Awan API.
 * [preferences] live in a separate 1-to-1 table ([UserPreferencesEntity]) to
 * keep each concern independently updatable.
 */
@Entity(tableName = "users")
data class UserEntity(
    /** UUID returned by the server (stored as String to avoid UUID ↔ String converter overhead). */
    @PrimaryKey val id: String,
    val email: String,
    /** Null until onboarding is completed. */
    val firstName: String?,
    /** Null until onboarding is completed. */
    val lastName: String?,
    /** ISO date `YYYY-MM-DD` or null. */
    val birthDate: String?,
    /** Cumulative points from gamification. */
    val points: Int,
    /** Current consecutive-day streak. */
    val streak: Int,
    /** Highest streak ever achieved. */
    val maxStreak: Int,
    val expiryTime: Long = 0L,
    /** URL to the user's profile picture. */
    val profilePictureUrl: String? = null,
    /** Whether the user is new and hasn't completed onboarding. */
    val isNew: Boolean = false,
)
