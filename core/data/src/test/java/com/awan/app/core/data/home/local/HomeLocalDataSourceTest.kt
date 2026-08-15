package com.awan.app.core.data.home.local

import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.UpcomingSessionRow
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.network.dto.session.SessionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLocalDataSourceTest {

    private val sessionDao = FakeSessionDao()
    private val taskDao = FakeTaskDao()
    private val local = HomeLocalDataSourceImpl(
        userDao = FakeUserDao(),
        taskDao = taskDao,
        sessionDao = sessionDao,
        zoneDao = FakeZoneDao(),
        categoryDao = FakeCategoryDao(),
    )

    private val cached = SessionEntity(
        id = "s1",
        taskId = "t1",
        zoneId = "z1",
        date = "2026-08-09",
        startTime = "09:00:00",
        endTime = "10:00:00",
        status = "SCHEDULED",
        locked = false,
    )

    /**
     * The reported bug. The API sends offset-free timestamps; the mapper used to fail to parse them
     * and fall back to the *stored* time, so a moved session was rewritten unchanged — the card
     * snapped back on screen and Room never moved.
     */
    @Test
    fun `a moved session stores the new times`() = runTest {
        sessionDao.rows["s1"] = cached

        val applied = local.cacheSession(
            SessionDto(id = "s1", start = "2026-08-09T14:30:00", end = "2026-08-09T15:30:00")
        )

        assertTrue(applied)
        assertEquals("14:30:00", sessionDao.rows.getValue("s1").startTime)
        assertEquals("15:30:00", sessionDao.rows.getValue("s1").endTime)
    }

    @Test
    fun `a session moved to another day changes its date`() = runTest {
        sessionDao.rows["s1"] = cached

        local.cacheSession(SessionDto(id = "s1", start = "2026-08-10T09:00:00", end = "2026-08-10T10:00:00"))

        assertEquals("2026-08-10", sessionDao.rows.getValue("s1").date)
    }

    /** Moving a completed session must not silently reopen it. */
    @Test
    fun `a response without a status keeps the stored one`() = runTest {
        sessionDao.rows["s1"] = cached.copy(status = "COMPLETED")

        local.cacheSession(SessionDto(id = "s1", start = "2026-08-09T14:30:00", end = "2026-08-09T15:30:00"))

        assertEquals("COMPLETED", sessionDao.rows.getValue("s1").status)
    }

    @Test
    fun `caching a session this device never synced reports that it did nothing`() = runTest {
        assertFalse(local.cacheSession(SessionDto(id = "unknown", start = START, end = END)))
        assertTrue(sessionDao.rows.isEmpty())
    }

    @Test
    fun `deleting a task clears its dependencies too`() = runTest {
        taskDao.rows["t1"] = TaskEntity(
            id = "t1",
            title = "Read",
            description = null,
            estimatedDuration = 30,
            status = "SCHEDULED",
            mandatory = false,
            estimatedPoints = 5,
            allowTaskSplitting = false,
            goalId = null,
            categoryId = null,
        )

        local.deleteTask("t1")

        assertTrue(taskDao.rows.isEmpty())
        assertEquals(listOf("t1"), taskDao.clearedDependencies)
    }

    private companion object {
        const val START = "2026-08-09T09:00:00"
        const val END = "2026-08-09T10:00:00"
    }
}

private class FakeSessionDao : SessionDao {
    val rows = mutableMapOf<String, SessionEntity>()

    override suspend fun upsertSession(session: SessionEntity) { rows[session.id] = session }
    override suspend fun upsertSessions(sessions: List<SessionEntity>) { sessions.forEach { rows[it.id] = it } }
    override fun observeUpcomingSessions(startDate: String, endDate: String): Flow<List<UpcomingSessionRow>> = flowOf(emptyList())
    override suspend fun getUpcomingSessions(startDate: String, endDate: String): List<UpcomingSessionRow> = emptyList()
    override suspend fun getSession(id: String): SessionEntity? = rows[id]
    override suspend fun deleteSession(id: String) { rows.remove(id) }
    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> =
        flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = emptyList()
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
}

private class FakeTaskDao : TaskDao {
    val rows = mutableMapOf<String, TaskEntity>()
    val clearedDependencies = mutableListOf<String>()

    override suspend fun upsertTask(task: TaskEntity) { rows[task.id] = task }
    override suspend fun getTask(taskId: String): TaskEntity? = rows[taskId]
    override suspend fun deleteTask(taskId: String) { rows.remove(taskId) }
    override suspend fun deleteAllDependenciesForTask(taskId: String) { clearedDependencies += taskId }
    override suspend fun upsertTasks(tasks: List<TaskEntity>) {}
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
    override suspend fun deleteTasksByGoal(goalId: String) {}
    override suspend fun nullifyOrphanedGoalReferences() {}
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
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeZoneDao : ZoneDao {
    override suspend fun upsertZone(zone: ZoneEntity) {}
    override suspend fun upsertZones(zones: List<ZoneEntity>) {}
    override fun observeZone(zoneId: String): Flow<ZoneEntity?> = flowOf(null)
    override suspend fun getZone(zoneId: String): ZoneEntity? = null
    override fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
    override fun observeZonesForOverride(overrideId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
    override fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>> =
        flowOf(emptyList())
    override suspend fun deleteZone(zoneId: String) {}
    override suspend fun deleteZonesForTemplate(templateId: String) {}
    override suspend fun deleteZonesForOverride(overrideId: String) {}
}

private class FakeCategoryDao : CategoryDao {
    override suspend fun upsertCategories(categories: List<CategoryEntity>) {}
    override suspend fun upsertCategory(category: CategoryEntity) {}
    override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
    override suspend fun getCategory(id: String): CategoryEntity? = null
    override suspend fun deleteCategory(id: String) {}
    override suspend fun deleteAllCategories() {}
    override suspend fun getMinExpiryTime(): Long? = null
}
