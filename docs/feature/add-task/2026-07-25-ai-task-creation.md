# AI task creation in the add-task sheet

## Context

The add-task sheet today is a pure client-side flow: you type a sentence, `TaskInputParser` pulls
time/duration/zone tokens out of it, chips read back what it understood, and `CreateTaskUseCase`
posts it. Everything the task knows, the user typed.

The backend now has an AI vertical that can do that work instead. This adds a switch in **Task**
mode that hands the job over: the form collapses to a title field plus a free-text note, the parser
stands down, and `POST /v1/ai/task-create` comes back with a fully-specified task — duration, points,
mandatory, splittable, category — which the sheet then shows for review and editing before anything
is committed.

Two things came out of pinning down the backend contract, and both are load-bearing:

1. **`POST /v1/ai/task-create` persists.** It returns `201` with a real task id, auto-assigned to
   Inbox. It is not a dry run. Every path out of the review stage therefore has to either keep that
   task or delete it — there is no third option, and leaking it means an orphan row in the user's
   Inbox every time they back out.
2. **Zone and category are now different things server-side.** `ZoneResponse` carries
   `category (auto-created from zone name)`; a *task* has a `categoryId`, a *session* has a `zoneId`.
   The sheet's chip currently picks a zone and puts its id on the task, which no longer matches the
   model. Phase 1 fixes that before the AI work lands on top of it.

Target outcome: the user flips one switch, types a sentence in plain language, and gets back a task
that Awan filled in — then either lets Awan schedule it too, or takes the wheel and picks the time.
And they can't lose a half-written draft by tapping outside the sheet.

---

## Backend contract (verified via Postman MCP, collection `Awan`)

| Call | Shape |
|---|---|
| `POST /v1/ai/task-create` | `{title, description?}` → `201 TaskInfoResponse` (**persisted**, Inbox, includes `category`) |
| `POST /v1/schedule/task` | `{taskId, horizonDays?}` → `200 {taskId, scheduledSessions[], unscheduledTasks[]}` |
| `DELETE /v1/tasks/{id}?cascade=false` | `204` |
| `GET /v1/categories` | `[{id, name}]` |
| `GET /v1/zones/date/{date}` | `ZoneResponse[]`, each with `category: {id,name}?` |
| `POST /v1/tasks` | accepts `categoryId` |
| `POST /v1/tasks/with-sessions` | accepts `categoryId` in `task` (confirmed by the user; docs omit it) |

`scheduleTask` can succeed at the HTTP level and still fail to place the task — `unscheduledTasks`
non-empty with a `reason`. That is a UI state, not an error path.

---

## Phase 1 — Zone → Category in the task vertical

Prerequisite. The AI response hands back a `category`, and there is nowhere correct to put it today.

**`:core:model`**
- New `Category(id: String, name: String)`.
- `DayZone` gains `category: Category?`.
- `Task` gains `category: Category?`, `estimatedPoints` and `allowTaskSplitting` already exist.
- `TaskDraft`: `zoneId` → `categoryId`, `zoneToken` → `categoryToken`; add `estimatedPoints: Int = 0`
  and `allowTaskSplitting: Boolean = false` (today `TaskMappers.toRequest()` hardcodes both, which
  would silently discard the AI's suggestions on confirm).

**`:core:network`** — follow the recipe in `dto/` + `api/` + `di/NetworkModule.kt`
- `CategoryDto`; `CategoryApiService.getCategories(): List<CategoryDto>` (`GET v1/categories`) plus a
  `@Provides @Singleton` in `NetworkModule`.
- `TaskApiService`: `createTaskWithAi(CreateTaskWithAiRequest): TaskInfoResponse`
  (`POST v1/ai/task-create`), `scheduleTask(ScheduleTaskRequest): TaskScheduleResponse`
  (`POST v1/schedule/task`), `deleteTask(@Path id, @Query cascade = false)` (`DELETE v1/tasks/{id}`).
- `ZoneDto` and `TaskInfoResponse` gain `category: CategoryDto?`; `CreateTaskRequest` and the nested
  task object in `CreateTaskWithSessionsRequest` gain `categoryId: String?`, `estimatedPoints`,
  `allowTaskSplitting`.

**`:core:data`** — copy the task vertical verbatim (`TaskRemoteDataSourceImpl` is the template)
- `category/` package: `CategoryRemoteDataSource(+Impl)`, `CategoryRepositoryImpl`,
  `CategoryMappers.kt`; `@Binds @Singleton` pair in `di/DataModule.kt`.
- `TaskRemoteDataSource` gains the three new methods, each a one-line `safeApiCall`.
- `TaskMappers`: map `category`, stop hardcoding points/splitting, send `categoryId`.

**`:core:domain`**
- `category/repository/CategoryRepository.getCategories(): Result<List<Category>>`.
- `category/usecase/GetCategoriesUseCase`.
- Parser rename, mechanical — the `@token` **syntax is unchanged**, only the names:
  `TaskTokenKind.ZONE` → `CATEGORY`, `ParsedTaskInput.zoneToken` → `categoryToken`,
  `TaskInputWriter.withZone` → `withCategory`, `TaskAttribute.In(zoneName)` → `In(categoryName)`.
- `CreateTaskUseCase` gains `GetZonesForDateUseCase` as a constructor dep. When the draft has a
  `startAt`, it resolves the session's `zoneId` by finding the day's zone whose
  `category.id == draft.categoryId`, falling back to a zone-less session. This is the seam where the
  two concepts meet, and a use case orchestrating two repositories is exactly the right home for it.
- New: `DeleteTaskUseCase`, `CreateTaskWithAiUseCase(title, description?): Result<Task>`,
  `ScheduleTaskWithAiUseCase(taskId): Result<TaskSchedule>` where
  `TaskSchedule(sessions: List<TaskSession>, unscheduledReason: String?)`.

**`:feature:add-task`**
- `AddTaskState`: `availableZones/resolvedZone/isResolvingZone` → `availableCategories`
  `(List<Category>)` / `resolvedCategory` / `isResolvingCategory`. `AddTaskPicker.ZONE` → `CATEGORY`.
- `AddTaskViewModel`: swap `GetZonesForDateUseCase` for `GetCategoriesUseCase`. Categories are
  date-independent, so the whole `loadedZoneDate` / `refreshZones(token, date)` guard **goes away** —
  fetch once in `init`, match the typed token against the result. This deletes code.
- `TaskAttributeChips`: `ZoneChip`/`ZoneMenu` → `CategoryChip`/`CategoryMenu`, unchanged behaviour
  (resolved / resolving / unknown-token-in-destructive-tone / empty).
- Keep `AddTaskViewModel.matching()` as-is — exact match then `startsWith`, which is what makes
  `withCategory`'s first-word write (`"Afternoon Work"` → `@Afternoon`) round-trip.

**Tests to update:** `TaskInputParserTest`, `TaskInputWriterTest`, `AddTaskViewModelTest`,
`TaskRepositoryImplTest`, `TaskRemoteDataSourceTest`.

---

## Phase 2 — The AI stage machine

One enum drives everything. In `AddTaskState.kt`:

```kotlin
enum class AddTaskAiStage { OFF, COMPOSING, WORKING, REVIEW, MANUAL }
```

| Stage | Sheet |
|---|---|
| `OFF` | Today's form, unchanged. Switch visible. |
| `COMPOSING` | Switch on. Chips row + hint **hidden**. Parser stood down. Title field wears the aura. Note placeholder → "Anything else Awan should know?". Button → "Ask Awan". |
| `WORKING` | Request in flight. Fields disabled, aura at full tilt, button `isLoading`, mascot `Curious`. |
| `REVIEW` | AI task returned. Mode selector **and** switch hidden. Parser back on. Chips back — **except the when chip**. Two buttons: "Schedule with Awan" (primary) / "Pick a time myself" (quiet). |
| `MANUAL` | When chip revealed, single "Add task" button. |

**Standing the parser down without losing the text.** `input` is never touched; `onInputChanged`
simply skips `parseTaskInput` while the stage is `COMPOSING`/`WORKING` and leaves
`parsed = ParsedTaskInput.Empty`. No tokens means `rememberTokenHighlight` returns an identity
transformation, so the highlights disappear for free. Toggling back to `OFF`, or landing in `REVIEW`,
re-parses the current `input`. One consequence to handle: `canSubmit` reads `parsed.title`, so it
must read `input` instead while the parser is down.

**Rebuilding the sentence in `REVIEW`.** Reuse `TaskInputWriter` rather than inventing a second
formatter — start from the AI's `title`, apply `withDuration(estimatedDuration)`, then
`withCategory(category.name)`, then parse the result. `description`, `mandatory` come straight off
the response; `id`, `estimatedPoints`, `allowTaskSplitting` are parked in state
(`aiTaskId`, `aiPoints`, `aiSplittable`) and ride through to whichever confirm path is taken.

**New state:** `aiStage`, `aiTaskId: String?`, `aiPoints: Int`, `aiSplittable: Boolean`.
**New actions:** `AiToggled`, `ScheduleWithAi`, `ScheduleManually`, `DismissRequested`,
`DiscardConfirmed`, `DiscardCancelled`. `Submit` keeps its name and branches on the stage.

**The two confirm paths:**

- *Schedule with Awan* — `ScheduleTaskWithAiUseCase(aiTaskId)`. The task already exists, so nothing
  is created or deleted. If the response carries `unscheduledTasks`, stay in `REVIEW` and show
  `add_task_error_ai_schedule_failed` so the user can fall back to picking a time.
- *Pick a time myself* → `MANUAL` → on confirm, `DeleteTaskUseCase(aiTaskId)` **then**
  `CreateTaskUseCase(draft)`. Delete-first is the deliberate order: if the create then fails, the
  sheet still holds every field and a retry re-creates the task, whereas create-first leaves a
  duplicate that nothing will ever clean up. There is no standalone create-session endpoint, so
  patching the AI task instead of replacing it could never attach the chosen time.

---

## Phase 3 — The AI aura

New `core/design-system/src/main/java/com/awan/app/core/designsystem/AwanAiAura.kt`:

```kotlin
@Composable
fun AwanAiAura(
    active: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = AwanTheme.shapes.button,
    content: @Composable () -> Unit,
)
```

- `rememberInfiniteTransition` drives an angle 0→360° over ~2500 ms, `LinearEasing`.
- `Modifier.drawWithContent { drawContent(); rotate(angle) { drawOutline(outline, sweep, style = Stroke(2.dp)) } }`.
- Sweep stops `zoneViolet → sky → zoneTangerine → zoneViolet` — the loop must close or the seam shows
  as a hard line once per turn.
- Stroke alpha via `animateFloatAsState(if (active) 1f else 0f)` so it fades rather than pops.
- `reducedMotion()` → no rotation, static sweep at 0°. Every DS animation gates on this.
- Paired light/dark `@Preview`s per the DS convention (`AwanTextField.kt:152` is the template).

This is drawn *outside* the Styles API rather than through `AwanTheme.styles.textField`, because
`border()` in a `Style` block takes a solid `Color` — there is no brush overload. Worth a comment on
the file so the next person doesn't try to move it into `AwanStyles`.

Then wrap the title field: `AwanAiAura(active = state.aiStage.isAiComposing) { AwanTextField(...) }`.

---

## Phase 4 — Unsaved-changes guard

Applies to the whole sheet — Task or Goal, AI or manual — as the user asked.

- `AddTaskState.isDirty = input.isNotBlank() || description.isNotBlank() || aiStage != OFF`.
  Mode-agnostic by construction, so it covers Goal mode the moment Goal mode grows fields.
- `ModalBottomSheet(onDismissRequest = { onAction(DismissRequested) })` plus a `BackHandler` — today
  `AddTaskSheet.kt:90` calls `onDismiss` straight through and has to stop doing that.
- `DismissRequested` → emit `Dismissed` if clean, else raise `showDiscardConfirm`.
- Render the existing `AwanActionSheet` (title / body / "Discard" as `Destructive` / "Keep editing").
  No new dialog component.
- **`DiscardConfirmed` must also delete `aiTaskId` when one exists.** Backing out of `REVIEW` after
  the AI has already persisted a task is the one path that silently leaves an orphan in the Inbox.
  Fire-and-forget `DeleteTaskUseCase` before emitting `Dismissed`.

---

## Strings

New keys in **both** `feature/add-task/src/main/res/values/strings.xml` and `values-ar/strings.xml`:

`add_task_ai_switch`, `add_task_ai_switch_content_description`, `add_task_ai_note_placeholder`,
`add_task_ai_submit`, `add_task_ai_working`, `add_task_ai_schedule_with_ai`,
`add_task_ai_schedule_manually`, `add_task_error_ai_failed`, `add_task_error_ai_schedule_failed`,
`add_task_discard_title`, `add_task_discard_body`, `add_task_discard_confirm`,
`add_task_discard_cancel`.

Renames (zone → category): `add_task_chip_no_zone`, `add_task_zone_menu_empty`,
`add_task_chip_zone_unknown`, `add_task_chip_zone_resolving` → `..._category_...`. Also delete
`add_task_close`, which is already unused. Note `add_task_hint` names `@play` — reword for categories.

`:app` lint runs with `MissingTranslation` as an error, so a missing `values-ar` key fails the build.

---

## Critical files

| Area | Files |
|---|---|
| Switch | `feature/add-task/.../ui/components/MandatoryToggle.kt` — the rail-and-dot switch to copy for the AI toggle rather than reaching for Material's `Switch` |
| Sheet | `feature/add-task/.../ui/AddTaskSheet.kt` (dismiss routing, stage branching, aura wrap) |
| Chips | `feature/add-task/.../ui/components/TaskAttributeChips.kt` |
| Presentation | `feature/add-task/.../presentation/{AddTaskState,AddTaskAction,AddTaskViewModel}.kt` |
| Parser | `core/domain/.../task/parser/{ParsedTaskInput,TaskInputParser,TaskInputWriter}.kt` |
| Use cases | `core/domain/.../task/usecase/`, new `core/domain/.../category/` |
| Data | `core/data/.../task/{TaskRepositoryImpl,TaskMappers}.kt`, `core/data/.../task/remote/`, new `core/data/.../category/`, `core/data/.../di/DataModule.kt` |
| Network | `core/network/.../api/TaskApiService.kt`, `core/network/.../dto/`, `core/network/.../di/NetworkModule.kt` |
| DS | new `core/design-system/.../AwanAiAura.kt` |

Reference verticals to copy, not invent against: `core/data/task/` (remote source → repo → mapper →
`@Binds`) and `core/domain/auth/` (contract + models + use cases).

## Ordering note

Phases 1 and 3 are independent of each other and of 2; 2 depends on 1; 4 depends on 2 only for the
`aiTaskId` cleanup. Phase 1 is a wide mechanical rename and is worth landing as its own commit so the
AI work reads as a clean diff on top.

Per `CLAUDE.md`, the plan gets copied to `docs/feature/add-task/2026-07-25-ai-task-creation.md`
before work starts, and grows an `## Implementation notes (what actually differed)` section when it's
done. The current branch is `feature/AWAN-82-quick-add-task`; AWAN-82 is the quick-add ticket, so this
needs its own Jira issue — I'll search the AWAN project for a match before the first commit and ask
if there isn't one.

---

## Verification

1. `./gradlew :core:domain:testDebugUnitTest :feature:add-task:testDebugUnitTest :core:data:testDebugUnitTest`
   — run one existing test file first to confirm the harness is alive before writing new ones.
   (`./gradlew detekt` is documented in CLAUDE.md but not actually wired up — don't plan around it.)
2. `./gradlew assembleDebug lint` — `MissingTranslation` is an error, so this catches Arabic gaps.
3. **Confirm `categoryId` is accepted by `POST /v1/tasks/with-sessions`** against the dev backend
   before wiring the manual path — the Postman docs omit it. If it 422s, the fallback is a follow-up
   `PATCH /v1/tasks/{id}`.
4. New unit tests: stage transitions in `AddTaskViewModelTest` (toggle preserves `input` and clears
   tokens; `REVIEW` rebuilds a parseable sentence; manual confirm deletes then creates; discard from
   `REVIEW` deletes), `TaskInputWriter.withCategory`, category mapping in `TaskRepositoryImplTest`.
5. On emulator (`:app:installDebug`): toggle AI on with text already typed → text survives,
   highlights vanish, chips vanish, aura spins. Send → review stage shows AI values with no when
   chip. Both confirm paths. Tap outside with a dirty form → discard sheet. Discard from review →
   verify via `GET /v1/tasks/{id}` that the AI task is gone.
6. Check the aura in dark theme and with developer-option animations off (`reducedMotion()` path).

The dev backend seeded no zones as of the 2026-07-24 zone-chip work — check whether it seeds
categories, or the category chip's populated path will again only be covered by unit tests.

---

## Implementation notes (what actually differed)

**Status.** `./gradlew testDebugUnitTest assembleDebug lint` green. Add-task unit tests went 25 → 45.
Not yet driven on a device; the emulator pass and the two backend checks below are still outstanding.

### Deviations from the plan as written

1. **The stage enum grew two derived properties instead of the UI branching on it.** `isComposing`
   (`COMPOSING`/`WORKING`) and `isReviewing` (`REVIEW`/`MANUAL`) are on `AddTaskAiStage`, and the
   sheet reads four named booleans off state — `showsAiSwitch`, `showsModeSelector`,
   `showsAttributeChips`, `showsWhenChip`. The composables ended up with no knowledge of the stage
   machine at all, which is what the split was for.

2. **`TaskDraft.zoneId` was deleted, not renamed.** The plan said `zoneId` → `categoryId`; in fact
   the draft now carries *only* `categoryId`, and `CreateTaskUseCase` resolves the session's zone by
   matching `zone.category.id`. Nothing outside that use case knows a zone id exists. This is the
   only place the two concepts touch.

3. **`AddTaskState.hasUnknownZone` was deleted rather than renamed.** It was computed and never read
   — `CategoryChip` re-derives the same condition inline, as `ZoneChip` always did.

4. **`refreshZones`/`loadedZoneDate` are gone entirely.** Categories aren't date-scoped, so the
   whole "refetch when the day changes" guard collapsed into one `loadCategories()` in `init`. Net
   deletion in the ViewModel.

5. **`DotOnARail` was promoted to `internal` inside the feature, not into the design system.** The
   AI switch reuses it with a violet tone. It stays feature-local because there is still no
   `AwanSwitch` and no second caller outside this sheet — see
   `2026-07-23-length-menu-and-chip-in-design-system.md` on not promoting early.

6. **`OnboardingViewModelTest` needed a `FakeZoneRepository`.** `CreateTaskUseCase` gained a
   constructor dep, and onboarding constructs it directly. Its fake `error()`s on every method —
   onboarding's first task is unscheduled, so the zone branch is genuinely unreachable, and a fake
   that throws proves it rather than assuming it.

7. **`celebrateAndClose` re-runs `loadCategories()`.** Resetting to a fresh `AddTaskState` wipes
   `availableCategories`, which the original `submit()` also did — but the sheet is a singleton
   ViewModel that survives a create, so the category menu would have come back empty on the second
   task. This was a pre-existing latent bug in the zone version, surfaced by making the fetch
   `init`-only.

### Traps worth carrying forward

- **`POST /v1/ai/task-create` persists.** Every exit from `REVIEW`/`MANUAL` deletes the task it
  returned: *Pick a time myself* deletes then creates, *discard* deletes then dismisses. There are
  tests for both (`calls` on the fake records ordering, not just membership). Adding a third way out
  of the review stage without deleting leaks a row into the user's Inbox.
- **Delete-then-create, not create-then-delete.** If the create fails after the delete, the sheet
  still holds every field and a retry rebuilds the task; the reverse order leaves a duplicate that
  nothing cleans up. The failure branch clears `aiTaskId` so the retry doesn't try to delete a task
  that is already gone.
- **`scheduleTask` returning 200 does not mean scheduled.** `TaskSchedule.isScheduled` requires a
  non-empty `sessions` *and* a null `unscheduledReason`; `TaskScheduleResponse.toModel()` invents a
  reason when both lists come back empty, so an empty response can't pass for success.
- **Standing the parser down is what hides the highlights.** `onInputChanged` skips
  `parseTaskInput` while `aiStage.isComposing`, leaving `parsed` empty — no tokens means
  `rememberTokenHighlight` returns an identity transformation. Nothing hides the tints explicitly.
  Consequence handled: `canSubmit` reads `input` rather than `parsed.title` in those two stages.
- **The switch is hidden during `WORKING`, and `toggleAi()` guards on `showsAiSwitch` rather than
  re-deriving the condition.** A toggle landing mid-flight would let Awan's answer arrive on a sheet
  that had already switched back to manual, pushing it into `REVIEW` from nowhere. Any new gate on
  the switch belongs in `showsAiSwitch`, not in the ViewModel.
- **The Styles API can't hold the aura.** `border()` inside a `Style` block takes a solid `Color`
  only — no brush overload — so `AwanAiAura` draws the rotating sweep as a wrapper. There is a
  comment on the file; don't try to move it into `AwanStyles`.
- **The sweep's first and last stop must be the same colour.** An unclosed `sweepGradient` shows the
  seam as a hard line once per turn.
- **`@Play` still writes only the first word of a multi-word category.** Unchanged from the zone
  version, and it is what makes Awan's `"Afternoon Work"` round-trip through `matching()`'s
  `startsWith` arm. Two categories sharing a prefix would resolve to the wrong one.

### Second pass — three things the first emulator run caught

8. **The aura rotated the wrong thing.** `rotate(angle) { drawOutline(...) }` spins the *geometry*,
   so a wide text field became a tall rounded rectangle swinging diagonally across the whole sheet.
   Fixed by rotating the **colour stops** instead: `Brush.sweepGradient` is rebuilt each frame from
   24 samples of a 3-colour cyclic palette shifted by the animation progress, and the outline never
   moves. `colourAt()` wraps modulo the palette size, which is what closes the ring — the palette is
   no longer padded with a repeat of the first colour.

9. **The AI switch was a chip, and vanished into the chip row.** It sat between "Any length" and
   "Must do" wearing the same pill, so it read as a fourth attribute. It is now an `AwanCard` with a
   heading, a sentence explaining what it does, and a 48×28 rail — and it moved *above* the form,
   since it is the offer to skip that form. `MandatoryToggle` keeps the small shared `DotOnARail`;
   the card has its own larger track rather than a size parameter on the shared one.

10. **A created task now shows a receipt instead of auto-dismissing.** `celebrateAndClose` (900 ms
    then close) became `confirm(TaskConfirmation)`: the form crossfades to a panel naming the task
    and its first session, with a Done button. Applied to **all three** creation paths, not just the
    AI one — the AI-schedule path reports the slot the *engine* chose (`sessions.minByOrNull`),
    the other two report what the sentence asked for, and an Inbox task says so rather than
    inventing a time. Knock-ons: `isDirty` is false once `confirmation` is set (the receipt must not
    argue about discarding a task that already exists), `DismissRequested` emits `TaskCreated`
    rather than `Dismissed` when closing a receipt, and `showsAiSwitch`/`showsModeSelector` are
    false there too. `isCelebrating` is now a 900 ms beat that ends on its own instead of a gate the
    dismiss waits behind.

11. **The sheet reopened on the last receipt.** `AddTaskSheet` is composed behind an `if` in
    `AwanApp`, but `hiltViewModel()` scopes to the Activity — the ViewModel outlives every
    dismissal. The old `celebrateAndClose` reset `_state` as a side effect of closing; `confirm()`
    deliberately doesn't, and nothing else did, so tapping `+` reopened the finished flow. Fixed
    with a single `close(event)` that every exit routes through: it resets `_state`, re-runs
    `loadCategories()` (the reset wipes the loaded list), then sends the event. **Any new way out of
    this sheet must go through `close()`** — dismissal is the only place the sheet is wiped.

    Easy to reintroduce because the ViewModel is host-scoped and nothing about the sheet's
    composition hints at it. Two tests cover it, and both were confirmed to fail with the reset
    removed rather than just asserted to pass.

12. **The AI card got its own mark.** A four-point spark on a tinted tile, drawn as a `Path` with
    each arm's control point at the centre — that concavity is what reads as "AI"; straight arms of
    the same radius read as a compass rose. Two sparks breathe on one cycle in opposite phase so
    they alternate rather than pulse together, and the whole animation is skipped when the switch is
    off or motion is reduced.

    Drawn rather than imported: the module has `material-icons-core`, and `AutoAwesome` lives in
    `material-icons-extended` — a few thousand vectors for one glyph.

    Switched on, the card also wears `AwanAiAura` at `shapes.card`, so it and the text field below
    run the same sweep. `AwanCard.selected` is deliberately **not** used for the on state: it
    hardcodes sky, which fights the AI violet. The lift comes from the aura plus a
    `lerp(surface, zoneViolet, 0.10f)` fill — blended, never alpha, per the rim trap in
    `2026-07-23-chips-as-buttons-and-day-picking.md`.

13. **The discard confirmation was a second bottom sheet, and that cannot work.** Symptoms: the
    add-task sheet slid away *first*, then the confirmation appeared; "Keep editing" closed the
    confirmation and left nothing behind; and the next tap anywhere — including on `+` — reopened the
    confirmation out of nowhere.

    One root cause. `ModalBottomSheet` animates itself to `Hidden` and only *then* calls
    `onDismissRequest`, so reacting to the dismissal is always too late. Because `onDismiss` was
    never invoked, `showAddTask` in `AwanApp` stayed `true` and the sheet sat mounted-but-hidden —
    an invisible scrim over the whole screen, eating every tap and re-raising the dialog each time.

    Fixed at the only point that runs *before* the sheet moves. From
    `ModalBottomSheet.kt:151` in material3 1.4.0:

    ```kotlin
    val animateToDismiss: () -> Unit = {
        if (sheetState.anchoredDraggableState.confirmValueChange(Hidden)) { ... }
    }
    ```

    Scrim tap, drag-settle and back press all gate on `confirmValueChange`, so
    `rememberModalBottomSheetState(confirmValueChange = ...)` returning `false` for `Hidden` while
    dirty means the sheet never leaves. The confirmation became `AwanConfirmDialog` (a new DS
    component wrapping `AlertDialog`, alongside — not replacing — `AwanActionSheet`, which onboarding
    still uses): a dialog is its own window and renders in front, which is the only arrangement where
    "discard this?" makes sense.

    The `BackHandler` was removed rather than kept: material3's back path already routes through the
    same veto, and intercepting it skipped the slide-down that a *clean* close should still get.

    **Never confirm a bottom sheet with another bottom sheet.** The first has to leave before the
    second arrives; that is structural, not a styling problem.

### Still to verify

- **`categoryId` inside `POST /v1/tasks/with-sessions`.** The Postman docs omit it from the nested
  `task` object; confirmed verbally, not against the running backend. If it 422s, the fallback is a
  follow-up `PATCH /v1/tasks/{id}`.
- **Whether the dev backend seeds categories.** As of the 2026-07-24 zone-chip work it seeded no
  zones, so only the empty-menu path was ever exercised end to end. Same risk here.
- **Emulator pass**: toggle with text already typed (text survives, highlights and chips vanish,
  aura spins), both confirm paths, discard from review, dark theme, and animations-off
  (`reducedMotion()` freezes the sweep at 0°).
