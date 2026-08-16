package com.awan.app.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.buildInMemoryDb
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AwanDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setup() {
        db = buildInMemoryDb()
        dao = db.taskDao()
    }

    @After
    fun teardown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun insertGoal(id: String = "goal1") {
        db.goalDao().upsertGoal(
            GoalEntity(
                id = id,
                title = "G",
                description = null,
                status = "ACTIVE",
                targetDate = null,
                createdAt = "2026-01-01T00:00:00Z",
                isInbox = false,
            )
        )
    }

    private fun task(
        id: String = "t1",
        title: String = "Task",
        goalId: String = "goal1",
        status: String = "SCHEDULED",
    ) = TaskEntity(
        id = id,
        title = title,
        description = null,
        estimatedDuration = 30,
        status = status,
        mandatory = false,
        estimatedPoints = 10,
        allowTaskSplitting = false,
        goalId = goalId,
    )

    private fun dep(taskId: String, dependsOnTaskId: String) =
        TaskDependencyEntity(taskId = taskId, dependsOnTaskId = dependsOnTaskId)

    // ── upsertTask / getTask ──────────────────────────────────────────────────

    @Test
    fun insertsAndRetrieves() = runTest {
        insertGoal()
        dao.upsertTask(task())
        assertEquals(task(), dao.getTask("t1"))
    }

    @Test
    fun returnsNullForMissingTask() = runTest {
        assertNull(dao.getTask("missing"))
    }

    @Test
    fun upsertTask_withUncachedCategoryId_succeedsWithoutForeignKeyException() = runTest {
        insertGoal()
        val taskWithUncachedCategory = task().copy(categoryId = "uncached-category-id")
        dao.upsertTask(taskWithUncachedCategory)
        assertEquals("uncached-category-id", dao.getTask("t1")?.categoryId)
    }

    @Test
    fun upsertUpdatesExistingTask() = runTest {
        insertGoal()
        dao.upsertTask(task(title = "Old"))
        dao.upsertTask(task(title = "New"))
        assertEquals("New", dao.getTask("t1")!!.title)
    }

    // ── observeTasksByGoal ────────────────────────────────────────────────────

    @Test
    fun observesTasksForGoal() = runTest {
        insertGoal("g1")
        insertGoal("g2")
        dao.upsertTasks(listOf(task("t1", goalId = "g1"), task("t2", goalId = "g2")))
        val g1Tasks = dao.observeTasksByGoal("g1").first()
        assertEquals(1, g1Tasks.size)
        assertEquals("t1", g1Tasks.first().id)
    }

    @Test
    fun emptyListForGoalWithNoTasks() = runTest {
        insertGoal()
        assertTrue(dao.observeTasksByGoal("goal1").first().isEmpty())
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    fun deletesTask() = runTest {
        insertGoal()
        dao.upsertTask(task())
        dao.deleteTask("t1")
        assertNull(dao.getTask("t1"))
    }

    // ── TaskDependencyEntity ──────────────────────────────────────────────────

    @Test
    fun insertsAndObservesDependsOnIds() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2"), task("t3")))
        dao.upsertDependency(dep("t1", "t2"))
        dao.upsertDependency(dep("t1", "t3"))

        val prereqs = dao.observeDependsOnIds("t1").first()
        assertEquals(setOf("t2", "t3"), prereqs.toSet())
    }

    @Test
    fun observesDependentIds() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2"), task("t3")))
        dao.upsertDependency(dep("t2", "t1"))
        dao.upsertDependency(dep("t3", "t1"))

        val blockers = dao.observeDependentIds("t1").first()
        assertEquals(setOf("t2", "t3"), blockers.toSet())
    }

    @Test
    fun deleteDependency() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2")))
        dao.upsertDependency(dep("t1", "t2"))
        dao.deleteDependency(dep("t1", "t2"))
        assertTrue(dao.observeDependsOnIds("t1").first().isEmpty())
    }

    @Test
    fun deleteAllDependenciesForTask_removesBothDirections() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2"), task("t3")))
        dao.upsertDependencies(listOf(dep("t1", "t2"), dep("t3", "t1")))

        dao.deleteAllDependenciesForTask("t1")

        assertTrue(dao.observeDependsOnIds("t1").first().isEmpty())
        assertTrue(dao.observeDependentIds("t1").first().isEmpty())
    }

    @Test
    fun taskDeleteCascadesDependencies() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2")))
        dao.upsertDependency(dep("t1", "t2"))
        dao.deleteTask("t2")
        // t2 deleted → dependency row should be gone (CASCADE)
        assertTrue(dao.observeDependsOnIds("t1").first().isEmpty())
    }

    @Test
    fun noDuplicateDependencyOnUpsert() = runTest {
        insertGoal()
        dao.upsertTasks(listOf(task("t1"), task("t2")))
        dao.upsertDependency(dep("t1", "t2"))
        dao.upsertDependency(dep("t1", "t2")) // idempotent
        assertEquals(1, dao.observeDependsOnIds("t1").first().size)
    }

    // ── replaceTasksForGoal ───────────────────────────────────────────────────

    @Test
    fun replaceTasksForGoal_replacesAllTasksAtomically() = runTest {
        insertGoal()
        dao.upsertTask(task("old", goalId = "goal1"))
        dao.replaceTasksForGoal("goal1", listOf(task("new", goalId = "goal1")), emptyList())
        assertNull(dao.getTask("old"))
        assertEquals("new", dao.getTask("new")!!.id)
    }

    @Test
    fun replaceTasksForGoal_doesNotTouchOtherGoals() = runTest {
        insertGoal("g1")
        insertGoal("g2")
        dao.upsertTask(task("tA", goalId = "g1"))
        dao.upsertTask(task("tB", goalId = "g2"))

        dao.replaceTasksForGoal("g1", emptyList(), emptyList())

        assertNull(dao.getTask("tA"))
        assertEquals("tB", dao.getTask("tB")!!.id)
    }

    @Test
    fun replaceTasksForGoal_rejectsTasksFromAnotherGoal() = runTest {
        insertGoal("g1")
        insertGoal("g2")
        dao.upsertTask(task("existing", goalId = "g1"))

        try {
            dao.replaceTasksForGoal("g1", listOf(task("g2-task", goalId = "g2")), emptyList())
            fail("Expected replacement to reject tasks from another goal")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals("existing", dao.getTask("existing")!!.id)
        assertNull(dao.getTask("g2-task"))
    }

    @Test
    fun replaceTasksForGoal_rejectsDependenciesOutsideReplacementBatch() = runTest {
        insertGoal("g1")
        insertGoal("g2")
        dao.upsertTask(task("existing-g1-task", goalId = "g1"))
        dao.upsertTask(task("g2-task", goalId = "g2"))

        try {
            dao.replaceTasksForGoal("g1", listOf(task("g1-task", goalId = "g1")), listOf(dep("g1-task", "g2-task")))
            fail("Expected replacement to reject cross-goal dependencies")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals("existing-g1-task", dao.getTask("existing-g1-task")!!.id)
        assertNull(dao.getTask("g1-task"))
        assertTrue(dao.observeDependentIds("g2-task").first().isEmpty())
    }

    @Test
    fun replaceTasksForGoal_rejectsDependencyWithDependentOutsideReplacementBatch() = runTest {
        insertGoal()
        dao.upsertTask(task("existing"))

        try {
            dao.replaceTasksForGoal("goal1", listOf(task("replacement")), listOf(dep("existing", "replacement")))
            fail("Expected replacement to reject dependencies with a dependent outside the replacement batch")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals("existing", dao.getTask("existing")!!.id)
        assertNull(dao.getTask("replacement"))
        assertTrue(dao.observeDependsOnIds("existing").first().isEmpty())
    }

    @Test
    fun replaceTasksForGoal_writesDependencies() = runTest {
        insertGoal()
        val tasks = listOf(task("t1"), task("t2"))
        dao.replaceTasksForGoal("goal1", tasks, listOf(dep("t1", "t2")))
        assertEquals(listOf("t2"), dao.observeDependsOnIds("t1").first())
    }
}
