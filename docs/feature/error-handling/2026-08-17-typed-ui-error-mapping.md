# Typed UI error mapping

## Problem

The network layer already maps offline/IO failures to `AppError.Network`, timeouts to
`AppError.Timeout`, and HTTP 5xx responses to `AppError.Server`. Several feature ViewModels discard
that distinction and always render copy telling the user to check their connection.

## Plan

- Reuse `:core:common` error resources for genuine network, timeout, unauthorized, serialization,
  and server failures.
- Keep feature-specific fallback copy for operation/API failures, but remove connection blame from
  those generic fallbacks.
- Show the existing AI-unavailable message for AI HTTP 503 failures.
- Fix Task Details and Daily Zones so timeout is not mapped to their network-only message.
- Add focused ViewModel regression coverage for offline, 500, 503, timeout, and failed acceptance.

## Verification

- `./gradlew :feature:add-task:testDebugUnitTest :feature:ai-tasks:impl:testDebugUnitTest`
- `./gradlew :feature:task-details:impl:testDebugUnitTest :feature:profile:impl:testDebugUnitTest`
- `./gradlew assembleDebug lint`

## Implementation notes (what actually differed)

- The transport mapper needed no change: it already distinguishes connectivity, timeout, and HTTP
  server failures. The incorrect connection copy was confined to feature presentation mappings.
- Add Task, AI Tasks, Task Details, and Daily Zones now preserve typed errors. AI HTTP 503 failures
  use the localized AI-unavailable message; other 5xx responses use the shared server message.
- Generic operation failures no longer tell the user to check their connection. Connection wording
  is reserved for `AppError.Network`.
- The four focused feature test suites passed, including regression cases for 500, 503, timeout,
  offline, and failed acceptance. `./gradlew assembleDebug lint` also passed. No device smoke test was
  run.
