package com.awan.app.core.data.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.*
import com.awan.app.core.database.model.*
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.UpdateSessionParams
import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskUpdateRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.ZoneDto
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

private class FakeHomeRemoteDataSource : HomeRemoteDataSource {
    var lastUpdate: UpdateArgs? = null
    data class UpdateArgs(val id: String, val status: String?, val locked: Boolean?, val start: String?, val end: String?)

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> = Result.Success(emptyList())
    override suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>> = Result.Success(emptyList())
    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> = Result.Success(emptyList())
    override suspend fun getTemplateOverrides(): Result<List<TemplateOverrideDto>> = Result.Success(emptyList())
    override suspend fun getUserProfile(): Result<CompleteOnboardingResponse> = Result.Error(com.awan.app.core.common.error.AppError.Network)
    override suspend fun getSession(sessionId: String): Result<SessionDto> = Result.Error(com.awan.app.core.common.error.AppError.NotFound)
    override suspend fun getTask(taskId: String): Result<TaskInfoResponse> = Result.Error(com.awan.app.core.common.error.AppError.NotFound)

    override suspend fun completeSession(sessionId: String): Result<CompleteSessionResponse> = error("")
    override suspend fun uncompleteSession(sessionId: String): Result<SessionDto> = error("")
    override suspend fun cancelSession(sessionId: String): Result<SessionDto> = error("")
    override suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<SessionDto> = error("")
    override suspend fun lockSession(sessionId: String): Result<SessionDto> = error("")
    override suspend fun unlockSession(sessionId: String): Result<SessionDto> = error("")
    override suspend fun updateTask(taskId: String, request: TaskUpdateRequest): Result<TaskInfoResponse> = error("")
    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> = Result.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryImplTest {

    private val remoteDataSource = FakeHomeRemoteDataSource()
    private val connectivityMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    // Dummy implementations to satisfy compiler
    private fun dummyUserDao() = object : UserDao {
        override suspend fun upsertUser(user: UserEntity) {}
        override suspend fun getFirstUser(): UserEntity? = null
        override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = flowOf(null)
        override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
        override suspend fun upsertUserWithPreferences(user: UserEntity, preferences: UserPreferencesEntity) {}
        override suspend fun upsertPreferences(preferences: UserPreferencesEntity) {}
        override suspend fun getMinExpiryTime(): Long? = null
        override fun observeUser(userId: String): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUser(userId: String): UserEntity? = null
        override suspend fun deleteUser(userId: String) {}
        override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = flowOf(null)
        override suspend fun getPreferences(userId: String): UserPreferencesEntity? = null
    }
    private fun dummyTaskDao() = object : TaskDao {
        override suspend fun upsertTask(task: TaskEntity) {}
        override suspend fun upsertTasks(tasks: List<TaskEntity>) {}
        override suspend fun getTask(taskId: String): TaskEntity? = null
        override suspend fun deleteTask(taskId: String) {}
        override suspend fun nullifyOrphanedGoalReferences() {}
        override fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
        override fun observeInboxTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
        override fun observeAllTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun getAllTasks(): List<TaskEntity> = emptyList()
        override fun observeTask(taskId: String): Flow<TaskEntity?> = flowOf(null)
        override suspend fun upsertDependency(dependency: TaskDependencyEntity) {}
        override suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>) {}
        override suspend fun deleteDependency(dependency: TaskDependencyEntity) {}
        override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
        override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun deleteAllDependenciesForTask(taskId: String) {}
        override suspend fun deleteTasksByGoal(goalId: String) {}
    }
    private fun dummySessionDao() = object : SessionDao {
        override suspend fun upsertSession(session: SessionEntity) {}
        override suspend fun upsertSessions(sessions: List<SessionEntity>) {}
        override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
        override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> = flowOf(emptyList())
        override suspend fun getSessionsForDate(date: String): List<SessionEntity> = emptyList()
        override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = emptyList()
        override suspend fun getSession(id: String): SessionEntity? = null
        override suspend fun deleteSessionsForDates(dates: List<String>) {}
        override suspend fun deleteSession(id: String) {}
        override suspend fun replaceSessionsForDates(dates: List<String>, sessions: List<SessionEntity>) {}
    }
    private fun dummyZoneDao() = object : ZoneDao {
        override suspend fun upsertZones(zones: List<ZoneEntity>) {}
        override fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
        override fun observeZonesForOverride(overrideId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
        override suspend fun upsertZone(zone: ZoneEntity) {}
        override fun observeZone(zoneId: String): Flow<ZoneEntity?> = flowOf(null)
        override suspend fun getZone(zoneId: String): ZoneEntity? = null
        override suspend fun deleteZone(zoneId: String) {}
        override suspend fun deleteZonesForTemplate(templateId: String) {}
        override suspend fun deleteZonesForOverride(overrideId: String) {}
    }
    private fun dummyTemplateDao() = object : TemplateDao {
        override suspend fun upsertTemplates(templates: List<TemplateEntity>) {}
        override suspend fun upsertDays(days: List<TemplateDayOfWeekEntity>) {}
        override suspend fun getDayAssignment(dayOfWeek: String): TemplateDayOfWeekEntity? = null
        override suspend fun getMinExpiryTime(): Long? = null
        override suspend fun upsertTemplate(template: TemplateEntity) {}
        override fun observeAllTemplates(): Flow<List<TemplateEntity>> = flowOf(emptyList())
        override fun observeTemplate(templateId: String): Flow<TemplateEntity?> = flowOf(null)
        override suspend fun getTemplate(templateId: String): TemplateEntity? = null
        override suspend fun deleteTemplate(templateId: String) {}
        override fun observeDaysForTemplate(templateId: String): Flow<List<TemplateDayOfWeekEntity>> = flowOf(emptyList())
        override suspend fun deleteDaysForTemplate(templateId: String) {}
    }
    private fun dummyTemplateOverrideDao() = object : TemplateOverrideDao {
        override suspend fun upsertOverrides(overrides: List<TemplateOverrideEntity>) {}
        override suspend fun getOverrideForDate(date: String): TemplateOverrideEntity? = null
        override suspend fun upsertOverride(override: TemplateOverrideEntity) {}
        override fun observeAllOverrides(): Flow<List<TemplateOverrideEntity>> = flowOf(emptyList())
        override fun observeOverride(overrideId: String): Flow<TemplateOverrideEntity?> = flowOf(null)
        override suspend fun getOverride(overrideId: String): TemplateOverrideEntity? = null
        override suspend fun deleteOverride(overrideId: String) {}
    }
    private fun dummyCategoryDao() = object : CategoryDao {
        override suspend fun upsertCategory(category: CategoryEntity) {}
        override suspend fun upsertCategories(categories: List<CategoryEntity>) {}
        override suspend fun getCategory(id: String): CategoryEntity? = null
        override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
        override suspend fun getMinExpiryTime(): Long? = null
        override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun deleteCategory(id: String) {}
        override suspend fun deleteAllCategories() {}
    }
}
