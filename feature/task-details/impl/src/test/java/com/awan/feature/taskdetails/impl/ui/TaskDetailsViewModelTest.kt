@file:Suppress("NewApi")

package com.awan.feature.taskdetails.impl.ui

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.AddTaskDependencyUseCase
import com.awan.app.core.domain.task.usecase.AddTaskSessionsUseCase
import com.awan.app.core.domain.task.usecase.DeleteTaskUseCase
import com.awan.app.core.domain.task.usecase.GetTaskDependenciesUseCase
import com.awan.app.core.domain.task.usecase.GetTaskDependentsUseCase
import com.awan.app.core.domain.task.usecase.GetTasksByGoalUseCase
import com.awan.app.core.domain.task.usecase.GetTaskSessionsUseCase
import com.awan.app.core.domain.task.usecase.GetTaskUseCase
import com.awan.app.core.domain.task.usecase.MoveTaskUseCase
import com.awan.app.core.domain.task.usecase.RemoveTaskDependencyUseCase
import com.awan.app.core.domain.task.usecase.UpdateTaskUseCase
import com.awan.app.core.model.Goal
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeTaskRepository = FakeTaskRepository()
    private val fakeGoalRepository = FakeGoalRepository()
    private val fakeSessionRepository = FakeSessionRepository()

    private lateinit var viewModel: TaskDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskDetailsViewModel(
            getTaskUseCase = GetTaskUseCase(fakeTaskRepository),
            updateTaskUseCase = UpdateTaskUseCase(fakeTaskRepository),
            moveTaskUseCase = MoveTaskUseCase(fakeTaskRepository),
            deleteTaskUseCase = DeleteTaskUseCase(fakeTaskRepository),
            addDependencyUseCase = AddTaskDependencyUseCase(fakeTaskRepository),
            removeDependencyUseCase = RemoveTaskDependencyUseCase(fakeTaskRepository),
            getDependenciesUseCase = GetTaskDependenciesUseCase(fakeTaskRepository),
            getDependentsUseCase = GetTaskDependentsUseCase(fakeTaskRepository),
            getTaskSessionsUseCase = GetTaskSessionsUseCase(fakeTaskRepository),
            addTaskSessionsUseCase = AddTaskSessionsUseCase(fakeTaskRepository),
            deleteSessionUseCase = com.awan.app.core.domain.home.usecase.DeleteSessionUseCase(fakeSessionRepository),
            getGoalsUseCase = GetGoalsUseCase(fakeGoalRepository),
            getTasksByGoalUseCase = GetTasksByGoalUseCase(fakeTaskRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initTaskId loads task and calculates duration dynamically from sessions`() = runTest(testDispatcher) {
        val testTask = Task(
            id = "task-1",
            title = "Design Mockups",
            description = "Figma designs",
            estimatedDurationMinutes = 30,
            status = TaskStatus.SCHEDULED,
            mandatory = true,
            estimatedPoints = 15,
            allowTaskSplitting = false,
            goalId = "goal-1",
        )
        fakeTaskRepository.tasks["task-1"] = testTask

        val start1 = LocalDateTime.of(2026, 8, 16, 9, 0)
        val end1 = LocalDateTime.of(2026, 8, 16, 9, 45) // 45 min
        val start2 = LocalDateTime.of(2026, 8, 16, 14, 0)
        val end2 = LocalDateTime.of(2026, 8, 16, 14, 30) // 30 min

        fakeTaskRepository.sessions["task-1"] = listOf(
            TaskSession(id = "s-1", start = start1, end = end1, status = SessionStatus.SCHEDULED),
            TaskSession(id = "s-2", start = start2, end = end2, status = SessionStatus.SCHEDULED),
        )

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Design Mockups", state.editTitle)
        assertEquals(15, state.editPoints) // Read-only points
        assertEquals(2, state.sessions.size)
        // 45 + 30 = 75 minutes calculated duration
        assertEquals(75, state.calculatedDurationMinutes)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `adding a new session updates sessions list and recalculates total duration`() = runTest(testDispatcher) {
        val testTask = Task(
            id = "task-1",
            title = "Study Kotlin",
            description = null,
            estimatedDurationMinutes = 0,
            status = TaskStatus.SCHEDULED,
            mandatory = false,
            estimatedPoints = 10,
        )
        fakeTaskRepository.tasks["task-1"] = testTask
        fakeTaskRepository.sessions["task-1"] = emptyList()

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.calculatedDurationMinutes)

        // Add a 60 min session
        val start = LocalDateTime.of(2026, 8, 17, 10, 0)
        val end = LocalDateTime.of(2026, 8, 17, 11, 0)
        val draft = SessionDraft(start = start, end = end)

        viewModel.onAction(TaskDetailsAction.AddSessions(listOf(draft)))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sessions.size)
        assertEquals(60, state.calculatedDurationMinutes)
    }

    @Test
    fun `saveChanges passes calculated duration and preserves points`() = runTest(testDispatcher) {
        val testTask = Task(
            id = "task-1",
            title = "Original Title",
            description = "Original Description",
            estimatedDurationMinutes = 30,
            status = TaskStatus.SCHEDULED,
            mandatory = false,
            estimatedPoints = 25,
        )
        fakeTaskRepository.tasks["task-1"] = testTask
        fakeTaskRepository.sessions["task-1"] = listOf(
            TaskSession(
                id = "s-1",
                start = LocalDateTime.of(2026, 8, 16, 9, 0),
                end = LocalDateTime.of(2026, 8, 16, 9, 45), // 45 min
            )
        )

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(TaskDetailsAction.TitleChanged("Updated Title"))
        viewModel.onAction(TaskDetailsAction.MandatoryToggled(true))
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onAction(TaskDetailsAction.SaveChanges)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedTask = fakeTaskRepository.tasks["task-1"]!!
        assertEquals("Updated Title", savedTask.title)
        assertEquals(45, savedTask.estimatedDurationMinutes) // Calculated from session
        assertEquals(25, savedTask.estimatedPoints) // Points preserved
        assertTrue(savedTask.mandatory)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }
    @Test
    fun `deleting a session requires confirmation and updates calculated duration`() = runTest(testDispatcher) {
        val testTask = Task(
            id = "task-1",
            title = "Design System",
            estimatedDurationMinutes = 60,
            status = TaskStatus.SCHEDULED,
        )
        val s1 = TaskSession(
            id = "s-1",
            start = LocalDateTime.of(2026, 8, 16, 9, 0),
            end = LocalDateTime.of(2026, 8, 16, 9, 30), // 30m
        )
        val s2 = TaskSession(
            id = "s-2",
            start = LocalDateTime.of(2026, 8, 16, 10, 0),
            end = LocalDateTime.of(2026, 8, 16, 10, 45), // 45m
        )
        fakeTaskRepository.tasks["task-1"] = testTask
        fakeTaskRepository.sessions["task-1"] = listOf(s1, s2)

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.sessions.size)
        assertEquals(75, viewModel.uiState.value.calculatedDurationMinutes)

        // 1. Request delete s-1 -> opens confirmation sheet
        viewModel.onAction(TaskDetailsAction.RequestDeleteSession(s1))
        assertEquals(s1, viewModel.uiState.value.sessionToDelete)

        // 2. Cancel delete -> dismisses confirmation sheet
        viewModel.onAction(TaskDetailsAction.CancelDeleteSession)
        assertEquals(null, viewModel.uiState.value.sessionToDelete)
        assertEquals(2, viewModel.uiState.value.sessions.size)

        // 3. Request delete again and Confirm
        viewModel.onAction(TaskDetailsAction.RequestDeleteSession(s1))
        viewModel.onAction(TaskDetailsAction.ConfirmDeleteSession)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sessions.size)
        assertEquals("s-2", state.sessions.single().id)
        assertEquals(45, state.calculatedDurationMinutes)
        assertEquals(listOf("s-1"), fakeSessionRepository.deletedSessions)
    }

    @Test
    fun `loadAll loads goals in parallel so goal titles are available immediately`() = runTest(testDispatcher) {
        val testTask = Task(
            id = "task-1",
            title = "Design System",
            goalId = "goal-1",
            estimatedDurationMinutes = 60,
            status = TaskStatus.SCHEDULED,
        )
        fakeTaskRepository.tasks["task-1"] = testTask
        fakeGoalRepository.goalsList = listOf(
            Goal(id = "goal-1", title = "Launch Mobile App", emoji = "🚀"),
        )

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.goals.size)
        assertEquals("Launch Mobile App", viewModel.uiState.value.goals.first().title)
    }

    @Test
    fun `ShowAddDependencyPicker loads tasks belonging to same goal into goalTasks`() = runTest(testDispatcher) {
        val currentTask = Task(id = "task-1", title = "Task 1", goalId = "goal-1")
        val otherTaskInSameGoal = Task(id = "task-2", title = "Task 2", goalId = "goal-1")
        val taskInDifferentGoal = Task(id = "task-3", title = "Task 3", goalId = "goal-2")

        fakeTaskRepository.tasks["task-1"] = currentTask
        fakeTaskRepository.tasks["task-2"] = otherTaskInSameGoal
        fakeTaskRepository.tasks["task-3"] = taskInDifferentGoal

        viewModel.initTaskId("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(TaskDetailsAction.ShowAddDependencyPicker)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showAddDependencyPicker)
        assertEquals(listOf(currentTask, otherTaskInSameGoal), state.goalTasks)
        val available = state.availableDependencyTasks(state.goalTasks)
        assertEquals(listOf(otherTaskInSameGoal), available)
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeSessionRepository : com.awan.app.core.domain.zones.repository.SessionRepository {
    val deletedSessions = mutableListOf<String>()

    override suspend fun getSessionsByDate(date: LocalDate): Result<List<com.awan.app.core.domain.zones.model.Session>> = error("not used")
    override suspend fun getSessionsByRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, List<com.awan.app.core.domain.zones.model.Session>>> = error("not used")
    override suspend fun getSession(sessionId: String): Result<com.awan.app.core.domain.zones.model.Session> = error("not used")
    override suspend fun updateSession(sessionId: String, params: com.awan.app.core.model.UpdateSessionParams): Result<com.awan.app.core.domain.zones.model.Session> = error("not used")
    override suspend fun lockSession(sessionId: String): Result<com.awan.app.core.domain.zones.model.Session> = error("not used")
    override suspend fun unlockSession(sessionId: String): Result<com.awan.app.core.domain.zones.model.Session> = error("not used")
    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        deletedSessions.add(sessionId)
        return Result.Success(Unit)
    }
}

private class FakeTaskRepository : TaskRepository {
    val tasks = mutableMapOf<String, Task>()
    val sessions = mutableMapOf<String, List<TaskSession>>()
    val dependencies = mutableMapOf<String, List<Task>>()
    val dependents = mutableMapOf<String, List<Task>>()

    override suspend fun createTask(draft: TaskDraft): Result<Task> = error("not used")
    override suspend fun createTaskWithSessions(draft: TaskDraft, sessions: List<SessionDraft>): Result<TaskWithSessions> = error("not used")
    override suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>> = error("not used")
    override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> = error("not used")
    override suspend fun proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?): Result<TaskProposals> = error("not used")
    override suspend fun scheduleTask(taskId: String): Result<TaskSchedule> = error("not used")
    override suspend fun completeTask(taskId: String): Result<Task> = error("not used")
    override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> {
        tasks.remove(taskId)
        return Result.Success(Unit)
    }
    override suspend fun getInboxTasks(): Result<List<TaskWithSessions>> = error("not used")

    override suspend fun getTask(taskId: String): Result<Task> {
        val task = tasks[taskId] ?: return Result.Error(com.awan.app.core.common.error.AppError.NotFound)
        return Result.Success(task)
    }

    override suspend fun updateTask(
        taskId: String,
        title: String?,
        description: String?,
        estimatedDuration: Int?,
        status: String?,
        mandatory: Boolean?,
        estimatedPoints: Int?,
        allowTaskSplitting: Boolean?,
        categoryId: String?,
    ): Result<Task> {
        val existing = tasks[taskId] ?: return Result.Error(com.awan.app.core.common.error.AppError.NotFound)
        val updated = existing.copy(
            title = title ?: existing.title,
            description = description ?: existing.description,
            estimatedDurationMinutes = estimatedDuration ?: existing.estimatedDurationMinutes,
            status = status?.let { TaskStatus.valueOf(it) } ?: existing.status,
            mandatory = mandatory ?: existing.mandatory,
            estimatedPoints = estimatedPoints ?: existing.estimatedPoints,
            allowTaskSplitting = allowTaskSplitting ?: existing.allowTaskSplitting,
            category = existing.category,
        )
        tasks[taskId] = updated
        return Result.Success(updated)
    }

    override suspend fun moveTask(taskId: String, goalId: String?): Result<Task> {
        val existing = tasks[taskId] ?: return Result.Error(com.awan.app.core.common.error.AppError.NotFound)
        val updated = existing.copy(goalId = goalId)
        tasks[taskId] = updated
        return Result.Success(updated)
    }

    override suspend fun addDependency(taskId: String, dependsOnTaskId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun removeDependency(taskId: String, dependsOnTaskId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun getTaskDependencies(taskId: String): Result<List<Task>> = Result.Success(dependencies[taskId] ?: emptyList())
    override suspend fun getTaskDependents(taskId: String): Result<List<Task>> = Result.Success(dependents[taskId] ?: emptyList())
    override suspend fun getTasksByGoal(goalId: String): Result<List<Task>> = Result.Success(tasks.values.filter { it.goalId == goalId })
    override suspend fun getTaskSessions(taskId: String, status: String?): Result<List<TaskSession>> = Result.Success(sessions[taskId] ?: emptyList())

    override suspend fun addTaskSessions(taskId: String, sessions: List<SessionDraft>): Result<List<TaskSession>> {
        val current = this.sessions[taskId] ?: emptyList()
        val created = sessions.mapIndexed { index, s ->
            TaskSession(id = "s-new-$index", start = s.start, end = s.end, status = SessionStatus.SCHEDULED)
        }
        this.sessions[taskId] = current + created
        return Result.Success(created)
    }
}

private class FakeGoalRepository : GoalRepository {
    var goalsList: List<Goal> = emptyList()

    override fun observeGoals(): Flow<List<Goal>> = flowOf(goalsList)
    override fun observeGoal(goalId: String): Flow<Goal?> = flowOf(goalsList.find { it.id == goalId })
    override suspend fun getGoals(): Result<List<Goal>> = Result.Success(goalsList)
    override suspend fun createGoal(title: String, description: String?, targetDate: String?): Result<Goal> = error("not used")
    override suspend fun getInboxGoal(): Result<Goal> = error("not used")
    override suspend fun getGoal(goalId: String): Result<Goal> = error("not used")
    override suspend fun updateGoal(
        goalId: String,
        title: String?,
        description: String?,
        status: String?,
        targetDate: String?,
    ): Result<Goal> = error("not used")
    override suspend fun deleteGoal(goalId: String): Result<Unit> = error("not used")
    override suspend fun continueDecomposition(sessionId: String?, message: String): Result<com.awan.app.core.model.GoalDecompositionReply> = error("not used")
    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> = error("not used")
    override suspend fun getDecompositionTranscript(sessionId: String): Result<com.awan.app.core.model.GoalDecompositionTranscript> = error("not used")
    override suspend fun cancelDecomposition(sessionId: String): Result<Unit> = error("not used")
    override suspend fun scheduleGoal(goalId: String): Result<Unit> = error("not used")
    override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.model.GoalScheduleProposal> = error("not used")
    override suspend fun confirmGoalSchedule(goalId: String, sessions: List<com.awan.app.core.model.ProposedGoalSession>): Result<Unit> = error("not used")
}
