package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal data class SoapFilmSample(
    val color: Color,
    val alpha: Float,
    val distortedRadius: Float = 0f,
)

internal object SoapBubbleMath {
    private const val TWO_PI = SoapBubbleStyle.TWO_PI

    private fun fract(x: Float): Float = x - floor(x)

    private fun clamp(v: Float, min: Float, max: Float): Float =
        if (v < min) min else if (v > max) max else v

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun mix(x: Float, y: Float, a: Float): Float = x * (1f - a) + y * a

    private fun mixColor(c1: Color, c2: Color, a: Float): Color {
        val clampedA = clamp(a, 0f, 1f)
        return Color(
            red = mix(c1.red, c2.red, clampedA),
            green = mix(c1.green, c2.green, clampedA),
            blue = mix(c1.blue, c2.blue, clampedA),
            alpha = 1f,
        )
    }

    fun soapPalette(t: Float, baseColor: Color = SoapBubbleStyle.DEFAULT_BASE_COLOR): Color {
        var phase = fract(t) * 4.0f
        if (phase < 0f) phase += 4.0f

        // Monochromatic palette: 4 distinct shades/tints of the same base color
        val shade0 = mixColor(baseColor, Color.White, 0.45f)
        val shade1 = mixColor(baseColor, Color.White, 0.20f)
        val shade2 = baseColor
        val shade3 = mixColor(baseColor, Color.Black, 0.18f)

        return when {
            phase < 1.0f -> mixColor(shade0, shade1, phase)
            phase < 2.0f -> mixColor(shade1, shade2, phase - 1.0f)
            phase < 3.0f -> mixColor(shade2, shade3, phase - 2.0f)
            else -> mixColor(shade3, shade0, phase - 3.0f)
        }
    }

    fun sampleSoapFilm(
        normalizedRadius: Float,
        angleRadians: Float,
        normalizedTime: Float,
        strength: Float,
        baseColor: Color = SoapBubbleStyle.DEFAULT_BASE_COLOR,
    ): SoapFilmSample {
        val r = normalizedRadius
        val angle = angleRadians
        val t = normalizedTime * TWO_PI

        val px = cos(angle) * r
        val py = sin(angle) * r

        // 1. Organic wave distortion matching AGSL shader with integer time harmonics for seamless looping
        val wave1 = sin(angle * 3.0f + t + r * 5.0f) * 0.06f
        val wave2 = sin(angle * 5.0f - t - r * 8.0f) * 0.04f
        val wave3 = sin(px * 4.0f + py * 3.0f + t) * 0.03f
        val distortedR = r + wave1 + wave2 + wave3

        // 2. Swirling flow
        val flow = sin(angle * 4.0f + t + distortedR * 6.0f) * 0.35f

        // 3. Film thickness & interference simulation
        val band1 = sin(distortedR * 22.0f + flow * 2.0f - t * 2.0f)
        val band2 = sin(angle * 6.0f - distortedR * 14.0f + t * 2.0f)
        val band3 = sin(cos(angle) * 8.0f + sin(angle) * 6.0f + t)
        val interference = band1 * 0.45f + band2 * 0.35f + band3 * 0.20f

        // 4. Iridescence color phase
        val phase = (distortedR * 4.0f + angle * 0.8f - t + flow * 0.5f) / TWO_PI
        val cycle = phase + interference * 0.25f

        // 5. Film region mask with gradient breakpoints:
        // - Center (r = 0.0): 10% (0.10) opacity
        // - 30% in from edge (r = 0.70): 80% (0.80) opacity
        // - Outer edge (r = 1.00): 100% (1.00) full opacity
        val clampedR = clamp(distortedR, 0f, 1.0f)
        val radialAlpha = if (clampedR < 0.70f) {
            mix(0.10f, 0.80f, clampedR / 0.70f)
        } else {
            mix(0.80f, 1.00f, (clampedR - 0.70f) / 0.30f)
        }
        val edgeMask = 1.0f - smoothstep(0.95f, 1.0f, distortedR)
        val filmRegion = radialAlpha * edgeMask

        // 6. Swirl patches and gravity boost
        val patches = (sin(distortedR * 8.0f - t + angle * 2.0f) * 0.5f + 0.5f)
        val dirY = sin(angle)
        val gravity = smoothstep(-0.45f, 0.90f, dirY)
        val gravityBoost = mix(0.85f, 1.25f, gravity)

        // 7. Monochromatic palette sampling
        val rawRainbow = soapPalette(cycle, baseColor)

        val intensity = SoapBubbleStyle.SOAP_BUBBLE_COLOR_INTENSITY_ANDROID_12
        val rainbow = Color(
            red = clamp(0.5f + (rawRainbow.red - 0.5f) * intensity, 0f, 1f),
            green = clamp(0.5f + (rawRainbow.green - 0.5f) * intensity, 0f, 1f),
            blue = clamp(0.5f + (rawRainbow.blue - 0.5f) * intensity, 0f, 1f),
            alpha = 1.0f,
        )

        // 8. Alpha computation calibrated for rich visibility on Canvas
        val rainbowAlpha = filmRegion * (0.06f + patches * 0.28f) * gravityBoost * strength
        val clampedAlpha = clamp(rainbowAlpha, 0f, 0.38f)

        return SoapFilmSample(
            color = rainbow,
            alpha = clampedAlpha,
            distortedRadius = distortedR,
        )
    }
}
