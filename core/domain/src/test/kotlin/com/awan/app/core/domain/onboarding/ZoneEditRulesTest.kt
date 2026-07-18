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
}
