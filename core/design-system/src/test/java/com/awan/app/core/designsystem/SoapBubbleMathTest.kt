package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoapBubbleMathTest {

    @Test
    fun sampleSoapFilm_periodicityAtTimeZeroAndOne() {
        val sampleAtZero = SoapBubbleMath.sampleSoapFilm(
            normalizedRadius = 0.84f,
            angleRadians = 1.2f,
            normalizedTime = 0.0f,
            strength = 1.0f,
        )

        val sampleAtOne = SoapBubbleMath.sampleSoapFilm(
            normalizedRadius = 0.84f,
            angleRadians = 1.2f,
            normalizedTime = 1.0f,
            strength = 1.0f,
        )

        assertEquals(sampleAtZero.alpha, sampleAtOne.alpha, 0.001f)
        assertEquals(sampleAtZero.color.red, sampleAtOne.color.red, 0.001f)
        assertEquals(sampleAtZero.color.green, sampleAtOne.color.green, 0.001f)
        assertEquals(sampleAtZero.color.blue, sampleAtOne.color.blue, 0.001f)
    }

    @Test
    fun sampleSoapFilm_alphaIsWithinBounds() {
        val radii = SoapBubbleStyle.RingRadii
        for (r in radii) {
            for (angleIndex in 0 until 36) {
                val angle = (angleIndex.toFloat() / 36f) * SoapBubbleStyle.TWO_PI
                for (timeStep in 0..10) {
                    val time = timeStep / 10f
                    val sample = SoapBubbleMath.sampleSoapFilm(
                        normalizedRadius = r,
                        angleRadians = angle,
                        normalizedTime = time,
                        strength = 1.0f,
                    )

                    assertTrue("Alpha should be >= 0", sample.alpha >= 0f)
                    assertTrue("Alpha should be <= 0.40", sample.alpha <= 0.40f + 1e-4f)
                    assertFalse("Alpha should not be NaN", sample.alpha.isNaN())
                    assertFalse("Alpha should not be Infinite", sample.alpha.isInfinite())
                }
            }
        }
    }

    @Test
    fun sampleSoapFilm_colorChannelsAreValid() {
        for (angleIndex in 0 until 18) {
            val angle = (angleIndex.toFloat() / 18f) * SoapBubbleStyle.TWO_PI
            val sample = SoapBubbleMath.sampleSoapFilm(
                normalizedRadius = 0.84f,
                angleRadians = angle,
                normalizedTime = 0.25f,
                strength = 1.0f,
                baseColor = SoapBubbleStyle.UrgentRed,
            )

            assertTrue(sample.color.red in 0f..1f)
            assertTrue(sample.color.green in 0f..1f)
            assertTrue(sample.color.blue in 0f..1f)
            assertTrue(sample.color.alpha in 0f..1f)
        }
    }

    @Test
    fun colorForTimeRemaining_returnsExpectedStops() {
        val red = SoapBubbleStyle.colorForTimeRemaining(0.0f)
        assertEquals(SoapBubbleStyle.UrgentRed.red, red.red, 0.01f)
        assertEquals(SoapBubbleStyle.UrgentRed.green, red.green, 0.01f)
        assertEquals(SoapBubbleStyle.UrgentRed.blue, red.blue, 0.01f)

        val amber = SoapBubbleStyle.colorForTimeRemaining(0.5f)
        assertEquals(SoapBubbleStyle.MidAmber.red, amber.red, 0.01f)
        assertEquals(SoapBubbleStyle.MidAmber.green, amber.green, 0.01f)
        assertEquals(SoapBubbleStyle.MidAmber.blue, amber.blue, 0.01f)

        val green = SoapBubbleStyle.colorForTimeRemaining(1.0f)
        assertEquals(SoapBubbleStyle.CalmEmerald.red, green.red, 0.01f)
        assertEquals(SoapBubbleStyle.CalmEmerald.green, green.green, 0.01f)
        assertEquals(SoapBubbleStyle.CalmEmerald.blue, green.blue, 0.01f)
    }

    @Test
    fun soapPalette_producesMonochromaticShadesOfBaseColor() {
        val base = Color(0xFFEF4444)
        val phases = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.5f, 3.5f, 4.0f)
        for (p in phases) {
            val color = SoapBubbleMath.soapPalette(p, base)
            assertTrue(color.red in 0f..1f)
            assertTrue(color.green in 0f..1f)
            assertTrue(color.blue in 0f..1f)
            // Red component should remain dominant for red base color
            assertTrue(color.red >= color.green)
            assertTrue(color.red >= color.blue)
        }
    }
}
