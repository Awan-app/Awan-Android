package com.awan.app.core.data.home

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.home.repository.HomeRepositoryImpl
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHomeRemoteDataSource : HomeRemoteDataSource {
    var sessionResult: Result<SessionDto> = Result.Error(AppError.Unknown())
    var taskResult: Result<TaskInfoResponse> = Result.Error(AppError.Unknown())

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> = Result.Success(emptyList())
    override suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>> = Result.Success(emptyList())
    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> = Result.Success(emptyList())
    override suspend fun getTemplateOverrides(): Result<List<TemplateOverrideDto>> = Result.Success(emptyList())
    override suspend fun getUserProfile(): Result<CompleteOnboardingResponse> = Result.Error(AppError.Unknown())
    override suspend fun getSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun getTask(taskId: String): Result<TaskInfoResponse> = taskResult
    override suspend fun updateSession(
        sessionId: String,
        status: String?,
        locked: Boolean?,
        startIso: String?,
        endIso: String?,
    ): Result<SessionDto> = Result.Error(AppError.Unknown())

    override suspend fun lockSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun unlockSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest,
    ): Result<TaskInfoResponse> = taskResult
    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> = Result.Success(Unit)
}

private class FakeUserDao : UserDao {
    override suspend fun upsertUser(user: UserEntity) {}
    override fun observeUser(userId: String): Flow<UserEntity?> = flowOf(null)
    override suspend fun getUser(userId: String): UserEntity? = null
    override suspend fun getFirstUser(): UserEntity? = null
    override suspend fun deleteUser(userId: String) {}
    override suspend fun upsertPreferences(preferences: UserPreferencesEntity) {}
    override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = flowOf(null)
    override suspend fun getPreferences(userId: String): UserPreferencesEntity? = null
    override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = flowOf(null)
    override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
}

class HomeRepositoryImplTest {

    @Test
    fun `getSessionDetail fetches session and task sequentially and returns success`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val repository = HomeRepositoryImpl(fakeRemote, FakeUserDao())

        val sessionId = "session-123"
        val taskId = "task-456"

        fakeRemote.sessionResult = Result.Success(
            SessionDto(
                id = sessionId,
                start = "2026-08-05T09:00:00",
                end = "2026-08-05T09:30:00",
                status = "SCHEDULED",
                locked = false,
                zoneId = "zone-789",
                taskId = taskId,
            )
        )

        fakeRemote.taskResult = Result.Success(
            TaskInfoResponse(
                id = taskId,
                title = "Study French Vocabulary",
                description = "Review 50 new words",
                estimatedDuration = 30,
                status = "SCHEDULED",
                mandatory = true,
                estimatedPoints = 10,
                allowTaskSplitting = false,
                goalId = "goal-000",
                category = CategoryDto(id = "cat-1", name = "Language Learning"),
                dependsOnTaskIds = emptyList(),
            )
        )

        val result = repository.getSessionDetail(sessionId)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(sessionId, data.session.id)
        assertEquals(taskId, data.task.id)
        assertEquals("Study French Vocabulary", data.task.title)
        assertEquals("Language Learning", data.task.categoryName)
        assertEquals(true, data.task.mandatory)
    }

    @Test
    fun `getSessionDetail returns error if session call fails`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val repository = HomeRepositoryImpl(fakeRemote, FakeUserDao())

        val sessionId = "session-error"
        val error = AppError.Network
        fakeRemote.sessionResult = Result.Error(error)

        val result = repository.getSessionDetail(sessionId)

        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }
}
