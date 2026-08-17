# Goal scheduling cancellation and tab-stack fix

## Problem

Cancelling `AiTaskProposalsRoute` resets the current tab's sub-stack to `HomeRoute()`. When goal
creation starts from Goals, this places Home inside the Goals sub-stack: Home renders while Goals
remains selected, and Home UI state appears shared across both tabs. Goal scheduling also skips its
discard confirmation while proposals are loading or empty because dirtiness only checks the list.

## Plan

- Make `Navigator.resetCurrentSubStack()` reset only to `currentTopLevelKey`; a tab can no longer be
  reset to another tab's route.
- Dismiss ordinary AI task creation with `goBack()`. After confirmed goal-schedule cancellation,
  clear the schedule draft and reset the current tab to its own root, removing the whole creation
  flow from that sub-stack.
- Treat an active `goalId` as discardable work even while loading or empty.
- Replace the schedule screen's back affordance with a localized X cancel control and show
  goal-specific confirmation copy that explains the goal remains while its schedule draft is lost.
- Add focused navigation and ViewModel regression tests, then run the affected tests and build.

## Verification

- `./gradlew :core:navigation:testDebugUnitTest`
- `./gradlew :feature:ai-tasks:impl:testDebugUnitTest`
- `./gradlew assembleDebug`

## Implementation notes (what actually differed)

- `Navigator.resetCurrentSubStack()` now has no route argument and always restores the current
  top-level key. Successful task creation switches to Home through normal top-level navigation;
  ordinary cancellation pops once, while goal-schedule cancellation clears the current tab flow.
- Goal scheduling counts as dirty as soon as `goalId` is loaded, so X and system Back both show the
  confirmation during loading, error, empty, and review states. Confirming still clears the Room
  schedule draft before navigation.
- Reused `AwanIconButton`, the Lucide X icon, and existing theme tokens. Added matching English and
  Arabic accessibility and goal-specific confirmation strings.
- Verification passed: focused `:core:navigation` and `:feature:ai-tasks:impl` unit tests,
  `./gradlew assembleDebug`, `./gradlew lint`, and `git diff --check`.
- No emulator smoke test was run; the behavior is covered at navigation/state boundaries and the
  full app compiles, but the final visual interaction should still be exercised on-device.
