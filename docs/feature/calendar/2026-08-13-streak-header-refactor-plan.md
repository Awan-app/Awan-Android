# AWAN-140: Calendar Four-State Streak Header Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Calendar's static streak summary card with a four-state server-backed header driven by `GamificationProgress` and today's activity status, deferring insights/reports.

**Architecture:** MVI state transformation in `CalendarUiState`/`CalendarViewModel` mapping `streak`, `maxStreak`, and `isTodayActive` to `CalendarStreakHeaderState` enum; `CalendarScreen` renders immutable UI state via state-tinted `AwanCard` with fallback icons and no dynamic animation code/dependencies.

**Tech Stack:** Kotlin 2.4.0, Jetpack Compose BOM 2026.06.01, Now in Android MVI architecture, Hilt DI, Coroutines Flow, `kotlinx-coroutines-test`.

## Global Constraints

- **Scope & Boundary**: Replace Calendar's static streak summary with a four-state server-backed header; insights and reports are deferred.
- **State Source**: Existing server-owned `GamificationProgress.streak` and `maxStreak` observed by `ObserveGamificationProgressUseCase`, plus existing `GetActivityDatesUseCase(today, today)` as the sole authority for completion today.
- **Snapshot & MaxStreak Rules**: `CalendarSnapshot.user` is `CalendarUser(id, streak, timezone)` and has no `maxStreak`. `maxStreak` comes strictly from `GamificationProgress`. `render(snapshot)` must preserve the current `maxStreak`.
- **Strict Activity Fallback Rule**: Pending or failed today activity query means not completed: an active streak stays `Protect`, never falsely `Celebrate`. Loading visible month activity dates (`loadActivityDates(month)`) must update grid dates only and must NOT change `isTodayActive` or the header state.
- **Timezone Day Change & Today Query Rule**: Production wiring uses a private `loadTodayActivity(today: LocalDate)` helper in `CalendarViewModel`. It immediately sets `isTodayActive = false` and recomputes the header state using current `streak`/`maxStreak`, then requests `getActivityDatesUseCase(today, today)`. Only if `_state.value.today == requestedToday` does a successful result update `isTodayActive = true` (if it contains `today`). On error it leaves `isTodayActive = false`. When `render(snapshot)` detects a timezone day change (`today != current.today`), it invokes `loadTodayActivity(today)`. On `GamificationProgress` emissions, update clamped `streak`/`maxStreak`, recompute state with `isTodayActive = false`, and call `loadTodayActivity(_state.value.today)`.
- **Internal Presentation API**: `CalendarStreakHeaderState { Start, Restart, Protect, Celebrate }`. Resolver takes `(streak: Int, maxStreak: Int, isTodayActive: Boolean)` and clamps negative counts to `0` first.
- **Exact State Resolution Rules**:
  - `Start`: `streak == 0 && maxStreak == 0`
  - `Restart`: `streak == 0 && maxStreak > 0`
  - `Protect`: `streak > 0 && !isTodayActive` (today inactive or unknown/failed)
  - `Celebrate`: `streak > 0 && isTodayActive` (today active)
- **Verbatim Copy Requirements**:
  - `Start`: Title `Start your streak` / Subtitle `Complete a task today to light your fire.`
  - `Restart`: Title `Ready to start again?` / Subtitle `Your last streak ended. Complete a task today to begin a new one.`
  - `Protect`: Title `%1$d-day streak at risk` / Subtitle `Complete a task today to keep your fire burning.`
  - `Celebrate`: Title `%1$d-day streak! You\'re on fire` / Subtitle `You completed today\'s task. Keep it going tomorrow.`
  - Both English (`res/values/strings.xml`) and Arabic (`res/values-ar/strings.xml`) MUST be added. Arabic string XML values must use ASCII XML numeric character references.
- **Design Specifications**: State-reactive `AwanCard` (not `AwanSurface`) with state-tinted face/rim and translucent layered frosted circles using existing theme colors (`zoneSun`, `zoneCoral`, `zoneTangerine`, `sky`).
  - `Start`: cool sky (`AwanTheme.colors.sky`)
  - `Restart`: muted coral (`AwanTheme.colors.zoneCoral`)
  - `Protect`: amber/tangerine urgency (`AwanTheme.colors.zoneTangerine`)
  - `Celebrate`: sun/coral concentric bright circles (`AwanTheme.colors.zoneSun` / `AwanTheme.colors.zoneCoral`)
  - Informational only — no CTAs.
- **Future Animation Boundary**: No animation assets, Lottie, blur, new dependency, CTA, or animation code. Four explicit, private fallback-icon branches using existing `ic_flame_filled`, with stable test tags `streak_header_start_icon`, `streak_header_restart_icon`, `streak_header_protect_icon`, `streak_header_celebrate_icon`; root card keeps `streak_summary_card` and has variant tag `streak_header_<state>` (`streak_header_start`, `streak_header_restart`, `streak_header_protect`, `streak_header_celebrate`).
- **MVI Layering**: State transformation belongs strictly in `CalendarUiState`/`CalendarViewModel`; `CalendarScreen` renders immutable state only; do not introduce repository/API/persistence/schema changes.
- **Testing Standard**: No Mockito. Use small local test fakes in `CalendarViewModelTest.kt` implementing `CalendarRepository` and `GamificationRepository`.
- **Dead Code Cleanup**: Remove the unreferenced `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/ui/CalendarScreen.kt` in Task 3 Green step.
- **Build Note**: App assemble (`./gradlew assembleDebug`) is blocked without `app/google-services.json`. Use focused test/compile commands per task gate.

---

### Task 1: Add CalendarStreakHeaderState API and Resolver Unit Tests

**Files:**
- Modify: `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarContract.kt`
- Create: `feature/calendar/impl/src/test/java/com/awan/feature/calendar/impl/presentation/CalendarStreakHeaderStateTest.kt`

**Interfaces:**
- Consumes: None
- Produces: `CalendarStreakHeaderState` enum (`Start`, `Restart`, `Protect`, `Celebrate`) with `from(streak: Int, maxStreak: Int, isTodayActive: Boolean)` resolver in `CalendarContract.kt`.

- [ ] **Step 1: Write failing unit test for CalendarStreakHeaderState mapping**

Create `feature/calendar/impl/src/test/java/com/awan/feature/calendar/impl/presentation/CalendarStreakHeaderStateTest.kt`:

```kotlin
package com.awan.feature.calendar.impl.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarStreakHeaderStateTest {

    @Test
    fun mapsZeroStreakAndZeroMaxStreakToStart() {
        val state = CalendarStreakHeaderState.from(streak = 0, maxStreak = 0, isTodayActive = false)
        assertEquals(CalendarStreakHeaderState.Start, state)
    }

    @Test
    fun mapsZeroStreakAndPositiveMaxStreakToRestart() {
        val state = CalendarStreakHeaderState.from(streak = 0, maxStreak = 5, isTodayActive = false)
        assertEquals(CalendarStreakHeaderState.Restart, state)
    }

    @Test
    fun mapsPositiveStreakAndInactiveTodayToProtect() {
        val state = CalendarStreakHeaderState.from(streak = 3, maxStreak = 5, isTodayActive = false)
        assertEquals(CalendarStreakHeaderState.Protect, state)
    }

    @Test
    fun mapsPositiveStreakAndActiveTodayToCelebrate() {
        val state = CalendarStreakHeaderState.from(streak = 4, maxStreak = 5, isTodayActive = true)
        assertEquals(CalendarStreakHeaderState.Celebrate, state)
    }

    @Test
    fun clampsNegativeStreakAndMaxStreakToZero() {
        val stateStart = CalendarStreakHeaderState.from(streak = -2, maxStreak = -5, isTodayActive = false)
        assertEquals(CalendarStreakHeaderState.Start, stateStart)

        val stateRestart = CalendarStreakHeaderState.from(streak = -1, maxStreak = 3, isTodayActive = false)
        assertEquals(CalendarStreakHeaderState.Restart, stateRestart)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run command:
`./gradlew :feature:calendar:impl:testDebugUnitTest --tests "com.awan.feature.calendar.impl.presentation.CalendarStreakHeaderStateTest"`

Expected result: Compilation failure with un-resolved reference `CalendarStreakHeaderState`.

- [ ] **Step 3: Implement CalendarStreakHeaderState enum and resolver in CalendarContract.kt**

Modify `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarContract.kt`:

```kotlin
package com.awan.feature.calendar.impl.presentation

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class CalendarStreakHeaderState {
    Start,
    Restart,
    Protect,
    Celebrate;

    companion object {
        fun from(streak: Int, maxStreak: Int, isTodayActive: Boolean): CalendarStreakHeaderState {
            val clampedStreak = streak.coerceAtLeast(0)
            val clampedMaxStreak = maxStreak.coerceAtLeast(0)
            return when {
                clampedStreak == 0 && clampedMaxStreak == 0 -> Start
                clampedStreak == 0 -> Restart
                isTodayActive -> Celebrate
                else -> Protect
            }
        }
    }
}
```

- [ ] **Step 4: Run test and compile gates to verify pass**

Run commands:
`./gradlew :feature:calendar:impl:testDebugUnitTest --tests "com.awan.feature.calendar.impl.presentation.CalendarStreakHeaderStateTest"`
`./gradlew :feature:calendar:impl:compileDebugKotlin`

Expected result: `BUILD SUCCESSFUL` and test passes.

- [ ] **Step 5: Review task delivery**

Verify `CalendarStreakHeaderState` enum exists, handles all 4 states, clamps negative inputs, and test passes without side effects.

---

### Task 2: Wire GamificationProgress & Today Activity in CalendarUiState and CalendarViewModel

**Files:**
- Modify: `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarContract.kt`
- Modify: `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarViewModel.kt`
- Create: `feature/calendar/impl/src/test/java/com/awan/feature/calendar/impl/presentation/CalendarViewModelTest.kt`

**Interfaces:**
- Consumes: `CalendarStreakHeaderState` from Task 1, `ObserveGamificationProgressUseCase`, `GetActivityDatesUseCase`
- Produces: `CalendarUiState` with `maxStreak: Int`, `isTodayActive: Boolean`, `streakHeaderState: CalendarStreakHeaderState` fields. Updated `CalendarViewModel` observing `GamificationProgress` and querying today-only activity.

- [ ] **Step 1: Write failing unit test for CalendarViewModel using local fakes**

Create `feature/calendar/impl/src/test/java/com/awan/feature/calendar/impl/presentation/CalendarViewModelTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run command:
`./gradlew :feature:calendar:impl:testDebugUnitTest --tests "com.awan.feature.calendar.impl.presentation.CalendarViewModelTest"`

Expected result: Compilation error because `maxStreak`, `isTodayActive`, and `streakHeaderState` do not exist on `CalendarUiState`.

- [ ] **Step 3: Update CalendarUiState and CalendarViewModel**

Modify `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarContract.kt` to include the fields:

```kotlin
data class CalendarUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val isTodayActive: Boolean = false,
    val streakHeaderState: CalendarStreakHeaderState = CalendarStreakHeaderState.Start,
    val timezone: ZoneId = ZoneId.systemDefault(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val currentYearMonth: YearMonth = YearMonth.now(),
    val streakDates: Set<LocalDate> = emptySet(),
    val upcomingGoals: List<CalendarGoal> = emptyList(),
    val monthDays: List<DayState> = emptyList(),
)
```

Modify `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarViewModel.kt`:

1. Add private `loadTodayActivity(today: LocalDate)` helper method:

```kotlin
    private fun loadTodayActivity(today: LocalDate) {
        _state.update { current ->
            current.copy(
                isTodayActive = false,
                streakHeaderState = CalendarStreakHeaderState.from(
                    streak = current.streak,
                    maxStreak = current.maxStreak,
                    isTodayActive = false,
                ),
            )
        }
        viewModelScope.launch {
            val result = getActivityDatesUseCase(startDate = today, endDate = today)
            if (_state.value.today != today) return@launch
            val active = result is Result.Success && result.data.contains(today)
            _state.update { current ->
                if (current.today != today) current else {
                    current.copy(
                        isTodayActive = active,
                        streakHeaderState = CalendarStreakHeaderState.from(
                            streak = current.streak,
                            maxStreak = current.maxStreak,
                            isTodayActive = active,
                        ),
                    )
                }
            }
        }
    }
```

2. Update `observeGamificationProgressUseCase` collection block in `init`:

```kotlin
        viewModelScope.launch {
            observeGamificationProgressUseCase().collect { progress ->
                val updatedStreak = progress.streak.coerceAtLeast(0)
                val updatedMaxStreak = progress.maxStreak.coerceAtLeast(0)
                _state.update { current ->
                    current.copy(
                        streak = updatedStreak,
                        maxStreak = updatedMaxStreak,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.from(
                            streak = updatedStreak,
                            maxStreak = updatedMaxStreak,
                            isTodayActive = false,
                        ),
                    )
                }
                loadTodayActivity(_state.value.today)
            }
        }
```

3. Update `render(snapshot: CalendarSnapshot)`: Note `CalendarUser` has no `maxStreak`. Retain `current.maxStreak`. If `today` changes (`today != current.today`), invoke `loadTodayActivity(today)`:

```kotlin
    private fun render(snapshot: CalendarSnapshot) {
        val zone = CalendarDateMapper.parseZoneIdOrDefault(snapshot.user.timezone)
        val today = LocalDate.now(zone)
        val current = _state.value
        val dayChanged = today != current.today

        val selected = if (current.selectedDate == current.today) today else current.selectedDate
        val month = if (current.currentYearMonth == YearMonth.from(current.today)) YearMonth.from(today) else current.currentYearMonth
        val goals = CalendarDateMapper.filterAndSortUpcomingGoals(snapshot.goals, today)
        val streakCount = snapshot.user.streak.coerceAtLeast(0)

        val streakDates = current.streakDates.ifEmpty {
            CalendarDateMapper.calculateStreakDates(streakCount, today)
        }

        val preservedMaxStreak = current.maxStreak
        val isTodayActive = if (dayChanged) false else current.isTodayActive
        val headerState = CalendarStreakHeaderState.from(
            streak = streakCount,
            maxStreak = preservedMaxStreak,
            isTodayActive = isTodayActive,
        )

        _state.value = CalendarUiState(
            isLoading = false,
            errorMessage = null,
            streak = streakCount,
            maxStreak = preservedMaxStreak,
            isTodayActive = isTodayActive,
            streakHeaderState = headerState,
            timezone = zone,
            today = today,
            selectedDate = selected,
            currentYearMonth = month,
            streakDates = streakDates,
            upcomingGoals = goals,
            monthDays = CalendarDateMapper.buildMonthDays(month, today, selected, streakDates, goals.map { it.targetDate }.toSet()),
        )

        if (dayChanged) {
            loadTodayActivity(today)
        }
    }
```

4. Ensure `loadActivityDates(month)` updates grid `streakDates` and `monthDays` only, preserving `isTodayActive`, `maxStreak`, and `streakHeaderState`.

- [ ] **Step 4: Run unit tests and compile gate to verify pass**

Run commands:
`./gradlew :feature:calendar:impl:testDebugUnitTest`
`./gradlew :feature:calendar:impl:compileDebugKotlin`

Expected result: `BUILD SUCCESSFUL`, all unit tests pass.

- [ ] **Step 5: Review task delivery**

Verify `CalendarUiState` exposes `maxStreak`, `isTodayActive`, and `streakHeaderState`; today-only query runs on gamification progress emissions via `loadTodayActivity`; errors fallback safely to `isTodayActive = false`; grid paging leaves header state intact; `CalendarSnapshot.user` has no `maxStreak` references.

---

### Task 3: Implement Four-State UI Header in CalendarScreen, Add String Resources, Delete Duplicate Screen, Update Compose Tests

**Files:**
- Modify: `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarScreen.kt`
- Modify: `feature/calendar/impl/src/main/res/values/strings.xml`
- Modify: `feature/calendar/impl/src/main/res/values-ar/strings.xml`
- Modify: `feature/calendar/impl/src/androidTest/java/com/awan/feature/calendar/impl/presentation/CalendarScreenTest.kt`
- Delete: `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/ui/CalendarScreen.kt` (in Green step)

**Interfaces:**
- Consumes: `CalendarUiState.streakHeaderState`, `CalendarUiState.streak`, localized string resources
- Produces: Four-state `StreakHeaderCard` using `AwanCard` replacing `StreakSummaryCard` in `CalendarScreen.kt` with test tags `streak_summary_card`, `streak_header_<state>`, `streak_header_<state>_icon`. Deleted unreferenced `ui/CalendarScreen.kt`.

- [ ] **Step 1: Write failing Compose tests in CalendarScreenTest.kt**

Modify `feature/calendar/impl/src/androidTest/java/com/awan/feature/calendar/impl/presentation/CalendarScreenTest.kt` to add separate test cases for all four variants while preserving the existing day-selection test:

```kotlin
package com.awan.feature.calendar.impl.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.designsystem.AwanTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarTitleMatchesHomeGreetingScaleAndStreakHeaderRendersStart() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme(dark = true) {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 0,
                        maxStreak = 0,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Start,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = CalendarDateMapper.buildMonthDays(
                            YearMonth.from(today),
                            today,
                            today,
                            emptySet(),
                            emptySet(),
                        ),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        val title = composeRule.onNodeWithTag("calendar_title")
        title.assertIsDisplayed()
        val titleBounds = title.getUnclippedBoundsInRoot()
        assertTrue(titleBounds.bottom - titleBounds.top <= 32.dp)
        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_start").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_start_icon").assertIsDisplayed()
        composeRule.onNodeWithText("Start your streak").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersRestartStateForEndedStreak() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 0,
                        maxStreak = 5,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Restart,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_restart").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_restart_icon").assertIsDisplayed()
        composeRule.onNodeWithText("Ready to start again?").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersProtectStateForActiveStreakNotCompletedToday() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 5,
                        maxStreak = 10,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Protect,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_protect").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_protect_icon").assertIsDisplayed()
        composeRule.onNodeWithText("5-day streak at risk").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersCelebrateStateForActiveStreakCompletedToday() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 5,
                        maxStreak = 10,
                        isTodayActive = true,
                        streakHeaderState = CalendarStreakHeaderState.Celebrate,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_celebrate").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_celebrate_icon").assertIsDisplayed()
        composeRule.onNodeWithText("5-day streak! You're on fire").assertIsDisplayed()
    }

    @Test
    fun selectingADayDispatchesItsDate() {
        val today = LocalDate.of(2026, 8, 1)
        val actions = mutableListOf<CalendarAction>()
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = CalendarDateMapper.buildMonthDays(
                            YearMonth.from(today),
                            today,
                            today,
                            emptySet(),
                            emptySet(),
                        ),
                    ),
                    onAction = actions::add,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").performClick()

        assertTrue(actions == listOf(CalendarAction.SelectDate(today)))
    }
}
```

- [ ] **Step 2: Run Android Test compile gate to verify failure (Red step)**

Run command:
`./gradlew :feature:calendar:impl:compileDebugAndroidTestKotlin`

Expected result: Compilation failure because string resources and UI implementation are not yet updated.

- [ ] **Step 3: Add English and Arabic string resources, implement StreakHeaderCard with AwanCard, and delete duplicate screen (Green step)**

Add to `feature/calendar/impl/src/main/res/values/strings.xml`:

```xml
    <string name="calendar_streak_start_title">Start your streak</string>
    <string name="calendar_streak_start_subtitle">Complete a task today to light your fire.</string>
    <string name="calendar_streak_restart_title">Ready to start again?</string>
    <string name="calendar_streak_restart_subtitle">Your last streak ended. Complete a task today to begin a new one.</string>
    <string name="calendar_streak_protect_title">%1$d-day streak at risk</string>
    <string name="calendar_streak_protect_subtitle">Complete a task today to keep your fire burning.</string>
    <string name="calendar_streak_celebrate_title">%1$d-day streak! You\'re on fire</string>
    <string name="calendar_streak_celebrate_subtitle">You completed today\'s task. Keep it going tomorrow.</string>
```

Add to `feature/calendar/impl/src/main/res/values-ar/strings.xml`:

```xml
    <string name="calendar_streak_start_title">&#x0627;&#x0628;&#x062F;&#x0623; &#x0633;&#x0644;&#x0633;&#x0644;&#x062A;&#x0643;</string>
    <string name="calendar_streak_start_subtitle">&#x0623;&#x0643;&#x0645;&#x0644; &#x0645;&#x0647;&#x0645;&#x0629; &#x0627;&#x0644;&#x064A;&#x0648;&#x0645; &#x0644;&#x0625;&#x0634;&#x0639;&#x0627;&#x0644; &#x062D;&#x0645;&#x0627;&#x0633;&#x0643;.</string>
    <string name="calendar_streak_restart_title">&#x062C;&#x0627;&#x0647;&#x0632; &#x0644;&#x0644;&#x0628;&#x062F;&#x0623; &#x0645;&#x0646; &#x062C;&#x062F;&#x064A;&#x062F;&#x061F;</string>
    <string name="calendar_streak_restart_subtitle">&#x0627;&#x0646;&#x062A;&#x0647;&#x062A; &#x0633;&#x0644;&#x0633;&#x0644;&#x062A;&#x0643; &#x0627;&#x0644;&#x0623;&#x062E;&#x064A;&#x0631;&#x0629;. &#x0623;&#x0643;&#x0645;&#x0644; &#x0645;&#x0647;&#x0645;&#x0629; &#x0627;&#x0644;&#x064A;&#x0648;&#x0645; &#x0644;&#x0628;&#x062F;&#x0623; &#x0633;&#x0644;&#x0633;&#x0644;&#x0629; &#x062C;&#x062F;&#x064A;&#x062F;&#x0629;.</string>
    <string name="calendar_streak_protect_title">&#x0633;&#x0644;&#x0633;&#x0644;&#x0629; %1$d &#x064A;&#x0648;&#x0645; &#x0641;&#x064A; &#x062E;&#x0637;&#x0631;</string>
    <string name="calendar_streak_protect_subtitle">&#x0623;&#x0643;&#x0645;&#x0644; &#x0645;&#x0647;&#x0645;&#x0629; &#x0627;&#x0644;&#x064A;&#x0648;&#x0645; &#x0644;&#x0644;&#x062D;&#x0641;&#x0627;&#x0638; &#x0639;&#x0644;&#x0649; &#x062D;&#x0645;&#x0627;&#x0633;&#x0643;.</string>
    <string name="calendar_streak_celebrate_title">&#x0633;&#x0644;&#x0633;&#x0644;&#x0629; %1$d &#x064A;&#x0648;&#x0645;! &#x0623;&#x0646;&#x062A; &#x0645;&#x062A;&#x0623;&#x0644;&#x0642;</string>
    <string name="calendar_streak_celebrate_subtitle">&#x0623;&#x0643;&#x0645;&#x0644;&#x062A; &#x0645;&#x0647;&#x0645;&#x0629; &#x0627;&#x0644;&#x064A;&#x0648;&#x0645;. &#x0627;&#x0633;&#x062A;&#x0645;&#x0631; &#x063A;&#x062F;&#x0627;&#x064B;.</string>
```

In `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarScreen.kt`:

Replace call to `StreakSummaryCard(streak = state.streak)` with `StreakHeaderCard(state = state.streakHeaderState, streak = state.streak)`.

Implement `StreakHeaderCard` using `AwanCard`:

```kotlin
private data class CalendarStreakHeaderVisual(
    val faceColor: Color,
    val rimColor: Color,
    val circleColor: Color,
    val titleText: String,
    val subtitleText: String,
)

@Composable
private fun StreakHeaderCard(
    state: CalendarStreakHeaderState,
    streak: Int,
) {
    val colors = AwanTheme.colors
    val variantTag = "streak_header_${state.name.lowercase()}"
    val visual = when (state) {
        CalendarStreakHeaderState.Start -> CalendarStreakHeaderVisual(
            faceColor = colors.sky.copy(alpha = 0.12f),
            rimColor = colors.sky.copy(alpha = 0.35f),
            circleColor = colors.sky.copy(alpha = 0.20f),
            titleText = stringResource(R.string.calendar_streak_start_title),
            subtitleText = stringResource(R.string.calendar_streak_start_subtitle),
        )
        CalendarStreakHeaderState.Restart -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneCoral.copy(alpha = 0.12f),
            rimColor = colors.zoneCoral.copy(alpha = 0.35f),
            circleColor = colors.zoneCoral.copy(alpha = 0.20f),
            titleText = stringResource(R.string.calendar_streak_restart_title),
            subtitleText = stringResource(R.string.calendar_streak_restart_subtitle),
        )
        CalendarStreakHeaderState.Protect -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneTangerine.copy(alpha = 0.14f),
            rimColor = colors.zoneTangerine.copy(alpha = 0.45f),
            circleColor = colors.zoneTangerine.copy(alpha = 0.25f),
            titleText = stringResource(R.string.calendar_streak_protect_title, streak),
            subtitleText = stringResource(R.string.calendar_streak_protect_subtitle),
        )
        CalendarStreakHeaderState.Celebrate -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneSun.copy(alpha = 0.16f),
            rimColor = colors.zoneSun.copy(alpha = 0.50f),
            circleColor = colors.zoneCoral.copy(alpha = 0.30f),
            titleText = stringResource(R.string.calendar_streak_celebrate_title, streak),
            subtitleText = stringResource(R.string.calendar_streak_celebrate_subtitle),
        )
    }

    AwanCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_summary_card")
            .testTag(variantTag),
        background = visual.faceColor,
        customRimColor = visual.rimColor,
        contentPadding = PaddingValues(AwanTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(visual.circleColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(visual.circleColor.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        CalendarStreakHeaderState.Start -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.sky,
                            modifier = Modifier.testTag("streak_header_start_icon"),
                        )
                        CalendarStreakHeaderState.Restart -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.zoneCoral,
                            modifier = Modifier.testTag("streak_header_restart_icon"),
                        )
                        CalendarStreakHeaderState.Protect -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.zoneTangerine,
                            modifier = Modifier.testTag("streak_header_protect_icon"),
                        )
                        CalendarStreakHeaderState.Celebrate -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.zoneSun,
                            modifier = Modifier.testTag("streak_header_celebrate_icon"),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(AwanTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = visual.titleText,
                    style = AwanTheme.styles.titleText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AwanText(
                    text = visual.subtitleText,
                    style = AwanTheme.styles.captionText,
                )
            }
        }
    }
}
```

Delete unreferenced duplicate screen file:
Delete `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/ui/CalendarScreen.kt`.

- [ ] **Step 4: Run compilation, lint, and test gates**

Run commands:
`./gradlew :feature:calendar:impl:compileDebugKotlin`
`./gradlew :feature:calendar:impl:compileDebugAndroidTestKotlin`
`./gradlew :feature:calendar:impl:lintDebug`
`./gradlew :feature:calendar:impl:connectedDebugAndroidTest` (if device/emulator is connected)

Expected result: `BUILD SUCCESSFUL` across Kotlin compilation, androidTest compilation, and lint.

- [ ] **Step 5: Review task delivery and final state**

Verify:
1. `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/ui/CalendarScreen.kt` is deleted.
2. `CalendarScreen.kt` uses `AwanCard` and renders state-reactive header with exact copy and test tags (`streak_summary_card`, `streak_header_<state>`, `streak_header_<state>_icon`).
3. English and Arabic string resources exist for all 4 states using ASCII XML character references for Arabic.
4. Color tokens use `zoneSun`, `zoneCoral`, `zoneTangerine`, and `sky`.
5. Visual mapping uses private `CalendarStreakHeaderVisual` data class instead of generic tuple or `AwanSurface`.
6. No production code changes outside `:feature:calendar:impl`.

## Implementation notes (what actually differed)

- Product scope stayed as approved: server-backed four-state Calendar header (Start, Restart, Protect, Celebrate); insights/reports and animation assets/dependencies remain deferred.
- Existing GamificationProgress plus a today-only GetActivityDatesUseCase query drive the state; current CalendarSnapshot preserves maxStreak because CalendarUser has none.
- The static header became AwanCard state variants with explicit fallback icon seams for future per-state animations; existing unreferenced impl/ui/CalendarScreen.kt was deleted.
- Verification passed: :feature:calendar:impl:testDebugUnitTest, :feature:calendar:impl:compileDebugKotlin, :feature:calendar:impl:compileDebugAndroidTestKotlin, and :feature:calendar:impl:lintDebug.
- Connected execution could not run tests: Android blocked test APK installation with INSTALL_FAILED_USER_RESTRICTED: Install canceled by user (0 tests ran); this is an environment/device permission restriction, not a code failure.
- App assemble is still not a valid gate in this worktree because app/google-services.json is absent.
