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

    init {
        viewModelScope.launch {
            repository.observeCalendar().collect { snapshot ->
                snapshot?.let { render(it) }
            }
        }
        viewModelScope.launch {
            // Same source as the home header, so the two can never disagree.
            observeGamificationProgressUseCase().collect { progress ->
                _state.update { it.copy(streak = progress.streak) }
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
                    goalDates = current.upcomingGoals.map { it.targetDate }.toSet(),
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
        val current = _state.value
        val selected = if (current.selectedDate == current.today) today else current.selectedDate
        val month = if (current.currentYearMonth == YearMonth.from(current.today)) YearMonth.from(today) else current.currentYearMonth
        val goals = CalendarDateMapper.filterAndSortUpcomingGoals(snapshot.goals, today)
        val streakCount = snapshot.user.streak.coerceAtLeast(0)
        // Real activity dates win once they land; the back-counted estimate is only a stand-in
        // until then, and must not clobber them when the snapshot re-emits.
        val streakDates = current.streakDates.ifEmpty {
            CalendarDateMapper.calculateStreakDates(streakCount, today)
        }

        val routineDates = CalendarDateMapper.calculateRoutineDates(month, snapshot.templates, snapshot.overrides)

        _state.value = CalendarUiState(
            isLoading = false,
            errorMessage = null,
            streak = streakCount,
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
                goalDates = goals.map { it.targetDate }.toSet(),
                routineDates = routineDates,
            ),
        )
        
        loadActivityDates(month)
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
                    goalDates = state.upcomingGoals.map { it.targetDate }.toSet(),
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
                    goalDates = state.upcomingGoals.map { it.targetDate }.toSet(),
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
                timezone = zone,
                today = today,
                selectedDate = today,
                currentYearMonth = month,
                streakDates = emptySet(),
                upcomingGoals = emptyList(),
                monthDays = CalendarDateMapper.buildMonthDays(month, today, today, emptySet(), emptySet(), emptySet()),
            )
        }
    }
}

sealed interface CalendarEvent { data class DateSelected(val date: LocalDate) : CalendarEvent }

