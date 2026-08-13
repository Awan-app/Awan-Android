# Goal Preview Persistence Design

## Goal

Let the user choose whether an AI-generated goal is saved without tasks as a draft or saved with every proposed task, while tightening preview alignment.

## Backend mapping

- Draft calls `POST /v1/goals` with `title`, `description`, `targetDate`, and an empty `tasks` array.
- Add to tasks first creates the goal with `POST /v1/goals` and an empty `tasks` array, then calls `POST /v1/goals/{goalId}/tasks/bulk` with every proposed task.
- Each proposed task receives a client-generated `tempId`; missing duration/points use the backend-safe defaults already represented by the request DTO. If bulk creation fails, delete the newly-created goal and leave the preview retryable.
- After either successful create, cancel the unused AI decomposition session as best-effort cleanup. A cancellation failure must not hide the already-created goal.

## Android design

`AddTaskViewModel` remains the single state owner. Tapping the existing preview check action only opens a choice bottom sheet. New state actions select Draft or Add to tasks; both call one domain use case that creates the goal, emits the existing `GoalCreated` event, and resets the flow through the current navigation path.

The choice uses the existing `AwanActionSheet` bottom sheet with two localized actions. The preview `LazyColumn` and footer use the same horizontal `AwanTheme.spacing.sm` inset as the top-button row, removing the current extra inset without introducing a new layout abstraction.

## Verification

- ViewModel tests prove the dialog action does not persist immediately, draft sends no tasks, add-to-tasks sends all proposal tasks, and both paths emit `GoalCreated`.
- Repository tests prove proposal tasks map to unique `tempId`s and preserve titles/durations/points.
- Run the focused add-task/domain/data tests, affected Kotlin compilation, and app assembly.
