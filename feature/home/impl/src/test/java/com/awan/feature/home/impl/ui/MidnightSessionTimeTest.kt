package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.formatMinutesToRange
import com.awan.feature.home.impl.ui.components.calculateDurationMinutes
import java.time.LocalDateTime
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MidnightSessionTimeTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `calculateDurationMinutes handles normal same day session`() {
        val start = LocalDateTime.of(2026, 8, 13, 10, 0)
        val end = LocalDateTime.of(2026, 8, 13, 11, 0)
        val duration = calculateDurationMinutes(start, end)
        assertEquals(60, duration)
    }

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
    fun `calculateDurationMinutes handles equal start and end as zero duration`() {
        val start = LocalDateTime.of(2026, 8, 13, 10, 0)
        val end = LocalDateTime.of(2026, 8, 13, 10, 0)
        val duration = calculateDurationMinutes(start, end)
        assertEquals(0, duration)
    }

    @Test
    fun `calculateDurationMinutes returns zero for invalid inverted same day session`() {
        val start = LocalDateTime.of(2026, 8, 13, 10, 0)
        val end = LocalDateTime.of(2026, 8, 13, 9, 0) // 10 AM to 9 AM on same date
        val duration = calculateDurationMinutes(start, end)
        assertEquals(0, duration)
    }

    @Test
    fun `calculateDurationMinutes handles 11 59 PM to 12 00 AM boundary case`() {
        val start = LocalDateTime.of(2026, 8, 13, 23, 59)
        val end = LocalDateTime.of(2026, 8, 13, 0, 0)
        val duration = calculateDurationMinutes(start, end)
        assertEquals(1, duration)
    }

    @Test
    fun `formatMinutesToRange formats 11 PM to 12 AM deterministically in US locale`() {
        val startMinutes = 23 * 60 // 1380 mins (11:00 PM)
        val durationMinutes = 60
        val range = formatMinutesToRange(startMinutes, durationMinutes).replace('\u202f', ' ')
        assertEquals("11:00 PM - 12:00 AM", range)
    }
}
