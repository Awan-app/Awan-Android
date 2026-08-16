# AI goal scheduling editor

## Goal

After a user accepts all proposed tasks, confirm the decomposition, request a non-persistent schedule proposal, and transition the proposal into an editor where the user can adjust task placements and sessions before the final schedule is persisted.

## Flow

- Keep as draft: call `POST /api/v1/ai/goal-decompose/{sessionId}/confirm`, persist the returned goal/tasks, and navigate to Goals. Do not call scheduling endpoints.
- Add all tasks: call the same decomposition confirm endpoint, persist the returned goal/tasks, then call `POST /api/v1/ai/schedule` and open the scheduling editor for that goal.
- Final save: send the editor's selected sessions to `POST /api/v1/ai/schedule/confirm`; persist returned sessions locally, clear the scheduling draft, and pop the current navigation stack back to the existing Home route. Do not replace the top-level stack with Home.
- Abandon before decomposition confirmation: call `POST /api/v1/ai/goal-decompose/{sessionId}/cancel`.
- Discard editor edits: remove the local scheduling draft only after bottom-sheet confirmation; leave the confirmed goal/tasks intact and return to Goals.

## Editor behavior

- The editor is a dedicated Goals route with a scalar `goalId` and a Hilt ViewModel. It edits placement/session fields only: date, start, end, and optional zone.
- Normal proposed sessions are selected by default. `NO_ZONE` and `OVERLAP` suggestions are visible with their reason/overlap details and remain unselected until the user explicitly opts in.
- Tasks without selected sessions require manual placement before final save. The editor must never fabricate missing sessions.
- Every confirmation in the preview/editor flow is an `AwanActionSheet`, including discard and final-save confirmations. Primary and secondary actions both render `AwanButton`; use `Primary`, `Secondary`, or `Destructive` variants, never a flat text/quiet button for a confirmation choice.
- The editor is reachable by forward navigation from the existing stack. Success returns by popping/resetting the current sub-stack to the existing Home route, preserving Home as the already-created top-level destination.

## Persistence and recovery

- Add the smallest Room-backed draft representation needed to retain the schedule proposal and every placement edit across process death.
- Persist an awaiting-proposal state before requesting `/ai/schedule`, then persist the complete proposal and each local edit.
- On startup after authentication/onboarding, resume the pending editor. If the app died during `/ai/schedule/confirm`, refresh affected dates first; if all intended sessions exist locally, finish successfully, otherwise restore the editor with an explicit retry.
- On successful schedule confirmation, write returned sessions to Room with the existing schedule TTL and clear the draft transactionally so Home observes the persisted sessions.

## Contract

- Retrofit/data/domain mapping must retain proposal task titles, suggestions, overlap metadata, unscheduled tasks, and the returned persisted session IDs.
- The final request must include the user's edited `taskId`, nullable `zoneId`, `start`, and `end` values.
- Keep presentation dependent on use cases, domain contracts free of DTO/Room/Compose types, and network writes mirrored into Room.

## Tasks

1. Correct the scheduling DTOs, API return type, domain models, repository mapping, Room draft/session persistence, migration, and focused data tests.
2. Correct preview confirmation semantics and add the scheduling handoff event; replace remaining preview confirmation dialogs with bottom sheets and use Awan primary/secondary buttons.
3. Implement the dedicated scheduling editor route, ViewModel, state/actions/events, placement UI, picker interactions, validation, and focused presentation tests.
4. Wire app-level Navigation 3 forward navigation, Home pop/reset behavior, process-death resume, and end-to-end navigation tests; run full verification and update implementation notes.

## Verification

- Focused unit tests for DTO/domain mapping, repository persistence, ViewModel state transitions, payload construction, and recovery.
- Room migration/schema checks for the new draft storage.
- Affected module compilation, `testDebugUnitTest`, `lint`, `assembleDebug`, and `git diff --check`.

## Global constraints

- Work only on `refactor/goal-preview-draft-choice`; do not switch to or modify `develop`.
- Preserve the existing uncommitted goal-preview changes unless they conflict with this plan; resolve conflicts in the smallest correct scope.
- Do not add a new feature module or a second navigation framework.
- Do not add a generic repository/abstraction with one implementation when the existing Goal repository/data boundary can own the behavior.
- Do not commit hardcoded user-facing strings; add English and Arabic resources.
- The orchestrator reviews and commits delegated changes; implementers leave changes uncommitted.
