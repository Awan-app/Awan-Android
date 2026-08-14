# Goal preview draft or tasks choice

## Goal

After AI decomposition, let the user save only the goal or save the goal with all proposed tasks.

## Backend contract

- Draft uses POST /v1/goals with no tasks.
- After a successful draft create, the AI decomposition session is cancelled as best-effort cleanup.
- Add to tasks uses POST /v1/ai/goal-decompose/{sessionId}/confirm; the response creates the goal and returns all server-created tasks.
- The confirm response is persisted to Room, including categories and dependencies. Confirm failures leave the preview retryable.
- The confirm flow does not call the cancellation endpoint.

## UI

- The existing preview check action opens a two-action AwanActionSheet bottom sheet.
- Draft and Add tasks share the existing loading/error/event path.
- Preview list/footer horizontal padding matches the top-button row at AwanTheme.spacing.sm.

## Verification

- Add-task ViewModel tests cover draft creation/cancellation, confirmation, and retry behavior.
- Repository tests cover confirmation request routing and persistence of returned tasks.
- Run focused tests, affected compilation, app assembly, and git diff --check.