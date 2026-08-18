# 2026-08-18 Refactor Inbox Task Row & Goal Task Navigation — Implementation Plan

## Overview

Refactor the Inbox screen and data layer to remove the expandable task row behavior and session display/fetching. Also wire task click navigation on the Goal Details screen so clicking any task redirects directly to the Task Details screen (`TaskDetailsRoute`).

## Architecture layers touched

| Layer | What changes |
|---|---|
| `:core:data` | `TaskRemoteDataSource` + impl `getInboxTasks()` returns `Result<List<TaskInfoResponse>>`; `TaskRepositoryImpl.getInboxTasks()` maps to `List<Task>` |
| `:core:domain` | `TaskRepository.getInboxTasks()` returns `Result<List<Task>>`; `GetInboxTasksUseCase` returns `Result<List<Task>>` |
| `:feature:goals:impl` | `InboxTaskCard` made non-expandable, sessions removed, card clickable; `InboxViewModel` consumes `List<Task>`; `GoalTaskTimelineItem` made clickable; `GoalDetailsScreen` & `GoalsEntryProvider` wire `onNavigateToTaskDetails` |

## Navigation

- Inbox task row click: navigates to `TaskDetailsRoute(taskId)`.
- Goal details task row click: navigates to `TaskDetailsRoute(taskId)`.

## Implementation notes (what actually differed)

- **Verification**: Executed `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, and `./gradlew :core:domain:lint :core:data:lint :feature:goals:impl:lint` — all builds, tests (790+ tasks), and lints passed with 0 errors.
- **Data layer refactor**: `getInboxTasks()` in `TaskRepository` and `GetInboxTasksUseCase` returns `Result<List<Task>>` rather than `Result<List<TaskWithSessions>>`. `TaskRemoteDataSource` returns `Result<List<TaskInfoResponse>>`.
- **UI clean-up**: Removed expandable chevron button, session list accordion, session models, and session time filters from inbox task rows and filter sheets. The entire `InboxTaskCard` is clickable to navigate to task details.
- **Goal Task Click Navigation**: Updated `GoalTaskTimelineItem` to accept `onClick: (() -> Unit)?` and make the task content column clickable. Threaded `onNavigateToTaskDetails` from `GoalsEntryProvider` through `GoalDetailsRouteScreen`, `GoalDetailsScreen`, `GoalDetailsContent` into `GoalTaskTimelineItem`.

