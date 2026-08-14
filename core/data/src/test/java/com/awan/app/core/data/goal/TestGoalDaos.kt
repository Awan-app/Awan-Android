package com.awan.app.core.data.goal

import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal open class TestCategoryDao : CategoryDao {
    val upserted = mutableListOf<CategoryEntity>()
    override suspend fun upsertCategories(categories: List<CategoryEntity>) { upserted += categories }
    override suspend fun upsertCategory(category: CategoryEntity) { upserted += category }
    override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(upserted)
    override suspend fun getAllCategories(): List<CategoryEntity> = upserted
    override suspend fun getCategory(id: String): CategoryEntity? = upserted.firstOrNull { it.id == id }
    override suspend fun deleteCategory(id: String) { upserted.removeIf { it.id == id } }
    override suspend fun deleteAllCategories() { upserted.clear() }
    override suspend fun getMinExpiryTime(): Long? = null
}

internal open class TestTaskDao : TaskDao {
    val tasks = mutableListOf<TaskEntity>()
    val dependencies = mutableListOf<TaskDependencyEntity>()
    override suspend fun upsertTask(task: TaskEntity) { tasks += task }
    override suspend fun upsertTasks(tasks: List<TaskEntity>) { this.tasks += tasks }
    override fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(tasks.filter { it.goalId == goalId })
    override fun observeInboxTasks(): Flow<List<TaskEntity>> = flowOf(tasks.filter { it.goalId == null })
    override fun observeAllTasks(): Flow<List<TaskEntity>> = flowOf(tasks)
    override suspend fun getAllTasks(): List<TaskEntity> = tasks
    override fun observeTask(taskId: String): Flow<TaskEntity?> = flowOf(tasks.firstOrNull { it.id == taskId })
    override suspend fun getTask(taskId: String): TaskEntity? = tasks.firstOrNull { it.id == taskId }
    override suspend fun deleteTask(taskId: String) { tasks.removeIf { it.id == taskId } }
    override suspend fun upsertDependency(dependency: TaskDependencyEntity) { dependencies += dependency }
    override suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>) { this.dependencies += dependencies }
    override suspend fun deleteDependency(dependency: TaskDependencyEntity) { this.dependencies.remove(dependency) }
    override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(dependencies.filter { it.taskId == taskId }.map { it.dependsOnTaskId })
    override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(dependencies.filter { it.dependsOnTaskId == taskId }.map { it.taskId })
    override suspend fun deleteAllDependenciesForTask(taskId: String) { dependencies.removeIf { it.taskId == taskId || it.dependsOnTaskId == taskId } }
    override suspend fun replaceTasksForGoal(goalId: String, tasks: List<TaskEntity>, dependencies: List<TaskDependencyEntity>) {
        deleteTasksByGoal(goalId)
        upsertTasks(tasks)
        upsertDependencies(dependencies)
    }
    override suspend fun deleteTasksByGoal(goalId: String) { tasks.removeIf { it.goalId == goalId } }
    override suspend fun nullifyOrphanedGoalReferences() {}
}

internal open class TestScheduleDraftDao : com.awan.app.core.database.dao.ScheduleDraftDao {
    val insertedDraftsIfNotExist = mutableListOf<com.awan.app.core.database.model.ScheduleDraftEntity>()
    val upsertedDraftSessions = mutableListOf<com.awan.app.core.database.model.ScheduleDraftSessionEntity>()

    override suspend fun insertDraft(draft: com.awan.app.core.database.model.ScheduleDraftEntity) {}
    override suspend fun insertDraftSessions(sessions: List<com.awan.app.core.database.model.ScheduleDraftSessionEntity>) {}
    override suspend fun insertDraftUnscheduledTasks(tasks: List<com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity>) {}

    override fun observeDraft(goalId: String): kotlinx.coroutines.flow.Flow<com.awan.app.core.database.model.ScheduleDraftEntity?> = kotlinx.coroutines.flow.flowOf(null)
    override fun observeDraftSessions(goalId: String): kotlinx.coroutines.flow.Flow<List<com.awan.app.core.database.model.ScheduleDraftSessionEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observeDraftUnscheduledTasks(goalId: String): kotlinx.coroutines.flow.Flow<List<com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getDraft(goalId: String): com.awan.app.core.database.model.ScheduleDraftEntity? = null
    override suspend fun getDraftSessions(goalId: String): List<com.awan.app.core.database.model.ScheduleDraftSessionEntity> = emptyList()
    override suspend fun getDraftUnscheduledTasks(goalId: String): List<com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity> = emptyList()
    override suspend fun getPendingDraftGoalId(): String? = null

    val deletedDrafts = mutableListOf<String>()
    val insertedSessions = mutableListOf<com.awan.app.core.database.model.SessionEntity>()

    val draftStates = mutableMapOf<String, String>()
    override suspend fun deleteDraft(goalId: String) { deletedDrafts.add(goalId); draftStates.remove(goalId) }
    override suspend fun deleteDraftSessions(goalId: String) {}
    override suspend fun deleteDraftUnscheduledTasks(goalId: String) {}
    override suspend fun deleteDraftEntity(goalId: String) {}
    override suspend fun updateDraftState(goalId: String, state: String) { draftStates[goalId] = state }
    override suspend fun insertDraftIfNotExists(draft: com.awan.app.core.database.model.ScheduleDraftEntity) { insertedDraftsIfNotExist.add(draft) }
}

internal open class TestSessionDao : com.awan.app.core.database.dao.SessionDao {
    val upserted = mutableListOf<com.awan.app.core.database.model.SessionEntity>()
    override suspend fun upsertSession(session: com.awan.app.core.database.model.SessionEntity) { upserted += session }
    override suspend fun upsertSessions(sessions: List<com.awan.app.core.database.model.SessionEntity>) { upserted += sessions }
    override fun observeSessionsForDate(date: String): Flow<List<com.awan.app.core.database.model.SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<com.awan.app.core.database.model.SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<com.awan.app.core.database.model.SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<com.awan.app.core.database.model.SessionEntity> = emptyList()
    override suspend fun getSession(id: String): com.awan.app.core.database.model.SessionEntity? = null
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
    override suspend fun deleteSession(id: String) {}
}
