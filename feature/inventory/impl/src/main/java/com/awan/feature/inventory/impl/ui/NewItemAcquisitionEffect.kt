package com.awan.feature.inventory.impl.ui

import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val PARTICLE_COUNT = 8

private data class Spark(
    val angle: Float,       // radians; evenly distributed with random jitter
    val speed: Float,       // travel-speed multiplier relative to maxReach
    val lengthDp: Float,    // trail length in dp
)

/**
 * First-time item acquisition animation (triggered when the item's "seen" flag is false).
 *
 * Sequence:
 * 1. Item hovers upward then slams back to rest position with spring physics.
 * 2. Impact emits 8 golden sparks from the centre of the item's image area, radiating
 *    outward in randomised directions.  While inside the card the sparks are occluded by
 *    the card's opaque surface; they only become visible once they breach the card edge.
 * 3. A brief white glass-glimmer sweeps diagonally across the card face.
 *
 * --- Why the previous implementation broke ---
 * • Canvas used Modifier.fillMaxSize() inside a LazyVerticalGrid whose height constraint
 *   is unbounded.  fillMaxSize() on an unbounded axis collapses to height ≈ 0, so
 *   size.height / 2 ≈ 0 — placing every spark at the top of the cell.
 *   Fix: Modifier.matchParentSize() (BoxScope-only) sizes the Canvas to match the card.
 *
 * • drawLine(brush = Brush.linearGradient(...)) on a very short segment (≈10 dp) creates
 *   a degenerate gradient shader that falls back to black.
 *   Fix: use android.graphics.Paint + android.graphics.LinearGradient via drawIntoCanvas,
 *   which reliably renders the gradient on any segment length.
 *
 * • FastOutSlowInEasing front-loads all motion into the first ~100 ms, then the curve
 *   flattens — making sparks appear to freeze or snap.
 *   Fix: LinearEasing over a longer duration (700 ms) keeps travel continuous and visible.
 */
@Composable
fun NewItemAcquisitionEffect(
    index: Int = 0,
    itemId: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = reducedMotion()

    val yAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    // Master spark timeline: a single 0→1 value from which position and alpha are
    // derived as independent curves so travel and fade can overlap without snapping.
    val sparkAnim = remember { Animatable(0f) }
    val shineAnim = remember { Animatable(0f) }

    // Stable randomised trajectories — not re-generated on recomposition
    val sparks = remember {
        val step = (2f * PI.toFloat()) / PARTICLE_COUNT
        List(PARTICLE_COUNT) { i ->
            Spark(
                angle = i * step + (Random.nextFloat() - 0.5f) * step * 0.75f,
                speed = 0.65f + Random.nextFloat() * 0.70f,
                lengthDp = 6f + Random.nextFloat() * 8f,
            )
        }
    }

    // Reuse one Paint object to avoid per-frame allocations inside the draw loop
    val sparkPaint = remember {
        AndroidPaint().apply {
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
            isAntiAlias = true
        }
    }

    // Survives recomposition AND scroll-driven recycling of the LazyGrid slot.
    // Once true, the animation will never replay even if the composable re-enters
    // composition (e.g. after the item scrolls off-screen and back into view).
    var hasAnimated by rememberSaveable(itemId) { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (reduced || hasAnimated) return@LaunchedEffect
        hasAnimated = true

        // Stagger items so they don't all animate simultaneously
        delay((index * 80L).milliseconds)

        // ── Phase 1: hover up ─────────────────────────────────────────────────
        coroutineScope {
            launch { yAnim.animateTo(-18f, tween(220, easing = FastOutSlowInEasing)) }
            launch { scaleAnim.animateTo(1.06f, tween(220, easing = FastOutSlowInEasing)) }
        }
        delay(35.milliseconds)

        // ── Phase 2: slam down ────────────────────────────────────────────────
        coroutineScope {
            launch {
                yAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(130, easing = FastOutLinearInEasing),
                )
            }
        }

        // ── Phase 3: impact — sparks + glass shine concurrently ───────────────
        // sparkAnim drives a master 0→1 timeline over 1 100 ms.
        // sparkPos  = travel curve: ramps 0→1 over the FIRST 60 % (0–660 ms).
        // sparkAlpha = fade  curve: full brightness for first 30 % (0–330 ms),
        //              then linearly falls to 0 at 100 % (1 100 ms).
        // Result: sparks fly out fast, stay bright while traveling, then
        // remain at maximum distance and fade away slowly — no snapping.
        coroutineScope {
            launch {
                sparkAnim.snapTo(0f)
                sparkAnim.animateTo(1f, tween(1100, easing = LinearEasing))
            }
            launch {
                // Radial shine pulse: 600 ms is enough for a clean fade-in / fade-out.
                shineAnim.snapTo(0f)
                shineAnim.animateTo(1f, tween(600, easing = LinearEasing))
            }
        }
    }

    if (reduced) {
        Box(modifier) { content() }
        return
    }

    // Read animated values at composition level.
    // This causes one recomposition per animation frame, which is necessary to:
    //  – keep the zIndex modifier reactive (layout-phase modifier, not draw-phase)
    //  – ensure both Canvas and drawWithContent lambdas receive up-to-date values
    val sparkT = sparkAnim.value
    val sT = shineAnim.value

    // ── Derived spark curves ──────────────────────────────────────────────────
    // Travel: 0→1 over the first 60 % of the timeline; holds at 1 after that.
    val sparkPos = (sparkT / 0.6f).coerceIn(0f, 1f)
    // Alpha: full opacity for the first 30 %, then linear fall to 0 at 100 %.
    val sparkAlpha = if (sparkT < 0.3f) 1f else ((1f - sparkT) / 0.7f).coerceIn(0f, 1f)

    val burstActive = sparkT in 0.001f..0.999f
    val shape = AwanTheme.shapes.card

    Box(
        modifier = modifier
            // Elevate above neighbouring grid items during the burst so overflowing
            // sparks are drawn on top of adjacent cards
            .zIndex(if (burstActive) 2f else 0f)
            .graphicsLayer {
                translationY = yAnim.value * density
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            },
    ) {
        // ─────────────────────────────────────────────────────────────────────
        // LAYER 0 — Sparks   (drawn FIRST inside Box = behind the card)
        //
        // matchParentSize(): Canvas is measured AFTER the card child establishes
        // the Box size, so it gets the exact card dimensions (not an unbounded size).
        //
        // graphicsLayer { clip = false }: the Canvas's graphics layer does not clip
        // its own drawing, allowing sparks to visually overflow beyond the card edge
        // and be seen by the user once they breach the card boundary.
        //
        // Because the card (LAYER 1) is drawn on top with an opaque background, sparks
        // that are still inside the card are naturally occluded — no extra masking needed.
        // ─────────────────────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { clip = false },
        ) {
            if (!burstActive) return@Canvas

            // Spark origin = centre of the card's image area.
            // The card image uses aspectRatio(1:1), so imageHeight == imageWidth == size.width.
            // Using size.width / 2 for cy correctly centres on the image, not the full card
            // (which also includes the text row below the image).
            val cx = size.width / 2f
            val cy = size.width / 2f

            // maxReach > half card width → sparks clearly breach the card boundary
            val maxReach = size.width * 1.4f

            // sparkAlpha: full brightness first 30 %, then linear fall to 0 at 100 %.
            // Sparks travel to max distance first, THEN fade away — no coupling = no snap.
            val alphaInt = (sparkAlpha * 255).toInt()

            // Precompute pixel positions inside DrawScope where density (dp→px) is available.
            // drawIntoCanvas lambda does NOT have a Density context so dp.toPx() won't compile
            // if called inside it.
            val strokePx = 1.6.dp.toPx()
            val positions = Array(sparks.size) { i ->
                val spark = sparks[i]
                // sparkPos drives distance; sparkAlpha drives colour — fully independent.
                val dist = maxReach * sparkPos * spark.speed
                val trail = spark.lengthDp.dp.toPx()
                floatArrayOf(
                    cx + cos(spark.angle) * dist,              // sx
                    cy + sin(spark.angle) * dist,              // sy
                    cx + cos(spark.angle) * (dist + trail),    // ex
                    cy + sin(spark.angle) * (dist + trail),    // ey
                )
            }

            // Use native android.graphics.Paint + android.graphics.LinearGradient.
            // Compose's drawLine(brush = Brush.linearGradient(...)) can produce a degenerate
            // shader on short segments, rendering black.  The native path is guaranteed.
            drawIntoCanvas { composeCanvas ->
                val nc = composeCanvas.nativeCanvas
                sparkPaint.strokeWidth = strokePx
                positions.forEach { pos ->
                    // Bright gold #FFE100 → deep amber #FF5A00 along each spark trail
                    sparkPaint.shader = LinearGradient(
                        pos[0], pos[1], pos[2], pos[3],
                        AndroidColor.argb(alphaInt, 255, 225, 0),  // #FFE100
                        AndroidColor.argb(alphaInt, 255, 90, 0),   // #FF5A00
                        Shader.TileMode.CLAMP,
                    )
                    nc.drawLine(pos[0], pos[1], pos[2], pos[3], sparkPaint)
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // LAYER 1 — Card content + glass shine   (drawn LAST = on top of sparks)
        //
        // The card's own opaque surface hides any sparks still within the card
        // boundary — no explicit masking is required.
        // ─────────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    // Radial white-glass pulse driven by sin(sT·π):
                    //   sT=0.0 → alpha=0  (invisible, no snap-in)
                    //   sT=0.5 → alpha=1  (peak brightness at mid-animation)
                    //   sT=1.0 → alpha=0  (invisible, no snap-out)
                    // The sine curve guarantees smooth fade-in AND fade-out entirely
                    // on the card face — no linear sweep that exits the edge abruptly.
                    if (sT in 0.01f..0.99f) {
                        val pulseAlpha = sin(sT * PI.toFloat()).coerceIn(0f, 1f) * 0.45f
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = pulseAlpha),
                                    Color.White.copy(alpha = pulseAlpha * 0.4f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2f, size.width / 2f),
                                radius = size.width * 0.7f,
                            ),
                            size = Size(size.width, size.height),
                        )
                    }
                },
        ) {
            content()
        }
    }
}

