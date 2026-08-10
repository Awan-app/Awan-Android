package com.awan.app.core.data.common

import org.junit.Assert.assertEquals
import org.junit.Test

class IsoTimeUtilsTest {

    /**
     * The shape the API actually sends (`docs/feature/backend/AWAN_API_DOCUMENTATION.md:706`) — no
     * offset. It parses as neither `OffsetDateTime` nor `LocalTime`, which is why moving a session
     * used to write its *old* time back to Room and the card snapped back on screen.
     */
    @Test
    fun `an offset-free server timestamp keeps its time`() {
        assertEquals("09:00:00", extractTimeFromIso("2026-07-22T09:00:00"))
        assertEquals("2026-07-22", extractDateFromIso("2026-07-22T09:00:00"))
    }

    @Test
    fun `a zoned timestamp keeps its time`() {
        assertEquals("09:00:00", extractTimeFromIso("2026-07-22T09:00:00Z"))
        assertEquals("2026-07-22", extractDateFromIso("2026-07-22T09:00:00Z"))
    }

    @Test
    fun `an already-extracted time is passed through`() {
        assertEquals("09:00:00", extractTimeFromIso("09:00:00"))
    }

    @Test
    fun `an unparseable value falls back rather than throwing`() {
        assertEquals("07:30:00", extractTimeFromIso("not a time", fallback = "07:30:00"))
        assertEquals("00:00:00", extractTimeFromIso("not a time"))
    }

    /** Sub-second precision appears on `createdAt`; it must not defeat the parse. */
    @Test
    fun `fractional seconds are tolerated`() {
        assertEquals("10:44:03", extractTimeFromIso("2026-08-09T10:44:03.820200"))
    }
}
