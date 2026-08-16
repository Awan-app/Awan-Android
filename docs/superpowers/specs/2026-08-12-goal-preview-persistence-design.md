# Goal Preview Persistence Design

## Goal

Let the user choose whether an AI-generated goal is saved without tasks as a draft or saved with every proposed task, while tightening preview alignment.

## Backend mapping

- Draft calls POST /v1/goals with title, description, targetDate, and an empty tasks array.
- After a successful draft create, cancel the active AI decomposition session as best-effort cleanup. A cancellation failure must not hide the already-created goal.
- Add to tasks calls POST /v1/ai/goal-decompose/{sessionId}/confirm. The backend creates the goal and all proposed tasks in one operation and returns the complete goal response.
- Persist the confirmation response to Room, including task goal IDs, categories, and dependencies. A confirmation failure leaves the preview retryable.
- Do not cancel the decomposition session after confirmation; the confirm endpoint owns that transition.

## Android design

AddTaskViewModel remains the single state owner. Tapping the existing preview check action only opens a choice bottom sheet. Draft calls goal creation with no tasks; Add to tasks confirms the server proposal. Both call one domain use case, emit the existing GoalCreated event on success, and reset the flow through the current navigation path.

The choice uses the existing AwanActionSheet bottom sheet with two localized actions. The preview LazyColumn and footer use the same horizontal AwanTheme.spacing.sm inset as the top-button row, removing the current extra inset without introducing a new layout abstraction.

## Verification

- ViewModel tests prove the choice does not persist immediately, Draft sends no tasks and cancels, Add to tasks confirms once, and confirmation failure preserves the preview.
- Repository tests prove confirmation routes through the session endpoint and persists returned tasks, categories, and dependencies.
- Run the focused add-task/domain/data tests, affected Kotlin compilation, and app assembly.