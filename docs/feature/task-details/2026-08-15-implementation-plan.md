# Task Details Screen Implementation

## Summary
The Task Details feature has been implemented to provide full-screen management of individual tasks, including metadata editing, goal reassignment, dependency graph management (DAG), session scheduling, and deletion.

## Architecture
- **API module** (`:feature:task-details:api`): Defines `TaskDetailsRoute(val taskId: String)` which implements `Route`.
- **Impl module** (`:feature:task-details:impl`): Contains `TaskDetailsScreen`, `TaskDetailsViewModel`, `TaskDetailsUiState`, and `taskDetailsEntry`.
- **Domain Layer** (`:core:domain`):
  - `TaskRepository`: Extended with `getTask`, `updateTask`, `moveTask`, `addDependency`, `removeDependency`, `getTaskDependencies`, `getTaskDependents`, `getTaskSessions`, `addTaskSessions`, and `deleteTask` (with cascade option).
  - Use cases created: `GetTaskUseCase`, `UpdateTaskUseCase`, `MoveTaskUseCase`, `AddTaskDependencyUseCase`, `RemoveTaskDependencyUseCase`, `GetTaskDependenciesUseCase`, `GetTaskDependentsUseCase`, `GetTaskSessionsUseCase`, `AddTaskSessionsUseCase`, `ScheduleTaskUseCase`, `DeleteTaskUseCase`.
- **Data Layer** (`:core:data`):
  - `TaskRemoteDataSource` & `TaskRemoteDataSourceImpl`: Integrated Retrofit endpoints.
  - `TaskRepositoryImpl`: Coordinated network calls with local database cache (`TaskDao`).
- **Network Layer** (`:core:network`):
  - DTOs: `TaskMoveRequest`, `TaskDependencyRequest`, `AddTaskSessionsRequest`.
  - Service: `TaskApiService` updated with PATCH/POST/DELETE endpoints for task details, moves, dependencies, and sessions.
- **Home Integration**:
  - `UnifiedSessionTaskContent`: Task title converted into a clickable navigation link with open-in-new icon.
  - `SessionTaskDetailDialog`, `HomeScreen`, and `HomeEntryProvider`: Threaded `onNavigateToTaskDetails` callback.
  - `AwanApp`: Wired `TaskDetailsRoute` navigation.
- **Localization**:
  - Full English (`values/strings.xml`) and Arabic (`values-ar/strings.xml`) resource definitions added.
