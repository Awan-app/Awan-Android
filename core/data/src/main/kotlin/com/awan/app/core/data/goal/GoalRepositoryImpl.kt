package com.awan.app.core.data.goal

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.network.dto.GoalDecomposeRequest
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
) : GoalRepository {

    private val mockGoals = listOf(
        Goal(
            id = "1",
            title = "Learn Kotlin Coroutines",
            emoji = "📚",
            status = GoalStatus.ACTIVE,
            tasks = listOf(
                Task(id = "1-1", title = "Read coroutines guide", status = TaskStatus.COMPLETED, goalId = "1"),
                Task(id = "1-2", title = "Build a Flow example", status = TaskStatus.COMPLETED, goalId = "1"),
                Task(id = "1-3", title = "Understand CoroutineScope", status = TaskStatus.SCHEDULED, goalId = "1"),
                Task(id = "1-4", title = "Handle exceptions", status = TaskStatus.SCHEDULED, goalId = "1"),
            )
        ),
        Goal(
            id = "2",
            title = "Run a 5K",
            emoji = "🏃",
            status = GoalStatus.ACTIVE,
            tasks = listOf(
                Task(id = "2-1", title = "Day 1 — Walk 20 min", status = TaskStatus.COMPLETED, goalId = "2"),
                Task(id = "2-2", title = "Day 2 — Run 1.5 miles", status = TaskStatus.SCHEDULED, goalId = "2"),
                Task(id = "2-3", title = "Day 3 — Jog 15 min", status = TaskStatus.SCHEDULED, goalId = "2"),
            )
        ),
        Goal(
            id = "3",
            title = "Read 12 books this year",
            emoji = "🎉",
            status = GoalStatus.ACHIEVED,
            tasks = listOf(
                Task(id = "3-1", title = "Read book 1", status = TaskStatus.COMPLETED, goalId = "3"),
                Task(id = "3-2", title = "Read book 2", status = TaskStatus.COMPLETED, goalId = "3"),
                Task(id = "3-3", title = "Read book 3", status = TaskStatus.COMPLETED, goalId = "3"),
            )
        ),
        Goal(
            id = "4",
            title = "Launch side project",
            emoji = "🚀",
            status = GoalStatus.ACHIEVED,
            tasks = listOf(
                Task(id = "4-1", title = "Write app code", status = TaskStatus.COMPLETED, goalId = "4"),
                Task(id = "4-2", title = "Publish to Play Store", status = TaskStatus.COMPLETED, goalId = "4"),
            )
        )
    )

    override suspend fun getGoals(): Result<List<Goal>> =
        try {
            remoteDataSource.getGoals().map { networkGoals ->
                val list = networkGoals.map { it.toModel() }
                if (list.isEmpty()) mockGoals else list
            }
        } catch (e: Exception) {
            Result.Success(mockGoals)
        }

    override suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> =
        remoteDataSource.continueDecomposition(
            GoalDecomposeRequest(sessionId = sessionId, message = message),
        ).map { it.toDecompositionReply() }

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> =
        remoteDataSource.confirmDecomposition(sessionId).map { it.toModel() }
}
