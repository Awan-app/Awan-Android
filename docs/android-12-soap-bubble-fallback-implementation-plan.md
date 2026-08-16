# Android 12 Soap Bubble Fallback — Implementation Plan

## Status

**Phase 1 is already implemented.**

The existing Android 13+ AGSL `RuntimeShader` implementation is now the **visual source of truth**.

This plan covers only the follow-up work required to support the same soap-bubble design on:

```text
Android 12
API 31–32
```

The goal is not to redesign the effect.

The Android 12 implementation should approximate the already-approved AGSL result as closely as practical using APIs available on Android 12.

---

# 1. Goal

Implement an Android 12-compatible procedural renderer for the existing flat 2D soap-bubble effect.

The fallback must preserve the same visual language:

- transparent center;
- pastel cyan / lavender / pink / pale-yellow interference;
- irregular broken bands around the perimeter;
- subtle gravity bias;
- upper-left primary white highlight;
- lower-right secondary white highlight;
- slow organic movement;
- no obvious rotation;
- no scale pulsing;
- no glass-ball or 3D appearance;
- underlying item remains crisp and readable.

The target is **perceptual parity**, not pixel-identical rendering.

---

# 2. Final Platform Behaviour

After this plan is implemented:

```text
API 33+
    ↓
Existing AGSL RuntimeShader implementation
    ↓
Highest-fidelity effect

API 31–32
    ↓
New procedural Compose Canvas implementation
    ↓
Android 12 visual-parity fallback

API 30 and below
    ↓
Original UI content without animated bubble
```

The public component API should remain unchanged.

---

# 3. Source of Truth

Do not independently tune a new Android 12 style.

The existing AGSL implementation defines:

- palette;
- opacity;
- highlight placement;
- interference density;
- motion speed;
- gravity behaviour;
- overall visual weight.

When the Canvas fallback differs from the AGSL implementation, tune the fallback toward AGSL.

Do not tune AGSL toward the fallback.

---

# 4. Recommended Rendering Strategy

Use a **procedural multi-ring segmented arc renderer** in Compose `Canvas`.

Conceptually:

```text
                 outer ring
          ╭────────────────╮
       ╭──╯    middle ring   ╰──╮
      │     ╭────────────╮      │
      │     │ inner ring │      │
      │     │            │      │
      │     │  CONTENT   │      │
      │     │            │      │
      │     ╰────────────╯      │
       ╰──╮                  ╭──╯
          ╰────────────────╯
```

Only the outer region of the bubble is rendered.

Each ring is split into small arc segments.

Each segment samples a Kotlin version of the same mathematical interference field used by AGSL.

---

# 5. Why Not Use One Sweep Gradient

Do **not** approximate the effect with:

```text
single rainbow SweepGradient
+
rotation
```

That would create:

```text
rainbow ring
→ spins
→ repeats
```

The approved effect instead behaves like:

```text
one patch grows
another fades
two colors merge
a third band bends around them
the pattern evolves continuously
```

The segmented renderer is required because it allows independent variation by:

- angle;
- radius;
- time;
- interference wave;
- gravity;
- alpha.

---

# 6. Internal Architecture

Recommended file split:

```text
SoapBubbleOverlay.kt
SoapBubbleAgslLayer.kt
SoapBubbleCanvasLayer.kt
SoapBubbleMath.kt
SoapBubbleStyle.kt
```

## `SoapBubbleOverlay.kt`

Responsibilities:

- public API;
- content layering;
- platform dispatch.

## `SoapBubbleAgslLayer.kt`

Already implemented.

Responsibilities remain:

- API 33+ renderer;
- AGSL source;
- `RuntimeShader`.

Do not rewrite this as part of Phase 2 unless a small extraction is needed for shared configuration.

## `SoapBubbleCanvasLayer.kt`

New.

Responsibilities:

- Android 12 renderer;
- interference rings;
- highlight arcs;
- thin rim.

## `SoapBubbleMath.kt`

New.

Responsibilities:

- Kotlin version of interference sampling;
- palette interpolation;
- alpha rules;
- periodic time math;
- gravity weighting.

## `SoapBubbleStyle.kt`

New or extracted from the existing implementation.

Responsibilities:

- shared constants;
- default animation duration;
- fallback sampling density;
- ring radii and widths;
- palette values.

---

# 7. Preserve the Public API

Keep:

```kotlin
@Composable
fun SoapBubbleOverlay(
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    cycleDurationMillis: Int = 12_000,
    content: @Composable BoxScope.() -> Unit,
)
```

Do not expose Android-version-specific controls.

Consumers should not need to know whether the current device uses AGSL or Canvas.

---

# 8. Platform Dispatch

Update only the internal renderer selection.

```kotlin
when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        SoapBubbleAgslLayer(
            modifier = Modifier.matchParentSize(),
            strength = strength,
            cycleDurationMillis = cycleDurationMillis,
        )
    }

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        SoapBubbleCanvasLayer(
            modifier = Modifier.matchParentSize(),
            strength = strength,
            cycleDurationMillis = cycleDurationMillis,
        )
    }
}
```

Final policy:

```text
API >= 33  → AGSL
API 31–32  → Canvas
API <= 30  → no decorative bubble layer
```

---

# 9. Shared Animation Clock

Both renderers should use the same normalized animation timing.

Extract:

```kotlin
@Composable
internal fun rememberSoapBubbleTime(
    cycleDurationMillis: Int,
): Float {
    val transition = rememberInfiniteTransition(
        label = "SoapBubbleTransition",
    )

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = cycleDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "SoapBubbleTime",
    )

    return time
}
```

Both renderers work with:

```text
time ∈ [0, 1]
```

and internally convert to:

```text
t = time × 2π
```

This ensures both implementations use the same loop duration and repeat boundary.

---

# 10. Kotlin Soap-Film Sampler

Create a pure Kotlin implementation of the visual math.

Suggested model:

```kotlin
internal data class SoapFilmSample(
    val color: Color,
    val alpha: Float,
)
```

Suggested API:

```kotlin
internal fun sampleSoapFilm(
    normalizedRadius: Float,
    angleRadians: Float,
    normalizedTime: Float,
    strength: Float,
): SoapFilmSample
```

The function should mirror the existing AGSL stages.

---

# 11. Math Stages to Match AGSL

Implement these in the same order as the current shader:

```text
1. Convert normalized time to radians.
2. Calculate angular organic warp.
3. Calculate warped radius.
4. Calculate film thickness.
5. Add gravity bias.
6. Evaluate wave A.
7. Evaluate wave B.
8. Combine interference.
9. Convert interference into visible patches.
10. Calculate outer-film mask.
11. Calculate palette phase.
12. Interpolate soap palette.
13. Mix color toward white.
14. Calculate alpha.
15. Clamp output.
```

The formulas should be copied from the finished shader where possible.

Do not create a separate artistic interpretation.

---

# 12. Palette

Use the same values as AGSL.

```kotlin
private val Cyan = Color(
    red = 0.48f,
    green = 0.90f,
    blue = 1.00f,
    alpha = 1f,
)

private val Lavender = Color(
    red = 0.76f,
    green = 0.66f,
    blue = 1.00f,
    alpha = 1f,
)

private val Pink = Color(
    red = 1.00f,
    green = 0.67f,
    blue = 0.87f,
    alpha = 1f,
)

private val PaleYellow = Color(
    red = 1.00f,
    green = 0.91f,
    blue = 0.58f,
    alpha = 1f,
)
```

Interpolation order:

```text
cyan
→ lavender
→ pink
→ pale yellow
→ cyan
```

Then blend approximately:

```text
23% toward white
```

to maintain the same pastel feel.

Do not replace these with Material theme colors.

---

# 13. Initial Ring Configuration

Start with three rings.

Recommended normalized radii:

```kotlin
internal val RingRadii = floatArrayOf(
    0.74f,
    0.84f,
    0.93f,
)
```

Recommended relative stroke widths:

```kotlin
internal val RingWidths = floatArrayOf(
    0.055f,
    0.050f,
    0.040f,
)
```

These are starting values.

Tune them only by comparing directly against the AGSL reference.

---

# 14. Segment Density

Start with:

```kotlin
internal const val SegmentCount = 72
```

That gives:

```text
360° / 72 = 5° per segment
```

For three rings:

```text
72 × 3 = 216 arc draws per bubble per frame
```

Later profiling should compare:

```text
48 segments
72 segments
96 segments
```

Use the lowest density that does not reveal visible angular segmentation at real production sizes.

---

# 15. Canvas Renderer Structure

Start from:

```kotlin
@Composable
internal fun SoapBubbleCanvasLayer(
    modifier: Modifier,
    strength: Float,
    cycleDurationMillis: Int,
) {
    val time = rememberSoapBubbleTime(
        cycleDurationMillis = cycleDurationMillis,
    )

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val bubbleCenter = center

        repeat(RingRadii.size) { ringIndex ->
            val normalizedRadius = RingRadii[ringIndex]
            val ringRadius = maxRadius * normalizedRadius
            val strokeWidth = maxRadius * RingWidths[ringIndex]

            repeat(SegmentCount) { segmentIndex ->
                val startFraction =
                    segmentIndex.toFloat() / SegmentCount

                val angleRadians =
                    startFraction * TWO_PI

                val sample = sampleSoapFilm(
                    normalizedRadius = normalizedRadius,
                    angleRadians = angleRadians,
                    normalizedTime = time,
                    strength = strength,
                )

                if (sample.alpha > MIN_VISIBLE_ALPHA) {
                    drawArc(
                        color = sample.color.copy(
                            alpha = sample.alpha,
                        ),
                        startAngle = startFraction * 360f,
                        sweepAngle =
                            360f / SegmentCount +
                            SEGMENT_OVERLAP_DEGREES,
                        useCenter = false,
                        topLeft = Offset(
                            bubbleCenter.x - ringRadius,
                            bubbleCenter.y - ringRadius,
                        ),
                        size = Size(
                            ringRadius * 2f,
                            ringRadius * 2f,
                        ),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
        }

        drawSoapBubbleRim(
            radius = maxRadius,
            strength = strength,
        )

        drawSoapBubbleHighlights(
            radius = maxRadius,
            time = time,
            strength = strength,
        )
    }
}
```

---

# 16. Prevent Visible Segment Gaps

Use all of these:

```text
rounded stroke caps
+
slight arc overlap
+
low alpha
+
at least 48 samples
+
multiple rings
+
continuous neighboring math samples
```

Recommended starting overlap:

```kotlin
internal const val SEGMENT_OVERLAP_DEGREES = 0.75f
```

Do not apply a large blur merely to hide arc segmentation.

---

# 17. Thin Rim

After the interference rings, draw a very subtle outer rim.

Requirements:

- near-white;
- slightly cool if necessary;
- low opacity;
- not fully continuous in visual weight;
- approximately 1–2 px at normal token sizes.

Possible implementation:

```kotlin
drawCircle(
    color = Color.White.copy(
        alpha = 0.08f * strength,
    ),
    radius = radius * 0.97f,
    style = Stroke(
        width = rimWidth,
    ),
)
```

If this looks too uniform compared with AGSL, replace it with segmented or gradient-weighted arc sections.

---

# 18. Highlight Crescents

Draw highlights independently of the interference field.

## Primary highlight

Target:

```text
upper-left
35–55°
stronger
```

## Secondary highlight

Target:

```text
lower-right
18–35°
weaker
```

Example structure:

```kotlin
drawArc(
    color = Color.White.copy(
        alpha = primaryAlpha,
    ),
    startAngle = primaryStartAngle,
    sweepAngle = primarySweepAngle,
    useCenter = false,
    topLeft = ...,
    size = ...,
    style = Stroke(
        width = highlightWidth,
        cap = StrokeCap.Round,
    ),
)
```

The highlights should not orbit around the bubble.

At most, allow very small position and alpha drift.

---

# 19. Gravity Bias

Match the AGSL behaviour.

The lower portion should, on average, receive a small visibility increase.

This should remain subtle.

Do not make the bottom of the bubble look visibly heavier or filled.

---

# 20. Optional Android 12 Softening

Android 12 supports `RenderEffect`, including blur.

Treat this as optional.

First attempt to achieve sufficient smoothness using:

- low opacity;
- rounded caps;
- segment overlap;
- enough segments;
- multiple rings.

Only if segmentation remains visible should a small local blur be tested.

Suggested range:

```text
0.5–1.5 dp
```

Important:

- blur only the decorative bubble layer;
- never blur underlying content;
- do not make blur required for the effect.

---

# 21. Avoid Per-Frame Allocations

The Canvas renderer executes every animation frame.

Avoid repeatedly creating:

- lists;
- arrays;
- brushes;
- paths;
- unnecessary data classes.

Precompute where useful:

```text
segment angles
ring configuration
palette constants
```

If profiling shows `SoapFilmSample` allocation is significant, replace it with a lower-allocation representation.

Do this only after measurement.

---

# 22. Multiple Bubbles

A grid of many bubbles is the main performance risk.

Test at:

```text
1
4
8
12
20
```

simultaneously visible bubbles.

If necessary, optimize in this order:

```text
1. Share one animation clock.
2. Reduce segment count.
3. Precompute segment angles.
4. Remove per-frame allocations.
5. Reduce ring count for compact mode.
```

Do not first weaken the visual design.

---

# 23. Shared Clock for Collections

When many bubbles are visible, prefer one shared time value.

Then give each item a stable phase offset.

Example:

```kotlin
val phaseOffset =
    ((stableId.hashCode() and 0xFFFF) / 65535f)

val localTime =
    (sharedTime + phaseOffset) % 1f
```

This avoids synchronized identical bubbles while keeping animation overhead low.

The phase must be stable.

Do not generate a new random offset on recomposition.

---

# 24. Quality Tiers

Keep these internal.

## Default

```text
3 rings
72 segments
```

Start here.

## Compact

```text
2–3 rings
48 segments
```

Use only if needed for dense inventory grids.

## High

```text
3 rings
96 segments
```

Use only if profiling proves it is inexpensive and visibly better.

The public API should not expose these modes initially.

---

# 25. Reduced Motion

Match the AGSL behaviour.

Normal:

```text
time = animated 0..1
```

Reduced motion:

```text
time = fixed frame
```

Example:

```kotlin
val time =
    if (reduceMotion) {
        0.18f
    } else {
        animatedTime
    }
```

The bubble remains visible but static.

---

# 26. Pure Math Tests

Test `sampleSoapFilm()` independently.

## Periodicity

Representative samples should satisfy:

```text
sample(time = 0f)
≈
sample(time = 1f)
```

## Alpha bounds

Ensure:

```text
0 <= alpha <= expected maximum
```

and that strength scales predictably.

## Color bounds

Every channel must remain:

```text
0..1
```

## Invalid numbers

No sample may produce:

```text
NaN
Infinity
-Infinity
```

## Gravity

Across multiple angular samples:

```text
average lower-edge alpha
>
average upper-edge alpha
```

by a small amount.

Do not require every individual lower sample to be stronger because interference waves intentionally vary.

---

# 27. Static Visual Validation

Freeze animation at:

```text
0.00
0.20
0.40
0.60
0.80
```

Capture Android 12 screenshots.

Validate:

- clear center;
- no obvious arc gaps;
- no polygonal ring;
- same pastel palette;
- similar visual weight to AGSL;
- similar highlight placement;
- no neon appearance;
- no heavy fill.

---

# 28. Motion Validation

Run the full cycle.

Validate:

- no loop snap;
- no obvious rotation;
- no pulse;
- no scale change;
- continuously evolving interference;
- stable circle geometry;
- readable underlying item.

---

# 29. Side-by-Side AGSL Comparison

Compare:

```text
Android 12 Canvas
vs
Android 13+ AGSL
```

at the actual production bubble sizes.

Compare these characteristics:

```text
transparency
palette
edge concentration
highlight placement
motion speed
organic behaviour
visual weight
```

Exact interference-band positions do not need to match.

The two implementations should feel like the same component.

---

# 30. Performance Validation

Profile:

```text
1 bubble
4 bubbles
8 bubbles
12 bubbles
20 bubbles
```

Watch:

- frame duration;
- jank;
- skipped frames;
- CPU usage;
- allocations;
- recomposition frequency.

If performance is poor, optimize in this order:

```text
1. Lower SegmentCount.
2. Share animation clocks.
3. Precompute angle values.
4. Eliminate per-frame allocations.
5. Reduce ring count only for compact mode.
```

---

# 31. Acceptance Criteria

Phase 2 is complete when:

- [ ] API 33+ still uses the existing AGSL implementation.
- [ ] API 31–32 automatically use the Canvas fallback.
- [ ] API 30 and below remain safe.
- [ ] Public `SoapBubbleOverlay` usage remains unchanged.
- [ ] Underlying text/icons/artwork remain crisp.
- [ ] Bubble center remains nearly transparent.
- [ ] Cyan/lavender/pink/yellow palette matches AGSL.
- [ ] Colors remain pastel.
- [ ] Interference remains concentrated near the perimeter.
- [ ] Multiple broken color patches are visible.
- [ ] Motion does not look like simple rotation.
- [ ] Primary upper-left highlight is present.
- [ ] Secondary lower-right highlight is present.
- [ ] Gravity weighting is subtle.
- [ ] Animation loops without visible snapping.
- [ ] Reduced-motion behaviour works.
- [ ] Single-bubble performance is smooth on API 31.
- [ ] Typical multi-item screens meet frame-performance requirements.
- [ ] Side-by-side visual review identifies both renderers as the same design effect.

---

# 32. Acceptable Differences From AGSL

The Android 12 renderer samples a finite number of arcs instead of evaluating every pixel.

Therefore it may have:

- slightly simpler band shapes;
- less microscopic interference detail;
- slightly more graphic transitions.

That is acceptable.

The design is already intentionally flat and 2D.

The fallback should preserve:

```text
feeling
motion language
palette
transparency
visual hierarchy
```

rather than chasing physically accurate optical simulation.

---

# 33. Implementation Order

Implement Phase 2 in this exact order:

```text
1. Treat the existing AGSL output as frozen reference.
2. Extract shared visual constants where useful.
3. Extract/shared normalized animation clock.
4. Create SoapBubbleMath.kt.
5. Implement the Kotlin interference sampler.
6. Add sampler unit tests.
7. Implement SoapBubbleCanvasLayer with one ring.
8. Compare one ring against AGSL.
9. Expand to three rings.
10. Add segment overlap and rounded caps.
11. Add thin rim.
12. Add upper-left highlight.
13. Add lower-right highlight.
14. Add gravity weighting.
15. Add API 31–32 dispatch.
16. Freeze and inspect five animation states.
17. Compare API 31/32 against API 33+.
18. Tune ring radii, widths, and alpha.
19. Tune SegmentCount.
20. Add reduced-motion support.
21. Profile one bubble.
22. Profile multi-bubble screens.
23. Add shared clock/per-item phase offsets if required.
24. Re-run visual parity review.
25. Complete final API 31/32 acceptance pass.
```

---

# 34. Final Target

The Android 12 version should never feel like a visibly inferior substitute.

A user should perceive both implementations as:

```text
transparent
soft
pastel
organic
alive
flat
playful
premium
```

The implementation mechanism changes.

The product design does not.
