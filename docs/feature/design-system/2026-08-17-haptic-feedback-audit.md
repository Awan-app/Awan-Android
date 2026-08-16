# Haptic feedback audit plan

## Scope

Audit the interactive Compose controls that bypass Awan's haptic-enabled buttons, then add purposeful feedback to navigation, selections, committed actions, and the daily wheel.

## Changes

1. Reuse the design-system haptic mapping for custom interactive surfaces rather than introducing device vibration APIs.
2. Add tab and add-task feedback to the shared bottom bar, so every top-level navigation action has one light acknowledgement.
3. Add feedback to high-value selection, expansion, and destructive controls that use raw `clickable`/`selectable` modifiers.
4. Keep the wheel's peg ticks tied to actual segment crossings during its animation, retain reduced-motion silence, and add one confirmed landing cue.

## Verification

Run the existing design-system test class first, then compile the affected modules with `./gradlew assembleDebug` and run lint if the environment permits.

## Implementation notes (what actually differed)

- Reused Compose's `LocalHapticFeedback` through `rememberHapticClick`, so raw interactive surfaces stay aligned with the button haptic mapping and device accessibility settings.
- Added feedback to the shared bottom bar, cards, dropdowns, schedule cards, calendar days, selection controls, task/date steppers, and the feature-specific action surfaces identified by the audit.
- The wheel already ticked on each segment crossing. It now confirms once when it lands and no longer sends the press confirmation twice. Reduced-motion mode remains silent while it is animating.
- Verified with `./gradlew :core:design-system:testDebugUnitTest`, affected-module Kotlin compilation, and `./gradlew assembleDebug lint`.
