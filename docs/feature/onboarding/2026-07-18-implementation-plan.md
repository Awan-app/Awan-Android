# Onboarding Flow — Implementation Plan (AWAN-47)

## Context

AWAN-47 (*Story 1.3 — First-run onboarding, Android*) covers the post-auth first-run flow that
personalizes Awan: capture display name, wake/sleep bounds, an editable suggested zone schedule,
preferred focus length, the user's first real task, and notification permission priming. The visual
and interaction source of truth is the Claude Design project **"Awan Auth & Onboarding"** (Skyward
direction, DP-01 spec). The backend is not ready, so the **data layer is mocked (fully in-memory)**
while the **domain and presentation layers are built accurately** so nothing above the repository
interface changes when the real backend lands.

### Sources of truth
- Design: claude.ai/design project `0cca131a-8a79-4743-87f0-4968737b491d` — `DP-01_Auth_and_Onboarding.md` + `Awan Auth & Onboarding.dc.html`
- Jira: AWAN-47 with subtasks AWAN-53 (US-1.3.0 name), AWAN-43 (US-1.3.1 wake/sleep), AWAN-45 (US-1.3.2 zones), AWAN-49 (US-1.3.3 first task), AWAN-51 (US-1.3.4 notifications)

### Decisions (confirmed with Mohannad)
1. Flow lives in **`feature/onboarding`**; the nav shell is reordered to splash → auth → onboarding → home. `feature/profile-setup` stays but leaves the main flow.
2. **"Difficulty order" step is excluded** (user decision). **"Preferred task length" step is included** (matches design + the 6-step progress indicator).
3. **Single route + one shared ViewModel** — the step is ViewModel state, not a back-stack entry. All steps share the draft (wake/sleep feeds zone suggestion; everything feeds the live day-preview).
4. **Everything in-memory** — all repository implementations are pure in-memory mocks. Nothing persists across launches; onboarding repeats each run until the real data layer arrives. No Room, no WorkManager, no DataStore writes in this story.

## Flow

```
splash → auth (Login → OTP) → ONBOARDING → home
```

Onboarding steps (Welcome is an intro screen, not counted in progress dots; 6 dotted steps):

| # | Step | Jira | Key rules |
|---|------|------|-----------|
| — | Welcome | — | Mascot greet, single CTA. |
| 1 | Name | AWAN-53 | First name required (1–50 trimmed), last optional (0–50). Live "Good morning, {name}" preview per keystroke. Continue gated on first name. Skip → generic greeting. |
| 2 | Wake/sleep | AWAN-43 | wake ≠ sleep (hard error "Wake and sleep times can't be the same."), waking window ≥ 4h (soft dismissible warning), overnight span across midnight handled, 5-min snapping. Skip → 07:00 / 23:00. |
| 3 | Zones | AWAN-45 | Suggestion is deterministic and fixed (NOT AI): the 4 default zones in fixed order, laid contiguously from **wake + 30 min** to **sleep − 30 min** (equal split, 5-min snap), with a UI nudge explaining the margins are wake-up/sleep routine time. MVP constraints: the 4 zones are fixed — **no add, no remove**; the user can **reorder**, **change start/end** (drag handles), and **disable** a zone (not remove it). start < end, ≥15 min, 5-min snap, clamp to bounds with gentle note, overlaps flagged non-blocking. Skip → accept suggestion as-is. |
| 4 | Task length | design DP-01 | 30/45/60/90/120/180-min snapping control, live human label ("About 1½ hours"). Default 60. Skip → 60. |
| 5 | First task | AWAN-49 | Title 1–140 chars, not blank after trim. Mocked capture "schedules" it into the matching zone; celebratory land-in-day moment (mascot bounce). Offline/failure → simple manual local task, never blocks. Skip → empty-but-configured home with "Add your first task" CTA. |
| 6 | Notifications | AWAN-51 | Value-first primer BEFORE the OS prompt (API 33+ `POST_NOTIFICATIONS`). Decline keeps app fully usable; permanently-denied routes to system Settings instead of re-prompting. Finishing marks onboarding complete → home. |

Every step: skippable, back-navigable, shows the "You can change this anytime" chip (steps 1–4), persistent progress dots (steps 1–6), no dead-ends.

## Architecture (NiA conventions)

### New modules

**`:core:model`** (`awan.android.library`; pure Kotlin classes, no Android imports)
- `UserProfile(firstName, lastName)`
- `DayBounds(wakeMinutes: Int, sleepMinutes: Int)` — minutes-from-midnight; overnight = sleep < wake
- `Zone(id, name, colorArgb, startMinutes, endMinutes, isEnabled)` + the four fixed defaults (Study, Work, Play, Personal — names/colors per Epic 2 spec); zone order = list order
- `FirstTask(id, title, zoneId, startMinutes, durationMinutes)`

**`:core:data`** (`awan.android.library` + `awan.android.hilt`; depends on `:core:model`, `:core:common`)
- `OnboardingRepository` interface: `val draft: Flow<OnboardingData>`, suspend setters (`saveProfile`, `saveDayBounds`, `saveZones`, `savePreferredTaskLength`), `suspend fun saveFirstTask(task: FirstTask): Result<Unit, AppError>` (reuses `:core:common` `Result`/`AppError`), `suspend fun completeOnboarding()` — the repository only persists; task *placement* is domain logic (see below)
- `InMemoryOnboardingRepository` — `internal`, `MutableStateFlow`-backed, `@Singleton`. `// ponytail: in-memory mock, swap for OfflineFirst impl when backend lands`
- `DataModule` with `@Binds` (interface → internal impl, per NiA golden rule)

Register both in `settings.gradle.kts`.

### Domain (accurate — in `feature/onboarding/impl/.../domain/`, promote to `:core:domain` only when Epic 2 reuses it)
- `SuggestZoneScheduleUseCase(bounds): List<Zone>` — deterministic: the 4 fixed zones in default order, contiguous equal split from wake + 30 min to sleep − 30 min, 5-min snap; must produce identical output to iOS for the same input (shared test vector per AWAN-45 DoD).
- `ScheduleFirstTaskUseCase(title, draft): FirstTask` — **this is the seam for the future scheduling engine.** MVP body is a deliberate fake: place the task at the start of the first enabled zone of the day, duration = preferred task length. When the Local Conflict Engine module lands, only this use case's body changes to call it — its signature, the ViewModel, and the repository stay untouched. `// ponytail: fake placement (first slot of the day); replace body with the scheduling-engine call when the engine module lands`
- `ValidateDayBounds(bounds): DayBoundsValidation` — pure function: `SameTime` (hard), `ShortWakingWindow` (soft, < 4h), `Valid`; correct across midnight.
- `ZoneEditRules` — pure functions used by the zones step: snap-to-5, clamp-to-bounds, min-15-duration, overlap detection (flag, non-blocking).
- No use case for simple writes — the ViewModel calls the repository directly (NiA rule).

### Presentation (`feature/onboarding/impl`, MVI per `android-presentation-mvi`)
- **`OnboardingViewModel`** (`@HiltViewModel`): single `StateFlow<OnboardingState>` via `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`; `MutableStateFlow` private; one-shot effects (navigate home, launch OS permission prompt) as an `OnboardingEvent` Channel.
- **`OnboardingState`**: `data class` holding `step: OnboardingStep` (enum: Welcome, Name, DayBounds, Zones, TaskLength, FirstTask, Notifications), per-step draft fields + validation results, `dayPreview: DayPreviewModel` derived from bounds + zones + first task, and celebration/loading flags.
- **`OnboardingAction`**: sealed — `NameChanged`, `WakeChanged`, `SleepChanged`, `DismissWakingWarning`, `UseSuggestedZones`, `EditZoneWindow`, `ReorderZone`, `ToggleZoneEnabled`, `TaskLengthChanged`, `FirstTaskTitleChanged`, `SubmitFirstTask`, `EnableNotifications`, `NotificationPermissionResult(granted)`, `Next`, `Skip`, `Back`. (No add/remove-zone actions — MVP fixes the 4 zones.)
- **Composables**: `OnboardingRoute` (stateful: hiltViewModel, `collectAsStateWithLifecycle`, observes events, hosts the `rememberLauncherForActivityResult` permission launcher) → stateless `OnboardingScreen(state, onAction)` with `AnimatedContent` over the step. One file per step (`NameStep`, `DayBoundsStep`, `ZonesStep`, `TaskLengthStep`, `FirstTaskStep`, `NotificationsStep`, `WelcomeStep`) plus shared pieces: `DayPreview` (the live timeline — bounds, zone blocks, task block; drag handles in the zones step), `StepProgressDots`, `ChangeAnytimeChip`, `StepScaffold` (title/subtitle/skip/continue frame). The zones step additionally shows per-zone reorder + enable/disable controls and the routine-margin nudge ("the first/last 30 minutes are for your wake-up/wind-down routine"). Previews with fabricated state for each step.
- Predictive back: `Back` action steps backward inside the flow; on the first step it exits via the navigator.
- Styling via the Styles API + existing `AwanTheme`/`AwanButton`/`AwanText`/`Tokens` (`:core:design-system`). Components stay in the feature impl; promote to design-system when a second feature needs them.
- Mascot: static placeholder drawables for the four expressions now (greet, curious, celebrate, idle); APNG set is a design deliverable to slot in later. `// ponytail: static mascot frames until APNG assets are delivered`

### Wiring changes
- `feature/onboarding/impl/build.gradle.kts`: add `implementation(project(":core:data"))`, `implementation(project(":core:model"))`.
- `app/src/main/java/com/awan/app/AwanApp.kt`: reorder — `splashEntry → LoginRoute`, `authEntry.onNavigateToNext → OnboardingRoute` (replaceAll), `onboardingEntry.onComplete → HomeRoute` (replaceAll). `profileSetupEntry` stays registered but out of the main flow.
- `feature/onboarding/impl/.../OnboardingEntryProvider.kt`: replace placeholder screen with `OnboardingRoute`.
- `app/src/main/AndroidManifest.xml`: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.

## Testing
Run one existing test file first to confirm the harness (`./gradlew :app:testDebugUnitTest`), then:
- `SuggestZoneScheduleUseCaseTest` — table-driven vectors (normal day, overnight sleep, tiny window; 30-min margins, equal split), the future shared iOS vector format.
- `ScheduleFirstTaskUseCaseTest` — task lands at the start of the first enabled zone with the preferred length; skips disabled zones.
- `ValidateDayBoundsTest`, `ZoneEditRulesTest` — snapping, clamping, overnight, overlap flags.
- `OnboardingViewModelTest` — with the real `InMemoryOnboardingRepository` (NiA: test with real code, not mocks): step progression, skip defaults (07:00/23:00, 60 min, suggested zones), continue-gating, first-task success + failure fallback, completion event.

## Verification (when implementation happens)
1. `./gradlew :feature:onboarding:impl:testDebugUnitTest` and `./gradlew detekt`.
2. `./gradlew assembleDebug`, install, walk the full flow on an API 33+ emulator: skip-everything path lands on home with defaults; full path shows live preview updates, zone drag-editing, task landing celebration, primer-before-OS-prompt; deny notifications → app still usable.
3. Overnight case: wake 22:00 / sleep 06:00 renders correctly on the preview.

## Out of scope (deferred with the mocked data layer)
Room persistence, WorkManager sync, real AI task capture (Epic 3/4), APNG mascot animation, Epic 2 settings surfaces that re-expose these values, and the removed difficulty-order step.
