# First Task step: create and schedule with AI, then show the real result

## Context

The First Task step currently shows the user a lie. `OnboardingViewModel.submitFirstTask` does two unrelated things:

1. calls `ScheduleFirstTaskUseCase` — a use case whose own KDoc calls it "a deliberate fake": it drops the task at the start of the first enabled zone with `preferredTaskLengthMinutes` as its duration;
2. calls `CreateTaskUseCase` → `POST v1/tasks`, creating a bare task with no time, no duration estimate, no zone — and **discards the response entirely**.

What lands on screen is the invented placement from (1). The card even shows the *zone's* whole window as the time range, not the task's. Nothing the user sees corresponds to anything on the server.

The backend already has the pieces to do this properly: `POST /v1/ai/task-create` enriches a bare title into a real task (AI-estimated duration, points, category, auto-assigned to Inbox), and `POST /v1/schedule/task` runs the scheduling engine and returns actual sessions with real start/end datetimes.

Outcome: the user types a title, Awan creates the task with AI and schedules it, and the card shows the **real** time the real task actually landed at.

## Contracts (from Postman)

**`POST {baseUrl}/v1/ai/task-create`** → `201 TaskInfoResponse`
```json
{ "title": "Build login page", "description": "optional, max 2000 chars" }
```
Returns the existing `TaskInfoResponse` shape: `id`, `title` (may be refined by AI), `estimatedDuration`, `status` (`SCHEDULED`), `estimatedPoints`, `goalId`, `category`. Errors: `VALIDATION_ERROR` (422), `AI_UNAVAILABLE` (503).

**`POST {baseUrl}/v1/schedule/task`** → `200 TaskScheduleResponse`
```json
{ "taskId": "…", "horizonDays": 14 }
```
```json
{
  "taskId": "…",
  "scheduledSessions": [{ "sessionId": "…", "taskId": "…", "zoneId": "…", "start": "…", "end": "…" }],
  "unscheduledTasks": [{ "taskId": "…", "taskTitle": "…", "reason": "…", "message": "…" }]
}
```
Errors: `TASK_NOT_FOUND` (404), `AI_UNAVAILABLE` (503). A 200 with a populated `unscheduledTasks` is the "no slot found" case — **not** an error status.

## Decisions taken

- **Template zones resolve the session's zone.** `createWeeklyTemplate` (AWAN-90) currently throws away `TemplateResponse`; it will return the created zones instead, so the server zone UUID on a scheduled session maps to a real name and colour. Local zone ids (`"study"`, `"work"`) never match server UUIDs, so without this the card has no zone to show.
- **Failures show an inline message** in place of the card, and the user can still continue. Covers `AI_UNAVAILABLE`, network failure, and the 200-with-`unscheduledTasks` case.
- **Core library desugaring is enabled** so `java.time` works on minSdk 24, and session datetimes are parsed with `OffsetDateTime`/`LocalDateTime` rather than by slicing the string. Substring parsing would silently display server time as local the moment the API returns a `Z` or an offset.

## Files

### Build — desugaring

- **EDIT** `gradle/libs.versions.toml` — add `desugar-jdk-libs` (`com.android.tools:desugar_jdk_libs:2.1.5`) with a version entry.
- **EDIT** `build-logic/convention/src/main/kotlin/com/apps/awan/KotlinAndroid.kt` — in **both** `configureKotlinAndroid` overloads set `compileOptions.isCoreLibraryDesugaringEnabled = true`, and add the `coreLibraryDesugaring` dependency via `dependencies { add("coreLibraryDesugaring", …) }` resolved from the version catalog. Both overloads matter: `:app` uses the `ApplicationExtension` one, every `:core`/`:feature` module the `LibraryExtension` one.

### `:core:network` — DTOs + endpoints

- **NEW** `dto/CreateAiTaskRequest.kt` — `title`, `description`.
- **NEW** `dto/ScheduleTaskRequest.kt` — `taskId`, `horizonDays`.
- **NEW** `dto/TaskScheduleResponse.kt` — `TaskScheduleResponse`, `ScheduledSessionResponse`, `UnscheduledTaskResponse`.
- **EDIT** `api/TaskApiService.kt` — add `createTaskWithAi` (`POST v1/ai/task-create`) and `scheduleTask` (`POST v1/schedule/task`) alongside the existing `createTask`. Both are task-shaped calls on the authed Retrofit; no new service and no new `@Provides`.
- **EDIT** `dto/TemplateResponse.kt` — unchanged shape, already carries `zones[]`.

### `:core:domain` — contract + use case

- **NEW** `task/repository/AiTaskRepository.kt`:
  ```kotlin
  interface AiTaskRepository {
      /** Creates the task with AI and schedules it. Success with a null task = created but no slot found. */
      suspend fun createAndScheduleTask(title: String): Result<FirstTask?>
  }
  ```
  Returns the `:core:model` domain type, so no DTO reaches the domain layer. The `null` carries the "created but unscheduled" case without inventing a second result type.
- **NEW** `task/usecase/CreateAndScheduleFirstTaskUseCase.kt` — thin delegate, same shape as `CreateWeeklyTemplateUseCase`.
- **EDIT** `template/repository/TemplateRepository.kt` — `createWeeklyTemplate` now returns `Result<List<Zone>>`.
- **DELETE** `onboarding/ScheduleFirstTaskUseCase.kt` and its test — the fake it exists to be is exactly what this change removes. It is referenced only by `OnboardingViewModel` and its own test.

### `:core:data` — impls

- **NEW** `task/AiTaskRepositoryImpl.kt` — orchestrates the two calls and maps the result:
  - `createTaskWithAi(title)` → take `id`;
  - `scheduleTask(taskId, HORIZON_DAYS)` → first `scheduledSessions` entry;
  - parse `start`/`end` to `startMinutes` (minutes-from-midnight) and `durationMinutes` via `java.time`, tolerating both offset-bearing and plain local datetimes;
  - no sessions → `Result.Success(null)`.
- **EDIT** `task/remote/TaskRemoteDataSource.kt` + `Impl` — two more `safeApiCall` wrappers. Reused rather than adding a parallel data source.
- **EDIT** `template/TemplateRepositoryImpl.kt` — map `TemplateResponse.zones` back to `Zone`: server id, name, `#RRGGBB` → ARGB int, `HH:mm:ss` → minutes-from-midnight. This is the inverse of the mapper written for AWAN-90.
- **EDIT** `di/DataModule.kt` — one `@Binds` for `AiTaskRepository`.
- **EDIT** `util/TimeFormat.kt` — add the `HH:mm:ss` → minutes parser next to the existing formatter.

`CreateTaskUseCase`/`TaskRepository` stay in `:core:data`. `CLAUDE.md` lists them as known violations to fix when touched, but moving them properly means giving tasks a domain model and mappers — `TaskRepository.createTask` currently returns the `TaskInfoResponse` DTO, so a naive move would put a Retrofit type in a domain signature and trade one violation for a worse one. The new `AiTaskRepository` is built the right way instead; the old pair is left for its own refactor.

### `:feature:onboarding:impl` — state, ViewModel, UI

- **EDIT** `presentation/OnboardingState.kt` — add `templateZones: List<Zone>` (server-side zones from the template call) and `firstTaskError: UiText?`. `dayPreview` takes `templateZones` so the task block resolves its colour.
- **EDIT** `presentation/DayPreviewModel.kt` — `from(bounds, zones, firstTask, taskZones)`; the timeline still draws local zones, only the task block looks up `taskZones`.
- **EDIT** `presentation/OnboardingViewModel.kt`:
  - `submitOnboarding()` stores the zones returned by `createWeeklyTemplate` into `templateZones`;
  - `submitFirstTask()` drops `scheduleFirstTask` and `createTaskUseCase` for `createAndScheduleFirstTask(title)`, then sets either `firstTask` + `celebrateTask`, or `firstTaskError`.
- **EDIT** `ui/steps/FirstTaskStep.kt` — resolve the zone from `state.templateZones`; the card shows the **task's** real time range (`start` → `start + duration`) instead of the zone's whole window; render `firstTaskError` inline when set; degrade gracefully when the zone cannot be resolved.
- **EDIT** `res/values/strings.xml` **and** `res/values-ar/strings.xml` — new keys for the unscheduled and failed cases, plus the task time range. Every key in both files.

### Tests

- **NEW** `core/data/src/test/.../task/AiTaskRepositoryImplTest.kt` — session datetime → `startMinutes`/`durationMinutes`; empty `scheduledSessions` → `Success(null)`; a failed create short-circuits before scheduling.
- **EDIT** `core/data/src/test/.../template/TemplateRepositoryImplTest.kt` — assert the returned zones map back from the response.
- **EDIT** `feature/onboarding/impl/src/test/.../OnboardingViewModelTest.kt` — replace the `CreateTaskUseCase` fake; assert the real scheduled task reaches state, and that a failure sets `firstTaskError` and leaves `firstTask` null.
- **DELETE** `core/domain/src/test/.../ScheduleFirstTaskUseCaseTest.kt`.

## Verification

1. `./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:onboarding:impl:testDebugUnitTest`.
2. `./gradlew assembleDebug lint` — desugaring touches every module, so a full assemble is the real check. (`./gradlew detekt` does not exist in this project despite `CLAUDE.md`; see the AWAN-90 plan.)
3. On device with the debug HTTP logger at `Level.BODY`, run onboarding to the First Task step and add a task: confirm `POST v1/ai/task-create` then `POST v1/schedule/task` fire, and that the time on the card equals the `start`/`end` in the schedule response — not the zone window.
4. Force the failure path (airplane mode at the moment of submit) and confirm the inline message appears and the step can still be continued.
5. Check the card in Arabic to confirm the new strings are translated and the time range reads correctly RTL.

## Known limits

- **The horizon is fixed at 14 days** (the server default). Nothing in onboarding asks the user how far out to look.
- **Only the first session is shown.** A long task the engine splits into several sessions still renders as one block — the card is a single-slot preview by design.
- **The AI task is not retried** if scheduling fails: the task exists on the server unscheduled, and the user is told so, but there is no in-flow "try again".

## Implementation notes (what actually differed)

Built as planned. Differences and traps:

- **`TaskRemoteDataSource` gained two members, which broke two unrelated test files.** `TaskRepositoryImplTest` and `TaskRemoteDataSourceTest` both build anonymous-object fakes of the interfaces, so every new interface member is a compile error in them. Both got `error("not used")` stubs. Any future endpoint added to these interfaces will do the same — the anonymous-fake convention has no default-method escape hatch.
- **`parseIsoDateTime` converts through the device zone on purpose.** `OffsetDateTime.parse(…).atZoneSameInstant(ZoneId.systemDefault())` first, falling back to `LocalDateTime.parse` for a plain local datetime. Reading `HH:mm` off the string would render server time as the user's own — the failure would be invisible in any test where both happen to be UTC, and wrong for every user not on the server's offset. Do not "simplify" this to a substring.
- **Duration comes from `Duration.between(start, end)`, not from arithmetic on minutes-of-day**, so a session crossing midnight (`23:30` → `00:15`) reports 45 minutes rather than a negative number. There is a test for exactly this.
- **`FirstTask.zoneId` changed meaning** from a local zone id (`"study"`) to a server zone UUID. Anything that looks a task's zone up in `state.zones` will now silently miss — `DayPreviewModel.from` and `FirstTaskStep` both had to switch to `state.templateZones`. `DayPreviewModel.from` takes `taskZones` as a defaulted fourth parameter, so a call site that forgets it compiles and just renders a colourless block.
- **The card now shows the task's own time range**, `formatClock(startMinutes)` → `formatClock(startMinutes + durationMinutes)`. It previously showed the whole zone window, which is why an invented 60-minute task appeared to span four hours.
- **`ScheduleFirstTaskUseCase` and its test are deleted.** It was the fake; nothing else referenced it.
- The zone lookup degrades rather than disappearing: `LandedTaskCard` takes a nullable `Zone` and drops the name/colour chrome when the scheduler used a zone this device never saw (template call failed, or the AI picked a zone outside the created template). This is why `onboarding_first_task_duration_only` exists alongside `onboarding_first_task_duration_zone`.
- Verified: all three module test suites green (4 new `AiTaskRepositoryImplTest` cases, 1 new template-zone mapping case, 4 rewritten/new ViewModel cases); `assembleDebug` and `lint` green across every module, which is the real check for the desugaring change. **Not exercised against the real backend** — Verification steps 3–5 remain outstanding, and the exact ISO shape the API emits for `start`/`end` is still unconfirmed, which is why the parser accepts both offset-bearing and plain values.
