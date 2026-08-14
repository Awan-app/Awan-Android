# AWAN-163 — Migrate from push to local notifications

**Jira**: [AWAN-163 — Local Notifications](https://ezdo.atlassian.net/browse/AWAN-163) (Story, Android, In Progress)
**Date**: 2026-08-13
**Branch**: `feature/AWAN-163-local-notifications` — one PR, one commit per phase
**Feature**: Notifications (`:core:database`, `:core:domain`, `:core:data`, `:core:datastore(-proto)`,
new `:core:notifications`, `:feature:profile`, `:feature:home`, `:app`)

## Context

Session notifications today are 100% server push. `AwanFirebaseMessagingService`
([app/.../notification/AwanFirebaseMessagingService.kt](app/src/main/java/com/awan/app/notification/AwanFirebaseMessagingService.kt))
is the only notification code in the repo: one channel, no actions, no scheduling, and the
`sessionId`/`type` extras it writes into the tap `PendingIntent` are never read by anything. Offline,
the user gets nothing at all — which is the whole problem, since the schedule itself is offline-first
and Room already knows every session time.

**Outcome:** the device schedules its own session notifications from Room — a reminder before each
session, a Live Update (Android's Dynamic-Island equivalent) while it runs, and an end prompt — each
with actions that call the real endpoints. Push survives only for the daily-wheel/reward payloads the
backend sends, and FCM token registration is untouched.

Decisions taken with the user:

- **`USE_EXACT_ALARM`** (auto-granted, no runtime prompt), with `SCHEDULE_EXACT_ALARM maxSdkVersion=32`
  for API 31–32. Awan is a scheduling app, so the Play declaration is honest — but it is a policy
  declaration you own on the Console.
- **Foreground = both** the in-app `AwanTopToast` and the system notification.
- **Actions queue through WorkManager** with a `CONNECTED` constraint, so a tap offline still lands.
- One branch, one PR, **separate commits per phase**.

### Two doc/reality mismatches found while exploring (not blockers, but do not trust the docs here)

- CLAUDE.md says `AwanDatabase` is at **version 4 with real migrations**. It is actually at
  **version 1** with `fallbackToDestructiveMigration(dropAllTables = true)`
  ([core/database/.../di/DatabaseModule.kt](core/database/src/main/kotlin/com/awan/app/core/database/di/DatabaseModule.kt)) —
  commit `48fb207e` reverted it. **This plan needs no schema change at all**, so it does not matter
  here, but the CLAUDE.md line should be corrected separately.
- `docs/feature/backend/AWAN_API_DOCUMENTATION.md` still documents `PATCH /v1/sessions/{id}/status`.
  The live contract is the dedicated `POST .../complete|uncomplete|cancel` endpoints already wired
  through `HomeRepository`.

---

## Design: one alarm, one reconcile function

The naive approach — an alarm per session per notification kind — needs a persistent ledger to know
what to cancel, and that ledger cannot survive `SessionDao.replaceSessionsForDates`, which **wipes and
rewrites whole date ranges** on every sync
([OfflineSyncCoordinator.kt:154](core/data/src/main/kotlin/com/awan/app/core/data/sync/OfflineSyncCoordinator.kt:154)).
Six different call sites write sessions to Room.

So instead: **hold exactly one exact alarm at a time — the next thing that has to happen — and derive
everything from Room.** One idempotent `rescheduleAll()` is the entire engine:

1. Read notification prefs.
2. One-shot read of sessions for `[today, tomorrow]` (joined with task titles).
3. `SessionNotificationPlanner.plan(sessions, prefs, now)` → time-ordered events
   (`Reminder`, `LiveTick`, `End`), pure Kotlin, no Android.
4. Post every event that is **due now** (within its grace window — so a reboot at noon does not
   replay this morning's reminders).
5. **Cancel** every posted notification that the plan no longer contains — walk
   `NotificationManager.activeNotifications` (API 23+, minSdk is 26) and drop any whose session was
   deleted, completed, cancelled, or moved. This is what makes "a changed session updates its
   notification" true without storing anything.
6. Set **one** `setExactAndAllowWhileIdle` alarm for the earliest remaining future event.

`rescheduleAll()` runs on: alarm fire, boot, app foreground, prefs change, timezone/time change, and
**any write to the sessions table**. That last one is the root-cause hook — a single collector on
`SessionDao`'s range Flow covers all six mutation sites, instead of a `reschedule()` call sprinkled at
each of them and forgotten at the seventh.

No new Room columns, no ledger table, no DB version bump.

---

## Phase 1 — data + preferences

**Commit**: `AWAN-163: add upcoming-session query and notification preferences`

**`:core:database`** — [SessionDao.kt](core/database/src/main/kotlin/com/awan/app/core/database/dao/SessionDao.kt):
add a projection joining `sessions` → `tasks` (verified: table `tasks`, column `title`), so titles do
not cost an N+1 of `getTask` per session. Both a `Flow` and a `suspend` variant of the same query —
the collector needs one, the receiver needs the other.

```kotlin
data class UpcomingSessionRow(
    val id: String, val taskId: String, val title: String,
    val date: String, val startTime: String, val endTime: String,
    val status: String, val zoneId: String?,
)

@Query("""
    SELECT s.id, s.taskId, t.title, s.date, s.startTime, s.endTime, s.status, s.zoneId
    FROM sessions s INNER JOIN tasks t ON t.id = s.taskId
    WHERE s.date BETWEEN :startDate AND :endDate
    ORDER BY s.date ASC, s.startTime ASC
""")
fun observeUpcomingSessions(startDate: String, endDate: String): Flow<List<UpcomingSessionRow>>
```

**`:core:domain`** — new `notifications/` vertical, shaped like `core/domain/auth/`:
`model/UpcomingSession.kt` (assembles `LocalDateTime` from the split `date`/`startTime` strings —
`SessionEntity` has no epoch column), `model/NotificationPreferences.kt`,
`repository/SessionNotificationRepository.kt` (`observeUpcoming` / `getUpcoming`), and use cases
`ObserveUpcomingSessionsUseCase`, `GetUpcomingSessionsUseCase`, `GetNotificationPreferencesUseCase`,
`SetNotificationPreferencesUseCase` (one setter taking the whole object — six single-field use cases
would be noise).

**`:core:data`** — `notifications/repository/SessionNotificationRepositoryImpl.kt` + a mapper, bound in
[DataModule.kt](core/data/src/main/kotlin/com/awan/app/core/data/di/DataModule.kt) alongside the
existing bindings.

**Preferences** — [user_preferences.proto](core/datastore-proto/src/main/proto/user_preferences.proto),
new tags 8+ (1–7 are frozen; the file's own header states the rules). Note proto3 bools default to
`false` and these features must default to **on**, so store them **negated** and flip once in the
mapper — every consumer then reads a positive `…Enabled`:

```proto
bool session_reminders_disabled       = 8;
bool session_live_activity_disabled   = 9;
bool session_end_notification_disabled = 10;
bool reward_notifications_disabled    = 11;
int32 session_reminder_lead_minutes   = 12;  // 0 => default 10
int32 session_snooze_minutes          = 13;  // 0 => default 10
```

Then the four files that must move in lockstep:
[UserPreferencesData.kt](core/datastore/src/main/kotlin/com/awan/app/core/datastore/model/UserPreferencesData.kt),
[UserPreferencesDataSource.kt](core/datastore/src/main/kotlin/com/awan/app/core/datastore/UserPreferencesDataSource.kt),
[AwanPreferencesDataSource.kt](core/datastore/src/main/kotlin/com/awan/app/core/datastore/AwanPreferencesDataSource.kt),
plus `UserData` / [UserDataRepository](core/domain/src/main/kotlin/com/awan/app/core/domain/profile/repository/UserDataRepository.kt)
and its impl.

---

## Phase 2 — `:core:notifications` engine

**Commit**: `AWAN-163: add core:notifications module with alarm scheduling engine`

New module in [settings.gradle.kts](settings.gradle.kts), `build.gradle.kts` applying
`awan.android.library` + `awan.android.hilt` + `awan.android.workmanager`. Depends on `:core:domain`,
`:core:common`, `:core:design-system` (for `AwanToastManager`). **Not** on `:core:data` (cycle) and
**not** on `:app` — the tap intent uses `packageManager.getLaunchIntentForPackage(packageName)`, so no
`MainActivity` reference is needed.

| File | Role |
|---|---|
| `AwanNotificationChannels.kt` | Creates 4 channels on first use |
| `SessionNotificationPlanner.kt` | **Pure** `(sessions, prefs, now) -> List<Event>`. Unit-tested. |
| `SessionNotificationScheduler.kt` | `@Singleton`, `rescheduleAll()` — the 6 steps above |
| `SessionNotificationPoster.kt` | Builds/posts/cancels; reconciles `activeNotifications` |
| `receiver/SessionAlarmReceiver.kt` | `goAsync()` → `rescheduleAll()` |
| `receiver/BootCompletedReceiver.kt` | `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` → reschedule worker |
| `work/NotificationRescheduleWorker.kt` | `@HiltWorker`, copies [SyncWorker](core/data/src/main/kotlin/com/awan/app/core/data/sync/SyncWorker.kt) |
| `SessionNotificationStarter.kt` | `@Singleton`; collects `observeUpcomingSessionsUseCase` on `@ApplicationScope`, calls `rescheduleAll()` on every emission |

`TIMEZONE_CHANGED` matters more than it looks: sessions are stored as local `date` + `startTime`
strings, so a flight changes every absolute trigger time.

Channels (localized names/descriptions in the module's own `values/` **and** `values-ar/`, keys
prefixed `notifications_`):

| id | importance | contents |
|---|---|---|
| `session_reminders` | HIGH | "Deep Work starts in 10 min" · Snooze, Reschedule |
| `session_live` | LOW | ongoing Live Update · Stop Here |
| `session_end` | HIGH | "Did you finish Deep Work?" · Mark complete, Reschedule |
| `rewards` | HIGH | daily wheel / reward pushes from FCM |

Manifest (merged from this module — receivers all `exported="false"`):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Wire the starter from [AwanApplication.kt](app/src/main/java/com/awan/app/AwanApplication.kt) (which
already implements `Configuration.Provider` for `HiltWorkerFactory`), and add a foreground catch-up
`rescheduleAll()` in [MainActivity.kt](app/src/main/java/com/awan/app/MainActivity.kt)'s existing
`repeatOnLifecycle(STARTED)` block — the same "never assume the background job ran" rule CLAUDE.md
sets for the Nightly Sweep.

**Test**: `SessionNotificationPlannerTest` — JUnit 4 + hand-written fakes, matching
[HomeRepositoryImplTest](core/data/src/test/java/com/awan/app/core/data/home/HomeRepositoryImplTest.kt).
No MockK/Turbine in this project; do not add them. Cases: lead time honoured; a `COMPLETED` /
`CANCELLED` session plans nothing; a past reminder outside the grace window is not replayed; a session
spanning `now` plans a live tick; disabled prefs remove exactly their own events.

---

## Phase 3 — reminders, end notifications, actions, deep links

**Commit**: `AWAN-163: post session reminder and end notifications with actions`

`NotificationActionReceiver` cancels the notification, calls `rescheduleAll()`, and enqueues
`NotificationActionWorker` — unique work `notif-action-$sessionId`, `ExistingWorkPolicy.REPLACE`,
`NetworkType.CONNECTED`, exponential backoff. That is what makes an offline tap land later instead of
failing.

| Action | Calls (all existing use cases in `core/domain/home/usecase/`) |
|---|---|
| Snooze | `MoveSessionUseCase(id, start + snooze, end + snooze)` |
| Mark complete | `CompleteSessionUseCase(id)` |
| Stop Here | `MoveSessionUseCase(id, start, now)` **then** `CompleteSessionUseCase(id)` |
| Reschedule / tap | launch intent, no worker |

`now` is captured **at tap time** and passed in the worker's input data — not read when the worker
finally runs, which could be hours later offline.

Deep link, fixing the dead-end the current push path has:

- [app/AndroidManifest.xml](app/src/main/AndroidManifest.xml) — `MainActivity` gets
  `android:launchMode="singleTop"`; add `onNewIntent` (without both, taps leak duplicate Activities).
- `HomeRoute` ([feature/home/api/.../HomeRoute.kt](feature/home/api/src/main/java/com/awan/feature/home/api/HomeRoute.kt))
  gains `sessionId: String? = null`. Note its existing `date` param is currently **ignored** by
  [HomeEntryProvider.kt](feature/home/impl/src/main/java/com/awan/feature/home/impl/navigation/HomeEntryProvider.kt) —
  wire both through to `HomeScreen`, which calls the existing
  `HomeViewModel.onSessionClicked(sessionId)` to open the detail sheet where rescheduling already lives.

Foreground: post the system notification **and** `AwanToastManager.showToast(...)`, reusing
[AwanToastManager](core/design-system/src/main/java/com/awan/app/core/designsystem/AwanToastManager.kt) +
`AwanTopToastHost` exactly as the FCM service does today. The live activity is always a system
notification only — a toast that re-fires every 60s would be unusable.

---

## Phase 4 — live activity

**Commit**: `AWAN-163: add session live activity notification`

Android's counterpart to a Dynamic Island Live Activity is the API 36 **Live Update**. Verified
present in the already-resolved `androidx.core:core-ktx 1.19.0` (`NotificationCompat$ProgressStyle`
ships `Api36Impl`/`Api37Impl`, and `Builder` has `setRequestPromotedOngoing` / `setShortCriticalText`)
— **no new dependency, and `NotificationCompat` degrades to a plain ongoing notification below 36.**

```kotlin
NotificationCompat.Builder(ctx, CHANNEL_SESSION_LIVE)
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setWhen(endMillis).setUsesChronometer(true).setChronometerCountDown(true)
    .setShortCriticalText(remainingShort)      // status-bar chip
    .setRequestPromotedOngoing(true)           // Live Update / chip + AOD
    .setStyle(
        NotificationCompat.ProgressStyle()
            .setProgress(elapsedMinutes)
            .addProgressSegment(NotificationCompat.ProgressStyle.Segment(totalMinutes))
    )
    .addAction(stopHereAction)
```

`setUsesChronometer` + `setChronometerCountDown` means **the system renders the countdown itself**, so
the remaining time stays correct even when no tick runs. Ticks (a `now + 60s` event folded into the
same single-alarm plan — not a second alarm) only advance the progress bar.

```kotlin
// ponytail: setExactAndAllowWhileIdle is throttled to ~1/9min in Doze, so the progress bar coarsens
// with the screen off. The chronometer countdown stays exact regardless. Upgrade path if the bar
// must be smooth in Doze: a foreground service for the session's duration.
```

---

## Phase 5 — remove push delivery

**Commit**: `AWAN-163: remove push notification delivery, keep FCM token registration`

In [AwanFirebaseMessagingService.kt](app/src/main/java/com/awan/app/notification/AwanFirebaseMessagingService.kt):

- **Delete** `showSystemNotification`, `createNotificationChannelIfNeeded`, `CHANNEL_ID`,
  `EXTRA_*`, and the silent `catch (e: SecurityException) {}`.
- **Keep** `onNewToken` and the whole device-token stack untouched —
  `DeviceTokenRepository`, `DeviceTokenApiService` (`POST/DELETE v1/device-tokens`), and the
  register/remove calls in `AuthRepositoryImpl`. The backend keeps being able to reach the device.
- `onMessageReceived` **drops** payloads whose `type` is now locally owned
  (session reminder / start / end) so the user never gets both, and routes the rest — wheel, rewards,
  general — through `SessionNotificationPoster` on the `rewards` channel, plus the foreground toast.
- Delete `app_notification_channel_name` / `app_notification_channel_description` from
  `app/src/main/res/values/strings.xml` **and** `values-ar/strings.xml` (channel strings now live in
  `:core:notifications`). `lint` has `MissingTranslation` as an error, so both must move together.

---

## Phase 6 — notification settings screen

**Commit**: `AWAN-163: add notification settings screen`

The profile row **already exists and already fires** `onSettingsClick("notifications")`
([SettingsCard.kt:27](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/ui/components/SettingsCard.kt:27)) —
it just falls through today. Follow the existing **McpSettings** sub-screen vertical exactly:

1. `feature/profile/api/.../NotificationSettingsRoute.kt` — `@Serializable data object … : Route`.
2. [ProfileEntryProvider.kt](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/navigation/ProfileEntryProvider.kt) —
   new param + `entry<NotificationSettingsRoute>`.
3. [ProfileRouteScreen.kt:27](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/navigation/ProfileRouteScreen.kt:27) —
   extend the `settingKey` branch with `"notifications"`.
4. [AwanApp.kt](app/src/main/java/com/awan/app/AwanApp.kt) — one more lambda on `profileEntry(...)`.
5. MVI quad + `ui/NotificationSettingsScreen.kt`, copying `McpSettingsScreen`'s
   `Scaffold` + `AwanBackButton` top bar. Rows are `PreferenceRow(onClick = null) { Switch(...) }` —
   the trailing-content slot already exists, same trick
   [AppearanceCard.kt](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/ui/components/AppearanceCard.kt)
   uses. There is no `AwanSwitch` in the design system; use Material3 `Switch` in the slot rather than
   adding a component for one screen.

Contents: a toggle per channel (reminders, live activity, end, rewards), reminder lead time
(5/10/15/30 min), snooze length (5/10/15 min), a row deep-linking to the **system** per-channel
settings, and a banner when `POST_NOTIFICATIONS` is denied — which also closes the existing gap that
the permission is only ever asked for inside onboarding
([OnboardingScreen.kt:63-88](feature/onboarding/impl/src/main/java/com/awan/feature/onboarding/impl/ui/OnboardingScreen.kt:63)),
with no way back if the user declined. Reuse that file's `findActivity()` / `openAppSettings()` shape.

Also fix the row itself: it passes `value = profile_enabled`, and `PreferenceRow` renders an
`ExpandMore` chevron whenever `value != null && onClick != null` — so it currently looks expandable
rather than navigable. Drop the `value`.

Strings go in `feature/profile/impl/src/main/res/values/strings.xml` **and** `values-ar/strings.xml`,
keys prefixed `profile_notifications_`. Both files are currently in sync at 225 lines; keep them so.
The ViewModel injects **use cases only** — no DataStore, no repository.

---

## Verification

1. `./gradlew assembleDebug`
2. `./gradlew testDebugUnitTest` — existing suites stay green, plus `SessionNotificationPlannerTest`.
3. `./gradlew lint` — `abortOnError = true`, `HardcodedText` and `MissingTranslation` are **errors**,
   so any missed Arabic string fails the build.
4. Device/emulator run — the parts unit tests cannot prove:
   - Create a session ~12 min out → reminder fires at T-10, in Arabic too (switch app language).
   - Tap **Snooze** with airplane mode on → notification clears, and the move lands once
     connectivity returns (`adb shell dumpsys jobscheduler` to confirm the worker is queued).
   - At start time the Live Update appears; confirm the status-bar chip on an API 36 emulator and a
     plain ongoing notification on an older one; the countdown decrements.
   - **Stop Here** → session ends at the tap time and is marked complete.
   - Delete a session from Home while its reminder is pending → the pending notification and its alarm
     both disappear.
   - Move a session → the reminder retimes.
   - `adb reboot` with a session scheduled after boot → the reminder still fires.
   - Change the device timezone → triggers retime.
   - Toggle each switch off in settings → that notification stops, the others keep working.

`./gradlew connectedDebugAndroidTest` is not part of this — there are no instrumented tests for this
area and writing them is not in scope.

## Risks / deliberately not doing

- **`USE_EXACT_ALARM` is a Play Console policy declaration.** Justified for a scheduling app, but it
  is on the submission, not just in the manifest.
- **Live Update chip is API 36+.** Below that it is a normal ongoing notification with a progress bar.
  There is no Dynamic-Island equivalent on older Android; nothing can be done about that.
- **No goal-deadline notifications.** [AWAN-156](https://ezdo.atlassian.net/browse/AWAN-156) pairs
  sessions with goal deadlines on iOS; this plan is sessions only.
- **No backend change.** If the server keeps sending session pushes they are dropped client-side —
  worth turning off server-side later, but not required for this to be correct.
- **Reward/wheel notifications stay push-only**, per your call to revisit in a later phase.

---

## Implementation notes (what actually differed)

### Build / test status

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | BUILD SUCCESSFUL — 461 tests, 0 failures, including 19 new `SessionNotificationPlannerTest` cases |
| `./gradlew lint` | BUILD SUCCESSFUL (`HardcodedText` and `MissingTranslation` are errors) |
| Device run | Pixel-class emulator, **API 37** — see below |

### Verified on device

Seeded sessions straight into Room (the account was logged in, so the first attempt was
overwritten by `OfflineSyncCoordinator.syncScheduleRange` calling `replaceSessionsForDates` — a live
demonstration of exactly the hazard this design avoids; the seed had to be done with the radio off).

Confirmed working:

- All four channels created with the intended importances (`session_reminders` 4, `session_live` 2,
  `session_end` 4, `rewards` 3).
- Reminder posted with **Snooze** and **Reschedule**, body "Starts in 9 minutes · 10:51 PM – 11:51 PM",
  on a task with an Arabic title.
- Live activity posted as a genuine Live Update: dumped extras show
  `android.template=android.app.Notification$ProgressStyle` (not a compat fallback),
  `requestPromotedOngoing=true`, `shortCriticalText=30m`, `progress=2` of `progressMax=32` —
  correct for a session 2 minutes into 32 — plus `showChronometer`/`chronometerCountDown` and the
  **Stop here** action. The shade showed the countdown ticking (28:47 → 28:14) and the progress bar.
- Tapping **Stop here** offline cancelled the notification immediately and left
  `NotificationActionWorker` `ENQUEUED` with `required_network_type=1`, i.e. the offline queue works.
  *(That queued action was then deleted from the WorkManager DB rather than let it mutate a real
  session on the account.)*
- Reconciliation: when connectivity returned and the sync restored the real session times, both
  posted notifications disappeared on their own.
- One `RTC_WAKEUP` alarm at a time, tagged
  `*walarm*:com.awan.app/.core.notifications.receiver.SessionAlarmReceiver`.
- Merged manifest carries `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` capped at 32,
  `RECEIVE_BOOT_COMPLETED`, all three receivers, and `MainActivity` `launchMode="singleTop"`.
- Settings screen in English **and** Arabic (RTL correct); selecting 15 min wrote proto field 12 = 15
  and left fields 8–11 absent, confirming the negated-default scheme.

### Not verified

- **Reboot survival.** `BootCompletedReceiver` is registered and the reschedule worker exists, but no
  `adb reboot` cycle was run.
- **Timezone change** retiming.
- **A real Snooze / Mark complete against the backend** — the one queued action was deliberately
  removed rather than allowed to mutate real data. The endpoint calls themselves are the same
  `MoveSessionUseCase` / `CompleteSessionUseCase` the Home screen already uses.
- Doze behaviour of the one-minute tick.

### Deviations from the plan as written

1. **`NotificationPreferences` lives in `:core:model`, not `:core:domain`.** `:core:datastore` cannot
   see `:core:domain`, and mirroring six fields plus their defaults in both places invites drift.
2. **`:core:notifications` takes the Compose BOM but not the compose convention plugin.** It depends on
   `:core:design-system` only for `AwanToastManager`, and that module exposes Compose artifacts as
   `api` with BOM-governed versions; without the BOM they resolve to no version at all. The module has
   no composables, so turning on the Compose compiler for it would be pure cost.
3. **Deep link is passed beside the route, not on it.** `HomeRoute` did gain a `sessionId`, but the
   navigator matches top-level keys by equality — `HomeRoute(date, sessionId)` is not `HomeRoute()`, so
   navigating to it stacks a second Home on the open tab instead of switching tabs. The link is held in
   `:app` state and handed to `homeEntry`, surviving until Home composes (so a tap during splash still
   lands). `HomeRoute.date`, which was previously accepted and silently ignored, is now wired through.
4. **Reconciliation is by notification tag, not by session id.** The plan said to diff against the
   sessions Room knows about. That is wrong for the headline case: a deleted session contributes no id
   to compare against, so its notification would be stranded on screen forever. All session
   notifications now carry the tag `awan-session` and anything posted under it outside the current plan
   is cancelled; the reward notification uses `awan-remote` so it is never collateral.
5. **`SessionNotificationPlanner.nextAfter` was extracted** so the "next alarm" rule is pure and
   testable, instead of living inline in the Android-dependent scheduler.
6. **Channels are created at startup**, not lazily on first post, so the system notification settings
   page is not empty before the user's first session.
7. **`MinutesPreferenceRow` was added** to `:feature:profile:impl`. `PreferenceRow`'s trailing slot sits
   beside the title, which crushed "Remind me" onto three lines with four options — and Arabic is wider.

### Traps — the three bugs that only surfaced under review or on a device

These are all the same shape: the engine silently does nothing, or does something forever, and the
build stays green.

1. **A live event is never due if judged by `at`.** `at` on a `Live` event is the *next redraw*, always
   a tick in the future while the session runs. The other two events are judged `at <= now`; applying
   that uniformly meant the live notification never appeared **at all**. It is judged on
   `start <= now < end` instead. Easy to reintroduce by "tidying" `isDue` into one uniform rule.
2. **Picking the next alarm from "not due" instead of "future" loops the chain.** A plan legitimately
   contains past events (this morning's reminders, after a reboot at noon). One of those chosen as the
   next alarm fires immediately, is still not due, and reschedules itself — forever. `nextAfter`
   filters on `at.isAfter(now)`, and a test asserts the premise that stranded events really do exist.
3. **A finished session kept planning ticks.** Without the `now.isBefore(session.end)` guard, an ended
   session produces a tick a minute out forever: never due, always the next alarm, one exact alarm per
   minute for a session that is over.

### Other things worth knowing

- **The observation window is fixed at process start** (`today .. today+2`). A process alive for days
  stops reacting to edits outside it. Alarms stay correct regardless — `rescheduleAll` recomputes
  `today` every run — and the foreground catch-up covers real usage.
- **`USE_EXACT_ALARM` is a Play Console declaration**, not just a manifest line.
- **The API doc is stale**: `docs/feature/backend/AWAN_API_DOCUMENTATION.md` still documents
  `PATCH /v1/sessions/{id}/status`.
- **CLAUDE.md is stale about the database**: it says `AwanDatabase` is at version 4 with real
  migrations; it is at **version 1** with `fallbackToDestructiveMigration`. This change needed no
  schema change, so it did not matter here.
