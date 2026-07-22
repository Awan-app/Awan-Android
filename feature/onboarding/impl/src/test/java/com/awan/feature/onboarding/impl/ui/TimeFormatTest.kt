package com.awan.feature.onboarding.impl.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `midnight and noon are 12, not 0`() {
        assertEquals(ClockParts(12, 0, isAm = true), clockParts(0))
        assertEquals(ClockParts(12, 0, isAm = false), clockParts(12 * 60))
    }

    @Test
    fun `hours either side of the meridiem flip`() {
        assertEquals(ClockParts(11, 59, isAm = true), clockParts(11 * 60 + 59))
        assertEquals(ClockParts(1, 0, isAm = false), clockParts(13 * 60))
        assertEquals(ClockParts(11, 59, isAm = false), clockParts(23 * 60 + 59))
    }

    @Test
    fun `out-of-range minutes wrap into the day`() {
        assertEquals(clockParts(7 * 60), clockParts(31 * 60))
        assertEquals(ClockParts(11, 0, isAm = false), clockParts(-60))
    }
}
