# Add Task — Todoist-style quick-entry sheet

## Context

Awan currently has no way to add a task outside the onboarding wizard. `POST /v1/tasks` is
already wired (`core/data/.../task/`), but it is reachable only from `OnboardingViewModel`'s
"first task" step, and it can only create an unscheduled Inbox task — no time, no zone.

The ask: a Todoist-style capture surface. A `+` in the bottom navbar opens a bottom sheet with a
single text field; the user types `Gym session tomorrow 6pm @play` and a parser lifts the time
and zone out of the sentence into structured attributes, shown as chips and highlighted inline in
the field. A segmented control at the top switches between **Task** and **Goal** (Goal is a
placeholder in this pass). Voice capture is a separate pass and will reuse this parser verbatim.

Alongside the feature, this corrects a layering violation the current code has: `TaskRepository`
and `CreateTaskUseCase` live in `:core:data` and the repository returns `TaskInfoResponse`, a
Retrofit DTO. Per the ask, contracts move to `:core:domain` and the repository returns
`:core:model` types.

### Decisions taken (from Q&A)

| | |
|---|---|
| Priority | **No `!!n` token.** Backend has no priority field. Tasks are created `mandatory = true`, with a toggle chip in the sheet to turn it off. |
| Voice | Separate pass. This plan ships the parser it will reuse. |
| Entry point | Center `+` in the bottom bar: Home · Calendar · **(+)** · Goals · Profile. Chat leaves the bar. |
| Zones | `@zone` token is parsed and resolved against `GET /v1/zones/date/{date}`; the matched UUID goes on the session. |

### Backend contract (verified against the Postman `Awan` collection, workspace `Dukkan`)

- `POST /v1/tasks` → `TaskInfoResponse`. Body: `title`, `description?`, `estimatedDuration?`,
  `mandatory?`, `estimatedPoints?`, `allowTaskSplitting?`, `goalId?`. **`goalId` null → Inbox.**
- `POST /v1/tasks/with-sessions` → `TaskWithSessionsResponse`. Body: `{ task: {…same…},
  sessions: [{ zoneId?, start, end, status? }] }`. `start`/`end` are **`LocalDateTime`**,
  format `YYYY-MM-DDTHH:mm:ss`, **no timezone offset**. Max 50 sessions. `end` must be > `start`.
- `GET /v1/zones/date/{YYYY-MM-DD}` → `ZoneResponse[]`: `id`, `name`, `startTime` (`HH:mm:ss`),
  `endTime`, `color`, `templateId?`, `templateOverrideId?`. Returns `[]` if no template covers
  that weekday.
- There is **no standalone create-session endpoint** — a scheduled task must go through
  `/with-sessions`.
- There is **no `priority` and no task-level zone/category** field. Zones attach to sessions only.

## Prerequisite: core library desugaring

`minSdk = 24` and desugaring is off (`build-logic/.../KotlinAndroid.kt`), so `java.time` is not
available. Everything below — `LocalDateTime`, `LocalDate`, `LocalTime`, `DayOfWeek` — needs it.

- `gradle/libs.versions.toml`: add `desugarJdkLibs = "2.1.5"` and
  `android-desugarJdkLibs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }`.
- `build-logic/convention/src/main/kotlin/com/apps/awan/KotlinAndroid.kt`: set
  `compileOptions { isCoreLibraryDesugaringEnabled = true }` in **both** the `LibraryExtension`
  and `ApplicationExtension` overloads, and add `coreLibraryDesugaring(desugarJdkLibs)` to the
  project's dependencies from within each function (NiA does exactly this).

Chosen over `kotlinx-datetime` (new dependency, and the API talks `LocalDateTime`) and over
minutes-from-midnight ints (weekday arithmetic gets ugly fast).

## Layers

### `:core:model` — new pure-Kotlin models

`Task.kt`, `TaskSession.kt`, `DayZone.kt`, `TaskDraft.kt`.

```kotlin
data class Task(id, title, description?, estimatedDurationMinutes?, status,
                mandatory, estimatedPoints, allowTaskSplitting, goalId?, dependsOnTaskIds)
data class TaskSession(id, start: LocalDateTime, end: LocalDateTime, status, locked, zoneId?)
data class DayZone(id, name, startTime: LocalTime, endTime: LocalTime, colorHex: String?)
data class TaskDraft(title, description?, mandatory = true, durationMinutes?,
                     startAt: LocalDateTime?, zoneToken: String?, zoneId: String?)
```

`DayZone` is deliberately **not** the existing `core/model/Zone.kt`. That one is the local
onboarding model — string ids (`"study"`), minutes-from-midnight, ARGB int. Backend zones are
UUIDs with `LocalTime` windows. Bending one into the other would break onboarding.

### `:core:network`

- `dto/`: `CreateTaskWithSessionsRequest` (`task: CreateTaskRequest`, `sessions: List<SessionDraftDto>`),
  `SessionDraftDto` (`zoneId?`, `start: String`, `end: String`, `status?`), `SessionDto`,
  `TaskWithSessionsResponse`, `ZoneDto`. `start`/`end` stay `String` in the DTO; formatting with
  `DateTimeFormatter.ISO_LOCAL_DATE_TIME` happens in the data-layer mapper.
- `api/TaskApiService.kt`: add `@POST("v1/tasks/with-sessions")`.
- `api/ZoneApiService.kt` (new): `@GET("v1/zones/date/{date}") suspend fun getZonesByDate(@Path("date") date: String): List<ZoneDto>`.
- `di/NetworkModule.kt`: `providesZoneApiService(retrofit)` — authed `Retrofit`, same shape as
  `providesTaskApiService`.

### `:core:domain` — contracts + the parser

Moved from `:core:data` (delete the originals):

- `task/repository/TaskRepository.kt` — now returns `:core:model` types, and gains
  `suspend fun createTaskWithSessions(draft: TaskDraft, sessions: List<TaskSession>): Result<Task>`.
- `task/usecase/CreateTaskUseCase.kt` — unchanged signature apart from returning `Task`;
  `OnboardingViewModel` keeps using it.

New:

- `task/usecase/CreateTaskFromDraftUseCase.kt` — the single decision point:
  `startAt == null` → `createTask` (Inbox, unscheduled); otherwise `createTaskWithSessions` with
  one session `[startAt, startAt + (durationMinutes ?: 60)]`.
- `zone/repository/ZoneRepository.kt` — `suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>>`.
- `zone/usecase/GetZonesForDateUseCase.kt`.
- `task/parser/TaskInputParser.kt` + `ParsedTaskInput.kt` + `task/usecase/ParseTaskInputUseCase.kt`.

Follows the existing shape exactly — `core/domain/.../auth/usecase/VerifyOtpUseCase.kt` is the
template (constructor `@Inject`, `suspend operator fun invoke`, returns `core.common.result.Result`).

#### Parser spec

Pure function, no Android, no dependency — regex plus a small keyword table. Scans in this order,
removing each match from the remainder:

| Token | Matches |
|---|---|
| Zone | `@\w+` |
| Duration | `for 45m`, `45 min`, `1h`, `1h30`, `90 minutes` |
| Date | `today`, `tonight`, `tomorrow`, `mon`…`sunday`, `next <weekday>`, `by <weekday>`, `d/M`, `d-M` |
| Time | `3pm`, `3:30 pm`, `15:00`, `noon`, `midnight` |

Resolution, given an injected `Clock`/`LocalDateTime` "now" so tests are deterministic:

- date + time → that instant.
- date only → that date at **09:00**.
- time only → today if still ahead of now, else tomorrow.
- neither → `startAt = null`, task goes to the Inbox unscheduled.
- Leftover text, whitespace-collapsed and trimmed, becomes the title. Empty title → invalid,
  send button disabled.

Returns `ParsedTaskInput(title, startAt, durationMinutes, zoneToken, tokens: List<ParsedToken>)`
where `ParsedToken(range: IntRange, kind: DateTime | Duration | Zone)` drives the inline
highlighting.

`ponytail:` English keywords only in v1 — the token table is a `Map<String, …>`, so the upgrade
path is a locale-keyed table plus `DayOfWeek.getDisplayName(locale)`. All *UI* strings are
localised normally; only the recognised input keywords are English. Also `ponytail:` the 60-minute
fallback duration — the real value is the user's `preferredSessionDuration`, which needs a profile
fetch this pass doesn't do.

### `:core:data`

- `task/TaskRepositoryImpl.kt` — implement the domain contract, map DTO→model via new
  `task/TaskMappers.kt`. Delete `task/TaskRepository.kt` and `task/CreateTaskUseCase.kt`.
- `task/remote/TaskRemoteDataSource(.Impl)` — add `createTaskWithSessions`, wrapped in the
  existing `safeApiCall(dispatcher, json)`.
- `zone/` — `remote/ZoneRemoteDataSource(.Impl)`, `ZoneRepositoryImpl`, `ZoneMappers.kt`,
  mirroring the task package one-for-one.
- `di/DataModule.kt` — bind the two new zone types; the existing `bindTaskRepository` now binds
  to the **domain** interface.

### `:feature:add-task` (new module)

Single module, **no api/impl split**: the sheet is not a navigation destination, so there is no
`Route` to export and nothing but `:app` depends on it. Precedent for state-driven (not
route-driven) sheets is `feature/onboarding/.../ui/components/ZoneSheet.kt`.

`settings.gradle.kts`: `include(":feature:add-task")`. Build file copies
`feature/onboarding/impl/build.gradle.kts` (`awan.android.feature` + `awan.android.compose`,
depending on `:core:design-system`, `:core:domain`, `:core:model`).

MVI split matching `feature/onboarding/impl/presentation/`:

- `presentation/AddTaskState.kt` — `mode: Task|Goal`, `rawInput: TextFieldValue`, `description`,
  `parsed: ParsedTaskInput?`, `resolvedZone: DayZone?`, `mandatory: Boolean = true`,
  `isSubmitting`, `errorMessage: Int?`.
- `presentation/AddTaskAction.kt` / `AddTaskEvent.kt` (`Dismiss`, `TaskCreated`).
- `presentation/AddTaskViewModel.kt` — `@HiltViewModel`, injects `ParseTaskInputUseCase`,
  `GetZonesForDateUseCase`, `CreateTaskFromDraftUseCase`. Never touches a repository. Re-parses
  on every keystroke (pure and cheap); when the parse yields a `zoneToken` *and* a date, fetches
  that day's zones and name-matches case-insensitively. No match → chip renders "unknown zone",
  `zoneId` stays null, the task still creates.

UI, one file per widget per the extract-widgets-not-builder-functions rule:

- `ui/AddTaskSheet.kt` — `ModalBottomSheet` root, `hiltViewModel()`, `ObserveAsEvents`.
- `ui/components/AddTaskModeSelector.kt` — the Task | Goal segmented control, built from
  `AwanTheme.shapes.pill` + `AwanTheme.colors`.
- `ui/components/TaskInputField.kt` — wraps the existing `AwanTextField`, which already accepts
  `visualTransformation`.
- `ui/components/TokenHighlightTransformation.kt` — `VisualTransformation` applying
  `SpanStyle(background = tone.copy(alpha = 0.18f), color = tone)` over each `ParsedToken`
  (sky = date/time, violet = duration, tangerine = zone — all existing `AwanColors` tokens).
- `ui/components/TaskAttributeChips.kt` — the row under the field, built on the existing
  `AwanChip` / `AwanChipTone`: date-time, duration, zone, and a toggleable Mandatory chip.
- `ui/components/GoalPlaceholder.kt` — what the Goal segment shows for now; send disabled.
- `ui/components/SendButton.kt` — the circular ↑, disabled while the title is blank or submitting.

`res/values/strings.xml` + `res/values-ar/strings.xml`, keys prefixed `add_task_`.

### `:app`

- `AwanApp.kt` — extract an `AwanBottomBar` composable: two `NavigationBarItem`s, the center `+`,
  two more. `var showAddTask by rememberSaveable { mutableStateOf(false) }` above the `Scaffold`;
  when true, render `AddTaskSheet(onDismiss = { showAddTask = false })`.
- `TopLevelDestination.kt` — drop `CHAT` (it drives the bar), and replace the hardcoded `label:
  String` with `@StringRes labelRes: Int`. `MainActivity`'s `topLevelKeys` and `chatEntry()`
  **stay as they are**, so `ChatRoute` remains registered and routable for whenever chat gets an
  entry point; only its bar item goes.
- `res/values/strings.xml` + `values-ar` — add `navigation_goals`, `navigation_profile`,
  `navigation_add_task` (`navigation_home` / `navigation_calendar` already exist).
- `build.gradle.kts` — `implementation(project(":feature:add-task"))`.

### `:feature:onboarding:impl`

`OnboardingViewModel.kt` — import path change for `CreateTaskUseCase` (`core.data.task` →
`core.domain.task.usecase`) and adapt to the `Task` return instead of `TaskInfoResponse`. Same
for `OnboardingViewModelTest` / `FakeOnboardingRepository`.

## Verification

Per the workflow rule, **first** run one existing test file to confirm the harness still works
after the desugaring change, before writing new tests:

```bash
./gradlew :core:data:testDebugUnitTest --tests "*TaskRepositoryImplTest"
```

Then:

1. `./gradlew assembleDebug` — catches the desugaring config and the new module wiring.
2. New `core/domain/src/test/.../task/parser/TaskInputParserTest.kt` — the one runnable check the
   non-trivial logic leaves behind. Table-driven against a fixed "now", covering: bare title;
   `tomorrow 6pm`; `mon 3 pm`; `by fri 4 pm`; `15:00`; time-only rolling to tomorrow;
   `for 45m` / `1h30`; `@play`; all tokens at once; a title that is only tokens (invalid);
   `@nosuchzone` (parses, resolves to null).
3. `./gradlew testDebugUnitTest` — plus updated `TaskRepositoryImplTest`, new
   `ZoneRepositoryImplTest`, new `AddTaskViewModelTest`.
4. `./gradlew detekt lint` — `lint` has `error += "MissingTranslation"`, so it is the gate that
   proves every new `add_task_*` and `navigation_*` key exists in `values-ar` too.
5. Manual, against the backend on `localhost:8080` (`AWAN_BASE_URL`): log in, tap `+`, type
   `Gym session tomorrow 6pm @play`, confirm the chips read *Tomorrow 6:00 PM · 60m · Play* and
   the highlighting matches, send, then verify with Postman's **Get Task Sessions**
   (`GET /v1/tasks/{taskId}/sessions`) that the session start/end and `zoneId` are right. Repeat
   with a bare title and confirm it lands in the Inbox via **Get Inbox** with no session.

## Before starting

- **Copy this plan** to `docs/feature/add-task/2026-07-23-quick-add-task-plan.md` per CLAUDE.md,
  and append the `## Implementation notes (what actually differed)` section when the work lands.
- **Jira: AWAN-82** *"Implement Add Task Flow"* (Story, In Progress, assigned to you). Branch
  `feature/AWAN-82-quick-add-task`; every commit message prefixed `AWAN-82: `.

---

Skipped: local Room persistence (no `:core:database` task entities yet — this is remote-only, so
the sheet needs the network to succeed); the Goal segment's real flow; voice capture; a
`preferredSessionDuration` fetch. Add each when its own ticket comes up.

## Implementation notes (what actually differed)

### Verification status

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**.
- `./gradlew testDebugUnitTest --rerun-tasks` — **BUILD SUCCESSFUL**, 95 tests, 0 failures, 0 errors.
  New: 22 in `TaskInputParserTest`, 11 in `AddTaskViewModelTest`, 3 added to `TaskRepositoryImplTest`,
  1 added to `TaskRemoteDataSourceTest`.
- `./gradlew lint` — **BUILD SUCCESSFUL**. `:app` runs it with `error += "MissingTranslation"`, so
  this is what proves every new `add_task_*` / `navigation_*` key exists in `values-ar`.
- `./gradlew detekt` — **could not run**. CLAUDE.md documents a `detekt` task and a root `detekt.yml`;
  neither exists in the repo (no detekt plugin in `libs.versions.toml`, no config file). Pre-existing
  gap, unrelated to this change — either wire the plugin up or correct CLAUDE.md.
- Not run: `connectedDebugAndroidTest` (no device), and the end-to-end pass against a local backend
  (step 5 of the plan) — the sheet is remote-only, so that check is still outstanding.

### Deviations from the plan

1. **`CreateTaskUseCase` and `CreateTaskFromDraftUseCase` merged into one.** The plan had two; the
   split bought nothing once both took the same `TaskDraft`. There is a single
   `core/domain/task/usecase/CreateTaskUseCase.kt` that branches on `draft.startAt` — null goes to
   `POST /v1/tasks`, non-null to `POST /v1/tasks/with-sessions`. `OnboardingViewModel` now passes a
   `TaskDraft` instead of named arguments.
2. **Extra `:core:model` types.** `SessionDraft` (a session with no id yet — `TaskSession` requires
   one, so the repository couldn't take a list of those) and `TaskWithSessions` (the
   `/with-sessions` response shape).
3. **`Result.map` added to `:core:common`.** Every repository was about to repeat the same
   three-branch `when` to map a DTO payload; one inline extension replaces all of them.
4. **`Clock` is injected, not read statically.** New `core/common/di/ClockModule.kt`.
   `ParseTaskInputUseCase` and `AddTaskViewModel` both take it, which is what makes the
   fixed-clock tests possible. `AddTaskState.today` carries the date into the chip labels so the UI
   never reads a clock itself.
5. **`ObserveAsEvents` moved** from `feature/onboarding/impl/ui/` to `:core:design-system`.
   `:feature:add-task` needed it and features must not depend on another feature's `impl`. The
   alternative was duplicating it.
6. **`:core:domain` now exposes `:core:model` and `:core:common` as `api`, not `implementation`** —
   the repository contracts have those types in their public signatures.
7. **No `SendButton.kt` / `TaskInputField.kt`.** The plan listed them; the existing `AwanButton`
   (which already has `isLoading`) and `AwanTextField` (which already accepts
   `visualTransformation`) cover both, so nothing new was written.
8. **The `+` is a `NavigationBarItem`, not a floating button.** `AwanBottomBar` splits
   `TopLevelDestination.entries` in half and drops the button between the halves — so the button
   stays centred as long as the enum has an even number of entries. That invariant is a comment on
   the enum, not something the type enforces.

### Traps

- **Core library desugaring was a hard prerequisite.** minSdk is 24, so `java.time` does not exist
  on device without it. It is now enabled centrally in `build-logic/.../KotlinAndroid.kt` for every
  Android module (both the library and application overloads), which also adds the
  `coreLibraryDesugaring` dependency. Removing it will break at runtime on API 24–25 devices, not
  at compile time — the failure mode is a `NoClassDefFoundError`, not a build error.
- **Session times must not carry a timezone.** The backend field is a Java `LocalDateTime` and the
  format is `YYYY-MM-DDTHH:mm:ss`. `TaskMappers.kt` formats with `ISO_LOCAL_DATE_TIME` deliberately;
  switching to an `Instant`/`OffsetDateTime` would silently start sending offsets the server
  rejects. `TaskRepositoryImplTest` asserts the exact string.
- **Token-match order in the parser is load-bearing.** Zone → duration → time → date, with each
  match claiming a character range that later matchers must not overlap. Reordering makes `7am`
  parse as "7 minutes" (the duration regex would reach the `m`). `TaskInputParserTest` has
  `a duration does not swallow a meridiem time` and `token ranges never overlap` guarding this.
- **The weekday regex relies on `\b` for backtracking.** The alternation is built from the
  `WEEKDAYS` map in insertion order, so `mon` is tried before `monday`; only the trailing `\b`
  forces the backtrack to the longer form. Dropping it silently truncates every full weekday name.
- **A title containing a weekday word gets eaten** — "Sun salutation" parses as Sunday plus the
  title "salutation". Known limitation of a keyword parser, not a bug to chase.
- **Parser keywords are English-only.** All UI strings are localised (including `values-ar`) and
  dates/times are *formatted* with the device locale, but the words the parser *recognises* are
  English. The upgrade path is noted in the KDoc on `TaskInputParser`.
- **Tasks now default to `mandatory = true` everywhere**, including onboarding's first task, which
  previously sent `false`. That follows from the product decision to make Mandatory the default with
  a toggle, but it is a behaviour change to onboarding, not just to the new sheet.
- **Chat lost its bottom-bar item** but is still a registered route and a live top-level key in
  `MainActivity`. It is currently unreachable from the UI until something links to it.
