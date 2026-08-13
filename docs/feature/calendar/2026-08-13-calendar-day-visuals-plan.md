# Calendar day visuals

## Summary

Refactor the existing per-day calendar rendering only. Keep `DayState` and data loading unchanged; remove the row-wide streak gradient and selected-date outline. No insights, Lottie asset, or shader implementation in this change.

## Global constraints

- Keep `DayCell` as the single visual decision point; use small private layers rather than a new configuration system.
- Plain/missed days show only the day number.
- Render a 36dp circular base—the current today-circle size—for today’s primary circle, the streak day-number circle, and the future/today deadline shader host.
- Streak days show a static existing fire icon placeholder plus the 36dp day-number circle, positioned by local normalized x/y anchor values and independently tunable circle size. This is the future Lottie replacement seam.
- Deadline days from today forward reserve a transparent circular shader layer under the number. No visible fallback effect until the shader is added.
- Enforce precedence: today + deadline is primary circle → same-size circular shader host → number; today + streak is primary-tinted fire host → anchored day-number circle; today + streak + deadline is primary circle → circular shader host → number, plus an independent fire badge at the cell’s top-right.
- Remove the current selection outline from every day state. Tapping still performs the existing navigation to Home.
- Motion behavior remains unchanged; no reduced-motion variant is added.
- User-facing descriptions must remain accessible and localized through the existing resource pattern.

## Tasks

### Task 1: Calendar day rendering

Update `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarScreen.kt` to render the visual states above. Remove the row-wide streak gradient and selection outline. Reuse the existing static fire icon and existing theme tokens. Keep the deadline layer transparent but circular and leave a clear local seam for later shader/Lottie replacement.

### Task 2: Calendar visual tests

Update the focused Calendar tests to cover plain, today, streak, deadline, today + deadline, today + streak, and today + streak + deadline. Verify upcoming deadline filtering, no selected-date outline, and unchanged tap navigation. Tests must assert meaningful semantics or rendered state, not only that composition completes.

### Task 3: Verification

Run the focused Calendar tests and compile the Calendar feature module. Inspect the final diff for scope, localization, accessibility, and regressions. Record actual verification and any deviations in this plan’s implementation notes.


## Implementation notes (what actually differed)

- `./gradlew :feature:calendar:impl:testDebugUnitTest` completed with `BUILD SUCCESSFUL in 4s` (107 actionable tasks: 4 executed, 103 up-to-date).
- `./gradlew :feature:calendar:impl:compileDebugAndroidTestKotlin` completed with `BUILD SUCCESSFUL in 1s` (96 actionable tasks: 96 up-to-date).
- `git diff --check 1e80377c..HEAD` exited 0 with no output. The pre-notes `git status --short --branch` was `## refactor/calendar-screen...origin/develop [ahead 13]` with this plan file untracked.
- `adb devices` returned exactly `List of devices attached` with no attached device/emulator. Therefore `./gradlew :feature:calendar:impl:connectedDebugAndroidTest` was not run; this is a runtime-verification limitation, not a code failure.
- Diff review covered only `CalendarScreen.kt`, `CalendarDateMapper.kt`, and focused Calendar instrumentation tests. The row-wide streak gradient and selected-date outline are removed; existing localized streak/deadline descriptions remain on the day number and decorative fire icons have null descriptions.
- The actual mapper change makes `hasDeadline` date-scoped to today and future dates, so historical goal dates do not reserve a deadline host. The instrumentation test explicitly distinguishes a past deadline from a future one.
- Visual tests use date-scoped semantic test tags plus ancestor matching, rather than global cell tags or pixel assertions, so identical day numbers/decorations elsewhere in the grid cannot satisfy a target-day assertion.
- As planned, `DeadlineShaderHost` is a transparent circular shader seam and the existing static flame drawable is a Lottie replacement placeholder; neither adds a visible shader nor Lottie motion in this refactor.
