# Notification fixes — rounding, re-posting, the end-of-day nudge, and snooze on the notification

Follows `2026-08-14-engagement-notifications.md`. A device run of that work surfaced four problems;
three are defects in the engine, one is a restructure of the settings screen.

1. A 5-minute reminder reads "Starts in 4 minutes".
2. Tapping a reminder brings it straight back.
3. The end-of-day nudge never appears, and nothing says why.
4. One decision is split across two settings rows, and the snooze length is a setting the user has to
   leave the notification to change.

---

## 1. Reminder minutes are truncated, not rounded

`SessionNotificationPoster.postReminder` uses `Duration.between(now, start).toMinutes()`, which
floors. `AlarmManager` always delivers at or *after* the trigger instant, so with a 5-minute lead the
remaining duration at post time is `4m 59.x s` → "4 minutes". Every lead value is one minute low,
every time.

The same truncation is in the Android 16 status-bar chip (`notifications_live_short_remaining`),
which is why the chip and the system chronometer beside it disagree, and why the chip reads `0m` for
the last full minute of a session.

Two pure helpers on `SessionNotificationPlanner` — already the Android-free home for the timing rules
and already unit-tested:

- `roundedMinutes(duration)` — `(seconds + 30) / 60`, for "starts in".
- `ceilMinutes(duration)` — `(seconds + 59) / 60`, for time remaining. Ceiling rather than rounding
  so the chip never claims `0m` while the chronometer still shows time.

## 2. A one-shot notification re-posts after it is dismissed

The only "already shown" check was `poster.postedSessionIds()`, which reads `activeNotifications` —
*still visible*, not *already delivered*. The reminder is `setAutoCancel(true)`, so tapping it removes
it and launches the app; the foreground `rescheduleAll()` then finds the reminder still due (it stays
due for its whole 5-minute grace) and no longer showing, and posts it again.

Not reminder-specific: swiping away an Ended, FollowUp, StreakRisk or DailyBrief notification
re-posted it too, and `rescheduleAll()` runs on every app foreground, every session write and every
preference change.

`LiveNotificationDismissals` already solves this exact shape for the live notification — a
SharedPreferences map of window-scoped records, pruned on read. Generalised rather than duplicated:

- Renamed to `NotificationRecords`, with `live:` keys (the existing dismissals, keyed by session id)
  and `post:` keys (new, keyed by notification id). Old unprefixed entries are swept as unreadable;
  worst case one live notification returns once after the upgrade.
- The stored window for a post is `(event.at, event.at + grace)` — it expires exactly when the event
  stops being due, so the existing `isExpired` rule works unchanged and the map stays tiny.
- `SessionNotificationPlanner.graceFor(event)` exposes the grace constants `isDue` already switches
  on.

The scheduler posts an event when it is a Live redraw, or when its id is neither showing nor already
recorded at the same window — then records it. Live is never recorded: redrawing it is the point.

Because the window carries `event.at`, a moved or snoozed session changes `at`, which invalidates the
record and correctly posts a fresh reminder for the new time.

This is also what makes the snooze picker below possible: it replaces the reminder notification in
place, and without the ledger the next reschedule would overwrite it with the plain reminder.

## 3. The end-of-day nudge never fires

Three separate causes.

**A running session deleted the event and its alarm.** `isStreakAtRisk` returned false while any
session was running. That is evaluated at *plan* time, and an event that is not in the plan gets no
alarm — so a session running at the warning's moment lost it for the whole day, because the chain only
revisits it when some later alarm fires, usually past the 45-minute grace. The `running` check is
gone; `completedAny` stays. "Nothing finished yet today" is still true while a session is merely
running, and the alternative is an alarm that silently evaporates.

**`scheduleKnown` flipping true triggered no reschedule.** Every day event is gated on
`day.scheduleKnown`, read from `cached_schedule_dates`, but `SessionNotificationStarter` observed only
sessions and preferences, with `distinctUntilChanged`. On a day with no sessions — the exact day you
would test a "you have done nothing today" nudge on — the first sync writes the cached-date row and
emits no session change, so nothing re-planned and the gate stayed shut until the *next* app
foreground. The starter now also observes `observeIsDateCached(today)`, through a new
`ObserveScheduleKnownUseCase`.

**Nothing said why.** `rescheduleAll()` now logs the day context (wake, day end, schedule known), each
planned event with its moment, which were due, and which were skipped as already posted.
`adb logcat -s AwanNotifications` answers the question in seconds instead of a clock-shifting session.

**Debug harness.** `DebugNotificationReceiver` lives in the debug source set only and posts any
notification kind immediately through the real poster:

```bash
adb shell am broadcast -a com.awan.app.DEBUG_NOTIFICATION --es kind streak
```

The notification settings screen shows the same list as a `DEBUG` card, guarded by
`BuildConfig.DEBUG`, firing the same broadcast. Its labels are hardcoded English — a deliberate
exception to the localization rule, since the card never ships.

## 4. Settings restructure, and snooze picked on the notification

### Each timing merges into its switch

The `TIMING` section is gone. Each timing choice sits directly under the switch that owns it and
collapses with it, rather than sitting elsewhere greyed out:

```
SESSIONS
  Before a session starts             [on]
      Remind me (min)       5 · 10 · 15 · 30
      Snooze by (min)       5 · 10 · 15 · Ask
  Live progress while running         [on]
  When a session ends                 [on]
  If you don't reply after a session  [on]
      Follow up after (min) 30 · 60 · 90 · 120
```

Snooze moves under the reminder switch because the Snooze button only exists on that notification. No
new components: `SwitchRow` and `AwanChoiceRow` are unchanged, and the `enabled` gating on the choice
rows disappears — hidden, not disabled.

### "Ask every time"

`snoozeMinutes` carries a sentinel rather than gaining a proto field: `SNOOZE_ASK = -1`, in
`SNOOZE_CHOICES` alongside 5/10/15, and it is the **default** — a new user is asked every time until
they pick a number. The datastore mapper reads any non-zero stored value (`0` still means "never
set").

The reminder's button says what it will do: `Snooze 5 min` with a fixed duration, `Snooze` in Ask
mode. That needs the preference at post time, so `SessionNotificationPoster.post` takes the
preferences the scheduler already holds.

Tapping `Snooze` in Ask mode does not cancel the notification — it re-posts it at the same id as a
picker: same channel, same title, "Snooze for…", and one button per duration. Tapping a duration
cancels and enqueues the worker with an explicit minutes extra, which the worker prefers over the
stored preference. Dismissing the picker is the cancel affordance; three actions is the platform's
practical limit and a fourth would push a duration off screen.

## Verification

- `./gradlew :core:notifications:testDebugUnitTest` — rounding, `graceFor`, the streak change, the
  ledger round trip and pruning, and the "every default is one of its own options" invariant now that
  the snooze default is `SNOOZE_ASK`.
- `./gradlew assembleDebug lint`.
- On device, with `adb logcat -s AwanNotifications`: fire each kind through the debug receiver; tap a
  reminder and confirm it does not return; swipe away a brief and confirm the same; toggle the
  session switches and watch the timings collapse; snooze in both Ask and fixed mode; repeat in
  Arabic.

---

## Implementation notes (what actually differed)

`./gradlew assembleDebug testDebugUnitTest lint` all green; 51 tests in `:core:notifications`.
Installed on `emulator-5554` and driven from `adb`.

### The end-of-day nudge was never broken

The new logging answered it on the first run, which is the whole reason it exists:

```
reschedule at 2026-08-14T21:48:50 · wake=07:00 dayEnd=23:00 scheduleKnown=true
  DailyBrief at 2026-08-14T08:00 · missed
  Reminder at 2026-08-14T15:20 · missed
  no StreakRisk planned · something is completed today, or it is switched off
```

`dayEnd=23:00` and `scheduleKnown=true` — the two suspects — were both fine. The day simply had a
completed session in it (10:30–11:30, green tick on the home screen), which is exactly what the nudge
checks for. Testing this by working normally in the app and then waiting for 21:00 can never show it.
Hence the debug receiver: `--es kind streak` posts it on the `nudges` channel on demand, verified in
`dumpsys notification`.

The two real defects found while reading that path — the plan-time `running` check throwing the
alarm away, and `scheduleKnown` flipping without a reschedule — were both fixed, and both would have
bitten later on a day with nothing completed.

### Traps

1. **Three snooze buttons, one PendingIntent.** `NotificationIntents.action` derived its request code
   from `"$actionName:$sessionId"`, so the picker's 5/10/15 buttons were all `SNOOZE` on the same
   session and collapsed into a single `PendingIntent` under `FLAG_UPDATE_CURRENT` — every button
   would have snoozed by whatever the last one built. The length is now part of the request code.
   Easy to reintroduce by "tidying" the request code back to one per action.
2. **The snooze press is the one action that must not cancel its notification.** Every other button
   cancels first so the press feels immediate; asking how long to snooze for replaces that very
   notification, and cancelling first takes the question with it.
3. **The picker only survives because of the post-once ledger.** It is posted by the receiver at the
   reminder's own id, and the reminder event stays *due* for another five minutes — so before the
   ledger, the next `rescheduleAll()` (any session write, any app foreground) would have overwritten
   the picker with the plain reminder.
4. **The delivery record is keyed on the event's moment, not the session's window.** A follow-up
   fires 90 minutes *after* the session ends, so a record scoped to the session window would already
   be expired when it was written, and the dedup would silently do nothing.

### Deviations from the plan

- `LiveDismissalRecord` was renamed to `NotificationRecord` alongside `LiveNotificationDismissals` →
  `NotificationRecords`. The class now stores delivery notes as well as dismissals, and the old name
  described half of it. Test file renamed to match.
- `EXTRA_SESSION_TITLE` was added to the action intent. The picker re-posts the notification from
  inside a broadcast receiver, which has no window to do a Room read in, and without the title it
  would have re-posted with a blank one.
- `NotificationPreferences.SNOOZE_LENGTH_CHOICES` was added rather than filtering `SNOOZE_CHOICES` at
  each of the three call sites that need "the real lengths".
- The debug card sends the same broadcast `adb` does rather than calling the poster, so nothing about
  notifications reaches a ViewModel and `feature/profile/impl` needs no new module dependency. Its
  labels are hardcoded English behind `BuildConfig.DEBUG` — a deliberate exception to the
  localization rule, marked with a `ponytail:` comment.

### Not verified

The re-post fix could not be exercised end to end on the emulator: it needs a real session whose
reminder is inside its grace window, and the placement engine chooses session times, so one cannot be
put where the test needs it without editing Room underneath the app. The logic is covered by
`NotificationRecordTest` (round trip, expiry) and by the scheduler branch being a single equality
check. Tapping a reminder is the first thing to try on a real day.

The Arabic layout of the four-chip snooze row and the longer "Snooze %1$d min" button label were not
looked at on a device.
