# AWAN-141 — Session-status migration (gamification Phase 2)

**Date**: 2026-08-08
**Jira**: [AWAN-141 — Migrate session status to the dedicated complete/uncomplete/cancel endpoints](https://ezdo.atlassian.net/browse/AWAN-141)
**Feature**: Gamification (`:core:network`, `:core:domain`, `:core:data`, `:feature:home:impl`)
**Contract**: [gamification-framework.md](gamification-framework.md) §2 and §3
**Precedes**: [2026-08-08-points-streak-and-wheel.md](2026-08-08-points-streak-and-wheel.md) (Phase 1)

## Context

Phase 1 ([AWAN-140](https://ezdo.atlassian.net/browse/AWAN-140), commits `874834e` + `6b27daf`) built the
whole reward pipeline — `GamificationEventBus`, the reward DTOs, the points flight, the streak loot
overlay — but wired only the daily wheel into it. **Session completion still cannot award anything**,
so the streak overlay has no live trigger and points only move when the wheel is spun.

The cause is the endpoint: the app mutates session status through the generic
`PUT v1/sessions/{id}` with a `status` in the body
([SessionApiService.kt:11](core/network/src/main/kotlin/com/awan/app/core/network/api/SessionApiService.kt:11)),
which the backend has retired. Only `POST .../complete` returns a `reward` payload.

**Outcome:** completing a session awards points and moves the streak, and both celebrations fire.

Confirmed reachable now that the base URL is fixed: `POST /api/v1/sessions/{id}/complete` returns
**401, not 404**, so the dedicated routes exist. The error envelope carries `errorCode`, which the
existing `AppError.Api` mapping relies on.

### Decisions already taken

- **Stays on `feature/AWAN-140-gamification-points-streak-wheel`** — one PR will carry both phases.
  The commit message must therefore start with **`AWAN-141:`**, since the branch name only carries
  AWAN-140 and CLAUDE.md needs the issue ID in one or the other.
- **Verification is build + tests + lint only.** No live run. See the honesty note at the bottom.

---

## Work

### 1. `:core:network`

**`api/SessionApiService.kt`** — add the three dedicated endpoints, keep `@PUT` for drag-to-move:

```kotlin
@POST("v1/sessions/{sessionId}/complete")
suspend fun completeSession(@Path("sessionId") sessionId: String): CompleteSessionResponse

@POST("v1/sessions/{sessionId}/uncomplete")
suspend fun uncompleteSession(@Path("sessionId") sessionId: String): SessionDto

@POST("v1/sessions/{sessionId}/cancel")
suspend fun cancelSession(@Path("sessionId") sessionId: String): SessionDto
```

**New `dto/session/CompleteSessionResponse.kt`** — `session: SessionDto`, `reward: RewardDto?`.
`RewardDto`/`PointsRewardDto`/`StreakRewardDto` already exist from Phase 1
([dto/gamification/RewardDto.kt](core/network/src/main/kotlin/com/awan/app/core/network/dto/gamification/RewardDto.kt));
**do not redefine them.**

**`SessionDto`** — add `firstCompletedAt: String? = null`.

**Remove `status`** from `UpdateSessionRequest` and from `SessionDraftDto`
([CreateTaskWithSessionsRequest.kt:18](core/network/src/main/kotlin/com/awan/app/core/network/dto/task/CreateTaskWithSessionsRequest.kt:18)).
Verified safe: `SessionDraft.toDto()`
([TaskMappers.kt:52](core/data/src/main/kotlin/com/awan/app/core/data/task/TaskMappers.kt:52)) never sets
it, and no other production code constructs a `SessionDraftDto`. Two test files pass it positionally
by name only — they compile unchanged.

### 2. `:core:domain`

**`home/repository/HomeRepository`** — replace `updateSessionStatus(...)`:

```kotlin
suspend fun completeSession(sessionId: String): Result<SessionReward>
suspend fun uncompleteSession(sessionId: String): Result<Unit>
suspend fun cancelSession(sessionId: String): Result<Unit>
suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<Unit>
```

`SessionReward` already exists
([gamification/model/SessionReward.kt](core/domain/src/main/kotlin/com/awan/app/core/domain/gamification/model/SessionReward.kt)).

**`home/usecase/`** — five new use cases, each one `operator fun invoke` delegating to the repository,
shaped like [GetDayScheduleUseCase](core/domain/src/main/kotlin/com/awan/app/core/domain/home/usecase/GetDayScheduleUseCase.kt):
`CompleteSessionUseCase`, `UncompleteSessionUseCase`, `CancelSessionUseCase`, `MoveSessionUseCase`,
**`GetUserProfileUseCase`**.

`GetUserProfileUseCase` is not optional padding: `HomeViewModel` calls `homeRepository` in **three**
places, not two — [line 81](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:81)
is `getUserProfile()`. Without it the repository injection survives and the CLAUDE.md violation is
only cosmetically fixed.

`cancelSession` gets full plumbing but **no UI** — nothing in the app cancels a session yet. It is the
documented replacement for a status the domain enum already carries.

### 3. `:core:data`

**`home/remote/HomeRemoteDataSource(.Impl)`** — replace `updateSession(sessionId, status, startIso, endIso)`
with `completeSession`, `uncompleteSession`, `cancelSession`, and `moveSession(sessionId, startIso, endIso)`.
Each is a one-line `safeApiCall(dispatcher = ioDispatcher, json = json) { … }`, matching the file's
existing style.

**`home/repository/HomeRepositoryImpl`**
([line 133](core/data/src/main/kotlin/com/awan/app/core/data/home/repository/HomeRepositoryImpl.kt:133)):

- Inject `GamificationEventBus`.
- `completeSession` → map with the existing `RewardDto.toDomain()`
  ([gamification/mapper/GamificationMapper.kt](core/data/src/main/kotlin/com/awan/app/core/data/gamification/mapper/GamificationMapper.kt)),
  publish via `eventBus.publishSessionReward(reward)`, patch the cache to `COMPLETED`, return the reward.
  The mapper already collapses `awarded:false`/`updated:false` to null, and `publishSessionReward`
  already no-ops on a null half — so a re-completion emits nothing without any extra guard here.
- `uncompleteSession` → patch cache to `SCHEDULED`. **Publishes nothing** (doc §3: no points are taken back).
- `cancelSession` → patch cache to `CANCELLED`.
- `moveSession` → times only, no status.
- Extract the existing `scheduleCache` patch loop into one private
  `patchCachedSession(sessionId, transform)` rather than repeating it four times.

### 4. `:feature:home:impl`

**`HomeViewModel`** — drop the `HomeRepository` injection entirely; inject the five use cases.

- **`toggleSessionStatus`** ([line 218](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:218)):
  keep the optimistic flip, call `CompleteSessionUseCase`/`UncompleteSessionUseCase`, and **roll the
  flip back on `Result.Error`** — today the result is discarded, so a failed call leaves the UI lying.
  Capture the pre-toggle status and restore it, recomputing the derived counts.
  - On **complete**: drop the Phase 1 `refreshGamificationProgressUseCase()` call. The reward now
    arrives through the bus and updates progress itself; refreshing as well would race the
    star-by-star count-up and snap the badge to the final total.
  - On **uncomplete**: **keep** a `refreshGamificationProgressUseCase()` call. The contract says no
    points are taken back but is silent on the streak, so re-reading is the only way the badges stay
    truthful if the server did adjust it.
- **`moveSession`** ([line 285](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:285))
  → `MoveSessionUseCase`. This is the one path that must **not** move to the dedicated endpoints.
- `loadUserProfile` → `GetUserProfileUseCase`.

### 5. Tests — `:core:data`

New `src/test/java/…/home/HomeRepositoryImplTest.kt`, JUnit 4 + hand-written fakes, matching
[TaskRepositoryImplTest](core/data/src/test/java/com/awan/app/core/data/task/TaskRepositoryImplTest.kt).
No MockK/Turbine in this project — don't add them.

- Completing publishes exactly one `RewardEvent.Points` and one `RewardEvent.Streak`, in that order.
- A re-completion (`awarded:false`/`updated:false`) publishes nothing.
- Uncompleting publishes nothing.
- `maxStreakBroken` survives onto the emitted streak event.
- Completing patches the cached schedule to `COMPLETED`.

`HomeRepositoryImpl` needs a `UserDao`, which is only touched by `getUserProfile`. The fake implements
the interface and `TODO()`s every member the session paths don't call — honest about what is covered.

---

## Deliberately not doing

- **The two `SessionStatus` enums stay.** `domain.home.model.SessionStatus` keeps `IN_PROGRESS` and
  `core.model.SessionStatus` keeps `UNKNOWN`/`MISSED`. `HomeMapper.parseSessionStatus` still reads
  them off the wire; only the *sending* direction disappears. Unifying them means touching the whole
  task-creation flow for no behavioural gain.
- **No cancel UI.**
- **No retry/undo affordance** on a failed completion beyond the rollback — the user can simply tap again.

---

## Verification

1. `./gradlew assembleDebug`
2. `./gradlew testDebugUnitTest` — the 23 existing test classes stay green, plus the new ones.
3. `./gradlew lint` — `abortOnError = true`, `HardcodedText`/`MissingTranslation` as errors.

**What this does not prove.** Per your call, there is no live run, so the reward path is exercised
only against fakes: the real `complete` response shape, the actual points/streak numbers, and the
animations firing from a genuine completion all stay unverified — as do the four Phase 1 gamification
endpoints. The backend is up and reachable now, so this is a deliberate deferral, not a blocker. It
goes in the implementation notes as an explicit gap.

## Housekeeping

- Write `docs/feature/gamification/2026-08-08-session-status-migration.md` **before** starting
  (CLAUDE.md: plans are written first and are immutable once written), and append its
  `## Implementation notes (what actually differed)` section at the end.
- Commit message starts **`AWAN-141:`**.

---

## Implementation notes (what actually differed)

### Build / test status

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | BUILD SUCCESSFUL — `:core:data` at 92 tests, 0 skipped, 0 failures, including 6 new `HomeRepositoryImplTest` cases |
| `./gradlew lint` | BUILD SUCCESSFUL |

### Not verified — deliberate

Per the agreed scope there was **no live run and no device run**. The reward path is exercised only
against hand-written fakes. Still unproven against the real server:

- the actual `POST /sessions/{id}/complete` response shape and its real points/streak numbers;
- the points flight and streak overlay firing from a genuine completion;
- the rollback path on a failed completion;
- the four Phase 1 gamification endpoints, which have never had a live check either.

The backend is up and reachable (`/api/v1/sessions/{id}/complete` answers **401**, not 404, so the
route exists and the URL is right). This is a deferral, not a blocker — it is the first thing to do
when someone next runs the app.

### Deviations from the plan as written

1. **Two cache helpers instead of one.** The plan called for a single
   `patchCachedSession(sessionId, transform)`. It became `patchCachedStatus(sessionId, status)` plus
   `patchOnSuccess(result, sessionId, status)`, because three of the four calls share the identical
   "on success, patch, return Unit" shape and only `completeSession` needs to return a payload.
   Collapsing them into one transform-taking helper made every call site longer, not shorter.
2. **`unexpectedLoading()` added.** `Result` is a three-armed sealed interface, and `Loading` is never
   emitted by these suspend calls. Rather than repeat a throwaway `else ->` branch four times, one
   named helper says why the branch exists.
3. **`SessionStatus` import removed from `HomeViewModel`.** With the status strings gone, the enum was
   no longer referenced there at all. The enum itself stays — `HomeMapper.parseSessionStatus` still
   reads statuses off the wire.
4. **`CancelSessionUseCase` has no caller**, as planned. It exists so the third documented transition
   is reachable; wiring a UI for it is a separate piece of work.
5. **`refreshGamificationProgressUseCase()` now runs only on uncomplete.** On the complete path it was
   removed, not merely replaced. **This is easy to reintroduce by accident**: it looks like a harmless
   "make sure the badge is fresh" call, but the reward already carries `newValue`, and refreshing in
   parallel overwrites the badge with the final total while the stars are still mid-flight — killing
   the count-up. If the count-up ever stops animating on session completion, look here first.
6. **`AwanScheduleTaskCard`'s stamped tap origin needed no change** — Phase 1 already records the
   checkbox bounds on tap, so the stars fly out of the tapped session with no extra wiring.
