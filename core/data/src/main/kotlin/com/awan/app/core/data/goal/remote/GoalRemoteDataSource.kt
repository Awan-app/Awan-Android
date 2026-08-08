package com.awan.app.core.data.goal.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse
import com.awan.app.core.network.dto.goal.ScheduleGoalRequest
import com.awan.app.core.network.dto.task.TaskScheduleResponse

interface GoalRemoteDataSource {
    suspend fun getGoals(): Result<List<GoalInfoResponse>>
    suspend fun createGoal(request: CreateGoalRequest): Result<GoalInfoResponse>
    suspend fun getInboxGoal(): Result<GoalInfoResponse>
    suspend fun getGoal(goalId: String): Result<GoalInfoResponse>
    suspend fun deleteGoal(goalId: String): Result<Unit>
    suspend fun continueDecomposition(request: GoalDecomposeRequest): Result<GoalDecomposeResponse>
    suspend fun confirmDecomposition(sessionId: String): Result<GoalInfoResponse>
    suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscriptResponse>
    suspend fun cancelDecomposition(sessionId: String): Result<Unit>
    suspend fun scheduleGoal(goalId: String): Result<TaskScheduleResponse>
    suspend fun proposeGoalSchedule(goalId: String): Result<AiGoalScheduleProposalResponse>
    suspend fun confirmGoalSchedule(request: ConfirmAiScheduleRequest): Result<Unit>
}

