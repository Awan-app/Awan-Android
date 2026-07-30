# Full-screen AI Goal Preview Design

**Status:** Approved on 2026-07-29
**Scope:** Replace only the AI Goal Preview phase of Quick Add; Initial, multiple-choice, and writing phases remain in the Quick Add bottom sheet.

## Goal

Give the proposed goal and all of its tasks a full mobile viewport without losing the existing AI session, native speech input, discard protection, or Awan visual language.

## Chosen boundary

Use an app-owned full-screen Quick Add overlay, not a Navigation 3 route and not a full-screen dialog.

- Preview is a transient state of the existing Quick Add flow, not an independently restorable destination.
- The existing AddTaskViewModel remains the sole MVI owner of the session, proposal, revision text, submission state, speech transcript action, and discard state.
- A Navigation 3 route would require passing or retaining the active session across a route boundary without adding a user-facing capability. It is intentionally out of scope.
- The app boundary already owns the Quick Add modal. It will place the modal and the full-screen preview above the app Scaffold in an explicit Box, so the preview is truly edge-to-edge and can cover the bottom bar.

## UI composition

1. Introduce a public AddTaskFlow host in feature:add-task. It owns the Hilt AddTaskViewModel, collects immutable state, observes one-shot events, owns the native speech recognizer, and renders the shared discard dialog and pickers.
2. Keep the existing ModalBottomSheet for Task mode and Goal Initial, MultipleChoice, and WritingQuestion states.
3. When `state.mode == GOAL` and `state.goalStep is Preview`, render a new GoalPreviewScreen instead of the bottom sheet. It receives state plus action and speech callbacks; it has no ViewModel, repository, navigation stack, or mutable business state.
4. Move the existing sheet-only host internals behind AddTaskFlow. GoalFormContent remains the stateless bottom-sheet content; its speech props come from AddTaskFlow so the same recognizer fills both the sheet and the preview composer.
5. Wrap the app Scaffold and AddTaskFlow in an app-owned Box, with AddTaskFlow emitted after Scaffold so the full-screen preview draws above application chrome. No NavKey, entry provider, navigator mutation, backend, or dependency is added.

## Full-screen preview layout

- Reuse the animated CloudDrift, mascot expression crossfade, and celebration burst from the sheet header through a shared GoalFlowSkyHeader composable. The screen variant omits the drag handle.
- Put Close on the left and the compact existing Accept action on the right of that header. Close and system Back both dispatch DismissRequested; the existing dirty-state confirmation dialog warns before any session is abandoned.
- Render assistant supporting text, proposal title, description, target date, ordered task rows, duration, and points in a LazyColumn. The list gets the full viewport below the compact header.
- Put the existing AwanAiAura revision field, speech mic, and quiet Update preview action after the task items. This preserves the established revision request and keeps dense proposal data as the priority.
- Render localized error feedback as an accessible polite live region within the screen. Disable Accept, text input, mic, and revision submit while the continuation or confirmation request is active.
- Reuse existing strings where they fit; add only localized English and Arabic labels required for the full-screen title and Close control. All new keys retain the add_task_goal_ prefix.

## Motion and accessibility

- Proposal entry uses a restrained forward shared-axis treatment: opacity plus a short upward translation using the existing Awan motion tokens. It communicates forward progress without pretending the sheet surface physically morphs across containers.
- With reduced motion, replace that travel with a short crossfade. Reuse the design-system reducedMotion check; do not run looping or scale motion beyond the existing CloudDrift behavior, which already rests under reduced motion.
- When a revision returns a new proposal, retain the full-screen surface, reset the list to the proposal title, and animate only the changed proposal content. Do not replay the screen-entry sequence or dispose and recreate the speech controller.
- Preserve visible labels, 48 dp interactive controls through existing design-system components, logical focus order, null descriptions for decorative icons, and RTL-safe layout. The Accept action is not duplicated in a persistent bottom bar.

## State, errors, and exit

- No new AddTaskAction, GoalStep property, repository call, data-transfer object, or persisted session state is introduced.
- Accept continues to dispatch AcceptGoalProposal; revision continues to update InputChanged and dispatch Submit. Existing ViewModel retry and error behavior remains authoritative.
- A preview dismissal invokes the existing DismissRequested path. Because the goal session makes state dirty, it opens the existing discard confirmation. Confirming discards and closes the flow; cancelling leaves the full-screen preview unchanged.
- A successfully accepted goal still closes Quick Add and replaces the app root with GoalsRoute through the existing app callback.

## Tests and verification

- Write Compose semantics tests before production code for the full-screen preview: all proposal fields and task metadata render, Close and system Back request dismissal, Accept dispatches AcceptGoalProposal, revision text and mic dispatch the existing actions, submission disables controls, and refreshed proposals remain on the screen.
- Preserve existing AddTaskViewModel tests for accept, revision, retry, and discard behavior; add a local JVM test only if the production state contract changes.
- Run `.\gradlew.bat :feature:add-task:testDebugUnitTest --no-daemon --console=plain`, `.\gradlew.bat :feature:add-task:compileDebugAndroidTestKotlin --no-daemon --console=plain`, and `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain` with ANDROID_HOME set to the installed Android SDK.
- Per user direction, do not run connected instrumentation tests. Compile their Kotlin sources only.

## Exclusions

- No Navigation 3 destination, deep link, session restoration, transcript, offline cache, backend contract change, new icon library, new speech service, or design-system dependency.
- No task editing inside the preview; a revision remains a natural-language request to the existing AI continuation endpoint.

## Documentation and delivery

- This design is an approved follow-up to `2026-07-28-ai-goal-creation-plan.md`.
- The executable implementation plan will be a new dated document in this same feature folder. The prior plan remains immutable except for its existing implementation notes.
- No commit is made until a matching AWAN Jira issue ID is known, as required by this repository.
