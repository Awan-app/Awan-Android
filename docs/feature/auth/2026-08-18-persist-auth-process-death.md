# Persist Authentication State Across Android Process Death

## Problem

When the user enters their email address and requests an OTP code, they often switch to their email client app (e.g., Gmail, Outlook) to view and copy the received code. During this background transition, the Android OS may terminate the app's process under memory pressure.

Prior to this fix:
1. `EmailViewModel` did not utilize `SavedStateHandle`, meaning any in-progress email input or active rate-limiting cooldown state was wiped on process recreation.
2. `OtpViewModel` wiped its state whenever `setEmail` was triggered during screen composition / recreation, resetting digits, resetting the resend cooldown to 120s rather than calculating elapsed time, and dropping any error/lock status.
3. No dedicated unit tests guarded against state loss across process death for both `EmailViewModel` and `OtpViewModel`.

## Approach

Use `SavedStateHandle` in both `EmailViewModel` and `OtpViewModel` to automatically persist and restore the authentication flow state across process death.

### 1. `EmailViewModel`
- Injects `SavedStateHandle`.
- Persists `email`, `rate_limit_email`, and `rate_limit_timestamp`.
- On ViewModel recreation, restores user email and calculates remaining rate-limit cooldown seconds against `System.currentTimeMillis()`.
- Falls back to `GetLastUsedEmailUseCase()` only when no saved state exists in `SavedStateHandle`.

### 2. `OtpViewModel`
- Reads and updates `SavedStateHandle` keys: `KEY_EMAIL`, `KEY_DIGITS`, `KEY_STATUS`, and `KEY_SENT_TIMESTAMP`.
- Calculates `resendSecondsRemaining` and `isResendEnabled` dynamically by comparing `System.currentTimeMillis()` against `KEY_SENT_TIMESTAMP`.
- In `setEmail(email)`, guards against resetting the UI state if the email matches the state already restored from `SavedStateHandle`.
- Updates `KEY_DIGITS` in `SavedStateHandle` as the user enters OTP digits.
- Clears saved state upon successful OTP verification.

### 3. Security Considerations
- The OTP itself is never stored persistently or in plain text across disk storage.
- Ephemeral state (timestamp, partial digits in memory bundle, email) is scoped to the component's lifecycle and cleared immediately upon login success.

### 4. Verification & Testing
- Add comprehensive unit tests in `EmailViewModelTest` and `OtpViewModelTest` simulating process death and verifying state restoration (email, digits, timer recalculation, and rate-limit recovery).

## Implementation notes (what actually differed)

- **Verification:** Ran `./gradlew :feature:auth:impl:testDebugUnitTest` and full `./gradlew testDebugUnitTest` across all modules (796 tasks, all unit tests passed).
- **`EmailViewModel` Rate Limit Recovery:** `EmailViewModel` computes remaining rate limit seconds by calculating `RATE_LIMIT_COOLDOWN_SECONDS - (System.currentTimeMillis() - KEY_RATE_LIMIT_TIMESTAMP) / 1000`. If cooldown has elapsed during process death, it automatically unlocks submission without showing a stale rate limit banner.
- **`OtpViewModel` Dynamic Cooldown:** `OtpViewModel` recalculates `resendSecondsRemaining` from `KEY_SENT_TIMESTAMP`. If the user returns from their mail app after 30 seconds, the countdown seamlessly resumes from 90s; if >120s elapsed, the resend action is immediately active.
- **`setEmail` Idempotence:** Guarded `setEmail` so `LaunchedEffect(email)` on screen composition does not wipe restored state or reset the countdown timer when called with the same email already loaded in `SavedStateHandle`.
- **Clean Architecture Compliance:** No data layer or repository dependencies leaked into presentation; use cases and `SavedStateHandle` provide complete state isolation.

