package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.formatMinutesToRange
import com.awan.feature.home.impl.ui.components.calculateDurationMinutes
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MidnightSessionTimeTest {

    @Test
    fun `calculateDurationMinutes handles 11 PM to 12 AM midnight rollover`() {
        val start = LocalDateTime.of(2026, 8, 13, 23, 0)
        val end = LocalDateTime.of(2026, 8, 13, 0, 0) // Same date 00:00 midnight end
        val duration = calculateDurationMinutes(start, end)
        assertEquals(60, duration)
    }

    @Test
    fun `calculateDurationMinutes handles 11 PM to 12 AM next day midnight end`() {
        val start = LocalDateTime.of(2026, 8, 13, 23, 0)
        val end = LocalDateTime.of(2026, 8, 14, 0, 0) // Next date 00:00 midnight end
        val duration = calculateDurationMinutes(start, end)
        assertEquals(60, duration)
    }

    @Test
    fun `calculateDurationMinutes handles 10 59 PM to 11 59 PM`() {
        val start = LocalDateTime.of(2026, 8, 13, 22, 59)
        val end = LocalDateTime.of(2026, 8, 13, 23, 59)
        val duration = calculateDurationMinutes(start, end)
        assertEquals(60, duration)
    }

    @Test
    fun `formatMinutesToRange formats 11 PM to 12 AM correctly`() {
        val startMinutes = 23 * 60 // 1380 mins (11:00 PM)
        val durationMinutes = 60
        val range = formatMinutesToRange(startMinutes, durationMinutes)
        assertTrue(range.contains("11:00") && range.contains("12:00"))
    }
}
