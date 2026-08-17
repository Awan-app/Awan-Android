package com.awan.feature.goals.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTaskRepository : TaskRepository {
        var inboxTasksResult: Result<List<Task>> = Result.Success(emptyList())

        override suspend fun getInboxTasks(): Result<List<Task>> = inboxTasksResult

        override suspend fun createTask(draft: TaskDraft): Result<Task> = error("not used")
        override suspend fun createTaskWithSessions(draft: TaskDraft, sessions: List<SessionDraft>): Result<TaskWithSessions> = error("not used")
        override suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>> = error("not used")
        override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> = error("not used")
        override suspend fun proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?): Result<TaskProposals> = error("not used")
        override suspend fun scheduleTask(taskId: String): Result<TaskSchedule> = error("not used")
        override suspend fun completeTask(taskId: String): Result<Task> = error("not used")
        override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> = error("not used")
    }

    private fun createViewModel(repo: FakeTaskRepository): InboxViewModel {
        return InboxViewModel(
            getInboxTasksUseCase = GetInboxTasksUseCase(repo),
        )
    }

    @Test
    fun `initial load populates inbox tasks successfully`() = runTest {
        val sampleTasks = listOf(
            Task(id = "1", title = "Task 1", description = "Desc 1", status = TaskStatus.INBOX),
            Task(id = "2", title = "Task 2", description = "Desc 2", status = TaskStatus.SCHEDULED),
        )
        val repo = FakeTaskRepository().apply {
            inboxTasksResult = Result.Success(sampleTasks)
        }

        val viewModel = createViewModel(repo)
        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(2, state.allTasks.size)
        assertEquals(2, state.visibleTasks.size)
        assertEquals("Task 1", state.visibleTasks[0].title)
        assertEquals(InboxTaskDisplayStatus.Drafted, state.visibleTasks[0].displayStatus)
        assertEquals(InboxTaskDisplayStatus.Active, state.visibleTasks[1].displayStatus)
    }

    @Test
    fun `initial load sets error state on failure`() = runTest {
        val repo = FakeTaskRepository().apply {
            inboxTasksResult = Result.Error(AppError.Network)
        }

        val viewModel = createViewModel(repo)
        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertTrue(state.allTasks.isEmpty())
        assertTrue(state.visibleTasks.isEmpty())
    }

    @Test
    fun `search query filters visible tasks by title and description`() = runTest {
        val sampleTasks = listOf(
            Task(id = "1", title = "Buy Groceries", description = "Milk and eggs", status = TaskStatus.INBOX),
            Task(id = "2", title = "Read Book", description = "Clean Architecture", status = TaskStatus.INBOX),
            Task(id = "3", title = "Clean Room", description = null, status = TaskStatus.INBOX),
        )
        val repo = FakeTaskRepository().apply {
            inboxTasksResult = Result.Success(sampleTasks)
        }

        val viewModel = createViewModel(repo)
        
        viewModel.onAction(InboxAction.SearchQueryChanged("Clean"))
        val state = viewModel.state.value
        assertEquals(2, state.visibleTasks.size)
        assertEquals(listOf("2", "3"), state.visibleTasks.map { it.id })
    }

    @Test
    fun `status filter toggling correctly filters visible tasks`() = runTest {
        val sampleTasks = listOf(
            Task(id = "1", title = "Draft task", status = TaskStatus.INBOX),
            Task(id = "2", title = "Active task", status = TaskStatus.SCHEDULED),
            Task(id = "3", title = "Completed task", status = TaskStatus.COMPLETED),
        )
        val repo = FakeTaskRepository().apply {
            inboxTasksResult = Result.Success(sampleTasks)
        }

        val viewModel = createViewModel(repo)
        
        viewModel.onAction(InboxAction.StatusFilterToggled(InboxTaskDisplayStatus.Active))
        var state = viewModel.state.value
        assertEquals(1, state.visibleTasks.size)
        assertEquals("2", state.visibleTasks[0].id)

        // Untoggle filter
        viewModel.onAction(InboxAction.StatusFilterToggled(InboxTaskDisplayStatus.Active))
        state = viewModel.state.value
        assertEquals(3, state.visibleTasks.size)
    }

    @Test
    fun `back click emits NavigateBack event`() = runTest {
        val repo = FakeTaskRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAction(InboxAction.BackClicked)
        val event = viewModel.events.first()

        assertEquals(InboxEvent.NavigateBack, event)
    }
}
