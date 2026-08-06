package com.awan.feature.goals.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
        var result: Result<List<Goal>> = Result.Success(emptyList())

        override suspend fun getGoals(): Result<List<Goal>> = result

        override suspend fun continueDecomposition(
            sessionId: String?,
            message: String,
        ): Result<GoalDecompositionReply> = error("Not used in GoalsViewModelTest")

        override suspend fun confirmDecomposition(sessionId: String): Result<Goal> =
            error("Not used in GoalsViewModelTest")
    }

    @Test
    fun `initial load partitions active and achieved goals`() {
        val repo = FakeGoalRepository().apply {
            result = Result.Success(
                listOf(
                    Goal(id = "g-1", title = "Active Goal 1", emoji = "🎯", status = GoalStatus.ACTIVE),
                    Goal(id = "g-2", title = "Achieved Goal 2", emoji = "🏆", status = GoalStatus.ACHIEVED),
                )
            )
        }
        val viewModel = GoalsViewModel(GetGoalsUseCase(repo))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(1, state.activeGoals.size)
        assertEquals("g-1", state.activeGoals[0].id)
        assertEquals(1, state.completedGoals.size)
        assertEquals("g-2", state.completedGoals[0].id)
    }

    @Test
    fun `failure exposes error and retry reloads successfully`() {
        val repo = FakeGoalRepository().apply {
            result = Result.Error(AppError.Network)
        }
        val viewModel = GoalsViewModel(GetGoalsUseCase(repo))

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
        assertEquals(1, successState.activeGoals.size)
        assertEquals("g-3", successState.activeGoals[0].id)
    }

    @Test
    fun `tab selection changes only the selected tab`() {
        val repo = FakeGoalRepository()
        val viewModel = GoalsViewModel(GetGoalsUseCase(repo))

        assertEquals(GoalsTab.Active, viewModel.state.value.tab)

        viewModel.onAction(GoalsAction.TabSelected(GoalsTab.Completed))

        assertEquals(GoalsTab.Completed, viewModel.state.value.tab)
    }
}
