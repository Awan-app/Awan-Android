package com.awan.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex


@Composable
fun AwanCloud(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    baseColor: Color = Color.White,
    shadeColor: Color = Color(0xFFC7D2FE),
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val leftRadius = w * 0.22f
        val leftCenter = Offset(w * 0.25f, h * 0.58f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.95f), shadeColor.copy(alpha = 0.6f)),
                center = Offset(leftCenter.x - leftRadius * 0.3f, leftCenter.y - leftRadius * 0.3f),
                radius = leftRadius * 1.3f,
            ),
            radius = leftRadius,
            center = leftCenter,
        )

        val rightRadius = w * 0.24f
        val rightCenter = Offset(w * 0.75f, h * 0.58f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.95f), shadeColor.copy(alpha = 0.65f)),
                center = Offset(rightCenter.x - rightRadius * 0.3f, rightCenter.y - rightRadius * 0.3f),
                radius = rightRadius * 1.3f,
            ),
            radius = rightRadius,
            center = rightCenter,
        )

        val topRightRadius = w * 0.27f
        val topRightCenter = Offset(w * 0.62f, h * 0.38f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.98f), shadeColor.copy(alpha = 0.55f)),
                center = Offset(topRightCenter.x - topRightRadius * 0.3f, topRightCenter.y - topRightRadius * 0.3f),
                radius = topRightRadius * 1.3f,
            ),
            radius = topRightRadius,
            center = topRightCenter,
        )

        val mainRadius = w * 0.32f
        val mainCenter = Offset(w * 0.40f, h * 0.45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.98f), shadeColor.copy(alpha = 0.5f)),
                center = Offset(mainCenter.x - mainRadius * 0.35f, mainCenter.y - mainRadius * 0.35f),
                radius = mainRadius * 1.35f,
            ),
            radius = mainRadius,
            center = mainCenter,
        )

        val bottomRadius = w * 0.30f
        val bottomCenter = Offset(w * 0.50f, h * 0.68f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.92f), shadeColor.copy(alpha = 0.7f)),
                center = Offset(bottomCenter.x - bottomRadius * 0.2f, bottomCenter.y - bottomRadius * 0.4f),
                radius = bottomRadius * 1.4f,
            ),
            radius = bottomRadius,
            center = bottomCenter,
        )
    }
}


@Composable
fun AwanCloudsHorizon(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds_horizon_float")

    val floatY1 by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloud1_y",
    )

    val floatY2 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloud2_y",
    )

    val floatY3 by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloud3_y",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black,
                        ),
                        startY = 0f,
                        endY = size.height * 0.40f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        AwanCloud(
            size = 140.dp,
            baseColor = Color(0xFFF1F5F9),
            shadeColor = Color(0xFFCBD5E1),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = (10 + floatY2).dp),
        )

        AwanCloud(
            size = 150.dp,
            baseColor = Color(0xFFF8FAFC),
            shadeColor = Color(0xFFC7D2FE),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 25.dp, y = (5 + floatY3).dp),
        )

        AwanCloud(
            size = 160.dp,
            baseColor = Color.White,
            shadeColor = Color(0xFFCBD5E1),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-50).dp, y = floatY1.dp),
        )

        AwanCloud(
            size = 145.dp,
            baseColor = Color.White,
            shadeColor = Color(0xFFC7D2FE),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 60.dp, y = (- floatY1).dp),
        )
    }
}
