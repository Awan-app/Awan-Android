package com.awan.app.core.domain.onboarding

import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.onboarding.utils.DayBoundsValidation
import com.awan.app.core.domain.onboarding.utils.ValidateDayBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateDayBoundsTest {

    private val validate = ValidateDayBounds()

    @Test
    fun `same wake and sleep is a hard error`() {
        assertEquals(DayBoundsValidation.SameTime, validate(DayBounds(480, 480)))
    }

    @Test
    fun `waking window under four hours warns softly`() {
        assertEquals(DayBoundsValidation.ShortWakingWindow, validate(DayBounds(10 * 60, 13 * 60)))
    }

    @Test
    fun `overnight short window is correctly detected`() {
        assertEquals(DayBoundsValidation.ShortWakingWindow, validate(DayBounds(23 * 60, 2 * 60)))
    }

    @Test
    fun `normal day is valid`() {
        assertEquals(DayBoundsValidation.Valid, validate(DayBounds.Default))
    }

    @Test
    fun `overnight long day is valid`() {
        assertEquals(DayBoundsValidation.Valid, validate(DayBounds(22 * 60, 6 * 60)))
    }
}
