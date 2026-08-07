package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.designsystem.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiMappersTest {

    @Test
    fun `resolveNonOverlappingZones adjusts overlapping zones sequentially`() {
        val zone1 = ScheduleZone(
            id = "z1",
            categoryId = "cat1",
            category = TaskCategory.Study,
            startHour = 8,
            endHour = 12,
        )
        val zone2 = ScheduleZone(
            id = "z2",
            categoryId = "cat2",
            category = TaskCategory.Work,
            startHour = 10,
            endHour = 14,
        )
        val zone3 = ScheduleZone(
            id = "z3",
            categoryId = "cat3",
            category = TaskCategory.Personal,
            startHour = 13,
            endHour = 16,
        )

        val resolved = resolveNonOverlappingZones(listOf(zone1, zone2, zone3))

        assertEquals(3, resolved.size)
        // Zone 1 remains 8..12
        assertEquals(8, resolved[0].startHour)
        assertEquals(12, resolved[0].endHour)
        // Zone 2 (duration 4h) adjusted to start at 12 -> 12..16
        assertEquals(12, resolved[1].startHour)
        assertEquals(16, resolved[1].endHour)
        // Zone 3 (duration 3h) adjusted to start at 16 -> 16..19
        assertEquals(16, resolved[2].startHour)
        assertEquals(19, resolved[2].endHour)

        // Verify no two zones overlap
        for (i in 0 until resolved.size - 1) {
            assertTrue(resolved[i].endHour <= resolved[i + 1].startHour)
        }
    }
}
