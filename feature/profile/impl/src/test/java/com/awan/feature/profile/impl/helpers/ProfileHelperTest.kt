package com.awan.feature.profile.impl.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHelperTest {

    @Test
    fun `getDisplayName returns first and last name when both present`() {
        assertEquals("John Doe", ProfileHelper.getDisplayName("John", "Doe", "john@example.com"))
    }

    @Test
    fun `getDisplayName returns only first name when last name is not entered`() {
        assertEquals("John", ProfileHelper.getDisplayName("John", "not entered", "john@example.com"))
    }

    @Test
    fun `getDisplayName returns only first name when last name is blank`() {
        assertEquals("John", ProfileHelper.getDisplayName("John", "", "john@example.com"))
    }

    @Test
    fun `getDisplayName falls back to email prefix when names are missing or not entered`() {
        assertEquals("john", ProfileHelper.getDisplayName(null, "not entered", "john@example.com"))
        assertEquals("john", ProfileHelper.getDisplayName("", "", "john@example.com"))
    }
}
