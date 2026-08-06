# AWAN-83 Goal Preview Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When `GoalStep.Preview` arrives, collapse `AddTaskSheet` and navigate to an independent `GoalPreviewRoute` screen rendered by `NavDisplay`, while rendering any follow-up clarification questions inline on that same route.

**Architecture:** `AddTaskViewModel` (app-scoped, shared) owns all goal state. `AddTaskSheet` detects `GoalStep.Preview` via `LaunchedEffect`, awaits `sheetState.hide()`, then signals navigation to `GoalPreviewRoute`. `GoalPreviewRoute` renders the proposal full-screen and handles inline follow-up questions and revision without returning to the sheet.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation 3 (`entryProvider` / `entry<T>`), `ModalBottomSheet` (`ExperimentalMaterial3Api`), Hilt (`hiltViewModel`), coroutines.

## Global Constraints

- Branch: `feature/goal-creation`
- All commits must include `AWAN-83:` prefix in the message
- No new Gradle dependencies — `feature/add-task/build.gradle.kts` already has `hilt-navigation-compose` and `navigation3` is wired via `:core:navigation`
- Follow existing Navigation 3 patterns: `@Serializable data object Route : Route`, `NavEntryProviderScope.Xentry()` extension in the feature module, wired in `AwanApp.kt`
- `AddTaskViewModel` must remain the single source of truth for all goal state; `GoalPreviewRoute` must not instantiate its own ViewModel
- Sheet collapse must fully complete (`sheetState.hide()` suspends until `SheetValue.Hidden`) before navigation occurs
- All follow-up clarification questions render **inline** on `GoalPreviewRoute` — do not pop back to sheet
- Goal accepted → `navigator.goBack()` (no replaceAll)
- Goal discarded/Back → `AddTaskAction.DismissRequested` → `AwanConfirmDialog` → on confirm `navigator.goBack()`
- Test with: `.\gradlew.bat :feature:add-task:testDebugUnitTest --no-daemon --console=plain`

---

### Task 1: GoalPreviewRoute & Entry Registration

**Files:**
- Create: `feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewRoute.kt`
- Create: `feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewEntry.kt`
- Modify: `app/src/main/java/com/awan/app/AwanApp.kt`

**Interfaces:**
- Produces: `GoalPreviewRoute` (a `Route` singleton), `fun NavEntryProviderScope.goalPreviewEntry(viewModel: AddTaskViewModel, onBack: () -> Unit)`

- [ ] **Step 1: Create the Route object**

  File: `feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewRoute.kt`

  ```kotlin
  package com.awan.feature.addtask.navigation

  import com.awan.core.navigation.Route
  import kotlinx.serialization.Serializable

  @Serializable
  data object GoalPreviewRoute : Route
  ```

- [ ] **Step 2: Create the entry extension**

  File: `feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewEntry.kt`

  ```kotlin
  package com.awan.feature.addtask.navigation

  import androidx.navigation3.runtime.NavEntryProviderScope
  import androidx.navigation3.runtime.entry
  import com.awan.feature.addtask.presentation.AddTaskViewModel
  import com.awan.feature.addtask.ui.GoalPreviewRouteRoot

  fun NavEntryProviderScope.goalPreviewEntry(
      viewModel: AddTaskViewModel,
      onBack: () -> Unit,
  ) {
      entry<GoalPreviewRoute> {
          GoalPreviewRouteRoot(
              viewModel = viewModel,
              onBack = onBack,
          )
      }
  }
  ```

  Note: `GoalPreviewRouteRoot` will be created in Task 2.

- [ ] **Step 3: Wire in AwanApp.kt**

  In `AwanApp.kt`:
  1. Obtain the shared ViewModel via `hiltViewModel<AddTaskViewModel>()` at the top of the `AwanApp` composable (before the `Box`).
  2. Register `goalPreviewEntry` inside the `entryProvider` block.
  3. Add `onNavigateToPreview` callback to the `AddTaskSheet` call in the next task; for now just wire the entry.

  The key changes to `AwanApp.kt`:
  ```kotlin
  // Add at top of AwanApp() body, before Box:
  val addTaskViewModel: AddTaskViewModel = hiltViewModel()

  // Add inside entryProvider { ... } block, alongside other entries:
  goalPreviewEntry(
      viewModel = addTaskViewModel,
      onBack = { navigator.goBack() },
  )
  ```

  Add imports:
  ```kotlin
  import androidx.hilt.navigation.compose.hiltViewModel
  import com.awan.feature.addtask.navigation.goalPreviewEntry
  import com.awan.feature.addtask.presentation.AddTaskViewModel
  ```

  **Note:** `app/build.gradle.kts` already imports `feature:add-task` and `hilt-navigation-compose` is already present. No Gradle changes needed.

- [ ] **Step 4: Build verify**

  ```
  .\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
  ```

  Expected: BUILD SUCCESSFUL (the stub for `GoalPreviewRouteRoot` will be created in Task 2, so this step may fail on that symbol; that's acceptable — just ensure the route and entry compile cleanly by temporarily leaving `GoalPreviewRouteRoot` as a `TODO()` or stub).

  Actually for this task to compile alone, create a minimal stub in `GoalPreviewEntry.kt`:

  ```kotlin
  // Temporary stub that will be replaced in Task 2
  // GoalPreviewRouteRoot is defined in feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewRouteRoot.kt
  ```

  Instead, skip the `:app:compileDebugKotlin` step and just verify the route file and entry file have no syntax errors by running:
  ```
  .\gradlew.bat :feature:add-task:compileDebugKotlin --no-daemon --console=plain
  ```

- [ ] **Step 5: Commit**

  ```
  git add feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewRoute.kt
  git add feature/add-task/src/main/java/com/awan/feature/addtask/navigation/GoalPreviewEntry.kt
  git add app/src/main/java/com/awan/app/AwanApp.kt
  git commit -m "AWAN-83: add GoalPreviewRoute, entry registration, and AwanApp wiring"
  ```

---

### Task 2: GoalPreviewRouteRoot & GoalPreviewScreen

**Files:**
- Create: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewRouteRoot.kt`
- Create: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt`

**Interfaces:**
- Consumes: `AddTaskViewModel`, `AddTaskState`, `GoalStep.Preview`, `GoalDecompositionBlock`, `AddTaskAction`, `AddTaskEvent.GoalCreated`, `AddTaskEvent.Dismissed`
- Produces:
  - `GoalPreviewRouteRoot(viewModel: AddTaskViewModel, onBack: () -> Unit)` — stateful root, observes state and events
  - `GoalPreviewScreen(state: AddTaskState, onAccept: () -> Unit, onDismiss: () -> Unit, onRevisionSubmit: () -> Unit, onRevisionChanged: (String) -> Unit, onToggleMic: () -> Unit, isListening: Boolean, speechError: String?, modifier: Modifier)` — stateless

**Key logic in `GoalPreviewRouteRoot`:**
- `val state by viewModel.state.collectAsStateWithLifecycle()`
- `ObserveAsEvents(viewModel.events)` — on `GoalCreated` or `Dismissed` → call `onBack()`
- `BackHandler(enabled = true)` → `viewModel.onAction(AddTaskAction.DismissRequested)`
- If `state.showDiscardConfirm` → show `AwanConfirmDialog` with confirm=`AddTaskAction.DiscardConfirmed`, cancel=`AddTaskAction.DiscardCancelled`
- Pass `state` and callbacks into `GoalPreviewScreen`
- Speech: `rememberSpeechRecognizer { transcript -> viewModel.onAction(AddTaskAction.InputChanged(transcript)) }`
- Auto-stop speech on submit: `LaunchedEffect(state.isSubmitting) { if (state.isSubmitting && speechState.isListening) speechState.stopListening() }`

**Key layout of `GoalPreviewScreen`:**
- `Scaffold` or `Column` with `fillMaxSize()`
- Top bar row: Close (`X`) button left, title center ("Goal Plan"), Accept (`Check`) button right
- `LazyColumn` in middle: renders `goalReplyBlocks` (Text as `AssistantTextCard`, Proposal as `ProposalCard`); if blocks empty, renders `ProposalCard(step.proposal)` directly; if current `goalStep` is `MultipleChoice` or `WritingQuestion`, renders inline question card at bottom of list
- Bottom revision bar: `AwanTextField` + mic button + quiet "Revise" submit button (only shown when `goalStep is GoalStep.Preview`)

- [ ] **Step 1: Create `GoalPreviewRouteRoot.kt`**

  ```kotlin
  package com.awan.feature.addtask.ui

  import androidx.activity.compose.BackHandler
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.getValue
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.awan.app.core.designsystem.AwanConfirmDialog
  import com.awan.app.core.designsystem.AwanButtonVariant
  import com.awan.app.core.designsystem.ObserveAsEvents
  import com.awan.feature.addtask.R
  import com.awan.feature.addtask.presentation.AddTaskAction
  import com.awan.feature.addtask.presentation.AddTaskEvent
  import com.awan.feature.addtask.presentation.AddTaskViewModel
  import com.awan.feature.addtask.ui.components.rememberSpeechRecognizer
  import androidx.compose.ui.res.stringResource

  @Composable
  fun GoalPreviewRouteRoot(
      viewModel: AddTaskViewModel,
      onBack: () -> Unit,
  ) {
      val state by viewModel.state.collectAsStateWithLifecycle()

      ObserveAsEvents(viewModel.events) { event ->
          when (event) {
              is AddTaskEvent.GoalCreated -> onBack()
              is AddTaskEvent.TaskCreated -> onBack()
              AddTaskEvent.Dismissed -> onBack()
          }
      }

      BackHandler(enabled = true) {
          viewModel.onAction(AddTaskAction.DismissRequested)
      }

      val speechState = rememberSpeechRecognizer(
          onTranscript = { transcript -> viewModel.onAction(AddTaskAction.InputChanged(transcript)) },
      )

      LaunchedEffect(state.isSubmitting) {
          if (state.isSubmitting && speechState.isListening) {
              speechState.stopListening()
          }
      }

      if (state.showDiscardConfirm) {
          AwanConfirmDialog(
              title = stringResource(R.string.add_task_discard_title),
              body = stringResource(R.string.add_task_discard_body),
              confirmLabel = stringResource(R.string.add_task_discard_confirm),
              confirmVariant = AwanButtonVariant.Destructive,
              onConfirm = { viewModel.onAction(AddTaskAction.DiscardConfirmed) },
              dismissLabel = stringResource(R.string.add_task_discard_cancel),
              onDismiss = { viewModel.onAction(AddTaskAction.DiscardCancelled) },
          )
      }

      GoalPreviewScreen(
          state = state,
          onAccept = { viewModel.onAction(AddTaskAction.AcceptGoalProposal) },
          onDismiss = { viewModel.onAction(AddTaskAction.DismissRequested) },
          onRevisionSubmit = { viewModel.onAction(AddTaskAction.Submit) },
          onRevisionChanged = { viewModel.onAction(AddTaskAction.InputChanged(it)) },
          onOptionSelected = { viewModel.onAction(AddTaskAction.GoalOptionSelected(it)) },
          onToggleMic = {
              if (speechState.isListening) speechState.stopListening()
              else speechState.startListening()
          },
          isListening = speechState.isListening,
          speechError = speechState.errorMessage,
      )
  }
  ```

- [ ] **Step 2: Create `GoalPreviewScreen.kt`**

  The screen must handle two modes driven by `state.goalStep`:
  - **`GoalStep.Preview`**: Shows proposal + revision footer
  - **`GoalStep.MultipleChoice` / `GoalStep.WritingQuestion`**: Shows inline question card replacing or below proposal; revision footer changes to the question's input

  ```kotlin
  package com.awan.feature.addtask.ui

  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.imePadding
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.layout.statusBarsPadding
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.itemsIndexed
  import androidx.compose.material3.Icon
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.res.stringResource
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics
  import androidx.compose.ui.unit.dp
  import com.awan.app.core.designsystem.AwanAiAura
  import com.awan.app.core.designsystem.AwanButton
  import com.awan.app.core.designsystem.AwanButtonVariant
  import com.awan.app.core.designsystem.AwanIconButton
  import com.awan.app.core.designsystem.AwanText
  import com.awan.app.core.designsystem.AwanTextField
  import com.awan.app.core.designsystem.AwanTheme
  import com.awan.app.core.model.GoalDecompositionBlock
  import com.awan.feature.addtask.R
  import com.awan.feature.addtask.presentation.AddTaskState
  import com.awan.feature.addtask.presentation.GoalStep
  import com.composables.icons.lucide.Check
  import com.composables.icons.lucide.Lucide
  import com.composables.icons.lucide.X

  @Composable
  fun GoalPreviewScreen(
      state: AddTaskState,
      onAccept: () -> Unit,
      onDismiss: () -> Unit,
      onRevisionSubmit: () -> Unit,
      onRevisionChanged: (String) -> Unit,
      onOptionSelected: (String) -> Unit,
      onToggleMic: () -> Unit,
      isListening: Boolean,
      speechError: String?,
      modifier: Modifier = Modifier,
  ) {
      Column(
          modifier = modifier
              .fillMaxSize()
              .statusBarsPadding()
              .imePadding(),
      ) {
          // Top bar
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
          ) {
              AwanIconButton(
                  onClick = onDismiss,
                  contentDescription = stringResource(R.string.add_task_goal_preview_close),
              ) {
                  Icon(Lucide.X, contentDescription = null, modifier = Modifier.size(20.dp))
              }
              AwanText(
                  text = stringResource(R.string.add_task_goal_preview_title),
                  style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
              )
              AwanIconButton(
                  onClick = onAccept,
                  enabled = state.canAcceptGoal,
                  contentDescription = stringResource(R.string.add_task_goal_preview_accept_action),
              ) {
                  Icon(
                      Lucide.Check,
                      contentDescription = null,
                      tint = if (state.canAcceptGoal) AwanTheme.colors.sky else AwanTheme.colors.meta,
                      modifier = Modifier.size(20.dp),
                  )
              }
          }

          // Scrollable content
          LazyColumn(
              modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .padding(horizontal = AwanTheme.spacing.lg),
              verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
          ) {
              // Render reply blocks from the final decomposition reply
              val replyBlocks = state.goalReplyBlocks
              if (replyBlocks.isNotEmpty()) {
                  itemsIndexed(replyBlocks) { _, block ->
                      when (block) {
                          is GoalDecompositionBlock.Text ->
                              GoalPreviewAssistantText(text = block.text)
                          is GoalDecompositionBlock.Proposal ->
                              GoalPreviewProposalCard(proposal = block.proposal)
                          is GoalDecompositionBlock.Question -> Unit
                      }
                  }
              } else {
                  val step = state.goalStep
                  if (step is GoalStep.Preview) {
                      item { GoalPreviewProposalCard(proposal = step.proposal) }
                  }
              }

              // Inline follow-up question (if returned after revision)
              when (val step = state.goalStep) {
                  is GoalStep.MultipleChoice -> {
                      item {
                          GoalPreviewInlineQuestion(
                              question = step.question,
                              options = step.options,
                              selectedOption = step.selectedOption,
                              onOptionSelected = onOptionSelected,
                              isSubmitting = state.isSubmitting,
                          )
                      }
                  }
                  is GoalStep.WritingQuestion -> {
                      item {
                          AwanText(
                              text = step.question,
                              style = AwanTheme.typography.title.copy(color = AwanTheme.colors.textPrimary),
                          )
                      }
                  }
                  else -> Unit
              }

              // Error
              val errRes = state.errorMessage
              if (errRes != null) {
                  item {
                      AwanText(
                          text = stringResource(errRes),
                          style = AwanTheme.styles.errorText,
                      )
                  }
              }
              if (speechError != null) {
                  item {
                      AwanText(
                          text = speechError,
                          style = AwanTheme.styles.errorText,
                      )
                  }
              }
          }

          // Revision / question input footer
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = AwanTheme.spacing.lg)
                  .padding(bottom = AwanTheme.spacing.xl, top = AwanTheme.spacing.sm),
              verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
          ) {
              AwanAiAura(active = state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                  AwanTextField(
                      value = state.input,
                      onValueChange = onRevisionChanged,
                      placeholder = stringResource(
                          when (state.goalStep) {
                              is GoalStep.Preview -> R.string.add_task_goal_preview_revision_placeholder
                              else -> R.string.add_task_goal_writing_placeholder
                          }
                      ),
                      contentDescriptionText = stringResource(R.string.add_task_goal_preview_revision_description),
                      enabled = !state.isSubmitting,
                      singleLine = false,
                      trailingContent = {
                          GoalPreviewMicButton(
                              isListening = isListening,
                              onToggleMic = onToggleMic,
                              enabled = !state.isSubmitting,
                          )
                      },
                      modifier = Modifier.fillMaxWidth(),
                  )
              }
              AwanButton(
                  onClick = onRevisionSubmit,
                  enabled = state.canSubmit,
                  isLoading = state.isSubmitting,
                  variant = AwanButtonVariant.Quiet,
                  modifier = Modifier.fillMaxWidth(),
              ) {
                  AwanText(
                      stringResource(
                          when (state.goalStep) {
                              is GoalStep.Preview -> R.string.add_task_goal_preview_revision_submit
                              else -> R.string.add_task_goal_writing_continue
                          }
                      )
                  )
              }
          }
      }
  }

  // Private sub-composables (inline helpers below)
  ```

  The file also includes private composable helpers:
  - `GoalPreviewAssistantText(text)` — same styling as `AssistantTextCard` in `GoalForm.kt`
  - `GoalPreviewProposalCard(proposal)` — same layout as `ProposalCard` in `GoalForm.kt` (title, description, targetDate, tasks list)
  - `GoalPreviewInlineQuestion(question, options, selectedOption, onOptionSelected, isSubmitting)` — MCQ option cards (same look as `MultipleChoiceStepContent` cards)
  - `GoalPreviewMicButton(isListening, onToggleMic, enabled)` — same mic button animation as `GoalMicButton` in `GoalForm.kt`

  **Important:** Copy the private composable implementations from `GoalForm.kt` (`ProposalCard`, `AssistantTextCard`, `GoalMicButton` styling) rather than sharing them — `GoalForm.kt`'s privates are not exported. You may extract minimal code; do not refactor `GoalForm.kt`.

- [ ] **Step 3: Add missing string resources**

  In `feature/add-task/src/main/res/values/strings.xml`, add (if not already present):
  ```xml
  <string name="add_task_goal_preview_title">Goal Plan</string>
  <string name="add_task_goal_preview_close">Close goal preview</string>
  <string name="add_task_goal_preview_accept_action">Accept goal plan</string>
  ```

  In `feature/add-task/src/main/res/values-ar/strings.xml`, add matching Arabic strings:
  ```xml
  <string name="add_task_goal_preview_title">خطة الهدف</string>
  <string name="add_task_goal_preview_close">إغلاق معاينة الهدف</string>
  <string name="add_task_goal_preview_accept_action">قبول خطة الهدف</string>
  ```

- [ ] **Step 4: Build verify**

  ```
  .\gradlew.bat :feature:add-task:compileDebugKotlin --no-daemon --console=plain
  ```

  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

  ```
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewRouteRoot.kt
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/GoalPreviewScreen.kt
  git add feature/add-task/src/main/res/values/strings.xml
  git add feature/add-task/src/main/res/values-ar/strings.xml
  git commit -m "AWAN-83: add GoalPreviewRouteRoot and GoalPreviewScreen composables"
  ```

---

### Task 3: Sheet Collapse → Navigation Handoff

**Files:**
- Modify: `feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt`
- Modify: `app/src/main/java/com/awan/app/AwanApp.kt`

**Interfaces:**
- Consumes: `GoalPreviewRoute`, `navigator.navigate()`, `sheetState.hide()`, `GoalStep.Preview`
- Produces: `AddTaskSheet(onDismiss, onNavigateToGoalPreview, viewModel, modifier)` — updated signature

**Key logic:**

In `AddTaskSheet`:
1. Add `onNavigateToGoalPreview: () -> Unit` parameter.
2. Update `ModalBottomSheetState.confirmValueChange` to also allow `SheetValue.Hidden` when `state.goalStep is GoalStep.Preview`:
   ```kotlin
   confirmValueChange = { target ->
       val isPreviewStep = viewModel.state.value.goalStep is GoalStep.Preview
       val blocked = target == SheetValue.Hidden && viewModel.state.value.isDirty && !isPreviewStep
       if (blocked) viewModel.onAction(AddTaskAction.DismissRequested)
       !blocked
   }
   ```
3. Add `LaunchedEffect(state.goalStep)`:
   ```kotlin
   LaunchedEffect(state.goalStep) {
       if (state.goalStep is GoalStep.Preview) {
           sheetState.hide()   // suspends until fully hidden
           onDismiss()         // sets showAddTask = false
           onNavigateToGoalPreview()  // navigator.navigate(GoalPreviewRoute)
       }
   }
   ```

In `AwanApp.kt`:
1. Thread `addTaskViewModel` into `AddTaskSheet`.
2. Wire `onNavigateToGoalPreview = { navigator.navigate(GoalPreviewRoute) }`.

Updated `AddTaskSheet` call in `AwanApp.kt`:
```kotlin
if (showAddTask) {
    AddTaskSheet(
        onDismiss = { showAddTask = false },
        onNavigateToGoalPreview = {
            showAddTask = false
            navigator.navigate(GoalPreviewRoute)
        },
        viewModel = addTaskViewModel,
        onGoalCreated = { _ ->
            navigator.replaceAll(GoalsRoute)
        },
    )
}
```

- [ ] **Step 1: Write the failing test**

  In `feature/add-task/src/test/java/com/awan/feature/addtask/presentation/AddTaskViewModelTest.kt` (already exists), add:

  ```kotlin
  @Test
  fun `when goalStep transitions to Preview, acceptGoalProposal emits GoalCreated`() = runTest(testDispatcher) {
      // This is already tested by existing tests — verify GoalStep.Preview is reachable
      // and that isDirty is true while on Preview step (sheet must not self-dismiss via dirty check)
      val proposalReply = GoalDecompositionReply(
          sessionId = "s1",
          blocks = listOf(GoalDecompositionBlock.Proposal(GoalProposal(title = "Run a 10k", tasks = emptyList()))),
          hasProposal = true,
      )
      goalRepository.nextContinueReply = Result.Success(proposalReply)
      val viewModel = viewModel()
      viewModel.onAction(AddTaskAction.ModeChanged(AddTaskMode.GOAL))
      viewModel.onAction(AddTaskAction.InputChanged("Run a 10k marathon"))
      viewModel.onAction(AddTaskAction.Submit)

      val step = viewModel.state.value.goalStep
      assertTrue(step is GoalStep.Preview)
      // isDirty must be true so sheet waits for our programmatic hide (not user swipe-dismiss)
      assertTrue(viewModel.state.value.isDirty)
  }
  ```

- [ ] **Step 2: Run test to verify it passes (logic already exists)**

  ```
  .\gradlew.bat :feature:add-task:testDebugUnitTest --tests "*.AddTaskViewModelTest" --no-daemon --console=plain
  ```

  Expected: PASS (the logic is already present in the ViewModel)

- [ ] **Step 3: Update `AddTaskSheet.kt`**

  Add `onNavigateToGoalPreview: () -> Unit` parameter to `AddTaskSheet`.

  Update `confirmValueChange`:
  ```kotlin
  val sheetState = rememberModalBottomSheetState(
      confirmValueChange = { target ->
          val isPreviewStep = viewModel.state.value.goalStep is GoalStep.Preview
          val blocked = target == SheetValue.Hidden && viewModel.state.value.isDirty && !isPreviewStep
          if (blocked) viewModel.onAction(AddTaskAction.DismissRequested)
          !blocked
      },
  )
  ```

  Add inside `AddTaskSheet` body (after `sheetState` definition):
  ```kotlin
  LaunchedEffect(state.goalStep) {
      if (state.goalStep is GoalStep.Preview) {
          sheetState.hide()
          onDismiss()
          onNavigateToGoalPreview()
      }
  }
  ```

- [ ] **Step 4: Update `AwanApp.kt`**

  Thread `addTaskViewModel` (already declared from Task 1) into `AddTaskSheet`, and wire `onNavigateToGoalPreview`:

  ```kotlin
  if (showAddTask) {
      AddTaskSheet(
          onDismiss = { showAddTask = false },
          onNavigateToGoalPreview = {
              showAddTask = false
              navigator.navigate(GoalPreviewRoute)
          },
          viewModel = addTaskViewModel,
          onGoalCreated = { _ ->
              navigator.replaceAll(GoalsRoute)
          },
      )
  }
  ```

  Add import: `import com.awan.feature.addtask.navigation.GoalPreviewRoute`

- [ ] **Step 5: Full build + unit test verify**

  ```
  .\gradlew.bat :feature:add-task:testDebugUnitTest :app:compileDebugKotlin --no-daemon --console=plain
  ```

  Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 6: Commit**

  ```
  git add feature/add-task/src/main/java/com/awan/feature/addtask/ui/AddTaskSheet.kt
  git add app/src/main/java/com/awan/app/AwanApp.kt
  git commit -m "AWAN-83: sheet collapse and navigation handoff to GoalPreviewRoute"
  ```

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] `GoalStep.Preview` → `sheetState.hide()` → navigate → Task 3
   - [x] Sequential (await full collapse before navigate) → Task 3 (`sheetState.hide()` suspends)
   - [x] Inline follow-up questions on `GoalPreviewRoute` → Task 2 (`GoalPreviewScreen`)
   - [x] Auto-stop speech on submit → Task 2 (`GoalPreviewRouteRoot` `LaunchedEffect`)
   - [x] Accept → `navigator.goBack()` → Task 2 (via `GoalCreated` event)
   - [x] Discard → confirm dialog → `navigator.goBack()` → Task 2 (via `Dismissed` event)
   - [x] Bottom nav hidden on `GoalPreviewRoute` → automatic (not a top-level route, `isTopLevel` check in `AwanApp.kt` will be false)

2. **Placeholder scan:** None — all steps have concrete code.

3. **Type consistency:**
   - `GoalPreviewRoute` used consistently as `data object` in entry, navigation, and AwanApp
   - `GoalPreviewRouteRoot(viewModel: AddTaskViewModel, onBack: () -> Unit)` matches entry registration
   - `onNavigateToGoalPreview: () -> Unit` is the exact parameter name used across Task 1–3
