package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LABEL_MILLIS = 520
private const val FLIGHT_MILLIS = 620
private const val STAGGER_MILLIS = 90
private const val HOLD_AFTER_MILLIS = 600L
private const val FALLBACK_FADE_MILLIS = 220
private const val MIN_STARS = 3
private const val MAX_STARS = 6
private const val POINTS_PER_STAR = 5
private const val ARC_LIFT = 0.45f
private val StarSize = 22.dp

/**
 * Flies a points award from where it was earned to the points badge.
 *
 * A `+N` label pops at the origin, breaks into stars, and the stars arc one after another into the
 * badge — each arrival kicking the badge and ticking the running total up by its share, so the last
 * star lands on exactly [newTotal].
 *
 * When [targetBounds] is null the user is not on a screen that has a points badge. Rather than skip
 * the celebration, the overlay draws its own badge near the top of the screen, flies the stars into
 * that, and fades it away afterwards.
 */
@Composable
fun PointsFlightOverlay(
    amount: Int,
    newTotal: Int,
    originBounds: Rect?,
    targetBounds: Rect?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val starCount = remember(amount) { (amount / POINTS_PER_STAR).coerceIn(MIN_STARS, MAX_STARS) }

    // Stars land one at a time, so the count climbs in the same number of steps.
    var arrivedStars by remember { mutableIntStateOf(0) }
    val displayedTotal = if (arrivedStars >= starCount) {
        newTotal
    } else {
        newTotal - amount + (amount * arrivedStars / starCount)
    }

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var fallbackBounds by remember { mutableStateOf<Rect?>(null) }
    val usesFallback = targetBounds == null
    val resolvedTarget = targetBounds ?: fallbackBounds

    // Hand the running total to the badge for the duration of the flight, and hand it back after.
    // Published from a SideEffect rather than inline: the badge composes before this overlay does,
    // so writing the state during composition would be a backwards write into a frame that has
    // already read it.
    val anchors = LocalRewardAnchors.current
    SideEffect {
        anchors.animatedPoints = displayedTotal
        anchors.pointsPulse = arrivedStars in 1 until starCount
    }
    DisposableEffect(Unit) {
        onDispose {
            anchors.animatedPoints = null
            anchors.pointsPulse = false
        }
    }

    val labelProgress = remember { Animatable(0f) }
    val fallbackAlpha = remember { Animatable(0f) }
    val starProgress = remember(starCount) { List(starCount) { Animatable(0f) } }

    if (reduced) {
        // No flight, no particles — credit the total and get out of the way.
        LaunchedEffect(Unit) {
            arrivedStars = starCount
            delay(HOLD_AFTER_MILLIS)
            currentOnFinished()
        }
    } else {
        LaunchedEffect(starCount, resolvedTarget != null) {
            if (resolvedTarget == null) return@LaunchedEffect
            if (usesFallback) fallbackAlpha.animateTo(1f, tween(FALLBACK_FADE_MILLIS))
            labelProgress.animateTo(1f, tween(LABEL_MILLIS))
            starProgress.forEachIndexed { index, animatable ->
                launch {
                    delay(index.toLong() * STAGGER_MILLIS)
                    animatable.animateTo(1f, tween(FLIGHT_MILLIS, easing = LinearEasing))
                    arrivedStars += 1
                }
            }
            delay(
                (starCount - 1).toLong() * STAGGER_MILLIS + FLIGHT_MILLIS + HOLD_AFTER_MILLIS
            )
            if (usesFallback) fallbackAlpha.animateTo(0f, tween(FALLBACK_FADE_MILLIS))
            currentOnFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSize = it.size },
    ) {
        if (usesFallback) {
            AwanPointsBadge(
                pointsCount = displayedTotal,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .graphicsLayer { alpha = if (reduced) 1f else fallbackAlpha.value }
                    .onGloballyPositioned { fallbackBounds = it.boundsInRoot() },
            )
        }

        val target = resolvedTarget
        if (reduced || target == null) return@Box

        val originPoint = originBounds?.center
            ?: Offset(rootSize.width / 2f, rootSize.height / 2f)
        val targetPoint = target.center
        val starSizePx = with(LocalDensity.current) { StarSize.toPx() }

        // The label is the source of the stars, so it fades out as they take over.
        val labelAlpha = (1f - labelProgress.value * 1.6f).coerceIn(0f, 1f)
        if (labelAlpha > 0f) {
            AwanText(
                text = "+$amount",
                style = AwanTheme.typography.heading.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.pointsIcon,
                ),
                modifier = Modifier.graphicsLayer {
                    val scale = 0.6f + labelProgress.value * 0.7f
                    translationX = originPoint.x - size.width / 2f
                    translationY = originPoint.y - size.height / 2f - labelProgress.value * 28f
                    scaleX = scale
                    scaleY = scale
                    alpha = labelAlpha
                },
            )
        }

        starProgress.forEachIndexed { index, animatable ->
            val t = animatable.value
            if (t <= 0f || t >= 1f) return@forEachIndexed
            val control = bezierControl(originPoint, targetPoint, index, starCount)
            val point = quadraticBezier(originPoint, control, targetPoint, t)
            Icon(
                imageVector = Lucide.Star,
                contentDescription = null,
                tint = AwanTheme.colors.pointsIcon,
                modifier = Modifier
                    .size(StarSize)
                    .graphicsLayer {
                        translationX = point.x - starSizePx / 2f
                        translationY = point.y - starSizePx / 2f
                        val scale = 1f - t * 0.45f
                        scaleX = scale
                        scaleY = scale
                        rotationZ = t * 220f * if (index % 2 == 0) 1f else -1f
                        alpha = (1f - t * 0.25f).coerceIn(0f, 1f)
                    },
            )
        }
    }
}

/**
 * Bows each star's path away from the straight line, alternating sides so a burst of them reads as
 * a spray rather than a single stream.
 */
private fun bezierControl(from: Offset, to: Offset, index: Int, count: Int): Offset {
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    val spread = (index - (count - 1) / 2f) / count.coerceAtLeast(1)
    val distance = (to - from).getDistance()
    return Offset(
        x = mid.x + spread * distance * 0.5f,
        y = mid.y - distance * ARC_LIFT,
    )
}

/** Shared with [ItemFlightOverlay] — both fly something along an arc to an anchor. */
internal fun quadraticBezier(from: Offset, control: Offset, to: Offset, t: Float): Offset {
    val inverse = 1f - t
    return Offset(
        x = inverse * inverse * from.x + 2f * inverse * t * control.x + t * t * to.x,
        y = inverse * inverse * from.y + 2f * inverse * t * control.y + t * t * to.y,
    )
}
