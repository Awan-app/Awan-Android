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
