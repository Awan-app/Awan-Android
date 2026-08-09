package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * The rotation that parks wedge [index] under the pointer, wound forward past where the wheel
 * currently is so it always settles by slowing down — never by jumping or reversing.
 */
private fun landingRotation(current: Float, index: Int, count: Int): Float {
    val sweep = 360f / count
    val aligned = -(index * sweep + sweep / 2f)
    val minimum = current + LANDING_TURNS * 360f
    return aligned + ceil((minimum - aligned) / 360f) * 360f
}

private const val SCRIM_ALPHA = 0.6f

/** One turn of the free spin, at constant speed, while the server is deciding. */
private const val FREE_TURN_MILLIS = 700

/** How long the wheel takes to slow down and settle once the outcome is known. */
private const val LANDING_MILLIS = 2_200

/** Turns the wheel must still travel while decelerating, so it never appears to snap or reverse. */
private const val LANDING_TURNS = 3

private const val POINTER_ANGLE_DEG = -90f
private val WheelSize = 280.dp

/**
 * A wedge as the wheel draws it. The design system keeps its own shape rather than taking the
 * domain model, matching how the timeline takes [ScheduleSession] instead of a `DaySession`.
 */
@Immutable
data class WheelSegmentUi(
    val id: String,
    val label: String,
    val isItem: Boolean,
)

/**
 * The daily-gift wheel.
 *
 * The spin starts the moment it is tapped and free-wheels at constant speed while the request is in
 * flight, because a wheel that sits still until the network answers reads as a broken tap. The server
 * still decides the outcome — [landingSegmentId] arriving is the cue to stop *there*, and the wheel
 * decelerates onto it rather than jumping.
 *
 * The prize is announced only once the wheel has actually stopped on it. Showing the text the moment
 * the response lands would spoil the result while the wheel is still travelling toward it.
 *
 * Rewards are deliberately not *paid out* here: the caller queues them for after the overlay closes,
 * so the wheel is not fighting flying stars for the same screen.
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
    val rotation = remember { Animatable(0f) }
    var landed by remember { mutableStateOf(false) }
    var wasFreeSpinning by remember { mutableStateOf(false) }

    LaunchedEffect(landingSegmentId, isSpinning, segments.size) {
        val landing = landingSegmentId
        val index = landing?.let { id -> segments.indexOfFirst { it.id == id } } ?: -1
        when {
            // A segment the local config does not know about still deserves a clean stop, so an
            // unrecognised id falls through to the wind-down rather than freezing mid-turn.
            index >= 0 -> {
                wasFreeSpinning = false
                val target = landingRotation(rotation.value, index, segments.size)
                if (reduced) {
                    rotation.snapTo(target)
                } else {
                    // Eases out from the speed the free spin left it at, so the two phases read as
                    // one continuous spin winding down.
                    rotation.animateTo(target, tween(LANDING_MILLIS, easing = LinearOutSlowInEasing))
                }
                landed = true
            }

            isSpinning && !reduced -> {
                // Free-wheels a turn at a time until the outcome arrives and cancels this.
                wasFreeSpinning = true
                while (true) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(FREE_TURN_MILLIS, easing = LinearEasing),
                    )
                }
            }

            // The spin ended with nothing to stop on — a failed request, or a wedge this build does
            // not recognise. Coast to a halt rather than freezing mid-turn, which reads as a hang.
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

    // A failure has no wedge to travel to and no surprise to protect, so it is shown straight away.
    // A win waits for the wheel to actually stop on it.
    val isWin = landingSegmentId != null
    val showResult = resultText != null && (landed || !isWin)

    // Still turning: either the request is out, or the outcome is known but the wheel has not
    // finished travelling to it.
    val inMotion = isSpinning || (isWin && !showResult)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                // Dismissable before a spin and after a result, but never mid-spin: closing then
                // would strand a gift the server has already charged against today's allowance.
                onClick = { if (!inMotion) currentOnClose() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A visible way out for someone who opened the gift but does not want to spin yet. Hidden
        // mid-spin, where there is nothing to cancel and the gift is already spent.
        if (!inMotion) {
            val closeLabel = stringResource(R.string.ds_wheel_close)
            Icon(
                imageVector = Lucide.X,
                contentDescription = closeLabel,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { currentOnClose() },
                    )
                    .padding(10.dp)
                    .size(24.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AwanText(
                text = stringResource(R.string.ds_wheel_title),
                style = AwanTheme.typography.title.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                ),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.TopCenter) {
                Wheel(
                    segments = segments,
                    rotationDegrees = rotation.value,
                    // `canSpin` is what keeps an already-claimed day from being retried; a plain
                    // network failure leaves it true, so that one stays tappable.
                    enabled = canSpin && !isSpinning && landingSegmentId == null,
                    onSpin = onSpin,
                )
                WheelPointer()

                // Only a real prize gets confetti — an error is not a celebration.
                if (showResult && isWin) {
                    SparkleBurst(celebrate = true, modifier = Modifier.size(WheelSize))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AwanText(
                text = resultText.takeIf { showResult } ?: stringResource(
                    if (inMotion) R.string.ds_wheel_spinning else R.string.ds_wheel_tap_to_spin
                ),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            if (showResult) {
                Spacer(modifier = Modifier.height(20.dp))
                AwanButton(onClick = currentOnClose, variant = AwanButtonVariant.Primary) {
                    AwanText(
                        // Nothing was won on a failure, so offering to "collect" it would be a lie.
                        text = stringResource(
                            if (isWin) R.string.ds_wheel_collect else R.string.ds_wheel_close
                        ),
                        style = AwanTheme.typography.button,
                    )
                }
            }
        }
    }
}

@Composable
private fun Wheel(
    segments: List<WheelSegmentUi>,
    rotationDegrees: Float,
    enabled: Boolean,
    onSpin: () -> Unit,
) {
    val colors = AwanTheme.colors
    val palette = remember(colors) {
        listOf(
            colors.zoneSun, colors.zoneTangerine, colors.zoneCoral,
            colors.zoneViolet, colors.zoneLavender, colors.sky,
        )
    }
    val labelColor = colors.surface.toArgb()
    val labelSizePx = with(LocalDensity.current) { 15.sp.toPx() }

    Canvas(
        modifier = Modifier
            .size(WheelSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onSpin,
            ),
    ) {
        if (segments.isEmpty()) return@Canvas
        val sweep = 360f / segments.size
        rotate(degrees = rotationDegrees) {
            segments.forEachIndexed { index, _ ->
                // Wedge 0 starts at the pointer so index maps directly onto the landing rotation.
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = POINTER_ANGLE_DEG + index * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                )
            }
        }
        // Drawn outside the rotation: the labels travel with their wedge but stay upright, because a
        // spinning wheel of upside-down numbers is unreadable.
        segments.forEachIndexed { index, segment ->
            drawWedgeLabel(
                label = segment.label,
                angleDeg = POINTER_ANGLE_DEG + index * sweep + sweep / 2f + rotationDegrees,
                labelColor = labelColor,
                labelSizePx = labelSizePx,
            )
        }
        drawCircle(color = colors.surface, radius = size.minDimension * 0.11f)
    }
}

/** Places a label at [angleDeg] around the wheel, always the right way up. */
private fun DrawScope.drawWedgeLabel(
    label: String,
    angleDeg: Float,
    labelColor: Int,
    labelSizePx: Float,
) {
    val radians = Math.toRadians(angleDeg.toDouble())
    val radius = size.minDimension * 0.33f
    val position = Offset(
        x = center.x + (cos(radians) * radius).toFloat(),
        y = center.y + (sin(radians) * radius).toFloat(),
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        position.x,
        position.y + labelSizePx / 3f,
        android.graphics.Paint().apply {
            color = labelColor
            textSize = labelSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        },
    )
}

@Composable
private fun WheelPointer() {
    val pointerColor = AwanTheme.colors.surface
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2f, size.height)
            lineTo(0f, 0f)
            lineTo(size.width, 0f)
            close()
        }
        drawPath(path = path, color = pointerColor)
    }
}
