package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.data.onboarding.InMemoryOnboardingRepository
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.model.DayBounds
import com.awan.app.core.domain.onboarding.DayBoundsValidation
import com.awan.app.core.domain.onboarding.ScheduleFirstTaskUseCase
import com.awan.app.core.domain.onboarding.SuggestZoneScheduleUseCase
import com.awan.app.core.domain.onboarding.ValidateDayBounds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: InMemoryOnboardingRepository
    private lateinit var viewModel: OnboardingViewModel

    private val fakeApiService = object : com.awan.app.core.network.api.OnboardingApiService {
        override suspend fun completeOnboarding(
            request: com.awan.app.core.network.dto.CompleteOnboardingRequest
        ): com.awan.app.core.network.dto.CompleteOnboardingResponse {
            return com.awan.app.core.network.dto.CompleteOnboardingResponse(id = "fake-id")
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryOnboardingRepository(onboardingApiService = fakeApiService)
        viewModel = OnboardingViewModel(
            repository = repository,
            suggestZoneSchedule = SuggestZoneScheduleUseCase(),
            scheduleFirstTask = ScheduleFirstTaskUseCase(),
            validateDayBounds = ValidateDayBounds(),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts on Welcome with four suggested zones`() = runTest(testDispatcher.scheduler) {
        val state = viewModel.state.value
        assertEquals(OnboardingStep.Welcome, state.step)
        assertEquals(4, state.zones.size)
        assertEquals(SuggestZoneScheduleUseCase()(DayBounds.Default).first().startMinutes, state.zones.first().startMinutes)
    }

    @Test
    fun `continue on the name step is gated on a non-blank first name`() = runTest(testDispatcher.scheduler) {
        assertFalse(viewModel.state.value.canContinueName)
        viewModel.onAction(OnboardingAction.NameChanged("Sam", ""))
        assertTrue(viewModel.state.value.canContinueName)
    }

    @Test
    fun `same wake and sleep blocks continuing on bounds`() = runTest(testDispatcher.scheduler) {
        viewModel.onAction(OnboardingAction.WakeChanged(23 * 60))
        viewModel.onAction(OnboardingAction.SleepChanged(23 * 60))
        assertEquals(DayBoundsValidation.SameTime, viewModel.state.value.boundsValidation)
        assertFalse(viewModel.state.value.canContinueBounds)
    }

    @Test
    fun `changing bounds re-suggests zones while not user-edited`() = runTest(testDispatcher.scheduler) {
        viewModel.onAction(OnboardingAction.WakeChanged(8 * 60))
        val expected = SuggestZoneScheduleUseCase()(viewModel.state.value.bounds).first().startMinutes
        assertEquals(expected, viewModel.state.value.zones.first().startMinutes)
    }

    @Test
    fun `skipping every step applies defaults and completes onboarding`() = runTest(testDispatcher.scheduler) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events += it }
        }

        viewModel.onAction(OnboardingAction.Next) // Welcome -> Name
        repeat(OnboardingStep.DOT_COUNT) { viewModel.onAction(OnboardingAction.Skip) }

        assertEquals(DayBounds.Default, viewModel.state.value.bounds)
        assertEquals(OnboardingData.DEFAULT_TASK_LENGTH_MINUTES, viewModel.state.value.preferredTaskLengthMinutes)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
        assertTrue(repository.draft.first().completed)
    }

    @Test
    fun `submitting a first task schedules it, celebrates, and persists`() = runTest(testDispatcher.scheduler) {
        viewModel.onAction(OnboardingAction.FirstTaskTitleChanged("Write brief"))
        viewModel.onAction(OnboardingAction.SubmitFirstTask)

        val state = viewModel.state.value
        assertNotNull(state.firstTask)
        assertTrue(state.celebrateTask)
        assertFalse(state.isSubmittingTask)
        assertEquals("Write brief", repository.draft.first().firstTask?.title)
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
    fun `skipping from Welcome completes onboarding with all defaults`() = runTest(testDispatcher.scheduler) {
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { events += it }
        }

        assertEquals(OnboardingStep.Welcome, viewModel.state.value.step)
        viewModel.onAction(OnboardingAction.SkipSetup)

        assertEquals(DayBounds.Default, viewModel.state.value.bounds)
        assertEquals(OnboardingData.DEFAULT_TASK_LENGTH_MINUTES, viewModel.state.value.preferredTaskLengthMinutes)
        assertTrue(events.contains(OnboardingEvent.NavigateHome))
        assertTrue(repository.draft.first().completed)
    }
}
