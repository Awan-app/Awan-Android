package com.awan.app.core.data.notifications

import com.awan.app.core.data.notifications.mapper.toUpcomingSessions
import com.awan.app.core.database.model.UpcomingSessionRow
import com.awan.app.core.model.SessionStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingSessionMapperTest {

    @Test
    fun `a session maps to the datetimes the scheduler alarms on`() {
        val session = listOf(row(date = "2026-08-14", start = "09:00", end = "10:30"))
            .toUpcomingSessions()
            .single()

        assertEquals(LocalDateTime.of(2026, 8, 14, 9, 0), session.start)
        assertEquals(LocalDateTime.of(2026, 8, 14, 10, 30), session.end)
        assertEquals("Deep work", session.title)
        assertEquals(SessionStatus.SCHEDULED, session.status)
    }

    @Test
    fun `a session dragged past midnight ends on the following day`() {
        // The row keeps the start date for both times, so an end before the start is the next day —
        // not a session of negative length, which would make every duration and progress bar wrong.
        val session = listOf(row(date = "2026-08-14", start = "23:30", end = "00:30"))
            .toUpcomingSessions()
            .single()

        assertEquals(LocalDateTime.of(2026, 8, 14, 23, 30), session.start)
        assertEquals(LocalDateTime.of(2026, 8, 15, 0, 30), session.end)
        assertTrue(session.end.isAfter(session.start))
    }

    @Test
    fun `an unparseable row is dropped rather than defaulted`() {
        // A row that silently became midnight today would fire a notification at the wrong time,
        // which is worse than firing none.
        val rows = listOf(
            row(id = "bad-date", date = "not-a-date"),
            row(id = "bad-start", start = "25:00"),
            row(id = "bad-end", end = ""),
            row(id = "good"),
        )

        val mapped = rows.toUpcomingSessions()

        assertEquals(listOf("good"), mapped.map { it.id })
    }

    @Test
    fun `every status the backend sends survives the round trip`() {
        val statuses = listOf(
            "SCHEDULED" to SessionStatus.SCHEDULED,
            "in_progress" to SessionStatus.IN_PROGRESS,
            "COMPLETED" to SessionStatus.COMPLETED,
            "CANCELLED" to SessionStatus.CANCELLED,
            "MISSED" to SessionStatus.MISSED,
            // Anything new the backend grows is UNKNOWN, never a status that means something else:
            // the planner skips completed and cancelled sessions, so a wrong guess silences a real
            // block or announces a dead one.
            "something_new" to SessionStatus.UNKNOWN,
        )

        statuses.forEach { (raw, expected) ->
            assertEquals(raw, expected, listOf(row(status = raw)).toUpcomingSessions().single().status)
        }
    }

    private fun row(
        id: String = "session-1",
        date: String = "2026-08-14",
        start: String = "09:00",
        end: String = "10:00",
        status: String = "SCHEDULED",
    ) = UpcomingSessionRow(
        id = id,
        taskId = "task-1",
        title = "Deep work",
        date = date,
        startTime = start,
        endTime = end,
        status = status,
        zoneId = "study",
    )
}
