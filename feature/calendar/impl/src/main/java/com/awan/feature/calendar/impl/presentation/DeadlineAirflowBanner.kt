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
        // Matching SoapBubbleStyle.UrgentRed (#EF4444), MidAmber (#F59E0B), CalmEmerald (#10B981)
        float3 red = float3(0.937, 0.267, 0.267);    // #EF4444 (due soon / urgent)
        float3 amber = float3(0.961, 0.620, 0.043);  // #F59E0B (halfway)
        float3 green = float3(0.063, 0.725, 0.506);  // #10B981 (open sky / plenty of time)

        float3 baseCol = (x < 0.5) 
            ? mix(red, amber, x * 2.0) 
            : mix(amber, green, (x - 0.5) * 2.0);

        float pi2x = x * 6.28318530718;
        float pi4x = x * 12.5663706144;

        // -------------------------------------------------------------------
        // 7 Curated Weaving Streams — exact match with AirflowCanvasFallback
        // -------------------------------------------------------------------
        // 1. Primary bold spine ribbon: baseY = 0.50, amp1 = 0.15, f1 = 1, amp2 = 0.035, f2 = 2, speed = 1, phase = 0.0
        float w1 = 0.50 + 0.15 * sin(pi2x - time * 1.0) + 0.035 * cos(pi4x + time * 1.0);

        // 2. Ultra-fine hairline companion: baseY = 0.50, amp1 = 0.12, f1 = 1, amp2 = 0.045, f2 = 2, speed = 2, phase = 0.9
        float w2 = 0.50 + 0.12 * sin(pi2x - time * 2.0 + 0.9) + 0.045 * cos(pi4x + time * 2.0 + 0.9);

        // 3. Medium upper weave: baseY = 0.44, amp1 = 0.17, f1 = 1, amp2 = 0.030, f2 = 2, speed = 1, phase = 2.0
        float w3 = 0.44 + 0.17 * sin(pi2x - time * 1.0 + 2.0) + 0.030 * cos(pi4x + time * 1.0 + 2.0);

        // 4. Fine lower hairline: baseY = 0.56, amp1 = 0.16, f1 = 1, amp2 = 0.030, f2 = 2, speed = 1, phase = 4.1
        float w4 = 0.56 + 0.16 * sin(pi2x - time * 1.0 + 4.1) + 0.030 * cos(pi4x + time * 1.0 + 4.1);

        // 5. Broad soft flowing ribbon: baseY = 0.52, amp1 = 0.14, f1 = 1, amp2 = 0.050, f2 = 1, speed = 2, phase = 3.2
        float w5 = 0.52 + 0.14 * sin(pi2x - time * 2.0 + 3.2) + 0.050 * cos(pi2x + time * 2.0 + 3.2);

        // 6. Medium crest stream: baseY = 0.40, amp1 = 0.15, f1 = 1, amp2 = 0.040, f2 = 1, speed = 2, phase = 1.5
        float w6 = 0.40 + 0.15 * sin(pi2x - time * 2.0 + 1.5) + 0.040 * cos(pi2x + time * 2.0 + 1.5);

        // 7. Dynamic fine swoop thread: baseY = 0.50, amp1 = 0.19, f1 = 1, amp2 = 0.035, f2 = 2, speed = 1, phase = 1.3
        float w7 = 0.50 + 0.19 * sin(pi2x - time * 1.0 + 1.3) + 0.035 * cos(pi4x + time * 1.0 + 1.3);

        // 1 dp in pixels for 130dp banner height
        float dp = resolution.y / 130.0;

        float d1 = abs(fragCoord.y - w1 * resolution.y);
        float d2 = abs(fragCoord.y - w2 * resolution.y);
        float d3 = abs(fragCoord.y - w3 * resolution.y);
        float d4 = abs(fragCoord.y - w4 * resolution.y);
        float d5 = abs(fragCoord.y - w5 * resolution.y);
        float d6 = abs(fragCoord.y - w6 * resolution.y);
        float d7 = abs(fragCoord.y - w7 * resolution.y);

        // Thread stroke half-widths and alphas matching AirflowCanvasFallback exactly
        // 1. sWidth = 3.6 dp, alpha = 0.38
        float hw1 = 1.80 * dp;
        float stream1 = smoothstep(hw1 + 1.0, hw1 - 1.0, d1) * 0.38;

        // 2. sWidth = 1.0 dp, alpha = 0.30
        float hw2 = 0.50 * dp;
        float stream2 = smoothstep(hw2 + 1.0, hw2 - 1.0, d2) * 0.30;

        // 3. sWidth = 2.2 dp, alpha = 0.28
        float hw3 = 1.10 * dp;
        float stream3 = smoothstep(hw3 + 1.0, hw3 - 1.0, d3) * 0.28;

        // 4. sWidth = 1.2 dp, alpha = 0.26
        float hw4 = 0.60 * dp;
        float stream4 = smoothstep(hw4 + 1.0, hw4 - 1.0, d4) * 0.26;

        // 5. sWidth = 3.0 dp, alpha = 0.22
        float hw5 = 1.50 * dp;
        float stream5 = smoothstep(hw5 + 1.0, hw5 - 1.0, d5) * 0.22;

        // 6. sWidth = 2.0 dp, alpha = 0.25
        float hw6 = 1.00 * dp;
        float stream6 = smoothstep(hw6 + 1.0, hw6 - 1.0, d6) * 0.25;

        // 7. sWidth = 1.4 dp, alpha = 0.28
        float hw7 = 0.70 * dp;
        float stream7 = smoothstep(hw7 + 1.0, hw7 - 1.0, d7) * 0.28;

        // Background ambient gradient matching bgGradientBrush (alpha = 0.07)
        float bgAlpha = 0.07;

        float streamAlpha = stream1 + stream2 + stream3 + stream4 + stream5 + stream6 + stream7;
        float totalAlpha = clamp(bgAlpha + streamAlpha, 0.0, 1.0) * strength;

        // Edge fades
        float edgeX = smoothstep(0.0, 0.02, x) * smoothstep(1.0, 0.98, x);
        float edgeY = smoothstep(0.0, 0.03, y) * smoothstep(1.0, 0.97, y);
        float alpha = totalAlpha * edgeX * edgeY;

        half a = half(alpha);
        half3 rgb = half3(baseCol) * a;

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
