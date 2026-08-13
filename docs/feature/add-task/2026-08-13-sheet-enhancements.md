# Add-task sheet: voice input, 3D segmented control, animated height, goal thinking state

## Context

Five enhancements to the add-task/goal bottom sheet, plus several real bugs found while tracing them.

1. **Voice input is goal-only.** `rememberSpeechRecognizer` exists but is wired to the goal text fields only; the task title field has no mic. On top of that, Arabic speech is not recognized — it comes back English.
2. **The mic permission is asked cold.** Tapping the mic fires the OS permission dialog with no in-app explanation, so the settings-screen fallback that follows a permanent denial arrives with no context.
3. **The Task|Goal segmented control is a one-off.** It is a flat sliding pill living in the feature module, while three other hand-rolled copies exist elsewhere in the app. None of them use the app's rim/3D language.
4. **The sheet's height snaps rather than animates**, and intermittently flickers — repeatedly growing toward full screen and shrinking back, worse on short screens and with the keyboard up.
5. **The goal wizard freezes on the question it just answered.** After picking an answer and pressing Continue, the same question stays on screen with a spinner in the button until the reply lands.

Outcome: one shared speech helper and one shared 3D segmented control in `:core:design-system`, Arabic recognition fixed at the root, a permission ask that explains itself first, a smoothly animated sheet height with the flicker eliminated by breaking a confirmed layout feedback loop, and a mascot-led thinking state for the goal flow.

---

## Part 1 — Speech-to-text: promote, fix Arabic, add to the task field

### 1a. Root cause of the Arabic failure (confirmed)

[SpeechRecognizerHelper.kt:79](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:79) builds the intent with:

```kotlin
putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
```

`Locale.getDefault()` is the **process/system** locale. The app's language is not the system's — it lives in Proto DataStore (`user_preferences.proto:35`) and reaches the UI two ways, neither of which touches `Locale.getDefault()`:

- `AppCompatDelegate.setApplicationLocales(...)` — [MainActivity.kt:70](app/src/main/java/com/awan/app/MainActivity.kt:70)
- a `LocalConfiguration` override — [MainActivity.kt:90-111](app/src/main/java/com/awan/app/MainActivity.kt:90)

So on an English-system phone with the app set to Arabic, the recognizer is asked for `en-US`. That is exactly the reported symptom.

**Fix:** read the app locale from the composition, reusing the override MainActivity already provides (composition locals propagate into the sheet's dialog subcomposition):

```kotlin
val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
```

Per your choice, the tag is passed through **as configured** — bare `ar`, no region substitution.

### 1b. Three further defects in the same flow

| Defect | Evidence | Fix |
|---|---|---|
| No `<queries>` for `RecognitionService` | No `<queries>` element anywhere in the repo | Add it; on API 30+ its absence can make `SpeechRecognizer.isRecognitionAvailable()` return false, showing "unavailable" instead of a mic |
| No `EXTRA_LANGUAGE_PREFERENCE` | [SpeechRecognizerHelper.kt:77-81](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:77) | Set it to the same tag, so the recognizer's fallback language matches the request instead of its own preference |
| Transcript **overwrites** the field | `onResults`/`onPartialResults` both call `onTranscript(matches[0])` ([:120](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:120), [:127](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:127)) | Snapshot the field value when listening starts and emit `base + transcript`, so speaking after typing appends instead of wiping. Matters most on the task field, where a partly-typed sentence is the norm |

`ERROR_LANGUAGE_NOT_SUPPORTED` / `ERROR_LANGUAGE_UNAVAILABLE` currently fall into the generic error bucket ([:110-112](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:110)). Give them their own localized message so a rejected `ar` says so out loud rather than looking like a generic failure.

> If bare `ar` turns out to be rejected on your devices, the fix is one line — retry once with `ar-SA` on those two error codes. Deliberately not included, since you chose the bare tag.

### 1c. Promote to `:core:design-system`

Three call sites (goal form, goal preview, and now the task field) with two duplicated mic buttons ([GoalForm.kt:751](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt:751), [GoalPreviewScreen.kt:437](feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt:437)) is past the repo's own promotion threshold (`docs/feature/add-task/2026-07-23-length-menu-and-chip-in-design-system.md`).

| Action | Path |
|---|---|
| New | `core/design-system/.../AwanSpeechRecognizer.kt` — `rememberSpeechRecognizer` + `SpeechRecognizerState`, verbatim move plus the 1a/1b fixes |
| New | `core/design-system/.../AwanMicButton.kt` — the pulsing mic, deduping both copies |
| New | `core/design-system/src/main/AndroidManifest.xml` — `RECORD_AUDIO` + the `<queries>` block, so the dependency travels with the component |
| Delete | `feature/add-task/src/main/AndroidManifest.xml` — existed only for that permission |
| Delete | `SpeechRecognizerHelper.kt`, `GoalMicButton`, `PreviewGoalMicButton` |

Design-system already owns `AwanConfirmDialog` (used by the permanent-denial path) and `AwanTextField`, so nothing new is pulled in.

### 1d. One recognizer, both fields

`GoalForm` creates its own recognizer at [GoalForm.kt:74](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt:74). Both the task field and every goal field bind the **same** `state.input`, so hoist a single `rememberSpeechRecognizer` into `AddTaskSheetContent` and pass it to both `TaskForm` and `GoalForm`. One `SpeechRecognizer` instance, one shared error state, and `GoalFormContent` already takes `isListening`/`onToggleMic`/`speechError`/`isPermissionError` as params — only the thin `GoalForm` wrapper changes.

The task title field ([AddTaskSheet.kt:302](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:302)) gets `trailingContent = { AwanMicButton(...) }`, matching the goal field. Mic goes on the **title** only, not the description note — the note is deliberately the quieter field.

`GoalPreviewRouteRoot` keeps its own recognizer (separate screen, separate lifetime); it only changes imports.

### 1e. Ask for the mic permission with an explanation first

Today, tapping the mic on a first run goes straight to the OS dialog ([SpeechRecognizerHelper.kt:228-232](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:228)). New flow:

```
mic tap → no permission → AwanConfirmDialog ("Awan needs the microphone…")
        → Accept  → OS permission dialog → granted: start listening
                                         → denied:  inline field error
        → Not now → nothing happens, no error
```

Permanently-denied still routes to the existing settings-screen dialog ([:179-200](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:179)) — which is the point: after an in-app explanation and an explicit Accept, being sent to Settings finally reads as a consequence of a choice the user made rather than an unexplained jump.

Implementation is one more `showRationaleDialog` boolean and a second `AwanConfirmDialog` in the same file, reusing the component already imported there. `hasRequestedMicPermission` stays in DataStore — it is still the only way to tell "never asked" from "permanently denied", since `shouldShowRequestPermissionRationale` is false in both cases.

**Bug fixed in passing:** [:230-231](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/SpeechRecognizerHelper.kt:230) sets `isPermissionError = true` and the "permission denied" message *before* launching the OS dialog, so the text field turns red and shows a denial error while the system prompt is still on screen and unanswered. The error must only be set on an actual denial.

---

## Part 2 — `AwanSegmentedControl` in the design system

### Construction — reuse `AwanButton`, do not rebuild the rim

`AwanButton` already solves every hard part: the face-first / rim-fixed measure policy ([AwanButton.kt:220-247](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanButton.kt:220)), a `latchedPressed` flag that holds the face sunk on its rim ([:105](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanButton.kt:105)), RTL-correct sink direction, and `SegmentTick` haptics on the `Chip` variant. A selected segment *is* the `latchedPressed` case.

New file `core/design-system/.../AwanSegmentedControl.kt`:

```kotlin
@Composable
fun <T> AwanSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
)
```

- **Track** — `Box`, `AwanTheme.shapes.button` (18dp, your chosen rounded-rect), `background(colors.disabledSurface)`, `padding(4.dp)`.
- **Segments** — a `Row` of equal-weight `AwanButton(variant = Chip, latchedPressed = isSelected)`. Unselected sit raised on their rim; the selected one is sunk and tinted.
- **Tint** — per-segment inline `Style {}` handed to `style`/`rimStyle`, exactly the pattern [AwanChip.kt:79](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanChip.kt:79) uses. Selected: face `colors.sky`, content `colors.onSky`, rim `colors.skyPressed`. Unselected: face `colors.surface`, rim `colors.line`.
- **Semantics** — override `AwanButton`'s `Role.Button` with `Modifier.semantics { role = Role.Tab; selected = isSelected }`.
- **Previews** — Light/Dark pair delegating to one private composable wrapped in `AwanTheme(dark = …)` and `.styleable(null, AwanTheme.styles.screen)`, per the house pattern in [AwanTextField.kt](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanTextField.kt).

No new strings: labels come from call sites.

### Migrations (the three you selected)

| Call site | Change |
|---|---|
| [AddTaskModeSelector.kt](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/AddTaskModeSelector.kt) | **Delete the file.** Call `AwanSegmentedControl` directly from `AddTaskSheetContent` over `AddTaskMode.entries`. Also fixes a latent RTL bug: [:65](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/AddTaskModeSelector.kt:65) offsets the thumb by raw `halfWidth * slide` with no mirroring, so in Arabic it slides the wrong way |
| [GoalsTabRow.kt](feature/goals/impl/src/main/java/com/awan/feature/goals/impl/ui/components/GoalsTabRow.kt) | Body becomes one `AwanSegmentedControl` call; the file keeps only the count formatting (`goals_tab_badge_format`), which is already a single label string |
| [GoalsEntryProvider.kt:102-145](feature/goals/impl/src/main/java/com/awan/feature/goals/impl/navigation/GoalsEntryProvider.kt:102) | Replace the inline hand-rolled block. Its selection is already sky-filled, so it lands visually unchanged |

Inventory's `FilterChip` row is out of scope, as agreed.

---

## Part 3 — Sheet height: animation and flicker

### Root cause of the flicker (confirmed in Material3 1.4.0 sources)

Read from the resolved artifact for this BOM (`material3-android-1.4.0-sources.jar`):

| Source | Behaviour |
|---|---|
| `ModalBottomSheet.kt:295-307` | `Expanded` anchor `= max(0f, fullHeight - sheetSize.height)` — recomputed inside `measure()` every layout pass |
| `ModalBottomSheet.kt:338` | `.consumeWindowInsets(WindowInsets(top = sheetState.offset.toInt().coerceAtLeast(0)))` |
| `ModalBottomSheet.kt:362` | content `Column` pays `.windowInsetsPadding(contentWindowInsets())` |
| `SheetDefaults.kt:400-402` | default insets `= safeDrawing.only(Bottom + Top)` |

With `F` = available height, `T` = top safe-drawing inset, `S` = sheet height:

```
offset = max(0, F − S)          // anchor
topPad = max(0, T − offset)     // consumed-then-paid top inset
S      = min(H + topPad + bottomPad, F)   // wrap-content content
```

While `offset < T` the loop gain is exactly **+1**: one pixel less offset is one pixel more top padding is one pixel taller sheet is one pixel less offset. There is no interior equilibrium — only `S = F` (pinned full-screen) or `offset ≥ T` (clear of the inset). Whenever content height sits near `F − T`, the sheet has **no stable resting height**, and each iteration runs `updateAnchors`, which cancels and restarts the in-flight settle spring (`AnchoredDraggable.kt:513-521`, `restartable(inputs = { anchors })`).

This explains every reported detail: worse on short screens (small `F`), triggered by the keyboard (the IME shrinks `F` ~300dp and pushes content across the threshold), intermittent (only once content is tall enough), and never self-settling — `CloudDrift` runs an infinite transition, so a frame is produced every vsync and the loop is re-evaluated continuously.

**Two of your suspicions were checked and ruled out.** The keyboard is the trigger, not the mechanism: `ModalBottomSheet.android.kt:586` forces `SOFT_INPUT_ADJUST_NOTHING` on the sheet's own window at API 30+, so the Activity's `adjustResize` never applies here. And `Modifier.imePadding()` at [AddTaskSheet.kt:151](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:151) is **dead code** — M3's own root `Box(Modifier.fillMaxSize().imePadding())` (`ModalBottomSheet.kt:186`) has already consumed the IME inset. It is not double-padding; it is just misleading.

### Fix — keep the sheet clear of the top inset

The loop can only close while `offset < T`. Cap the content so that can never happen. In `AddTaskSheetContent` ([AddTaskSheet.kt:201-217](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:201)):

```kotlin
BoxWithConstraints(modifier) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight - topInset - AwanTheme.spacing.sm)) {
        SkyHeader(state = state)
        // inner scroll column, per below
    }
}
```

and drop the dead modifier at [:151](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:151) → `Modifier.fillMaxWidth()`.

Why this is the root fix and not a guard: it guarantees `offset ≥ T + 12dp` unconditionally, so `max(0, T − offset)` is a constant `0` and the feedback path carries no signal. `asPaddingValues()` reads the **raw** inset and is blind to consumption, so it cannot itself become a second feedback path — which is why `statusBarsPadding()` would not work here. The 12dp margin absorbs the `offset.toInt()` truncation at `:338`.

**Accepted trade-off:** the sheet can no longer reach full-bleed under the status bar; a status-bar-plus-12dp strip of scrim stays visible. Standard M3 behaviour, and it keeps the sky header and drag pill on screen.

### Animated height shift

The sheet itself cannot be animated — `updateAnchors` hard-snaps the offset via `trySnapTo` (`AnchoredDraggable.kt:405`). So animate the **content** and let the sheet track it, with exactly one size authority.

**Today there are three, all fighting:**

- `Crossfade` at [AddTaskSheet.kt:229](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:229) is a plain `Box` holding both children — it jumps to `max(task, goal)`, then jumps down. Two hard snaps, no size animation at all.
- The nested `AnimatedContent` at [GoalForm.kt:130](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt:130) runs a default spring `SizeTransform` inside that `Crossfade`.
- `AwanCard.animateContentSize` ([AwanCard.kt:73](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanCard.kt:73)) on every MCQ option card.

**Changes:**

1. Put a single `animateContentSize(if (reducedMotion()) snap() else AwanTheme.motion.settle.spec<IntSize>())` on the inner scroll column, **outside** `verticalScroll` so it animates the already-clamped height — the one number the sheet's anchors read.
2. Replace the outer `Crossfade` with `AnimatedContent` using `fadeIn/fadeOut` and `SizeTransform { _, _ -> snap() }`, so it reports its target size immediately and cedes height to (1).
3. Same `SizeTransform { _, _ -> snap() }` on `GoalFormContent`'s `AnimatedContent`.
4. **Fix the wizard-step snap.** `contentKey = { it::class }` ([GoalForm.kt:132](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt:132)) collapses consecutive `MultipleChoice → MultipleChoice` and `WritingQuestion → WritingQuestion` steps into one key, so `AnimatedContent` runs **no transition at all** between them — this is the step-to-step snap. Key on the question text instead (`"mcq:${step.question}"`, `"writing:${step.question}"`). Keying on the whole step object would be wrong: `AddTaskViewModel.kt:148` rewrites `selectedOption` in place, which would fire a full transition on every option tap. This `contentKey` is extended once more in Part 4 — write it once, with the phase included.
5. Leave `AwanCard.animateContentSize` alone — it only fires when an existing card resizes, `SizeAnimationModifierNode` does not animate a first measure, and once the loop is open it can no longer reach the sheet anchors.

All three animations gate on `reducedMotion()`.

---

## Part 4 — Goal thinking state and the "plan is ready" beat

### The problem

`submitGoalContinuation` sets `isSubmitting = true` ([AddTaskViewModel.kt:316](feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskViewModel.kt:316)) and leaves `goalStep` untouched until the reply lands, so the UI keeps rendering the question the user just answered with a spinner in the Continue button. When the reply *is* a proposal, `goalStep` becomes `Preview` and [AddTaskSheet.kt:110-116](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:110) hides the sheet and navigates on the very next frame — no acknowledgement that anything was produced.

### Model it as a derived phase — no new stored state, no ViewModel change

Both conditions are already fully expressed by existing state. Add one derived property to `AddTaskState`:

```kotlin
enum class GoalPhase { Editing, Thinking, PlanReady }

val goalPhase: GoalPhase
    get() = when {
        mode != AddTaskMode.GOAL -> GoalPhase.Editing
        goalStep is GoalStep.Preview -> GoalPhase.PlanReady
        isSubmitting -> GoalPhase.Thinking
        else -> GoalPhase.Editing
    }
```

Safe because in the sheet, `GOAL && isSubmitting` only ever means an in-flight `continueGoalDecomposition`: `createDirectly` is TASK-mode, and `acceptGoalProposal` runs on the separate `GoalPreviewRouteRoot` screen.

### Rendering

**Thinking / ready panel** replaces the step content inside `GoalFormContent`'s `AnimatedContent`, by folding the phase into the target and `contentKey` written in Part 3:

```kotlin
targetState = state.goalPhase to state.goalStep,
contentKey = { (phase, step) ->
    when (phase) {
        GoalPhase.Thinking -> "thinking"
        GoalPhase.PlanReady -> "ready"
        GoalPhase.Editing -> when (step) {
            GoalStep.Initial -> "initial"
            is GoalStep.MultipleChoice -> "mcq:${step.question}"
            is GoalStep.WritingQuestion -> "writing:${step.question}"
            is GoalStep.Preview -> "preview"
        }
    }
},
```

The question, its options and the Continue button all disappear the instant Continue is pressed, and the Part 3 height animation carries the sheet down to the smaller panel.

**One mascot, not two.** The sheet already has a mascot on its biggest stage — the `SkyHeader` `CloudDrift` band ([AddTaskSheet.kt:269](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:269)), driven by `AddTaskState.mascot`. Adding a second one in the body would put two Awans on screen. Instead, teach the existing one the two new expressions by adding two branches above the `mode == GOAL -> Curious` line at [AddTaskState.kt:155](feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskState.kt:155):

```kotlin
goalPhase == GoalPhase.PlanReady -> MascotExpression.Celebrate
goalPhase == GoalPhase.Thinking  -> MascotExpression.Idle   // "normal", per your note
```

The body panel then holds only the status text, which sits beneath the mascot exactly as asked.

**Make the mascot move more while thinking.** `AwanMascot` already floats on an infinite sine ([AwanMascot.kt:60-77](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanMascot.kt:60)) but the idle bob is deliberately subtle. Add a `thinking: Boolean = false` param that shortens the phase tween (3200ms → ~1400ms) and adds a small ±2° sway — four lines, reusing the existing `cheering` ternaries, and useful to every future loading state.

**Alternating text.** A `List<Int>` of string resources cycled on a `LaunchedEffect` every ~2s, faded with `AnimatedContent`, snapping under `reducedMotion()`. Carries `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` so TalkBack announces each line — the same treatment the speech error already gets at [GoalForm.kt:150](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalForm.kt:150).

**The ready beat.** `PlanReady` shows the Celebrate mascot and a single "Your plan is ready" line, then navigates. Only the navigation trigger at [AddTaskSheet.kt:110](feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt:110) changes — a one-second dwell before the existing hide-and-navigate:

```kotlin
LaunchedEffect(state.goalPhase) {
    if (state.goalPhase == GoalPhase.PlanReady) {
        delay(PlanReadyDwellMillis)   // 1000
        sheetState.hide()
        onDismiss()
        onNavigateToGoalPreview()
    }
}
```

The dwell is a visual beat that belongs next to `sheetState.hide()`, so it stays in the UI rather than becoming a ViewModel timer. Note this also removes a pre-existing wart called out during exploration: today the sheet grows to full `Preview` height *while* sliding away, because `GoalStep.Preview` renders for a frame before `hide()` completes. The `PlanReady` panel is small and fixed, so the sheet now shrinks first and leaves cleanly.

**Intermediate replies are unchanged** — another question just replaces the thinking panel. The celebrate beat fires only for a proposal.

New strings (add-task module, en + ar): four rotating thinking lines and one `add_task_goal_plan_ready`.

---

## Localization

Ten string keys move from `feature/add-task` (`add_task_goal_mic_*`, `add_task_goal_speech_*`, `add_task_goal_permission_dialog_*`) to `core/design-system` under the module's `ds_` prefix, in **both** `values/` and `values-ar/`, and are deleted from both add-task locale files.

New keys, all in both locales:

| Module | Keys |
|---|---|
| `core/design-system` | `ds_speech_language_unsupported`; `ds_speech_rationale_title` / `_body` / `_confirm` / `_cancel` |
| `feature/add-task` | `add_task_goal_thinking_1`…`_4`, `add_task_goal_plan_ready` |

No new strings for the segmented control — its labels come from call sites.

---

## Verification

**Build/static:** `./gradlew assembleDebug` and `./gradlew lint`. (There is no detekt task in this project.) Run `./gradlew :feature:add-task:testDebugUnitTest` first to confirm the harness works before touching `AddTaskViewModelTest.kt` (1626 lines, unaffected by these UI changes but a good canary).

**Existing instrumented tests:** `GoalFormTest.kt` asserts mic button states at lines 45, 77, 130, 263 — update its imports for the moved composable and re-run `./gradlew :feature:add-task:connectedDebugAndroidTest`.

**Flicker — the objective check.** Temporarily log `onSizeChanged` on the sheet content root and `snapshotFlow { sheetState.requireOffset() }`, capture with `adb logcat -s SheetH`. Repro **before** fixing to confirm the mechanism, then re-run:

| Scenario | Fail (today) | Pass |
|---|---|---|
| Sheet open, keyboard up, finger off screen, 5 s | continuous log lines, `Δoffset` sign reverses repeatedly | zero lines after the open animation settles |
| Task → Goal | 2 height samples, non-monotone | monotone ramp, converges < 600 ms |
| MCQ → MCQ (two in a row) | exactly 1 sample — a hard snap | monotone ramp |

Single metric: **count sign changes of `Δoffset` across the session. Pass = 0.**

Repro config that maximises the loop: short screen with a tall cutout — `adb shell wm size 1080x1920 && adb shell wm density 440`, emulator display cutout set to Tall. Open Goal, reach an MCQ step with ≥4 options, focus the `singleLine = false` custom-answer field, type several newlines. Also check gesture vs 3-button nav (different bottom inset), landscape, and `adb shell settings put global animator_duration_scale 0` for the reduced-motion path.

**Speech (the actual reported bug):** set the app language to Arabic in Profile while leaving the **system** language English — that is the configuration that reproduces it. Speak Arabic into both the task title and a goal field; the transcript must come back Arabic. Then type text first and speak, to confirm the transcript appends rather than wipes. Also verify the mic works when app and system languages agree.

**Permission flow**, from a fresh install (`adb shell pm revoke com.awan.app android.permission.RECORD_AUDIO` between runs):

1. Tap mic → in-app explanation dialog appears, **no OS dialog yet**, field is **not** red.
2. "Not now" → dialog closes, nothing happens, no error text.
3. Tap mic → Accept → OS dialog appears → Allow → listening starts.
4. Revoke, tap mic → Accept → Deny → inline field error only (no settings dialog).
5. Revoke, repeat until permanently denied → tap mic → in-app explanation → Accept → settings dialog, which now follows an explicit choice.

**Goal thinking state:** start a goal, answer a question, press Continue. The question and Continue button must vanish immediately; the SkyHeader mascot switches to the idle/normal face and visibly bobs; the status line cycles. On a proposal reply the mascot switches to Celebrate with "Your plan is ready", holds ~1s, and only then does the sheet slide down and the preview route open — verify the sheet **shrinks before** it leaves rather than growing mid-slide. On a follow-up question reply, the next question simply replaces the panel with no celebrate beat. Re-run with TalkBack to confirm the status line is announced, and with `animator_duration_scale 0` to confirm the text still cycles while transitions snap.

**Segmented control:** visually check all three migrated call sites in light/dark and LTR/RTL, confirming the selected segment sinks the right way in Arabic. TalkBack should announce each segment as a selected/unselected tab.

**Sibling sheets to spot-check for the same latent loop** (untouched by this plan, but they contain text fields): `EditPersonalInfoSheet.kt`, `ZoneEditSheet.kt`. If they flicker on the short-screen config, they need the same cap — follow-up, not this change.

---

## Before starting

1. Write this plan to `docs/feature/add-task/2026-08-13-sheet-enhancements.md` per the repo's feature-plan rule, and append an `## Implementation notes (what actually differed)` section when the work lands.
2. No Jira issue ID is known for this work. Search the `AWAN` project for a matching issue before the first commit; if there is no confident match, ask rather than guessing — the ID must appear in the branch name or commit message.
