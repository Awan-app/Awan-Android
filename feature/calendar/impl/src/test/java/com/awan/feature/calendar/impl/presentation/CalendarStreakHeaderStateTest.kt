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
