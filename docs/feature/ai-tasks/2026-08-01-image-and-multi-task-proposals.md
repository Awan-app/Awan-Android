# AWAN-101 — AI task proposals: image input, multi-task review, bulk accept

## Context

The backend replaced the single-task AI endpoint with a **proposal** contract, and added an image
pipeline plus a bulk create. Verified today against Postman workspace `Dukkan`, collection `Awan`
(updated 2026-08-01):

| Call | Before (what the app ships) | Now |
|---|---|---|
| `POST /v1/ai/task-create` | `?persist=false`, body `{title, description}` → `AiTaskPreviewResponse` (one task) | body `{text}` (≤4000 chars) → `TaskProposalResponse` (**many** tasks), never persists |
| `POST /v1/ai/task-create` | no query → persisted `TaskWithSessionsDto` (onboarding) | **gone** — the endpoint never persists |
| `POST /v1/ai/image-to-tasks` | — | multipart `image` (PNG/JPEG/WebP/GIF, ≤10 MB) + optional `note` → same `TaskProposalResponse` |
| `POST /v1/tasks/with-sessions/bulk` | — | `{tasks: [...]}` (≤50, atomic) → `{tasks: [TaskWithSessionsResponse]}` |

`TaskProposalResponse` = `{ sourceSummary: String?, tasks: [{ draft, aiProposedSessions, reason }], timestamp }`
where `draft` is literally a `/tasks/with-sessions` request body.

Two consequences the ticket has to absorb:

1. **The add-task sheet's whole AI stage machine is obsolete.** `COMPOSING → WORKING → REVIEW → MANUAL`
   was built around one task coming back, folded into the typed sentence and scheduled by a second
   `POST /v1/schedule/task` call. The new contract returns a *list* with sessions already attached.
   A sentence with chips cannot express N tasks.
2. **Onboarding breaks too.** `AiTaskRepositoryImpl.createAndScheduleTask` (`core/data/.../task/AiTaskRepositoryImpl.kt`)
   sends `{title}` and expects a persisted task id back so it can call `/v1/schedule/task`. Both
   assumptions are now false. It is fixed in this change, not deferred.

Outcome: the sheet becomes pure capture (text, note, **photo**); a new full-screen destination owns
the AI call, a mascot loading animation, the proposal list with per-task editing, and one bulk
accept.

---

## Shape of the flow

```
  [ + FAB ] → AddTaskSheet (capture)                    NEW full screen
  ┌──────────────────────┐                          ┌────────────────────────┐
  │ ~ sky ~   🐑         │   AddTaskEvent           │ ← Awan's plan          │
  │ ┌──────────────────┐ │   .AiRequested           │ ┌────────────────────┐ │
  │ │ describe it…     │ │   (text, note, imageUri) │ │ 🖼 thumbnail        │ │
  │ └──────────────────┘ │  ──────────────────────▶ │ │ "what I read…"     │ │
  │ [note…]              │  :app dismisses sheet    │ └────────────────────┘ │
  │ [🖼 Add a photo]     │  + navigator.navigate(   │ ┌────────────────────┐ │
  │ [   Ask Awan   ]     │      AiTaskProposalsRoute│ │☑ Build login page  │ │
  └──────────────────────┘    )                     │ │ 60m · 30pt · Dev   │ │
                                                    │ │ ✦ Thu 09:00–10:00  │ │
   aiEnabled off → today's manual create, untouched │ └────────────────────┘ │
                                                    │══════════════════════ │
                                                    │ [   Add 2 tasks    ]  │
                                                    └────────────────────────┘
```

Back from the full screen returns to whatever screen was showing. The bottom nav bar hides for free —
`AwanApp.kt:101-104` only composes `AwanBottomNavBar` when `currentKey` is a `TopLevelDestination`.

---

## New module: `:feature:ai-tasks` (api + impl)

The repo's convention for a navigable screen (`CLAUDE.md`, and `:feature:calendar` is the exact
template). `feature/ai-tasks/` already exists on disk as stale `build/` output from a removed module —
delete those directories, then create real sources.

The route carries its own input, so the screen has **no** dependency on `:feature:add-task` and no
shared ViewModel is needed:

```kotlin
// feature/ai-tasks/api/.../AiTaskProposalsRoute.kt
@Serializable
data class AiTaskProposalsRoute(
    val text: String,
    val note: String? = null,
    val imageUri: String? = null,
) : Route
```

`impl` mirrors `feature/calendar/impl`: `navigation/AiTasksEntryProvider.kt` exposing
`fun EntryProviderScope<Route>.aiTasksEntry(onBack: () -> Unit, onTasksCreated: (Int) -> Unit)`,
plus `presentation/` (State/Action/Event/ViewModel) and `ui/`.

Wiring in `:app`: two `include(...)` lines in `settings.gradle.kts`, two `implementation(project(...))`
in `app/build.gradle.kts`, one `aiTasksEntry(...)` line inside the existing `entryProvider { }` block
in `AwanApp.kt`.

---

## Network — `:core:network`

**`api/TaskApiService.kt`**

```kotlin
@POST("v1/ai/task-create")
suspend fun proposeTasksFromText(@Body request: AiTextToTasksRequest): TaskProposalResponse

@Multipart
@POST("v1/ai/image-to-tasks")
suspend fun proposeTasksFromImage(
    @Part image: MultipartBody.Part,
    @Part("note") note: RequestBody?,
): TaskProposalResponse

@POST("v1/tasks/with-sessions/bulk")
suspend fun createTasksWithSessions(
    @Body request: BulkCreateTasksWithSessionsRequest,
): TasksWithSessionsResponse
```

Delete `createTaskWithAi` and `previewTaskWithAi`. Retrofit + OkHttp already ship `@Multipart` /
`MultipartBody` — **no new dependency**.

**DTOs** — new `dto/TaskProposalResponse.kt`:

```kotlin
@Serializable data class TaskProposalResponse(
    @SerialName("sourceSummary") val sourceSummary: String? = null,
    @SerialName("tasks") val tasks: List<ProposedTaskDto> = emptyList(),
)
@Serializable data class ProposedTaskDto(
    @SerialName("draft") val draft: CreateTaskWithSessionsRequest,
    @SerialName("aiProposedSessions") val aiProposedSessions: List<SessionDraftDto> = emptyList(),
    @SerialName("reason") val reason: String? = null,
)
```

`draft` decodes straight into the **existing** `CreateTaskWithSessionsRequest` / `CreateTaskRequest` /
`SessionDraftDto` — no parallel proposal-task DTO. **Check first** that every `CreateTaskRequest`
field has a default; if any is required, decoding a proposal that omits it throws. Add defaults where
missing.

New `dto/AiTextToTasksRequest.kt` (`{text}`), `dto/BulkTasksRequests.kt`
(`BulkCreateTasksWithSessionsRequest(tasks)`, `TasksWithSessionsResponse(tasks: List<TaskWithSessionsDto>)`).

**Delete** `dto/AiTaskPreviewResponse.kt` and `dto/CreateTaskWithAiRequest.kt`.

`AiTimeoutInterceptor` (`interceptor/AiTimeoutInterceptor.kt:17`) matches `/ai/` as a substring, so
`v1/ai/image-to-tasks` already gets the 90 s read/write window. No change.

---

## Models — `:core:model`

```kotlin
data class TaskProposals(val sourceSummary: String?, val tasks: List<TaskProposal>)

data class TaskProposal(
    val draft: TaskDraft,
    val sessions: List<ProposedSession>,
    val reason: String?,
)

/** [isAiSuggested] distinguishes Awan's own availability-grounded pick from timing the source stated. */
data class ProposedSession(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zoneId: String? = null,
    val isAiSuggested: Boolean = false,
)

data class TaskWithSessionsDraft(val task: TaskDraft, val sessions: List<SessionDraft>)
```

Per your call: the two server-side lists are **merged into one `sessions` list** at the mapper.
`draft.sessions` → `isAiSuggested = false`, `aiProposedSessions` → `isAiSuggested = true`. The UI shows
one time list per task; AI-picked entries wear a sparkle badge; every entry is editable and deletable.

**Delete** `AiTaskSuggestion.kt` — it modelled a single unpersisted task and has no callers left.

---

## Domain — `:core:domain`

`task/repository/TaskRepository.kt`:
- **remove** `previewTaskWithAi`
- **add** `proposeTasksFromText(text: String): Result<TaskProposals>`
- **add** `proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?): Result<TaskProposals>`
- **add** `createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>>`
- keep `createTask`, `createTaskWithSessions`, `scheduleTask`, `deleteTask`

`ByteArray` + `mimeType` keeps the domain Android-free: reading and downscaling the `content://` Uri
happens in the feature module, never below it.

New use cases in `task/usecase/`:
- `ProposeTasksFromTextUseCase` — trims, rejects blank, rejects > 4000 chars before the round trip
- `ProposeTasksFromImageUseCase` — rejects > 10 MB and any mime outside png/jpeg/webp/gif
- `CreateTasksUseCase(drafts)` — the bulk accept; caps at 50 per the contract

**Delete** `PreviewTaskWithAiUseCase.kt` and `ScheduleTaskWithAiUseCase.kt` (no callers after this
change). `TaskRepository.scheduleTask` and its DTO/mapper/test plumbing stay — the endpoint is live and
still mapped; deleting it is churn, not simplification.

---

## Data — `:core:data`

- `task/remote/TaskRemoteDataSource(.Impl).kt` — swap the two AI methods for the three above, each a
  one-line `safeApiCall`. Build the `MultipartBody.Part` here (`"image"`, filename `image.<ext>`,
  `bytes.toRequestBody(mimeType.toMediaType())`) and the `note` `RequestBody` — keeping okhttp types
  out of the repository.
- `task/TaskMappers.kt` — add `TaskProposalResponse.toModel(): TaskProposals` (merging the two session
  lists as above), `TaskDraft/List<SessionDraft> → CreateTaskWithSessionsRequest` reusing the existing
  `toRequest(sessions)`, and `TasksWithSessionsResponse.toModel(): List<Task>`. Delete
  `AiTaskPreviewResponse.toModel()`. Session times keep `DateTimeFormatter.ISO_LOCAL_DATE_TIME` — the
  backend speaks offsetless `LocalDateTime` and converting here would invent information.
- `task/TaskRepositoryImpl.kt` — the three new pass-throughs.
- **`task/AiTaskRepositoryImpl.kt` — the onboarding fix.** `createAndScheduleTask(title)` becomes:
  `proposeTasksFromText(title)` → first proposal (empty list ⇒ `Success(null)`) → `createTaskWithSessions`
  with that proposal's draft and its merged sessions → map the earliest session into `FirstTask`. The
  `POST /v1/schedule/task` call disappears: the proposal already carries availability-grounded timing.
  `AiTaskRepository`'s domain contract (`Result<FirstTask?>`) and `OnboardingViewModel` are untouched,
  so the diff stops at this file plus its test.

Existing time helpers to reuse rather than rewrite: `core/data/util/TimeFormat.kt:29` `parseIsoDateTime`
(offset-aware with a plain-`LocalDateTime` fallback) and the session mappers at `TaskMappers.kt:98-128`.

---

## `:feature:add-task` — becomes capture only

The sheet stops calling the AI and stops reviewing anything.

- **`AddTaskState.kt`** — delete `AddTaskAiStage`, `aiPoints`, `aiSplittable`. Replace the enum with
  `val aiEnabled: Boolean = false`. Add `val imageUri: String? = null`.
  - `isComposing` → `aiEnabled`; `isReviewing`/`showsWhenChip`/`showsAttributeChips` collapse
    accordingly (chips hide while `aiEnabled`, as `COMPOSING` did).
  - `canSubmit` when `aiEnabled` = `input.isNotBlank() || imageUri != null` — a photo alone is a valid
    ask; the title field is the optional note in that case.
  - `isDirty` gains `|| imageUri != null`.
- **`AddTaskAction.kt`** — delete `ScheduleWithAi`, `ScheduleManually`. Add `ImagePicked(uri: String?)`,
  `ImageCleared`.
- **`AddTaskEvent.kt`** — add `data class AiRequested(val text: String, val note: String?, val imageUri: String?)`.
- **`AddTaskViewModel.kt`** — delete `askAwan()`, `intoReview()`, `scheduleWithAi()` and the
  `previewTaskWithAi` / `scheduleTaskWithAi` / (now unused) injections. `submit()` when `aiEnabled`
  routes through the existing `close(AiRequested(...))` so the sheet is wiped on the way out — the
  reset-on-every-exit rule from `2026-07-25-ai-task-creation.md` §11 still holds, and `AiRequested` is
  a new exit that must not bypass it. Everything for the manual path (`createDirectly`, `confirm`,
  `TaskConfirmationPanel`, discard guard) is unchanged.
- **`ui/AddTaskSheet.kt` / new `ui/components/ImageAttachment.kt`** — a photo row under the note:
  `AwanButton(variant = Quiet, icon = …)` "Add a photo" when empty, else a small rounded thumbnail with
  an ✕. Two launchers:
  - `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` — gallery/screenshots,
    no permission.
  - `ActivityResultContracts.TakePicture()` into a cache-dir `FileProvider` Uri — no `CAMERA`
    permission is required as long as the manifest does not declare one, which it must not.
  - New `feature/add-task/src/main/AndroidManifest.xml` contributing the `FileProvider`
    (`${applicationId}.fileprovider`) + `res/xml/awan_file_paths.xml` with a `cache-path`. Manifest
    merger folds it into `:app`.
  - Thumbnail decode: `produceState` + `ImageDecoder`/`BitmapFactory` at ~400 px. No Coil, no new
    dependency.
- **`:app/AwanApp.kt`** — pass `onAiRequested` into `AddTaskSheet`: set `showAddTask = false`, then
  `navigator.navigate(AiTaskProposalsRoute(text, note, imageUri))`.

The Uri is passed as a `String` through an in-memory route key. `PickVisualMedia` grants read access
for the process lifetime and the camera file is ours, so no `takePersistableUriPermission` is needed —
but the route is **not** restorable across process death, which is acceptable for a transient AI ask.

---

## `:feature:ai-tasks:impl` — the full screen

**State**

```kotlin
data class AiTasksState(
    val isLoading: Boolean = true,
    val imageUri: String? = null,
    val sourceSummary: String? = null,
    val proposals: List<ProposalUi> = emptyList(),
    val isAccepting: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val errorDetail: String? = null,
)
data class ProposalUi(
    val id: Int,
    val draft: TaskDraft,
    val sessions: List<ProposedSession>,
    val reason: String?,
    val isSelected: Boolean = true,
    val isExpanded: Boolean = false,
)
val selectedCount get() = proposals.count { it.isSelected }
val isEmptyResult get() = !isLoading && errorMessage == null && proposals.isEmpty()
```

**Actions** — `Load`, `Retry`, `ToggleSelected(id)`, `ToggleExpanded(id)`, `TitleChanged(id, …)`,
`DescriptionChanged`, `DurationPicked`, `CategoryPicked`, `MandatoryToggled`, `SessionTimeEdited(id, index, start)`,
`SessionRemoved(id, index)`, `SessionAdded(id)`, `AcceptSelected`, `BackRequested`, `DiscardConfirmed`,
`DiscardCancelled`.

**ViewModel** — injects `ProposeTasksFromTextUseCase`, `ProposeTasksFromImageUseCase`,
`CreateTasksUseCase`, `GetCategoriesUseCase` (for the category menu), `Clock`, and a feature-local
`ImageSource` (below). `Load` branches on `imageUri`: present ⇒ image endpoint with `text` as the
`note`; absent ⇒ text endpoint. `AcceptSelected` maps the selected `ProposalUi`s to
`TaskWithSessionsDraft` and fires the single bulk call, then emits `AiTasksEvent.TasksCreated(count)`.

**`ImageSource`** — `@Singleton class ImageSource @Inject constructor(@ApplicationContext context)`
with `suspend fun read(uri: String): Result<ImageBytes>`: opens the `content://` stream, decodes with
`BitmapFactory.Options.inSampleSize` to a max edge of **2048 px**, re-encodes JPEG at quality 85, and
returns `(bytes, "image/jpeg")`. **This is not optional** — a modern phone photo is 3–12 MB and the
endpoint rejects anything over 10 MB with `INVALID_OPERATION` (400). One consumer, so a concrete class,
no interface.

**UI** — `ui/AiTasksScreen.kt` + `ui/components/`:

- *Header* — `AwanIconButton` back + title, on the `AwanTheme.styles.screen` gradient.
- *Loading* — the piece the spec explicitly asks for. `AwanMascot(MascotExpression.Curious)` at ~160 dp
  inside `AwanAiAura(active = true, shape = shapes.pill)`, with a `Crossfade` cycling 4 reassurance
  strings every ~2.2 s ("Reading your note…", "Checking your calendar…", "Working out how long…",
  "Almost there…"). Gate the cycle on `reducedMotion()` — show the first string statically.
  `core/design-system/res/raw/mascot.json` (a real 6 s Lottie idle loop) is available if the vector
  mascot reads as too static; using it means adding `implementation(libs.lottie.compose)` to this
  module, as `:feature:auth:impl` already does.
- *Source card* — only when `sourceSummary != null` (image path): the thumbnail beside the vision
  model's text inside an `AwanCard`, collapsed to 3 lines with a "show more".
- *Proposal card* — `AwanCard(selected = isSelected, onClick = ToggleSelected)`: title (inline
  `AwanTextField` when expanded), description, then a chip row reusing the shapes from
  `feature/add-task/.../TaskAttributeChips.kt` (duration menu, category menu, must-do toggle) and an
  `AwanBadge` for points. `categoryId == null` ⇒ `AwanBadge(tone = Neutral)` "Unassigned".
- *Session rows* — one row per `ProposedSession`: time range, an `AwanBadge(tone = Violet, leadingIcon = ✦)`
  reading `ai_tasks_session_suggested` when `isAiSuggested`, tap to open the existing
  `AwanDatePickerDialog` → `AwanTimePickerDialog` pair, and an ✕ to delete it. A task with no sessions
  says so ("No time set — lands in your Inbox"). `reason` renders as `styles.metaText` under the rows.
- *Sticky bottom bar* — a `Box`-aligned surface with `AwanButton(isLoading = isAccepting)` reading
  `ai_tasks_accept` ("Add %1$d tasks", plural-aware), disabled at `selectedCount == 0`. The list needs
  its own bottom padding for it, the same way `AwanScheduleTimeline.kt:75` reserves 160 dp for the
  overlaid nav bar.
- *Empty* — "No actionable tasks found", plus the spec's advice line (try a clearer photo / a fuller
  description) and a retry button.
- *Error* — message + retry, following the established `HomeScreen.kt:110-165` shape. Map the codes the
  contract names: `Api(415, UNSUPPORTED_IMAGE_TYPE)`, `Api(400, INVALID_OPERATION)`, `Timeout`,
  everything else generic. Note `NetworkErrorMapper.kt:23` collapses 5xx to `AppError.Server(code)`
  and drops `errorCode`, so `503 AI_UNAVAILABLE` can only be matched on the status — that is enough
  for "Awan is busy, try again" and is not worth changing `:core:common` for.
- *Back with unaccepted proposals* — reuse `AwanConfirmDialog` exactly as `AddTaskSheet.kt:131-141`
  does. Nothing is persisted until accept, so there is no cleanup, only the confirmation.

---

## Strings

New `ai_tasks_*` keys in **both** `feature/ai-tasks/impl/src/main/res/values/strings.xml` and
`values-ar/strings.xml`: loading lines ×4, source-summary title/show-more, session suggested badge,
no-session line, unassigned badge, accept button (`plurals`), empty title/body/advice, retry, the four
error messages, discard title/body/confirm/cancel, and content descriptions for back/checkbox/remove-session.

New `add_task_*` keys in both files: `add_task_add_photo`, `add_task_photo_from_gallery`,
`add_task_photo_take`, `add_task_remove_photo`, `add_task_photo_content_description`.

**Delete** the now-dead add-task keys: `add_task_ai_working`, `add_task_ai_review_hint`,
`add_task_ai_schedule_with_ai`, `add_task_ai_schedule_manually`, `add_task_error_ai_failed`,
`add_task_error_ai_schedule_failed` — from both locales.

`:app` lint runs `MissingTranslation` as an error with `abortOnError = true`, so a missing Arabic key
fails the build.

---

## Critical files

| Area | Files |
|---|---|
| Contract | `core/network/.../api/TaskApiService.kt`, new `dto/TaskProposalResponse.kt`, `dto/AiTextToTasksRequest.kt`, `dto/BulkTasksRequests.kt`; delete `dto/AiTaskPreviewResponse.kt`, `dto/CreateTaskWithAiRequest.kt` |
| Domain | `core/domain/.../task/repository/TaskRepository.kt`, new `usecase/ProposeTasksFrom{Text,Image}UseCase.kt`, `usecase/CreateTasksUseCase.kt`; delete `PreviewTaskWithAiUseCase.kt`, `ScheduleTaskWithAiUseCase.kt` |
| Models | new `core/model/.../{TaskProposals,TaskProposal,ProposedSession,TaskWithSessionsDraft}.kt`; delete `AiTaskSuggestion.kt` |
| Data | `core/data/.../task/{TaskRepositoryImpl,TaskMappers}.kt`, `task/remote/TaskRemoteDataSource(+Impl).kt`, **`task/AiTaskRepositoryImpl.kt`** (onboarding fix) |
| Capture | `feature/add-task/.../presentation/*`, `ui/AddTaskSheet.kt`, new `ui/components/ImageAttachment.kt`, new `src/main/AndroidManifest.xml` + `res/xml/awan_file_paths.xml` |
| Review | new `feature/ai-tasks/{api,impl}` — copy `feature/calendar/{api,impl}` structure verbatim |
| Shell | `settings.gradle.kts`, `app/build.gradle.kts`, `app/.../AwanApp.kt` |

Reference verticals: `core/data/task/` (remote → repo → mapper → `@Binds` in `di/DataModule.kt:83-135`)
and `feature/calendar/` (api Route + impl EntryProvider + Route-screen wrapper). Reuse `ObserveAsEvents`,
`AwanCard`, `AwanBadge`, `AwanChip`, `AwanButton(isLoading)`, `AwanDatePickerDialog`,
`AwanTimePickerDialog`, `AwanConfirmDialog`, `AwanAiAura`, `AwanMascot`, `CascadeItem`, `reducedMotion()`
— all already in `:core:design-system`. No new design-system component is needed.

## Sequencing

1. Contract layer (network DTOs + service) and models — compiles alone.
2. Domain contract + use cases, data impls + mappers, **including `AiTaskRepositoryImpl`**. Green tests
   here mean onboarding is safe before any UI moves.
3. `:feature:ai-tasks` api + impl, wired into `:app`, driven with the text path only.
4. Strip the sheet down to capture, add the picker/camera, wire `AiRequested` → navigate.
5. Image path end to end (downscale, multipart, source-summary card).

Steps 1–2 are a self-contained commit; landing them separately keeps the UI diff readable.

## Verification

1. **Harness check first**: `./gradlew :feature:add-task:testDebugUnitTest` on the existing
   `AddTaskViewModelTest` (52 tests) before writing anything new. (`./gradlew detekt` does not exist in
   this repo despite older docs — don't plan around it.)
2. `./gradlew testDebugUnitTest` — new tests: `TaskProposalResponse.toModel()` merges both session
   lists with the right `isAiSuggested` flags; empty `tasks` maps to an empty proposal list;
   `AiTaskRepositoryImplTest` rewritten for the proposal path (first proposal wins, empty ⇒
   `Success(null)`, earliest session becomes `FirstTask`); `AddTaskViewModelTest` rewritten around
   `aiEnabled` + `AiRequested` (the AI-stage tests go away); `AiTasksViewModelTest` for load/empty/
   error/accept and per-task edits surviving into the bulk request.
3. `./gradlew assembleDebug lint` — `MissingTranslation` + `HardcodedText` are errors in `:app`.
4. **Against the dev backend, HTTP logger at `Level.BODY`:**
   - Type a multi-task note ("Build login page with validation, also set up DB schema. Gym Mon and Wed
     6-8pm") → confirm `POST /v1/ai/task-create` body is `{"text": …}` and ≥2 cards render.
   - Photograph a handwritten list → confirm the multipart part names are exactly `image` and `note`,
     that the uploaded body is under 10 MB after downscaling, and that `sourceSummary` renders.
   - Accept 2 of 3 → confirm exactly **one** `POST /v1/tasks/with-sessions/bulk` fires with 2 entries,
     and that an edited duration/time/category is present in the body.
   - Delete every session on a task, accept → task lands in the Inbox unscheduled.
   - Airplane mode mid-call → error state + retry recovers.
   - Run onboarding's First Task step and confirm the card shows a real scheduled time (no
     `/v1/schedule/task` call in the log).
5. Emulator/UX pass: dark theme, Arabic (RTL session rows and the sparkle badge), animations-off
   (`reducedMotion()` freezes the aura and stops the loading-text cycle), back-with-unaccepted
   confirmation, and that the bottom nav bar is absent on the proposals screen.

## Housekeeping

- Branch is already `feature/AWAN-101-ai-task-proposals`; **AWAN-101** goes in every commit message.
- Per `CLAUDE.md`, copy this plan to `docs/feature/ai-tasks/2026-08-01-image-and-multi-task-proposals.md`
  before work starts, and append `## Implementation notes (what actually differed)` when it lands.
- Delete the stale `feature/ai-tasks/{api,impl}/build/` output from the removed module before creating
  sources there.

## Implementation notes (what actually differed)

**Status.** `./gradlew testDebugUnitTest assembleDebug :app:lintDebug` all green (0 lint errors, the
pre-existing 27 warnings/6 hints are unrelated to this change). Test counts: `core:data` 22 (7+9+6),
`core:domain` unchanged plumbing tests untouched, `feature:add-task` 45, `feature:onboarding:impl` 17,
`feature:ai-tasks:impl` 13 — all new or rewritten for this change. Sequencing followed the plan exactly:
contract layer → domain/data → `:feature:ai-tasks` on the text path → sheet stripped to capture → image
path.

### Deviations from the plan as written

1. **`ImageSource` became an interface (`ImageSource`) + Hilt-bound impl (`AndroidImageSource`) instead
   of one concrete class.** The plan's single `@Singleton class ImageSource` can't be faked in a plain
   JVM unit test — `BitmapFactory`/`ContentResolver` need Android runtime. Splitting it (plus a small
   `@Binds` `ImageSourceModule`) let `AiTasksViewModelTest` cover the image-load path (including a
   decode failure) without Robolectric. `AndroidImageSource`'s actual downscale logic is exactly as
   planned (coarse `inSampleSize` decode, then `Bitmap.createScaledBitmap` to a 2048px max edge, JPEG
   quality 85).
2. **`ProposeTasksFromTextUseCase`/`ProposeTasksFromImageUseCase` don't re-validate the 4000-char /
   10 MB / mime-type limits client-side.** The plan asked for pre-flight rejection; built instead as
   thin delegates with the limits kept as public companion constants (`MAX_TEXT_LENGTH`,
   `MAX_IMAGE_BYTES`) for a future character-counter or size check to reference. Duplicating a numeric
   limit the backend already enforces (and returns `VALIDATION_ERROR`/`INVALID_OPERATION` for) is a
   second copy of the same rule that can silently drift from the real one; the backend's error is
   surfaced through `AiTasksViewModel`'s `toProposalErrorRes()` either way.
3. **The onboarding fix removes the `/v1/schedule/task` call from `AiTaskRepositoryImpl` entirely**,
   as the plan intended, but this leaves `TaskRepository.scheduleTask` / `ScheduleTaskRequest` /
   `TaskScheduleResponse` with **zero callers anywhere in the app** — `AddTaskViewModel` never called
   it either (the old `ScheduleTaskWithAiUseCase` was its only caller and is deleted). The plan chose
   to keep the endpoint mapped rather than delete it ("the endpoint is live... deleting it is churn"),
   reasoning it might back a future "schedule this Inbox task" action; that reasoning still holds, but
   worth flagging explicitly since dead API surface is easy to forget about later. Nothing was deleted
   beyond what the plan named.
4. **`add_task_add_photo` became a small caption above the two picker buttons** ("Choose photo" /
   "Take photo") rather than sitting unused — the plan listed it as a key without saying where it goes.
5. **The photo attachment row only renders while `aiEnabled` is on**, appearing directly under the note
   field. This wasn't fully specified in the plan's sheet section but follows from its own framing: a
   photo is context for Awan, so it has no reason to exist in the manual (non-AI) form.
6. **Session editing is a two-step date-then-time flow** (`PickerStep.DATE` → `PickerStep.TIME` inside
   `AiTasksState.sessionPicker`), mirroring the add-task sheet's own picker pattern rather than
   inventing a different one. Picking a new time for a session sets `isAiSuggested = false` on it —
   once the user has chosen the time themselves, the sparkle badge no longer applies.

### Traps worth carrying forward

- **`TaskRemoteDataSource`'s `proposeTasksFromText` takes the whole `AiTextToTasksRequest` DTO, but
  `proposeTasksFromImage` takes raw `(ByteArray, mimeType, note)`** — deliberately asymmetric, so the
  `MultipartBody.Part` construction (which needs `okhttp3` types) stays inside the data-source impl and
  never leaks into `TaskRepositoryImpl` or the domain layer. Getting this backwards was the single most
  common self-inflicted compile error while building this (test fakes had to match the exact split).
- **`ProposedTaskDto.toModel()` merges `draft.sessions` and `aiProposedSessions` into one list**,
  tagging each with `isAiSuggested`. There is no code path that keeps them separate past the mapper —
  if a future change needs to tell "the source explicitly said this" apart from "Awan guessed this"
  for anything beyond the badge (e.g. different edit permissions), that distinction has to be
  reconstructed from `isAiSuggested`, not re-fetched from two lists.
- **`AiTaskRepositoryImpl.createAndScheduleTask` takes the *first* proposal and discards the rest.**
  Onboarding's first-task flow only ever wanted one task from one title; if the AI ever returns
  multiple proposals for a single-sentence title (unlikely but not contractually forbidden), everything
  after the first is silently dropped. Acceptable for onboarding's scope, worth revisiting if onboarding
  ever asks for more than one task.
- **`AiTasksViewModel.hasLoaded` guards `Load` against a second call, but `Retry` bypasses it with
  `force = true`.** Any new caller of `Load` from the UI must go through the Root composable's single
  `LaunchedEffect(Unit)` — calling it a second time from anywhere else silently does nothing.

### Still to verify

Everything in the plan's "Verification" section under **"Against the dev backend"** and **"Emulator/UX
pass"** is outstanding — no `adb` device or emulator was available in this environment (`adb devices`
returned empty), so none of the following happened:

- Real network round-trips for `proposeTasksFromText`/`proposeTasksFromImage`/`createTasksWithSessions`
  against the dev backend, including checking the *exact* multipart field names the backend expects
  (`image` non-nullable, `note` nullable, per the Postman collection).
- The photo pickers (`PickVisualMedia`, camera + `FileProvider`) have not been exercised on-device —
  the `FileProvider` manifest entry merges cleanly (`app:processDebugMainManifest` succeeds) but a
  clean manifest merge doesn't prove the authority string or cache-path scoping actually work at
  runtime.
- Onboarding's First Task step showing a real scheduled time end-to-end.
- Dark theme, Arabic/RTL layout (especially the session rows and sparkle badge, and the two-step date
  picker dialogs), and `reducedMotion()` freezing the loading-text cycle and the aura.
- The bottom nav bar actually being absent on the proposals screen at runtime (verified only by reading
  `AwanApp.kt`'s `isTopLevel` gate, which the new route is deliberately not part of).

All of the above are UI/runtime concerns that unit tests structurally cannot cover; they are the
highest-risk unverified surface of this change and should be the first thing checked on a real device
before this ships.

## Review pass (Standards + Spec) — findings and fixes

A standards/spec review against this plan and `CLAUDE.md` found 6 hard architecture violations, 3
duplication clusters, one real crash, and 9 spec deviations. All were fixed; `./gradlew
testDebugUnitTest assembleDebug :app:lintDebug` green afterward (0 lint errors; `feature:ai-tasks:impl`
tests 13 → 17).

**Architecture (the worst finding):** `AiTasksViewModel` injected `ImageSource` — a DataSource — directly
into presentation code, and that DataSource's contract, impl, and Hilt `@Binds` module all lived inside
the feature module. Both are `CLAUDE.md` non-negotiables. Fixed by splitting the concern across the
proper layers: `ImageBytes` moved to `:core:model`; `ImageRepository` (contract) + `ReadImageUseCase` to
`:core:domain/image/`; `ImageRepositoryImpl` (the actual `BitmapFactory`/`ContentResolver` work) to
`:core:data/image/`, bound in the existing `DataModule`. `AiTasksViewModel` now injects
`ReadImageUseCase` like every other use case. The primitive-obsession `Pair<Int, Int>` for decoded
bounds became a named `ImageDimensions` class in the same move.

**Gradle:** `feature/ai-tasks/impl/build.gradle.kts` applied `awan.android.hilt` and declared
`:core:common` explicitly — both already supplied by the `awan.android.feature` convention plugin
(confirmed against `feature/marketplace/impl`, the clean sibling; `feature/calendar/impl`, which this
was copied from, carries the same redundancy and should get the same fix separately). Removed both.

**Speculative scaffolding, removed:** `onTasksCreated: (Int) -> Unit` was threaded through
`aiTasksEntry` → `AiTasksRouteScreen` → an event branch, and its one caller passed an empty lambda with
a comment claiming a confirmation UI that doesn't exist. Deleted the parameter; both `TasksCreated` and
`Dismissed` now just call `onBack()`. `MAX_TEXT_LENGTH`/`MAX_IMAGE_BYTES` companions on the two propose
use cases had zero readers anywhere — deleted rather than kept "for a future character counter."

**Crash, fixed:** `ErrorBody(messageRes = state.errorMessage!!)` lived inside `Crossfade`. `Crossfade`
keeps the outgoing composition alive to animate the fade; `Retry` nullifies `errorMessage` before that
composition has finished exiting, and the live `state` read behind the `!!` throws. Fixed by making the
phase a sealed type (`ScreenPhase.Error(val messageRes: Int)`) so the value is snapshotted onto the
phase itself at the moment it was computed, immune to what `state` does afterward.

**Spec deviations, fixed:**
- `fetchFromImage(imageUri, note ?: text)` and the equivalent text-path call silently dropped whichever
  of the sentence field or the note field wasn't the one being sent — a photo + typed sentence + note
  all present lost the sentence entirely, and pure-text submissions never sent the note at all (neither
  AI endpoint has room for two separate free-text fields). Fixed with a `combineContext(text, note)`
  fold that joins both into the one field each endpoint accepts, so nothing typed disappears.
- The multipart image filename was `"image"` with no extension; `ImageRepositoryImpl` always re-encodes
  to JPEG, so it's now `"image.jpg"`.
- `CreateTasksUseCase` didn't actually cap at 50 despite its own KDoc claiming it did — added
  `drafts.take(50)`.
- `CreateTaskRequest.title` had no default, so a single malformed proposal (missing `title`) would have
  thrown decoding the *entire* `TaskProposalResponse`. Defaulted to `""` so it degrades to one
  blank-titled card instead.
- `SessionAdded` and `ToggleExpanded`/`isExpanded` were named in this plan but never implemented —
  built now. Removing a task's last session used to strand it with no way back to having a time;
  `SessionAdded` appends a fresh (non-AI-suggested) session and opens the picker on it immediately.
  Every proposal card was permanently in full-edit mode with no summary view; cards now default
  collapsed (static title + read-only duration/category/mandatory badges) and expand on tap of the
  card itself (the selection dot is a separate, dedicated tap target so the two gestures never
  collide).

**Duplication, addressed where it was cheap and left where fixing it meant new scope:**
- `ProposedSession.toSessionDraft()` existed twice (`AiTaskRepositoryImpl`, `AiTasksViewModel`) — moved
  to `:core:model` as a shared extension, both call sites updated.
- `UriThumbnail` existed twice (`AiTasksScreen.kt`, `ImageAttachment.kt`) — promoted to
  `AwanUriImage` in `:core:design-system`, since both feature modules already depend on it.
- `durationLabel`/`MINUTES_PER_HOUR` were declared three times *within* `:feature:ai-tasks:impl` alone
  — consolidated into one `ui/components/Formatters.kt`. The cross-module duplication of the same
  handful of lines against `:feature:add-task`'s own `TaskDraftFormat.kt`/`TaskAttributeChips.kt`
  copies was **not** touched: `CLAUDE.md` explicitly lists `:core:ui` as "still to build," and standing
  that module up now to host ~15 lines of formatting logic is scope the ticket didn't ask for. Left as
  acknowledged debt for whoever builds `:core:ui`.
- The explanatory KDoc on `AiTaskRepositoryImpl.createAndScheduleTask` (narrating "the proposal endpoint
  replaced the old persist-and-schedule pair…") was trimmed to state only the current constraint.

**Localization:** `ProposalCard.kt` composed `"$date, $startTime – $endTime"` and `"+${points}"` in
Kotlin instead of `strings.xml` — added `ai_tasks_session_range` and `ai_tasks_points_badge` with the
punctuation baked into the resource, in both locales. `getQuantityString(...)` on a `LocalContext` read
was replaced with `pluralStringResource(...)`, matching the established idiom in
`feature/onboarding/impl/.../TimeFormat.kt`. The Arabic strings file also spelled Awan "عوان" in five
places where every other locale file in the repo (including this module's own English strings and
`feature/add-task`'s Arabic file) uses "أوان" — corrected.

## Deep-review pass (4 lenses + judge) — findings and fixes

A second, independent review (logic / architecture / reuse / clean-code lenses, merged by a fresh
judge) produced 15 raw findings → 9 reported, all fixed here plus one issue raised by the user.
`./gradlew testDebugUnitTest assembleDebug :app:lintDebug` green afterward; `feature:ai-tasks:impl`
tests 17 → 23, `TaskRepositoryImplTest` 9 → 10.

**The worst one, and it compounded.** `acceptSelected()` wrote its failure into the same
`errorMessage` field the initial fetch uses, and `screenPhase()` cannot tell the two apart — so a
failed bulk create swapped the whole reviewed list for the full-screen `ErrorBody`, whose only
control is Retry → `load(force = true)` → `proposals = emptyList()` and a refetch. Every edit the
user had made was gone, and the sticky `AcceptBar` stayed laid out under the error page offering a
second, contradictory recovery. Now a separate `AiTasksState.acceptError` renders as an inline line
inside `AcceptBar`, cleared by `load()` and by any `updateProposal` edit. The list never leaves the
screen for an accept failure.

It compounded with **blank titles**: `CreateTaskRequest.title` defaults to `""` (so one malformed
proposal degrades to one blank card instead of failing the whole decode), `ProposalUi.isSelected`
defaults to `true`, and a collapsed card renders a blank title identically to a placeholder hint. The
bulk endpoint is **atomic**, so one un-expanded blank card would have rejected every selected task →
straight into the failure above. `acceptSelected()` now refuses on `!draft.isValid` with its own
message. `TaskDraft.isValid` was dead code before this — nothing else calls it, contrary to what one
lens claimed.

**Other fixes:**
- `acceptSelected()` gained `if (isAccepting) return`. The button's `enabled` only disables after
  recomposition, so two taps in one frame each launched their own bulk create — duplicate tasks
  server-side.
- `AwanUriImage` decoded full-resolution bitmaps on the composition thread (no `inSampleSize`, no
  `withContext(IO)`) for 56–72dp thumbnails — ~48 MB for a 12 MP photo, at two call sites at once.
  Now a bounds pass + power-of-two `inSampleSize` to a 512px max edge on `Dispatchers.IO`. It cannot
  reuse `ImageRepositoryImpl`'s identical math: that lives in `:core:data`, which
  `:core:design-system` must not depend on.
- `AiTaskRepositoryImpl` hand-rolled a `CreateTaskWithSessionsRequest` next to the
  `TaskDraft.toRequest(sessions)` mapper this same change added — now calls it.
- `MINUTES_PER_HOUR` existed twice in one module under two names, falsifying the earlier review-pass
  claim that it existed once. The ViewModel copy is gone: `date.atStartOfDay().plusMinutes(...)`
  needs no constant at all.
- `CategoryPicked.categoryId: String?` had no caller passing null. Added the "Unassigned" menu entry
  (reusing `ai_tasks_chip_no_category`) rather than narrowing the type — Awan's category guess is a
  guess, and clearing it has to be reachable.
- `ImageBytes` is no longer a `data class`: the `ByteArray` gave it reference equality while the
  generated `equals` promised value semantics.
- **Test gap the earlier pass left:** every existing test populated only `aiProposedSessions`, so the
  `draft.sessions` → `isAiSuggested = false` half of `ProposedTaskDto.toModel()`'s merge was never
  exercised — a regression dropping `stated` entirely would have kept the suite green. Covered now,
  along with accept-failure, accept reentrancy, and the blank-title stop.

**Client-side limits, now actually implemented** (reversing deviation 2 in the notes above, at the
user's call). `combineContext` concatenates the sentence and the note into one field, which makes the
4000-char ceiling easier to hit than the plan assumed, and the backend answers both "blank" and "too
long" with the same 422 — which rendered as `ai_tasks_error_validation` ("Awan couldn't make sense of
that"), the wrong description for a length error. `ProposeTasksFromTextUseCase` now trims and rejects
blank/over-4000 before the round trip; `ProposeTasksFromImageUseCase` rejects >10 MB and any mime
outside png/jpeg/webp/gif before the upload. Carrying the reason needed a new
`AppError.Validation(ValidationReason)` in `:core:common` — the four reasons map to four distinct
strings in `:feature:ai-tasks`. Two pre-existing exhaustive `when (AppError)` blocks
(`HomeViewModel.toReadableMessage`, `OtpViewModel.toOtpStatusAndMessage`) needed an `else`; neither
screen validates input locally, so both fall through to their generic message.

**Deliberately not added:** a live character counter in `AddTaskSheet`. The guard is what the plan
specified; `ProposeTasksFromTextUseCase.MAX_TEXT_LENGTH` is public for a counter to reference when
someone wants one.

**Not touched, flagged instead of silently accepted:** `settings.gradle.kts`'s duplicate
`:core:domain`/`:core:data` include removal and the `CLAUDE.md` edits were pre-existing uncommitted
changes in the working tree before this session started, not part of this change — left alone.

## Implementation notes — 2026-08-02 polish pass (motion, haptics, ViewModel scoping)

Verified with `./gradlew assembleDebug testDebugUnitTest lint` — all green. Nothing here was checked
on a device; the ViewModel-scoping fix in particular is reasoned from the library bytecode (see
below), not observed running.

**The serious one: every ViewModel in the app was Activity-scoped.** Reported as "reopening a screen
restores the last visit's state" — discard the AI proposals, submit a new image, and the results
screen came back with the old proposals *and* the discard dialog still up, with no request sent.

Root cause is in `:app`, not in this feature. `NavDisplay`'s default `entryDecorators` in
navigation3 1.1.4 is `rememberSaveableStateHolderNavEntryDecorator()` and nothing else — there is no
ViewModel decorator in that default, and `rememberViewModelStoreNavEntryDecorator` lives in a
separate artifact (`androidx.lifecycle:lifecycle-viewmodel-navigation3`, which was already on the
classpath, unused). With no decorator providing one, `LocalViewModelStoreOwner` resolves to
`MainActivity`, so `hiltViewModel()` handed every screen an Activity-scoped instance that outlived
its `NavEntry`. Popping a destination cleared nothing; navigating back to it reused the same object,
which is why `AiTasksViewModel.hasLoaded` short-circuited `load()` and why `showDiscardConfirm` was
still `true`.

Fixed in `AwanApp.rememberDecoratedEntries`: each sub-stack is decorated separately with
`rememberSaveableStateHolderNavEntryDecorator()` + `rememberViewModelStoreNavEntryDecorator()`, and
`NavDisplay` takes the resulting `List<NavEntry<Route>>` instead of a raw key list. Per-stack rather
than one decorator set on the visible stack, because `NavDisplay` only ever sees `currentSubStack` —
decorating just that one would clear a tab's ViewModels every time the user switched tabs. All
stacks are decorated on every recomposition, so background tabs keep their state; only a genuine pop
clears a store. `subStacks` grows at runtime (`Navigator.replaceAll` inserts Login/Onboarding), hence
the `key(topLevelKey)` around each call.

Easy to reintroduce: anyone adding a second `NavDisplay`, or "simplifying" back to the
`backStack = …, entryProvider = …` overload, silently restores Activity-scoped ViewModels. There is
no crash and no warning — it only shows up as stale state on the second visit to a screen.

`AiTasksAction.DiscardConfirmed` also never reset `showDiscardConfirm` before closing. Harmless once
the ViewModel dies with its entry, but it was the second half of "the dialog is still showing".

**Motion.** `AwanCard`'s `animateContentSize` now runs on `AwanTheme.motion.settle` (snap under
`reducedMotion()`) instead of Compose's default spring. The proposals list reuses `CascadeItem`, with
the index capped at 6 — uncapped, a card scrolled into view as item 20 would sit blank for over a
second before rising in, because `CascadeItem` replays on every fresh subcomposition and the index
*is* the delay multiplier. The accept bar slides up on `settle`, the phase `Crossfade` runs at
`emphasizedMillis`, and the selection dot's tick pops on `bouncy`.

**Haptics** on the proposal card (`ContextClick` opening / `SegmentTick` closing) and the selection
dot (`Confirm` / `TextHandleMove`, mirroring `AwanScheduleTaskCard`). Deliberately *not* pushed into
`AwanCard` itself — that would give every card in the app a haptic nobody asked for.

**Found while wiring the haptic:** `SelectionDot` had an `onClick` parameter that was never attached
to anything — the `Box` had no `clickable`. Tapping the dot fell through to the card and toggled
expansion, so a proposal's inclusion could not be changed at all; the accept button always sent
exactly what Awan proposed. Now `clickable(role = Role.Checkbox)`. Its touch target is still 22dp,
below the 48dp minimum — left alone because expanding it reflows the title row.

**Loading state**, per user feedback on a screenshot: the `AwanAiAura` pill is gone. The aura strokes
its content's bounding box, and around a mascot that is nowhere near rectangular it read as a stray
capsule outline. `AwanMascot`'s Idle expression floats on its own. Expressions swapped too — Idle
while loading, Curious on error.

### Follow-up the same day: selection replaced by removal + undo

Per-task selection is gone. `ProposalUi.isSelected` and `AiTasksAction.ToggleSelected` are deleted;
`AcceptSelected` is now `Accept` and sends whatever is still on screen. Selection and removal were
answering the same question twice — "is this task going in?" — and the checkbox lost, because a
deselected card still occupies the list it is no longer part of.

Removal is an X in the card header, deliberately **not** a swipe: this screen is seen once per
request, so a gesture with no affordance would go undiscovered, and a horizontal drag competes with
both the scroll and the chip row inside the card. The X matches the one already on each session row.

Two levels of undo, because they answer different regrets:
- `lastRemoved` holds exactly one removal, surfaced as a named "Removed X — Undo" row above the CTA.
  A second X drops the older entry rather than queueing: the row can only name one task, and undoing
  something the user has stopped thinking about is worse than not offering it.
- `canReset` compares the working list against `originalProposals` and shows a quiet "Back to Awan's
  original plan" under the CTA. It compares with `isExpanded` normalised away — otherwise merely
  opening a card to read it offers a reset, which reads as a bug.

`isEmptyResult` now keys off `originalProposals`, not `proposals`. Removing the last card used to
flip the screen to the "no actionable tasks" body, which took the undo row and the reset link with
it and left re-requesting as the only way back. The accept bar's visibility keys off the same field
for the same reason; the button falls back to `ai_tasks_accept_none` at zero rather than rendering
"Add 0 tasks".

Animation for the removal is `Modifier.animateItem(placementSpec = …)` on the list items, so the gap
closes under the cards below and a restored card fades back into its slot. All of this screen's
specs now go through one `settleSpec()` helper that returns `snap()` under `reducedMotion()`.

The source-summary card is clickable across its whole surface; the show more/less line stays as the
label for what a tap does rather than being the only target.

**Not changed, contrary to what was said in the previous section:** `AddTaskSheet` does not retain a
stale draft. `AddTaskViewModel.close()` already resets state and reloads categories on every exit
path — dismiss, discard, and the AI hand-off all route through it.
