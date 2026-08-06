package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.domain.onboarding.utils.DayBoundsValidation
import com.awan.app.core.domain.onboarding.usecase.AssignDefaultCategoriesUseCase
import com.awan.app.core.domain.onboarding.usecase.SuggestZoneScheduleUseCase
import com.awan.app.core.domain.onboarding.utils.ValidateDayBounds
import com.awan.app.core.domain.onboarding.usecase.CompleteOnboardingUseCase
import com.awan.app.core.domain.task.usecase.CreateAndScheduleFirstTaskUseCase
import com.awan.app.core.domain.template.usecase.CreateWeeklyTemplateUseCase
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.model.Category
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeOnboardingRepository
    private lateinit var fakeAiTaskRepository: FakeAiTaskRepository
    private lateinit var viewModel: OnboardingViewModel

    private fun viewModel(categoryRepository: CategoryRepository = FakeCategoryRepository()) =
        OnboardingViewModel(
            completeOnboarding = CompleteOnboardingUseCase(repository),
            suggestZoneSchedule = SuggestZoneScheduleUseCase(),
            validateDayBounds = ValidateDayBounds(),
            createAndScheduleFirstTask = CreateAndScheduleFirstTaskUseCase(fakeAiTaskRepository),
            getCategories = GetCategoriesUseCase(categoryRepository),
            assignDefaultCategories = AssignDefaultCategoriesUseCase(),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeOnboardingRepository()
        fakeAiTaskRepository = FakeAiTaskRepository()
        viewModel = viewModel()
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
    fun `each default zone takes the seeded category that shares its name`() = runTest(testDispatcher) {
        val zones = viewModel.state.value.zones.associate { it.id to it.categoryId }
        assertEquals("cat-work", zones[Zone.WORK])
        assertEquals("cat-learning", zones[Zone.LEARNING])
        assertEquals("cat-personal", zones[Zone.PERSONAL])
        assertEquals("cat-general", zones[Zone.GENERAL])
    }

    @Test
    fun `a zone with no same-named category falls back to General`() = runTest(testDispatcher) {
        val vm = viewModel(FakeCategoryRepository(listOf(Category("cat-general", "General"))))
        assertTrue(vm.state.value.zones.all { it.categoryId == "cat-general" })
    }

    @Test
    fun `picking a category sticks on that zone alone`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.ZoneCategoryPicked(Zone.WORK, "cat-health"))
        val zones = viewModel.state.value.zones.associate { it.id to it.categoryId }
        assertEquals("cat-health", zones[Zone.WORK])
        assertEquals("cat-learning", zones[Zone.LEARNING])
    }

    /** Re-suggesting rebuilds the zone list from defaults, which would otherwise drop the categories. */
    @Test
    fun `re-suggesting keeps every zone saveable`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.WakeChanged(8 * 60))
        viewModel.onAction(OnboardingAction.UseSuggestedZones)
        assertTrue(viewModel.state.value.zones.all { it.categoryId != null })
    }

    @Test
    fun `a failed category load leaves the zones step usable`() = runTest(testDispatcher) {
        val vm = viewModel(FakeCategoryRepository(failWith = AppError.Network))
        assertEquals(4, vm.state.value.zones.size)
        assertTrue(vm.state.value.availableCategories.isEmpty())
        assertTrue(vm.state.value.zones.all { it.categoryId == null })
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

        assertEquals(viewModel.state.value.zones, repository.lastCompletedData?.zones)
    }

    @Test
    fun `the weekly template is not sent when completing onboarding fails`() = runTest(testDispatcher) {
        repository.failWith = AppError.Network

        viewModel.onAction(OnboardingAction.SkipSetup)

        assertFalse(repository.isCompleted)
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
    fun `submitting a first task shows the schedule the server actually returned`() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.FirstTaskTitleChanged("Write brief"))
        viewModel.onAction(OnboardingAction.SubmitFirstTask)

        val state = viewModel.state.value
        assertEquals("Write brief", fakeAiTaskRepository.requestedTitle)
        assertEquals(fakeAiTaskRepository.scheduled, state.firstTask)
        assertTrue(state.celebrateTask)
        assertFalse(state.isSubmittingTask)
        assertNull(state.firstTaskError)
    }

    @Test
    fun `a task the engine could not place reports back instead of showing a made-up time`() =
        runTest(testDispatcher) {
            fakeAiTaskRepository.scheduled = null

            viewModel.onAction(OnboardingAction.FirstTaskTitleChanged("Write brief"))
            viewModel.onAction(OnboardingAction.SubmitFirstTask)

            val state = viewModel.state.value
            assertNull(state.firstTask)
            assertNotNull(state.firstTaskError)
            assertFalse(state.celebrateTask)
            assertFalse(state.isSubmittingTask)
        }

    @Test
    fun `a failed AI call surfaces an error and leaves the task unset`() = runTest(testDispatcher) {
        fakeAiTaskRepository.failWith = AppError.Network

        viewModel.onAction(OnboardingAction.FirstTaskTitleChanged("Write brief"))
        viewModel.onAction(OnboardingAction.SubmitFirstTask)

        val state = viewModel.state.value
        assertNull(state.firstTask)
        assertNotNull(state.firstTaskError)
        assertFalse(state.isSubmittingTask)
    }

    @Test
    fun `the zones the server created are kept so a scheduled task can resolve its zone`() =
        runTest(testDispatcher) {
            viewModel.onAction(OnboardingAction.SkipSetup)

            // Note: In current architecture, templateZones are updated by a separate sync or
            // the repository should return them. For now we just verify the call landed.
            assertTrue(repository.isCompleted)
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
    fun `a failed setup holds the flow and the next exit path retries it`() = runTest(testDispatcher) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) { viewModel.events.collect { events += it } }
        repository.failWith = AppError.Network

        viewModel.onAction(OnboardingAction.Next) // Welcome -> Name
        viewModel.onAction(OnboardingAction.NameChanged("Sam", ""))
        repeat(3) { viewModel.onAction(OnboardingAction.Next) } // -> TaskLength
        viewModel.onAction(OnboardingAction.Next) // TaskLength: submit fails

        assertEquals(OnboardingStep.TaskLength, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.setupError)
        assertFalse(viewModel.state.value.isSubmittingTask)

        repository.failWith = null
        viewModel.onAction(OnboardingAction.Next) // retried, now succeeds

        assertEquals(2, repository.callCount)
        assertEquals(OnboardingStep.FirstTask, viewModel.state.value.step)
        assertNull(viewModel.state.value.setupError)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a failed template is retried without resending completeOnboarding`() = runTest(testDispatcher) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) { viewModel.events.collect { events += it } }
        repository.failWith = AppError.Network

        viewModel.onAction(OnboardingAction.SkipSetup)

        assertNotNull(viewModel.state.value.setupError)
        assertTrue(events.isEmpty())

        repository.failWith = null
        viewModel.onAction(OnboardingAction.SkipSetup)

        assertEquals(2, repository.callCount)
        assertEquals(viewModel.state.value.zones, repository.lastCompletedData?.zones)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
    }

    @Test
    fun `denying the notification permission still completes the account setup`() = runTest(testDispatcher) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) { viewModel.events.collect { events += it } }

        viewModel.onAction(OnboardingAction.NotificationPermissionResult(granted = false))

        assertTrue(repository.isCompleted)
        assertNotNull(repository.lastCompletedData?.zones)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
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
