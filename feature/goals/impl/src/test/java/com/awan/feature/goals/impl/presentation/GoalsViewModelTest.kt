package com.awan.feature.goals.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.goal.usecase.ObserveGoalsUseCase
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class GoalsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGoalRepository : GoalRepository {
        val goalsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<Goal>>(emptyList())
        var result: Result<List<Goal>> = Result.Success(emptyList())
            set(value) {
                field = value
                if (value is Result.Success) {
                    goalsFlow.value = value.data
                }
            }

        override fun observeGoals(): Flow<List<Goal>> = goalsFlow
        override fun observeGoal(goalId: String): Flow<Goal?> = flowOf(null)
        override suspend fun getGoals(): Result<List<Goal>> = result

        override suspend fun continueDecomposition(
            sessionId: String?,
            message: String,
        ): Result<GoalDecompositionReply> = error("Not used in GoalsViewModelTest")

        override suspend fun confirmDecomposition(sessionId: String): Result<Goal> =
            error("Not used in GoalsViewModelTest")

        override suspend fun createGoal(title: String, description: String?, targetDate: String?): Result<Goal> = error("Not implemented")
        override suspend fun getInboxGoal(): Result<Goal> = error("Not implemented")
        override suspend fun getGoal(goalId: String): Result<Goal> = error("Not implemented")
        override suspend fun updateGoal(
            goalId: String,
            title: String?,
            description: String?,
            status: String?,
            targetDate: String?,
        ): Result<Goal> = error("Not implemented")
        override suspend fun deleteGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun getDecompositionTranscript(sessionId: String): Result<com.awan.app.core.model.GoalDecompositionTranscript> = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String): Result<Unit> = error("Not implemented")
        override suspend fun scheduleGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.model.GoalScheduleProposal> = error("Not implemented")
        override suspend fun confirmGoalSchedule(goalId: String, sessions: List<com.awan.app.core.model.ProposedGoalSession>): Result<Unit> = error("Not implemented")
    }

    private class FakeTaskRepository : TaskRepository {
        override suspend fun createTask(draft: com.awan.app.core.model.TaskDraft): Result<com.awan.app.core.model.Task> = error("not used")
        override suspend fun createTaskWithSessions(draft: com.awan.app.core.model.TaskDraft, sessions: List<com.awan.app.core.model.SessionDraft>): Result<TaskWithSessions> = error("not used")
        override suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<com.awan.app.core.model.Task>> = error("not used")
        override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> = error("not used")
        override suspend fun proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?): Result<TaskProposals> = error("not used")
        override suspend fun scheduleTask(taskId: String): Result<com.awan.app.core.model.TaskSchedule> = error("not used")
        override suspend fun deleteTask(taskId: String): Result<Unit> = error("not used")
        override suspend fun getInboxTasks(): Result<List<TaskWithSessions>> = Result.Success(emptyList())
        override suspend fun completeTask(taskId: String): Result<com.awan.app.core.model.Task> = error("not used")
        override suspend fun moveTask(taskId: String, goalId: String?): Result<com.awan.app.core.model.Task> = error("not used")
    }

    @Test
    fun `initial load populates goals`() {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(id = "g-1", title = "Active Goal 1", emoji = "🎯", status = GoalStatus.ACTIVE),
                    Goal(id = "g-2", title = "Achieved Goal 2", emoji = "🏆", status = GoalStatus.ACHIEVED),
                )
            )
        }
        val viewModel = createViewModel(repo)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(2, state.goals.size)
        assertEquals("g-1", state.goals[0].id)
        assertEquals("g-2", state.goals[1].id)
    }

    @Test
    fun `failure exposes error and retry reloads successfully`() {
        val repo = FakeGoalRepository().apply {
            result = Result.Error(AppError.Network)
        }
        val viewModel = createViewModel(repo)

        val errorState = viewModel.state.value
        assertFalse(errorState.isLoading)
        assertTrue(errorState.isError)

        repo.result = Result.Success(
            listOf(Goal(id = "g-3", title = "Active Goal 3", emoji = "🎯", status = GoalStatus.ACTIVE))
        )
        viewModel.onAction(GoalsAction.RetryClicked)

        val successState = viewModel.state.value
        assertFalse(successState.isLoading)
        assertFalse(successState.isError)
        assertEquals(1, successState.goals.size)
        assertEquals("g-3", successState.goals[0].id)
    }

    @Test
    fun `search query updates state`() {
        val repo = FakeGoalRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.SearchQueryChanged("test"))

        assertEquals("test", viewModel.state.value.searchQuery)
    }

    @Test
    fun `search query filters goals by title correctly`() = runTest {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(id = "g-1", title = "Learn Spanish", emoji = "🇪🇸"),
                    Goal(id = "g-2", title = "Read Clean Code", emoji = "📚"),
                )
            )
        }
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.SearchQueryChanged("Spanish"))

        val filtered = viewModel.state.value.filteredGoals
        assertEquals(1, filtered.size)
        assertEquals("Learn Spanish", filtered[0].title)
    }

    @Test
    fun `status filter works correctly without search query`() = runTest {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(id = "g-1", title = "Active Goal", emoji = "🎯", status = GoalStatus.ACTIVE),
                    Goal(id = "g-2", title = "Achieved Goal", emoji = "🏆", status = GoalStatus.ACHIEVED),
                )
            )
        }
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.FilterClicked)
        viewModel.onAction(GoalsAction.PendingStatusFilterChanged(GoalStatus.ACHIEVED))
        viewModel.onAction(GoalsAction.ApplyFiltersClicked)

        val filtered = viewModel.state.value.filteredGoals
        assertEquals(1, filtered.size)
        assertEquals("Achieved Goal", filtered[0].title)
    }

    @Test
    fun `search query and status filter combined work correctly`() = runTest {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(id = "g-1", title = "Spanish Basics", emoji = "🇪🇸", status = GoalStatus.ACTIVE),
                    Goal(id = "g-2", title = "Spanish Advanced", emoji = "🇪🇸", status = GoalStatus.ACHIEVED),
                    Goal(id = "g-3", title = "Clean Code", emoji = "📚", status = GoalStatus.ACTIVE),
                )
            )
        }
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.SearchQueryChanged("Spanish"))
        viewModel.onAction(GoalsAction.FilterClicked)
        viewModel.onAction(GoalsAction.PendingStatusFilterChanged(GoalStatus.ACHIEVED))
        viewModel.onAction(GoalsAction.ApplyFiltersClicked)

        val filtered = viewModel.state.value.filteredGoals
        assertEquals(1, filtered.size)
        assertEquals("Spanish Advanced", filtered[0].title)
    }

    @Test
    fun `type filter for goals only works correctly`() = runTest {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(
                        id = "g-1",
                        title = "Spanish Goal",
                        emoji = "🇪🇸",
                        tasks = listOf(Task(id = "t-1", title = "Some Task"))
                    ),
                )
            )
        }
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.FilterClicked)
        viewModel.onAction(GoalsAction.PendingTypeFilterChanged(GoalSearchType.GOALS))
        viewModel.onAction(GoalsAction.ApplyFiltersClicked)

        // Query matches goal title
        viewModel.onAction(GoalsAction.SearchQueryChanged("Spanish"))
        assertEquals(1, viewModel.state.value.filteredGoals.size)

        // Query matches task title but filter is set to GOALS
        viewModel.onAction(GoalsAction.SearchQueryChanged("Some"))
        assertEquals(0, viewModel.state.value.filteredGoals.size)
    }

    @Test
    fun `type filter for tasks only works correctly`() = runTest {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(
                        id = "g-1",
                        title = "Spanish Goal",
                        emoji = "🇪🇸",
                        tasks = listOf(Task(id = "t-1", title = "Learn Vocab"))
                    ),
                )
            )
        }
        val viewModel = createViewModel(repo)

        viewModel.onAction(GoalsAction.FilterClicked)
        viewModel.onAction(GoalsAction.PendingTypeFilterChanged(GoalSearchType.TASKS))
        viewModel.onAction(GoalsAction.ApplyFiltersClicked)

        // Query matches task title
        viewModel.onAction(GoalsAction.SearchQueryChanged("Vocab"))
        assertEquals(1, viewModel.state.value.filteredGoals.size)

        // Query matches goal title but filter is set to TASKS
        viewModel.onAction(GoalsAction.SearchQueryChanged("Spanish"))
        assertEquals(0, viewModel.state.value.filteredGoals.size)
    }

    private fun createViewModel(
        goalRepo: GoalRepository,
        taskRepo: TaskRepository = FakeTaskRepository()
    ) = GoalsViewModel(
        getGoalsUseCase = GetGoalsUseCase(goalRepo),
        observeGoalsUseCase = ObserveGoalsUseCase(goalRepo),
        getInboxTasksUseCase = GetInboxTasksUseCase(taskRepo)
    )
}
