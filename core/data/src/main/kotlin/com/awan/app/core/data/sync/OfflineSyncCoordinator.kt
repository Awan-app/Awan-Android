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
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.CachedScheduleDateEntity
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.category.remote.CategoryRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.data.profile.remote.ProfileRemoteDataSource
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.task.toEntity
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.awan.app.core.data.common.extractDateFromIso
import com.awan.app.core.data.common.extractTimeFromIso
import com.awan.app.core.data.marketplace.asStoreItemType
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSyncCoordinator @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val goalRemoteDataSource: GoalRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    private val storeRemoteDataSource: StoreRemoteDataSource,
    private val profileRemoteDataSource: ProfileRemoteDataSource,
    private val zonesRemoteDataSource: ZonesRemoteDataSource,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: SessionDao,
    private val goalDao: GoalDao,
    private val storeDao: StoreDao,
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
        forceRefresh: Boolean = false
    ): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        var success = true
        if (!syncProfile(forceRefresh)) success = false
        if (!syncCategories(forceRefresh)) success = false
        if (!syncGoals(forceRefresh)) success = false
        if (!syncScheduleRange(startDate, endDate, forceRefresh)) success = false
        if (!syncZonesAndTemplates(forceRefresh)) success = false
        if (!syncMarketplace(forceRefresh)) success = false

        taskDao.nullifyOrphanedGoalReferences()

        success
    }

    suspend fun syncScheduleRange(
        startDate: LocalDate,
        endDate: LocalDate,
        forceRefresh: Boolean = false
    ): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        val startStr = startDate.toString()
        val endStr = endDate.toString()

        if (!forceRefresh) {
            val minExpiry = cachedScheduleDateDao.getMinExpiryTimeForRange(startStr, endStr)
            if (minExpiry != null && minExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val result = taskRemoteDataSource.getTasksByRange(startStr, endStr)
        if (result is Result.Success) {
            val rangeMap = result.data
            val nowIso = Instant.now().toString()
            val scheduleExpiry = SyncTtl.computeExpiry(SyncTtl.SCHEDULE_TTL_MS)
            val categoriesExpiry = SyncTtl.computeExpiry(SyncTtl.CATEGORIES_TTL_MS)

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
                    val categoryEntity = t.category?.let { CategoryEntity(id = it.id, name = it.name, expiryTime = categoriesExpiry) }
                    if (categoryEntity != null) {
                        categoriesToUpsert.add(categoryEntity)
                    }

                    tasksToUpsert.add(t.toEntity(expiryTime = scheduleExpiry))

                    for (s in item.sessions) {
                        sessionsToUpsert.add(s.toEntity(taskId = t.id, date = dateStr, expiryTime = scheduleExpiry))
                    }
                }
            }

            if (categoriesToUpsert.isNotEmpty()) {
                categoryDao.upsertCategories(categoriesToUpsert.distinctBy { it.id })
            }

            val validCategoryIds = categoriesToUpsert.mapTo(HashSet()) { it.id }
            categoryDao.getAllCategories().mapTo(validCategoryIds) { it.id }

            val sanitizedTasksToUpsert = tasksToUpsert.map { task ->
                val validCategoryId = task.categoryId?.takeIf { it in validCategoryIds }
                if (validCategoryId != task.categoryId) {
                    task.copy(categoryId = validCategoryId)
                } else {
                    task
                }
            }

            if (sanitizedTasksToUpsert.isNotEmpty()) {
                taskDao.upsertTasks(sanitizedTasksToUpsert.distinctBy { it.id })
            }
            sessionDao.replaceSessionsForDates(datesInRange, sessionsToUpsert.distinctBy { it.id })
            cachedScheduleDateDao.upsertCachedDates(
                datesInRange.map { CachedScheduleDateEntity(date = it, lastSyncedAt = nowIso, expiryTime = scheduleExpiry) }
            )

            true
        } else {
            false
        }
    }

    suspend fun syncGoals(forceRefresh: Boolean = false): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        if (!forceRefresh) {
            val minExpiry = goalDao.getMinExpiryTime()
            if (minExpiry != null && minExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val result = goalRemoteDataSource.getGoals()
        if (result is Result.Success) {
            val expiry = SyncTtl.computeExpiry(SyncTtl.GOALS_TTL_MS)
            val entities = result.data.map {
                GoalEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    status = it.status.name,
                    targetDate = it.targetDate,
                    createdAt = it.createdAt ?: "",
                    isInbox = it.inbox,
                    expiryTime = expiry
                )
            }
            goalDao.upsertGoals(entities)
            true
        } else {
            false
        }
    }

    suspend fun syncCategories(forceRefresh: Boolean = false): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        if (!forceRefresh) {
            val minExpiry = categoryDao.getMinExpiryTime()
            if (minExpiry != null && minExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val result = categoryRemoteDataSource.getCategories()
        if (result is Result.Success) {
            val expiry = SyncTtl.computeExpiry(SyncTtl.CATEGORIES_TTL_MS)
            val entities = result.data.map { CategoryEntity(id = it.id, name = it.name, expiryTime = expiry) }
            categoryDao.upsertCategories(entities)
            true
        } else {
            false
        }
    }

    suspend fun syncProfile(forceRefresh: Boolean = false): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        if (!forceRefresh) {
            val minExpiry = userDao.getMinExpiryTime()
            if (minExpiry != null && minExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val result = profileRemoteDataSource.getProfileInfo()
        if (result is Result.Success) {
            val res = result.data
            val userId = res.id ?: return@withContext false
            val expiry = SyncTtl.computeExpiry(SyncTtl.PROFILE_TTL_MS)
            
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
                    expiryTime = expiry
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
                        // Note: Depending on existing entity fields, we might or might not need expiryTime here,
                        // assuming UserEntity handles the primary TTL for profile.
                    )
                )
            }
            true
        } else {
            false
        }
    }

    suspend fun syncZonesAndTemplates(forceRefresh: Boolean = false): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        if (!forceRefresh) {
            val minExpiry = templateDao.getMinExpiryTime()
            if (minExpiry != null && minExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val expiry = SyncTtl.computeExpiry(SyncTtl.TEMPLATES_TTL_MS)
        
        val tplRes = zonesRemoteDataSource.getTemplates()
        if (tplRes is Result.Success) {
            val tplEntities = tplRes.data.map {
                TemplateEntity(
                    id = it.id,
                    name = it.name,
                    expiryTime = expiry
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

    suspend fun syncMarketplace(forceRefresh: Boolean = false): Boolean = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext false

        if (!forceRefresh) {
            val storeExpiry = storeDao.getMinExpiryTime()
            val ownedExpiry = storeDao.getMinOwnedExpiryTime()
            val equippedExpiry = storeDao.getMinEquippedExpiryTime()
            
            if (storeExpiry != null && storeExpiry > System.currentTimeMillis() &&
                ownedExpiry != null && ownedExpiry > System.currentTimeMillis() &&
                equippedExpiry != null && equippedExpiry > System.currentTimeMillis()) {
                return@withContext true
            }
        }

        val expiry = SyncTtl.computeExpiry(SyncTtl.STORE_TTL_MS)
        var success = true

        // 1. Sync Store Items
        val itemsRes = storeRemoteDataSource.getStoreItems()
        if (itemsRes is Result.Success) {
            val entities = itemsRes.data.mapNotNull { dto ->
                val mappedType = dto.type.asStoreItemType() ?: return@mapNotNull null
                StoreItemEntity(
                    id = dto.id,
                    name = dto.name ?: "",
                    description = dto.description ?: "",
                    image = dto.image ?: "",
                    info = dto.info,
                    price = dto.price,
                    version = dto.version ?: "",
                    type = mappedType.name,
                    expiryTime = expiry
                )
            }
            storeDao.replaceStoreItems(entities)
        } else {
            success = false
        }

        // 2. Sync Inventory
        val invRes = storeRemoteDataSource.getInventory()
        if (invRes is Result.Success) {
            val entities = invRes.data.map { dto ->
                OwnedItemEntity(
                    id = dto.id,
                    itemId = dto.item.id,
                    boughtAt = dto.boughtAt,
                    expiryTime = expiry
                )
            }
            storeDao.replaceOwnedItems(entities)
        } else {
            success = false
        }

        // 3. Sync Equipped Items
        val eqRes = storeRemoteDataSource.getEquippedItems()
        if (eqRes is Result.Success) {
            val entities = eqRes.data.map { dto ->
                EquippedItemEntity(
                    type = dto.type.name,
                    itemId = dto.item.id,
                    equippedAt = dto.equippedAt,
                    expiryTime = expiry
                )
            }
            storeDao.replaceEquippedItems(entities)
        } else {
            success = false
        }

        success
    }
}
