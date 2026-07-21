# AWAN-47 (follow-up): Make onboarding one continuous, living flow + rebuild the Zones step

> Follows [`2026-07-18-implementation-plan.md`](./2026-07-18-implementation-plan.md), which covers the original AWAN-47 build and is already implemented. This plan is a motion/UX pass over that result plus a redesign of the Zones step. It **diverges** from the original plan's Zones design: the ↑/↓ reorder buttons and always-visible Starts/Ends chips described there are replaced by drag-and-drop and a bottom sheet (see Part B).

## Context

Two problems, one branch.

**1. The flow renders as seven standalone screens.** `OnboardingScreen.kt:86` wraps `AnimatedContent(targetState = state.step)` around the *entire* step, and each of the seven `ui/steps/*Step.kt` files calls `StepScaffold` itself. So on every step change the whole chrome — gradient background, back chevron, skip link, progress bar, footer CTA — is torn down and rebuilt in a new subcomposition, then slid and faded in as if it were new content.

Concretely: `AwanStepProgress` never animates, because its `animateColorAsState` (`AwanStepProgress.kt:36`) starts at its target value every time the composable is recreated. The primary CTA is a different `AwanButton` instance on every step, so it cannot read as one persistent object. `AwanMascot`'s `rememberInfiniteTransition` restarts from phase 0 at each transition, visibly resetting its float.

**2. The Zones step is unusable.** Every `ZoneCard` renders a drag handle, name, toggle, two reorder buttons, and two time chips in a `FlowRow` that wraps onto a second line (`ZoneCard.kt:78-92`). Four zones produce an enormous scroll, and the ↑/↓ buttons are a poor substitute for direct manipulation. Time editing should not be visible by default.

The goal is to invert scaffold ownership so shared chrome animates in place, layer motion so the app feels like it is engaging with the user, and rebuild the zones list around drag-and-drop with time editing behind a sheet.

### Decisions confirmed with the user

| Area | Decision |
|---|---|
| Mascot | Persists only on the four steps where it already appears; hidden without leaving composition on DayBounds/Zones/TaskLength |
| Welcome→Name mascot | **Flies** between positions via `LookaheadScope` + `Modifier.animateBounds`, shrinking 190dp→132dp. Both step layouts unchanged |
| Body transition | Staggered cascade, ~55ms apart |
| Motion extras | All four: progress spring + fill pulse, celebration burst, CTA reactions, live content reactions |
| Token scope | `AwanMotion` added to `core:design-system`, applied in onboarding. Other screens untouched |
| Zone drag trigger | **Both** — instant drag from the handle, and long-press-anywhere to pick up |
| Zone row | Two lines: name + toggle on top, time range beneath. Nothing truncates |
| Zone time editing | Bottom sheet with Starts/Ends buttons opening the existing `TimePickerDialog` |
| Discoverability | One-time wiggle on entry + lift feedback while dragging + haptics throughout |

---

# Part A — Structural fix and motion

## A1. Motion tokens — `core:design-system`

Motion literals are scattered across nine files; `120` is already duplicated between `AwanStyles.kt:33` and `:170`.

**`Tokens.kt`** — spring specs are generic over the animated type, and call sites need `Float`, `Dp`, `IntOffset`, and `IntSize`. Storing pre-built typed specs would mean near-duplicate fields per motion, so store the parameters and expose a typed factory:

```kotlin
@Immutable
data class AwanSpring(val dampingRatio: Float, val stiffness: Float) {
    fun <T> spec(): SpringSpec<T> = spring(dampingRatio, stiffness)
}

@Immutable
data class AwanMotion(
    val settle: AwanSpring = AwanSpring(0.85f, Spring.StiffnessMediumLow),
    val bouncy: AwanSpring = AwanSpring(0.55f, Spring.StiffnessLow),
    val playful: AwanSpring = AwanSpring(0.42f, Spring.StiffnessMediumLow),
    val fastMillis: Int = AWAN_BUTTON_ANIMATION_DURATION_MILLIS,
    val standardMillis: Int = 220,
    val emphasizedMillis: Int = 320,
    val staggerMillis: Int = 55,
)

internal val AwanMotionTokens = AwanMotion()
```

`settle` for in-place moves (mascot bounds, row reorder, timeline bands). `bouncy` for the progress fill, so it overshoots and settles. `playful` for the CTA pop and celebration.

**`AwanTheme.kt`** — mirror `spacing` exactly: add `val motion: AwanMotion` to `AwanThemeValues`, `motion = AwanMotionTokens` in the `values` construction, and the `@Composable @ReadOnlyComposable` accessor.

**`build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt`** — `Modifier.animateBounds` is experimental. Add alongside the two existing entries:

```kotlin
optIn.add("androidx.compose.animation.ExperimentalSharedTransitionApi")
```

**No Gradle dependency change.** `androidx.compose.animation` already resolves transitively — `AwanStepProgress.kt` imports `animateColorAsState` and `OnboardingScreen.kt` imports `AnimatedContent` today, with the BOM applied by the convention plugin. Add an explicit alias only if a strict-dependency lint later complains.

## A2. `AwanStepProgress` — continuous spring fill

Public signature `AwanStepProgress(current: Int, count: Int, modifier: Modifier)` stays unchanged, so no callsite churn.

Replace the per-segment `animateColorAsState` with **one** spring-animated fraction:

```kotlin
val progress by animateFloatAsState(
    targetValue = current.toFloat(),
    animationSpec = AwanTheme.motion.bouncy.spec(),
    label = "progress",
)
```

Segment `i` draws the `line` track with a `sky` fill whose width fraction is `(progress - i).coerceIn(0f, 1f)`. The low damping ratio makes the fill spill past the newly completed segment and settle back — the "emitted from one step to the next, spring-loaded" effect. It works now only because the composable persists (§A3).

**Fill pulse:** an `Animatable` scaleY driven by `LaunchedEffect(current)` gives the just-completed segment a brief squash-and-release, applied via `graphicsLayer` on that segment only.

**Implementation note:** `Modifier.fillMaxWidth(fraction)` rejects `0f`. Draw the fill with `drawBehind` instead — reading `progress` inside a draw lambda also defers it to the draw phase, avoiding a recomposition per frame.

## A3. Invert scaffold ownership

Target shape — everything outside `AnimatedContent` is composed once and never leaves composition:

```
OnboardingScreen                    ← owns state, computes StepChrome
└── StepScaffold                    ← PERSISTENT
    ├── background gradient
    ├── header row (back + skip)    ← children animate in/out
    ├── AwanStepProgress            ← springs in place
    ├── LookaheadScope
    │   ├── AwanMascot              ← animateBounds
    │   └── AnimatedContent(step)   ← the ONLY animated region
    │       └── StepBody            ← cascade-staggered children
    └── footer                      ← one AwanButton + animated secondary
```

### `ui/StepChrome.kt` (new)

```kotlin
@Immutable
data class StepAction(val label: String, val onClick: () -> Unit)

@Immutable
data class StepChrome(
    val progressCurrent: Int,
    val primary: StepAction,
    val primaryEnabled: Boolean = true,
    val showBack: Boolean = true,
    val onSkip: (() -> Unit)? = null,
    val secondary: StepAction? = null,
    val mascot: MascotExpression? = null,
    val mascotWidth: Dp = 0.dp,
)

@Composable
fun stepChrome(state: OnboardingState, onAction: (OnboardingAction) -> Unit): StepChrome
```

One `when (state.step)` returning the seven rows below — the existing per-step configuration, transcribed verbatim into one readable table instead of scattered across eight scaffold arguments in seven files. A composable returning a value with a lowercase name matches the existing `buttonContentColor` in `AwanButton.kt`.

| Step | back | skip | progress | mascot | primary | secondary |
|---|---|---|---|---|---|---|
| Welcome | no | — | 0 | Greet 190dp | `onboarding_welcome_lets_go` | `onboarding_welcome_skip_setup` → Skip |
| Name | yes | Skip | dotIndex | Greet 132dp | `onboarding_continue`, enabled=`canContinueName` | — |
| DayBounds | yes | Skip | dotIndex | — | `onboarding_continue`, enabled=`canContinueBounds` | — |
| Zones | yes | Skip | dotIndex | — | `onboarding_zones_use_this` | `onboarding_zones_reset` → UseSuggestedZones |
| TaskLength | yes | Skip | dotIndex | — | `onboarding_continue` | — |
| FirstTask | yes | Skip | dotIndex | Curious/Celebrate 148dp | dynamic (scheduling / continue / add) | `onboarding_first_task_skip` → Skip |
| Notifications | yes | — | dotIndex | Idle 136dp | `onboarding_notifications_turn_on` → EnableNotifications | `onboarding_notifications_not_now` → Next |

FirstTask computes `landed = state.firstTask != null` locally and derives label, `onClick` (`if (landed) Next else SubmitFirstTask`) and `enabled` (`landed || canSubmitFirstTask`) from it — same logic as today.

### `ui/components/StepScaffold.kt` — rewritten

```kotlin
@Composable
fun StepScaffold(
    chrome: StepChrome,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
)

@Composable
fun StepBody(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    arrangement: Arrangement.Vertical = Arrangement.spacedBy(AwanTheme.spacing.sm),
    content: @Composable ColumnScope.() -> Unit,
)
```

`StepBody` is the old scaffold's content `Column` lifted out — same `verticalScroll(rememberScrollState())` and arrangement logic, now *inside* the animated region so Welcome's `Arrangement.Center` + non-scroll stays per-step without the persistent scaffold knowing about it. `StepHeadline` and `CenteredHeadline` are unchanged.

Region behaviour:

- **Header** — `Box(Modifier.size(38.dp)) { AnimatedVisibility(chrome.showBack) { ... } }`. The fixed-size `Box` replaces today's conditional `Spacer`, so the chevron fades without displacing the skip link. Skip side uses `AnimatedVisibility(chrome.onSkip != null)`; read `chrome.onSkip` *inside* the click lambda so a click mid-exit is a correct no-op.
- **Progress bar** — always composed. On Welcome (`progressCurrent == 0`) animate its height and alpha to zero rather than branching it out of the tree. Wrapping it in `AnimatedVisibility` would dispose the instance across Welcome↔Name and lose exactly the continuity this change exists to create.
- **Footer** — one `AwanButton(onClick = chrome.primary.onClick, enabled = chrome.primaryEnabled, modifier = Modifier.fillMaxWidth())` for the entire flow, its content wrapped in `Crossfade(chrome.primary.label)`. `Crossfade` rather than `AnimatedContent` because the label is single-line at fixed width — there is no size to transform and `Crossfade` won't jitter the button height.
- **Secondary link** — `AnimatedContent(targetState = chrome.secondary)`, *not* `AnimatedVisibility`. `AnimatedVisibility(visible = secondary != null)` would force retaining the last non-null label so it doesn't blank out mid-exit; `AnimatedContent` keeps outgoing and incoming composed simultaneously and handles `null→X`, `X→null`, and `X→Y` with no retained state.
- **CTA pop** — `LaunchedEffect(chrome.primaryEnabled)`: on a `false → true` flip, run an `Animatable` 1f → 1.06f → 1f with `motion.playful`, applied via `graphicsLayer`. This is the moment the app notices you finished typing your name.

### The flying mascot

The mascot must sit **outside** `AnimatedContent` — that composable builds separate subcompositions for outgoing and incoming content, so nothing inside it can persist. Hoist it into the content region as the first child, inside `LookaheadScope`:

```kotlin
LookaheadScope {
    AwanMascot(
        expression = chrome.mascot ?: MascotExpression.Idle,
        width = animatedWidth,
        modifier = Modifier.animateBounds(this@LookaheadScope),
    )
    AnimatedContent(targetState = step, ...) { body(it) }
}
```

Welcome uses `Arrangement.Center` and the others align to the top, so when the arrangement flips the mascot's measured position changes and `animateBounds` animates along that change. That produces the fly-and-shrink, and both step layouts keep their designed composition because neither had to move.

**Critical:** on DayBounds/Zones/TaskLength the mascot must be hidden **without leaving composition** — otherwise it loses its animation state and the fly back into FirstTask breaks. Do not use `AnimatedVisibility`; animate `width` to `0.dp` via `animateDpAsState(motion.settle.spec())` and alpha to `0f` in a `graphicsLayer`. Expression changes crossfade.

### `ui/OnboardingScreen.kt`

Signature unchanged, so all seven previews keep working:

```kotlin
@Composable
fun OnboardingScreen(state: OnboardingState, onAction: (OnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    BackHandler { onAction(OnboardingAction.Back) }
    StepScaffold(chrome = stepChrome(state, onAction), onBack = { onAction(OnboardingAction.Back) }, modifier = modifier) {
        AnimatedContent(targetState = state.step, label = "onboardingStep", transitionSpec = { ... }) { step ->
            when (step) { ... }
        }
    }
}
```

Give the `AnimatedContent` and each step body `Modifier.fillMaxSize()` so the two children measure identically and `SizeTransform` stays a no-op.

`OnboardingRoot`'s permission plumbing is untouched.

## A4. Step bodies and the cascade

Each of the seven step files drops its `StepScaffold` call, its footer lambda, and its mascot `Column`, keeping only its content inside `StepBody { ... }`. `WelcomeStep` passes `scrollable = false, arrangement = Arrangement.Center`; the rest use defaults. `WelcomeStep`'s signature drops to `WelcomeStep()` — its only two actions were both footer buttons. Net deletion of roughly 15 lines per file.

### `ui/components/CascadeItem.kt` (new)

```kotlin
@Composable
fun CascadeItem(index: Int, content: @Composable () -> Unit)
```

Each body enters a fresh `AnimatedContent` subcomposition, so `remember` / `LaunchedEffect(Unit)` re-fire naturally on every step entry. Delay `index * motion.staggerMillis`, then spring alpha 0→1 and translationY from ~16dp with `motion.settle`, via `graphicsLayer`. Callsites pass explicit indices — no positional magic, no scope receiver.

**Exit** is one block (fade + slight downward drift, `motion.standardMillis`), set in the `AnimatedContent` `transitionSpec`. Staggered exits read as sluggish.

## A5. Live content reactions

- **`DayPreview.kt`** — `ZoneBands` computes `top`/`bandHeight` straight from the fractions, so bands jump when wake/sleep change. Wrap each in `key(block.id)` and animate `startFraction`/`endFraction` with `animateFloatAsState(motion.settle.spec())`. Same for the task pill.
- **`TaskLengthStep`** — wrap `humanDuration(...)` in an `AnimatedContent` with vertical slide + fade so the number rolls. `TaskLengthSlider.kt:56`'s knob `animateDpAsState` adopts `motion.settle`.

## A6. Celebration burst

`state.celebrateTask` exists in `OnboardingState.kt:22`, is set by the ViewModel, and is asserted in tests — but **no composable reads it**. The landed-card animation currently rides on `state.firstTask != null` instead.

**`ui/components/SparkleBurst.kt`** (new) — a `Canvas` drawing N particles radiating from a single `Animatable` 0f→1f driven by `LaunchedEffect(celebrate)`, with radial spread, gravity droop, and fade. Colours from the existing zone palette (`zoneSun`, `zoneTangerine`, `zoneViolet`) so it stays on-brand. Overlay on `LandedTaskCard`.

No ViewModel change: the burst fires on the transition into `true`, and the step is left afterwards, so the flag never needs resetting.

## A7. Optional style polish

Two small changes that make the persistent CTA read correctly, since a button that never leaves composition now visibly *animates* between enabled states rather than being replaced:

- **`AwanStyles.kt`** — wrap the `disabled { }` blocks of `primaryButtonRim` and `primaryButtonFace` in `animate(tween(AwanMotionTokens.standardMillis)) { ... }`. The Styles API `animate(spec) { }` overload takes a whole `Style` block, so `background` and `contentColor` both animate. Use `AwanMotionTokens` directly — `StyleScope` is not a composable scope, matching how `AWAN_BUTTON_ANIMATION_DURATION_MILLIS` is already used there.
- **`AwanButton.kt`** — `buttonContentColor` hard-switches, which would leave the label snapping while the background fades. Wrap it in `animateColorAsState(animationSpec = tween(AwanTheme.motion.standardMillis))`.

---

# Part B — Rebuild the Zones step

## B1. The compact row

Replace `ZoneCard` with a two-line row. Today's version is a `Column` containing a header `Row` plus a `FlowRow` of four controls that wraps; the new one is a fixed two-line layout that never truncates:

```
┌───────────────────────────────┐
│ ⠿  ● Study              ██● │
│      7:30 – 11:15 AM          │
│ ⠿  ● Work               ██● │
│      11:15 AM – 3:00 PM       │
└───────────────────────────────┘
```

New signature — the reorder callbacks collapse from index-stepping into a single move, and the time setters move to the sheet:

```kotlin
@Composable
fun ZoneRow(
    zone: Zone,
    overlapping: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
)
```

- **Line 1:** drag handle, colour dot, zone name, `Switch` (kept on the row — one-tap enable/disable, as chosen).
- **Line 2:** the time range, reusing the **existing** `onboarding_time_range` string and `formatClock` helper. No new formatting code.
- Disabled zones dim their dot and text; the overlap warning (`onboarding_zone_overlap`) stays, shown inline.
- `EditTimeChip`, `ReorderButton`, and the `FlowRow` are deleted. `DragHandle` is kept and promoted.

**Tap affordance.** You picked the wiggle and lift feedback but not the hint line or the trailing chevron, which leaves *drag* taught and *tap* not. Cheapest fix inside those constraints: style the line-2 time range as a tappable element — `sky`-coloured rather than `meta`-grey, on a faint rounded background. It reads as a control without adding width or a new string. Flagging this as my call, since none of the offered options covered it.

## B2. Drag and drop

Both triggers, as chosen. The list is four items in a plain `Column` inside `StepBody`'s `verticalScroll`, so gesture arbitration matters.

- **Handle drag** — `Modifier.pointerInput { detectDragGestures { ... } }` on the handle. It consumes the gesture immediately, so there is zero conflict with page scroll.
- **Long-press drag** — `detectDragGesturesAfterLongPress` on the row. Safe against the parent scroll because a scroll gesture would already have been claimed before the long-press timeout elapses, and safe against tap-to-edit because the tap resolves first.

State: `draggingIndex` and `dragOffsetY`. Rows are uniform height, so measure one via `onSizeChanged` rather than hardcoding, then `targetIndex = (draggingIndex + (dragOffsetY / rowHeight).roundToInt()).coerceIn(indices)`. On drop, emit the **existing** `OnboardingAction.ReorderZone(from, to)` — no ViewModel or action changes.

Non-dragged rows shift via `animateDpAsState(motion.settle.spec())`. The dragged row lifts: `graphicsLayer` translationY, scale ~1.04, plus shadow elevation.

**Entry:** rows arrive with `CascadeItem`, then the top row performs a single one-time nudge — handle shifts and springs back with `motion.playful` — via `LaunchedEffect(Unit)` so it runs once per step entry.

## B3. The time sheet

Tapping a row opens a `ModalBottomSheet` (material3, `@OptIn(ExperimentalMaterial3Api::class)` — same pattern `TimePickerDialog.kt:17` already uses). Contents: zone name and colour, a **Starts** button and an **Ends** button.

Each opens the **existing** `TimePickerDialog` unchanged — it already handles minutes-from-midnight, is already localized, and is already shared with the DayBounds step. Confirming calls the existing `OnboardingAction.EditZoneWindow(zoneId, start, end)`, which routes through `ZoneEditRules.editWindow` for snapping, the 15-minute minimum, and clamping. **No domain, ViewModel, action, or state changes anywhere in Part B.**

Dismissal is by scrim or swipe; no confirm button, so no new string for it.

`material3` is not declared in `feature/onboarding/impl/build.gradle.kts` but resolves transitively today (`ZoneCard.kt` imports `Switch`, `TimePickerDialog.kt` imports `AlertDialog`). If `ModalBottomSheet` fails to resolve, add `implementation(libs.androidx.compose.material3)` explicitly.

## B4. Haptics

Per the request, throughout the flow via `LocalHapticFeedback`:

| Moment | Feedback |
|---|---|
| Drag pickup | `GestureThresholdActivate` |
| Each reorder crossing | `SegmentFrequentTick` |
| Drop | `Confirm` |
| Zone toggle | `ToggleOn` / `ToggleOff` |
| Step advance | `Confirm` |
| First task lands | `Confirm`, alongside the sparkle burst |

## B5. Accessibility — required, not optional

Removing the ↑/↓ buttons **removes the only reorder path for screen-reader users**. This must be replaced, not dropped:

- Add `CustomAccessibilityAction`s for "move up" / "move down" on each row's semantics, calling the same `ReorderZone` action.
- `contentDescription` on the drag handle.
- The row's tap target announces that it opens hour editing.

Also fix the pre-existing gaps in this area: `onboarding_back` is defined in **both** locales but referenced nowhere — the back control is a Canvas-drawn `BackChevron` (`StepScaffold.kt:140`) with no `contentDescription`. The `⌄` chevron in `WakeSleepRow` is glyph text with no semantics.

**Reduced motion.** A flow this animated must honour the system setting. One helper reading `Settings.Global.ANIMATOR_DURATION_SCALE`; when it is `0`, cascade delays become `0`, the wiggle is skipped, and springs are replaced with `snap()`.

## B6. Strings

Parts A and the sheet need **no** new strings — all 13 footer/header labels already exist in both `values/strings.xml` and `values-ar/strings.xml`, and `onboarding_time_range`, `onboarding_zone_starts`, `onboarding_zone_ends`, `onboarding_zone_overlap` are reused as-is.

New keys required only for the accessibility work, added to **both** `res/values/strings.xml` and `res/values-ar/strings.xml`:

- `onboarding_zone_drag_handle`
- `onboarding_zone_move_up`
- `onboarding_zone_move_down`
- `onboarding_zone_edit_hours`

`onboarding_zone_starts` / `onboarding_zone_ends` survive — they move from the deleted chips into the sheet.

---

## Files touched

**`core:design-system`**
- `Tokens.kt` — `AwanSpring`, `AwanMotion`, `AwanMotionTokens`
- `AwanTheme.kt` — `motion` in `AwanThemeValues` + accessor
- `AwanStepProgress.kt` — spring-fill rewrite (signature unchanged)
- `AwanStyles.kt`, `AwanButton.kt` — animated disabled state (§A7)

**`build-logic`**
- `AndroidComposeConventionPlugin.kt` — `ExperimentalSharedTransitionApi` opt-in

**`feature/onboarding/impl`**
- `ui/OnboardingScreen.kt` — hosts the persistent scaffold
- `ui/StepChrome.kt`, `ui/components/CascadeItem.kt`, `ui/components/SparkleBurst.kt`, `ui/components/ZoneSheet.kt` — **new**
- `ui/components/StepScaffold.kt` — inverted, plus `StepBody`
- `ui/components/ZoneCard.kt` → `ZoneRow.kt` — rebuilt, drag-and-drop
- `ui/steps/*.kt` × 7 — body-only, net shorter; `ZonesStep` rebuilt
- `ui/components/DayPreview.kt`, `TaskLengthSlider.kt` — animated bands, knob spec
- `res/values/strings.xml` + `res/values-ar/strings.xml` — 4 a11y keys

**Unchanged — verify these stay untouched**
- `OnboardingViewModel`, `OnboardingState`, `OnboardingAction`, `OnboardingEvent`, `OnboardingStep`
- `core:domain` — `ZoneEditRules`, `SuggestZoneScheduleUseCase`
- `OnboardingEntryProvider.kt`, `AwanApp.kt` — still one Nav3 destination
- `ui/OnboardingPreviews.kt` — `OnboardingScreen`'s signature is preserved, so all seven previews work unmodified. They improve: each now renders the real persistent chrome instead of a step's private copy.
- `OnboardingViewModelTest.kt` — tests the ViewModel only. **Zero test changes expected.**

---

## Implementation order

1. `Tokens.kt` + `AwanTheme.kt` — compiles standalone, nothing consumes it yet.
2. `AwanStepProgress` spring rewrite; `AwanStyles`/`AwanButton` disabled animation. Verify against the existing `AwanButton` previews and `AwanButtonTest.kt`.
3. `ui/StepChrome.kt` — new file, no callers, so it compiles green while the seven-row table is transcribed and diffed against the old step files.
4. `StepScaffold` rewrite + `StepBody`. This breaks all seven step files; that is intentional and the compiler enumerates the work.
5. `OnboardingScreen` — single scaffold + `AnimatedContent`.
6. The seven step files in table order. **Stop here and verify chrome persistence is correct with no new animation yet** — this is the actual fix for the reported problem, and it should be reviewable on its own.
7. `CascadeItem` and body transitions.
8. Flying mascot (`LookaheadScope` + `animateBounds`) — after 4–6 are stable, since it is most likely to need tuning.
9. CTA pop, sparkle burst, live content reactions.
10. Zones rebuild: `ZoneRow`, drag-and-drop, sheet, wiggle.
11. Haptics, reduced motion, content descriptions, Arabic strings.

Steps 1–6 fix the reported structural problem. 7–11 are the polish and the Zones rebuild.

---

## Verification

**Build and static analysis** — all four must pass:
```
./gradlew assembleDebug
./gradlew detekt
./gradlew lint
./gradlew testDebugUnitTest
```
`lint` matters: `app/build.gradle.kts` sets `error += "HardcodedText"`, which catches any string that slips into Kotlin.

**Previews** — all seven in `OnboardingPreviews.kt` must render. Cheapest check that the inversion didn't break per-step layout.

**On device** — each item below is a specific regression risk, not a general smoke test:

1. Step forward through all seven. The progress bar fills **continuously with an overshoot** and never snaps or re-enters. The CTA never slides, fades, or flickers — it is one object throughout.
2. Welcome → Name: the mascot travels from centered to the top slot while shrinking, as one object. Welcome still looks vertically centered.
3. Name → DayBounds the mascot shrinks away; TaskLength → FirstTask it returns at 148dp. Its float never visibly resets.
4. Step **backward** through the flow — the bar drains with the same spring, cascade reads correctly in reverse.
5. On Name, type a first name: the CTA pops the instant it becomes enabled.
6. On FirstTask, submit: sparkle burst fires, mascot cheers, haptic confirms.
7. **Zones:** four rows fit without a wall of scroll; no time controls visible by default. Drag from the handle — immediate pickup, lift + shadow, others spring aside, haptic ticks per crossing. Long-press a row body — same result. Tap a row — sheet opens, set Starts and Ends, confirm the timeline bands glide to the new windows and the overlap warning appears when zones collide. Confirm the one-time wiggle fires on entry and does **not** repeat when returning to the step.
8. Developer options → **Animator duration scale → Off**: content appears instantly, no delays, nothing stuck invisible or at alpha 0, no wiggle.
9. Switch the device to **Arabic**: flow is fully localized; RTL must not break the flying mascot, the progress fill direction, or the drag axis.
10. **TalkBack**: back button, drag handle, and row are announced; the move-up/move-down custom actions are present and actually reorder — this replaces the ↑/↓ buttons being deleted.

---

## Implementation notes (what actually differed)

Status: **implemented**. Build, 24 unit tests, detekt, and lint all pass; the full flow was walked on an API 36 emulator. Three things only showed up at runtime and are worth recording, because each would be easy to reintroduce.

**1. Never constrain `AwanButton` to a fixed box.** The back chevron was first placed in a `Box(Modifier.size(38.dp))` so the slot could reserve width while the chevron faded. `AwanButton` applies `AwanTheme.styles.buttonFocus`, which sets `minWidth(48.dp)`/`minHeight(48.dp)`, so the 38dp `maxWidth` was below the style's `minWidth` and `Constraints` threw `maxWidth must be >= than minWidth` on the Welcome→Name transition. The original code sidestepped this by swapping in a `Spacer` rather than constraining the button. `BackSlot` now uses an unconstrained `Box`; reserving width is unnecessary anyway, because Welcome is the only step with `showBack = false` and it has no skip link to shift.

**2. A bounds animation that can reach zero must not overshoot.** The mascot is hidden by animating its width to `0.dp`, and `Modifier.animateBounds` was given the `settle` spring (damping 0.85). An underdamped spring interpolates *past* its target, so the bounds passed through a negative size and `Constraints.fixed` threw `width and height must be >= 0` on entering DayBounds. The mascot's `BoundsTransform` is now critically damped (`Spring.DampingRatioNoBouncy`) while keeping `settle`'s stiffness. The travel still glides; it just cannot undershoot. The bouncy character elsewhere (progress fill, CTA pop, cascade) is untouched.

**3. `pointerInput` caches its lambda block.** Drag-and-drop silently did nothing: `endDrag` closed over `target`, a plain local computed during composition, and because `pointerInput`'s keys never changed it kept invoking the lambda captured on first composition — where `target` was still `null`. The drop target is now computed *inside* `endDrag` from live `MutableState` (`draggingIndex`, `dragOffset`, `slotHeight`), with `onReorder` read through `rememberUpdatedState`. Anything a `pointerInput` block calls must read state at invocation time, not close over composed values.

Smaller deviations from the plan above:

- **No Gradle change.** `androidx.compose.animation` already resolves transitively and the BOM is applied by the convention plugin, so no catalog alias was added. Only the `ExperimentalSharedTransitionApi` opt-in was needed.
- **Progress bar stays composed on Welcome**, with alpha and gap height animated to zero, rather than being wrapped in `AnimatedVisibility` — that would dispose the instance across Welcome↔Name and lose the continuity this change exists to create.
- **Secondary footer link uses `AnimatedContent`**, not `AnimatedVisibility`, so `null→X`, `X→null`, and `X→Y` are all handled without retaining the last non-null label.
- **The tappable hour range reuses the existing `skipLink` style** rather than adding a new one; it is already the sky-coloured "this is a control" treatment. This is the only affordance signalling tap-to-edit, since neither the hint line nor the trailing chevron was chosen.
- **`ZoneCard.kt` was deleted**, replaced by `ZoneRow.kt` (the compact row) and `ZoneReorderList.kt` (drag, haptics, accessibility actions, entry nudge).
- **Five new strings**, not four: `onboarding_zone_sheet_title` was needed for the sheet header. All present in both `values/` and `values-ar/` (66 keys, verified identical).
