package com.awan.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AwanCloudsFooter(
    modifier: Modifier = Modifier,
    title: String = "Next Sky Level Locked",
    subtitle: String = "Complete today's sessions to unlock tomorrow",
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloud_float_y",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // 1. Layered Canvas Animated Cloud Horizon
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val width = size.width
            val height = size.height

            // Background soft blue cloud layer
            val bgPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.45f)
                cubicTo(
                    width * 0.2f, height * 0.25f + floatY,
                    width * 0.35f, height * 0.55f - floatY,
                    width * 0.55f, height * 0.35f + floatY,
                )
                cubicTo(
                    width * 0.75f, height * 0.15f - floatY,
                    width * 0.9f, height * 0.45f + floatY,
                    width, height * 0.35f,
                )
                lineTo(width, height)
                close()
            }
            drawPath(
                path = bgPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDBEAFE).copy(alpha = 0.65f),
                        Color(0xFFEEF2FF).copy(alpha = 0.9f),
                    ),
                ),
            )

            // Foreground white cloud puffs layer
            val fgPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.6f)
                cubicTo(
                    width * 0.15f, height * 0.35f - floatY,
                    width * 0.35f, height * 0.65f + floatY,
                    width * 0.5f, height * 0.45f - floatY,
                )
                cubicTo(
                    width * 0.7f, height * 0.25f + floatY,
                    width * 0.85f, height * 0.6f - floatY,
                    width, height * 0.5f,
                )
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fgPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFF1F5F9),
                    ),
                ),
            )
        }

        // 2. Center Glassmorphic Locked Level Pill Badge
        val badgeShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .offset(y = (-18).dp + floatY.dp)
                .shadow(8.dp, badgeShape, spotColor = Color(0xFF6366F1))
                .clip(badgeShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF).copy(alpha = 0.95f),
                            Color(0xFFF8FAFC).copy(alpha = 0.98f),
                        ),
                    ),
                )
                .border(1.5.dp, Color(0xFFC7D2FE), badgeShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Lock Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color(0xFFC7D2FE), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = "🔒",
                        style = AwanTheme.typography.body.copy(fontSize = 16.sp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AwanText(
                            text = title,
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF312E81),
                            ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Sparkle icon
                        AwanText(
                            text = "✨",
                            style = AwanTheme.typography.caption.copy(fontSize = 12.sp),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    AwanText(
                        text = subtitle,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                        ),
                    )
                }
            }
        }
    }
}
