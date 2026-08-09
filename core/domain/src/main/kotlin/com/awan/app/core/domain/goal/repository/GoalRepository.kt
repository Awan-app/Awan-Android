package com.awan.app.core.domain.goal.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalDecompositionTranscript
import com.awan.app.core.model.GoalScheduleProposal
import com.awan.app.core.model.ProposedGoalSession

import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    suspend fun getGoals(): Result<List<Goal>>
    suspend fun createGoal(title: String, description: String?, targetDate: String?): Result<Goal>
    suspend fun getInboxGoal(): Result<Goal>
    suspend fun getGoal(goalId: String): Result<Goal>
    suspend fun deleteGoal(goalId: String): Result<Unit>
    suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply>
    suspend fun confirmDecomposition(sessionId: String): Result<Goal>
    suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscript>
    suspend fun cancelDecomposition(sessionId: String): Result<Unit>
    suspend fun scheduleGoal(goalId: String): Result<Unit>
    suspend fun proposeGoalSchedule(goalId: String): Result<GoalScheduleProposal>
    suspend fun confirmGoalSchedule(goalId: String, sessions: List<ProposedGoalSession>): Result<Unit>
}

