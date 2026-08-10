package com.awan.app.core.data.home.local

import com.awan.app.core.data.common.extractDateFromIso
import com.awan.app.core.data.common.extractTimeFromIso
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every Room read and write the Home feature makes. `HomeRepositoryImpl` holds no DAOs, so what the
 * timeline caches is decided in one file and a repository test needs one fake instead of five.
 */
interface HomeLocalDataSource {

    fun observeSessionsForDate(date: String): Flow<List<SessionEntity>>

    fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>>

    suspend fun getSession(sessionId: String): SessionEntity?

    suspend fun getTask(taskId: String): TaskEntity?

    suspend fun getCategory(categoryId: String): CategoryEntity?

    suspend fun getCachedUser(): UserEntity?

    suspend fun upsertUser(user: UserEntity)

    /**
     * Mirrors the server's copy of a session. Returns false when the row is not cached yet — the
     * caller is looking at a session this device has never synced, and there is nothing to update.
     */
    suspend fun cacheSession(session: SessionDto): Boolean

    suspend fun deleteSession(sessionId: String)

    /** Applies a task edit to the cached row, leaving fields the response omitted alone. */
    suspend fun cacheTask(taskId: String, task: TaskInfoResponse)

    suspend fun deleteTask(taskId: String)
}

@Singleton
class HomeLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val zoneDao: ZoneDao,
    private val categoryDao: CategoryDao,
) : HomeLocalDataSource {

    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> =
        sessionDao.observeSessionsForDate(date)

    override fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>> =
        zoneDao.observeEffectiveZonesForDate(date, dayOfWeek)

    override suspend fun getSession(sessionId: String): SessionEntity? = sessionDao.getSession(sessionId)

    override suspend fun getTask(taskId: String): TaskEntity? = taskDao.getTask(taskId)

    override suspend fun getCategory(categoryId: String): CategoryEntity? = categoryDao.getCategory(categoryId)

    override suspend fun getCachedUser(): UserEntity? = userDao.getFirstUser()

    override suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)

    /**
     * The status falls back to what is stored rather than to a guess, so moving a completed session
     * cannot silently reopen it. The date is taken from the response too — a session dragged past
     * midnight belongs to the new day, and leaving it behind would strand it on the old one.
     */
    override suspend fun cacheSession(session: SessionDto): Boolean {
        val existing = sessionDao.getSession(session.id) ?: return false
        sessionDao.upsertSession(
            existing.copy(
                status = session.status ?: existing.status,
                date = extractDateFromIso(session.start, existing.date),
                startTime = extractTimeFromIso(session.start, existing.startTime),
                endTime = extractTimeFromIso(session.end, existing.endTime),
                locked = session.locked,
            )
        )
        return true
    }

    override suspend fun deleteSession(sessionId: String) = sessionDao.deleteSession(sessionId)

    override suspend fun cacheTask(taskId: String, task: TaskInfoResponse) {
        val existing = taskDao.getTask(taskId) ?: return
        taskDao.upsertTask(
            existing.copy(
                title = task.title,
                description = task.description ?: existing.description,
                estimatedDuration = task.estimatedDuration ?: existing.estimatedDuration,
                estimatedPoints = task.estimatedPoints ?: existing.estimatedPoints,
                mandatory = task.mandatory ?: existing.mandatory,
                allowTaskSplitting = task.allowTaskSplitting ?: existing.allowTaskSplitting,
            )
        )
    }

    /** Sessions CASCADE from tasks; dependencies do not carry the task's own row away. */
    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteAllDependenciesForTask(taskId)
        taskDao.deleteTask(taskId)
    }
}
