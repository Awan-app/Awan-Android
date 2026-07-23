# Onboarding Data Layer Implementation Plan

**Date:** 2026-07-22  
**Target:** Onboarding flow data layer (Remote Data Source, Repository, Offline-First Sync, DI bindings, DataStore integration, Unit Tests)

## Overview

This plan details the full implementation of the data layer for the Onboarding feature in accordance with the project's Now in Android (NiA) architecture guidelines. The implementation replaces the in-memory stub repository (`InMemoryOnboardingRepository`) with an offline-first production repository backed by `OnboardingRemoteDataSource` (connecting to the Postman API spec `POST /v1/onboarding`) and `UserPreferencesDataSource` (Proto DataStore).

## Architecture & Component Breakdown

```
                         +--------------------------+
                         |  OnboardingViewModel     |
                         +------------+-------------+
                                      |
                                      v
                         +--------------------------+
                         |   OnboardingRepository   |  <--- (:core:data)
                         +------------+-------------+
                                      |
              +-----------------------+-----------------------+
              |                                               |
              v                                               v
+---------------------------+                   +----------------------------+
| OnboardingRemoteDataSource|                   | UserPreferencesDataSource  |
+-------------+-------------+                   +-------------+--------------+
              |                                               |
              v                                               v
+---------------------------+                   +----------------------------+
|    OnboardingApiService   |                   |      Proto DataStore       |
|    (POST /v1/onboarding)  |                   |   (UserPreferencesProto)   |
+---------------------------+                   +----------------------------+
```

### 1. Remote Data Source Layer (`:core:data`)
- **Interface:** `com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource`
  - `suspend fun completeOnboarding(request: CompleteOnboardingRequest): Result<CompleteOnboardingResponse>`
- **Implementation:** `com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSourceImpl`
  - Uses `OnboardingApiService` and `safeApiCall` on `AwanDispatchers.IO`.

### 2. Repository Layer (`:core:data`)
- **Implementation:** `com.awan.app.core.data.onboarding.OnboardingRepositoryImpl`
  - Implements `OnboardingRepository` interface.
  - Manages reactive in-memory `OnboardingData` draft flow for step-by-step state changes (`saveProfile`, `saveDayBounds`, `saveZones`, `savePreferredTaskLength`, `saveFirstTask`).
  - On `completeOnboarding()`:
    - Constructs `CompleteOnboardingRequest` with formatted `wakeupTime` (`HH:mm:ss`), `sleepTime` (`HH:mm:ss`), preferred session duration, buffer, scheduling type, timezone, name, and birth date.
    - Calls `OnboardingRemoteDataSource.completeOnboarding(request)`.
    - Updates local `UserPreferencesDataSource.setOnboardingCompleted(true)` and stores user preferences locally upon completion.
    - Handles offline/network errors gracefully so user progress is preserved offline.

### 3. Dependency Injection (`:core:data`)
- Update `DataModule` in `:core:data`:
  - `@Binds @Singleton abstract fun bindOnboardingRemoteDataSource(impl: OnboardingRemoteDataSourceImpl): OnboardingRemoteDataSource`
  - `@Binds @Singleton abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository`

### 4. Verification & Unit Tests
- `OnboardingRemoteDataSourceTest` verifying Retrofit + `safeApiCall` integration and error handling.
- `OnboardingRepositoryImplTest` verifying draft persistence, payload formatting (`wakeupTime`, `sleepTime`, `timezone`), remote service invocation, and DataStore completion flag updates.

## Implementation notes (what actually differed)

### Build, Test & Lint Verification
- **Unit Tests**: `./gradlew testDebugUnitTest` passed cleanly across all `:core:data` and `:feature:onboarding:impl` test suites.
- **Lint**: `./gradlew lint` passed with 0 errors.

### Deviations from Plan
- Updated `OnboardingRepository.completeOnboarding(data: OnboardingData)` to be completely stateless: transient wizard step state is held in `OnboardingViewModel`, and the complete `OnboardingData` payload is passed to `completeOnboarding` at completion time.
- Added explicit `testImplementation(libs.junit)` and `testImplementation(libs.kotlinx.coroutines.test)` to `core/data/build.gradle.kts` for `:core:data` unit testing.

