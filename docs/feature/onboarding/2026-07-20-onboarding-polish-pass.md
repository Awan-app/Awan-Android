# AWAN-47 follow-up: six onboarding polish fixes

## Context

The onboarding flow (branch `feature/AWAN-47-onboarding-flow`, built per
`docs/feature/onboarding/2026-07-20-motion-and-zones-plan.md`) works end to end, but six
issues remain before it reads as finished:

1. Reordering zones moves rows without moving their **times**, so the list order and the day
   timeline disagree.
2. The Day Preview gradient is washed out in the middle, near-black on the right, and identical
   in dark theme; the centre of the bar shows a dead placeholder string.
3. The task-length hint card is static regardless of the chosen length.
4. The Welcome ↔ Name transition visibly jumps.
5. The task-length slider knob track and its tick labels don't line up.
6. The back chevron is a hardcoded light-theme navy, so in dark theme it reads as disabled.

Six independent fixes, each at its root cause. No new abstractions.

---

## 1. Reorder must resequence zone windows

**Root cause:** `OnboardingViewModel.reordered()` (`OnboardingViewModel.kt:166`) only permutes the
list. `Zone.startMinutes`/`endMinutes` are untouched, so a zone dragged to position 1 keeps its old
afternoon window.

**Fix — `core/domain/.../onboarding/ZoneEditRules.kt`**, alongside the existing `editWindow` /
`overlappingZoneIds`, reusing the private `linear`/`absolute` helpers already in that file:

```kotlin
/**
 * Re-lay the zones back-to-back in list order. Each zone keeps its own duration; the chain is
 * anchored at the earliest start currently occupied, so the wake-up routine margin survives.
 */
fun resequence(zones: List<Zone>, bounds: DayBounds): List<Zone> {
    var cursor = zones.minOfOrNull { linear(it.startMinutes, bounds.wakeMinutes) } ?: return zones
    return zones.map { zone ->
        val start = absolute(cursor, bounds.wakeMinutes)
        cursor += zone.durationMinutes
        zone.copy(startMinutes = start, endMinutes = start + zone.durationMinutes)
    }
}
```

Disabled zones stay in the chain — "starts after the previous zone's end" applied uniformly is
predictable, and the existing UI already dims rather than removes them.

**`OnboardingViewModel.kt:61`:**

```kotlin
is OnboardingAction.ReorderZone -> updateZones {
    ZoneEditRules.resequence(reordered(action.fromIndex, action.toIndex), _state.value.bounds)
}
```

Nothing else changes — `updateZones` already recomputes `overlappingZoneIds`, and `DayPreviewModel`
already derives fractions from zone times, so the timeline animates to the new layout for free via
the `animateFloatAsState` already in `DayTimeline.ZoneSegments`.

---

## 2. Day Preview gradient + centre text

**`Tokens.kt`** — add four fields to `AwanColors` (individual `Color`s, not a `List`, to keep the
class stable) with light and dark values:

| field | Light | Dark |
|---|---|---|
| `skyDawn` | `#FFD9A8` | `#6B4A2E` |
| `skyMorning` | `#CFE8FF` | `#24455F` |
| `skyMidday` | `#A6D2FA` | `#1B3A55` |
| `skyDusk` | `#6E7FB8` | `#2A2F52` |

**`ui/components/DayTimeline.kt`:**
- Delete the private `SkyDawn` / `SkyDay` / `SkyNight` constants.
- Track background becomes the four-stop horizontal gradient from the tokens
  (`0f`, `0.28f`, `0.62f`, `1f`), plus a vertical overlay (`Color.White` 8% → `Color.Black` 10%)
  drawn on top for depth.
- Delete the top-right `onboarding_day_preview_open_sky` `AwanText` from the header row; keep
  `onboarding_day_preview_label` ("YOUR DAY").
- `HintPill` renders `onboarding_day_preview_open_sky` with `wakingHours(preview)` instead of
  `onboarding_day_preview_zones_hint`. Its `showZones && zones.any { it.enabled }` branch is
  unchanged, so the pill shows on DayBounds (`showZones = false`) and the segments own the bar on
  Zones.
- Replace the pill's `Color.White.copy(alpha = 0.7f)` with `AwanTheme.colors.surface.copy(alpha = 0.82f)`
  so it isn't a white chip on a dark bar.

**Strings** — `onboarding_day_preview_zones_hint` becomes unused: delete from both
`res/values/strings.xml` and `res/values-ar/strings.xml`.

---

## 3. Task-length hint reacts to the chosen length

Three buckets. In `res/values/strings.xml` and `res/values-ar/strings.xml`, replace
`onboarding_task_length_hint` with:

- `onboarding_task_length_hint_short` (≤ 45) — short and light, more blocks, easier to start
- `onboarding_task_length_hint_balanced` (60–90) — deep enough to matter, short enough to fit
- `onboarding_task_length_hint_deep` (≥ 120) — long and deep, split into linked sessions with breaks

Arabic translations required for all three (project rule: every key in every locale).

**`ui/steps/TaskLengthStep.kt`** — in the `CascadeItem(3)` card, resolve the key with a `when` on
`state.preferredTaskLengthMinutes` and wrap the `AwanText` in a `Crossfade` so it changes with the
same feel as the existing `RollingDuration`. `Crossfade`, not `AnimatedContent`: the card is
multi-line and a size transform would jitter its height.

---

## 4. Welcome transition jump

**Root cause:** `StepScaffold` swaps the body slot's *layout rules* on the same frame the step
changes — `StepScaffold.kt:111` flips `verticalArrangement` between `Center` and `Top`, and
`StepScaffold.kt:120-125` swaps the body `Box` between `Modifier.fillMaxWidth()` (Welcome) and
`Modifier.weight(1f).fillMaxWidth()` (everything else). `AnimatedContent` keeps the outgoing body
composed for the full transition, so during Name → Welcome the still-composed Name body loses its
height bound; being `verticalScroll`, its intrinsic height is its full content height, which blows
up the `AnimatedContent` measurement and shoves Welcome's content out of the `clipToBounds()`
region for the duration. Forward (Welcome → Name) hits the mirror image, and the mascot's
`animateBounds` is chasing a target that moved discontinuously.

**Fix — make the body slot's rules constant and express "centred" as an animatable property.**

`ui/StepChrome.kt` — replace `centeredContent: Boolean` with `leadingSpace: Dp = 0.dp`; Welcome
sets `leadingSpace = 64.dp` (tuning knob — adjust once on device).

`ui/components/StepScaffold.kt` — inside `LookaheadScope`, the content column becomes:

```kotlin
val lead by animateDpAsState(chrome.leadingSpace, AwanTheme.motion.settle.spec(), label = "lead")
Column(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {   // arrangement always Top
    Spacer(Modifier.height(lead))
    Mascot(...)                                               // unchanged, still animateBounds
    Box(Modifier.weight(1f).fillMaxWidth()) { body() }        // rules now constant for all steps
}
```

The body slot is always height-bounded, so both `AnimatedContent` children measure identically and
neither can overflow. The mascot+text cluster still reads as centred on Welcome, and the spacer
*animates* to 0, so the mascot glides rather than snaps — `animateBounds` now tracks a continuous
target.

`ui/steps/WelcomeStep.kt` keeps `scrollable = false` and its current arrangement; it now simply
sits top-aligned under the mascot inside the constant slot.

`ui/OnboardingScreen.kt` — add `sizeTransform = null` to the `AnimatedContent` `transitionSpec`.
Both children are now the same size, so the default size animation has nothing to do; removing it
is cheap insurance against a residual half-frame of resize.

---

## 5. Slider tick alignment

**Root cause:** the knob's centre travels `[KnobSize/2, width − KnobSize/2]`
(`TaskLengthSlider.kt:74`), but the tick labels are a separate `Row` with
`Arrangement.SpaceBetween` (`:128`), which aligns the outer labels by their *edges* at `0` and
`width` and distributes the rest by gap. The two coordinate systems never match.

**Fix — `ui/components/TaskLengthSlider.kt`:** move the label row *inside* the existing
`BoxWithConstraints` (widen it to `height(KnobSize + labelRow)`) so the labels reuse the same
`knobPx` / `usable` values already computed there, and position each label centred on its stop:

```kotlin
options.forEachIndexed { i, minutes ->
    val centre = with(density) { (knobPx / 2f + usable * i / (count - 1)).toDp() }
    Box(Modifier.offset(x = centre - LabelSlot / 2).width(LabelSlot), contentAlignment = Alignment.Center) {
        AwanText(tickLabel(minutes), style = ..., maxLines = 1)
    }
}
```

`LabelSlot = 48.dp` comfortably fits every `tickLabel` output ("30m" … "3h"). `Modifier.offset` is
RTL-aware and matches how the knob is already positioned, so Arabic stays correct.

---

## 6. Back chevron colour

**Root cause:** `StepScaffold.kt:319` hardcodes `private val ink = Color(0xFF16455E)` — the light
theme's ink. In dark theme the chevron stays dark navy on a dark secondary button and reads as
disabled.

**Fix:** `AwanButton` already provides `LocalContentColor` (`AwanButton.kt:118`) with the correct
per-variant, per-enabled-state, animated colour. `BackChevron` reads
`LocalContentColor.current` and passes it to `drawPath`; delete the `ink` constant. Root-cause fix
— it also picks up the disabled and pressed states for free.

---

## Files touched

- `core/design-system/.../Tokens.kt` — four sky colours in `AwanColors`, light + dark
- `core/domain/.../onboarding/ZoneEditRules.kt` — `resequence`
- `feature/onboarding/impl/.../presentation/OnboardingViewModel.kt` — reorder routes through `resequence`
- `feature/onboarding/impl/.../ui/StepChrome.kt` — `centeredContent` → `leadingSpace`
- `feature/onboarding/impl/.../ui/OnboardingScreen.kt` — `sizeTransform = null`
- `feature/onboarding/impl/.../ui/components/StepScaffold.kt` — constant body slot, animated lead spacer, themed chevron
- `feature/onboarding/impl/.../ui/components/DayTimeline.kt` — gradient, centre pill
- `feature/onboarding/impl/.../ui/components/TaskLengthSlider.kt` — tick alignment
- `feature/onboarding/impl/.../ui/steps/TaskLengthStep.kt` — reactive hint
- `feature/onboarding/impl/src/main/res/values/strings.xml` + `values-ar/strings.xml` — 3 added, 2 removed

Untouched: `OnboardingState`, `OnboardingAction`, `OnboardingEvent`, `OnboardingStep`,
`SuggestZoneScheduleUseCase`, `DayPreviewModel`, `ZoneRow`, `ZoneSheet`, navigation.

---

## Verification

**Tests (extend, don't create suites):**
- `core/domain/src/test/.../ZoneEditRulesTest.kt` — `resequence` keeps every duration, produces a
  contiguous chain, anchors at the earliest original start, and is a no-op on an already-ordered
  contiguous schedule. One case with a wake time that makes the chain cross midnight.
- `feature/onboarding/impl/src/test/.../OnboardingViewModelTest.kt` — `ReorderZone(0, 2)` changes
  the moved zone's `startMinutes` and leaves `overlappingZoneIds` empty.

**Commands:**
```
./gradlew :core:domain:testDebugUnitTest --tests "*ZoneEditRulesTest"
./gradlew :feature:onboarding:impl:testDebugUnitTest
./gradlew assembleDebug detekt lint
```
Per the global workflow rule, run the two existing test files above *before* adding cases, to
confirm the harness is green.

**On device — the five visual fixes cannot be asserted by unit tests:**
1. Welcome → "Let's go" → back. Mascot glides both ways; no jump, no clipped text, no flash of
   oversized content. Repeat fast to catch mid-transition state.
2. DayBounds: bar shows the new gradient with "N h of open sky" centred; change wake/sleep and
   confirm the number tracks. Repeat in dark theme.
3. Zones: drag Personal to the top → its time range becomes the earliest and every row's times
   re-chain contiguously; the timeline segments animate to match.
4. TaskLength: drag across all six stops — each tick label sits directly under the knob, and the
   card copy switches at 60 and at 120.
5. Dark theme, any step past Welcome: the back chevron is clearly visible sky-blue, not navy.

**On completion**, append `## Implementation notes (what actually differed)` to
`docs/feature/onboarding/<today>-onboarding-polish-pass.md` (this plan, saved to the feature folder
before work starts, per the project convention).

---

## Implementation notes (what actually differed)

**Status.** All six fixes implemented. `assembleDebug`, `detekt`, `:core:domain:test`,
`:feature:onboarding:impl:testDebugUnitTest` all green. `lint` is green once
`core/design-system/src/androidTest/.../TempLibraryBugProofTest.kt` is excluded — that file is
untracked scratch work that predates this pass and fails `RememberInComposition`; it was not
touched. **The five visual fixes have not been verified on a device** — the on-device checklist
above is still outstanding.

**Deviations from the plan as written:**

- `AnimatedContent` has no `sizeTransform` parameter on its `transitionSpec` lambda; the size
  transform is attached to the `ContentTransform` with the `using` infix. `OnboardingScreen.kt`
  therefore ends its spec with `) using null` rather than passing a named argument.
- `DayTimeline`'s header collapsed from a `SpaceBetween` `Row` to a bare `AwanText`, since removing
  the top-right open-sky label left only one child. `HintPill` was renamed `OpenSkyPill` — it is no
  longer a hint.
- The vertical depth overlay is a second `Modifier.background` stacked on the gradient rather than a
  separate child. Two `background` modifiers compose in order, so it needs no extra `Box`.
- `TaskLengthSlider` lost its outer `Column`: the `BoxWithConstraints` moved to the top so the label
  row can read the same `knobPx`/`usable`, and the `Column` now lives inside it. The gesture `Box`
  picked up the explicit `height(KnobSize)` the `BoxWithConstraints` used to supply.
- `hintFor()` in `TaskLengthStep.kt` is a plain `@StringRes` function, not a composable — the
  `stringResource` call happens inside the `Crossfade` content.

**Traps worth knowing:**

- **The body slot's measurement rules must stay identical on every step.** This was the whole cause
  of the Welcome jump: `AnimatedContent` keeps the outgoing body composed for the full transition,
  and every step body except Welcome's is `verticalScroll`. A scrolling body that loses its height
  bound reports its entire content height, which blows the region past `clipToBounds()` and shoves
  the incoming content off-screen. Any future "just center this one step" that reaches for a
  conditional `Modifier.weight` or a per-step `verticalArrangement` reintroduces it. Use
  `StepChrome.leadingSpace` — an animated value, not a layout switch.
- `StepChrome.leadingSpace = 64.dp` on Welcome is an unverified eyeball figure. It is the one number
  in this pass that wants tuning on a real screen.
- `resequence` anchors at the *earliest occupied start*, not at `bounds.wakeMinutes`. That is what
  preserves `SuggestZoneScheduleUseCase`'s 30-minute wake-up routine margin across a reorder;
  anchoring at wake would silently eat it.
- The first attempt at the overnight `resequence` test asserted the wrong number (expected the second
  zone at 00:30 when the chain puts it at 23:30). The implementation was correct; the test was not.
  Zone ends stay unwrapped past 1440 when a zone crosses midnight, matching `editWindow`'s existing
  convention — `durationMinutes` depends on it.
- `BackChevron` now reads `LocalContentColor`, which `AwanButton` provides. Any Canvas-drawn glyph
  added to a button should do the same rather than naming a colour; the hardcoded `0xFF16455E` was
  invisible-ish in dark theme precisely because it bypassed that.
