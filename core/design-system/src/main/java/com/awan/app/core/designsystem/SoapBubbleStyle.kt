package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.Color

object SoapBubbleStyle {
    const val DEFAULT_CYCLE_DURATION_MILLIS = 12_000
    const val DEFAULT_STRENGTH = 0.90f
    const val SOAP_BUBBLE_COLOR_INTENSITY = 0.40f
    const val SOAP_BUBBLE_COLOR_INTENSITY_ANDROID_12 = 1.80f

    const val SEGMENT_COUNT = 72
    const val SEGMENT_OVERLAP_DEGREES = 1.2f
    const val MIN_VISIBLE_ALPHA = 0.005f

    // 5 concentric overlapping wave bands covering the full bubble surface
    val RingRadii = floatArrayOf(0.30f, 0.48f, 0.65f, 0.80f, 0.92f)
    val RingWidths = floatArrayOf(0.24f, 0.22f, 0.20f, 0.16f, 0.10f)

    val DEFAULT_BASE_COLOR = Color(0xFF10B981)

    val UrgentRed = Color(0xFFEF4444)
    val MidAmber = Color(0xFFF59E0B)
    val CalmEmerald = Color(0xFF10B981)

    const val TWO_PI = 6.28318530718f
    const val PI = 3.14159265359f

    /**
     * Determines base color based on fraction of time remaining before deadline over full duration:
     * - 0.0 (deadline today / urgent): Warning Red (0xFFEF4444)
     * - 0.5 (halfway): Warm Amber (0xFFF59E0B)
     * - 1.0 (plenty of time): Calm Emerald Green (0xFF10B981)
     */
    fun colorForTimeRemaining(fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return if (f < 0.5f) {
            val t = f / 0.5f
            mixColor(UrgentRed, MidAmber, t)
        } else {
            val t = (f - 0.5f) / 0.5f
            mixColor(MidAmber, CalmEmerald, t)
        }
    }

    internal fun mix(a: Float, b: Float, t: Float): Float = a * (1f - t) + b * t

    internal fun mixColor(c1: Color, c2: Color, a: Float): Color {
        val clampedA = a.coerceIn(0f, 1f)
        return Color(
            red = mix(c1.red, c2.red, clampedA),
            green = mix(c1.green, c2.green, clampedA),
            blue = mix(c1.blue, c2.blue, clampedA),
            alpha = 1f,
        )
    }
}
