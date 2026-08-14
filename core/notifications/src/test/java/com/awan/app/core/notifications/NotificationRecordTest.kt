package com.awan.app.core.notifications

import com.awan.app.core.notifications.model.SessionWindow
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRecordTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 13, 9, 0)
    private val window = SessionWindow(start = now.minusMinutes(10), end = now.plusMinutes(50))

    @Test
    fun `a window survives the round trip through storage`() {
        // If it does not, every record is silently dropped on the next read and the notification the
        // user stopped — or already received — comes straight back.
        assertEquals(window, NotificationRecord.decode(NotificationRecord.encode(window)))
    }

    @Test
    fun `an unreadable record decodes to nothing rather than throwing`() {
        assertNull(NotificationRecord.decode(""))
        assertNull(NotificationRecord.decode("not-a-date"))
        assertNull(NotificationRecord.decode("2026-08-13T09:00"))
        assertNull(NotificationRecord.decode("2026-08-13T09:00|nonsense"))
    }

    @Test
    fun `a record lasts exactly as long as the window it names`() {
        // Not a moment longer: without expiry the store is append-only and every dismissed session
        // and every posted notification leaves a record behind forever.
        assertFalse(NotificationRecord.isExpired(window, now))
        assertFalse(NotificationRecord.isExpired(window, window.end.minusSeconds(1)))
        assertTrue(NotificationRecord.isExpired(window, window.end))
        assertTrue(NotificationRecord.isExpired(window, window.end.plusDays(1)))
    }
}
