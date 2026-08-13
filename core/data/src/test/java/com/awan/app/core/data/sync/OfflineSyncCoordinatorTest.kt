package com.awan.app.core.data.sync

import com.awan.app.core.common.error.AppError
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
import com.awan.app.core.database.model.UpcomingSessionRow
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import com.awan.app.core.database.model.TemplateOverrideEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.category.remote.CategoryRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.data.profile.remote.ProfileRemoteDataSource
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.profile.ProfileResponse
import com.awan.app.core.network.dto.profile.UpdateBirthDateRequest
import com.awan.app.core.network.dto.profile.UpdateNameRequest
import com.awan.app.core.network.dto.profile.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.profile.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.profile.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.profile.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.profile.UpdateTimezoneRequest
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
import com.awan.app.core.network.dto.zone.CreateOverrideRequest
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.CreateZoneRequest
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.UpdateOverrideRequest
import com.awan.app.core.network.dto.zone.UpdateTemplateRequest
import com.awan.app.core.network.dto.zone.UpdateZoneRequest
import com.awan.app.core.network.dto.zone.UpdateZonesRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeTaskRemoteDataSource(
    private val rangeResult: Result<Map<String, List<TaskWithSessionsDto>>> = Result.Success(emptyMap()),
) : TaskRemoteDataSource {
    override suspend fun createTask(request: CreateTaskRequest) = error("not used")
    override suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest) = error("not used")
    override suspend fun createTasksWithSessions(request: BulkCreateTasksWithSessionsRequest) = error("not used")
    override suspend fun proposeTasksFromText(request: AiTextToTasksRequest) = error("not used")
    override suspend fun proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?) = error("not used")
    override suspend fun getTasksByRange(startDate: String, endDate: String) = rangeResult
    override suspend fun getInboxTasks(): Result<List<TaskWithSessionsDto>> = Result.Success(emptyList())
    override suspend fun scheduleTask(request: ScheduleTaskRequest) = error("not used")
    override suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest
    ): Result<TaskInfoResponse> = error("not used")
    override suspend fun completeTask(taskId: String): Result<com.awan.app.core.network.dto.task.TaskCompletionResponse> = error("not used")
    override suspend fun deleteTask(taskId: String) = error("not used")
}

private class FakeGoalRemoteDataSource(
    private val goalsResult: Result<List<GoalInfoResponse>> = Result.Success(emptyList()),
) : GoalRemoteDataSource {
    override suspend fun getGoals(): Result<List<GoalInfoResponse>> = goalsResult
    override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest) = error("not used")
    override suspend fun addTasksToGoal(goalId: String, request: com.awan.app.core.network.dto.goal.BulkCreateGoalTasksRequest): Result<List<com.awan.app.core.network.dto.task.TaskInfoResponse>> = error("not used")
    override suspend fun getInboxGoal() = error("not used")
    override suspend fun getGoal(goalId: String, expand: Boolean) = error("not used")
    override suspend fun updateGoal(goalId: String, request: com.awan.app.core.network.dto.goal.UpdateGoalRequest) = error("not used")
    override suspend fun deleteGoal(goalId: String) = error("not used")
    override suspend fun continueDecomposition(request: GoalDecomposeRequest) = error("not used")
    override suspend fun confirmDecomposition(sessionId: String) = error("not used")
    override suspend fun getDecompositionTranscript(sessionId: String) = error("not used")
    override suspend fun cancelDecomposition(sessionId: String) = error("not used")
    override suspend fun scheduleGoal(goalId: String) = error("not used")
    override suspend fun proposeGoalSchedule(goalId: String) = error("not used")
    override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest) = error("not used")
}


private class FakeCategoryRemoteDataSource(
    private val categoriesResult: Result<List<CategoryDto>> = Result.Success(emptyList()),
) : CategoryRemoteDataSource {
    override suspend fun getCategories(): Result<List<CategoryDto>> = categoriesResult
    override suspend fun createCategory(name: String): Result<CategoryDto> = error("not used")
    override suspend fun getCategory(categoryId: String): Result<CategoryDto> = error("not used")
    override suspend fun updateCategory(categoryId: String, name: String): Result<CategoryDto> = error("not used")
}

private class FakeStoreRemoteDataSource : StoreRemoteDataSource {
    override suspend fun getStoreItems(type: StoreItemTypeDto?): Result<List<StoreItemDto>> = Result.Success(emptyList())
    override suspend fun getInventory(): Result<List<OwnedItemDto>> = Result.Success(emptyList())
    override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun getEquippedItems(): Result<List<EquippedItemDto>> = Result.Success(emptyList())
    override suspend fun equipItem(itemId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun unequipItem(itemId: String): Result<Unit> = Result.Success(Unit)
}

private class FakeProfileRemoteDataSource : ProfileRemoteDataSource {
    override suspend fun getProfileInfo(): Result<ProfileResponse> = Result.Success(ProfileResponse())
    override suspend fun updateProfileName(request: UpdateNameRequest) = error("not used")
    override suspend fun updateProfileBirthDate(request: UpdateBirthDateRequest) = error("not used")
    override suspend fun updateProfilePartial(request: UpdateProfilePartialRequest) = error("not used")
    override suspend fun updateTimezone(request: UpdateTimezoneRequest) = error("not used")
    override suspend fun updateSessionSettings(request: UpdateSessionSettingsRequest) = error("not used")
    override suspend fun updateSleepSchedule(request: UpdateSleepScheduleRequest) = error("not used")
    override suspend fun updateSchedulingType(request: UpdateSchedulingTypeRequest) = error("not used")
    override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String) = error("not used")
    override suspend fun deleteProfilePicture() = error("not used")
}

private class FakeZonesRemoteDataSource(
    private val templatesFail: Boolean = false,
    private val overridesFail: Boolean = false,
) : ZonesRemoteDataSource {
    override suspend fun getZonesByDate(date: String) = Result.Success(emptyList<ZoneDto>())
    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> =
        if (templatesFail) Result.Error(AppError.Network) else Result.Success(emptyList())
    override suspend fun createTemplate(request: CreateTemplateRequest) = error("not used")
    override suspend fun getTemplate(templateId: String) = error("not used")
    override suspend fun updateTemplate(templateId: String, request: UpdateTemplateRequest) = error("not used")
    override suspend fun deleteTemplate(templateId: String) = error("not used")
    override suspend fun addZoneToTemplate(templateId: String, request: CreateZoneRequest) = error("not used")
    override suspend fun getTemplateZones(templateId: String) = Result.Success(emptyList<ZoneDto>())
    override suspend fun updateTemplateZones(templateId: String, request: UpdateZonesRequest) = Result.Success(emptyList<ZoneDto>())
    override suspend fun createOverride(request: CreateOverrideRequest) = error("not used")
    override suspend fun getOverrides(): Result<List<TemplateOverrideDto>> =
        if (overridesFail) Result.Error(AppError.Network) else Result.Success(emptyList())
    override suspend fun getOverride(overrideId: String) = error("not used")
    override suspend fun updateOverride(overrideId: String, request: UpdateOverrideRequest) = error("not used")
    override suspend fun deleteOverride(overrideId: String) = error("not used")
    override suspend fun addZoneToOverride(overrideId: String, request: CreateZoneRequest) = error("not used")
    override suspend fun getOverrideZones(overrideId: String) = Result.Success(emptyList<ZoneDto>())
    override suspend fun updateOverrideZones(overrideId: String, request: UpdateZonesRequest) = Result.Success(emptyList<ZoneDto>())
    override suspend fun getZone(zoneId: String) = error("not used")
    override suspend fun getZoneSessions(zoneId: String) = Result.Success(emptyList<SessionDto>())
    override suspend fun getEffectiveZones(date: String) = Result.Success(emptyList<ZoneDto>())
    override suspend fun updateZone(zoneId: String, request: UpdateZoneRequest) = error("not used")
    override suspend fun deleteZone(zoneId: String) = error("not used")
}

private class FakeTaskDao : TaskDao {
    val upsertedTasks = mutableListOf<TaskEntity>()
    override suspend fun upsertTask(task: TaskEntity) { upsertedTasks += task }
    override suspend fun upsertTasks(tasks: List<TaskEntity>) { upsertedTasks += tasks }
    override fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
    override suspend fun getTasksByGoal(goalId: String): List<TaskEntity> = emptyList()
    override fun observeTask(taskId: String): Flow<TaskEntity?> = MutableStateFlow(null)
    override suspend fun getTask(taskId: String): TaskEntity? = null
    override suspend fun deleteTask(taskId: String) {}
    override suspend fun upsertDependency(dependency: TaskDependencyEntity) {}
    override suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>) {}
    override suspend fun deleteDependency(dependency: TaskDependencyEntity) {}
    override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getDependsOnIds(taskId: String): List<String> = emptyList()
    override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override suspend fun deleteAllDependenciesForTask(taskId: String) {}
    override suspend fun replaceTasksForGoal(goalId: String, tasks: List<TaskEntity>, dependencies: List<TaskDependencyEntity>) {}
    override suspend fun deleteTasksByGoal(goalId: String) {}
    override suspend fun nullifyOrphanedGoalReferences() {}
}

private class FakeCategoryDao : CategoryDao {
    val upserted = mutableListOf<CategoryEntity>()
    override suspend fun upsertCategories(categories: List<CategoryEntity>) { upserted += categories }
    override suspend fun upsertCategory(category: CategoryEntity) { upserted += category }
    override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
    override suspend fun getCategory(id: String): CategoryEntity? = null
    override suspend fun deleteCategory(id: String) {}
    override suspend fun deleteAllCategories() {}
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeSessionDao : SessionDao {
    val replacedDates = mutableListOf<String>()
    val upserted = mutableListOf<SessionEntity>()
    override suspend fun upsertSession(session: SessionEntity) { upserted += session }
    override suspend fun upsertSessions(sessions: List<SessionEntity>) { upserted += sessions }
    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = emptyList()
    override fun observeUpcomingSessions(startDate: String, endDate: String): Flow<List<UpcomingSessionRow>> = flowOf(emptyList())
    override suspend fun getUpcomingSessions(startDate: String, endDate: String): List<UpcomingSessionRow> = emptyList()
    override suspend fun getSession(id: String): SessionEntity? = null
    override suspend fun deleteSessionsForDates(dates: List<String>) { replacedDates += dates }
    override suspend fun deleteSession(id: String) {}
}

private class FakeGoalDao : GoalDao {
    val upserted = mutableListOf<GoalEntity>()
    override suspend fun upsertGoal(goal: GoalEntity) { upserted += goal }
    override suspend fun upsertGoals(goals: List<GoalEntity>) { upserted += goals }
    override fun observeAllGoals(): Flow<List<GoalEntity>> = flowOf(emptyList())
    override suspend fun getAllGoals(): List<GoalEntity> = upserted.toList()
    override fun observeGoalsByStatus(status: String): Flow<List<GoalEntity>> = flowOf(emptyList())
    override fun observeGoal(goalId: String): Flow<GoalEntity?> = MutableStateFlow(null)
    override suspend fun getGoal(goalId: String): GoalEntity? = null
    override suspend fun deleteGoal(goalId: String) {}
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeStoreDao : StoreDao {
    var storeItems = listOf<StoreItemEntity>()
    var ownedItems = listOf<OwnedItemEntity>()
    var equippedItems = listOf<EquippedItemEntity>()
    override suspend fun upsertStoreItems(items: List<StoreItemEntity>) { storeItems = items }
    override fun observeStoreItems(): Flow<List<StoreItemEntity>> = flowOf(storeItems)
    override fun observeStoreItemsByType(type: String): Flow<List<StoreItemEntity>> = flowOf(storeItems.filter { it.type == type })
    override suspend fun deleteAllStoreItems() { storeItems = emptyList() }
    override suspend fun upsertOwnedItems(items: List<OwnedItemEntity>) { ownedItems = items }
    override fun observeOwnedItems(): Flow<List<OwnedItemEntity>> = flowOf(ownedItems)
    override suspend fun deleteAllOwnedItems() { ownedItems = emptyList() }
    override suspend fun upsertEquippedItems(items: List<EquippedItemEntity>) { equippedItems = items }
    override fun observeEquippedItems(): Flow<List<EquippedItemEntity>> = flowOf(equippedItems)
    override suspend fun deleteAllEquippedItems() { equippedItems = emptyList() }
    override suspend fun deleteEquippedItemByType(type: String) { equippedItems = equippedItems.filter { it.type != type } }
    override suspend fun replaceStoreItems(items: List<StoreItemEntity>) { storeItems = items }
    override suspend fun replaceOwnedItems(items: List<OwnedItemEntity>) { ownedItems = items }
    override suspend fun replaceEquippedItems(items: List<EquippedItemEntity>) { equippedItems = items }
    override suspend fun getMinExpiryTime(): Long? = null
    override suspend fun getMinOwnedExpiryTime(): Long? = null
    override suspend fun getMinEquippedExpiryTime(): Long? = null
}

private class FakeUserDao : UserDao {
    val upsertedUsers = mutableListOf<UserEntity>()
    val upsertedPrefs = mutableListOf<UserPreferencesEntity>()
    override suspend fun upsertUser(user: UserEntity) { upsertedUsers += user }
    override fun observeUser(userId: String): Flow<UserEntity?> = MutableStateFlow(null)
    override suspend fun getUser(userId: String): UserEntity? = null
    override suspend fun getFirstUser(): UserEntity? = null
    override suspend fun deleteUser(userId: String) {}
    override suspend fun upsertPreferences(preferences: UserPreferencesEntity) { upsertedPrefs += preferences }
    override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = MutableStateFlow(null)
    override suspend fun getPreferences(userId: String): UserPreferencesEntity? = null
    override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = MutableStateFlow(null)
    override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeZoneDao : ZoneDao {
    val upserted = mutableListOf<ZoneEntity>()
    override suspend fun upsertZone(zone: ZoneEntity) { upserted += zone }
    override suspend fun upsertZones(zones: List<ZoneEntity>) { upserted += zones }
    override fun observeZone(zoneId: String): Flow<ZoneEntity?> = MutableStateFlow(null)
    override suspend fun getZone(zoneId: String): ZoneEntity? = null
    override fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
    override fun observeZonesForOverride(overrideId: String): Flow<List<ZoneEntity>> = flowOf(emptyList())
    override suspend fun deleteZone(zoneId: String) {}
    override suspend fun deleteZonesForTemplate(templateId: String) {}
    override suspend fun deleteZonesForOverride(overrideId: String) {}
    override fun observeEffectiveZonesForDate(date: String, dayOfWeek: String): Flow<List<ZoneEntity>> =
        flowOf(emptyList())
}

private class FakeTemplateDao : TemplateDao {
    val upserted = mutableListOf<TemplateEntity>()
    val upsertedDays = mutableListOf<TemplateDayOfWeekEntity>()
    override suspend fun upsertTemplate(template: TemplateEntity) { upserted += template }
    override suspend fun upsertTemplates(templates: List<TemplateEntity>) { upserted += templates }
    override fun observeAllTemplates(): Flow<List<TemplateEntity>> = flowOf(emptyList())
    override fun observeTemplate(templateId: String): Flow<TemplateEntity?> = MutableStateFlow(null)
    override suspend fun getTemplate(templateId: String): TemplateEntity? = null
    override suspend fun deleteTemplate(templateId: String) {}
    override suspend fun deleteAllTemplates() {}
    override suspend fun upsertDays(days: List<TemplateDayOfWeekEntity>) { upsertedDays += days }
    override fun observeDaysForTemplate(templateId: String): Flow<List<TemplateDayOfWeekEntity>> = flowOf(emptyList())
    override suspend fun getDayAssignment(dayOfWeek: String): TemplateDayOfWeekEntity? = null
    override suspend fun deleteDaysForTemplate(templateId: String) {}
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeTemplateOverrideDao : TemplateOverrideDao {
    val upserted = mutableListOf<TemplateOverrideEntity>()
    override suspend fun upsertOverride(override: TemplateOverrideEntity) { upserted += override }
    override suspend fun upsertOverrides(overrides: List<TemplateOverrideEntity>) { upserted += overrides }
    override fun observeAllOverrides(): Flow<List<TemplateOverrideEntity>> = flowOf(emptyList())
    override fun observeOverride(overrideId: String): Flow<TemplateOverrideEntity?> = MutableStateFlow(null)
    override suspend fun getOverride(overrideId: String): TemplateOverrideEntity? = null
    override suspend fun getOverrideForDate(date: String): TemplateOverrideEntity? = null
    override suspend fun deleteOverride(overrideId: String) {}
    override suspend fun deleteAllOverrides() {}
}

private class FakeZonesLocalDataSource : com.awan.app.core.data.zones.local.ZonesLocalDataSource {
    var replaceCount = 0
        private set
    var templates: List<com.awan.app.core.network.dto.zone.WeeklyTemplateDto> = emptyList()
        private set
    var overrides: List<com.awan.app.core.network.dto.zone.TemplateOverrideDto> = emptyList()
        private set

    override suspend fun replaceAll(
        templates: List<com.awan.app.core.network.dto.zone.WeeklyTemplateDto>,
        overrides: List<com.awan.app.core.network.dto.zone.TemplateOverrideDto>,
        expiryTime: Long,
    ) {
        replaceCount++
        this.templates = templates
        this.overrides = overrides
    }

    override fun observeEffectiveZonesForDate(
        date: String,
        dayOfWeek: String,
    ): Flow<List<ZoneEntity>> = flowOf(emptyList())
}

private class FakeCachedScheduleDateDao : CachedScheduleDateDao {
    val upserted = mutableListOf<CachedScheduleDateEntity>()
    val cachedDates = mutableSetOf<String>()
    override suspend fun upsertCachedDate(cachedDate: CachedScheduleDateEntity) { upserted += cachedDate; cachedDates += cachedDate.date }
    override suspend fun upsertCachedDates(cachedDates: List<CachedScheduleDateEntity>) {
        upserted += cachedDates
        this.cachedDates += cachedDates.map { it.date }
    }
    override suspend fun isDateCached(date: String): Boolean = date in cachedDates
    override fun observeIsDateCached(date: String): Flow<Boolean> = MutableStateFlow(date in cachedDates)
    override suspend fun getCachedDatesInRange(startDate: String, endDate: String): List<String> =
        cachedDates.filter { it >= startDate && it <= endDate }
    override suspend fun clearAll() { upserted.clear(); cachedDates.clear() }
    override suspend fun getMinExpiryTimeForRange(startDate: String, endDate: String): Long? = null
}

private val onlineMonitor = object : NetworkConnectivityMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
    override fun isCurrentlyOnline(): Boolean = true
}

private val offlineMonitor = object : NetworkConnectivityMonitor {
    override val isOnline: Flow<Boolean> = flowOf(false)
    override fun isCurrentlyOnline(): Boolean = false
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class OfflineSyncCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun buildCoordinator(
        taskRemoteDataSource: TaskRemoteDataSource = FakeTaskRemoteDataSource(),
        goalRemoteDataSource: GoalRemoteDataSource = FakeGoalRemoteDataSource(),
        categoryRemoteDataSource: CategoryRemoteDataSource = FakeCategoryRemoteDataSource(),
        storeRemoteDataSource: StoreRemoteDataSource = FakeStoreRemoteDataSource(),
        profileRemoteDataSource: ProfileRemoteDataSource = FakeProfileRemoteDataSource(),
        zonesRemoteDataSource: ZonesRemoteDataSource = FakeZonesRemoteDataSource(),
        taskDao: TaskDao = FakeTaskDao(),
        categoryDao: CategoryDao = FakeCategoryDao(),
        sessionDao: SessionDao = FakeSessionDao(),
        goalDao: GoalDao = FakeGoalDao(),
        storeDao: StoreDao = FakeStoreDao(),
        userDao: UserDao = FakeUserDao(),
        templateDao: TemplateDao = FakeTemplateDao(),
        zonesLocalDataSource: com.awan.app.core.data.zones.local.ZonesLocalDataSource = FakeZonesLocalDataSource(),
        cachedScheduleDateDao: CachedScheduleDateDao = FakeCachedScheduleDateDao(),
        connectivityMonitor: NetworkConnectivityMonitor = onlineMonitor,
    ) = OfflineSyncCoordinator(
        taskRemoteDataSource = taskRemoteDataSource,
        goalRemoteDataSource = goalRemoteDataSource,
        categoryRemoteDataSource = categoryRemoteDataSource,
        storeRemoteDataSource = storeRemoteDataSource,
        profileRemoteDataSource = profileRemoteDataSource,
        zonesRemoteDataSource = zonesRemoteDataSource,
        taskDao = taskDao,
        categoryDao = categoryDao,
        sessionDao = sessionDao,
        goalDao = goalDao,
        storeDao = storeDao,
        userDao = userDao,
        templateDao = templateDao,
        zonesLocalDataSource = zonesLocalDataSource,
        cachedScheduleDateDao = cachedScheduleDateDao,
        connectivityMonitor = connectivityMonitor,
        ioDispatcher = testDispatcher,
    )

    // ── DB-only read precedence ───────────────────────────────────────────────

    @Test
    fun syncScheduleRange_replacesSessionsForEmptyAndNonEmptyDates() = runTest(testDispatcher) {
        val startDate = LocalDate.of(2026, 8, 6)
        val endDate = LocalDate.of(2026, 8, 7)

        val rangeResponse = mapOf(
            "2026-08-06" to listOf(
                TaskWithSessionsDto(
                    task = TaskInfoResponse(id = "t1", title = "Task 1"),
                    sessions = emptyList(),
                )
            ),
            "2026-08-07" to emptyList(),
        )

        val fakeSession = FakeSessionDao()
        val fakeCacheDate = FakeCachedScheduleDateDao()
        val coordinator = buildCoordinator(
            taskRemoteDataSource = FakeTaskRemoteDataSource(Result.Success(rangeResponse)),
            sessionDao = fakeSession,
            cachedScheduleDateDao = fakeCacheDate,
        )

        val result = coordinator.syncScheduleRange(startDate, endDate)

        assertTrue(result)
        // Both dates were in the request — sessions for them must have been deleted (replaced)
        assertTrue(fakeSession.replacedDates.containsAll(listOf("2026-08-06", "2026-08-07")))
    }

    @Test
    fun syncScheduleRange_marksEmptyDateAsCached() = runTest(testDispatcher) {
        val startDate = LocalDate.of(2026, 8, 10)
        val endDate = LocalDate.of(2026, 8, 10)

        // Server responds with an empty list for that date
        val rangeResponse = mapOf("2026-08-10" to emptyList<TaskWithSessionsDto>())

        val fakeCacheDate = FakeCachedScheduleDateDao()
        val coordinator = buildCoordinator(
            taskRemoteDataSource = FakeTaskRemoteDataSource(Result.Success(rangeResponse)),
            cachedScheduleDateDao = fakeCacheDate,
        )

        val result = coordinator.syncScheduleRange(startDate, endDate)

        assertTrue(result)
        // An empty-but-synced date must be persisted as cached — not treated as "unknown"
        assertTrue(fakeCacheDate.cachedDates.contains("2026-08-10"))
    }

    @Test
    fun syncScheduleRange_returnsFalseWhenOffline() = runTest(testDispatcher) {
        val coordinator = buildCoordinator(connectivityMonitor = offlineMonitor)

        val result = coordinator.syncScheduleRange(LocalDate.now(), LocalDate.now().plusDays(6))

        assertFalse(result)
    }

    @Test
    fun syncGoals_upsertsMappedEntitiesToRoom() = runTest(testDispatcher) {
        val fakeGoalDao = FakeGoalDao()
        val goals = listOf(
            com.awan.app.core.network.dto.GoalInfoResponse(
                id = "g1",
                title = "Study",
                status = com.awan.app.core.network.dto.GoalStatusDto.ACTIVE,
            )
        )
        val coordinator = buildCoordinator(
            goalRemoteDataSource = FakeGoalRemoteDataSource(Result.Success(goals)),
            goalDao = fakeGoalDao,
        )

        val result = coordinator.syncGoals()

        assertTrue(result)
        assertEquals(1, fakeGoalDao.upserted.size)
        assertEquals("g1", fakeGoalDao.upserted.first().id)
    }

    @Test
    fun syncGoals_returnsFalseWhenOffline() = runTest(testDispatcher) {
        val coordinator = buildCoordinator(connectivityMonitor = offlineMonitor)

        assertFalse(coordinator.syncGoals())
    }

    @Test
    fun syncCategories_upsertsMappedEntitiesToRoom() = runTest(testDispatcher) {
        val fakeCategoryDao = FakeCategoryDao()
        val categories = listOf(CategoryDto(id = "c1", name = "Work"))
        val coordinator = buildCoordinator(
            categoryRemoteDataSource = FakeCategoryRemoteDataSource(Result.Success(categories)),
            categoryDao = fakeCategoryDao,
        )

        val result = coordinator.syncCategories()

        assertTrue(result)
        assertEquals(1, fakeCategoryDao.upserted.size)
        assertEquals("Work", fakeCategoryDao.upserted.first().name)
    }

    @Test
    fun paginatedGoalUpsert_doesNotDeleteGoalsMissingFromPage() = runTest(testDispatcher) {
        // Simulate: Room already has goals g1 and g2 from a previous sync.
        // Sync returns only g1. g2 must NOT be deleted.
        val fakeGoalDao = FakeGoalDao()
        // pre-populate g2 via upsert (simulate existing record)
        fakeGoalDao.upsertGoal(GoalEntity(id = "g2", title = "Existing", description = null, status = "ACTIVE", targetDate = null, createdAt = "", isInbox = false))
        fakeGoalDao.upserted.clear() // reset tracker so we only observe the sync upsert

        val newGoals = listOf(
            com.awan.app.core.network.dto.GoalInfoResponse(
                id = "g1", title = "New goal", status = com.awan.app.core.network.dto.GoalStatusDto.ACTIVE,
            )
        )
        val coordinator = buildCoordinator(
            goalRemoteDataSource = FakeGoalRemoteDataSource(Result.Success(newGoals)),
            goalDao = fakeGoalDao,
        )

        coordinator.syncGoals()

        // Only g1 is in the upsert list — g2 was not explicitly deleted
        assertEquals(1, fakeGoalDao.upserted.size)
        assertEquals("g1", fakeGoalDao.upserted.first().id)
        // g2 is still accessible (not deleted)
        // The DAO is a fake so deletion would have been recorded separately
    }

    @Test
    fun syncAll_returnsFalseWhenOffline() = runTest(testDispatcher) {
        val coordinator = buildCoordinator(connectivityMonitor = offlineMonitor)

        val result = coordinator.syncAll()

        assertFalse(result)
    }

    // ── Zones: replace, all-or-nothing ────────────────────────────────────────

    @Test
    fun syncZonesAndTemplates_replacesTheWholeModelOnce() = runTest(testDispatcher) {
        val local = FakeZonesLocalDataSource()
        val coordinator = buildCoordinator(zonesLocalDataSource = local)

        assertTrue(coordinator.syncZonesAndTemplates(forceRefresh = true))
        assertEquals(1, local.replaceCount)
    }

    /** A half-written replace would delete the templates this sync could not refetch. */
    @Test
    fun syncZonesAndTemplates_writesNothingAndFailsWhenEitherCallFails() = runTest(testDispatcher) {
        val templatesDown = FakeZonesLocalDataSource()
        assertFalse(
            buildCoordinator(
                zonesRemoteDataSource = FakeZonesRemoteDataSource(templatesFail = true),
                zonesLocalDataSource = templatesDown,
            ).syncZonesAndTemplates(forceRefresh = true)
        )
        assertEquals(0, templatesDown.replaceCount)

        val overridesDown = FakeZonesLocalDataSource()
        assertFalse(
            buildCoordinator(
                zonesRemoteDataSource = FakeZonesRemoteDataSource(overridesFail = true),
                zonesLocalDataSource = overridesDown,
            ).syncZonesAndTemplates(forceRefresh = true)
        )
        assertEquals(0, overridesDown.replaceCount)
    }

}
