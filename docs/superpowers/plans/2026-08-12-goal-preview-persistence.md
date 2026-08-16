# Goal Preview Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save an AI goal either without tasks or with every proposed task, then return through the existing goal-created navigation flow.

**Architecture:** Extend the existing domain goal repository create contract to accept proposal tasks. A new `SaveGoalProposalUseCase` orchestrates creation and best-effort decomposition cleanup; the ViewModel owns only choice/dialog/loading state. The preview reuses `AwanDialog` and matches its content inset to the top controls.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit/Kotlin serialization, JUnit Jupiter/JUnit-compatible repository tests, existing Awan design-system components.

## Global Constraints

- Work only on `refactor/goal-preview-draft-choice`; do not edit `develop` directly.
- Use the live Postman contract: `POST /v1/goals` supports optional `tasks`; AI confirm is not used for this choice because it always creates tasks.
- Keep repository access out of presentation; `AddTaskViewModel` receives a domain use case.
- Keep all user-facing copy in English and Arabic `strings.xml`.
- Do not add dependencies or extract a new design-system component for this one dialog/layout.

---

### Task 1: Add the domain persistence seam

**Files:**
- Modify: `core/domain/src/main/kotlin/com/awan/app/core/domain/goal/repository/GoalRepository.kt`
- Create: `core/domain/src/main/kotlin/com/awan/app/core/domain/goal/usecase/SaveGoalProposalUseCase.kt`
- Modify: `core/data/src/main/kotlin/com/awan/app/core/data/goal/GoalRepositoryImpl.kt`
- Modify: `core/network/src/main/kotlin/com/awan/app/core/network/dto/goal/CreateGoalRequest.kt`
- Test: `core/domain/src/test/kotlin/com/awan/app/core/domain/goal/usecase/SaveGoalProposalUseCaseTest.kt`
- Test: `core/data/src/test/java/com/awan/app/core/data/goal/GoalRepositoryTest.kt`

**Interfaces:**
- `GoalRepository.createGoal(title: String, description: String?, targetDate: String?, tasks: List<ProposedTask> = emptyList()): Result<Goal>`
- `SaveGoalProposalUseCase(sessionId: String, proposal: GoalProposal, addTasks: Boolean): Result<Goal>`

- [ ] **Step 1: Extend the repository and fake test seam.** Add the optional `tasks` parameter and update all repository implementations/fakes to compile. Keep the default empty list so existing non-AI callers remain draft-compatible.
- [ ] **Step 2: Add the failing use-case tests.** Use a small fake `GoalRepository` capturing `createGoal` and `cancelDecomposition`; assert Draft passes `emptyList`, Add to tasks passes the proposal list, and a successful create triggers cleanup while returning the created goal.
- [ ] **Step 3: Implement `SaveGoalProposalUseCase`.** Call `createGoal` with either `proposal.tasks` or `emptyList`; after `Result.Success`, call `cancelDecomposition(sessionId)` and return the create result even if cleanup returns an error.
- [ ] **Step 4: Add repository mapping coverage.** Capture `CreateGoalRequest` in a fake remote data source and assert all proposal tasks are present with unique `tempId`s, titles preserved, positive duration fallback `30`, and points fallback `0`.
- [ ] **Step 5: Implement the production mapping.** In `GoalRepositoryImpl`, map each `ProposedTask` to `CreateGoalTaskDto(tempId = "proposal-task-$index", title = task.title, estimatedDuration = task.estimatedDuration?.takeIf { it > 0 } ?: 30, mandatory = false, estimatedPoints = task.estimatedPoints ?: 0, allowTaskSplitting = false)` and preserve goal fields. Keep Room upsert behavior unchanged.
- [ ] **Step 6: Run the domain/data tests.** `./gradlew.bat :core:domain:testDebugUnitTest :core:data:testDebugUnitTest --no-daemon --console=plain`

### Task 2: Add ViewModel choice state and persistence actions

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskAction.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskState.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskViewModel.kt`
- Modify: `feature/add-task/src/test/java/com/awan/feature/addtask/presentation/AddTaskViewModelTest.kt`

**Interfaces:**
- Actions: `AcceptGoalProposal`, `SaveGoalAsDraft`, `AddGoalTasks`, `GoalSaveChoiceDismissed`.
- State: `showGoalSaveChoice: Boolean`.

- [ ] **Step 1: Write failing ViewModel tests.** Assert `AcceptGoalProposal` only opens the choice, `SaveGoalAsDraft` creates with no tasks, `AddGoalTasks` creates with all proposal tasks, duplicate taps produce one request, and both success paths emit `AddTaskEvent.GoalCreated`.
- [ ] **Step 2: Inject `SaveGoalProposalUseCase`.** Remove the presentation dependency on `ConfirmGoalDecompositionUseCase`; keep existing continuation behavior unchanged.
- [ ] **Step 3: Implement the smallest state machine.** `AcceptGoalProposal` sets `showGoalSaveChoice = true`; dismiss clears it; each choice closes the dialog, sets `isSubmitting`, and invokes the use case with the current preview proposal and `addTasks` flag. Reuse `add_task_error_goal_confirm_failed` on failure and preserve the preview for retry.
- [ ] **Step 4: Run the focused ViewModel tests.** `./gradlew.bat :feature:add-task:testDebugUnitTest --tests "com.awan.feature.addtask.presentation.AddTaskViewModelTest" --no-daemon --console=plain`

### Task 3: Render the choice and tighten preview spacing

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewRouteRoot.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt`
- Modify: `feature/add-task/src/main/res/values/strings.xml`
- Modify: `feature/add-task/src/main/res/values-ar/strings.xml`
- Modify: `feature/add-task/src/androidTest/java/com/awan/feature/addtask/ui/components/GoalFormTest.kt` only if existing UI coverage can cover the new action; otherwise no new UI test file.

**Interfaces:**
- `GoalPreviewRouteRoot` maps the four new actions and renders `AwanDialog` while `state.showGoalSaveChoice` is true.
- `GoalPreviewScreen` remains stateless and keeps the existing top check callback.

- [ ] **Step 1: Add localized choice copy.** Add title/body, Draft, Add tasks, and dismiss labels to both resource files; keep the existing preview acceptance content description.
- [ ] **Step 2: Render the dialog in the route root.** Use `AwanDialog` with equal-width secondary Draft and primary Add tasks actions, dismissing without changing proposal state.
- [ ] **Step 3: Align preview insets.** Define one local `previewHorizontalPadding = AwanTheme.spacing.sm` inside `GoalPreviewScreen` and use it for the top row, `LazyColumn`, and footer. Do not change card internals or vertical rhythm.
- [ ] **Step 4: Compile the affected module.** `./gradlew.bat :feature:add-task:compileDebugKotlin --no-daemon --console=plain`

### Task 4: Full verification and diff review

**Files:**
- Read-only: all changed files and Git diff.

- [ ] **Step 1: Run focused tests and build.** `./gradlew.bat :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:add-task:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`
- [ ] **Step 2: Run diff hygiene checks.** `git diff --check`, `git diff --name-only`, and `git status --short --branch`; confirm `docs/ai_scheduling_contract.md` remains uncommitted and unrelated files are absent.
- [ ] **Step 3: Review the requirement checklist.** Confirm Draft creates a goal with zero tasks, Add to tasks sends every proposal task, errors preserve retryable preview state, English/Arabic strings exist, and preview content/footer align with top buttons.
- [ ] **Step 4: Commit only the implementation files.** Use `AWAN-83: add goal preview draft or tasks choice` and leave the user-owned untracked contract untouched.
