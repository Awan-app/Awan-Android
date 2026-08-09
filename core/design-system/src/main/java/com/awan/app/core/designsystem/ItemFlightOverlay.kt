package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val REVEAL_MILLIS = 700
private const val HOLD_MILLIS = 900L
private const val FLIGHT_MILLIS = 700
private const val ARC_LIFT = 0.35f
private val ItemSize = 128.dp

/**
 * Shows a won item, then flies it into the profile tab where the user's things live.
 *
 * The flight is what tells the user *where the item went* — without it a wheel item win is just a
 * card that disappears. If the profile tab is not on screen the flight has no destination, so the
 * reveal simply fades instead of flying somewhere arbitrary.
 */
@Composable
fun ItemFlightOverlay(
    name: String,
    imageUrl: String?,
    targetBounds: Rect?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnFinished by rememberUpdatedState(onFinished)

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val reveal = remember { Animatable(0f) }
    val flight = remember { Animatable(0f) }
    val revealSpec = AwanTheme.motion.bouncy.spec<Float>()

    LaunchedEffect(reduced, targetBounds != null) {
        if (reduced) {
            reveal.snapTo(1f)
            delay(HOLD_MILLIS)
            currentOnFinished()
            return@LaunchedEffect
        }
        reveal.animateTo(1f, revealSpec)
        delay(HOLD_MILLIS)
        if (targetBounds != null) {
            flight.animateTo(1f, tween(FLIGHT_MILLIS, easing = FastOutSlowInEasing))
        } else {
            reveal.animateTo(0f, tween(REVEAL_MILLIS))
        }
        currentOnFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSize = it.size },
        contentAlignment = Alignment.Center,
    ) {
        val start = Offset(rootSize.width / 2f, rootSize.height / 2f)
        val end = targetBounds?.center ?: start
        val t = flight.value
        val control = Offset(
            x = (start.x + end.x) / 2f,
            y = (start.y + end.y) / 2f - (end - start).getDistance() * ARC_LIFT,
        )
        val point = quadraticBezier(start, control, end, t)

        Box(
            modifier = Modifier
                .size(ItemSize)
                .graphicsLayer {
                    translationX = point.x - start.x
                    translationY = point.y - start.y
                    val scale = reveal.value * (1f - t * 0.75f)
                    scaleX = scale
                    scaleY = scale
                    alpha = reveal.value * (1f - t * 0.35f)
                },
            contentAlignment = Alignment.Center,
        ) {
            SparkleBurst(celebrate = reveal.value > 0.5f && t <= 0f)
            AwanRemoteImage(
                url = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AwanTheme.colors.surface),
            )
        }
    }
}
