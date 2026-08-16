package com.awan.app.core.designsystem

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val SOAP_BUBBLE_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform float strength;
    uniform float3 baseColor;

    const float PI = 3.14159265359;
    const float TAU = 6.28318530718;

    // Monochromatic soap-film palette:
    // 4 distinct shades/tints of the same base color
    float3 soapPalette(float t, float3 base) {
        t = fract(t) * 4.0;
        if (t < 0.0) t += 4.0;

        float3 s0 = mix(base, float3(1.0), 0.45);
        float3 s1 = mix(base, float3(1.0), 0.20);
        float3 s2 = base;
        float3 s3 = mix(base, float3(0.0), 0.18);

        if (t < 1.0) {
            return mix(s0, s1, t);
        }

        if (t < 2.0) {
            return mix(s1, s2, t - 1.0);
        }

        if (t < 3.0) {
            return mix(s2, s3, t - 2.0);
        }

        return mix(s3, s0, t - 3.0);
    }

    half4 main(float2 fragCoord) {
        float minDim = min(resolution.x, resolution.y);

        // Normalize around the center.
        // Radius 1.0 represents the outer circle edge.
        float2 p =
            (fragCoord - resolution * 0.5)
            / (minDim * 0.5);

        float r = length(p);

        if (r > 1.0) {
            return half4(0.0);
        }

        float2 dir = p / max(r, 0.0001);
        float angle = atan(p.y, p.x);

        // Kotlin provides normalized time in the range 0..1.
        // TAU turns it into one full periodic mathematical cycle.
        float t = time * TAU;

        // ------------------------------------------------------------
        // ORGANIC FILM MOVEMENT
        //
        // Counter-moving waves avoid the appearance of a texture simply rotating.
        // ------------------------------------------------------------

        float organicWarp =
              0.010 * sin(angle * 3.0 + t)
            + 0.006 * sin(angle * 5.0 - t * 2.0)
            + 0.004 * sin(angle * 7.0 + t * 3.0);

        float warpedRadius = r + organicWarp;

        // Film thickness simulation.
        float thickness =
              warpedRadius * 23.0
            + 1.65 * sin(angle * 2.0 + sin(t))
            + 1.10 * sin(angle * 5.0 - cos(t * 2.0))
            + 0.80 * sin(
                angle * 3.0
                + warpedRadius * 7.0
                + sin(t * 3.0)
              );

        // Small gravitational bias.
        thickness += p.y * 1.20;

        float waveA =
            0.5 + 0.5 * sin(thickness);

        float waveB =
            0.5 + 0.5 * sin(
                thickness * 0.72
                + angle * 2.0
                - sin(t * 2.0)
            );

        float interference =
            waveA * 0.68 +
            waveB * 0.32;

        float patches =
            smoothstep(0.32, 0.95, interference);

        // ------------------------------------------------------------
        // EDGE MASK & GRADIENT BREAKPOINTS
        //
        // - Center (r = 0.0): 10% (0.10) opacity
        // - 30% in from edge (r = 0.70): 80% (0.80) opacity
        // - Outer edge (r = 1.00): 100% (1.00) full opacity
        // ------------------------------------------------------------

        float clampedRadius = clamp(warpedRadius, 0.0, 1.0);
        float radialAlpha = (clampedRadius < 0.70)
            ? mix(0.10, 0.80, clampedRadius / 0.70)
            : mix(0.80, 1.0, (clampedRadius - 0.70) / 0.30);

        float edgeMask = 1.0 - smoothstep(0.95, 1.0, warpedRadius);
        float filmRegion = radialAlpha * edgeMask;

        // Very thin physical bubble boundary.
        float thinRim =
            smoothstep(0.91, 0.975, r)
            *
            (
                1.0 -
                smoothstep(0.975, 1.0, r)
            );

        // ------------------------------------------------------------
        // GRAVITY VISIBILITY BIAS
        // ------------------------------------------------------------

        float gravity =
            smoothstep(
                -0.45,
                0.90,
                dir.y
            );

        float gravityBoost =
            mix(
                0.85,
                1.25,
                gravity
            );

        // ------------------------------------------------------------
        // MONOCHROMATIC IRIDESCENT COLOR
        // ------------------------------------------------------------

        float palettePhase =
            thickness / TAU
            + 0.08 * sin(angle * 4.0 + t)
            + 0.04 * sin(t * 2.0);

        float3 rainbow =
            soapPalette(palettePhase, baseColor);

        // ------------------------------------------------------------
        // WHITE SPECULAR CRESCENTS
        // ------------------------------------------------------------

        // Upper-left primary highlight.
        float2 highlightDir1 =
            normalize(float2(-0.68, -0.74));

        float highlight1 =
            pow(
                max(
                    dot(dir, highlightDir1),
                    0.0
                ),
                22.0
            );

        // Lower-right secondary highlight.
        float2 highlightDir2 =
            normalize(float2(0.70, 0.72));

        float highlight2 =
            pow(
                max(
                    dot(dir, highlightDir2),
                    0.0
                ),
                32.0
            );

        float highlightRadial =
            smoothstep(0.78, 0.93, r);

        float highlights =
            (
                highlight1 * 0.85 +
                highlight2 * 0.55
            )
            * highlightRadial;

        // ------------------------------------------------------------
        // FINAL ALPHA
        // ------------------------------------------------------------

        float rainbowAlpha =
            filmRegion
            *
            (
                0.012 +
                patches * 0.105
            )
            *
            gravityBoost;

        float rimAlpha =
            thinRim * 0.13;

        float highlightAlpha =
            highlights * 0.22;

        float alpha =
            (
                rainbowAlpha +
                rimAlpha +
                highlightAlpha
            )
            * strength;

        alpha =
            clamp(
                alpha,
                0.0,
                0.27
            );

        // Blend toward white at specular highlights.
        float specMix =
            clamp(
                highlights * 0.85,
                0.0,
                1.0
            );

        float3 color =
            mix(
                rainbow,
                float3(1.0),
                specMix
            );

        // Premultiplied RGB for transparent overlay.
        half a = half(alpha);
        half3 rgb = half3(color) * a;

        return half4(rgb, a);
    }
"""

/**
 * A flat 2D transparent soap-bubble overlay designed for Jetpack Compose UI.
 */
@Composable
fun SoapBubbleOverlay(
    modifier: Modifier = Modifier,
    baseColor: Color = SoapBubbleStyle.DEFAULT_BASE_COLOR,
    strength: Float = SoapBubbleStyle.DEFAULT_STRENGTH,
    cycleDurationMillis: Int = 12_000,
    isReducedMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()

        SoapBubbleShaderLayer(
            modifier = Modifier.matchParentSize(),
            baseColor = baseColor,
            strength = strength,
            cycleDurationMillis = cycleDurationMillis,
            isReducedMotion = isReducedMotion,
        )
    }
}

/**
 * Direct drawing layer for the Soap Bubble effect.
 *
 * Dispatches to:
 * - Android 13+ (API 33+): AGSL RuntimeShader
 * - Android 12 (API 31–32): Procedural Compose Canvas fallback
 * - Android 11 and below (API <= 30): Content rendered without decorative overlay
 */
@Composable
fun SoapBubbleShaderLayer(
    modifier: Modifier = Modifier,
    baseColor: Color = SoapBubbleStyle.DEFAULT_BASE_COLOR,
    strength: Float = SoapBubbleStyle.DEFAULT_STRENGTH,
    cycleDurationMillis: Int = 12_000,
    isReducedMotion: Boolean = false,
) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            SoapBubbleShaderLayerTiramisu(
                modifier = modifier,
                baseColor = baseColor,
                strength = strength,
                cycleDurationMillis = cycleDurationMillis,
                isReducedMotion = isReducedMotion,
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            SoapBubbleCanvasLayer(
                modifier = modifier,
                baseColor = baseColor,
                strength = strength,
                cycleDurationMillis = cycleDurationMillis,
                isReducedMotion = isReducedMotion,
            )
        }
        else -> {
            Box(modifier = modifier)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun SoapBubbleShaderLayerTiramisu(
    modifier: Modifier,
    baseColor: Color,
    strength: Float,
    cycleDurationMillis: Int,
    isReducedMotion: Boolean,
) {
    val shader = remember {
        RuntimeShader(SOAP_BUBBLE_SHADER)
    }

    val brush = remember(shader) {
        ShaderBrush(shader)
    }

    val transition = rememberInfiniteTransition(label = "SoapBubbleTransition")

    val time by if (isReducedMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.18f) }
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = cycleDurationMillis,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "SoapBubbleTime",
        )
    }

    Canvas(modifier = modifier) {
        shader.setFloatUniform(
            "resolution",
            size.width,
            size.height,
        )

        shader.setFloatUniform(
            "time",
            time,
        )

        shader.setFloatUniform(
            "strength",
            strength,
        )

        shader.setFloatUniform(
            "baseColor",
            baseColor.red,
            baseColor.green,
            baseColor.blue,
        )

        drawCircle(
            brush = brush,
            radius = size.minDimension / 2f,
        )
    }
}
