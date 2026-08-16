# Profile and Onboarding User Info Updates Plan

- **Date**: 2026-08-16
- **Feature**: Profile & Onboarding — User Info Updates
- **Jira Issue**: AWAN-185

## Goal

1. Remove birth date completely from the user interface (specifically from `EditPersonalInfoSheet` in the profile module) while providing a static default (`"2000-01-01"`) for backend requests where required.
2. Make last name optional in the UI and business logic. When last name is empty or omitted, send a constant sentinel value (`"not entered"`) to satisfy backend requirements, and sanitize this sentinel across the application (mappers, caching, UI) to treat it as null/empty locally.
3. Default first name to `"Friend"` (with capital F) when the onboarding name step is skipped or onboarding setup is skipped entirely, sending `"Friend"` to the backend.

## Architectural Strategy

1. **`:core:model`**:
   - Create `com.awan.app.core.model.ProfileConstants` with:
     - `const val UNSET_LAST_NAME = "not entered"`
     - `const val DEFAULT_FIRST_NAME_FRIEND = "Friend"`
     - `const val DEFAULT_BIRTH_DATE = "2000-01-01"`
     - Extensions `String?.sanitizeLastName()` and `String?.toApiLastName()`.

2. **`:core:data`**:
   - `ProfileMapper`: Sanitize `lastName` in `toDomain()` and `asExternalModel()`.
   - `OnboardingRepositoryImpl`:
     - Default first name to `"Friend"` when missing or blank.
     - Default last name to `UNSET_LAST_NAME` when missing or blank.
     - Sanitize response `lastName` before saving to local database.
     - Pass `DEFAULT_BIRTH_DATE` in `CompleteOnboardingRequest`.
   - `ProfileRepositoryImpl`:
     - Map `lastName` using `toApiLastName()` in `updateName` and `updateProfilePartial`.
   - `HomeRepositoryImpl` & `CalendarLocalDataSource`:
     - Sanitize `lastName` on caching/retrieval.

3. **`:feature:profile:impl`**:
   - `EditPersonalInfoSheet`: Remove birth date field, date picker, and date formatting. Enable Save button when `firstName.isNotBlank()`.
   - `ProfileScreen`: Remove `initialBirthDate` and pass updated `(firstName, lastName)` to `onSave`.
   - `ProfileAction.UpdatePersonalInfo`: Remove `birthDate` property.
   - `ProfileViewModel`: Update `updatePersonalInfo` to update name via `updateProfilePartialUseCase` with sanitized/api last name, removing birth date use case.
   - `ProfileHelper.getDisplayName`: Sanitize `lastName` to ensure `"not entered"` is excluded from display names.

4. **`:feature:onboarding:impl`**:
   - `OnboardingViewModel`: When Name step is skipped or setup is skipped, ensure `firstName` defaults to `"Friend"`, and `lastName` defaults to `UNSET_LAST_NAME`.
   - `strings.xml`: Update `onboarding_name_default_friend` to `"Friend"`.

5. **Testing**:
   - Update tests across `:core:data`, `:feature:onboarding:impl`, `:feature:profile:impl`.

## Implementation notes (what actually differed)

- **What was built vs what was originally planned**:
  - Implemented exactly according to plan: created `ProfileConstants` with `"Friend"`, `"not entered"`, `"2000-01-01"`, `sanitizeLastName()`, and `toApiLastName()`.
  - Removed birth date completely from `EditPersonalInfoSheet`, `ProfileScreen`, `ProfileAction`, and `ProfileViewModel`.
  - Updated `ProfileMapper`, `ProfileRepositoryImpl`, `HomeRepositoryImpl`, `CalendarLocalDataSource`, and `ProfileHelper` to sanitize and map sentinel last names.
  - Updated `OnboardingRepositoryImpl`, `OnboardingViewModel`, and `strings.xml` to default first name to `"Friend"` (capital F) and last name to `"not entered"` on skip/submit.
- **Key decisions made during implementation**:
  - Sanitization logic checks case-insensitively for `"not entered"` and blank strings, treating them as `null` in domain and database entities.
  - Sanitization applied at mapper boundaries (`ProfileMapper`), repository cache updates, and helper formatting functions (`ProfileHelper.getDisplayName`).
- **Deviations from original plan and why**:
  - None; everything followed the approved implementation plan and Jira issue specification.
- **Known limitations or follow-up work needed**:
  - No backend modifications needed as sentinel values (`"not entered"` and `"2000-01-01"`) satisfy backend non-null constraints while remaining transparent to UI and domain layers.

