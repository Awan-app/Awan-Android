package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.formatAbbreviatedPointsValue
import org.junit.Assert.assertEquals
import org.junit.Test

class PointsFormattingTest {

    @Test
    fun `formatAbbreviatedPointsValue formats numbers under 1k as raw integers`() {
        assertEquals("0", formatAbbreviatedPointsValue(0))
        assertEquals("50", formatAbbreviatedPointsValue(50))
        assertEquals("999", formatAbbreviatedPointsValue(999))
    }

    @Test
    fun `formatAbbreviatedPointsValue formats numbers in thousands with k suffix`() {
        assertEquals("1k", formatAbbreviatedPointsValue(1000))
        assertEquals("1.2k", formatAbbreviatedPointsValue(1200))
        assertEquals("1.3k", formatAbbreviatedPointsValue(1290))
        assertEquals("10k", formatAbbreviatedPointsValue(10000))
        assertEquals("10.5k", formatAbbreviatedPointsValue(10500))
        assertEquals("100k", formatAbbreviatedPointsValue(100000))
        assertEquals("999.9k", formatAbbreviatedPointsValue(999949))
    }

    @Test
    fun `formatAbbreviatedPointsValue promotes post-rounding unit boundary to M or B`() {
        // 999_950 / 1000 = 999.95 -> rounds to 1000.0 -> promoted to 1M
        assertEquals("1M", formatAbbreviatedPointsValue(999950))
        assertEquals("1M", formatAbbreviatedPointsValue(999999))
        assertEquals("1M", formatAbbreviatedPointsValue(1000000))

        // 999_950_000 / 1_000_000 = 999.95 -> rounds to 1000.0 -> promoted to 1B
        assertEquals("1B", formatAbbreviatedPointsValue(999950000))
        assertEquals("1B", formatAbbreviatedPointsValue(999999999))
        assertEquals("1B", formatAbbreviatedPointsValue(1000000000))
    }

    @Test
    fun `formatAbbreviatedPointsValue handles Int MIN_VALUE and Int MAX_VALUE without overflow`() {
        assertEquals("2.1B", formatAbbreviatedPointsValue(Int.MAX_VALUE))
        assertEquals("-2.1B", formatAbbreviatedPointsValue(Int.MIN_VALUE))
    }

    @Test
    fun `formatAbbreviatedPointsValue handles negative boundary values correctly`() {
        assertEquals("-500", formatAbbreviatedPointsValue(-500))
        assertEquals("-1k", formatAbbreviatedPointsValue(-1000))
        assertEquals("-1M", formatAbbreviatedPointsValue(-999950))
        assertEquals("-1M", formatAbbreviatedPointsValue(-1000000))
        assertEquals("-1B", formatAbbreviatedPointsValue(-999999999))
    }

    @Test
    fun `formatAbbreviatedPointsValue supports localized Arabic format strings`() {
        val thousandFmt = "%1\$s ألف"
        val millionFmt = "%1\$s مليون"
        val billionFmt = "%1\$s مليار"

        assertEquals("1.2 ألف", formatAbbreviatedPointsValue(1200, thousandFmt, millionFmt, billionFmt))
        assertEquals("1.5 مليون", formatAbbreviatedPointsValue(1500000, thousandFmt, millionFmt, billionFmt))
        assertEquals("2.1 مليار", formatAbbreviatedPointsValue(2100000000, thousandFmt, millionFmt, billionFmt))
    }
}
