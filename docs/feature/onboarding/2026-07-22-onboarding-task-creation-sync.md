# Onboarding Pre-Task Completion & Task Repository Plan

**Date:** 2026-07-22  
**Target:** Trigger `completeOnboarding` prior to `FirstTask` step, implement `TaskApiService` & `TaskRepository`, expose `CreateTaskUseCase`, and integrate into `OnboardingViewModel`.

## Overview

This plan restructures the onboarding sequence so `completeOnboarding` (`POST /v1/onboarding`) is executed **before** the task creation step. This satisfies the backend contract requiring the user to have completed onboarding before creating tasks. Additionally, a new `TaskRepository` pattern (`TaskApiService` -> `TaskRemoteDataSource` -> `TaskRepository` -> `CreateTaskUseCase`) is added to manage task creation via `POST /v1/tasks`.

## Architectural Sequence & Design

```
Wizard Steps:
Welcome -> Name -> DayBounds -> Zones -> TaskLength ---> [ completeOnboarding() ] ---> FirstTask ---> [ createTask() ] ---> Notifications
```

1. **Step Transition Update**:
   - As the user leaves `TaskLength` (via `Next` or `Skip`), `OnboardingViewModel` calls `onboardingRepository.completeOnboarding(data)`.
   - On successful response from backend, `isNew` status transitions and `onboardingCompleted` is persisted in local DataStore.

2. **Network Layer (`:core:network`)**:
   - `CreateTaskRequest` (@Serializable): `title`, `description`, `estimatedDuration`, `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`.
   - `TaskInfoResponse` (@Serializable): `id`, `title`, `description`, `estimatedDuration`, `status`, `mandatory`, `estimatedPoints`, `allowTaskSplitting`, `goalId`, `dependsOnTaskIds`.
   - `TaskApiService`:
     - `@POST("v1/tasks") suspend fun createTask(@Body request: CreateTaskRequest): TaskInfoResponse`
   - Bind `TaskApiService` in `NetworkModule`.

3. **Data Layer (`:core:data`)**:
   - `TaskRemoteDataSource` & `TaskRemoteDataSourceImpl`: calls `TaskApiService.createTask` wrapped with `safeApiCall` on `AwanDispatchers.IO`.
   - `TaskRepository` & `TaskRepositoryImpl`: provides `suspend fun createTask(...) : Result<TaskInfoResponse>`.
   - Bind `TaskRemoteDataSourceImpl` and `TaskRepositoryImpl` in `DataModule`.

4. **Domain Layer (`:core:domain`)**:
   - `CreateTaskUseCase`: exposes `suspend operator fun invoke(title: String, estimatedDurationMinutes: Int? = null): Result<TaskInfoResponse>` calling `TaskRepository`.

5. **Presentation Layer (`OnboardingViewModel`)**:
   - Trigger `completeOnboarding()` when leaving `TaskLength` step.
   - Inject `CreateTaskUseCase` into `OnboardingViewModel`.
   - In `submitFirstTask()`, call `createTaskUseCase` to create the task on the backend via `POST /v1/tasks`.

6. **Unit Tests**:
   - Unit tests for `TaskRemoteDataSourceTest`, `TaskRepositoryImplTest`, and updated `OnboardingViewModelTest`.

## Implementation notes (what actually differed)

- **Verification Status**:
  - `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL** (All tests in `:core:data`, `:core:network`, `:core:domain`, and `:feature:onboarding:impl` passed cleanly).
  - `./gradlew lint`: **BUILD SUCCESSFUL** (0 lint issues).
- **Module Placement**:
  - `CreateTaskUseCase` is located in `:core:data` (`com.awan.app.core.data.task.CreateTaskUseCase`) so that `:core:data` repositories and use cases remain self-contained for feature modules depending on `:core:data`.
- **Runtime Behavior**:
  - Transitioning out of `TaskLength` step (via `Next` or `Skip`) immediately triggers `repository.completeOnboarding(data)` on the backend. This ensures `isNew = false` before the user interacts with `FirstTask`.
  - Submitting `FirstTask` calls `CreateTaskUseCase` (`POST /v1/tasks`), completing task creation on the backend before navigating to `Notifications`.

