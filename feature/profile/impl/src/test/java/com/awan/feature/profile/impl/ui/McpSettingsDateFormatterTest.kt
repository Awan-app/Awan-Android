package com.awan.feature.profile.impl.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class McpSettingsDateFormatterTest {

    @Test
    fun `formats token creation timestamp as date only`() {
        assertEquals(
            "2026-08-11",
            formatMcpTokenCreationDate("2026-08-11T12:34:56.789Z")
        )
    }
}
