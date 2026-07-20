# Awan Android

Native Android client for Awan, an AI-assisted adaptive scheduling app. The backend AI turns a goal into a strict task contract; the client’s Local Conflict Engine owns task placement, conflicts, and dependency math. See [[Awan Android - Architecture]].

## Current state — 2026-07-20

- Package and application ID: `com.awan.app`.
- UI: Jetpack Compose with Navigation 3, Hilt, and the Skyward shared design system.
- Current routes: Home, Arena, Calendar, and Settings.
- Shared modules are active for design system, common utilities, Proto DataStore/preferences, and networking/authentication.
- The app uses a single `:app` module for screens today. The richer `:core:*` / `:feature:*` structure remains a target, not a claim that all modules already exist.

## Where to look

| Need | Source |
| --- | --- |
| App entry point and navigation | `Awan-Android/app/src/main/java/com/awan/app/` |
| Shared UI tokens and controls | `Awan-Android/core/design-system/` |
| Preferences and secure token storage | `Awan-Android/core/datastore/` |
| Retrofit, OkHttp, auth and API DTOs | `Awan-Android/core/network/` |
| Module map and build conventions | `Awan-Android/settings.gradle.kts`, `Awan-Android/build-logic/` |
| Dependency versions | `Awan-Android/gradle/libs.versions.toml` |

## Build and checks

Run from `Awan-Android` with JDK 21 and a configured Android SDK:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
```

See [[Awan Android - Working Agreement]] before committing or documenting a change.
