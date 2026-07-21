# Onboarding: horizontal day timeline, slider fix, design-system haptics, shared CascadeItem

## Context

Five problems on the onboarding flow, all in `feature/onboarding/impl` and `core/design-system`:

1. **`DayPreview` eats the screen.** It's a fixed 200 dp vertical sky panel (`DayPreview.kt:47,61`). On `ZonesStep` it sits above a 4-row reorder list, under a headline, inside a scaffold that already spends height on progress bar + footer — roughly 250 dp of chrome for a graphic that only communicates "wake here, sleep there". Vertical stacking also makes zone bands short and their labels drop out below 22 dp (`DayPreview.kt:110`).
2. **The task-length slider desyncs.** `TaskLengthSlider.kt:56-57`: the knob offset is animated (`animateDpAsState`, default spring) but the track fill is computed straight from `index` with no animation. The fill snaps to the new stop while the knob springs behind it — that is the "bar raises ahead of the knob" the user sees. They are two independent values that can never agree.
3. **No haptics in the design system.** Every `AwanButton` caller has to fire its own (`AwanButton.kt` performs none); onboarding does it ad-hoc in `OnboardingScreen.kt:95-104` and `ZoneReorderList.kt`. Haptics should belong to the button and vary by variant.
4. **`CascadeItem` is stuck in the feature module** (`ui/components/CascadeItem.kt`) but is generally useful. It depends on `reducedMotion()` (`ui/Motion.kt`), which three other components also use, so both move together.
5. **Clock text changes appearance as the time changes.** `formatClock` (`ui/TimeFormat.kt:13-20`) uses `SimpleDateFormat("h:mm a", Locale.getDefault())`, so digits and the AM/PM marker come from the platform locale. Baloo2 (`Tokens.kt:21-25`) has no Arabic / Arabic-Indic glyphs, so non-Latin output falls back to a system font that renders visibly larger and differently shaped. Confirmed by the user: "digits/letters switch to a different-looking font". Compounded by `DayPreview.EdgeLabel` (`DayPreview.kt:165-168`) rendering the same string through raw `BasicText` + `typography.heading.copy()` instead of `AwanText` + a style, so the two clocks on `DayBoundsStep` don't even share a render path.

Outcome: a compact horizontal timeline used by both steps, a slider whose parts are mathematically locked together, variant-aware haptics owned by `AwanButton`, `CascadeItem` in `core:design-system` with docs, and a clock string that always renders in Baloo2.

**Step 0 (per CLAUDE.md):** copy this plan to `docs/feature/onboarding/2026-07-20-timeline-slider-haptics.md` before starting. Append an `## Implementation notes (what actually differed)` section when done.

---

## 1. Horizontal day timeline

Replace `ui/components/DayPreview.kt` with `ui/components/DayTimeline.kt`. **No presentation-layer change** — `DayPreviewModel.from` (`presentation/DayPreviewModel.kt:24-48`) already computes `startFraction`/`endFraction` relative to the *waking window* (`/waking`), which is exactly the axis chosen.

Signature stays drop-in: `DayTimeline(preview: DayPreviewModel, modifier: Modifier, showZones: Boolean = true)`.

Inside the existing `AwanCard`:

```
YOUR DAY                              16 h of open sky      <- existing metaText row, unchanged
┌──────────┬────────────────┬──────────┬──────────┐
│  Study   │      Work      │   Play   │ Personal │          <- 44.dp track, horizontal gradient
└──────────┴────────────────┴──────────┴──────────┘
☀️ 7:00 AM                             11:00 PM 🌙          <- endpoint row, clockText
```

- **Track**: `Box(height = 44.dp)`, `clip(AwanTheme.shapes.chip)`, `Brush.horizontalGradient(0f to SkyDawn, 0.55f to SkyDay, 1f to SkyNight)` — reuse the existing colour constants, just rotate the brush.
- **Zone segments**: keep the current `BoxWithConstraints` + per-`key(block.id)` `animateFloatAsState(AwanTheme.motion.settle.spec())` pattern verbatim (`DayPreview.kt:81-96`) — only the axis changes. Position with `Modifier.offset(x = fullWidth * startFraction).width(fullWidth * (end - start))`. `offset` is layout-direction aware, so RTL flips for free; do **not** use `absoluteOffset`. Drop segments narrower than 2.dp; drop the label below ~44.dp wide (mirrors the existing 22 dp height rule). Disabled alpha 0.22, enabled 0.9 — unchanged.
- **Starred task**: horizontally there is no room for the current chip. Render it as a 3.dp full-height white marker at `fullWidth * task.startFraction` with a ★ dot on top, and move `onboarding_day_preview_starred_task` to a caption line under the track. New string may be needed if the phrasing changes.
- **Endpoint row**: `Row(SpaceBetween)` with `AwanText(formatClock(wake), style = AwanTheme.styles.clockText)` + "☀️", and "🌙" + sleep on the end side. **Delete the private `EdgeLabel` raw-`BasicText` helper** — both clocks now go through `AwanText` + a style.
- **`showZones = false`** (DayBounds): track shows the gradient plus the existing centred `HintPill`. Keep `HintPill` as-is.

Height budget: ~18 (header) + 44 (track) + ~20 (endpoints) + spacers ≈ **90 dp** inside the card, versus ~250 dp today.

Call sites to update: `steps/DayBoundsStep.kt:51`, `steps/ZonesStep.kt:34`, and any previews in `ui/OnboardingPreviews.kt`.

---

## 2. Clock formatting

`ui/TimeFormat.kt` — make `formatClock` `@Composable` (all four call sites already are: `DayTimeline`, `WakeSleepRow.kt:42`, `ZoneRow.kt`, `ZoneSheet.kt`) and build the string from Latin digits plus a localized marker, so Baloo2 always has the glyphs:

- Compute `hour12` / `minute` / AM-vs-PM from `minutes.mod(DayBounds.MINUTES_PER_DAY)` — pure arithmetic, no `Calendar.getInstance()` (which currently drags today's date and timezone into what should be a pure clock format).
- Format via a new `onboarding_clock_format` string (`"%1$d:%2$02d %3$s"`) so RTL locales can reorder, with the marker from `onboarding_clock_am` / `onboarding_clock_pm`.
- Number formatting uses `Locale.ROOT` so digits stay Latin in every locale. This is a deliberate call: Baloo2 renders Latin digits, and mixed-script fallback inside one label is what causes the visual jump. Note it with a `ponytail:` comment.
- Add all three keys to `values/strings.xml` **and** `values-ar/strings.xml` (marker `ص` / `م`).

New style in `core/design-system/AwanStyles.kt`, next to `headingText`:

```kotlin
val clockText = Style { textStyle(typography.heading); contentColor(colors.textPrimary) }
```

Use it for every clock render; pass `maxLines = 1` at the call sites so a longer string ("11:30 PM" vs "7:00 AM") can never reflow the row.

---

## 3. Task-length slider

`ui/components/TaskLengthSlider.kt` — the root-cause fix is to derive knob and fill from **one** animated value instead of two:

```kotlin
val fraction by animateFloatAsState(
    targetValue = index / (count - 1f),
    animationSpec = AwanTheme.motion.settle.spec(),
    label = "sliderFraction",
)
val knobOffset = with(density) { (fraction * usable).toDp() }
val filledWidth = with(density) { (knobPx / 2f + fraction * usable).toDp() }
```

They are now the same number; desync is structurally impossible. This also puts the slider on `AwanTheme.motion` — it was the one animation in the feature still using a default spring.

Also in this file:
- Track `onSelect` calls: keep a `lastIndex` across the drag and only emit when the index actually changes, in both `detectTapGestures` and `detectHorizontalDragGestures`. Today it fires on every pointer move.
- Fire `HapticFeedbackType.SegmentFrequentTick` on each index change (drag *and* tap), via `LocalHapticFeedback.current`.
- Add `Modifier.progressSemantics(index.toFloat(), 0f..(count - 1f), count - 2)` plus a `stateDescription` of `humanDuration(selected)` — the control is currently invisible to accessibility services.
- `tickLabel` (`:105-109`) hardcodes `"30m"` / `"3h"` and otherwise prints raw minutes. Move to `strings.xml` (`onboarding_tick_minutes` / `onboarding_tick_hours`) in both locales.

`RollingDuration` (`steps/TaskLengthStep.kt:69-83`) stays as-is; its container resize is inherent to `AnimatedContent` on variable-width text and is not what the user reported.

---

## 4. Haptics in the design system

New `core/design-system/.../AwanHaptics.kt`:

```kotlin
fun awanButtonHaptic(variant: AwanButtonVariant): HapticFeedbackType = when (variant) {
    AwanButtonVariant.Primary -> HapticFeedbackType.Confirm
    AwanButtonVariant.Secondary -> HapticFeedbackType.ContextClick
    AwanButtonVariant.Destructive -> HapticFeedbackType.Reject
    AwanButtonVariant.Quiet -> HapticFeedbackType.SegmentTick
}
```

`AwanButton.kt` — add a nullable `haptic` parameter to **both** overloads (`:44`, `:135`), defaulted from the variant, and wrap the `clickable` `onClick` at `:99`:

```kotlin
@Composable
fun AwanButton(
    onClick: () -> Unit,
    ...
    variant: AwanButtonVariant = AwanButtonVariant.Primary,
    haptic: HapticFeedbackType? = awanButtonHaptic(variant),   // pass null to turn haptics off
    ...
)

onClick = { haptic?.let(hapticFeedback::performHapticFeedback); onClick() }
```

**Toggling haptics off is per-call-site**: `AwanButton(onClick = ..., haptic = null) { ... }` gives a completely silent button. Passing a different `HapticFeedbackType` overrides the variant default without touching the variant. One nullable parameter covers both "off" and "something else" — no separate boolean flag.

Apply the same parameter to `AwanIconButton.kt` so every button in the app routes through one place rather than each caller re-adding it.

**Then remove the now-duplicate feature haptics**: `OnboardingScreen.kt:100` fires `Confirm` on step change, but a step change is *caused* by pressing the Primary button — after this change that click buzzes twice. Delete the step-change one; keep the celebration one at `:104` (not button-driven). `ZoneReorderList.kt` haptics are drag/toggle gestures, not buttons — leave them.

These `HapticFeedbackType` constants are already in use at `ZoneReorderList.kt:56-161`, so the Compose version in the catalog supports them.

---

## 5. Move `CascadeItem` to `core:design-system`

Move two files into `core/design-system/src/main/java/com/awan/app/core/designsystem/`, package `com.awan.app.core.designsystem`:

- `CascadeItem.kt` — unchanged logic.
- `Motion.kt` → `AwanReducedMotion.kt`, keeping the `reducedMotion()` name. It only reads `Settings.Global.ANIMATOR_DURATION_SCALE`, which is fine in an Android library module. Moving it (rather than duplicating) is required — `CascadeItem` depends on it, and so do `StepScaffold.kt:204`, `ZoneReorderList.kt:75`, `SparkleBurst.kt:30`.

No build-file change: `core:design-system` already exposes compose foundation via `api()`, and `feature:onboarding:impl` already depends on it.

Expand the existing KDoc on `CascadeItem` with a usage snippet, since it's now a shared API:

```kotlin
/**
 * Reveals one body element, delayed by [index] steps, so a screen's content arrives in sequence
 * rather than all at once. Snaps instantly when the user has system animations turned off.
 *
 * Give siblings consecutive indices, starting at 0. Each item fades in and rises [CascadeRise],
 * staggered by `AwanTheme.motion.staggerMillis`. The reveal replays whenever the composable
 * enters a fresh subcomposition — e.g. each step of a wizard.
 *
 * ```
 * Column {
 *     CascadeItem(0) { Headline("Set your day") }
 *     CascadeItem(1, Modifier.fillMaxWidth()) { WakeRow() }
 *     CascadeItem(2, Modifier.fillMaxWidth()) { DayTimeline(preview) }
 * }
 * ```
 */
```

Import updates only (`com.awan.feature.onboarding.impl.ui.components.CascadeItem` → `com.awan.app.core.designsystem.CascadeItem`) in the six step files, `StepScaffold.kt`, `ZoneReorderList.kt`, `SparkleBurst.kt`.

---

## Files touched

**`core/design-system/src/main/java/com/awan/app/core/designsystem/`**
- `AwanHaptics.kt` (new), `CascadeItem.kt` (moved in), `AwanReducedMotion.kt` (moved in)
- `AwanButton.kt`, `AwanIconButton.kt` — haptic param
- `AwanStyles.kt` — `clockText`

**`feature/onboarding/impl/src/main/java/com/awan/feature/onboarding/impl/ui/`**
- `components/DayTimeline.kt` (replaces `components/DayPreview.kt`)
- `components/TaskLengthSlider.kt`, `TimeFormat.kt`
- `components/CascadeItem.kt`, `Motion.kt` — deleted
- Import-only edits: `steps/*.kt` (6), `components/StepScaffold.kt`, `components/ZoneReorderList.kt`, `components/SparkleBurst.kt`, `OnboardingPreviews.kt`
- `OnboardingScreen.kt` — drop the duplicate step-change haptic

**`feature/onboarding/impl/src/main/res/`** — `values/strings.xml` and `values-ar/strings.xml`, same keys in both: clock format + AM/PM markers, slider tick labels, any new timeline caption.

---

## Verification

1. `./gradlew :core:design-system:assembleDebug :feature:onboarding:impl:assembleDebug` — compiles.
2. `./gradlew detekt lint` — clean; lint's `MissingTranslation` catches any key added to `values/` but not `values-ar/`.
3. `./gradlew :core:design-system:testDebugUnitTest` — run the existing suite first to confirm the harness works before adding anything.
4. Compose previews in `OnboardingPreviews.kt` (and add a `DayTimeline` preview with zones on/off) — check the timeline in light, dark, and RTL (`@Preview(locale = "ar")`), and that no clock label falls back to a non-Baloo2 font.
5. On device (`./gradlew installDebug`), walk the flow:
   - **DayBounds** — change wake and sleep repeatedly; the clock must keep the same font, size and format at every value, and the row must not reflow.
   - **Zones** — the step should now fit without the timeline dominating; toggling and reordering zones animates the segments horizontally.
   - **Task length** — drag slowly across all stops: fill edge and knob stay glued together, one tick of haptic per stop, no double-fire.
   - **Buttons** — Primary/Secondary/Destructive/Quiet each feel distinct; advancing a step buzzes exactly once.
   - Turn system animations off (Developer options → Animator duration scale 0) and confirm the cascade snaps rather than animating.

---

## Implementation notes (what actually differed)

**Status:** `./gradlew assembleDebug detekt lint testDebugUnitTest` — all green. Not yet run on a device; every item under "Verification → On device" is still outstanding.

### Deviations from the plan

- **Endpoint tinting dropped.** The plan kept the `Sunrise` / moonlight colours on the two clock labels. The Styles API has no style-composition operator (`AwanText` takes a single `Style`, unlike `Modifier.styleable`'s varargs), so tinting would have meant either raw `BasicText` — reintroducing the second render path this change exists to remove — or two near-duplicate styles. Both clocks now render through `AwanStyles.clockText`; the ☀️/🌙 glyphs carry the meaning. `Sunrise`/`Moonlight` constants deleted.
- **`formatClock` split.** Rather than one `@Composable`, the 24h→12h arithmetic is an internal pure `clockParts(minutes): ClockParts` with `formatClock` doing only resource lookup and `String.format`. That made the edge cases unit-testable on the JVM — `TimeFormatTest` covers midnight/noon reading 12 rather than 0, the 11:59→13:00 meridiem flip, and negative/overflow wrapping.
- **Slider gesture staleness.** The plan's "keep a `lastIndex` across the drag" would have captured a stale `index` and `onSelect`: `pointerInput` lambdas outlive the composition that creates them. `index` and `onSelect` are read through `rememberUpdatedState`, and `pointerInput` is keyed on `usable` (not `count`) so the geometry captured by `indexFromX` invalidates when the track is re-measured. **Easy to reintroduce** — the naive version compiles, runs, and only misbehaves after a width change or a fast drag.
- **`tickLabel` generalized.** The plan said move the hardcoded `"30m"`/`"3h"` to strings; the replacement also drops the hardcoding, picking hours vs minutes from `minutes % 60 == 0` instead of matching the two literal values 30 and 180. New stops now label themselves.
- **Task marker.** Rendered as a 3.dp full-height surface-coloured marker on the track with the ★ label as a caption under the endpoints, not a ★ dot on top of the marker — a dot at 44.dp track height had nowhere to sit without overlapping a zone label. Low contrast in dark mode (surface `#17364C` on `SkyNight`); the caption carries the meaning, so left as is.
- **Import ordering.** Moving `CascadeItem`/`reducedMotion` into `com.awan.app.core.designsystem` put the rewritten imports out of ktlint order in all ten consuming files; the import blocks were re-sorted. No behaviour change, but it is most of the line count in those files' diffs.

### Runtime traps worth remembering

- **The step-change haptic double-fire.** `OnboardingScreen` fired `Confirm` in a `LaunchedEffect(state.step)`. Once `AwanButton` owns haptics, pressing Continue buzzes twice — the button, then the step change it caused. Deleted, with a comment saying why. Any future "buzz on X" effect where X is button-driven has the same problem.
- **`Modifier.offset` vs `absoluteOffset`.** The timeline is positioned entirely by `offset(x = …)`, which is layout-direction aware, so RTL flips for free. Switching to `absoluteOffset` would silently mirror the day backwards in Arabic. Two `locale = "ar"` previews were added to catch it.
- **Baloo2 has no Arabic-Indic glyphs.** This was the actual cause of the reported "the font changes as the time changes": `SimpleDateFormat` with `Locale.getDefault()` emitted locale digits, which fell back to a system font mid-label. Any future user-facing number rendered in a Baloo2 style must be formatted with `Locale.ROOT`, or it will regress in exactly the same way.
