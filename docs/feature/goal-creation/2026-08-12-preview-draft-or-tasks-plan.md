# Goal preview draft or tasks choice

## Goal

After AI decomposition, let the user save only the goal or save the goal with all proposed tasks.

## Backend contract

- Draft and Add to tasks both use `POST /v1/goals`.
- Draft sends no tasks.
- Add to tasks sends every proposal task with a unique `tempId`.
- The AI decomposition session is cancelled after successful creation as best-effort cleanup.

## UI

- The existing preview check action opens a two-action `AwanDialog`.
- Draft and Add tasks share the existing loading/error/event path.
- Preview list/footer horizontal padding matches the top-button row at `AwanTheme.spacing.sm`.

## Verification

- Domain/use-case tests cover task inclusion and cleanup.
- Repository tests cover request mapping.
- Add-task ViewModel tests cover both actions and retry behavior.
- Run focused tests, affected compilation, app assembly, and `git diff --check`.
