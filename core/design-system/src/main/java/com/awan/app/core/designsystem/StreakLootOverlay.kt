package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

private const val SCRIM_ALPHA = 0.55f
private const val SCRIM_FADE_MILLIS = 220
private const val IGNITE_DELAY_MILLIS = 300L
private const val DISMISS_MILLIS = 3_200L
private const val DISMISS_BEST_MILLIS = 4_500L
private const val REDUCED_DISMISS_MILLIS = 1_500L

/** How dim the fire sits before it catches. */
private const val UNLIT_ALPHA = 0.35f

/** A frame with the fire fully formed, used as the static pose when motion is off. */
private const val LIT_FRAME = 0.45f

private val FlameSize = 220.dp

/**
 * The full-screen streak celebration: the flame catches, the number rolls up to its new value, and
 * a line of encouragement lands underneath.
 *
 * The fire is held at its first frame — where the artwork has barely formed — and dimmed, so the
 * ignite is that same asset coming up to full brightness and starting to play. No second "unlit"
 * image is needed, and there is no swap for the eye to catch.
 *
 * Beating a personal best gets its own headline and a burst of sparks over the same fire, so a
 * record reads as bigger than an ordinary day without needing a second animation.
 *
 * Dismisses on tap or on its own — a celebration the user has to dismiss twice stops being one.
 */
@Composable
fun StreakLootOverlay(
    oldValue: Int,
    newValue: Int,
    maxStreakBroken: Boolean,
    maxStreakNew: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val scrimAlpha = remember { Animatable(0f) }
    val ignite = remember { Animatable(0f) }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.streak_fire)
    )

    // Held at frame 0 until the ignite starts, so the loop never begins mid-cycle.
    val looping by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduced && ignite.value > 0f,
    )

    val igniteSpec = AwanTheme.motion.bouncy.spec<Float>()

    LaunchedEffect(reduced) {
        if (reduced) {
            scrimAlpha.snapTo(1f)
            ignite.snapTo(1f)
            delay(REDUCED_DISMISS_MILLIS)
            currentOnDismiss()
            return@LaunchedEffect
        }
        scrimAlpha.animateTo(1f, tween(SCRIM_FADE_MILLIS))
        delay(IGNITE_DELAY_MILLIS)
        ignite.animateTo(1f, igniteSpec)
        delay(if (maxStreakBroken) DISMISS_BEST_MILLIS else DISMISS_MILLIS)
        currentOnDismiss()
    }

    val colors = AwanTheme.colors
    val glowColor = if (maxStreakBroken) colors.zoneSun else colors.zoneTangerine

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA * scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { currentOnDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(FlameSize)
                        .drawBehind {
                            // Glow blooms with the ignite so the fire lights the scrim around it.
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.45f * ignite.value),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.7f,
                                ),
                                radius = size.minDimension * 0.7f,
                            )
                        }
                        .graphicsLayer {
                            // Overshoots then settles, so the flame catches rather than fades in.
                            val t = ignite.value
                            val scale = 0.8f + t * 0.25f - (t * t) * 0.05f
                            scaleX = scale
                            scaleY = scale
                            alpha = UNLIT_ALPHA + (1f - UNLIT_ALPHA) * t
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { if (reduced) LIT_FRAME else looping },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (maxStreakBroken) {
                    SparkleBurst(
                        celebrate = ignite.value > 0.5f,
                        modifier = Modifier.size(FlameSize),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StreakCounter(oldValue = oldValue, newValue = newValue, ignited = ignite.value > 0.4f)

            Spacer(modifier = Modifier.height(16.dp))

            AwanText(
                text = stringResource(
                    if (maxStreakBroken) R.string.ds_streak_best_headline
                    else R.string.ds_streak_headline
                ),
                style = AwanTheme.typography.title.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (maxStreakBroken) colors.zoneSun else Color.White,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(6.dp))

            AwanText(
                text = if (maxStreakBroken) {
                    stringResource(R.string.ds_streak_best_subtitle, maxStreakNew)
                } else {
                    motivationFor(newValue)
                },
                style = AwanTheme.typography.body.copy(
                    fontSize = 15.sp,
                    color = colors.sky,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }
    }
}

/** Rolls the streak number from its old value to its new one as the flame catches. */
@Composable
private fun StreakCounter(oldValue: Int, newValue: Int, ignited: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        AnimatedContent(
            targetState = if (ignited) newValue else oldValue,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height } + fadeOut())
            },
            label = "streakCount",
        ) { value ->
            AwanText(
                text = value.toString(),
                style = AwanTheme.typography.display.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.zoneSun,
                ),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AwanText(
            text = stringResource(R.string.ds_streak_days),
            style = AwanTheme.typography.heading.copy(
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.85f),
            ),
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

/**
 * Picks a line of encouragement by streak length, saturating at the last one so a long streak keeps
 * the strongest message rather than wrapping back round to "day one".
 */
@Composable
private fun motivationFor(streak: Int): String {
    val lines = stringArrayResource(R.array.ds_streak_motivations)
    return lines[(streak - 1).coerceIn(0, lines.lastIndex)]
}
