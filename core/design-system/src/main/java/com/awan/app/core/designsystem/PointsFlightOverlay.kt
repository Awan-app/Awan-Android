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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.Zap

private const val LABEL_MILLIS = 520
private const val FLIGHT_MILLIS = 620
private const val STAGGER_MILLIS = 90
private const val COMBO_STAGGER_MILLIS = 45L
private const val HOLD_AFTER_MILLIS = 600L
private const val FALLBACK_FADE_MILLIS = 220
private const val MIN_STARS = 4
private const val MAX_STARS = 8
private const val POINTS_PER_STAR = 5
private const val ARC_LIFT = 0.45f
private val StarSize = 22.dp
private val ComboStarSize = 24.dp

/**
 * Flies a points award from where it was earned to the points badge.
 *
 * Supports single-task rewards and powerful multi-task combo celebrations ([comboCount] > 1),
 * spawning a dense spray of glowing stars and a combo bonus badge.
 */
@Composable
fun PointsFlightOverlay(
    amount: Int,
    newTotal: Int,
    comboCount: Int = 1,
    originBounds: Rect?,
    targetBounds: Rect?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val isCombo = comboCount > 1
    val starCount = remember(amount, comboCount) {
        when {
            comboCount >= 3 -> 20
            comboCount == 2 -> 14
            else -> (amount / POINTS_PER_STAR).coerceIn(MIN_STARS, MAX_STARS)
        }
    }
    val currentStarSize = if (isCombo) ComboStarSize else StarSize
    val staggerMillis = if (isCombo) COMBO_STAGGER_MILLIS else STAGGER_MILLIS.toLong()

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
    val shockwaveProgress = remember { Animatable(0f) }
    val fallbackAlpha = remember { Animatable(0f) }
    val starProgress = remember(starCount) { List(starCount) { Animatable(0f) } }

    if (reduced) {
        LaunchedEffect(Unit) {
            arrivedStars = starCount
            delay(HOLD_AFTER_MILLIS)
            currentOnFinished()
        }
    } else {
        LaunchedEffect(starCount, resolvedTarget != null) {
            if (resolvedTarget == null) return@LaunchedEffect
            if (usesFallback) fallbackAlpha.animateTo(1f, tween(FALLBACK_FADE_MILLIS))
            launch { shockwaveProgress.animateTo(1f, tween(450)) }
            labelProgress.animateTo(1f, tween(LABEL_MILLIS))
            starProgress.forEachIndexed { index, animatable ->
                launch {
                    delay(index.toLong() * staggerMillis)
                    animatable.animateTo(1f, tween(FLIGHT_MILLIS, easing = LinearEasing))
                    arrivedStars += 1
                }
            }
            delay(
                (starCount - 1).toLong() * staggerMillis + FLIGHT_MILLIS + HOLD_AFTER_MILLIS
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
        val starSizePx = with(LocalDensity.current) { currentStarSize.toPx() }

        // Explosive shockwave ring at origin for impact
        val shockAlpha = (1f - shockwaveProgress.value).coerceIn(0f, 1f)
        if (shockAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(if (isCombo) 110.dp else 70.dp)
                    .graphicsLayer {
                        val scale = 0.3f + shockwaveProgress.value * (if (isCombo) 2.2f else 1.5f)
                        translationX = originPoint.x - size.width / 2f
                        translationY = originPoint.y - size.height / 2f
                        scaleX = scale
                        scaleY = scale
                        alpha = shockAlpha * 0.7f
                    }
                    .clip(RoundedCornerShape(100))
                    .background(AwanTheme.colors.pointsIcon.copy(alpha = 0.35f))
                    .border(2.dp, AwanTheme.colors.pointsIcon, RoundedCornerShape(100))
            )
        }

        // The label is the source of the stars, so it fades out as they take over.
        val labelAlpha = (1f - labelProgress.value * 1.5f).coerceIn(0f, 1f)
        if (labelAlpha > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val scale = if (isCombo) {
                        0.7f + labelProgress.value * 0.7f
                    } else {
                        0.6f + labelProgress.value * 0.6f
                    }
                    translationX = originPoint.x - size.width / 2f
                    translationY = originPoint.y - size.height / 2f - labelProgress.value * 32f
                    scaleX = scale
                    scaleY = scale
                    alpha = labelAlpha
                }
            ) {
                if (isCombo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .background(AwanTheme.colors.pointsIcon, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Zap,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        AwanText(
                            text = "$comboCount" + "x COMBO BONUS!",
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                            )
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                }

                AwanText(
                    text = "+$amount",
                    style = AwanTheme.typography.heading.copy(
                        fontSize = if (isCombo) 32.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AwanTheme.colors.pointsIcon,
                    ),
                )
            }
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
                    .size(currentStarSize)
                    .graphicsLayer {
                        translationX = point.x - starSizePx / 2f
                        translationY = point.y - starSizePx / 2f
                        val scale = (1.2f - t * 0.55f).coerceAtLeast(0.4f)
                        scaleX = scale
                        scaleY = scale
                        rotationZ = t * 360f * if (index % 2 == 0) 1f else -1f
                        alpha = (1f - t * 0.2f).coerceIn(0f, 1f)
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
