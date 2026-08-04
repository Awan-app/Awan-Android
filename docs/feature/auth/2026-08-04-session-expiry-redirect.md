# Session expiry → Login redirect (AWAN-126)

## Problem

When the refresh token is expired or rejected, `TokenAuthenticator` clears the stored tokens and
returns `null`, so the original 401 propagates back to whichever screen made the call. Home maps it
to `home_error_unauthorized` and renders its full-screen "Session expired. Please log in again." +
**Retry** state. The user is parked on a dead Home screen: Retry can never succeed, because there
are no tokens left to retry with, and nothing takes them to Login.

There was also a silent hole: `TokenAuthenticator` returned `null` when no refresh token was stored
*without* calling `clearTokens()`, leaving `is_logged_in = true` with no tokens and no way out.

## Approach

A dedicated one-shot session-expired signal, fired from the one place a session actually dies, and
consumed once at the app shell.

`AuthTokenProvider.observeIsLoggedIn()` was the obvious candidate to reuse, but it can't carry this:
it also flips on a deliberate Logout (so it would toast "session expired" after the user chose to
log out), and being a `StateFlow` it replays `false` on every cold start while logged out. Both
would have needed suppression hacks in the collector.

### Chain

`TokenAuthenticator` (refresh fails) → `clearTokens()` + `notifySessionExpired()` →
conflated `Channel` in the `@Singleton` `EncryptedTokenStorage` → `AuthRepository.observeSessionExpired()`
→ `ObserveSessionExpiredUseCase` → `MainActivityViewModel` → `ObserveAsEvents` in `AwanApp`
→ Toast + `navigator.replaceAll(LoginRoute)`.

The channel is `CONFLATED` so an expiry fired while the app is backgrounded is still delivered on
resume. `notifySessionExpired()` is non-suspending so it is callable from `TokenAuthenticator`'s
`runBlocking` block.

### Files

- `core/datastore/.../auth/AuthTokenProvider.kt` — `val sessionExpired: Flow<Unit>`, `fun notifySessionExpired()`
- `core/datastore/.../auth/EncryptedTokenStorage.kt` — channel + `clearTokens()` now preserves the email
- `core/network/.../interceptor/TokenAuthenticator.kt` — fires the signal on both dead-session paths
- `core/domain/.../auth/repository/AuthRepository.kt` + `core/data/.../AuthRepositoryImpl.kt` — contract passthrough
- `core/domain/.../auth/usecase/ObserveSessionExpiredUseCase.kt` — new, mirrors `ObserveAuthStateUseCase`
- `app/.../MainActivityViewModel.kt`, `app/.../MainActivity.kt`, `app/.../AwanApp.kt` — single collector
- `feature/auth/impl/.../ui/email/EmailViewModel.kt` — pre-fills the last known email

### Easier re-login

`clearTokens()` used to call `sharedPreferences.edit().clear()`, wiping `user_email` along with the
tokens — so there was nothing left to pre-fill with. It now removes only the token keys, user id and
the logged-in flag, deliberately preserving `KEY_USER_EMAIL`. `EmailViewModel` reads it through the
existing `GetUserUseCase` and seeds the field via `onEmailChanged()`, so `isEmailValid` is computed
by the same path as typed input. It only pre-fills when the field is still empty, so it can't
clobber someone who started typing first.

This applies to deliberate logout too, not just expiry — the email is a login identifier and the
field stays editable.

### Strings

Reuses `error_unauthorized` from `core/common` — already translated in `values/` and `values-ar/`.
No new keys.

### Deliberately not done

- `HomeViewModel`'s `Unauthorized` → `home_error_unauthorized` mapping is untouched. Home unmounts
  before that state is visible now; the branch stays as a fallback for a 401 that never reached the
  authenticator.
- Logout navigation was not rerouted through the shell. Profile's existing
  `ProfileEvent.LogoutSuccess → replaceAll(LoginRoute)` keeps working.
- The `priorResponseCount() >= 2` loop guard does not fire the signal — tokens there may still be
  valid.

## Implementation notes (what actually differed)

- **Verified:** `./gradlew assembleDebug`, `./gradlew lint`, `./gradlew testDebugUnitTest` all pass.
  Not yet verified on a device — the runtime checks below are still outstanding.
- **`AwanApp` also clears `showAddTask`** on the expiry event. Not in the original plan.
  `AddTaskSheet` is state-driven and lives outside `NavDisplay`, so it survives `replaceAll` and
  would otherwise sit on top of the Login screen. Easy to reintroduce: any state-driven overlay
  hoisted in `AwanApp` needs dismissing here, since navigation alone won't touch it.
- **`CalendarRepositoryImplTest.FakeAuthTokenProvider`** had to implement the two new interface
  members. It is the only test double for `AuthTokenProvider`; the compile error surfaced only under
  `testDebugUnitTest`, not `assembleDebug`.
- **`AuthRepositoryImpl.getUser()` semantics changed as a side effect.** It returns null only when
  email, userId, accessToken and refreshToken are *all* null — now that the email survives
  `clearTokens()`, it returns a `User` carrying only an email after logout. `GetUserUseCase` has no
  consumers outside this pre-fill, so nothing else is affected today, but anything that later treats
  a non-null `getUser()` as "is signed in" would be wrong. Use `observeIsLoggedIn()` for that.

### Still to verify on device

1. Log in, invalidate the session server-side (or corrupt the stored refresh token), then change the
   date on Home → toast + immediate jump to Login, no Retry screen, back-press does not re-enter Home.
2. Clear only the refresh token (leave `is_logged_in = true`) and trigger a 401 → also lands on Login.
3. Tap Logout from Profile → lands on Login with **no** toast, email pre-filled.
4. Cold start while logged out → no toast, splash → Login as before.
5. Arabic: repeat (1) and confirm the toast is the Arabic string.
