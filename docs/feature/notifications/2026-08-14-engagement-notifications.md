# AWAN-163 — Engagement notifications, a live-notification "Stop", and an honest onboarding step

**Jira**: [AWAN-163 — Local Notifications](https://ezdo.atlassian.net/browse/AWAN-163) (Story, Android, In Progress)
**Date**: 2026-08-14
**Branch**: `feature/AWAN-163-local-notifications` — one commit per phase
**Feature**: Notifications (`:core:model`, `:core:domain`, `:core:data`, `:core:datastore(-proto)`,
`:core:notifications`, `:feature:profile`, `:feature:onboarding`)
**Follows**: [2026-08-13-local-notifications-migration.md](2026-08-13-local-notifications-migration.md)

## Context

AWAN-163 replaced push with a local notification engine (`:core:notifications`): one idempotent
`SessionNotificationScheduler.rescheduleAll()` rebuilds everything from Room, holds two exact alarms,
and posts three session events — `Reminder`, `Live`, `Ended`. It works, but the app only speaks when a
session is about to start, is running, or has just ended. Nothing happens when the user quietly drops
off: a day with no completed session passes in silence, a session that ended with no reply is never
mentioned again, and an empty day never prompts anyone to plan it.

Three problems to fix:

1. **The app goes quiet exactly when it should speak up.** Add a streak-loss warning before the day
   ends, a friendly follow-up ~90 min after a session ended with no reply, and a nudge to plan an
   empty day.
2. **The running-session notification is a one-way door.** It is `setOngoing(true)`, so it cannot be
   swiped away, and its only button ends *and completes* the session. A user who just wants it off
   their screen has no option that does not lie about what they did.
3. **The onboarding notifications step describes an app that does not exist.** Its bullets promise
   "only when a block starts" (wrong — there is a reminder before, a live update during, and an end
   prompt after) and "only when a plan needs a tiny fix" (there is no such notification at all). It
   was written as a blueprint before the engine existed.

Plus a standing rule for CLAUDE.md: **no notification ships without its own switch.**

---

## Design

The existing engine already does the hard part. Everything below is new *events* fed through the same
`plan → isDue → post → cancelStale → set alarm` loop, so reconciliation, the offline action queue,
Doze handling and timezone re-timing come for free.

Two new facts the planner needs, both local-only:

| Fact | Source | Fallback |
|---|---|---|
| When the day ends / starts | `ObserveProfileUseCase().first()?.preferences?.sleepTime` / `wakeupTime` (Room-backed, `flowOn(io)`, no network) | `DayBounds.Default` — 07:00 / 23:00. Required: onboarding never writes this row, and `CalendarLocalDataSourceImpl` can write `""`. |
| Is today's schedule actually known | new `SessionNotificationRepository.isScheduleKnown(date)` → existing `CachedScheduleDateDao.isDateCached(date)` | `false` → post nothing. Distinguishes "empty day" from "never synced", so a cold install is not nagged. |

"Has the user kept today's streak?" is **not** asked of the server. `GamificationRepository.getActivityDates`
is network-only and `UserEntity` caches no `lastActivityDate`, and CLAUDE.md forbids computing streak
client-side. The local proxy is "any session with status `COMPLETED` dated today" — already present in
the sessions the scheduler reads, since `UPCOMING_SESSIONS_QUERY` has no status filter. **Copy must
therefore say "you haven't finished anything today", never assert a streak number.**

### New events

`SessionNotificationEvent` currently requires `val session: UpcomingSession`, which day-scoped events
do not have. Split the interface — a shared base carrying only `at`, with the existing three staying
exactly as they are:

```kotlin
sealed interface AwanNotificationEvent { val at: LocalDateTime }

sealed interface SessionNotificationEvent : AwanNotificationEvent {
    val session: UpcomingSession
    data class Reminder(...)  // unchanged
    data class Live(...)      // unchanged
    data class Ended(...)     // unchanged
    /** end + followUpDelayMinutes, still not completed. */
    data class FollowUp(override val at: LocalDateTime, override val session: UpcomingSession) : …
}

sealed interface DayNotificationEvent : AwanNotificationEvent {
    val date: LocalDate
    data class StreakRisk(override val at: LocalDateTime, override val date: LocalDate) : …
    /** Morning summary, or a prompt to plan the day when it is empty. */
    data class DailyBrief(override val at: LocalDateTime, override val date: LocalDate) : …
}
```

| Event | Fires at | Only when | Grace | Actions |
|---|---|---|---|---|
| `FollowUp` | `session.end + followUpDelayMinutes` (default 90; 30/60/120 configurable) | status still `SCHEDULED`/`IN_PROGRESS`/`MISSED` | 60 min | Mark complete · Reschedule |
| `StreakRisk` | `dayEnd − 2h` | no `COMPLETED` session today **and** no session currently running | 45 min | tap → Home |
| `DailyBrief` | `wake + 1h`, plus `wake + ⌊waking/2⌋` **only if the day is still empty** | `isScheduleKnown(today)` | 30 min | tap → Home |

`DailyBrief` is one event with two bodies, decided from the same session list the planner already has:

- **Empty day** → "Nothing planned today — two minutes now and the day plans itself."
- **Day with sessions** → "3 blocks today · Deep Work is first, at 09:30."

The midday repeat is what "from time to time" buys without persisting anything, and it only fires
while the day is genuinely empty — once a session exists, the morning summary is the whole story.
Both moments have a short grace, so dismissing one does not make it reappear for long. One switch
covers both, since it is one event.

**At most one `FollowUp` posts at a time** — the most recent session's. Three missed sessions must not
mean three notifications. Collapsed in the planner (pure, testable), not in the scheduler.

`StreakRisk` must handle an overnight day (`sleepTime <= wakeupTime`, e.g. 01:00): the day's end is on
the *following* calendar date, or the alarm sits in the past all day and never fires.

### The live notification's two buttons

Today one button, `STOP_HERE`, which moves the end to now **and** completes. Becomes:

| Button | Enum | Behaviour |
|---|---|---|
| **Complete now** | `COMPLETE_NOW` (renamed from `STOP_HERE`; same worker path) | unchanged: `MoveSessionUseCase(id, start, now)` then `CompleteSessionUseCase(id)` |
| **Stop** | `DISMISS_LIVE` (new) | cancels the notification, no network call, no status change, no time change |

`DISMISS_LIVE` needs state: the `Live` event stays due for the whole session, so `rescheduleAll()` —
which runs on every session write and every app foreground — would re-post it seconds later.
`SessionNotificationScheduler` already knows about `replaceSessionsForDates` wiping Room, so this
cannot live beside the session. Store it in a small `@Singleton LiveNotificationDismissals` inside
`:core:notifications`, backed by `SharedPreferences`.

**The record is `(sessionId, start, end)`, not `(sessionId, until)`** — the dismissal is scoped to the
session *as it was when the user dismissed it*:

```kotlin
// key   = sessionId
// value = "startIso|endIso"
```

Suppression applies only while the stored window still matches the session in Room. Move, snooze, or
reschedule that session and the window no longer matches, so the dismissal is void and the live
notification comes back — which is right: a rescheduled session is a new thing to be told about, and
the alternative silently swallows the live update for a block the user just re-planned. Fed into the
planner as a map, so the rule stays pure and testable.

**Records are deleted once their `end` has passed** — a dismissal is only meaningful while the session
it names is still running. `rescheduleAll()` runs on every alarm, every session write and every app
foreground, and each run reads this map, so pruning on read is enough to keep it swept: every read
drops the expired entries and writes back the survivors. Without it the map is append-only and grows
one dead entry per dismissed session forever, which is a `SharedPreferences` file that only ever gets
bigger and a lookup that has to walk it.

```kotlin
// ponytail: one flat map in SharedPreferences, pruned on every read so expired windows never
// accumulate. It only ever holds sessions running right now, which the conflict engine keeps to
// roughly one. Upgrade path if this grows: move it into the proto DataStore beside the preferences.
```

A test covers the pruning directly: dismiss a session, advance past its end, read the map, assert it
is empty and that the backing file no longer holds the key.

Renaming the enum constant means a notification posted by the *previous* build has a button whose
name no longer parses — `NotificationActionReceiver` already logs and ignores that, and
`MY_PACKAGE_REPLACED` triggers a reschedule that re-posts. Acceptable.

---

## Phases

One commit per phase, on the current `feature/AWAN-163-local-notifications` branch, each message
prefixed `AWAN-163:`.

### Phase 1 — preferences and the two new local inputs

- **`:core:model`** [`NotificationPreferences.kt`](core/model/src/main/kotlin/com/awan/app/core/model/NotificationPreferences.kt):
  add `sessionFollowUpEnabled`, `streakReminderEnabled`, `dailyBriefEnabled` (all default `true`),
  `followUpDelayMinutes = DEFAULT_FOLLOW_UP_MINUTES (90)`, and `FOLLOW_UP_CHOICES = listOf(30, 60, 120)`.
- **`user_preferences.proto`** — tags 14–16 as **negated** bools (`session_follow_up_disabled`,
  `streak_reminder_disabled`, `daily_brief_disabled`), tag 17 `int32 session_follow_up_minutes`
  (0 ⇒ default). Tags 1–13 are frozen; follow the file's own header rules.
- **`AwanPreferencesDataSource.kt`** — extend the two existing mapper blocks (`toNotificationPreferences`
  and `setNotificationPreferences`). `UserPreferencesData` holds the whole object, so nothing else moves.
- **`SessionNotificationRepository`** (`:core:domain`) + impl (`:core:data`): add
  `suspend fun isScheduleKnown(date: LocalDate): Boolean` → `CachedScheduleDateDao.isDateCached`.
  New use case `IsScheduleKnownUseCase` beside the existing four.
- No new use case for day bounds — `ObserveProfileUseCase` already returns Room-backed
  `preferences.wakeupTime` / `sleepTime`.

### Phase 2 — planner: new events, pure rules, tests

- New `AwanNotificationEvent` / `DayNotificationEvent` split in
  [`model/SessionNotificationEvent.kt`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/model/SessionNotificationEvent.kt).
- [`SessionNotificationPlanner.kt`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/SessionNotificationPlanner.kt)
  `plan(...)` gains a `NotificationDayContext` param (`wake`, `dayEnd`, `scheduleKnown`, `today`) and
  `dismissedLive: Map<String, ClosedRange<LocalDateTime>>` (the stored window per session).
  `isDue` gains the three new branches; `nextUserEventAfter` picks up the day events automatically
  (they are not mid-session ticks).
- [`NotificationIds.kt`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/NotificationIds.kt):
  `followUp(sessionId)`, plus date-derived `streakRisk(date)` / `dailyBrief(date)`. All keep the
  `awan-session` tag so `cancelStale` sweeps them the same way.
- Tests in [`SessionNotificationPlannerTest.kt`](core/notifications/src/test/java/com/awan/app/core/notifications/SessionNotificationPlannerTest.kt),
  matching its style (plain JUnit 4, hand-built fixtures, no mocks — only JUnit + coroutines-test are on
  the classpath). Cover: follow-up skipped once completed; only the latest follow-up survives; streak
  risk suppressed by a completed session and by a running one; overnight `sleepTime`; `DailyBrief`
  silent when `scheduleKnown == false`; midday `DailyBrief` only on an empty day; a dismissed live
  session plans no `Live`, **and plans one again once its start or end changes**.

### Phase 3 — posting, channels, strings, and the live "Stop" button

- **New channel** `nudges` (IMPORTANCE_DEFAULT) in
  [`AwanNotificationChannels.kt`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/AwanNotificationChannels.kt)
  for `StreakRisk` + `DailyBrief`. `FollowUp` reuses `session_end` — it *is* a finished-session prompt.
- [`SessionNotificationPoster.kt`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/SessionNotificationPoster.kt):
  `postFollowUp`, `postStreakRisk`, `postDailyBrief` (empty vs. summary body) via the existing
  `notify(...)` helper (system notification + in-app toast when foregrounded). Live notification gets
  the second action.
- `NotificationAction`: `STOP_HERE` → `COMPLETE_NOW`; add `DISMISS_LIVE`.
  [`NotificationActionReceiver`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/receiver/NotificationActionReceiver.kt)
  handles `DISMISS_LIVE` itself — record `(sessionId, start, end)` in `LiveNotificationDismissals` from
  the extras the action intent already carries (`EXTRA_START_ISO` / `EXTRA_END_ISO`), cancel,
  `rescheduleAll()`, **no worker enqueued**.
- New `LiveNotificationDismissals.kt` (`@Singleton`, SharedPreferences, self-pruning, window-scoped).
- [`SessionNotificationScheduler.rescheduleAll()`](core/notifications/src/main/kotlin/com/awan/app/core/notifications/SessionNotificationScheduler.kt):
  read profile + `isScheduleKnown` + dismissals, build the `NotificationDayContext`, pass both to the
  planner. Everything downstream is unchanged.
- Strings in `core/notifications/src/main/res/values/strings.xml` **and** `values-ar/` — new keys plus
  a reworded `notifications_action_stop_here` → "Complete now" / "إنهاء الآن" and a new
  `notifications_action_dismiss` → "Stop" / "إيقاف". Arabic plurals need all six forms; `lint` treats
  `MissingTranslation` as an error.

### Phase 4 — notification settings

[`NotificationSettingsScreen.kt`](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/ui/NotificationSettingsScreen.kt)
+ [`NotificationSettingsAction.kt`](feature/profile/impl/src/main/java/com/awan/feature/profile/impl/presentation/NotificationSettingsAction.kt)
+ ViewModel. Reuse `SwitchRow` / `AwanChoiceRow` exactly as the existing rows do.

- **SESSIONS**: + "If you don't reply after a session" switch.
- **TIMING**: + "Follow up after (min)" choice row `30 / 60 / 120`, disabled unless follow-up is on.
- **NUDGES** (new section): "Streak reminder before your day ends", and "Daily brief" — one switch,
  subtitle "A morning summary, and a nudge if the day is empty."
- Strings `profile_notifications_*` in both `values/` and `values-ar/`. Keep both files in sync.

The ViewModel needs no new use case — `SetNotificationPreferencesUseCase` already takes the whole
object, and `SessionNotificationStarter` already `combine`s the preferences flow, so a toggle retimes
the alarms with no extra wiring.

### Phase 5 — honest onboarding step

[`NotificationsStep.kt`](feature/onboarding/impl/src/main/java/com/awan/feature/onboarding/impl/ui/steps/NotificationsStep.kt)
keeps its shape (`StepBody` + `CascadeItem` 0–3, `CenteredHeadline`, mock card, bullets, permission
note). Only content changes — no new step, no flow change, no `OnboardingViewModel` change.

- Replace the single `SampleNotification` with a **stack of three** mock cards, reusing its existing
  38dp emoji-box + two-line-column shape: a reminder ("Deep Work starts in 10 min"), a running session
  with a progress bar, and a gentle follow-up ("How did Deep Work go?"). Borrow the nested
  bordered-surface pattern from `LandedTaskCard` in
  [`FirstTaskStep.kt`](feature/onboarding/impl/src/main/java/com/awan/feature/onboarding/impl/ui/steps/FirstTaskStep.kt).
- Rewrite all three bullets to match what actually ships: a nudge **before** a session starts, a live
  countdown while it runs, and a friendly check-in if a session slips or the day would end empty.
  Delete the "plan needs a tiny fix" bullet — there is no such notification.
- Point the last bullet at the real control: *Profile → Notifications*, "every one of these has its own
  switch".
- Rewrite `onboarding_notifications_preview_body` (currently "starts in 5 min"; the default lead is 10).
- Add the missing `@Preview(locale = "ar")` for this step in `OnboardingPreviews.kt`, matching Zones
  and Day bounds.
- All copy in `feature/onboarding/impl/src/main/res/values/strings.xml` **and** `values-ar/`.

### Phase 6 — the rule, in writing

- **CLAUDE.md**, new bullet under `### Project-specific constraints`:

  > - **Every notification needs its own switch.** Any notification added or changed — a new kind, a new
  >   trigger, a new channel — ships in the same change with a toggle on the notification settings screen
  >   (`feature/profile/impl/.../ui/NotificationSettingsScreen.kt`), a field on `NotificationPreferences`
  >   (`:core:model`), and a proto field stored **negated** so it defaults to on. A notification the user
  >   cannot turn off on its own is a bug, not a preference gap.

- New plan doc `docs/feature/notifications/2026-08-14-engagement-notifications.md` (this file's content),
  per the immutable-dated-plan rule; implementation notes appended when the work lands.

---

## Verification

1. `./gradlew assembleDebug`
2. `./gradlew testDebugUnitTest` — existing 461 stay green, plus the new planner cases.
3. `./gradlew lint` — `abortOnError = true`; `HardcodedText` and `MissingTranslation` are errors, so a
   missed Arabic string fails the build.
4. Emulator, the parts unit tests cannot prove:
   - Seed a session that ended 95 min ago, still `SCHEDULED` → follow-up posts once, with **Mark
     complete** and **Reschedule**. Seed three such sessions → **exactly one** notification.
   - Set the profile `sleepTime` to two hours out with nothing completed today → streak warning fires;
     complete a session → it disappears on the next reconcile. Set `sleepTime` to `01:00:00` (overnight)
     → the alarm lands tonight, not this morning.
   - Empty, synced day → the brief fires at wake+1h asking you to plan it, and again at midday; add a
     session → the midday repeat stops and the morning body becomes the summary. Clear
     `cached_schedule_dates` → nothing fires.
   - Running session: **Complete now** ends and completes it (unchanged); **Stop** clears the
     notification and it stays gone across an app foreground, a session write, and a force-stop +
     relaunch — then **move that same session and the live notification comes back**, and the *next*
     session's live notification appears normally.
   - Every new switch off → that notification stops, the others keep working.
   - Onboarding step in English and Arabic (RTL), including the permanently-denied state.
5. `connectedDebugAndroidTest` is out of scope — there are no instrumented tests for this area.

## Deliberately not doing

- **No goal-deadline notifications** — still AWAN-156 territory, still sessions-only here.
- **No weekly recap.** Easy to add later from the same Room data; you passed on it.
- **No daily-wheel reminder.** `GetWheelConfig` is network-backed, so it cannot fire offline and would
  contradict the server; the wheel stays on the push/rewards path.
- **No server-side streak check.** `getActivityDates` is network-only, so the warning is driven by
  local completed-sessions-today and worded accordingly.
