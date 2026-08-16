package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverrideResolutionTest {

    @Test
    fun `findOverrideForDate returns correct override`() {
        val overrides = listOf(
            TemplateOverride(id = "1", dateOfDay = "2023-10-01", zones = emptyList()),
            TemplateOverride(id = "2", dateOfDay = "2023-10-02", zones = emptyList())
        )

        val result = DailyZonesHelper.findOverrideForDate(overrides, "2023-10-02")
        assertEquals("2", result?.id)
    }

    @Test
    fun `findOverrideForDate returns null if no match`() {
        val overrides = listOf(
            TemplateOverride(id = "1", dateOfDay = "2023-10-01", zones = emptyList())
        )

        val result = DailyZonesHelper.findOverrideForDate(overrides, "2023-10-02")
        assertNull(result)
    }
}
