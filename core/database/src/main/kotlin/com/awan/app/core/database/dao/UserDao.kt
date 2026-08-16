package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // ── UserEntity ────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("SELECT MIN(expiryTime) FROM users LIMIT 1")
    suspend fun getMinExpiryTime(): Long?

    // ── UserPreferencesEntity ─────────────────────────────────────────────────

    @Upsert
    suspend fun upsertPreferences(preferences: UserPreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun observePreferences(userId: String): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getPreferences(userId: String): UserPreferencesEntity?

    // ── Combined ──────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?>

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithPreferences(userId: String): UserWithPreferences?

    /**
     * Atomically persists both the user profile and their preferences.
     * Called after every API response that returns a full [UserProfileResponse].
     */
    @Transaction
    suspend fun upsertUserWithPreferences(
        user: UserEntity,
        preferences: UserPreferencesEntity,
    ) {
        upsertUser(user)
        upsertPreferences(preferences)
    }
}
