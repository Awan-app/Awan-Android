package com.awan.feature.calendar.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.calendar.CalendarRepository
import com.awan.feature.calendar.impl.domain.CalendarDateMapper
import com.awan.feature.calendar.impl.model.CalendarGoal
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
    private val _state = MutableStateFlow(createInitialDummyState())
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
        is CalendarAction.SelectDate -> viewModelScope.launch { events.send(CalendarEvent.DateSelected(action.date)) }
        CalendarAction.PreviousMonth -> changeMonth(-1)
        CalendarAction.NextMonth -> changeMonth(1)
        CalendarAction.Refresh -> refresh()
    }

    private fun refresh() = viewModelScope.launch {
        if (repository.refresh() is Result.Error && _state.value.upcomingGoals.isEmpty()) {
            _state.update { it.copy(isLoading = false, errorMessage = com.awan.feature.calendar.impl.R.string.calendar_refresh_error) }
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun render(snapshot: com.awan.app.core.data.calendar.CalendarSnapshot) {
        val zone = CalendarDateMapper.parseZoneIdOrDefault(snapshot.user.timezone)
        val today = LocalDate.now(zone)
        val current = _state.value
        val selected = current.selectedDate.takeIf { current.monthDays.isNotEmpty() } ?: today
        val month = current.currentYearMonth.takeIf { current.monthDays.isNotEmpty() } ?: YearMonth.from(selected)
        val repoGoals = CalendarDateMapper.filterAndSortUpcomingGoals(snapshot.goals, today)
        val repoStreaks = CalendarDateMapper.calculateStreakDates(snapshot.user.streak, today)

        val goals = repoGoals.ifEmpty { createDummyGoals(today) }
        val streakCount = if (snapshot.user.streak > 0) snapshot.user.streak else 7
        val streakDates = if (repoStreaks.isNotEmpty()) repoStreaks else createDummyStreakDates(today, streakCount)

        _state.value = CalendarUiState(
            isLoading = false,
            errorMessage = current.errorMessage,
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
        private fun createDummyGoals(today: LocalDate): List<CalendarGoal> = listOf(
            CalendarGoal(id = "dummy-1", title = "Complete System Architecture Spec", targetDate = today),
            CalendarGoal(id = "dummy-2", title = "Submit App Store Artifacts", targetDate = today.plusDays(2)),
            CalendarGoal(id = "dummy-3", title = "Sprint Review & Demo", targetDate = today.plusDays(5)),
            CalendarGoal(id = "dummy-4", title = "Release V1.0 Candidate", targetDate = today.plusDays(10)),
        )

        private fun createDummyStreakDates(today: LocalDate, count: Int): Set<LocalDate> =
            (0 until count).map { today.minusDays(it.toLong()) }.toSet()

        private fun createInitialDummyState(): CalendarUiState {
            val today = LocalDate.now()
            val month = YearMonth.from(today)
            val dummyStreak = 7
            val dummyStreakDates = createDummyStreakDates(today, dummyStreak)
            val dummyGoals = createDummyGoals(today)
            return CalendarUiState(
                isLoading = false,
                streak = dummyStreak,
                today = today,
                selectedDate = today,
                currentYearMonth = month,
                streakDates = dummyStreakDates,
                upcomingGoals = dummyGoals,
                monthDays = CalendarDateMapper.buildMonthDays(month, today, today, dummyStreakDates, dummyGoals.map { it.targetDate }.toSet()),
            )
        }
    }
}

sealed interface CalendarEvent { data class DateSelected(val date: LocalDate) : CalendarEvent }
