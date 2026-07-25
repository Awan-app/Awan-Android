package com.awan.app.core.domain.onboarding

import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.Zone
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleFirstTaskUseCaseTest {

    private val schedule = ScheduleFirstTaskUseCase()
    private val zones = SuggestZoneScheduleUseCase()(DayBounds.Default)

    @Test
    fun `lands at the start of the first enabled zone with the preferred length`() {
        val task = schedule("Write brief", zones, preferredTaskLengthMinutes = 45)
        assertEquals(Zone.STUDY, task.zoneId)
        assertEquals(zones.first().startMinutes, task.startMinutes)
        assertEquals(45, task.durationMinutes)
    }

    @Test
    fun `skips disabled zones`() {
        val withStudyOff = zones.map { if (it.id == Zone.STUDY) it.copy(isEnabled = false) else it }
        val task = schedule("Standup", withStudyOff, preferredTaskLengthMinutes = 60)
        assertEquals(Zone.WORK, task.zoneId)
        assertEquals(withStudyOff.first { it.id == Zone.WORK }.startMinutes, task.startMinutes)
    }

    @Test
    fun `trims the title`() {
        val task = schedule("  hello  ", zones, preferredTaskLengthMinutes = 60)
        assertEquals("hello", task.title)
    }
}
