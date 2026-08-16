package com.awan.feature.calendar.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.calendar.repository.CalendarRepository
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.domain.gamification.usecase.GetActivityDatesUseCase
import com.awan.app.core.domain.gamification.usecase.ObserveGamificationProgressUseCase
import com.awan.feature.calendar.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val getActivityDatesUseCase: GetActivityDatesUseCase,
    private val observeGamificationProgressUseCase: ObserveGamificationProgressUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()
    private val events = Channel<CalendarEvent>(Channel.BUFFERED)
    val event = events.receiveAsFlow()
    private var todayActivityJob: Job? = null
    private var todayActivityGeneration: Long = 0L

    init {
        viewModelScope.launch {
            repository.observeCalendar().collect { snapshot ->
                snapshot?.let { render(it) }
            }
        }
        viewModelScope.launch {
            // Same source as the home header, so the two can never disagree.
            observeGamificationProgressUseCase().collect { progress ->
                val updatedStreak = progress.streak.coerceAtLeast(0)
                val updatedMaxStreak = progress.maxStreak.coerceAtLeast(0)
                _state.update { current ->
                    // Build an optimistic estimate from the streak count. Today is only included
                    // if we already know it's active; otherwise loadTodayActivity will confirm it.
                    val estimatedDates = CalendarDateMapper.calculateStreakDates(updatedStreak, current.today)
                    val streakDates = if (current.isTodayActive) estimatedDates else estimatedDates - current.today
                    val routineDates = CalendarDateMapper.calculateRoutineDates(
                        current.currentYearMonth, current.templates, current.overrides,
                    )
                    val monthDays = CalendarDateMapper.buildMonthDays(
                        yearMonth = current.currentYearMonth,
                        today = current.today,
                        selectedDate = current.selectedDate,
                        streakDates = streakDates,
                        goals = current.upcomingGoals,
                        routineDates = routineDates,
                    )
                    current.copy(
                        streak = updatedStreak,
                        maxStreak = updatedMaxStreak,
                        isTodayActive = current.isTodayActive,
                        streakHeaderState = CalendarStreakHeaderState.from(
                            streak = updatedStreak,
                            maxStreak = updatedMaxStreak,
                            isTodayActive = current.isTodayActive,
                        ),
                        streakDates = streakDates,
                        monthDays = monthDays,
                    )
                }
                loadTodayActivity(_state.value.today)
            }
        }
        refresh()
    }

    fun onAction(action: CalendarAction) = when (action) {
        is CalendarAction.SelectDate -> selectDate(action.date)
        CalendarAction.PreviousMonth -> changeMonth(-1)
        CalendarAction.NextMonth -> changeMonth(1)
        CalendarAction.Refresh -> refresh()
    }

    private fun loadTodayActivity(today: LocalDate) {
        val generation = ++todayActivityGeneration
        _state.update { current ->
            val active = current.isTodayActive
            current.copy(
                isTodayActive = active,
                streakHeaderState = CalendarStreakHeaderState.from(
                    streak = current.streak,
                    maxStreak = current.maxStreak,
                    isTodayActive = active,
                ),
            )
        }
        todayActivityJob?.cancel()
        todayActivityJob = viewModelScope.launch {
            val result = getActivityDatesUseCase(startDate = today, endDate = today)
            if (_state.value.today != today || todayActivityGeneration != generation) return@launch
            val active = result is Result.Success && result.data.contains(today)
            _state.update { current ->
                if (current.today != today || todayActivityGeneration != generation) current else {
                    val updatedStreakDates = if (active) current.streakDates + today else current.streakDates - today
                    val routineDates = CalendarDateMapper.calculateRoutineDates(
                        current.currentYearMonth, current.templates, current.overrides,
                    )
                    val updatedMonthDays = CalendarDateMapper.buildMonthDays(
                        yearMonth = current.currentYearMonth,
                        today = current.today,
                        selectedDate = current.selectedDate,
                        streakDates = updatedStreakDates,
                        goals = current.upcomingGoals,
                        routineDates = routineDates,
                    )
                    current.copy(
                        isTodayActive = active,
                        streakHeaderState = CalendarStreakHeaderState.from(
                            streak = current.streak,
                            maxStreak = current.maxStreak,
                            isTodayActive = active,
                        ),
                        streakDates = updatedStreakDates,
                        monthDays = updatedMonthDays,
                    )
                }
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        _state.update { current ->
            val routineDates = CalendarDateMapper.calculateRoutineDates(
                current.currentYearMonth, current.templates, current.overrides,
            )

            current.copy(
                selectedDate = date,
                monthDays = CalendarDateMapper.buildMonthDays(
                    yearMonth = current.currentYearMonth,
                    today = current.today,
                    selectedDate = date,
                    streakDates = current.streakDates,
                    goals = current.upcomingGoals,
                    routineDates = routineDates,
                ),
            )
        }
        viewModelScope.launch { events.send(CalendarEvent.DateSelected(date)) }
    }

    private fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        val result = repository.refresh()
        _state.update { current ->
            if (result is Result.Error) {
                current.copy(isLoading = false, errorMessage = R.string.calendar_refresh_error)
            } else {
                current.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    private fun render(snapshot: CalendarSnapshot) {
        val zone = CalendarDateMapper.parseZoneIdOrDefault(snapshot.user.timezone)
        val today = LocalDate.now(zone)
        var shouldLoadTodayActivity = false

        _state.update { current ->
            val dayChanged = today != current.today
            if (dayChanged) {
                shouldLoadTodayActivity = true
            }

            val selected = if (current.selectedDate == current.today) today else current.selectedDate
            val month = if (current.currentYearMonth == YearMonth.from(current.today)) YearMonth.from(today) else current.currentYearMonth
            val goals = CalendarDateMapper.filterAndSortUpcomingGoals(snapshot.goals, today)

            val initialStreak = if (current.streak > 0) current.streak else snapshot.user.streak.coerceAtLeast(0)
            val isTodayActive = if (dayChanged) false else current.isTodayActive
            val estimatedDates = if (dayChanged) {
                CalendarDateMapper.calculateStreakDates(initialStreak, today)
            } else {
                current.streakDates.ifEmpty {
                    CalendarDateMapper.calculateStreakDates(initialStreak, today)
                }
            }
            // Never mark today as a streak day from the estimate alone — only loadTodayActivity
            // confirms today's activity. Strip today if we don't yet know it's active.
            val streakDates = if (isTodayActive) estimatedDates else estimatedDates - today
            val preservedStreak = initialStreak
            val preservedMaxStreak = current.maxStreak
            val headerState = CalendarStreakHeaderState.from(
                streak = preservedStreak,
                maxStreak = preservedMaxStreak,
                isTodayActive = isTodayActive,
            )

            val routineDates = CalendarDateMapper.calculateRoutineDates(month, snapshot.templates, snapshot.overrides)

            current.copy(
                isLoading = false,
                errorMessage = null,
                streak = preservedStreak,
                maxStreak = preservedMaxStreak,
                isTodayActive = isTodayActive,
                streakHeaderState = headerState,
                timezone = zone,
                today = today,
                selectedDate = selected,
                currentYearMonth = month,
                streakDates = streakDates,
                upcomingGoals = goals,
                templates = snapshot.templates,
                overrides = snapshot.overrides,
                monthDays = CalendarDateMapper.buildMonthDays(
                    yearMonth = month, 
                    today = today, 
                    selectedDate = selected, 
                    streakDates = streakDates, 
                    goals = goals,
                    routineDates = routineDates
                ),
            )
        }

        if (shouldLoadTodayActivity) {
            loadTodayActivity(today)
        }
    }

    private fun changeMonth(delta: Long) {
        _state.update { state ->
            val month = state.currentYearMonth.plusMonths(delta)
            val routineDates = CalendarDateMapper.calculateRoutineDates(
                month, state.templates, state.overrides,
            )

            state.copy(
                currentYearMonth = month, 
                monthDays = CalendarDateMapper.buildMonthDays(
                    yearMonth = month, 
                    today = state.today, 
                    selectedDate = state.selectedDate, 
                    streakDates = state.streakDates, 
                    goals = state.upcomingGoals,
                    routineDates = routineDates,
                )
            )
        }
        loadActivityDates(_state.value.currentYearMonth)
    }

    /**
     * Marks the days the user was actually active, rather than assuming the streak ran unbroken
     * back from today — which is only ever right for the current month, and only until the user
     * scrolls back a page.
     *
     * The grid spills into the neighbouring months, so the query covers the whole visible range.
     * On failure the back-counted estimate stands in, so the streak line survives being offline.
     */
    private fun loadActivityDates(month: YearMonth) = viewModelScope.launch {
        val days = _state.value.monthDays
        val start = days.firstOrNull()?.date ?: month.atDay(1)
        val end = days.lastOrNull()?.date ?: month.atEndOfMonth()

        val result = getActivityDatesUseCase(startDate = start, endDate = end)
        if (result !is Result.Success) return@launch

        _state.update { state ->
            val routineDates = CalendarDateMapper.calculateRoutineDates(
                state.currentYearMonth, state.templates, state.overrides,
            )

            state.copy(
                streakDates = result.data,
                monthDays = CalendarDateMapper.buildMonthDays(
                    yearMonth = state.currentYearMonth,
                    today = state.today,
                    selectedDate = state.selectedDate,
                    streakDates = result.data,
                    goals = state.upcomingGoals,
                    routineDates = routineDates,
                ),
            )
        }
    }

    companion object {
        private fun createInitialState(): CalendarUiState {
            val zone = java.time.ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val month = YearMonth.from(today)
            return CalendarUiState(
                isLoading = true,
                errorMessage = null,
                streak = 0,
                maxStreak = 0,
                isTodayActive = false,
                streakHeaderState = CalendarStreakHeaderState.Start,
                timezone = zone,
                today = today,
                selectedDate = today,
                currentYearMonth = month,
                streakDates = emptySet(),
                upcomingGoals = emptyList(),
                monthDays = CalendarDateMapper.buildMonthDays(month, today, today, emptySet(), emptyList()),
            )
        }
    }
}

sealed interface CalendarEvent { data class DateSelected(val date: LocalDate) : CalendarEvent }
