package com.awan.app.core.notifications

import com.awan.app.core.notifications.model.SessionWindow
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDismissalRecordTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 13, 9, 0)
    private val window = SessionWindow(start = now.minusMinutes(10), end = now.plusMinutes(50))

    @Test
    fun `a window survives the round trip through storage`() {
        // If it does not, every dismissal is silently dropped on the next read and the notification
        // the user stopped comes straight back.
        assertEquals(window, LiveDismissalRecord.decode(LiveDismissalRecord.encode(window)))
    }

    @Test
    fun `an unreadable record decodes to nothing rather than throwing`() {
        assertNull(LiveDismissalRecord.decode(""))
        assertNull(LiveDismissalRecord.decode("not-a-date"))
        assertNull(LiveDismissalRecord.decode("2026-08-13T09:00"))
        assertNull(LiveDismissalRecord.decode("2026-08-13T09:00|nonsense"))
    }

    @Test
    fun `a dismissal lasts exactly as long as the session it names`() {
        // Not a moment longer: without expiry the store is append-only and every dismissed session
        // leaves a record behind forever.
        assertFalse(LiveDismissalRecord.isExpired(window, now))
        assertFalse(LiveDismissalRecord.isExpired(window, window.end.minusSeconds(1)))
        assertTrue(LiveDismissalRecord.isExpired(window, window.end))
        assertTrue(LiveDismissalRecord.isExpired(window, window.end.plusDays(1)))
    }
}
