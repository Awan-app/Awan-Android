# Goal Plan Updates, Task Editing, Theming, and Resilience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement task editing on the Goal Plan screen modeled after `ai-tasks`, move the Approve button to the bottom beneath the input field with dynamic "Update plan" text, fix Room Database crashes and join goal tasks from Room, correct theming, adjust "All Goals" bottom padding, and ensure full process death recovery.

**Architecture:** Now in Android (NiA) Clean Architecture. Presentation interacts exclusively with use cases; `AddTaskViewModel` persists and restores goal decomposition state via `SavedStateHandle`; `GoalRepositoryImpl` guarantees database foreign key safety and caches goal tasks into `taskDao`; UI components adhere to `AwanTheme` and design system styles.

**Tech Stack:** Kotlin 2.4.0, Jetpack Compose, Jetpack ViewModel (`SavedStateHandle`), Room Database, Hilt, Navigation 3.

## Global Constraints
- Do not alter the core data model of tasks beyond adding edit support for existing fields.
- Preserve the visual style of the existing app; only adjust theming where necessary for the Goal Plan screen.
- The input field must be the same component (`AwanTextField` + `AwanAiAura` + speech mic) used on the Home screen.
- The Approve button must remain functional and only change its label to "Update plan" when the text field is non-empty.
- All display strings must live in `strings.xml` (both `res/values/` and `res/values-ar/`).
- Every commit message must be prefixed with `AWAN-83: `.

---

### Task 1: Room Database Foreign Key Safety and Goal Tasks Mapping

**Files:**
- Modify: `core/data/src/main/kotlin/com/awan/app/core/data/goal/GoalRepositoryImpl.kt`
- Modify: `core/data/src/main/kotlin/com/awan/app/core/data/goal/GoalMappers.kt`
- Modify: `core/domain/src/main/kotlin/com/awan/app/core/domain/goal/usecase/SaveGoalProposalUseCase.kt`
- Test: `core/data/src/test/java/com/awan/app/core/data/goal/GoalRepositoryTest.kt`

**Interfaces:**
- `GoalRepositoryImpl.proposeGoalSchedule(goalId: String)`: Ensures `goalDao.getGoal(goalId)` is present in Room before inserting `ScheduleDraftEntity`, creating a stub entity if missing to avoid `SQLiteConstraintException`.
- `GoalRepositoryImpl.getGoals()`: Maps tasks from `taskDao` into each `Goal` domain model.
- `SaveGoalProposalUseCase`: Passes `proposal.tasks` to `repository.createGoal(...)` when saving.

- [ ] **Step 1: Write unit tests in `GoalRepositoryTest.kt` for safe draft insertion and goal task retrieval**
- [ ] **Step 2: Run tests to verify failure/baseline**
  Run: `./gradlew :core:data:testDebugUnitTest --tests "com.awan.app.core.data.goal.GoalRepositoryTest"`
- [ ] **Step 3: Update `GoalRepositoryImpl.kt`, `GoalMappers.kt`, and `SaveGoalProposalUseCase.kt`**
  - In `proposeGoalSchedule`: If `goalDao.getGoal(goalId) == null`, insert a fallback `GoalEntity` so `schedule_drafts` foreign key is always satisfied.
  - In `getGoals()`: Ensure `Goal` models returned from cache include their tasks from `taskDao`.
  - In `SaveGoalProposalUseCase`: Pass `proposal.tasks` into `createGoal` when `addTasks` is true.
- [ ] **Step 4: Run tests to verify they pass**
  Run: `./gradlew :core:data:testDebugUnitTest --tests "com.awan.app.core.data.goal.GoalRepositoryTest"`
- [ ] **Step 5: Commit**
  `git commit -m "AWAN-83: fix Room crash on goal schedule draft and map cached goal tasks"`

---

### Task 2: ViewModel State Management, Process Death Recovery, and Task Edit Actions

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskAction.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskState.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/presentation/AddTaskViewModel.kt`
- Test: `feature/add-task/src/test/java/com/awan/feature/addtask/presentation/AddTaskViewModelTest.kt`

**Interfaces:**
- `AddTaskAction.UpdateProposedTask(index: Int, task: ProposedTask)`: Updates a task in the proposal.
- `AddTaskAction.RemoveProposedTask(index: Int)`: Removes a task from the proposal.
- `AddTaskViewModel(savedStateHandle: SavedStateHandle, ...)`: Saves/restores goal decomposition and preview state to survive process death.

- [ ] **Step 1: Write unit tests in `AddTaskViewModelTest.kt` for task editing, button label logic, and `SavedStateHandle` restoration**
- [ ] **Step 2: Run tests to verify failure**
  Run: `./gradlew :feature:add-task:testDebugUnitTest --tests "com.awan.feature.addtask.presentation.AddTaskViewModelTest"`
- [ ] **Step 3: Implement actions, state helpers, and `SavedStateHandle` persistence in `AddTaskViewModel.kt`**
  - Add `UpdateProposedTask` and `RemoveProposedTask` to `AddTaskAction`.
  - In `AddTaskViewModel`, inject `SavedStateHandle`, restore state on initialization, and save state on mutations.
- [ ] **Step 4: Run tests to verify they pass**
  Run: `./gradlew :feature:add-task:testDebugUnitTest --tests "com.awan.feature.addtask.presentation.AddTaskViewModelTest"`
- [ ] **Step 5: Commit**
  `git commit -m "AWAN-83: add task editing actions and SavedStateHandle process death resilience"`

---

### Task 3: Goal Plan Screen UI: Task Editing, Bottom Approve/Update Button, Input Parity, and Theming

**Files:**
- Modify: `feature/add-task/src/main/res/values/strings.xml`
- Modify: `feature/add-task/src/main/res/values-ar/strings.xml`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/GoalPreviewCards.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt`
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewRouteRoot.kt`

**Interfaces:**
- `GoalPreviewScreen`: Displays editable proposal tasks, removes top-bar check icon, places Approve/Update button below input field with dynamic label, and enforces `AwanTheme.colors.background` root theming.
- `GoalPreviewProposalCard`: Renders expandable task items with `AwanTextField` title editing, duration dropdown menu with presets (`15, 30, 45, 60, 90, 120, 180, 240`), points badge, and remove button.

- [ ] **Step 1: Add localization strings in `values/strings.xml` and `values-ar/strings.xml`**
  - Add `add_task_goal_preview_update_plan`, `add_task_goal_preview_approve_plan`, `add_task_goal_preview_remove_task`, `add_task_goal_preview_task_title_placeholder`.
- [ ] **Step 2: Update `GoalPreviewCards.kt` with editable task cards inspired by `ai-tasks` module**
  - Implement expandable task cards supporting title edit, duration quick-presets dropdown, points badge, and removal.
- [ ] **Step 3: Update `GoalPreviewScreen.kt` and `GoalPreviewRouteRoot.kt`**
  - Remove top bar check button.
  - Anchor the Approve / Update button at the bottom directly beneath the `AwanTextField` + `AwanAiAura` component.
  - Set button text to "Update plan" when `state.input` is not blank, and "Approve plan" when blank.
  - Apply `AwanTheme.colors.background` and proper theming to the screen root.
- [ ] **Step 4: Run feature unit tests**
  Run: `./gradlew :feature:add-task:testDebugUnitTest`
- [ ] **Step 5: Commit**
  `git commit -m "AWAN-83: modernize Goal Plan screen with task editing, bottom action, and theming"`

---

### Task 4: "All Goals" Screen Bottom Padding

**Files:**
- Modify: `feature/goals/impl/src/main/java/com/awan/feature/goals/impl/ui/GoalsScreen.kt`

- [ ] **Step 1: Add `contentPadding = PaddingValues(bottom = 100.dp)` to `GoalsScreen` LazyColumn**
- [ ] **Step 2: Run goals module unit tests**
  Run: `./gradlew :feature:goals:impl:testDebugUnitTest`
- [ ] **Step 3: Commit**
  `git commit -m "AWAN-83: add bottom content padding to All Goals list to prevent bottom sheet overlap"`

---

### Task 5: Full Verification & Quality Assurance

- [ ] **Step 1: Run all unit tests across the entire project**
  Run: `./gradlew testDebugUnitTest`
- [ ] **Step 2: Run assembleDebug to ensure clean build**
  Run: `./gradlew assembleDebug`
- [ ] **Step 3: Verify checklist against all user requirements**
