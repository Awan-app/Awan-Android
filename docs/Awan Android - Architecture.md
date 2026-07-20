# Awan Android - Architecture

## Runtime flow

```text
MainActivity
  -> AwanTheme (core:design-system)
  -> NavigationRoot (Navigation 3)
  -> Home | Arena | Calendar | Settings screens

Screens / future feature modules
  -> core:common, core:datastore, core:network, core:design-system
```

`AwanApplication` is the Hilt application root. `NavigationRoot` owns the in-memory Navigation 3 back stack and bottom bar.

## Active shared modules

| Module | Responsibility |
| --- | --- |
| `:core:design-system` | Awan theme, tokens, typography, shapes, surfaces, and reusable buttons/text. |
| `:core:common` | App errors, result type, coroutine dispatchers/scopes, and DI bindings. |
| `:core:datastore-proto` | Proto schema and generated `UserPreferences`. |
| `:core:datastore` | Preferences flow, encrypted token storage, and DataStore DI. |
| `:core:network` | Retrofit/OkHttp, JSON setup, auth interceptor/authenticator, DTOs, and network error mapping. |

The authenticated HTTP client adds the auth interceptor and 401 authenticator. The separate unauthenticated client is used for authentication calls, avoiding refresh recursion.

## Build structure

`settings.gradle.kts` includes `build-logic` and the app plus the five active core modules. Convention plugins keep module build files small:

- `awan.android.application` / `awan.android.library` — Android and Kotlin baseline.
- `awan.android.compose` — Compose setup.
- `awan.android.hilt` — KSP and Hilt dependencies.
- `awan.android.feature` — intended feature-module baseline.

Versions live in `gradle/libs.versions.toml`; do not scatter dependency versions through module builds.

## Product boundaries that must survive implementation

- The AI returns intent and the frozen JSON task contract; it does not assign start times.
- The Local Conflict Engine owns overlap, dependency, and scheduling math. It must stay pure Kotlin so Android and iOS can share test vectors.
- Tasks live in one of four editable zones: Study, Work, Play, or Personal. Scheduling stays within the task’s zone window.
- Scheduling changes require user approval via an Intelligent Nudge; Nightly Sweep also catches up when the app returns to foreground.

## Planned, not yet implemented as the target architecture

- Feature modules that depend on core modules, never on one another.
- Offline-first Room data layer, reactive UI, and background sync with WorkManager.
- The standalone pure-Kotlin Local Conflict Engine and shared QA vectors.

`home-widgets/` exists in the checkout but is not included in the active Gradle settings; treat it as an inactive POC until it is explicitly integrated.
