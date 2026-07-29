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
    fun `normal day splits equally from wake+30 to sleep-30`() {
        val zones = suggest(DayBounds(wakeMinutes = 7 * 60, sleepMinutes = 23 * 60))
        assertEquals(
            listOf(
                Zone.STUDY to (450 to 675),
                Zone.WORK to (675 to 900),
                Zone.PLAY to (900 to 1125),
                Zone.PERSONAL to (1125 to 1350),
            ),
            windows(zones),
        )
    }

    @Test
    fun `overnight day wraps the first zone past midnight`() {
        val zones = suggest(DayBounds(wakeMinutes = 22 * 60, sleepMinutes = 6 * 60))
        assertEquals(
            listOf(
                Zone.STUDY to (1350 to 1455),
                Zone.WORK to (15 to 120),
                Zone.PLAY to (120 to 225),
                Zone.PERSONAL to (225 to 330),
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
        assertEquals(listOf(Zone.STUDY, Zone.WORK, Zone.PLAY, Zone.PERSONAL), zones.map { it.id })
        assertTrue(zones.all { it.isEnabled })
    }
}
