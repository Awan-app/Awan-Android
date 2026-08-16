# Fix Schedule Confirmation UI Stuck in Loading

## Problem
In the AI Goal Creation flow, when a user selects **Add tasks** from the Goal Preview screen, navigates to the AI Schedule screen (`AiTaskProposalsRoute`), and approves the schedule proposal:
1. `POST v1/ai/schedule/confirm` succeeds with HTTP 200, Room persists the confirmed sessions with TTL, and the draft is deleted.
2. The UI remains stuck showing the AI loading screen (`LoadingBody`).

## Root Cause
1. `AddTaskViewModel._events` was defined as `MutableSharedFlow(replay = 1)`. When the user chose "Add tasks", `AddTaskEvent.GoalScheduleRequested(goalId)` was emitted and remained in the replay cache.
2. `AwanApp.kt` wired `aiTasksEntry(onBack = { navigator.goBack() })`. When schedule confirmation finished, it popped `AiTaskProposalsRoute` back to `GoalPreviewRoute`.
3. When `GoalPreviewRouteRoot` became active again (`STARTED` lifecycle), its `ObserveAsEvents` collected the cached `GoalScheduleRequested(goalId)` event again, immediately re-navigating to `AiTaskProposalsRoute(goalId)`.
4. On `AiTaskProposalsRoute`, `AiTasksViewModel` set `isLoading = true`, displaying `LoadingBody` indefinitely because the schedule was already confirmed and deleted.
5. In addition, when tasks are created / schedule is confirmed, the navigation stack must reset back to `HomeRoute()`, rather than popping one screen back to `GoalPreviewRoute`.

## Solution
1. **Single-shot events**: Converted `AddTaskViewModel._events` from `MutableSharedFlow(replay = 1)` to `Channel<AddTaskEvent>(Channel.BUFFERED).receiveAsFlow()`, matching the project's UDF pattern.
2. **Navigation callback propagation**: Added `onTasksCreated: (Int) -> Unit` to `AiTasksRouteScreen` and `AiTasksEntryProvider`.
3. **Sub-stack reset**: Wired `aiTasksEntry` in `AwanApp.kt` to reset the current sub-stack to `HomeRoute()`.

## Verification
- Unit tests in `AddTaskViewModelTest` verifying events do not replay on multiple collections.
- Unit tests in `NavigatorTest` verifying `resetCurrentSubStack` clears intermediate screens and resets to root.
- Full `./gradlew testDebugUnitTest`, `./gradlew lint`, and `./gradlew assembleDebug` pass.

## Implementation notes (what actually differed)
- Fixed `AddTaskViewModel._events` by migrating from `MutableSharedFlow(replay = 1)` to `Channel<AddTaskEvent>(Channel.BUFFERED).receiveAsFlow()`.
- Threaded `onTasksCreated: (Int) -> Unit` in `AiTasksEntryProvider` and `AiTasksRouteScreen`.
- Configured `aiTasksEntry` in `AwanApp.kt` to reset the sub-stack back to `HomeRoute()`.
- Verification completed: 790 unit tests executed and passed, lint passed with 0 errors, and debug APK assembled cleanly.

