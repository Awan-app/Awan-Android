# Latched goal image source disclosure

## Goal

Replace the goal image source dialog with a tactile disclosure button that stays pressed while a themed Camera/Gallery surface unfolds from behind it.

## Implementation

- Add a controlled `AwanDisclosure` to `:core:design-system` that reuses the existing button rim geometry, owns the attached surface overlap/animation, and respects reduced motion.
- Keep `GoalImagePicker` responsible for its local expanded state and existing Camera/Photo Picker launchers; keep image selection, thumbnail, removal, and backend behavior unchanged.
- Arrange exactly two equal-width Camera and Gallery actions horizontally, with localized expanded/collapsed accessibility state descriptions.

## Verification

- Add design-system Compose tests for hidden/revealed content, toggle semantics, and touch-target behavior.
- Extend the goal form Compose test to verify the two source actions share a horizontal row and the trigger toggles closed.
- Run focused instrumented tests when a device/emulator is available, then `:feature:add-task:testDebugUnitTest :app:assembleDebug`.

## Implementation notes (what actually differed)

- Design-system and add-task Kotlin/androidTest compilation passed; `:feature:add-task:testDebugUnitTest :app:assembleDebug` passed.
- Connected Compose execution was attempted but the attached Redmi device blocked test APK installation (`INSTALL_FAILED_USER_RESTRICTED`); an earlier add-task runner was also killed by the device, so no connected UI result is claimed.
- `AwanStyles.kt` did not need a change: the disclosure reuses the existing `AwanButton` rim constants and adds only the controlled latched face offset.
