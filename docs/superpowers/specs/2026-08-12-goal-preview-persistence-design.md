# Goal Preview Persistence Design

## Goal

Let the user choose whether an AI-generated goal is saved without tasks as a draft or saved with every proposed task, while tightening preview alignment.

## Backend mapping

- Draft calls `POST /v1/goals` with `title`, `description`, `targetDate`, and an empty `tasks` array.
- Add to tasks calls the same endpoint with every proposed task in `tasks`.
- Each proposed task receives a client-generated `tempId`; missing duration/points use the backend-safe defaults already represented by the request DTO.
- After either successful create, cancel the unused AI decomposition session as best-effort cleanup. A cancellation failure must not hide the already-created goal.

## Android design

`AddTaskViewModel` remains the single state owner. Tapping the existing preview check action only opens a choice dialog. New state actions select Draft or Add to tasks; both call one domain use case that creates the goal, emits the existing `GoalCreated` event, and resets the flow through the current navigation path.

The dialog reuses `AwanDialog` with two equal-width localized actions. The preview `LazyColumn` and footer use the same horizontal `AwanTheme.spacing.sm` inset as the top-button row, removing the current extra inset without introducing a new layout abstraction.

## Verification

- ViewModel tests prove the dialog action does not persist immediately, draft sends no tasks, add-to-tasks sends all proposal tasks, and both paths emit `GoalCreated`.
- Repository tests prove proposal tasks map to unique `tempId`s and preserve titles/durations/points.
- Run the focused add-task/domain/data tests, affected Kotlin compilation, and app assembly.
