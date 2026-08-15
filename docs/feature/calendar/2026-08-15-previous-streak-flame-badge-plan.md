# Previous Streak Flame Badge & Gradient Day Circle

## Summary

Update the calendar day cell rendering for previous-day streaks (`isStreakDay && !isToday`).
Instead of rendering a 48dp fire host icon with an anchored inner circle, previous streak days now render a standard 36dp circular day cell with a vibrant bright orange gradient background, white day number text, and a 16dp flame badge anchored at the top-right (`Alignment.TopEnd`).
All today streak/deadline visuals and non-streak day states remain strictly unchanged.

## Global Constraints

- **Scope**: Calendar day cell visual rendering in `:feature:calendar:impl`.
- **Today Invariants**: Do not alter `today` rendering paths (`today + streak + deadline`, `today + streak`, `today + deadline`, `today`).
- **Deadline Invariants**: Do not alter deadline shader host positioning or behavior.
- **Previous Streak Days**:
  - Circle diameter: 36dp (`dayCellCircleSize`).
  - Background: Vertical gradient `Brush.verticalGradient(listOf(Color(0xFFFF9E1B), Color(0xFFFF5722)))`.
  - Day Number: White text (`Color.White`).
  - Flame Badge: `FireBadge` at `Alignment.TopEnd` with `tint = AwanTheme.colors.streakIcon` and test tag `calendar_day_${date}_streak_badge`.
  - Tag: `calendar_day_${date}_streak_number` on the gradient circle.

## Implementation Tasks

### Task 1: Update DayCell rendering in `CalendarScreen.kt`
- Add `Brush` import if needed.
- Update `dayState.isStreakDay` branch in `DayCell` to render 36dp container, gradient circle, centered `DayNumberText` (white), and top-right `FireBadge`.

### Task 2: Update Instrumentation Tests in `CalendarScreenTest.kt`
- Update `dayCell_streak_showsFireHostAndNumberCircle` to assert gradient circle (`streak_number`), day number, and top-right flame badge (`streak_badge`), while asserting absence of old `streak_fire`.

### Task 3: Verification
- Run unit tests (`:feature:calendar:impl:testDebugUnitTest`).
- Run androidTest compilation (`:feature:calendar:impl:compileDebugAndroidTestKotlin`).
- Run lint (`:feature:calendar:impl:lintDebug`).

## Implementation notes (what actually differed)

- `./gradlew :feature:calendar:impl:testDebugUnitTest` executed with 0 failures (`BUILD SUCCESSFUL`).
- `./gradlew :feature:calendar:impl:compileDebugAndroidTestKotlin` completed with `BUILD SUCCESSFUL`.
- `./gradlew :feature:calendar:impl:lintDebug` completed with clean report.
- The `dayState.isStreakDay` branch in `CalendarScreen.kt` was updated to render a 36dp circular cell with `Brush.verticalGradient(listOf(Color(0xFFFF9E1B), Color(0xFFFF5722)))`, `Color.White` text, and a top-right `FireBadge` (`Alignment.TopEnd`).
- Today's streak visuals (48dp fire host, primary circle, deadline shader host, top-right badge) remain completely unchanged.
- Instrumentation tests in `CalendarScreenTest.kt` were updated to assert the new gradient circle and top-right flame badge on previous streak days while validating absence of the full fire host.

