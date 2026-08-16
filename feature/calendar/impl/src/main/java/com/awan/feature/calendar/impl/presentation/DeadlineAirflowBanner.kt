package com.awan.feature.calendar.impl.presentation

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.SoapBubbleStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val AIRFLOW_AGSL_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform float strength;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        float x = clamp(uv.x, 0.0, 1.0);
        float y = uv.y;

        // Base gradient from Left (Red) -> Center (Amber) -> Right (Green)
        float3 red = float3(0.937, 0.267, 0.267);    // #EF4444 (due soon / urgent)
        float3 amber = float3(0.961, 0.620, 0.043);  // #F59E0B (halfway)
        float3 green = float3(0.063, 0.725, 0.506);  // #10B981 (open sky / plenty of time)

        float3 baseCol = (x < 0.5) 
            ? mix(red, amber, x * 2.0) 
            : mix(amber, green, (x - 0.5) * 2.0);

        float3 highlightCol = mix(baseCol, float3(1.0), 0.55);

        // Strict integer harmonics for a 100% seamless, mathematically continuous loop
        float t1 = time * 1.0;
        float t2 = time * 2.0;

        float pi2x = x * 6.28318530718;
        float pi4x = x * 12.5663706144;

        // -------------------------------------------------------------------
        // 7 Curated Weaving Streams (Organized Fluid Corridor)
        // -------------------------------------------------------------------
        // 1. Primary bold spine ribbon (slow, central undulating backbone)
        float w1 = 0.50 + 0.15 * sin(pi2x - t1) + 0.035 * cos(pi4x + t2);

        // 2. Ultra-fine hairline companion (tightly orbiting & crossing the spine)
        float w2 = 0.50 + 0.12 * sin(pi2x - t2 + 0.9) + 0.045 * sin(pi4x - t1);

        // 3. Medium upper weave (drifts slightly above, swooping across at intervals)
        float w3 = 0.44 + 0.17 * sin(pi2x - t1 + 2.0) + 0.030 * cos(pi4x - t2);

        // 4. Hairline lower weave (drifts slightly below with independent phase)
        float w4 = 0.56 + 0.16 * sin(pi2x - t1 + 4.1) + 0.030 * sin(pi4x + t1);

        // 5. Broad soft flowing ribbon (soft lower-mid accent)
        float w5 = 0.52 + 0.14 * sin(pi2x - t2 + 3.2) + 0.050 * cos(pi2x + t1);

        // 6. Medium upper cresting ribbon (graceful crest above center)
        float w6 = 0.40 + 0.15 * sin(pi2x - t2 + 1.5) + 0.040 * sin(pi2x + t1);

        // 7. Dynamic swoop thread (traverses across the entire bundle from top to bottom)
        float w7 = 0.50 + 0.19 * sin(pi2x - t1 + 1.3) - 0.035 * sin(pi4x + t2);

        // Distances from pixel y to thread curves
        float d1 = abs(y - w1);
        float d2 = abs(y - w2);
        float d3 = abs(y - w3);
        float d4 = abs(y - w4);
        float d5 = abs(y - w5);
        float d6 = abs(y - w6);
        float d7 = abs(y - w7);

        // Varied thread thicknesses (bold ribbon, fine hairline, medium stream)
        float core1 = exp(-d1 * d1 * 480.0)  * 0.36; // Bold central ribbon
        float core2 = exp(-d2 * d2 * 1600.0) * 0.32; // Ultra-fine crisp hairline
        float core3 = exp(-d3 * d3 * 750.0)  * 0.28; // Medium upper ribbon
        float core4 = exp(-d4 * d4 * 1400.0) * 0.26; // Fine lower hairline
        float core5 = exp(-d5 * d5 * 380.0)  * 0.24; // Broad soft flow
        float core6 = exp(-d6 * d6 * 820.0)  * 0.25; // Medium crest stream
        float core7 = exp(-d7 * d7 * 1100.0) * 0.28; // Fine dynamic swoop

        float totalCores = core1 + core2 + core3 + core4 + core5 + core6 + core7;

        // Soft ambient atmospheric glow around the shared stream envelope
        float glow = (exp(-d1 * 22.0) + exp(-d3 * 18.0) + exp(-d5 * 18.0) + exp(-d6 * 18.0)) * 0.042;

        // Gentle crest shimmer
        float crest = pow(max(0.0, sin(pi2x - t1)), 4.0) * exp(-d1 * d1 * 350.0) * 0.28;

        float intensity = (totalCores + glow + crest) * strength;

        // Edge fades
        float edgeX = smoothstep(0.0, 0.05, x) * smoothstep(1.0, 0.95, x);
        float edgeY = smoothstep(0.0, 0.08, y) * smoothstep(1.0, 0.92, y);
        float alpha = clamp(intensity * edgeX * edgeY, 0.0, 0.45);

        float3 finalRgb = mix(baseCol, highlightCol, clamp(crest * 2.2, 0.0, 1.0));

        half a = half(alpha);
        half3 rgb = half3(finalRgb) * a;

        return half4(rgb, a);
    }
"""

@Composable
fun DeadlineAirflowBanner(
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    val colors = AwanTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line.copy(alpha = 0.60f), shape)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AirflowShaderTiramisu(
                modifier = Modifier.fillMaxSize(),
                isReducedMotion = isReducedMotion,
            )
        } else {
            AirflowCanvasFallback(
                modifier = Modifier.fillMaxSize(),
                isReducedMotion = isReducedMotion,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AirflowShaderTiramisu(
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
) {
    val shader = remember { RuntimeShader(AIRFLOW_AGSL_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }

    val infiniteTransition = rememberInfiniteTransition(label = "AirflowShaderTransition")
    val animatedTime by if (isReducedMotion) {
        remember { mutableFloatStateOf(0.5f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "AirflowShaderTime",
        )
    }

    Canvas(modifier = modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", animatedTime * (2f * PI.toFloat()))
        shader.setFloatUniform("strength", 1.0f)
        drawRect(brush = brush)
    }
}

@Composable
private fun AirflowCanvasFallback(
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AirflowCanvasTransition")
    val animatedTime by if (isReducedMotion) {
        remember { mutableFloatStateOf(0.5f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "AirflowCanvasTime",
        )
    }

    val gradientBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                SoapBubbleStyle.UrgentRed,
                SoapBubbleStyle.MidAmber,
                SoapBubbleStyle.CalmEmerald,
            )
        )
    }

    val bgGradientBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                SoapBubbleStyle.UrgentRed.copy(alpha = 0.07f),
                SoapBubbleStyle.MidAmber.copy(alpha = 0.07f),
                SoapBubbleStyle.CalmEmerald.copy(alpha = 0.07f),
            )
        )
    }

    Canvas(modifier = modifier) {
        drawRect(brush = bgGradientBrush)

        val width = size.width
        val height = size.height
        val timeRad = animatedTime * (2f * PI.toFloat())

        // 1. Primary bold spine ribbon (thick, prominent flow)
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.50f, amp1 = 0.15f, f1 = 1f, amp2 = 0.035f, f2 = 2f, speed = 1f, sWidth = 3.6.dp.toPx(), alpha = 0.38f)

        // 2. Ultra-fine hairline companion (tightly orbiting & crossing the spine)
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.50f, amp1 = 0.12f, f1 = 1f, amp2 = 0.045f, f2 = 2f, speed = 2f, sWidth = 1.0.dp.toPx(), alpha = 0.30f, phaseOffset = 0.9f)

        // 3. Medium upper weave
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.44f, amp1 = 0.17f, f1 = 1f, amp2 = 0.030f, f2 = 2f, speed = 1f, sWidth = 2.2.dp.toPx(), alpha = 0.28f, phaseOffset = 2.0f)

        // 4. Fine lower hairline weave
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.56f, amp1 = 0.16f, f1 = 1f, amp2 = 0.030f, f2 = 2f, speed = 1f, sWidth = 1.2.dp.toPx(), alpha = 0.26f, phaseOffset = 4.1f)

        // 5. Broad soft flowing ribbon
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.52f, amp1 = 0.14f, f1 = 1f, amp2 = 0.050f, f2 = 1f, speed = 2f, sWidth = 3.0.dp.toPx(), alpha = 0.22f, phaseOffset = 3.2f)

        // 6. Medium crest stream
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.40f, amp1 = 0.15f, f1 = 1f, amp2 = 0.040f, f2 = 1f, speed = 2f, sWidth = 2.0.dp.toPx(), alpha = 0.25f, phaseOffset = 1.5f)

        // 7. Dynamic fine swoop thread
        drawWaveStream(width, height, gradientBrush, timeRad, baseY = 0.50f, amp1 = 0.19f, f1 = 1f, amp2 = 0.035f, f2 = 2f, speed = 1f, sWidth = 1.4.dp.toPx(), alpha = 0.28f, phaseOffset = 1.3f)
    }
}

private fun DrawScope.drawWaveStream(
    width: Float,
    height: Float,
    brush: Brush,
    timeRad: Float,
    baseY: Float,
    amp1: Float,
    f1: Float,
    amp2: Float,
    f2: Float,
    speed: Float,
    sWidth: Float,
    alpha: Float,
    phaseOffset: Float = 0f,
) {
    val path = Path()
    val steps = 80
    for (i in 0..steps) {
        val frac = i.toFloat() / steps
        val x = frac * width
        val angle1 = (frac * f1 * 2f * PI.toFloat()) - (timeRad * speed) + phaseOffset
        val angle2 = (frac * f2 * 2f * PI.toFloat()) + (timeRad * speed) + phaseOffset
        val yFrac = baseY + amp1 * sin(angle1) + amp2 * cos(angle2)
        val y = yFrac * height

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        brush = brush,
        alpha = alpha,
        style = Stroke(
            width = sWidth,
            cap = StrokeCap.Round,
        ),
    )
}
