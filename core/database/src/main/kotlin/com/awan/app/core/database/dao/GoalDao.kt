package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.awan.app.core.database.model.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Upsert suspend fun upsertGoal(goal: GoalEntity)
    @Upsert suspend fun upsertGoals(goals: List<GoalEntity>)
    @Query("SELECT * FROM goals ORDER BY createdAt DESC") fun observeAllGoals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals ORDER BY createdAt DESC") suspend fun getAllGoals(): List<GoalEntity>
    @Query("SELECT * FROM goals WHERE status = :status ORDER BY createdAt DESC") fun observeGoalsByStatus(status: String): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals WHERE id = :goalId") fun observeGoal(goalId: String): Flow<GoalEntity?>
    @Query("SELECT * FROM goals WHERE id = :goalId") suspend fun getGoal(goalId: String): GoalEntity?
    @Query("DELETE FROM goals WHERE id = :goalId") suspend fun deleteGoal(goalId: String)
    @Query("SELECT MIN(expiryTime) FROM goals") suspend fun getMinExpiryTime(): Long?
}
