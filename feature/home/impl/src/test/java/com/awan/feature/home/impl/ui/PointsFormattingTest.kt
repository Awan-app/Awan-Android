package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.formatAbbreviatedPoints
import org.junit.Assert.assertEquals
import org.junit.Test

class PointsFormattingTest {

    @Test
    fun `formatAbbreviatedPoints formats numbers under 1k as raw integers`() {
        assertEquals("0", formatAbbreviatedPoints(0))
        assertEquals("50", formatAbbreviatedPoints(50))
        assertEquals("999", formatAbbreviatedPoints(999))
    }

    @Test
    fun `formatAbbreviatedPoints formats numbers in thousands with k suffix`() {
        assertEquals("1k", formatAbbreviatedPoints(1000))
        assertEquals("1.2k", formatAbbreviatedPoints(1200))
        assertEquals("1.3k", formatAbbreviatedPoints(1290))
        assertEquals("10k", formatAbbreviatedPoints(10000))
        assertEquals("10.5k", formatAbbreviatedPoints(10500))
        assertEquals("100k", formatAbbreviatedPoints(100000))
    }

    @Test
    fun `formatAbbreviatedPoints formats numbers in millions with M suffix`() {
        assertEquals("1M", formatAbbreviatedPoints(1000000))
        assertEquals("1.2M", formatAbbreviatedPoints(1200000))
        assertEquals("15.5M", formatAbbreviatedPoints(15500000))
    }
}
