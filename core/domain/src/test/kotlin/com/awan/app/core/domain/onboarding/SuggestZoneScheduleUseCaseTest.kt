package com.awan.app.core.domain.onboarding

import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.domain.onboarding.usecase.SuggestZoneScheduleUseCase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestZoneScheduleUseCaseTest {

    private val suggest = SuggestZoneScheduleUseCase()

    private fun windows(zones: List<Zone>) = zones.map { it.id to (it.startMinutes to it.endMinutes) }

    @Test
    fun `normal day splits by weight from wake+30 to sleep-30`() {
        val zones = suggest(DayBounds(wakeMinutes = 7 * 60, sleepMinutes = 23 * 60))
        assertEquals(
            listOf(
                Zone.WORK to (450 to 810),
                Zone.LEARNING to (810 to 930),
                Zone.PERSONAL to (930 to 1050),
                Zone.GENERAL to (1050 to 1350),
            ),
            windows(zones),
        )
    }

    @Test
    fun `weights are shares of the pool, so a short day scales all four down`() {
        // 8h waking leaves 7h usable; every zone keeps its 6:2:2:5 share rather than the last starving.
        val zones = suggest(DayBounds(wakeMinutes = 9 * 60, sleepMinutes = 17 * 60))
        val durations = zones.map { it.endMinutes - it.startMinutes }
        assertTrue(durations.all { it > 0 })
        assertEquals(7 * 60, durations.sum())
        assertEquals(durations[1], durations[2])
        assertTrue(durations[0] > durations[3] && durations[3] > durations[1])
    }

    @Test
    fun `overnight day wraps the first zone past midnight`() {
        val zones = suggest(DayBounds(wakeMinutes = 22 * 60, sleepMinutes = 6 * 60))
        assertEquals(
            listOf(
                Zone.WORK to (1350 to 1520),
                Zone.LEARNING to (80 to 135),
                Zone.PERSONAL to (135 to 190),
                Zone.GENERAL to (190 to 330),
            ),
            windows(zones),
        )
    }

    @Test
    fun `tiny window degrades to contiguous zero-width zones without crashing`() {
        val zones = suggest(DayBounds(wakeMinutes = 10 * 60, sleepMinutes = 11 * 60))
        assertEquals(4, zones.size)
        assertTrue(zones.all { it.startMinutes == 630 && it.endMinutes == 630 })
    }

    @Test
    fun `all suggested zones are enabled and in fixed default order`() {
        val zones = suggest(DayBounds.Default)
        assertEquals(listOf(Zone.WORK, Zone.LEARNING, Zone.PERSONAL, Zone.GENERAL), zones.map { it.id })
        assertTrue(zones.all { it.isEnabled })
    }
}
