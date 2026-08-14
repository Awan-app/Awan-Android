package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val SCRIM_ALPHA = 0.75f
private const val SCRIM_FADE_MILLIS = 300
private const val CELEBRATION_DURATION = 4000L

@Composable
fun GoalAchievementOverlay(
    goalTitle: String,
    goalEmoji: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val colors = AwanTheme.colors

    val scrimAlpha = remember { Animatable(0f) }
    val entryProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduced) {
            scrimAlpha.snapTo(1f)
            entryProgress.snapTo(1f)
            delay(2000)
            currentOnDismiss()
            return@LaunchedEffect
        }
        
        scrimAlpha.animateTo(1f, tween(SCRIM_FADE_MILLIS))
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(CELEBRATION_DURATION)
        scrimAlpha.animateTo(0f, tween(SCRIM_FADE_MILLIS))
        currentOnDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA * scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { currentOnDismiss() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = entryProgress.value
                val s = 0.8f + 0.2f * entryProgress.value
                scaleX = s
                scaleY = s
                translationY = (1f - entryProgress.value) * 100f
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(entryProgress.value * 1.5f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.zoneSun.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // The Mascot
                AwanMascot(
                    expression = MascotExpression.Celebrate,
                    width = 180.dp
                )

                // Sparkles
                SparkleBurst(
                    celebrate = entryProgress.value > 0.7f,
                    modifier = Modifier.size(240.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Goal Info
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                AwanText(
                    text = goalEmoji,
                    style = AwanTheme.typography.display.copy(fontSize = 32.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AwanText(
                text = "Goal Achieved!",
                style = AwanTheme.typography.display.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.zoneSun,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            AwanText(
                text = goalTitle,
                style = AwanTheme.typography.title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AwanText(
                text = "You're making amazing progress!",
                style = AwanTheme.typography.body.copy(
                    color = colors.sky,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
