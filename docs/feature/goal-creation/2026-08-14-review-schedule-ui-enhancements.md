# Zone Time-Matching Filter Plan

**Date:** 2026-08-14

## Goal

Filter available zones in the Task Details bottom sheet (`TaskDetailBottomSheet`) dynamically based on the user-chosen task date and time window (`startMinutes` to `endMinutes`). Only zones that encompass the task time (`taskStart >= zone.startTime && taskEnd <= zone.endTime`) will be displayed alongside the `"No zone"` option.

## Architecture

- **Clean Architecture & MVI:** Presentation in `:feature:goals:impl` (`GoalScheduleEditorScreen`, `GoalScheduleEditorViewModel`, `GoalScheduleEditorMvi`).
- **Domain Layer:** Consumes `DayZone` (`startTime`, `endTime`) provided by `GetZonesForDateUseCase`.

## Proposed Changes

1. **`GoalScheduleEditorScreen.kt`**:
   - Filter `availableZones` to only `eligibleZones` where `placement.startMinutes >= zoneStart && placement.endMinutes <= zoneEnd`.
   - Render "No zone" chip + `eligibleZones` chips.
2. **`GoalScheduleEditorViewModel.kt`**:
   - Automatically clear `zoneId = null` when user modifies date or times such that the currently chosen zone is no longer eligible.
   - Enforce zone eligibility check in `TaskDetailSaved`.
3. **`GoalScheduleEditorViewModelTest.kt`**:
   - Unit tests covering zone filtering and automatic zone reset.

## Verification

- Run `./gradlew :feature:goals:impl:testDebugUnitTest`
- Run `./gradlew assembleDebug`
