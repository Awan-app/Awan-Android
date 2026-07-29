package com.awan.feature.calendar.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.calendar.CalendarRepository
import com.awan.feature.calendar.impl.R
import com.awan.feature.calendar.impl.domain.CalendarDateMapper
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
) : ViewModel() {
    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()
    private val events = Channel<CalendarEvent>(Channel.BUFFERED)
    val event = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeCalendar().collect { snapshot -> snapshot?.let(::render) }
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
            current.copy(
                selectedDate = date,
                monthDays = CalendarDateMapper.buildMonthDays(
                    yearMonth = current.currentYearMonth,
                    today = current.today,
                    selectedDate = date,
                    streakDates = current.streakDates,
                    goalDates = current.upcomingGoals.map { it.targetDate }.toSet(),
                ),
            )
        }
        viewModelScope.launch { events.send(CalendarEvent.DateSelected(date)) }
    }

    private fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        val result = try {
            repository.refresh()
        } catch (e: Exception) {
            Result.Error(com.awan.app.core.common.error.AppError.Unknown(e))
        }
        _state.update { current ->
            if (result is Result.Error) {
                current.copy(isLoading = false, errorMessage = R.string.calendar_refresh_error)
            } else {
                current.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    private fun render(snapshot: com.awan.app.core.data.calendar.CalendarSnapshot) {
        val zone = CalendarDateMapper.parseZoneIdOrDefault(snapshot.user.timezone)
        val today = LocalDate.now(zone)
        val current = _state.value
        val selected = if (current.selectedDate == current.today) today else current.selectedDate
        val month = if (current.currentYearMonth == YearMonth.from(current.today)) YearMonth.from(today) else current.currentYearMonth
        val goals = CalendarDateMapper.filterAndSortUpcomingGoals(snapshot.goals, today)
        val streakCount = snapshot.user.streak.coerceAtLeast(0)
        val streakDates = CalendarDateMapper.calculateStreakDates(streakCount, today)

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
            monthDays = CalendarDateMapper.buildMonthDays(month, today, selected, streakDates, goals.map { it.targetDate }.toSet()),
        )
    }

    private fun changeMonth(delta: Long) = _state.update { state ->
        val month = state.currentYearMonth.plusMonths(delta)
        state.copy(currentYearMonth = month, monthDays = CalendarDateMapper.buildMonthDays(month, state.today, state.selectedDate, state.streakDates, state.upcomingGoals.map { it.targetDate }.toSet()))
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
                monthDays = CalendarDateMapper.buildMonthDays(month, today, today, emptySet(), emptySet()),
            )
        }
    }
}

sealed interface CalendarEvent { data class DateSelected(val date: LocalDate) : CalendarEvent }

