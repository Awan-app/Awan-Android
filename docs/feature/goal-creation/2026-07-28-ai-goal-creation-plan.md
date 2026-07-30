# AI goal creation in Quick Add

## Outcome

Replace the Goal placeholder in the existing Quick Add sheet with a four-state AI-assisted flow
that uses the same design language as task creation:

1. **Initial** — the user enters or dictates a goal and sends it to the AI.
2. **Multiple choice** — the AI asks one question and presents selectable answers.
3. **Writing question** — the AI asks one question and the user enters or dictates an answer.
4. **Preview** — the AI presents a proposed goal and task structure. The user can accept it or
   enter or dictate a revision and send that revision through the same AI session.

Network progress is an overlay on the current state, not a fifth state. Closing the sheet abandons
the in-memory session; v1 does not restore unfinished sessions.

## Existing code to reuse

- Keep the center Quick Add entry point, `AddTaskSheet`, Task/Goal mode selector, theme tokens,
  controls, and sheet motion from `:feature:add-task`.
- Extend the existing Goals domain/data/presentation vertical from AWAN-83; do not create a
  parallel repository or a second goal feature module.
- Keep the sheet in the single `:feature:add-task` module because it is app-owned modal UI, not a
  Navigation 3 destination.
- ViewModels depend on use cases only. Retrofit DTOs stay in `:core:network`, mapping and repository
  implementations stay in `:core:data`, contracts/use cases stay in `:core:domain`, and UI state
  stays in `:feature:add-task`.
- Use existing design-system controls and the already installed icon surface. If a new icon is
  necessary, use the project's Lucide icon pack; do not introduce another icon dependency.

## Backend contract

Source: `docs/feature/backend/AWAN_API_DOCUMENTATION.md`.

### Continue decomposition

`POST /v1/ai/goal-decompose`

```json
{
  "sessionId": null,
  "message": "Learn Spanish well enough for a trip"
}
```

Subsequent messages reuse the returned `sessionId`. The response contains ordered `blocks`:

- `text`: assistant copy.
- `question`: question copy plus `options`. Non-empty options render the multiple-choice state;
  empty or absent options render the writing-question state.
- `proposal`: title, description, target date, and proposed tasks.

`hasProposal` plus a valid proposal block renders Preview. Unknown block types must not crash the
flow; ignore them while preserving supported blocks.

### Accept proposal

`POST /v1/ai/goal-decompose/{sessionId}/confirm`

The response is the created goal. Success dismisses Quick Add and replaces the top-level stack with
`GoalsRoute` so the Goals screen reloads and includes the new goal.

Transcript and server-side cancel endpoints are outside this pass. Speech-to-text uses the native
Android recognizer and still sends ordinary text through the endpoints above.

## Domain and data

- Add small domain models for a decomposition reply, question, proposal, and proposed task.
- Extend the existing `GoalRepository` with:
  - `continueDecomposition(sessionId: String?, message: String)`
  - `confirmDecomposition(sessionId: String)`
- Add thin goal use cases for those two operations.
- Extend `GoalApiService` and the existing goal remote data source/repository implementation.
- Map transport errors through the shared typed `Result`/`AppError` path and preserve coroutine
  cancellation.
- Do not add local persistence or a generic chat/session abstraction; neither is required by this
  flow.

## Presentation state and actions

`AddTaskState` gains a goal sub-state with:

- `Initial(input)`
- `MultipleChoice(question, options, selectedOption?)`
- `WritingQuestion(question, input)`
- `Preview(proposal, revisionInput)`

The goal state also carries `sessionId`, assistant supporting copy, `isSubmitting`, and a
localized error. Selection and text edits are local. Sending the initial goal, an MCQ selection, a
written answer, or a preview revision calls the same continue-decomposition use case. Accept is
enabled only with a valid session and calls confirm.

One-shot success remains an `AddTaskEvent`; the app owns dismissal and Navigation 3 mutation.
Errors stay on the current state so input, session, and proposal are not lost and retry is possible.

## Speech-to-text

- Use Android `SpeechRecognizer` through a thin Compose-owned helper; never put `Context` or the
  recognizer in the ViewModel.
- Request `RECORD_AUDIO` at the point of use.
- The mic is available for Initial, Writing Question, and Preview revision; MCQ is tap-only.
- Recognition fills the editable field and never auto-sends.
- Destroy the recognizer on disposal. Handle unavailable, denied, cancelled, and recognition-error
  states with localized, accessible feedback.

## UI and motion

- Match task creation's bottom-sheet composition, spacing, surfaces, typography, color roles, and
  primary action hierarchy.
- Use `AnimatedContent` for state changes with a short fade/vertical transition; content size
  animates with the sheet rather than jumping.
- Choice cards use a restrained press/selection spring. The send/accept action uses existing button
  loading behavior.
- The mic has clear idle/listening states and a subtle listening pulse. AI processing may reuse the
  existing aura/working treatment.
- Motion must preserve spatial continuity and must respect reduced-motion settings: replace travel,
  scale, pulse, and repeated animation with an immediate or short crossfade when reduced motion is
  active.
- Every interactive target has a content description or text label, a minimum 48 dp target, useful
  selected/disabled semantics, and keyboard/screen-reader-safe ordering.
- Add all visible strings in English and Arabic with the `add_task_goal_` prefix.

## Tests and verification

Write behavior tests before each production slice:

1. Goal DTO/mapper/repository tests:
   - serializes a first message with `sessionId = null` and a continuation with the returned id;
   - maps text, MCQ, writing question, and proposal blocks;
   - ignores unknown blocks safely;
   - confirms the exact session;
   - maps backend/transport failures and rethrows cancellation.
2. `AddTaskViewModelTest`:
   - starts in Initial;
   - transitions Initial → MCQ → Writing → Preview using one session;
   - sends the selected option and written answer;
   - keeps current content/session on failures and supports retry;
   - preview revision returns through AI and updates Preview;
   - accept emits success exactly once and duplicate taps are ignored while loading;
   - mode switching does not corrupt the Task flow.
3. Compose UI tests:
   - each of the four states exposes the correct controls and semantics;
   - MCQ selection and preview accept/revision dispatch the expected actions;
   - mic controls are absent from MCQ and present in text-entry states.
4. Verification gates:
   - focused core-data and add-task unit tests;
   - goals feature tests;
   - `:app:assembleDebug`;
   - lint/MissingTranslation;
   - device/emulator Compose and speech smoke checks when a target is available.

## Delegation and review

Implementation is split into bounded Antigravity passes: existing Goals baseline, domain/data
contract, ViewModel state machine/tests, and Compose/speech/motion/tests. Each pass receives its own
detailed brief and may use only Gemini 3.6 Flash High or Claude Sonnet 4.6. Antigravity does not
commit. The tech lead reviews every production and test diff, requests corrections in the same
conversation, runs independent verification, and only then accepts the pass.

## Exclusions

- No offline goal cache or sync work.
- No server transcript/history UI.
- No resume-after-dismiss.
- No custom speech service or new animation/icon library.
- No AI-generated start-time scheduling; the proposal mirrors the backend contract and existing
  local scheduling boundaries remain unchanged.

## Implementation notes (what actually differed)

- Implemented the four-state flow in the existing Quick Add MVI, backed by the existing Goals
  domain/data vertical and the documented goal-decomposition endpoints. Goal success replaces the
  top-level stack with `GoalsRoute`.
- Added native `SpeechRecognizer` input for Initial, Writing Question, and Preview revisions,
  localized English/Arabic UI, Lucide Android icons, accessibility semantics, reduced-motion
  fallbacks, and behavior-focused ViewModel/data/Compose tests.
- Fresh verification passed:
  - `.\gradlew.bat :core:data:testDebugUnitTest :feature:goals:impl:testDebugUnitTest :feature:add-task:testDebugUnitTest --rerun-tasks --no-daemon --console=plain`
    (`BUILD SUCCESSFUL`, 174 tasks executed).
  - `.\gradlew.bat :feature:add-task:compileDebugAndroidTestKotlin :core:design-system:lintDebug :app:assembleDebug --no-daemon --console=plain`
    (exit 0).
  - After removing temporary device diagnostics,
    `.\gradlew.bat :feature:add-task:compileDebugAndroidTestKotlin --rerun-tasks --no-daemon --console=plain`
    (exit 0).
  - `git diff --check` passed; only line-ending conversion warnings were reported.
- A connected-device Compose run completed 6 of 8 tests with zero failures, then the Android test
  runner stalled before entering the next test body. Per user direction, connected tests were
  skipped after investigation; all temporary diagnostic edits and processes were removed. Native
  microphone/permission behavior therefore remains a manual device smoke check.
- The work remains uncommitted because the repository requires a known Jira issue ID before commit
  or push, and no matching ID was provided.

- Follow-up user direction intentionally extends the original MCQ "tap-only" scope: proposed-answer
  questions now also show a localized custom-answer text field with the existing native speech control.
  It reuses the shared `AddTaskState.input`: typed nonblank text clears the card choice, choosing a
  card clears the text, and continuation sends trimmed custom text before a selected card.
- Fresh follow-up verification passed:
  - `.\gradlew.bat :feature:add-task:testDebugUnitTest --tests '*AddTaskViewModelTest*' --rerun-tasks --no-daemon --console=plain`
    (exit 0).
  - `.\gradlew.bat :feature:add-task:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon --console=plain`
    (exit 0).
  - Connected instrumentation tests remain intentionally skipped per user direction; only their Kotlin
    sources were compiled.
