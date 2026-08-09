# Gamification — points, streak, daily wheel (+ session-status migration, split out)

**Date**: 2026-08-08
**Jira**: [AWAN-140 — React to Streak and Points Change](https://ezdo.atlassian.net/browse/AWAN-140)
**Feature**: Gamification (`:core:network`, `:core:domain`, `:core:data`, `:core:design-system`, `:app`,
`:feature:home:impl`, `:feature:calendar:impl`)
**Contract**: [gamification-framework.md](gamification-framework.md) — the backend's frozen spec, checked in
alongside this plan. Section references below (§1.2, §5, §6–§8) point at it.

## Context

The backend has shipped a gamification contract ([gamification-framework.md](gamification-framework.md)) and separately
changed how session status is mutated. Two problems today:

1. **Points and streak are faked client-side.** [HomeViewModel.kt:245](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:245)
   does `state.pointsCount ± sessionPoints` locally, fire-and-forget, with no rollback on failure. The
   calendar likewise *guesses* streak days by counting back from today
   ([CalendarDateMapper.calculateStreakDates](feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarDateMapper.kt)).
   There is no wheel, and nothing celebrates a reward.
2. **Session status is migrating.** The app mutates status through the generic `PUT v1/sessions/{id}` with a
   `status` field ([SessionApiService.kt:11](core/network/src/main/kotlin/com/awan/app/core/network/api/SessionApiService.kt:11)).
   The backend now requires dedicated `POST .../complete|uncomplete|cancel`, and only `complete` returns the
   reward payload.

**Outcome:** the server becomes the single source of truth for points/streak, the app celebrates rewards with
cross-screen animations, and a daily wheel appears on Home when a spin is available.

### Two phases, two commits

The only thing coupling these is the `reward` object on `POST /sessions/{id}/complete`. Everything else —
progress, activity dates, the wheel, and **all** the animation machinery — is independent, because the wheel
already produces both a points reward and an item reward.

| | Phase 1 — Gamification | Phase 2 — Session migration |
|---|---|---|
| Endpoints | `/gamification/*` (progress, activity-dates, wheel) | `POST /sessions/{id}/complete\|uncomplete\|cancel` |
| Reward source | daily wheel (coins + items) | session completion (points + streak) |
| Animations | built and live, driven by the wheel | streak overlay gains its live trigger |
| Session code | untouched, except deleting the local points math | `HomeRepository` / `HomeRemoteDataSource` / `SessionApiService` reshaped |
| Ships alone? | **yes** | needs Phase 1's reward bus |

Phase 2 is additive: it swaps the endpoint and publishes a `SessionReward` onto the bus Phase 1 already built.
No Phase 1 file is rewritten by Phase 2 — the touch lists are disjoint except `HomeViewModel` and
`HomeRepository`.

### Scope

**In:** `/gamification/progress` · `/gamification/activity-dates` · daily wheel (`/wheel/config`, `/wheel/spin`) ·
points-award animation · streak loot overlay · item-win flight animation · session-status migration (Phase 2).

**Out:** store catalog, buying, inventory screen, equipping (doc §6–§8). The wheel's item payout is animated
but never listed — we only need `name` + `image` from the spin response.

---

## Architecture decisions

- **No new Gradle module.** The overlays are not navigation destinations — same case as `:feature:add-task`.
  Stateless overlay composables go in `:core:design-system`; the host + event plumbing go in `:app`, mirroring
  how `sessionExpiredEvents` already flows `MainActivityViewModel → AwanApp`
  ([AwanApp.kt:110](app/src/main/java/com/awan/app/AwanApp.kt:110)). Wheel state lives in `HomeViewModel`
  because the badge and overlay are both on Home.
- **`:core:design-system` stays domain-free.** It defines its own UI models (as `ScheduleSession` already does);
  `:app`/`HomeViewModel` map domain → UI models.
- **`awarded:false` / `updated:false` collapse to `null` in the mapper.** The doc's "always read the flag" rule
  gets enforced once, in `:core:data`, so no caller can forget it.
- **Reward publication uses a data-layer bus, not presentation.** The wheel publishes in Phase 1; sessions
  publish through the identical path in Phase 2. Nothing routes rewards through a ViewModel.
- **Room stays the progress cache.** `UserEntity` already carries `points/streak/maxStreak` and
  `HomeRepositoryImpl.getUserProfile()` already writes them. No new proto DataStore fields — duplicate state.
- **Delete, don't extend, the old points API.** `ProfileRepository.incrementStreak/resetStreak/awardPoints/deductPoints`
  ([ProfileRepository.kt:37-43](core/domain/src/main/kotlin/com/awan/app/core/domain/profile/repository/ProfileRepository.kt:37))
  are called by nobody and are now *wrong* to call (doc §1.2: points only change via completion, wheel, purchase).

---

# Phase 1 — Gamification (standalone commit)

### 1.1 `:core:network`

New `api/GamificationApiService.kt` + `dto/gamification/`:

| Method | Endpoint | Returns |
|---|---|---|
| `getProgress()` | `GET v1/gamification/progress` | `GamificationProgressDto(points, streak, maxStreak)` |
| `getActivityDates(start, end)` | `GET v1/gamification/activity-dates` | `List<String>` |
| `getWheelConfig()` | `GET v1/gamification/wheel/config` | `WheelConfigDto(segments, claimedToday, lastClaim)` |
| `spinWheel()` | `POST v1/gamification/wheel/spin` | `WheelSpinDto(segmentId, payoutType, coinsAwarded, newBalance, item)` |

Register in `NetworkModule` alongside `SessionApiService` (~line 152). The base URL already ends in `/api/`,
so paths start at `v1/`.

Also define the reward DTOs here — `RewardDto`, `PointsRewardDto`, `StreakRewardDto` — even though only the
wheel populates them in Phase 1. They are the shared vocabulary both phases map into, and defining them now
is what keeps Phase 2 additive.

**Remove** the four gamification endpoints from `ProfileApiService` + `AwardPointsRequest`/`DeductPointsRequest`.

### 1.2 `:core:domain` — new `gamification/` package

`model/`: `GamificationProgress(points, streak, maxStreak)` ·
`SessionReward(points: PointsAward?, streak: StreakChange?)` ·
`PointsAward(amount, oldValue, newValue)` ·
`StreakChange(oldValue, newValue, maxStreakBroken, maxStreakNew)` ·
`WheelConfig(segments, claimedToday)` · `WheelSegment(id, coins, isItem)` ·
`WheelSpinResult(segmentId, coins, newBalance, item: WonItem?)` · `WonItem(id, name, imageUrl)` ·
`RewardEvent` (sealed: `Points(amount, newTotal)`, `Streak(...)`, `Item(name, imageUrl)`).

`repository/GamificationRepository`:
```kotlin
fun observeProgress(): Flow<GamificationProgress>
fun observeRewards(): Flow<RewardEvent>
suspend fun refreshProgress(): Result<GamificationProgress>
suspend fun getActivityDates(start: LocalDate, end: LocalDate): Result<Set<LocalDate>>
suspend fun getWheelConfig(): Result<WheelConfig>
suspend fun spinWheel(): Result<WheelSpinResult>
```

`usecase/`: `ObserveGamificationProgressUseCase`, `ObserveRewardEventsUseCase`,
`RefreshGamificationProgressUseCase`, `GetActivityDatesUseCase`, `GetWheelConfigUseCase`, `SpinWheelUseCase`.
One `operator fun invoke` each; follow
[VerifyOtpUseCase](core/domain/src/main/kotlin/com/awan/app/core/domain/auth/usecase/VerifyOtpUseCase.kt).

**Remove** the four dead gamification methods from `ProfileRepository`.

### 1.3 `:core:data` — new `gamification/` package

- **`GamificationEventBus`** (`@Singleton`, internal to `:core:data`) — holds
  `MutableStateFlow<GamificationProgress>` + `MutableSharedFlow<RewardEvent>(extraBufferCapacity = 8)` and
  `publish(...)`. Keeping it separate from `GamificationRepositoryImpl` is what lets `HomeRepositoryImpl`
  publish in Phase 2 without a data→data dependency on another impl.
- **`remote/GamificationRemoteDataSource(.Impl)`** — one `safeApiCall(dispatcher = ioDispatcher, json = json)`
  per endpoint, matching [AuthRemoteDataSourceImpl](core/data/src/main/kotlin/com/awan/app/core/data/auth/remote/AuthRemoteDataSourceImpl.kt).
- **`repository/GamificationRepositoryImpl`** (`@Singleton`) — seeds progress from `userDao.getFirstUser()`,
  refreshes from `/progress`, writes points/streak/maxStreak back to `UserEntity` on change so it survives
  process death.
- **`mapper/GamificationMapper.kt`** — DTO → domain, collapsing `awarded:false`/`updated:false` to `null`.
  Wheel spin branches on the **response's** `payoutType`, not the config's (doc §5: they disagree when the
  user already owns every item).
- **DI** — `@Binds @Singleton` pairs added to the existing
  [DataModule.kt](core/data/src/main/kotlin/com/awan/app/core/data/di/DataModule.kt); do **not** create a new module.
- Delete the four `ProfileRepositoryImpl` / `ProfileRemoteDataSource` gamification methods.

### 1.4 `:core:design-system`

**`RewardAnchors.kt`** — `@Stable class RewardAnchors { var pointsBadge: Rect?; var profileTab: Rect?;
var lastTapOrigin: Rect? }` + `LocalRewardAnchors` (default = inert instance so previews work) and a
`Modifier.rewardAnchor(slot)` using `onGloballyPositioned { it.boundsInRoot() }`. This is the only genuinely
new mechanism here — `onGloballyPositioned` is currently used exactly once, and only for size
([AwanScheduleTimeline.kt:187](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanScheduleTimeline.kt:187)).
It avoids threading pixel coordinates through three layers of composable signatures.

**`AwanPointsBadge.kt` / `AwanStreakBadge.kt`** — extract from the inlined blocks in
[AwanHeaderBar.kt:109-157](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanHeaderBar.kt:109),
replacing the hardcoded `Color(0xFFEA580C)` / `Color(0xFFEAB308)` / `Color(0xFFCA8A04)` with the existing
`AwanTheme.colors.streakSurface`/`streakIcon` tokens plus two new `pointsSurface`/`pointsIcon` tokens in
`Tokens.kt` (light + dark). The points badge registers its anchor and takes a `pulse: Boolean` for star
arrivals. Reused verbatim by the overlay's fallback pill.

**`PointsFlightOverlay.kt`**
1. `+N` label appears at `lastTapOrigin` (screen centre if null), scaling in with `AwanTheme.motion.playful`.
2. Label fades while `min(6, max(3, amount / 5))` star glyphs (`Lucide.Star`) materialise at the same point.
3. Stars fly one after another along a quadratic bézier to `pointsBadge`, staggered by
   `AwanTheme.motion.staggerMillis`, rotating and shrinking en route.
4. Each arrival pulses the badge (1 → 1.18 → 1) and ticks the number up by its share; the last star lands on
   the exact `newValue`.
5. **No badge anchor registered** (another screen, or the header scrolled away): the overlay renders its own
   `AwanPointsBadge` pill top-centre under the status bar, fades it in, flies the stars to it, holds ~600ms,
   fades out. This is the "if you are in another screen, this item appears on that screen" case.

**`StreakLootOverlay.kt`** — built in Phase 1, gains its live trigger in Phase 2. Previewable and demoable
standalone.

The fire is the **user-supplied Lottie** at `~/Downloads/Fire with png.json` — copied to
`core/design-system/src/main/res/raw/streak_fire.json` (raw resource names must be lowercase with no spaces;
the current filename is not a legal resource name). Verified: 512×512, 60fps, frames 60→240 (3.0s), 18 shape
layers, `assets: []` — despite the name it embeds **no** PNGs, so it is fully self-contained and needs no
drawable companions. `mascot.json` already lives in that same `res/raw/`.

1. Scrim fades to `Color.Black.copy(alpha = 0.55f)` over 220ms.
2. **Unlit:** the Lottie held at `progress = 0f` with a grayscale `ColorFilter` at 0.35 alpha.
3. **Ignite** at ~300ms: the grayscale filter and alpha ramp off over ~300ms while the composition plays
   forward, then loops (`LottieConstants.IterateForever`) for the rest of the overlay. A radial glow blooms
   behind it and the container scales 0.8 → 1.05 → 1.0 on `motion.bouncy`. Manual `progress` control via
   `LottieAnimation(composition, progress = { … })` for the ignite, handing over to
   `animateLottieCompositionAsState` for the loop.
4. Streak number below slides old → new via `AnimatedContent` as the ignite peaks.
5. Motivational line underneath in `AwanTheme.colors.sky`, picked from a small localized set by streak value.
6. **Max-streak-broken variant**: gold accent ring, a "new personal best" headline plus the new max, and one
   ember burst reusing [SparkleBurst.kt](core/design-system/src/main/java/com/awan/app/core/designsystem/SparkleBurst.kt).
   The normal ignite gets no burst — the Lottie already carries the fire, and stacking particles on it is
   redundant.
7. Dismiss on tap anywhere, or auto after 3.2s (4.5s for the max-streak variant).

`:core:design-system/build.gradle.kts` gains `implementation(libs.lottie.compose)`. The version catalog
already declares it (`lottie = "6.6.2"`); it is currently wired into `:feature:auth:impl` only
([AuthenticationMascot.kt:27](feature/auth/impl/src/main/java/com/awan/feature/auth/impl/ui/components/AuthenticationMascot.kt:27)
is the usage pattern to copy). The points-flight stars stay as `Lucide.Star` glyphs — no asset needed there.

**`ItemFlightOverlay.kt`** — `AwanRemoteImage` scales in at centre, then flies to `profileTab`, shrinking, with
a sparkle and tab pulse on arrival.

**`AwanWheelOverlay.kt` / `AwanWheelBadge.kt`** — badge shows only when a spin is available; overlay draws the
7 wedges on a `Canvas` in config order (coin amount, or a gift icon on the item wedge). Tap → spin → rotate
5 full turns plus the offset that lands the returned `segmentId` under the pointer (~2.6s, `FastOutSlowIn`) →
result card → close.

**All overlays honour `reducedMotion()`** ([AwanReducedMotion.kt](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanReducedMotion.kt)):
skip flight/particles, jump the counter, and show the streak overlay static for 1.5s with the fire Lottie
pinned to a lit frame (no playback, no ignite ramp).

**Moving text must carry a concrete `TextStyle`** — never `Modifier.styleable` inheritance. The
`isInheritedTextStyleEnabled = false` workaround at [MainActivity.kt:62-66](app/src/main/java/com/awan/app/MainActivity.kt:62)
exists precisely because foundation's inherited-style cache corrupts text that moves, and the flying `+N`
label is exactly that case.

**`AwanBottomNavBar`** — new `anchoredItemId: String? = null` param; registers that item's bounds as
`profileTab`. Keeps the design system ignorant of feature route names.

New `ds_*` strings in **both** `values/` and `values-ar/` (lint has `error += "MissingTranslation"`).

### 1.5 `:app`

- **`RewardOverlayHost.kt`** (new) — collects the reward flow into a **serial queue: points → item → streak**
  (the "start with the points order" ordering), renders one overlay at a time, drops queued events on logout.
  Placed as the last child of the root `Box` in
  [AwanApp.kt:130-238](app/src/main/java/com/awan/app/AwanApp.kt:130), so it sits above `NavDisplay` *and* the
  bottom bar while sharing their coordinate space (a `Dialog`/`Popup` would not).
- **`MainActivityViewModel`** — add `rewardEvents: Flow<RewardEvent>` and `progress: StateFlow<GamificationProgress>`,
  exactly as `sessionExpired` is exposed today.
- **`AwanApp`** — two new params; wrap the root `Box` in `CompositionLocalProvider(LocalRewardAnchors provides …)`;
  pass `anchoredItemId = TopLevelDestination.PROFILE.name` to `AwanBottomNavBar`.

### 1.6 `:feature:home:impl` — minimal touch, no session-endpoint change

- Inject `ObserveGamificationProgressUseCase`, `RefreshGamificationProgressUseCase`, `GetWheelConfigUseCase`,
  `SpinWheelUseCase`. `streakCount`/`pointsCount` now come from `observeProgress()` instead of the one-shot
  `getUserProfile()`.
- **Delete the local points arithmetic** in `toggleSessionStatus`
  ([lines 245-249](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:245)) and call
  `refreshProgress()` after the existing update succeeds. This is a net deletion, it keeps a single writer to
  `pointsCount`, and it needs none of Phase 2 — the session call itself stays on the current `PUT` for now.
- **Wheel state** in `HomeUiState`: `hasFreeSpin` (from `getWheelConfig().claimedToday == false`), `isWheelOpen`,
  `wheelSegments`, `spinResult`. A `409 DAILY_GIFT_ALREADY_CLAIMED` flips the badge to claimed rather than
  showing an error (doc §5).
- **`HomeScreen`** — wheel badge aligned `TopEnd` with `statusBarsPadding()` on the outer `Box`
  ([HomeScreen.kt:77](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeScreen.kt:77)), not inside
  `AwanHeaderBar` (the mascot owns that corner of the header row). Wheel overlay rendered from the same `Box`.
  On close, the coin/item reward is published so `RewardOverlayHost` animates it — deliberately after dismissal,
  never during the spin overlay.

### 1.7 `:feature:calendar:impl`

`CalendarViewModel` gains `GetActivityDatesUseCase` and `ObserveGamificationProgressUseCase`. On load and on
month change, fetch activity dates for the visible grid range (the grid spans 35–42 days, so query
`monthDays.first().date` … `monthDays.last().date`) and feed `CalendarUiState.streakDates` with real data
instead of `CalendarDateMapper.calculateStreakDates`. Keep that function as the offline fallback when the call
fails, so the streak line doesn't vanish offline. `CalendarUiState.streak` moves to `observeProgress()` so it
can't diverge from the header.

---

# Phase 2 — Session-status migration (separate commit)

Purely additive on top of Phase 1. Deferrable to a later PR without leaving Phase 1 broken.

### 2.1 `:core:network`

`SessionApiService` — add the three dedicated endpoints; keep `@PUT v1/sessions/{id}` (drag-to-move still needs
a time-only update) but drop `status` from `UpdateSessionRequest`:

```kotlin
@POST("v1/sessions/{sessionId}/complete")
suspend fun completeSession(@Path("sessionId") sessionId: String): CompleteSessionResponse

@POST("v1/sessions/{sessionId}/uncomplete")
suspend fun uncompleteSession(@Path("sessionId") sessionId: String): SessionDto

@POST("v1/sessions/{sessionId}/cancel")
suspend fun cancelSession(@Path("sessionId") sessionId: String): SessionDto
```

New `dto/session/CompleteSessionResponse(session, reward)` reusing the reward DTOs from Phase 1. Add
`firstCompletedAt` to `SessionDto`. Remove the dead `status` field from `SessionDraftDto`
([CreateTaskWithSessionsRequest.kt:14](core/network/src/main/kotlin/com/awan/app/core/network/dto/task/CreateTaskWithSessionsRequest.kt:14))
— nothing sets it and the backend ignores it.

### 2.2 `:core:domain`

`home/repository/HomeRepository` — replace `updateSessionStatus(...)` with:
```kotlin
suspend fun completeSession(sessionId: String): Result<SessionReward>
suspend fun uncompleteSession(sessionId: String): Result<Unit>
suspend fun cancelSession(sessionId: String): Result<Unit>
suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<Unit>
```

New `home/usecase/CompleteSessionUseCase`, `UncompleteSessionUseCase`, `MoveSessionUseCase`. This closes the
existing CLAUDE.md violation — `HomeViewModel` currently injects `HomeRepository` directly
([HomeViewModel.kt:45](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:45)).

`cancelSession` gets full plumbing but **no UI** — nothing in the app cancels a session today. It is the
documented replacement for a status the domain enum already has.

### 2.3 `:core:data`

`HomeRemoteDataSource(.Impl)` — replace `updateSession(status,…)` with `completeSession`, `uncompleteSession`,
`cancelSession`, `moveSession`. `HomeRepositoryImpl`
([line 133](core/data/src/main/kotlin/com/awan/app/core/data/home/repository/HomeRepositoryImpl.kt:133)) keeps
its `scheduleCache` patch loop, returns the mapped reward, and publishes it to the Phase 1 `GamificationEventBus`.

### 2.4 `:feature:home:impl`

- `toggleSessionStatus` → `CompleteSessionUseCase` / `UncompleteSessionUseCase`. The `refreshProgress()` call
  added in Phase 1 is replaced by the reward the completion already returned, which now feeds the animation.
- **Roll the optimistic flip back on `Result.Error`** — today the result is discarded entirely.
- `moveSession` ([line 285](feature/home/impl/src/main/java/com/awan/feature/home/impl/ui/HomeViewModel.kt:285))
  → `MoveSessionUseCase` (times only). This is the one path that must **not** move to the dedicated endpoints.

---

## Deliberately not doing

- **The two `SessionStatus` enums stay as they are.** `domain.home.model.SessionStatus` (with `IN_PROGRESS`) and
  `core.model.SessionStatus` (with `UNKNOWN`/`MISSED`) both survive. Unifying them means touching the whole
  task-creation flow for no behavioural gain, and after Phase 2 the app never *sends* a status string at all.
- **No proto DataStore fields for progress** — Room's `UserEntity` already caches it.
- **No `:feature:gamification` module** — see the first architecture decision.

---

## Verification

Run per phase, so Phase 1 is provably shippable on its own.

1. `./gradlew assembleDebug`
2. `./gradlew testDebugUnitTest` — the existing 21 test classes must stay green. Run one existing test file
   first to confirm the harness works before adding new ones.
3. **New tests** in `:core:data` (`src/test/java/…/gamification/`), JUnit 4 + hand-written fakes, matching
   [TaskRepositoryImplTest](core/data/src/test/java/com/awan/app/core/data/task/TaskRepositoryImplTest.kt).
   No MockK/Turbine in this project — don't add them.
   - *Phase 1:* `awarded:false` → null `PointsAward`; `updated:false` → null `StreakChange`; wheel spin branches
     on the **response** `payoutType`, not the config's; a coin spin publishes exactly one `RewardEvent.Points`;
     an item spin publishes exactly one `RewardEvent.Item`; `refreshProgress` updates the observed state.
   - *Phase 2:* completing publishes exactly one `RewardEvent.Points`; a null-reward completion publishes
     nothing; uncompleting publishes nothing (doc: "no points are taken back"); `maxStreakBroken` surfaces on
     the emitted streak event.
4. `./gradlew lint` — `abortOnError = true` with `HardcodedText` and `MissingTranslation` as errors, so this
   catches any missing `values-ar` key. There is no detekt in this project.
5. **On device** — the animations can't be verified by tests.
   - *Phase 1:* tap the wheel badge → spin → close → coins animate **after** dismissal, stars land on the points
     badge, number ticks up; an item win flies to the profile tab. Trigger the wheel from a non-Home screen's
     reward → the temporary points pill appears and receives the stars. Open the calendar → active days come
     from `activity-dates`, not a back-count from today. Streak overlay verified via a `@Preview` or a
     temporary debug trigger, since nothing emits a streak change until Phase 2 — confirm the fire Lottie
     renders, ignites from its unlit state, and loops without stutter.
   - *Phase 2:* complete a session → `+N` morphs to stars → stars land on the badge. Complete the day's first
     session → streak overlay ignites, number slides, motivational line shows. Beat the max streak → the
     celebration variant. Kill the network mid-complete → the optimistic flip rolls back.
   - *Both:* toggle "Remove animations" in developer options → everything degrades to instant. Switch to Arabic
     → check RTL flight directions and that no string is missing.
6. **Endpoint sanity** — the contract doc is the spec; real responses get verified against the running backend
   at implementation time (these endpoints need a live bearer token, so they can't be pre-checked from here).

## Before starting

- Write the plan to `docs/feature/gamification/2026-08-08-points-streak-and-wheel.md` (CLAUDE.md requires it
  before work starts; plans are immutable once written, and implementation notes get appended at the end).
  Phase 2 gets its own dated plan file when it's picked up.
- Find the Jira issue ID — every commit/branch needs an `AWAN-nn`. Search the `AWAN` project for a gamification
  issue; if there's no confident match, ask rather than guess. Phase 1 and Phase 2 may well be separate issues.

---

## Implementation notes (what actually differed)

Phase 1 only. Phase 2 (session-status migration) was deliberately deferred to a separate commit and
is not implemented here — see the phase table above.

### Build / test status

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | BUILD SUCCESSFUL — all pre-existing suites green, plus 15 new tests (9 mapper, 6 event bus), 0 skipped |
| `./gradlew lint` | BUILD SUCCESSFUL — `abortOnError = true` with `MissingTranslation`/`HardcodedText` as errors, so both `values/` and `values-ar/` are confirmed complete |
| On device (emulator-5554) | Installed, launched, no crash; both animations captured and verified |

### Verified on device

The points flight and the streak overlay were driven by a **temporary** debug trigger in
`RewardOverlayHost` (queue seeded directly, then reverted — it is not in the committed diff), because
the backend was down and no real reward could be earned. What was confirmed from screenshots:

- **Streak overlay**: scrim dims the screen, radial glow blooms, the max-streak sparkle ring fires,
  the counter slides to `6 day streak`, the gold `New personal best!` headline and the blue
  `7 days — your longest streak yet.` subtitle both render, and the fire Lottie plays and loops.
- **Points flight**: the header badge was observed reading `155` mid-flight while the screen's own
  `pointsCount` was `0` — proving the overlay drives the badge through `RewardAnchors.animatedPoints`
  and that the count climbs star by star rather than snapping to the total.

### Not verified — backend was down

`/gamification/progress`, `/activity-dates`, `/wheel/config` and `/wheel/spin` were **never exercised
against a live server**. Railway returned `{"status":"error","code":404,"message":"Application not
found"}` for every path, including pre-existing ones (`/api/v1/users/me`, `/api/v1/zones/date/…`) and
the `/api/` root — so this is the deployment being down, not wrong paths on our side. The request
URLs were confirmed correct in the OkHttp log. **Re-verify all four endpoints once the backend is
back**, especially the wheel's item-payout branch.

### Deviations from the plan as written

1. **`LocalRewardAnchors` is provided in `MainActivity`, not `AwanApp`.** Wrapping `AwanApp`'s root
   `Box` in a `CompositionLocalProvider` would have re-indented ~100 untouched lines for no benefit.
   The scope is identical.
2. **The badge count-up needed a mechanism the plan did not anticipate.** "The points number is
   increased incrementally on each star" cannot work if the header badge renders the ViewModel's
   `pointsCount`, because the server's new total is already in state by the time the animation runs —
   the badge would show the final figure before the first star landed. `RewardAnchors` gained
   `animatedPoints` and `pointsPulse`; `AwanPointsBadge` prefers them while a flight is in progress.
   **This is easy to reintroduce**: anyone who "simplifies" the badge back to plain `pointsCount`
   silently kills the count-up, and nothing fails — it just stops animating.
3. **`spinWheel()` no longer publishes its own reward.** It originally banked the balance and emitted
   immediately, which fired the star flight *behind* the still-open wheel overlay and left the
   counter with nothing to count up to. Publication moved to a separate
   `publishWheelReward` / `PublishWheelRewardUseCase`, called from `closeWheel()`. **Easy to
   reintroduce** by "tidying" the two calls back into one.
4. **`SideEffect` is load-bearing in `PointsFlightOverlay`.** The overlay writes `animatedPoints`,
   which the header badge — composed earlier in the same frame — reads. Writing it inline during
   composition is a backwards write. It must stay in the `SideEffect`.
5. **`CalendarViewModel.render()` treats the back-counted estimate as a fallback only.** It
   originally recomputed `streakDates` from the streak count on every snapshot emission, which
   overwrote the real activity dates seconds after they arrived. It now only fills in when
   `streakDates` is still empty.
6. **`RewardOverlayHost` does not sort its queue.** An earlier version sorted by event type, which
   could shuffle a later action's points ahead of an earlier action's streak. The data layer already
   publishes points before streak, so arrival order is correct as-is.
7. **The Lottie's first frame is nearly empty**, so the "unlit" state reads as *almost nothing* and
   the ignite as the fire forming. This looks better than the dimmed-still-frame the plan described,
   but it means `LIT_FRAME = 0.45f` (the reduced-motion pose) is a hand-picked frame — if the asset
   is ever replaced, that constant must be re-picked or reduced-motion users get an empty box.
8. **`AwanHeaderBar` lost its emoji badges.** The inline `🔥`/`🪙` blocks with hardcoded hex became
   `AwanStreakBadge`/`AwanPointsBadge` using Lucide icons and the theme's `streakSurface`/`streakIcon`
   plus new `pointsSurface`/`pointsIcon` tokens.
9. **`ProfileApiService` lost four endpoints** (`streak/increment`, `streak/reset`, `points/award`,
   `points/deduct`) along with `AwardPointsRequest`/`DeductPointsRequest` and their repository and
   data-source methods. Nothing called them, and the contract now forbids client-driven point changes.
