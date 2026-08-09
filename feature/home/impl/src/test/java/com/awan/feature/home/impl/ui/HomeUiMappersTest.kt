package com.awan.feature.home.impl.ui

import com.awan.app.core.domain.home.model.DayZone
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiMappersTest {

    @Test
    fun `toUiZone maps non-integer hour zones accurately without overlap`() {
        // Zone 1: 1:30 PM (90 mins) to 3:30 PM (210 mins)
        val dayZone1 = DayZone(
            id = "zone_1",
            name = "Work Zone",
            categoryId = "cat_1",
            categoryName = "Work",
            color = "#FF0000",
            startMinutes = 90,
            endMinutes = 210,
        )

        // Zone 2: 3:30 PM (210 mins) to 5:30 PM (330 mins)
        val dayZone2 = DayZone(
            id = "zone_2",
            name = "Study Zone",
            categoryId = "cat_2",
            categoryName = "Study",
            color = "#00FF00",
            startMinutes = 210,
            endMinutes = 330,
        )

        val uiZone1 = dayZone1.toUiZone()
        val uiZone2 = dayZone2.toUiZone()

        assertEquals(90, uiZone1.startMinutes)
        assertEquals(210, uiZone1.endMinutes)

        assertEquals(210, uiZone2.startMinutes)
        assertEquals(330, uiZone2.endMinutes)

        // Verify Zone 1 (90..210) and Zone 2 (210..330) meet at 210 with zero minute overlap
        val overlapMinutes = maxOf(0, minOf(uiZone1.endMinutes, uiZone2.endMinutes) - maxOf(uiZone1.startMinutes, uiZone2.startMinutes))
        assertEquals(0, overlapMinutes)
    }
}
