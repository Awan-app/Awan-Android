# Onboarding account setup must land before the flow is left

## Problem

`completeOnboarding` + `createWeeklyTemplate` are the only calls that configure the account
server-side, and onboarding is the only place they are ever made — `AwanApp` routes a logged-in
user from splash straight to Home and never re-checks `onboardingCompleted`. Once the flow is
left, there is no second chance.

Three paths left it without the account configured:

1. **A failed request counted as done.** `submitOnboarding()` set `isBackendOnboarded = true`
   unconditionally, including on `Result.Error`. A network hiccup on the TaskLength step meant the
   later exit paths (`finishOnboarding`) saw the flag, skipped resubmitting, and navigated Home with
   nothing sent. Silent — no error surfaced anywhere.
2. **A failed template was never retried.** The template was gated on `completeOnboarding` succeeding
   *within the same call*. If onboarding landed and the template call failed, the flag was still set,
   so no later path retried it — the user reached Home with no zone windows for the engine to place
   into.
3. **Denying the notification permission did nothing.** `NotificationPermissionResult(granted = false)`
   mapped to `Unit`: the primary button on the last step was dead, so the final retry opportunity
   was unreachable via that button.

## Change

- Track `isBackendOnboarded` and `isTemplateCreated` separately, each set only on `Result.Success`.
  `submitOnboarding()` self-guards on both and returns whether the account is fully configured, so
  every exit path can call it unconditionally and retries only the half that has not landed.
- A failure sets `OnboardingState.setupError` and blocks the exit — no step change, no
  `NavigateHome`. The step's primary button is the retry; tapping it re-runs the missing call.
- `skipSetup()` and `finishOnboarding()` collapsed into one; `SkipSetup` routes to
  `finishOnboarding()`. The shared `submitting {}` helper owns the `isSubmittingTask` toggle and
  clears the previous error.
- Either notification permission answer now finishes onboarding — the permission is optional, the
  account setup is not.
- `StepScaffold` gained a `notice: String?` param rendered as an `InlineNotice(Error)` above the
  primary button, fed from `state.setupError`. Routed through the scaffold rather than `StepChrome`
  so the error shows on whichever step the failure happened without touching all seven branches.

No new strings: `AppError.toUiText()` already covers every failure mode.

## Verification

- `./gradlew :feature:onboarding:impl:testDebugUnitTest` — 17 pass. Three new:
  a failed setup holds the flow and the next exit path retries it; a failed template is retried
  without resending `completeOnboarding`; denying the notification permission still completes the
  account setup.
- `./gradlew assembleDebug` — successful.

## Implementation notes (what actually differed)

Nothing differed from the plan above; it was written against the finished change.

The trap worth remembering: the old code read as correct because both named paths (Welcome
"skip setup" and the TaskLength skip) *did* call `submitOnboarding()`. The bug was one line inside
it — an unconditional `isBackendOnboarded = true` — which turned every retry into a no-op. If a
future change adds another exit path, it only needs to `submitOnboarding()` and honour the boolean;
it must not add its own `if (!isBackendOnboarded)` guard, or the same class of bug returns.
