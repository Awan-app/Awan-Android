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

### PR #32 review follow-up (2026-08-05)

Five of six review comments were acted on; one was rejected as factually wrong.

- **Transient network errors no longer end the session.** `TokenAuthenticator`'s refresh `catch`
  was generic, so an `IOException` or `SocketTimeoutException` cleared the tokens and bounced the
  user to Login with a perfectly valid refresh token still on the server. It now only treats
  `HttpException` 401/403 as a dead session; everything else returns `null` and leaves the tokens
  alone, so the next call retries.
- **`getUser()`'s invariant is restored.** Basing the null check on the surviving email made a
  logged-out user read back as a non-null `User`. The check now looks only at `userId` /
  `accessToken` / `refreshToken`, and the pre-fill reads the email through a new
  `GetLastUsedEmailUseCase` → `AuthRepository.getLastUsedEmail()`. `getUser() != null` means
  "has a session" again.
- **Concurrent 401s collapse to one signal.** Parallel requests each hit the no-refresh-token
  branch and each fired `notifySessionExpired()`, stacking toasts. Callers now notify *before*
  `clearTokens()`, and `notifySessionExpired()` is a no-op when `_isLoggedIn` is already false —
  so only the caller that found the session alive gets through.
- **`Navigator.replaceAll` resets every sub-stack, not just the target's.** It previously left the
  other top-level sub-stacks deep, so a session that expired on Profile > DailyZones would restore
  the *previous* user to DailyZones the next time that tab was selected after re-login. Covered by
  `NavigatorTest.replaceAll_resetsEverySubStackNotJustTheTarget`. This also fixes the two
  pre-existing deliberate-logout callers.
- **The email pre-fill can no longer crash Login.** `EncryptedSharedPreferences` throws once the
  Keystore master key is invalidated (OS update, lock-screen change); the unguarded read in
  `EmailViewModel.init` would have taken the process down. The read is wrapped — no pre-fill beats
  no login screen.

Rejected: the claim that `_sessionExpired.receiveAsFlow()` throws
`IllegalStateException: Channel's flows are one-shot`. That is `consumeAsFlow`. Confirmed against
kotlinx-coroutines 1.11.0 `flow/Channels.kt` — `receiveAsFlow` is `ChannelAsFlow(consume = false)`
and its `markConsumed()` is a no-op, so re-collection across `repeatOnLifecycle` restarts is safe.

`./gradlew assembleDebug`, `lint`, and `testDebugUnitTest` all pass. Device verification from the
list above is still outstanding.

### Merging develop, and the ViewModel leak the merge made live (2026-08-05)

The branch was 8 commits behind `develop`. Merging it (`18edf0d`) conflicted only in `AwanApp.kt`,
in two adjacent import hunks — no competing logic.

That merge brought in `rememberDecoratedEntries` from AWAN-101 (PR #31), which **invalidates the
original verdict on one review comment**. The reviewer's "previous user's `HomeViewModel` stays in
memory" comment was judged against this branch's tip, which predated that function, and was wrongly
called invalid. It was correct:

- `rememberDecoratedEntries` decorates *every* sub-stack each recomposition, on purpose, so
  background tabs keep their ViewModels across tab switches.
- `ViewModelStoreNavEntryDecorator` clears a store only via `onPop = { clearKey(key) }` — that is,
  only when a key leaves the backStack it watches.
- A **tab root never leaves its own sub-stack**. Resetting the stacks in `replaceAll` pops and
  clears the deep routes but never `HomeRoute`/`ProfileRoute`/`GoalsRoute`/`MarketplaceRoute`. So
  after logout or expiry, re-login handed the next user the previous user's ViewModel.

Fixed in `b54ce1c` via the library's own cleanup path rather than backstack manipulation:
`NavigationState.generation` is bumped by every `replaceAll`, and the per-sub-stack decorators are
keyed on it. Changing the key drops each stack's decorators; their `rememberViewModelStoreProvider`
is `remember(parent, key)`-scoped and calls `clearAllKeys()` in `onDispose`. Configuration changes
are unaffected — that hook checks the parent lifecycle and deliberately skips the clear when it is
already `DESTROYED`.

Bumping on *every* `replaceAll` is intentional: all six call sites (splash → Login/Onboarding/Home,
login → Home/Onboarding, onboarding → Home/Login, both logouts, expiry) are flow boundaries where
stale per-screen state is wrong. Pinned by `NavigatorTest.replaceAll_bumpsGenerationSoEntryDecoratorsDrop`
and `navigate_doesNotBumpGeneration`.

Also narrowed the `EmailViewModel` pre-fill catch from `Exception` to
`GeneralSecurityException`/`IOException`. The call suspends, and both a broad `catch` and the
`runCatching` idiom used elsewhere in the codebase would have swallowed `CancellationException`.

**Known gap, deliberately not fixed.** `receiveAsFlow`'s contract says an element is lost if the
collector is cancelled after receiving it but before processing it, and `ObserveAsEvents` cancels on
`STOPPED`. A session that dies exactly as the app backgrounds can therefore drop the signal, landing
the user back on Home's dead Retry screen — the very bug this plan fixes. The window is narrow and
the next 401 re-fires. The durable fix is a `MutableStateFlow` latch instead of a channel (a latch
has no element to lose), reset in `setLoggedIn(true)`; it replaces the mechanism this plan chose, so
it is left for a follow-up.

Additional device checks this work needs, on top of the list above:

6. Log in as A, open Home and Profile, log out, log in as B → B sees no trace of A's screen state.
7. Rotate the device on Home → state survives; the generation bump must not fire on configuration change.
8. Kill the network mid-refresh → stays logged in, no redirect to Login.
9. Fire concurrent 401s → exactly one toast, one redirect.

`./gradlew assembleDebug`, `lint`, and `testDebugUnitTest` pass after the merge and after the fix.
