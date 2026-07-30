# AI task creation becomes a real preview

## Context

`2026-07-25-ai-task-creation.md` shipped the AI flow against the only contract that existed then:
`POST /v1/ai/task-create` always persisted. That forced an awkward shape — Awan's task is saved the
moment you ask, so "let me pick a time instead" has to delete it and create a plain one, and backing
out of the sheet has to delete it too. Every exit path had to remember to clean up a row that should
never have existed yet.

The backend now supports `persist` (verified via Postman MCP, collection `Awan`, request
`Create Task via AI`, updated today):

| Call | Shape |
|---|---|
| `POST /v1/ai/task-create?persist=false` | `{title, description?}` → `201` proposed `TaskWithSessionsRequest` — **not saved** |
| `POST /v1/ai/task-create?persist=true` (default) | same body → `201 TaskWithSessionsResponse`, **persisted** to Inbox |

This lets the sheet do what it always conceptually wanted: ask Awan for a preview, let the user edit
the result through the existing chips (duration/category/mandatory — unchanged machinery from the
2026-07-25 plan), and only hit the network with a real create once they confirm. Nothing is ever
provisionally saved and then thrown away.

Target flow:

1. **Ask Awan** → `persist=false`. Response fills the chips for review, same as today. No task exists
   on the backend yet, so there is nothing to clean up if the user closes the sheet here.
2. **"Let Awan schedule it"** → the sheet now creates the (possibly user-edited) task itself via the
   ordinary `CreateTaskUseCase` (`POST /v1/tasks`), exactly like the manual path already does, then
   calls `POST /v1/schedule/task` with the id that came back.
3. **"I'll pick a time"** → unchanged: reveals the when-chip, and confirming calls `CreateTaskUseCase`
   with (or without) a `startAt` — the existing "no time chosen is still a valid submit" behavior from
   the 2026-07-25 plan stays exactly as it is.

The one case that still needs a delete: the user picks "let Awan schedule it", the engine finds no
slot (a `200` with `unscheduledTasks` non-empty — still not an error), and *then* switches to picking
a time by hand. At that point a task really was persisted by step 2, so the manual confirm has to
delete it first and create the scheduled one — the same one-endpoint-does-both-jobs constraint noted
in the 2026-07-25 plan (no "add a session to an existing task" endpoint exists). This is now the rare
branch instead of the common one.

## Design (revised after advisor review — see below)

**`AiTaskRepositoryImpl` (onboarding's first-task flow) already calls the same
`TaskRemoteDataSource.createTaskWithAi` for a completely different reason**: it wants the persisted
task immediately, to schedule it in the same breath. A single method defaulting its query param to
`persist=false` would silently break onboarding. So this is two Retrofit methods, not one method with
a flag:

- `TaskApiService.createTaskWithAi` — **unchanged**. Still `POST v1/ai/task-create` with no query
  param (backend defaults `persist=true`), still returns `TaskWithSessionsDto` with a real id. Still
  used only by `AiTaskRepositoryImpl`.
- `TaskApiService.previewTaskWithAi` — **new**. `POST v1/ai/task-create?persist=false` (literal in the
  `@POST` path — never anything else, so no `@Query` parameter to misuse). Returns a new
  `AiTaskPreviewResponse`, not `TaskWithSessionsDto`.

**The preview response gets its own tolerant DTO rather than reusing `TaskInfoResponse`.** The Postman
doc names the `persist=false` body `TaskWithSessionsRequest` — the *request*-shaped type, not the
response one. If the backend genuines echoes request-shaped fields (`categoryId: String?` instead of
a nested `category` object; sessions with no `id`/`status`/`locked`), decoding it as `TaskWithSessionsDto`
either silently drops the AI's category or throws on required fields the preview doesn't have. Fix:
`AiTaskPreviewResponse`/`AiTaskPreviewTaskResponse` (`core/network/.../dto/`) — every field nullable,
no `sessions` property declared at all (proposed timing from a preview is never used; the schedule
call is always a fresh explicit request, and `Json.ignoreUnknownKeys` drops whatever the body sends
for a key we don't declare). `categoryId` and a nested `category: CategoryDto?` are both declared so
either shape decodes.

**Domain gets an honest new model instead of a fake `Task`.** A preview was never persisted — it has
no id, no status. Bolting a placeholder id onto `Task` would let a blank id wander into
`scheduleTask`/`deleteTask` by accident. Instead: `core/model/AiTaskSuggestion` (title, description,
duration, mandatory, points, splittable, `categoryId`, `categoryName`). `TaskRepository.createTaskWithAi`
is renamed `previewTaskWithAi` and returns `Result<AiTaskSuggestion>`; `CreateTaskWithAiUseCase`
becomes `PreviewTaskWithAiUseCase`. The rename is deliberate, not churn — the old KDoc ("already
persisted... whoever calls this owns cleaning it up") is exactly the trap the 2026-07-25 plan warned
about, and leaving the old name on a method that no longer persists would resurrect that trap for the
next person who reads it.

**No `aiTaskId`/`createdTaskId` field at all.** `AddTaskState` drops the field entirely, because
`scheduleWithAi()` is now fully self-contained: create (via the ordinary `CreateTaskUseCase`, forcing
`startAt = null` — the composed sentence may still contain a time phrase the user typed before
switching Awan on, and "let Awan schedule it" has to mean the engine picks the slot, not whatever the
parser found) → schedule → **on anything other than a placed session, delete what was just created**
before surfacing the error. That makes every exit from `REVIEW`/`MANUAL` symmetric: either the sheet
is showing a receipt for a task that really is scheduled, or nothing was left behind. The manual
"I'll pick a time" submit becomes identical to the plain `createDirectly()` path (no delete-then-create
needed — nothing to delete, since nothing survives a failed schedule attempt), and `discard()` no
longer deletes anything either. The cost: two failed schedule attempts in a row cost two create+delete
round trips instead of reusing one row. Given the button is not expected to fail repeatedly, that
trade reads as the right one — simpler and correct beats reusing a row on the rare retry.

## Files touched

- `core/model/AiTaskSuggestion.kt` — new.
- `core/network/.../dto/AiTaskPreviewResponse.kt` — new (`AiTaskPreviewResponse`,
  `AiTaskPreviewTaskResponse`).
- `core/network/.../api/TaskApiService.kt` — new `previewTaskWithAi` method; `createTaskWithAi`
  untouched.
- `core/data/.../task/remote/TaskRemoteDataSource.kt` + `Impl.kt` — new `previewTaskWithAi`.
- `core/data/.../task/TaskMappers.kt` — `AiTaskPreviewResponse.toModel(): AiTaskSuggestion`.
- `core/data/.../task/TaskRepositoryImpl.kt` — `createTaskWithAi` → `previewTaskWithAi`.
- `core/domain/.../task/repository/TaskRepository.kt` — same rename + KDoc.
- `core/domain/.../task/usecase/CreateTaskWithAiUseCase.kt` → `PreviewTaskWithAiUseCase.kt`.
- `feature/add-task/.../presentation/AddTaskState.kt` — drop `aiTaskId`.
- `feature/add-task/.../presentation/AddTaskViewModel.kt` — `intoReview()` takes `AiTaskSuggestion`;
  `scheduleWithAi()` rewritten (create → schedule → cleanup-on-non-success); `submit()`'s `MANUAL`
  branch merges into `createDirectly()`; `replaceAiTaskWithScheduledOne()` removed; `discard()`
  simplified.
- Tests: `AddTaskViewModelTest.kt` (rewritten around the new call ordering),
  `TaskRepositoryImplTest.kt` (renamed test, new fake method), `TaskRemoteDataSourceTest.kt` and
  `AiTaskRepositoryImplTest.kt` (new interface method stub), `OnboardingViewModelTest.kt` (mechanical
  rename of its `TaskRepository` fake's stub method).

## Implementation notes (what actually differed)

Built as designed above after an advisor pass caught three problems with the first draft of this
plan before any code was written (kept here since they're the actual traps, not the design that
shipped):

1. `AiTaskRepositoryImpl` (onboarding's first-task flow) calls the same `TaskRemoteDataSource`
   method for the opposite reason — it wants the persisted task immediately. Defaulting
   `createTaskWithAi`'s query param to `persist=false` would have silently broken onboarding. Fixed
   by adding a second Retrofit method (`previewTaskWithAi`, literal `?persist=false` in the `@POST`
   path) instead of parameterizing the existing one.
2. The `persist=false` response is documented as `TaskWithSessionsRequest`, not
   `TaskWithSessionsResponse` — request-shaped, not response-shaped. Reusing `TaskInfoResponse`
   (`id` required, `SessionDto.id` required) risked either dropping the AI's category (flat
   `categoryId` vs. nested `category` object) or throwing on decode. Fixed with a dedicated tolerant
   DTO (`AiTaskPreviewResponse`/`AiTaskPreviewTaskResponse`, every field nullable, no `sessions`
   property declared at all) and a domain model built for what a preview actually is
   (`AiTaskSuggestion`, no id) rather than a `Task` with a placeholder id.
3. `scheduleWithAi()` must force `startAt = null` on the create it does — the composed sentence can
   still carry a time phrase typed before Awan was switched on, and `CreateTaskUseCase` branches to
   `createTaskWithSessions` whenever `startAt` is non-null. Missing this would have silently turned
   "let Awan schedule it" into "place it wherever the parser found" for that input shape.

One design call made explicit during the plan revision: `scheduleWithAi()` never memoizes the task
it creates. Every non-success outcome (create fails, schedule finds no slot, schedule errors) deletes
what was just created and returns to `REVIEW` clean. This means retrying "let Awan schedule it" after
a no-slot result creates and deletes again rather than reusing the row — a real cost, but it also
means there is no stale-task-id bug if the user edits a chip and retries, and it collapses
`AddTaskState` back to having no id field for an unpersisted flow to track at all. Verified with a
dedicated test (`switching to manual after a failed schedule attempt still creates fresh`).

Verification: `./gradlew testDebugUnitTest` (whole project, all green) and `./gradlew assembleDebug`.
`detekt` was not run — the plugin isn't wired up in this repo despite CLAUDE.md documenting the
command. No UI changes were needed (`AddTaskSheet.kt` already dispatches
`ScheduleWithAi`/`ScheduleManually`/`Submit`; none of those wire through `aiTaskId`), so this was not
manually re-verified in a running app — the behavior change is entirely inside the ViewModel/data
layer and is covered by the unit test suite.
