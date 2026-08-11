package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.X
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calculates the landing rotation that parks wedge [index] under the top pointer (-90°).
 */
private fun landingRotation(current: Float, index: Int, count: Int): Float {
    if (count <= 0) return current
    val sweep = 360f / count
    val aligned = -(index * sweep + sweep / 2f)
    val minimum = current + LANDING_TURNS * 360f
    return aligned + ceil((minimum - aligned) / 360f) * 360f
}

private const val SCRIM_ALPHA = 0.85f
private const val FREE_TURN_MILLIS = 650
private const val LANDING_MILLIS = 2_400
private const val LANDING_TURNS = 4
private const val POINTER_ANGLE_DEG = -90f

private val WheelCanvasSize = 310.dp

@Immutable
data class WheelSegmentUi(
    val id: String,
    val label: String,
    val isItem: Boolean,
)

/**
 * 2D Cartoon Happy Daily Gift Spin Wheel.
 *
 * Flow:
 * 1. Spin Available: Title "Daily Gift", Subtitle "Spin the wheel and reveal your reward", Wheel with Sparkles center cap, Bottom text "Tap the wheel to spin".
 * 2. Immediate Winning Victory State (Right after spin lands): Full Victory Overlay with Big Star Badge, "You won!", Prize text, and "✓ Awesome!" button.
 * 3. Already Claimed State (Opened later after claiming): Wheel with Lock center cap, Subtitle "Come back tomorrow for another gift", Bottom text "Today's gift is claimed" & "See you tomorrow!".
 */
@Composable
fun AwanWheelOverlay(
    segments: List<WheelSegmentUi>,
    landingSegmentId: String?,
    resultText: String?,
    isSpinning: Boolean,
    canSpin: Boolean,
    onSpin: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnClose by rememberUpdatedState(onClose)
    val haptics = LocalHapticFeedback.current

    val rotation = remember { Animatable(0f) }
    var landed by remember { mutableStateOf(false) }
    var wasFreeSpinning by remember { mutableStateOf(false) }

    var lastPegIndex by remember { mutableStateOf(-1) }
    val pointerRecoil = remember { Animatable(0f) }

    val segmentCount = segments.size.coerceAtLeast(1)
    val sweep = 360f / segmentCount

    // Dynamic mechanical pointer recoil during wheel rotation
    LaunchedEffect(rotation.value, isSpinning) {
        if (!reduced && segmentCount > 0) {
            val adjustedAngle = (rotation.value % 360f + 360f) % 360f
            val currentPeg = (adjustedAngle / sweep).toInt()
            if (currentPeg != lastPegIndex && lastPegIndex != -1) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                pointerRecoil.snapTo(-12f)
                pointerRecoil.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 600f))
            }
            lastPegIndex = currentPeg
        }
    }

    LaunchedEffect(landingSegmentId, isSpinning, segments.size) {
        val landing = landingSegmentId
        val index = landing?.let { id -> segments.indexOfFirst { it.id == id } } ?: -1
        when {
            index >= 0 -> {
                wasFreeSpinning = false
                val target = landingRotation(rotation.value, index, segments.size)
                if (reduced) {
                    rotation.snapTo(target)
                } else {
                    rotation.animateTo(
                        targetValue = target,
                        animationSpec = tween(LANDING_MILLIS, easing = LinearOutSlowInEasing),
                    )
                }
                landed = true
            }

            isSpinning && !reduced -> {
                wasFreeSpinning = true
                while (true) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(FREE_TURN_MILLIS, easing = LinearEasing),
                    )
                }
            }

            wasFreeSpinning -> {
                wasFreeSpinning = false
                if (!reduced) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(LANDING_MILLIS, easing = LinearOutSlowInEasing),
                    )
                }
                landed = landing != null
            }
        }
    }

    val isWin = landingSegmentId != null
    val showResult = resultText != null && (landed || !isWin)
    val inMotion = isSpinning || (isWin && !showResult)
    val isAlreadyClaimedSession = !canSpin && !isSpinning && landingSegmentId == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (!inMotion) currentOnClose() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Floating Top-Right Close Button
        if (!inMotion) {
            val closeLabel = stringResource(R.string.ds_wheel_close)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 20.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { currentOnClose() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = closeLabel,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // 1. Victory Celebration Overlay (Right after spin lands!)
        if (showResult && isWin) {
            VictoryOverlay(
                resultText = resultText.orEmpty(),
                onCollect = currentOnClose,
            )
        } else {
            // 2. Normal Wheel Screen (Spin available OR Already claimed later view)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                // Header Titles
                AwanText(
                    text = stringResource(R.string.ds_wheel_title),
                    style = AwanTheme.typography.title.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.height(6.dp))

                AwanText(
                    text = stringResource(
                        if (isAlreadyClaimedSession) R.string.ds_wheel_subtitle_claimed
                        else R.string.ds_wheel_subtitle_available
                    ),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // The Wheel & Pointer
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    WheelCanvas(
                        segments = segments,
                        rotationDegrees = rotation.value,
                        canSpin = canSpin && !isAlreadyClaimedSession,
                        isSpinning = isSpinning,
                        showResult = showResult,
                        enabled = canSpin && !isSpinning && landingSegmentId == null,
                        onSpin = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSpin()
                        },
                    )

                    // White Pointer Pin at Top (-90°)
                    WheelPointer(recoilDegrees = pointerRecoil.value)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action & Status Footer
                if (isAlreadyClaimedSession) {
                    ClaimedStatusFooter()
                } else {
                    AwanText(
                        text = stringResource(
                            if (inMotion) R.string.ds_wheel_spinning else R.string.ds_wheel_tap_to_spin
                        ),
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Victory Screen Overlay presented right after winning a prize (Matching user reference image).
 */
@Composable
private fun VictoryOverlay(
    resultText: String,
    onCollect: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        // Confetti Sparkles Burst
        SparkleBurst(celebrate = true, modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            // Large Glowing Golden Star Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.5f),
                                    Color.Transparent,
                                )
                            ),
                            radius = size.minDimension * 0.75f,
                        )
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFE885), Color(0xFFFFB800))
                            )
                        )
                        .border(3.dp, Color.White, CircleShape),
                ) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(64.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "You won!" Headline
            AwanText(
                text = stringResource(R.string.ds_wheel_you_won_headline),
                style = AwanTheme.typography.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Won Prize text
            AwanText(
                text = resultText,
                style = AwanTheme.typography.title.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button "✓ Awesome!"
            AwanButton(
                onClick = onCollect,
                variant = AwanButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AwanText(
                        text = stringResource(R.string.ds_wheel_awesome),
                        style = AwanTheme.typography.button.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Bottom Status Footer when gift is claimed today.
 * Displays "Today's gift is claimed" & "See you tomorrow!".
 */
@Composable
private fun ClaimedStatusFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(
            text = stringResource(R.string.ds_wheel_claimed_title),
            style = AwanTheme.typography.title.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(modifier = Modifier.height(6.dp))

        AwanText(
            text = stringResource(R.string.ds_wheel_see_you_tomorrow),
            style = AwanTheme.typography.heading.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * Main 2D Happy Cartoon Wheel Canvas.
 */
@Composable
private fun WheelCanvas(
    segments: List<WheelSegmentUi>,
    rotationDegrees: Float,
    canSpin: Boolean,
    isSpinning: Boolean,
    showResult: Boolean,
    enabled: Boolean,
    onSpin: () -> Unit,
) {
    // Vibrant cartoon game palette matching reference images
    val happyPalette = listOf(
        Color(0xFF1E2D4A), // 0: Dark Navy (Gift)
        Color(0xFFF7C92B), // 1: Golden Yellow (1 Star)
        Color(0xFFFF8E00), // 2: Bright Orange (5 Stars)
        Color(0xFFE83D3D), // 3: Coral Red (10 Stars)
        Color(0xFF7C3AED), // 4: Violet Purple (20 Stars)
        Color(0xFF0096FF), // 5: Sky Blue (50 Stars)
        Color(0xFF22C55E), // 6: Lime Green (100 Stars)
    )

    val density = LocalDensity.current
    val labelSizePx = remember(density) { with(density) { 15.sp.toPx() } }

    val goldOuterBorder = Color(0xFFFFD700)
    val goldInnerBorder = Color(0xFFFFF7C2)
    val whiteColor = Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(WheelCanvasSize),
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (segments.isEmpty()) return@Canvas
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.46f
            val wheelRadius = size.minDimension * 0.42f
            val hubRadius = size.minDimension * 0.14f
            val count = segments.size
            val sweep = 360f / count

            // 1. Draw Double Golden Outer Ring
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(goldOuterBorder, Color(0xFFFF9400))
                ),
                radius = outerRadius,
                center = centerOffset,
            )
            drawCircle(
                color = goldInnerBorder,
                radius = wheelRadius + 3.dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 3.dp.toPx()),
            )

            // 2. Draw Rotated Wheel Segments
            rotate(degrees = rotationDegrees, pivot = centerOffset) {
                segments.forEachIndexed { index, _ ->
                    val startAngle = POINTER_ANGLE_DEG + index * sweep
                    val baseColor = happyPalette[index % happyPalette.size]

                    // Wedge background arc
                    drawArc(
                        color = baseColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset(centerOffset.x - wheelRadius, centerOffset.y - wheelRadius),
                        size = androidx.compose.ui.geometry.Size(wheelRadius * 2f, wheelRadius * 2f),
                    )

                    // Polka-dot white texture inside wedge
                    val midAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    val dot1Radius = wheelRadius * 0.35f
                    val dot2Radius = wheelRadius * 0.75f

                    drawCircle(
                        color = whiteColor.copy(alpha = 0.35f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(
                            x = centerOffset.x + (cos(midAngleRad - 0.15) * dot1Radius).toFloat(),
                            y = centerOffset.y + (sin(midAngleRad - 0.15) * dot1Radius).toFloat(),
                        ),
                    )
                    drawCircle(
                        color = whiteColor.copy(alpha = 0.35f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(
                            x = centerOffset.x + (cos(midAngleRad + 0.15) * dot2Radius).toFloat(),
                            y = centerOffset.y + (sin(midAngleRad + 0.15) * dot2Radius).toFloat(),
                        ),
                    )

                    // Wedge line divider
                    val dividerRad = Math.toRadians(startAngle.toDouble())
                    val dx = centerOffset.x + (cos(dividerRad) * wheelRadius).toFloat()
                    val dy = centerOffset.y + (sin(dividerRad) * wheelRadius).toFloat()
                    drawLine(
                        color = whiteColor.copy(alpha = 0.4f),
                        start = centerOffset,
                        end = Offset(dx, dy),
                        strokeWidth = 2.dp.toPx(),
                    )

                    // Perimeter Dots / Rivets
                    val pegX = centerOffset.x + (cos(dividerRad) * (wheelRadius - 5.dp.toPx())).toFloat()
                    val pegY = centerOffset.y + (sin(dividerRad) * (wheelRadius - 5.dp.toPx())).toFloat()
                    drawCircle(
                        color = whiteColor,
                        radius = 3.dp.toPx(),
                        center = Offset(pegX, pegY),
                    )
                }
            }

            // 3. Draw Upright Wedge Labels & Icons
            segments.forEachIndexed { index, segment ->
                drawWedgeLabel(
                    label = segment.label,
                    isItem = segment.isItem,
                    angleDeg = POINTER_ANGLE_DEG + index * sweep + sweep / 2f + rotationDegrees,
                    labelColor = whiteColor.toArgb(),
                    labelSizePx = labelSizePx,
                    wheelRadius = wheelRadius,
                    centerOffset = centerOffset,
                )
            }

            // 4. Center Cap Outer Ring
            drawCircle(
                brush = Brush.verticalGradient(listOf(goldOuterBorder, Color(0xFFFF9400))),
                radius = hubRadius + 3.dp.toPx(),
                center = centerOffset,
            )
            drawCircle(
                color = whiteColor,
                radius = hubRadius,
                center = centerOffset,
            )
        }

        // Center Cap Icon (Sparkles when available vs Lock when claimed)
        CenterCapIcon(
            canSpin = canSpin,
            enabled = enabled,
            onSpin = onSpin,
        )
    }
}

/**
 * Draws wedge label with star/gift icon text, always upright.
 */
private fun DrawScope.drawWedgeLabel(
    label: String,
    isItem: Boolean,
    angleDeg: Float,
    labelColor: Int,
    labelSizePx: Float,
    wheelRadius: Float,
    centerOffset: Offset,
) {
    val radians = Math.toRadians(angleDeg.toDouble())
    val labelRadius = wheelRadius * 0.62f
    val position = Offset(
        x = centerOffset.x + (cos(radians) * labelRadius).toFloat(),
        y = centerOffset.y + (sin(radians) * labelRadius).toFloat(),
    )

    // Render Icon + Text inside wedge
    val displayText = if (isItem) "🎁\n$label" else "★\n$label"
    val paint = android.graphics.Paint().apply {
        color = labelColor
        textSize = labelSizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, Color.Black.copy(alpha = 0.5f).toArgb())
    }

    val lines = displayText.split("\n")
    var currentY = position.y - (lines.size - 1) * labelSizePx * 0.6f
    for (line in lines) {
        drawContext.canvas.nativeCanvas.drawText(line, position.x, currentY + labelSizePx / 3f, paint)
        currentY += labelSizePx * 1.1f
    }
}

/**
 * Center Cap Icon (Gold Sparkles when available vs Gold Lock when claimed).
 */
@Composable
private fun CenterCapIcon(
    canSpin: Boolean,
    enabled: Boolean,
    onSpin: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = if (isPressed && enabled) 0.92f else 1f

    Box(
        modifier = Modifier
            .scale(pressScale)
            .size(64.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onSpin,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (canSpin) Lucide.Sparkles else Lucide.Lock,
            contentDescription = null,
            tint = Color(0xFFFFB800),
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * Clean White Top Pointer Pin (-90°).
 */
@Composable
private fun WheelPointer(recoilDegrees: Float) {
    Box(
        modifier = Modifier
            .offset(y = (-4).dp)
            .rotate(recoilDegrees)
            .size(28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val path = Path().apply {
                moveTo(w / 2f, h)
                lineTo(0f, 0f)
                lineTo(w, 0f)
                close()
            }

            // Shadow
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.3f),
                style = Stroke(width = 2.dp.toPx()),
            )
            // White Pin Face
            drawPath(path = path, color = Color.White)
        }
    }
}
