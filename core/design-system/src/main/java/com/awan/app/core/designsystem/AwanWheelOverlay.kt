package com.awan.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.cos
import kotlin.math.sin

private const val SCRIM_ALPHA = 0.6f
private const val SPIN_MILLIS = 2_600
private const val SPIN_TURNS = 5
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
 * The server decides the outcome; this only animates towards it. [landingSegmentId] going non-null
 * is the cue to spin — until then the wheel sits still waiting for a tap, and the request is in
 * flight. Rewards are deliberately *not* celebrated here: the caller queues them for after the
 * overlay closes, so the wheel is not fighting flying stars for the same screen.
 */
@Composable
fun AwanWheelOverlay(
    segments: List<WheelSegmentUi>,
    landingSegmentId: String?,
    resultText: String?,
    isSpinning: Boolean,
    onSpin: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val currentOnClose by rememberUpdatedState(onClose)
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(landingSegmentId, segments.size) {
        val landing = landingSegmentId ?: return@LaunchedEffect
        val index = segments.indexOfFirst { it.id == landing }
        if (index < 0 || segments.isEmpty()) return@LaunchedEffect

        val sweep = 360f / segments.size
        // Bring the winning wedge's centre under the pointer at the top of the wheel.
        val target = -(index * sweep + sweep / 2f)
        if (reduced) {
            rotation.snapTo(target)
        } else {
            rotation.animateTo(
                targetValue = target + SPIN_TURNS * 360f,
                animationSpec = tween(SPIN_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (resultText != null) currentOnClose() },
            ),
        contentAlignment = Alignment.Center,
    ) {
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
                    enabled = !isSpinning && landingSegmentId == null,
                    onSpin = onSpin,
                )
                WheelPointer()
            }

            Spacer(modifier = Modifier.height(20.dp))

            AwanText(
                text = resultText ?: stringResource(
                    if (isSpinning) R.string.ds_wheel_spinning else R.string.ds_wheel_tap_to_spin
                ),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            if (resultText != null) {
                Spacer(modifier = Modifier.height(20.dp))
                AwanButton(onClick = currentOnClose, variant = AwanButtonVariant.Primary) {
                    AwanText(
                        text = stringResource(R.string.ds_wheel_collect),
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
            segments.forEachIndexed { index, segment ->
                // Wedge 0 starts at the pointer so index maps directly onto the landing rotation.
                val startAngle = POINTER_ANGLE_DEG + index * sweep
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                )
                drawWedgeLabel(
                    label = segment.label,
                    angleDeg = startAngle + sweep / 2f,
                    labelColor = labelColor,
                    labelSizePx = labelSizePx,
                )
            }
        }
        drawCircle(color = colors.surface, radius = size.minDimension * 0.11f)
    }
}

/**
 * Labels are drawn upright rather than rotated with their wedge — a spinning wheel of sideways
 * numbers is unreadable, and the wedge colour already conveys which slice is which.
 */
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
