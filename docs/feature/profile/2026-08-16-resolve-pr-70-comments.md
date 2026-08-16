# Resolve PR #70 Review Comments Plan

- **Date**: 2026-08-16
- **Feature**: Profile & Onboarding — User Info Updates
- **Jira Issue**: AWAN-185
- **PR**: https://github.com/Awan-app/Awan-Android/pull/70

## Goal

Resolve code review comments on PR #70:
1. **Comment 1**: Move input normalization out of `ProfileViewModel` so the presentation layer passes raw user input to use cases, keeping trimming and API sentinel mapping at the data layer boundary.
2. **Comment 2**: Decouple profile name update and profile picture operations into independent actions in `ProfileViewModel` and `ProfileState`, avoiding reporting the entire save as failed when one operation succeeds and the other fails, and avoiding redundant network requests when inputs have not changed.

## Architectural Changes

1. **`ProfileViewModel.kt`**:
   - Removed `.trim()` calls on `firstName` and `lastName` in `updatePersonalInfo`, delegating string normalization and sentinel mapping (`toApiLastName()`) to `ProfileRepositoryImpl`.
   - Decoupled name update and photo update/delete flows:
     - Check if name actually changed (`firstName.trim() != currentProfile.firstName` or `lastName.sanitizeLastName() != currentProfile.lastName.sanitizeLastName()`). If unchanged, skip name use case.
     - Execute name update and picture operation independently.
     - Handle partial success states: clear `pendingPicture` if photo succeeded; retain `pendingPicture` if photo failed; reflect accurate `fieldError` without losing already-persisted profile data.
     - Added `ProfileAction.DismissEditSheet` to cleanly reset `pendingPicture` and `fieldError` upon sheet dismissal.
2. **`ProfileScreen.kt` & `ProfileAction.kt`**:
   - Added `ProfileAction.DismissEditSheet` triggered when `EditPersonalInfoSheet` is dismissed.
   - Updated `isLoading` to reflect `isUpdatingField || isUploadingPicture`.
3. **`ProfileViewModelTest.kt`**:
   - Added unit test suite in `feature:profile:impl` covering raw input forwarding, independent success/failure states, and sheet dismissal cleanup.

## Implementation notes (what actually differed)

- **Build / Test Status**:
  - Executed `./gradlew testDebugUnitTest` across all modules: all unit tests passed.
  - Added and executed 12 unit tests in `ProfileViewModelTest` verifying:
    - Raw input forwarding without ViewModel trimming.
    - No-op dismissal when neither name nor photo changed.
    - Independent success and failure combinations (name ok + picture fail, name fail + picture ok, both ok, both fail).
    - Sheet dismiss state cleanup.
- **Deviations**: None.
