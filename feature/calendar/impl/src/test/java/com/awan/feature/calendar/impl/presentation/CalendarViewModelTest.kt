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

        override fun observeProgress(): Flow<GamificationProgress> = progressFlow

        override fun observeRewards(): Flow<RewardEvent> = emptyFlow()

        override suspend fun refreshProgress(): Result<GamificationProgress> = Result.Success(progressFlow.value)

        override suspend fun getActivityDates(startDate: LocalDate, endDate: LocalDate): Result<Set<LocalDate>> {
            activityRequests.add(startDate to endDate)
            return activityResults[startDate to endDate] ?: Result.Success(emptySet())
        }

        override suspend fun getWheelConfig(): Result<WheelConfig> = error("Not needed")

        override suspend fun spinWheel(): Result<WheelSpinResult> = error("Not needed")

        override suspend fun publishWheelReward(result: WheelSpinResult) = error("Not needed")
    }
}
