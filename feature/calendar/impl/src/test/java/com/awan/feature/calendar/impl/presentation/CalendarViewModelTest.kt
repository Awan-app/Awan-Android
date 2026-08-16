package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.calendar.repository.CalendarRepository
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import com.awan.app.core.domain.gamification.usecase.GetActivityDatesUseCase
import com.awan.app.core.domain.gamification.usecase.ObserveGamificationProgressUseCase
import com.awan.app.core.model.CalendarUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @Test
    fun gamificationProgressTriggersTodayActivityQueryAndUpdatesHeaderStateToCelebrate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            fakeGamificationRepository.activityResults[today to today] = Result.Success(setOf(today))

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 3, maxStreak = 10)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(3, state.streak)
            assertEquals(10, state.maxStreak)
            assertTrue(state.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Celebrate, state.streakHeaderState)
            assertTrue(fakeGamificationRepository.activityRequests.contains(today to today))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayActivityQueryFailureResetsTodayActiveToFalseAndProtectsActiveStreak() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            fakeGamificationRepository.activityResults[today to today] = Result.Error(AppError.Network)

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 3, maxStreak = 10)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(3, state.streak)
            assertFalse(state.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Protect, state.streakHeaderState)
            assertTrue(fakeGamificationRepository.activityRequests.contains(today to today))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun changingMonthDoesNotChangeTodayActiveOrHeaderState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            fakeGamificationRepository.activityResults[today to today] = Result.Success(setOf(today))

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 3, maxStreak = 10)
            advanceUntilIdle()

            val initialHeaderState = viewModel.state.value.streakHeaderState
            assertEquals(CalendarStreakHeaderState.Celebrate, initialHeaderState)

            viewModel.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()

            val updatedState = viewModel.state.value
            assertEquals(initialHeaderState, updatedState.streakHeaderState)
            assertTrue(updatedState.isTodayActive)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun snapshotRenderPreservesMaxStreakAndRecomputesState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 2, maxStreak = 8)

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            val user = CalendarUser(id = "user1", streak = 2, timezone = "UTC")
            fakeCalendarRepository.emitSnapshot(CalendarSnapshot(user = user, goals = emptyList()))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(8, state.maxStreak)
            assertEquals(2, state.streak)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun laterSnapshotWithDifferentUserStreakDoesNotOverwriteProgressStreakOrHeaderState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            fakeGamificationRepository.activityResults[today to today] = Result.Success(setOf(today))

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            // Progress authority emits streak 5, max 10
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 5, maxStreak = 10)
            advanceUntilIdle()

            val initialState = viewModel.state.value
            assertEquals(5, initialState.streak)
            assertEquals(10, initialState.maxStreak)
            assertTrue(initialState.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Celebrate, initialState.streakHeaderState)

            // Later snapshot arrives with a different user streak (2)
            val user = CalendarUser(id = "user1", streak = 2, timezone = java.time.ZoneId.systemDefault().id)
            fakeCalendarRepository.emitSnapshot(CalendarSnapshot(user = user, goals = emptyList()))
            advanceUntilIdle()

            // Verify progress-derived streak/max and resulting header state were NOT overwritten by snapshot's streak
            val updatedState = viewModel.state.value
            assertEquals(5, updatedState.streak)
            assertEquals(10, updatedState.maxStreak)
            assertTrue(updatedState.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Celebrate, updatedState.streakHeaderState)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun olderSameDayActivityQueryCompletionCannotOverwriteNewerRequest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            var request1Handled = false
            val request1Completer = CompletableDeferred<Result<Set<LocalDate>>>()

            fakeGamificationRepository.getActivityDatesHandler = { start, end ->
                if (!request1Handled) {
                    request1Handled = true
                    request1Completer.await()
                } else {
                    Result.Error(AppError.Network)
                }
            }

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            // Trigger Request 1 (will suspend waiting for request1Completer)
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 3, maxStreak = 10)
            testScheduler.runCurrent()

            // Trigger Request 2 (will complete immediately with Error)
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 105, streak = 3, maxStreak = 10)
            advanceUntilIdle()

            val stateAfterRequest2 = viewModel.state.value
            assertFalse(stateAfterRequest2.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Protect, stateAfterRequest2.streakHeaderState)

            // Now Request 1 finishes with Success
            request1Completer.complete(Result.Success(setOf(today)))
            advanceUntilIdle()

            // Cancelled Request 1 must NOT overwrite state to Celebrate
            val finalState = viewModel.state.value
            assertFalse(finalState.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Protect, finalState.streakHeaderState)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun nonCooperativeOlderSameDayActivityQueryCompletionCannotOverwriteNewerRequest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            var request1Handled = false
            val request1Completer = CompletableDeferred<Result<Set<LocalDate>>>()

            fakeGamificationRepository.getActivityDatesHandler = { start, end ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    if (!request1Handled) {
                        request1Handled = true
                        try {
                            request1Completer.await()
                        } catch (e: Throwable) {
                            // Ignore cancellation
                        }
                        Result.Success(setOf(today))
                    } else {
                        Result.Error(AppError.Network)
                    }
                }
            }

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            // Trigger Request 1 (will suspend waiting for request1Completer)
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 3, maxStreak = 10)
            testScheduler.runCurrent()

            // Trigger Request 2 (will complete immediately with Error)
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 105, streak = 3, maxStreak = 10)
            advanceUntilIdle()

            val stateAfterRequest2 = viewModel.state.value
            assertFalse(stateAfterRequest2.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Protect, stateAfterRequest2.streakHeaderState)

            // Now Request 1 finishes with Success despite being cancelled
            request1Completer.complete(Result.Success(setOf(today)))
            advanceUntilIdle()

            // Non-cooperative Request 1 must NOT overwrite state to Celebrate
            val finalState = viewModel.state.value
            assertFalse(finalState.isTodayActive)
            assertEquals(CalendarStreakHeaderState.Protect, finalState.streakHeaderState)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayActivityCompletionAddsTodayToStreakDatesAndRebuildsMonthDays() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val today = LocalDate.now()
            fakeGamificationRepository.activityResults[today to today] = Result.Success(setOf(today))

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.streakDates.contains(today))
            val todayDayState = state.monthDays.first { it.date == today }
            assertTrue(todayDayState.isStreakDay)
            assertTrue(state.isTodayActive)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun progressFlowEmissionRecalculatesStreakDatesAndMonthDays() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            val today = viewModel.state.value.today
            fakeGamificationRepository.progressFlow.value = GamificationProgress(points = 100, streak = 4, maxStreak = 10)
            advanceUntilIdle()

            // Today has no activity (fake returns emptySet), so today must be excluded from streakDates.
            // calculateStreakDates includes today in the initial estimate but loadTodayActivity corrects it.
            val expectedStreakDates = CalendarDateMapper.calculateStreakDates(4, today) - today
            val state = viewModel.state.value
            assertEquals(4, state.streak)
            assertEquals(expectedStreakDates, state.streakDates)
            assertFalse("Today must not be a streak day when today has no activity", state.streakDates.contains(today))
            for (date in expectedStreakDates) {
                val dayState = state.monthDays.firstOrNull { it.date == date }
                if (dayState != null) {
                    assertTrue("Expected dayState for $date to have isStreakDay = true", dayState.isStreakDay)
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun snapshotEmissionOnInitialLoadUsesUserStreakForHeaderAndGrid() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val fakeCalendarRepository = FakeCalendarRepository()
            val fakeGamificationRepository = FakeGamificationRepository()
            val getActivityDatesUseCase = GetActivityDatesUseCase(fakeGamificationRepository)
            val observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository)

            val viewModel = CalendarViewModel(fakeCalendarRepository, getActivityDatesUseCase, observeGamificationProgressUseCase)
            advanceUntilIdle()

            val today = viewModel.state.value.today
            val user = CalendarUser(id = "user1", streak = 5, timezone = java.time.ZoneId.systemDefault().id)
            fakeCalendarRepository.emitSnapshot(CalendarSnapshot(user = user, goals = emptyList()))
            advanceUntilIdle()

            // Today has no activity (fake returns emptySet), so today must be excluded from streakDates.
            val expectedStreakDates = CalendarDateMapper.calculateStreakDates(5, today) - today
            val state = viewModel.state.value
            assertEquals(5, state.streak)
            assertEquals(CalendarStreakHeaderState.from(5, state.maxStreak, state.isTodayActive), state.streakHeaderState)
            assertEquals(expectedStreakDates, state.streakDates)
            assertFalse("Today must not be a streak day when today has no activity", state.streakDates.contains(today))
            for (date in expectedStreakDates) {
                val dayState = state.monthDays.firstOrNull { it.date == date }
                if (dayState != null) {
                    assertTrue("Expected dayState for $date to have isStreakDay = true", dayState.isStreakDay)
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeCalendarRepository : CalendarRepository {
        private val calendarFlow = MutableStateFlow<CalendarSnapshot?>(null)

        override fun observeCalendar(): Flow<CalendarSnapshot?> = calendarFlow

        override suspend fun refresh(): Result<Unit> = Result.Success(Unit)

        fun emitSnapshot(snapshot: CalendarSnapshot?) {
            calendarFlow.value = snapshot
        }
    }

    private class FakeGamificationRepository : GamificationRepository {
        val progressFlow = MutableStateFlow(GamificationProgress(points = 0, streak = 0, maxStreak = 0))
        val activityRequests = mutableListOf<Pair<LocalDate, LocalDate>>()
        val activityResults = mutableMapOf<Pair<LocalDate, LocalDate>, Result<Set<LocalDate>>>()
        var getActivityDatesHandler: (suspend (LocalDate, LocalDate) -> Result<Set<LocalDate>>)? = null

        override fun observeProgress(): Flow<GamificationProgress> = progressFlow

        override fun observeRewards(): Flow<RewardEvent> = emptyFlow()

        override suspend fun refreshProgress(): Result<GamificationProgress> = Result.Success(progressFlow.value)

        override suspend fun getActivityDates(startDate: LocalDate, endDate: LocalDate): Result<Set<LocalDate>> {
            activityRequests.add(startDate to endDate)
            return getActivityDatesHandler?.invoke(startDate, endDate)
                ?: activityResults[startDate to endDate]
                ?: Result.Success(emptySet())
        }

        override suspend fun getWheelConfig(): Result<WheelConfig> = error("Not needed")

        override suspend fun spinWheel(): Result<WheelSpinResult> = error("Not needed")

        override suspend fun publishWheelReward(result: WheelSpinResult) = error("Not needed")

        override fun publishReward(event: RewardEvent) {}
    }
}
