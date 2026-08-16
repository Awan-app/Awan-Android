# Goal Plan Screen Updates, Task Editing, Theming, and Resilience Design

**Date:** 2026-08-16  
**Status:** Approved  
**Author:** AI Software Engineer  
**Jira Issue:** AWAN-83  

---

## 1. Overview & Motivation

The Goal Plan screen (`GoalPreviewScreen` on `GoalPreviewRoute`) provides the user with an AI-generated decomposition of a goal into actionable proposed tasks. Previously, tasks were purely static read-only cards, the Approve button was in the top bar, the input field lacked parity with the Home screen's goal sheet, and several stability issues existed:
- Proposed tasks could not be modified (title, duration, description, removal) prior to drafting or scheduling.
- The Approve button was in the top bar rather than anchored with the revision action at the bottom of the screen.
- Inserting schedule drafts into Room crashed with `SQLiteConstraintException` when the parent goal was not yet cached in the local `goals` table.
- The Goal Plan screen lacked explicit root background theming.
- The "All Goals" list in `GoalsScreen` lacked bottom content padding, causing the floating bottom navigation bar to obscure bottom items.
- Process death wiped out active goal decomposition state in `AddTaskViewModel`.

This design resolves each of these issues while maintaining full architectural alignment with Now in Android (NiA), Clean Architecture, and Awan design system standards.

---

## 2. Architectural Design & Component Breakdown

### 2.1 State Management & Process Death Resilience (`AddTaskViewModel`)
- Inject `SavedStateHandle` into `AddTaskViewModel`.
- Save state changes into `SavedStateHandle` whenever `_state` is updated:
  - `KEY_MODE`: `mode.name`
  - `KEY_INPUT`: `input`
  - `KEY_DESCRIPTION`: `description`
  - `KEY_MANDATORY`: `mandatory`
  - `KEY_IMAGE_URI`: `imageUri`
  - `KEY_AI_ENABLED`: `aiEnabled`
  - `KEY_GOAL_SESSION_ID`: `goalSessionId`
  - `KEY_GOAL_STEP_KIND`: `"INITIAL"`, `"MCQ"`, `"WRITING"`, `"PREVIEW"`
  - `KEY_GOAL_PROPOSAL_TITLE`, `KEY_GOAL_PROPOSAL_DESC`, `KEY_GOAL_PROPOSAL_TARGET_DATE`
  - `KEY_GOAL_PROPOSAL_TASKS_TITLES`, `KEY_GOAL_PROPOSAL_TASKS_DURATIONS`, `KEY_GOAL_PROPOSAL_TASKS_POINTS`
- On `init`, if `SavedStateHandle` contains saved state, restore `_state` with full fidelity so the Goal Plan screen survives process death.
- Support new actions for proposed task editing:
  - `AddTaskAction.UpdateProposedTask(index: Int, task: ProposedTask)`: Updates a task in `state.goalStep.proposal.tasks`.
  - `AddTaskAction.RemoveProposedTask(index: Int)`: Removes a task from `state.goalStep.proposal.tasks`.

### 2.2 Goal Proposal Task Editing (`GoalPreviewCards.kt`)
Modeled directly after the `ai-tasks` module's `ProposalCard`:
- Each proposed task inside `GoalPreviewProposalCard` is rendered as an interactive card.
- **Collapsed state:**
  - Index badge (`Box` with `AwanTheme.colors.sky` or pill style).
  - Task title (`AwanText` with `AwanTheme.typography.body`, semi-bold).
  - Duration badge (`AwanBadge` in Violet or `AwanText` duration label).
  - Points badge (`AwanBadge` in Sky if points > 0).
  - Remove button (`AwanIconButton` with `Lucide.X`).
  - Tapping anywhere on the card expands it with haptic feedback.
- **Expanded state:**
  - Title editing via `AwanTextField` (`textStyle = AwanTheme.styles.headingText`).
  - Attribute chips (`AttributeChips`):
    - Duration chip with dropdown menu displaying quick presets (`15, 30, 45, 60, 90, 120, 180, 240` minutes).
  - Remove button to discard the task.
  - Tapping the card header collapses the card.

### 2.3 Bottom Action & Input Field Component (`GoalPreviewScreen.kt`)
- **Top Bar:** Top bar contains only Close (`X`) button on the start and "Goal Plan" title (`AwanText`) in the center. The previous top-right Check button is removed.
- **Input Field:** Uses `AwanAiAura` wrapping `AwanTextField` with trailing `GoalMicButton` / speech recognizer, identical to the component on the Home screen Add Task / Goal Sheet.
- **Contextual Action Button:** Positioned directly beneath the input field:
  - When `state.input.isBlank()`:
    - Button text: **"Approve plan"** (`R.string.add_task_goal_preview_accept_action` or `add_task_goal_preview_approve_plan`).
    - Variant: `AwanButtonVariant.Primary`.
    - `onClick`: Triggers `onAccept()` (`AddTaskAction.AcceptGoalProposal`).
    - `enabled`: `state.canAcceptGoal`.
  - When `state.input.isNotBlank()`:
    - Button text: **"Update plan"** (`R.string.add_task_goal_preview_update_plan`).
    - Variant: `AwanButtonVariant.Primary` / `Quiet`.
    - `onClick`: Triggers `onRevisionSubmit()` (`AddTaskAction.Submit`).
    - `enabled`: `state.canSubmit`.

### 2.4 Room Database Crash Prevention & Data Consistency (`GoalRepositoryImpl`)
- **Foreign Key Constraint Crash Fix:** In `proposeGoalSchedule(goalId)`, before calling `scheduleDraftDao.insertDraftIfNotExists`, verify whether `goalDao.getGoal(goalId)` exists locally. If absent (e.g. cold start, draft navigation), insert a stub `GoalEntity` so that SQLite foreign key constraints are always satisfied.
- **Local Data Retrieval:** When querying goals via `getGoals()` or `getGoal(goalId)` from Room cache, query associated tasks from `taskDao` and map them into the `Goal` model instead of defaulting to `emptyList()`.
- **Customized Plan Persistence:** In `SaveGoalProposalUseCase`, pass the user's edited `proposal.tasks` to `createGoal` when saving, ensuring all edits are preserved on the backend and in Room.

### 2.5 Theming & Padding Improvements
- **Theming:** Explicitly apply `.background(AwanTheme.colors.background)` to the root container of `GoalPreviewScreen`, and ensure all colors (`colors.surface`, `colors.line`, `colors.textPrimary`, `colors.textSecondary`, `colors.sky`) and typography follow `AwanTheme`.
- **All Goals Padding:** In `GoalsScreen.kt`, add `contentPadding = PaddingValues(bottom = 100.dp)` to `LazyColumn` so the floating `AwanBottomNavBar` never obstructs the list content.

---

## 3. Localization Requirements

Add required string keys to `feature/add-task/src/main/res/values/strings.xml` and `values-ar/strings.xml`:
- `add_task_goal_preview_update_plan`: "Update plan" / "تحديث الخطة"
- `add_task_goal_preview_approve_plan`: "Approve plan" / "الموافقة على الخطة"
- `add_task_goal_preview_remove_task`: "Remove task" / "حذف المهمة"
- `add_task_goal_preview_task_title_placeholder`: "Task title" / "عنوان المهمة"

---

## 4. Verification Plan

1. **Unit Tests:**
   - `AddTaskViewModelTest`: Verify task editing (`UpdateProposedTask`, `RemoveProposedTask`), dynamic button text switching ("Approve plan" vs "Update plan"), `SavedStateHandle` restoration across process death, and goal proposal acceptance.
   - `GoalRepositoryTest` / `TestGoalDaos`: Verify safe draft insertion without foreign key violations.
2. **Build & Lint Verification:**
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
   - `./gradlew lint`
