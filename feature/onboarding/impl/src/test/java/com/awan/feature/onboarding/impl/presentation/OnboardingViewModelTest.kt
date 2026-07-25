package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.data.task.TaskRepository
import com.awan.app.core.domain.onboarding.DayBoundsValidation
import com.awan.app.core.domain.onboarding.ScheduleFirstTaskUseCase
import com.awan.app.core.domain.onboarding.SuggestZoneScheduleUseCase
import com.awan.app.core.domain.onboarding.ValidateDayBounds
import com.awan.app.core.domain.template.usecase.CreateWeeklyTemplateUseCase
import com.awan.app.core.data.task.CreateTaskUseCase
import com.awan.app.core.model.DayBounds
import com.awan.app.core.network.dto.TaskInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeOnboardingRepository
    private lateinit var fakeTaskRepository: FakeTaskRepository
    private lateinit var fakeTemplateRepository: FakeTemplateRepository
    private lateinit var viewModel: OnboardingViewModel

    private class FakeTaskRepository : TaskRepository {
        var createdTaskTitle: String? = null

        override suspend fun createTask(
            title: String,
            description: String?,
            estimatedDurationMinutes: Int?,
            mandatory: Boolean?,
            estimatedPoints: Int?,
            allowTaskSplitting: Boolean?,
            goalId: String?,
        ): Result<TaskInfoResponse> {
            createdTaskTitle = title
            return Result.Success(
                TaskInfoResponse(
                    id = "task-123",
                    title = title,
                    estimatedDuration = estimatedDurationMinutes,
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeOnboardingRepository()
        fakeTaskRepository = FakeTaskRepository()
        fakeTemplateRepository = FakeTemplateRepository()
        viewModel = OnboardingViewModel(
            repository = repository,
            suggestZoneSchedule = SuggestZoneScheduleUseCase(),
            scheduleFirstTask = ScheduleFirstTaskUseCase(),
            validateDayBounds = ValidateDayBounds(),
            createTaskUseCase = CreateTaskUseCase(fakeTaskRepository),
            createWeeklyTemplate = CreateWeeklyTemplateUseCase(fakeTemplateRepository),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts on Welcome with four suggested zones`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals(OnboardingStep.Welcome, state.step)
        assertEquals(4, state.zones.size)
        assertEquals(SuggestZoneScheduleUseCase()(DayBounds.Default).first().startMinutes, state.zones.first().startMinutes)
    }

    @Test
    fun `continue on the name step is gated on a non-blank first name`() = runTest(testDispatcher) {
        assertFalse(viewModel.state.value.canContinueName)
        viewModel.onAction(OnboardingAction.NameChanged("Sam", ""))
        assertTrue(viewModel.state.value.canContinueName)
    }

    @Test
    fun `same wake and sleep blocks continuing on bounds`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.WakeChanged(23 * 60))
        viewModel.onAction(OnboardingAction.SleepChanged(23 * 60))
        assertEquals(DayBoundsValidation.SameTime, viewModel.state.value.boundsValidation)
        assertFalse(viewModel.state.value.canContinueBounds)
    }

    @Test
    fun `changing bounds re-suggests zones while not user-edited`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.WakeChanged(8 * 60))
        val expected = SuggestZoneScheduleUseCase()(viewModel.state.value.bounds).first().startMinutes
        assertEquals(expected, viewModel.state.value.zones.first().startMinutes)
    }

    @Test
    fun `skipping TaskLength triggers completeOnboarding before entering FirstTask step`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.Next) // Welcome -> Name
        viewModel.onAction(OnboardingAction.NameChanged("Sam", ""))
        viewModel.onAction(OnboardingAction.Next) // Name -> DayBounds
        viewModel.onAction(OnboardingAction.Next) // DayBounds -> Zones
        viewModel.onAction(OnboardingAction.Next) // Zones -> TaskLength

        assertFalse(repository.isCompleted)
        viewModel.onAction(OnboardingAction.Next) // TaskLength -> FirstTask

        assertTrue(repository.isCompleted)
        assertEquals(OnboardingStep.FirstTask, viewModel.state.value.step)
    }

    @Test
    fun `leaving TaskLength sends the configured zones as the weekly template`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.Next) // Welcome -> Name
        viewModel.onAction(OnboardingAction.NameChanged("Sam", ""))
        repeat(3) { viewModel.onAction(OnboardingAction.Next) } // -> TaskLength

        viewModel.onAction(OnboardingAction.Next) // TaskLength -> FirstTask

        assertEquals(viewModel.state.value.zones, fakeTemplateRepository.createdZones)
    }

    @Test
    fun `the weekly template is not sent when completing onboarding fails`() = runTest(testDispatcher) {
        repository.failWith = AppError.Network

        viewModel.onAction(OnboardingAction.SkipSetup)

        assertTrue(repository.isCompleted)
        assertNull(fakeTemplateRepository.createdZones)
    }

    @Test
    fun `skipping every step applies defaults and completes onboarding`() = runTest(testDispatcher) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events += it }
        }

        viewModel.onAction(OnboardingAction.Next) // Welcome -> Name
        repeat(OnboardingStep.DOT_COUNT) { viewModel.onAction(OnboardingAction.Skip) }

        assertEquals(DayBounds.Default, viewModel.state.value.bounds)
        assertEquals(OnboardingData.DEFAULT_TASK_LENGTH_MINUTES, viewModel.state.value.preferredTaskLengthMinutes)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
        assertTrue(repository.isCompleted)
    }

    @Test
    fun `submitting a first task creates task via CreateTaskUseCase, celebrates, and persists`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.FirstTaskTitleChanged("Write brief"))
        viewModel.onAction(OnboardingAction.SubmitFirstTask)

        val state = viewModel.state.value
        assertNotNull(state.firstTask)
        assertTrue(state.celebrateTask)
        assertFalse(state.isSubmittingTask)
        assertEquals("Write brief", state.firstTask?.title)
        assertEquals("Write brief", fakeTaskRepository.createdTaskTitle)
    }

    @Test
    fun `reordering a zone moves its window, not just its row`() {
        val before = viewModel.state.value.zones
        val moved = before.last()

        viewModel.onAction(OnboardingAction.ReorderZone(before.lastIndex, 0))

        val after = viewModel.state.value.zones
        assertEquals(moved.id, after.first().id)
        assertEquals(before.first().startMinutes, after.first().startMinutes)
        assertEquals(moved.durationMinutes, after.first().durationMinutes)
        assertTrue(viewModel.state.value.overlappingZoneIds.isEmpty())
    }

    @Test
    fun `skipping from Welcome completes onboarding with all defaults`() = runTest(testDispatcher) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events += it }
        }

        assertEquals(OnboardingStep.Welcome, viewModel.state.value.step)
        viewModel.onAction(OnboardingAction.SkipSetup)

        assertEquals(DayBounds.Default, viewModel.state.value.bounds)
        assertEquals(OnboardingData.DEFAULT_TASK_LENGTH_MINUTES, viewModel.state.value.preferredTaskLengthMinutes)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
        assertTrue(repository.isCompleted)
    }
}
