package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── TaskEntity ────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    /** Observe all tasks for a specific goal. */
    @Query("SELECT * FROM tasks WHERE goalId = :goalId")
    fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>>

    /** Observe all Inbox tasks (goalId IS NULL). */
    @Query("SELECT * FROM tasks WHERE goalId IS NULL")
    fun observeInboxTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // ── TaskDependencyEntity ──────────────────────────────────────────────────

    @Upsert
    suspend fun upsertDependency(dependency: TaskDependencyEntity)

    @Upsert
    suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>)

    @Delete
    suspend fun deleteDependency(dependency: TaskDependencyEntity)

    /** Returns IDs of all tasks that [taskId] depends on (prerequisites). */
    @Query("SELECT dependsOnTaskId FROM task_dependencies WHERE taskId = :taskId")
    fun observeDependsOnIds(taskId: String): Flow<List<String>>

    /** Returns IDs of all tasks that depend on [taskId] (blocked by this task). */
    @Query("SELECT taskId FROM task_dependencies WHERE dependsOnTaskId = :taskId")
    fun observeDependentIds(taskId: String): Flow<List<String>>

    /** Remove all dependency links for a task (both as dependent and as prerequisite). */
    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId OR dependsOnTaskId = :taskId")
    suspend fun deleteAllDependenciesForTask(taskId: String)

    // ── Combined ──────────────────────────────────────────────────────────────

    /**
     * Replace all tasks (and their dependencies) for a goal atomically.
     * Called after a full goal sync to ensure the local state matches the server.
     */
    @Transaction
    suspend fun replaceTasksForGoal(
        goalId: String,
        tasks: List<TaskEntity>,
        dependencies: List<TaskDependencyEntity>,
    ) {
        require(tasks.all { it.goalId == goalId }) {
            "All replacement tasks must belong to goal $goalId."
        }
        val taskIds = tasks.mapTo(HashSet()) { it.id }
        require(dependencies.all { it.taskId in taskIds && it.dependsOnTaskId in taskIds }) {
            "All replacement dependencies must belong to replacement tasks for goal $goalId."
        }
        deleteTasksByGoal(goalId)
        upsertTasks(tasks)
        upsertDependencies(dependencies)
    }

    @Query("DELETE FROM tasks WHERE goalId = :goalId")
    suspend fun deleteTasksByGoal(goalId: String)
}
