# Send onboarding zones to the backend as a weekly template

## Context

Today the Zones step is the only part of onboarding whose output never reaches the server. `OnboardingViewModel` packs `state.zones` into `OnboardingData`, but `OnboardingRepositoryImpl` builds `CompleteOnboardingRequest` from profile + wake/sleep + session length only — the zone windows the user arranged die in memory when the process does.

The backend has the endpoint for them: `POST /api/v1/templates` (Postman → Awan → Templates → Create Template). One template holds a set of days plus its zones, so a single call after the Task Length step persists the user's zone layout for their whole week.

Outcome: after onboarding, the server holds one template named for the user's week, assigned to all seven days, carrying every enabled zone with the exact start/end times the user configured.

## Contract (from Postman)

`POST {baseUrl}/v1/templates` — auth required (`Authorization: Bearer …`, handled by `AuthInterceptor`).

```json
{
  "name": "My Week",
  "daysOfWeek": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"],
  "zones": [
    { "name": "Study", "startTime": "07:30:00", "endTime": "11:20:00", "color": "#7A64FF" }
  ]
}
```

Response `200` → `TemplateResponse`: `id`, `name`, `daysOfWeek`, `zones[]` (`ZoneResponse`: `id`, `name`, `startTime`, `endTime`, `color`, `templateId`, `templateOverrideId`), `category` (`id`, `name`).

Constraints that matter: `endTime` must be strictly after `startTime` (`INVALID_ZONE_TIME_RANGE` 400), max 50 zones, each day belongs to at most one template (`DAY_ALREADY_ASSIGNED` 409), zones must not overlap (`ZONE_OVERLAP` 409).

## Decisions taken

- **All seven days** on one template — one call, zone times identical every day.
- **No zone may cross midnight.** `LocalTime` on the server has no wrap concept. Enforced at the edit rule so the user can't set one, and clamped at the mapper as the safety net (see below).
- **Full Template vertical in `:core:domain`**, per the Clean Architecture section of `CLAUDE.md` — not bolted onto the existing `OnboardingRepository`, which that file already lists as a known violation.

## Midnight handling — two halves

`Zone.endMinutes` is *not* normalised: `SuggestZoneScheduleUseCase` computes `startMinutes.mod(1440)` then `startMinutes + duration`, so `endMinutes` legitimately exceeds 1440. `DayBounds` explicitly supports an overnight waking window (`isOvernight`, e.g. wake 22:00 / sleep 06:00), so wrapping zones are reachable today.

1. **Prevent** — in `ZoneEditRules.editWindow` (`core/domain/src/main/kotlin/com/awan/app/core/domain/onboarding/ZoneEditRules.kt`), the single choke point every user-set time flows through (`ZoneSheet` → `OnboardingAction.EditZoneWindow` → `editWindow`). After computing `absStart`/`duration`, coerce so the window ends by end of day:

   ```kotlin
   val absStart = absolute(linStart, bounds.wakeMinutes)
       .coerceAtMost(DayBounds.MINUTES_PER_DAY - MIN_ZONE_MINUTES)
   val absEnd = (absStart + duration).coerceAtMost(DayBounds.MINUTES_PER_DAY)
   return zone.copy(startMinutes = absStart, endMinutes = absEnd)
   ```

   Silent clamp, no new error state — so no new strings and no localization work.

2. **Clamp** — `resequence` and `SuggestZoneScheduleUseCase` can still emit a wrapping zone under overnight bounds (nothing the user "set"). The mapper is the backstop: `endMinutes >= 1440` → `"23:59:59"`. Lossy by decision; the after-midnight remainder is dropped rather than split.

Overnight `DayBounds` themselves stay legal — this plan does not touch `ValidateDayBounds`. If night-owl schedules should be blocked outright instead, that is a separate one-line change and a new hard-error string in both `values/` and `values-ar/`.

## Files

### `:core:network` — DTOs + service

- **NEW** `core/network/src/main/kotlin/com/awan/app/core/network/dto/CreateTemplateRequest.kt` — `CreateTemplateRequest(name, daysOfWeek: List<String>, zones: List<TemplateZoneRequest>)` and `TemplateZoneRequest(name, startTime, endTime, color)`. Follow the house style exactly: `@Serializable data class`, explicit `@SerialName` on every property (see `CreateTaskRequest.kt`).
- **NEW** `core/network/src/main/kotlin/com/awan/app/core/network/dto/TemplateResponse.kt` — `TemplateResponse`, `ZoneResponse`, `CategoryResponse`; response fields nullable with defaults. These names resolve the dangling KDoc links already written against them in `core/database/.../model/TemplateEntity.kt` and `ZoneEntity.kt`.
- **NEW** `core/network/src/main/kotlin/com/awan/app/core/network/api/TemplateApiService.kt`:
  ```kotlin
  interface TemplateApiService {
      @POST("v1/templates")
      suspend fun createTemplate(@Body request: CreateTemplateRequest): TemplateResponse
  }
  ```
- **EDIT** `core/network/src/main/kotlin/com/awan/app/core/network/di/NetworkModule.kt` — add `providesTemplateApiService(retrofit: Retrofit)` next to `providesTaskApiService`. Use the **unqualified** `Retrofit` (the authed one); `@NoAuthRetrofit` is for `AuthApiService` only.

### `:core:domain` — contract + use case

- **NEW** `core/domain/src/main/kotlin/com/awan/app/core/domain/template/repository/TemplateRepository.kt`:
  ```kotlin
  interface TemplateRepository {
      /** Creates the user's single weekly template covering all seven days. */
      suspend fun createWeeklyTemplate(zones: List<Zone>): Result<Unit>
  }
  ```
  Domain-typed in `:core:model`'s `Zone`; no DTO leaks. `Result<Unit>` — the response is not needed above the data layer, so no domain template model is introduced. `daysOfWeek` is not a parameter: "all seven" is the requirement, add the parameter when a second template exists.
- **NEW** `core/domain/src/main/kotlin/com/awan/app/core/domain/template/usecase/CreateWeeklyTemplateUseCase.kt` — thin `@Inject constructor` + single `suspend operator fun invoke(zones: List<Zone>)`, mirroring `core/domain/auth/usecase/VerifyOtpUseCase.kt`.

No `build.gradle.kts` change: `:core:domain` already has `:core:model` and `:core:common`, and `SuggestZoneScheduleUseCase` already returns `List<Zone>` across the module boundary.

### `:core:data` — impl, data source, DI

- **NEW** `core/data/src/main/kotlin/com/awan/app/core/data/template/remote/TemplateRemoteDataSource.kt` + `TemplateRemoteDataSourceImpl.kt` — DTO in, DTO out, one `safeApiCall`. Copy `OnboardingRemoteDataSourceImpl` verbatim in shape (inject api service, `Json`, `@Dispatcher(AwanDispatchers.IO)`).
- **NEW** `core/data/src/main/kotlin/com/awan/app/core/data/template/TemplateRepositoryImpl.kt` — the mapper lives here (this codebase has no separate mapper classes; `OnboardingRepositoryImpl` is the precedent). Per zone:
  - **filter** `isEnabled` — a toggled-off zone is not sent;
  - `name` → `zone.name` as-is;
  - `startTime` → `"%02d:%02d:00"` of `startMinutes` (already `0..1439`);
  - `endTime` → `"23:59:59"` when `endMinutes >= DayBounds.MINUTES_PER_DAY`, else the same formatter;
  - `color` → `"#%06X".format(Locale.US, zone.colorArgb and 0xFFFFFF)` — `Zone.colorArgb` is an ARGB `Int`, the API wants a 6-digit hex string;
  - `daysOfWeek` → a private `const`/`val` list of the seven uppercase day names;
  - `name` (template) → a constant. Mark it `// ponytail:` — there is no UI to name a template, and this is server-stored data, so it is deliberately not localized.

  Reuse the time formatter rather than duplicating it: `OnboardingRepositoryImpl.formatMinutesToTime` is private and identical — lift it to an internal top-level helper in `:core:data` (e.g. `core/data/src/main/kotlin/com/awan/app/core/data/util/TimeFormat.kt`) and have both call it. Keep its `mod(MINUTES_PER_DAY)`; the clamp is decided at the call site.
- **EDIT** `core/data/src/main/kotlin/com/awan/app/core/data/di/DataModule.kt` — two `@Binds @Singleton`, for the data source and the repository.

### `:feature:onboarding:impl` — the call

- **EDIT** `feature/onboarding/impl/src/main/java/com/awan/feature/onboarding/impl/presentation/OnboardingViewModel.kt`:
  - inject `CreateWeeklyTemplateUseCase`;
  - `completeOnboardingBeforeTask`, `skipSetup`, and `finishOnboarding` currently build the **same** `OnboardingData` three times. Collapse that into one private `suspend fun submitOnboarding()` and call the template use case there, so every path that completes onboarding also creates the template:
    ```kotlin
    private suspend fun submitOnboarding() {
        val s = _state.value
        val data = OnboardingData(/* … unchanged … */)
        if (repository.completeOnboarding(data) is Result.Success) {
            createWeeklyTemplate(s.zones)
        }
        isBackendOnboarded = true
    }
    ```
    The template call is gated on onboarding success — a 401 would only fail twice otherwise. This also puts the currently-unused `Result` import at line 5 to work.
  - No new state flag: `isSubmittingTask` already drives the spinner and disabled state in `StepChrome.kt` and both calls run inside the same coroutine.

### Tests

- **NEW** `core/data/src/test/java/com/awan/app/core/data/template/TemplateRepositoryImplTest.kt` — anonymous-object fake of the data source, `UnconfinedTestDispatcher`, plain JUnit asserts (the pattern in `OnboardingRemoteDataSourceTest.kt`). Assert on the captured request: disabled zones absent, `HH:mm:ss` format, `endMinutes = 1500` → `"23:59:59"`, `0xFF7A64FF.toInt()` → `"#7A64FF"`, seven days present.
- **EDIT** `core/domain/src/test/kotlin/com/awan/app/core/domain/onboarding/ZoneEditRulesTest.kt` — one case: under overnight bounds, an edit that would push `endMinutes` past 1440 comes back with `endMinutes <= 1440`.
- **EDIT** `feature/onboarding/impl/src/test/java/.../OnboardingViewModelTest.kt` — the constructor gains a sixth dependency; add a fake `TemplateRepository` alongside the existing `FakeOnboardingRepository` and assert the template is created when onboarding succeeds and skipped when it fails.

Per the global workflow rule, run one existing test file first (`./gradlew :core:data:testDebugUnitTest`) to confirm the harness works before writing the new suite.

## Verification

1. `./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:onboarding:impl:testDebugUnitTest` — new + existing suites green.
2. `./gradlew assembleDebug detekt lint`.
3. Run the app through onboarding on a device with the debug HTTP logger at `Level.BODY`: confirm `POST v1/templates` fires right after `POST v1/onboarding`, with seven days and one zone entry per enabled zone at the times shown on the Zones step.
4. Re-check with an overnight day (wake 22:00, sleep 06:00): every `endTime` in the body is `<= 23:59:59` and strictly after its `startTime` — no `INVALID_ZONE_TIME_RANGE`.
5. Optionally confirm server-side with the Postman **List Templates** request (`GET /v1/templates`).

## Known limits, called out rather than fixed

- **No retry / no idempotency.** A second onboarding run hits `DAY_ALREADY_ASSIGNED` (409) because all seven days already belong to the first template. Recovering would mean `List Templates` + `Bulk Update Template Zones` (`PUT /v1/templates/{id}/zones`), which is a separate change.
- **Failures stay silent.** `OnboardingViewModel` already discards every `Result` and sets `isBackendOnboarded = true` unconditionally; this plan does not change that, so a failed template call is invisible and unretried. Fixing it means an error path in `OnboardingState`/`OnboardingEvent` and new strings in `values/` and `values-ar/` — worth a follow-up ticket.
- **Zone names go to the server in English.** `Zone.defaults` hardcodes `"Study"`/`"Work"`/… in `:core:model` and `ZoneRow.kt:96` renders them directly — a pre-existing localization violation, untouched here.
- **Overlapping zones are still sendable.** The app flags overlaps but does not block them (`overlappingZoneIds` is advisory), so a user who leaves an overlap may get `ZONE_OVERLAP` (409).

## Before committing

- The current branch is `feature/AWAN-82-quick-add-task`, which does not cover this work — a Jira issue ID for the template call is needed for the branch/commit message.

## Implementation notes (what actually differed)

Built as planned; the vertical, the two-part midnight handling, and the ViewModel collapse all landed in the shape described above. Differences and things worth knowing:

- **`./gradlew detekt` does not exist.** `CLAUDE.md` lists it under Commands with a root `detekt.yml`, but the plugin is not applied to the root project or any module — `Task 'detekt' not found in root project 'Awan' and its subprojects`. Verification fell back to `assembleDebug` + `lint`, both clean. The `CLAUDE.md` line is stale.
- **The shared time formatter moved, and `OnboardingRepositoryImpl` shrank.** `formatMinutesToTime` was a private method there; it is now `internal` in `core/data/.../util/TimeFormat.kt` and used by both repositories. `OnboardingRepositoryImpl` lost its `DayBounds` and `java.util.Locale` imports as a result.
- **`FakeOnboardingRepository` gained a `failWith: AppError?`** so the "template is not sent when onboarding fails" case is testable. Its `isCompleted` flag still means "was called", not "succeeded" — left as-is to avoid churning the five existing assertions that read it.
- **`ZoneEditRules.resequence` was deliberately left able to cross midnight.** Its existing test (`resequence chains correctly when the waking window crosses midnight`) asserts a zone at `startMinutes = 1410, endMinutes = 1470`, which is the behaviour the overnight chain depends on. Only `editWindow` — the path a user-set time actually flows through — refuses to wrap. This is why the mapper clamp is not redundant: **reorder a zone under overnight bounds and you still get `endMinutes > 1440` reaching the repository.** Easy to reintroduce as a bug by "simplifying" one of the two halves away.
- **`String.format(Locale.US, "#%06X", colorArgb and 0xFFFFFF)`** is the ARGB→hex conversion. Masking matters: without `and 0xFFFFFF` the alpha byte makes it an 8-digit string the API will not accept as a hex color.
- Verified: `:core:data`, `:core:domain`, `:feature:onboarding:impl` unit tests green (4 new mapper tests, 1 new `editWindow` test, 2 new ViewModel tests); `assembleDebug` green, which also proves the Hilt graph resolves the two new `@Binds`; `lint` green. **Not yet exercised against the real backend** — the on-device checks in Verification steps 3–5 are still outstanding.
