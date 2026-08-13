# Goal preview draft or tasks choice

## Goal

After AI decomposition, let the user save only the goal or save the goal with all proposed tasks.

## Backend contract

- Draft uses `POST /v1/goals` with no tasks.
- Add to tasks first uses `POST /v1/goals` with no tasks, then `POST /v1/goals/{goalId}/tasks/bulk` with every proposal task and a unique `tempId`.
- If bulk creation fails, the newly-created goal is deleted and the preview remains retryable.
- The AI decomposition session is cancelled after successful creation as best-effort cleanup.

## UI

- The existing preview check action opens a two-action `AwanActionSheet` bottom sheet.
- Draft and Add tasks share the existing loading/error/event path.
- Preview list/footer horizontal padding matches the top-button row at `AwanTheme.spacing.sm`.

## Verification

- Domain/use-case tests cover task inclusion and cleanup.
- Repository tests cover request mapping.
- Add-task ViewModel tests cover both actions and retry behavior.
- Run focused tests, affected compilation, app assembly, and `git diff --check`.
