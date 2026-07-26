package com.awan.app.core.domain.onboarding

import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneEditRulesTest {

    private val bounds = DayBounds.Default

    @Test
    fun `snapToFive rounds to nearest five`() {
        assertEquals(10, ZoneEditRules.snapToFive(12))
        assertEquals(15, ZoneEditRules.snapToFive(13))
        assertEquals(5, ZoneEditRules.snapToFive(7))
    }

    @Test
    fun `editWindow snaps edges and preserves an in-range window`() {
        val zone = Zone("z", "Z", 0, startMinutes = 450, endMinutes = 675)
        val edited = ZoneEditRules.editWindow(zone, newStartMinutes = 452, newEndMinutes = 673, bounds = bounds)
        assertEquals(450, edited.startMinutes)
        assertEquals(675, edited.endMinutes)
    }

    @Test
    fun `editWindow enforces the 15-minute minimum`() {
        val zone = Zone("z", "Z", 0, startMinutes = 450, endMinutes = 675)
        val edited = ZoneEditRules.editWindow(zone, newStartMinutes = 660, newEndMinutes = 670, bounds = bounds)
        assertEquals(ZoneEditRules.MIN_ZONE_MINUTES, edited.endMinutes - edited.startMinutes)
    }

    @Test
    fun `editWindow never lets a window run past midnight`() {
        val overnight = DayBounds(wakeMinutes = 22 * 60, sleepMinutes = 6 * 60)
        val zone = Zone("z", "Z", 0, startMinutes = 23 * 60, endMinutes = 23 * 60 + 60)

        val edited = ZoneEditRules.editWindow(zone, newStartMinutes = 23 * 60, newEndMinutes = 24 * 60 + 60, overnight)

        assertEquals(23 * 60, edited.startMinutes)
        assertEquals(DayBounds.MINUTES_PER_DAY, edited.endMinutes)
    }

    @Test
    fun `overlapping enabled zones are flagged, disabled ones ignored`() {
        val a = Zone("a", "A", 0, startMinutes = 450, endMinutes = 675)
        val b = Zone("b", "B", 0, startMinutes = 600, endMinutes = 800)
        val c = Zone("c", "C", 0, startMinutes = 600, endMinutes = 800, isEnabled = false)
        val flagged = ZoneEditRules.overlappingZoneIds(listOf(a, b, c), bounds)
        assertTrue("a" in flagged)
        assertTrue("b" in flagged)
        assertFalse("c" in flagged)
    }

    @Test
    fun `adjacent non-overlapping zones are not flagged`() {
        val a = Zone("a", "A", 0, startMinutes = 450, endMinutes = 675)
        val b = Zone("b", "B", 0, startMinutes = 675, endMinutes = 900)
        assertTrue(ZoneEditRules.overlappingZoneIds(listOf(a, b), bounds).isEmpty())
    }

    @Test
    fun `resequence keeps durations and chains zones back to back`() {
        val zones = listOf(
            Zone("a", "A", 0, startMinutes = 450, endMinutes = 570),
            Zone("b", "B", 0, startMinutes = 570, endMinutes = 750),
            Zone("c", "C", 0, startMinutes = 750, endMinutes = 810),
        )
        val moved = listOf(zones[2], zones[0], zones[1])

        val result = ZoneEditRules.resequence(moved, bounds)

        assertEquals(listOf(60, 120, 180), result.map { it.durationMinutes })
        assertEquals(450, result[0].startMinutes)
        assertEquals(510, result[1].startMinutes)
        assertEquals(630, result[2].startMinutes)
        assertTrue(ZoneEditRules.overlappingZoneIds(result, bounds).isEmpty())
    }

    @Test
    fun `resequence is a no-op on an already contiguous schedule`() {
        val zones = listOf(
            Zone("a", "A", 0, startMinutes = 450, endMinutes = 570),
            Zone("b", "B", 0, startMinutes = 570, endMinutes = 750),
        )
        assertEquals(zones, ZoneEditRules.resequence(zones, bounds))
    }

    @Test
    fun `resequence chains correctly when the waking window crosses midnight`() {
        val overnight = DayBounds(wakeMinutes = 22 * 60, sleepMinutes = 6 * 60)
        val zones = listOf(
            Zone("a", "A", 0, startMinutes = 23 * 60, endMinutes = 23 * 60 + 60),
            Zone("b", "B", 0, startMinutes = 22 * 60 + 30, endMinutes = 22 * 60 + 90),
            Zone("c", "C", 0, startMinutes = 60, endMinutes = 120),
        )

        val result = ZoneEditRules.resequence(zones, overnight)

        // Anchored at the earliest occupied start (22:30), then chained straight through midnight.
        assertEquals(22 * 60 + 30, result[0].startMinutes)
        assertEquals(23 * 60 + 30, result[1].startMinutes)
        assertEquals(30, result[2].startMinutes)
        assertEquals(listOf(60, 60, 60), result.map { it.durationMinutes })
        assertTrue(ZoneEditRules.overlappingZoneIds(result, overnight).isEmpty())
    }
}
