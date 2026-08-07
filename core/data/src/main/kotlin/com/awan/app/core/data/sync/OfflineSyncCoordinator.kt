package com.awan.app.core.data.sync

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CachedScheduleDateEntity
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.category.remote.CategoryRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.profile.remote.ProfileRemoteDataSource
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSyncCoordinator @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val goalRemoteDataSource: GoalRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    private val profileRemoteDataSource: ProfileRemoteDataSource,
    private val zonesRemoteDataSource: ZonesRemoteDataSource,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: SessionDao,
    private val goalDao: GoalDao,
    private val userDao: UserDao,
    private val zoneDao: ZoneDao,
    private val templateDao: TemplateDao,
    private val templateOverrideDao: TemplateOverrideDao,
    private val cachedScheduleDateDao: CachedScheduleDateDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun syncAll(
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate = startDate.plusDays(6),
    ): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        var success = true
        if (!syncProfile()) success = false
        if (!syncCategories()) success = false
        if (!syncGoals()) success = false
        if (!syncScheduleRange(startDate, endDate)) success = false
        if (!syncZonesAndTemplates()) success = false

        success
    }

    suspend fun syncScheduleRange(startDate: LocalDate, endDate: LocalDate): Boolean =
        withContext(ioDispatcher) {
            if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

            val startStr = startDate.toString()
            val endStr = endDate.toString()

            val result = taskRemoteDataSource.getTasksByRange(startStr, endStr)
            if (result is Result.Success) {
                val rangeMap = result.data
                val nowIso = Instant.now().toString()

                val tasksToUpsert = mutableListOf<TaskEntity>()
                val categoriesToUpsert = mutableListOf<CategoryEntity>()
                val sessionsToUpsert = mutableListOf<SessionEntity>()

                val datesInRange = mutableListOf<String>()
                var current = startDate
                while (!current.isAfter(endDate)) {
                    datesInRange.add(current.toString())
                    current = current.plusDays(1)
                }

                for ((dateStr, tasksWithSessions) in rangeMap) {
                    for (item in tasksWithSessions) {
                        val t = item.task
                        val categoryEntity = t.category?.let { CategoryEntity(id = it.id, name = it.name) }
                        if (categoryEntity != null) {
                            categoriesToUpsert.add(categoryEntity)
                        }

                        tasksToUpsert.add(
                            TaskEntity(
                                id = t.id,
                                title = t.title,
                                description = t.description,
                                estimatedDuration = t.estimatedDuration ?: 0,
                                status = t.status ?: "SCHEDULED",
                                mandatory = t.mandatory ?: false,
                                estimatedPoints = t.estimatedPoints ?: 0,
                                allowTaskSplitting = t.allowTaskSplitting ?: false,
                                goalId = t.goalId,
                                categoryId = t.category?.id,
                            )
                        )

                        for (s in item.sessions) {
                            val sessionDate = if (s.start.length >= 10) s.start.substring(0, 10) else dateStr
                            val startTime = if (s.start.length >= 19) s.start.substring(11, 19) else "00:00:00"
                            val endTime = if (s.end.length >= 19) s.end.substring(11, 19) else "00:00:00"

                            sessionsToUpsert.add(
                                SessionEntity(
                                    id = s.id,
                                    taskId = s.taskId ?: t.id,
                                    zoneId = s.zoneId,
                                    date = sessionDate,
                                    startTime = startTime,
                                    endTime = endTime,
                                    status = s.status ?: "SCHEDULED",
                                    locked = s.locked,
                                )
                            )
                        }
                    }
                }

                if (categoriesToUpsert.isNotEmpty()) {
                    categoryDao.upsertCategories(categoriesToUpsert.distinctBy { it.id })
                }

                val existingGoalIds = goalDao.getAllGoals().mapTo(HashSet()) { it.id }
                val validCategoryIds = categoriesToUpsert.mapTo(HashSet()) { it.id }
                categoryDao.getAllCategories().mapTo(validCategoryIds) { it.id }

                val sanitizedTasksToUpsert = tasksToUpsert.map { task ->
                    val validGoalId = task.goalId?.takeIf { it in existingGoalIds }
                    val validCategoryId = task.categoryId?.takeIf { it in validCategoryIds }
                    if (validGoalId != task.goalId || validCategoryId != task.categoryId) {
                        task.copy(goalId = validGoalId, categoryId = validCategoryId)
                    } else {
                        task
                    }
                }

                if (sanitizedTasksToUpsert.isNotEmpty()) {
                    taskDao.upsertTasks(sanitizedTasksToUpsert.distinctBy { it.id })
                }
                sessionDao.replaceSessionsForDates(datesInRange, sessionsToUpsert.distinctBy { it.id })
                cachedScheduleDateDao.upsertCachedDates(
                    datesInRange.map { CachedScheduleDateEntity(date = it, lastSyncedAt = nowIso) }
                )

                true
            } else {
                false
            }
        }

    suspend fun syncGoals(): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false
        val result = goalRemoteDataSource.getGoals()
        if (result is Result.Success) {
            val entities = result.data.map {
                GoalEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    status = it.status.name,
                    targetDate = it.targetDate,
                    createdAt = it.createdAt ?: "",
                    isInbox = it.inbox,
                )
            }
            goalDao.upsertGoals(entities)
            true
        } else {
            false
        }
    }

    suspend fun syncCategories(): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false
        val result = categoryRemoteDataSource.getCategories()
        if (result is Result.Success) {
            val entities = result.data.map { CategoryEntity(id = it.id, name = it.name) }
            categoryDao.upsertCategories(entities)
            true
        } else {
            false
        }
    }

    suspend fun syncProfile(): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false
        val result = profileRemoteDataSource.getProfileInfo()
        if (result is Result.Success) {
            val res = result.data
            val userId = res.id ?: return@withContext false
            userDao.upsertUser(
                UserEntity(
                    id = userId,
                    email = res.email ?: "",
                    firstName = res.firstName ?: "",
                    lastName = res.lastName ?: "",
                    birthDate = res.birthDate,
                    points = res.points ?: 0,
                    streak = res.streak ?: 0,
                    maxStreak = res.maxStreak ?: 0,
                )
            )
            res.preferences?.let { prefs ->
                userDao.upsertPreferences(
                    UserPreferencesEntity(
                        userId = userId,
                        timezone = prefs.timezone ?: "UTC",
                        preferredSessionDuration = prefs.preferredSessionDuration ?: 45,
                        bufferBetweenSessions = prefs.bufferBetweenSessions ?: 10,
                        wakeupTime = prefs.wakeupTime ?: "08:00:00",
                        sleepTime = prefs.sleepTime ?: "22:00:00",
                        schedulingType = prefs.schedulingType ?: "BALANCED",
                    )
                )
            }
            true
        } else {
            false
        }
    }

    suspend fun syncZonesAndTemplates(): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false
        val tplRes = zonesRemoteDataSource.getTemplates()
        if (tplRes is Result.Success) {
            val tplEntities = tplRes.data.map {
                TemplateEntity(
                    id = it.id,
                    name = it.name,
                )
            }
            templateDao.upsertTemplates(tplEntities)

            val templateZones = mutableListOf<ZoneEntity>()
            val daysOfWeek = mutableListOf<TemplateDayOfWeekEntity>()
            for (tpl in tplRes.data) {
                for (day in tpl.daysOfWeek) {
                    daysOfWeek.add(TemplateDayOfWeekEntity(dayOfWeek = day, templateId = tpl.id))
                }
                for (z in tpl.zones) {
                    z.id?.let { zoneId ->
                        templateZones.add(
                            ZoneEntity(
                                id = zoneId,
                                name = z.name,
                                startTime = z.startTime,
                                endTime = z.endTime,
                                color = z.color,
                                templateId = tpl.id,
                                templateOverrideId = null,
                            )
                        )
                    }
                }
            }
            if (daysOfWeek.isNotEmpty()) {
                templateDao.upsertDays(daysOfWeek)
            }
            if (templateZones.isNotEmpty()) {
                zoneDao.upsertZones(templateZones)
            }
        }
        val overrideRes = zonesRemoteDataSource.getOverrides()
        if (overrideRes is Result.Success) {
            val overrideEntities = overrideRes.data.map {
                TemplateOverrideEntity(
                    id = it.id,
                    name = it.name,
                    dateOfDay = it.dateOfDay,
                )
            }
            templateOverrideDao.upsertOverrides(overrideEntities)

            val overrideZones = mutableListOf<ZoneEntity>()
            for (ov in overrideRes.data) {
                for (z in ov.zones) {
                    z.id?.let { zoneId ->
                        overrideZones.add(
                            ZoneEntity(
                                id = zoneId,
                                name = z.name,
                                startTime = z.startTime,
                                endTime = z.endTime,
                                color = z.color,
                                templateId = null,
                                templateOverrideId = ov.id,
                            )
                        )
                    }
                }
            }
            if (overrideZones.isNotEmpty()) {
                zoneDao.upsertZones(overrideZones)
            }
        }
        true
    }
}
