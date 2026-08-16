package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.awan.app.core.database.model.ScheduleDraftEntity
import com.awan.app.core.database.model.ScheduleDraftSessionEntity
import com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity

@Dao
interface ScheduleDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ScheduleDraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftSessions(sessions: List<ScheduleDraftSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftUnscheduledTasks(tasks: List<ScheduleDraftUnscheduledTaskEntity>)

    @Query("SELECT * FROM schedule_drafts WHERE goalId = :goalId")
    fun observeDraft(goalId: String): kotlinx.coroutines.flow.Flow<ScheduleDraftEntity?>

    @Query("SELECT * FROM schedule_draft_sessions WHERE goalId = :goalId")
    fun observeDraftSessions(goalId: String): kotlinx.coroutines.flow.Flow<List<ScheduleDraftSessionEntity>>

    @Query("SELECT * FROM schedule_draft_unscheduled_tasks WHERE goalId = :goalId")
    fun observeDraftUnscheduledTasks(goalId: String): kotlinx.coroutines.flow.Flow<List<ScheduleDraftUnscheduledTaskEntity>>

    @Query("SELECT * FROM schedule_drafts WHERE goalId = :goalId")
    suspend fun getDraft(goalId: String): ScheduleDraftEntity?

    @Query("SELECT * FROM schedule_draft_sessions WHERE goalId = :goalId")
    suspend fun getDraftSessions(goalId: String): List<ScheduleDraftSessionEntity>

    @Query("SELECT * FROM schedule_draft_unscheduled_tasks WHERE goalId = :goalId")
    suspend fun getDraftUnscheduledTasks(goalId: String): List<ScheduleDraftUnscheduledTaskEntity>

    @Query("SELECT goalId FROM schedule_drafts WHERE state IN ('AWAITING_PROPOSAL', 'READY', 'CONFIRMING') LIMIT 1")
    suspend fun getPendingDraftGoalId(): String?

    @Query("DELETE FROM schedule_draft_sessions WHERE goalId = :goalId")
    suspend fun deleteDraftSessions(goalId: String)

    @Query("DELETE FROM schedule_draft_unscheduled_tasks WHERE goalId = :goalId")
    suspend fun deleteDraftUnscheduledTasks(goalId: String)

    @Query("DELETE FROM schedule_drafts WHERE goalId = :goalId")
    suspend fun deleteDraftEntity(goalId: String)

    @Transaction
    suspend fun deleteDraft(goalId: String) {
        deleteDraftSessions(goalId)
        deleteDraftUnscheduledTasks(goalId)
        deleteDraftEntity(goalId)
    }

    @Transaction
    suspend fun replaceDraft(
        draft: ScheduleDraftEntity,
        sessions: List<ScheduleDraftSessionEntity>,
        unscheduledTasks: List<ScheduleDraftUnscheduledTaskEntity>,
    ) {
        deleteDraft(draft.goalId)
        insertDraft(draft)
        if (sessions.isNotEmpty()) insertDraftSessions(sessions)
        if (unscheduledTasks.isNotEmpty()) insertDraftUnscheduledTasks(unscheduledTasks)
    }

    @Query("UPDATE schedule_drafts SET state = :state WHERE goalId = :goalId")
    suspend fun updateDraftState(goalId: String, state: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDraftIfNotExists(draft: ScheduleDraftEntity)
}