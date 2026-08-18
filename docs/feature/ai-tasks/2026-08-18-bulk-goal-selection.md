# 2026-08-18 Bulk Goal Selection on Task Proposals — Implementation Plan

## Overview

Allow users adding tasks from the Home quick add bottom sheet to choose a single goal to apply to all newly added tasks (bulk goal selection), or select individual goals per task via the task row item UI (`ProposalCard`), while ensuring the Goal Scheduling screen remains unaffected.

## Architecture layers touched

| Layer | What changes |
|---|---|
| `:feature:ai-tasks:impl` | `AiTasksState` holds `availableGoals`; `AiTasksAction` adds `GoalPicked` and `BulkGoalPicked`; `AiTasksViewModel` loads `GetGoalsUseCase` when `goalId == null`; `ProposalCard` adds goal chip and summary badge; `AiTasksScreen` adds `BulkGoalSelector` |

## Implementation notes (what actually differed)

- **Verification**: Verified via `./gradlew :feature:ai-tasks:impl:testDebugUnitTest`, `./gradlew testDebugUnitTest` (797 tasks), and `./gradlew assembleDebug` (1166 tasks) — all builds and tests passed with 0 errors.
- **Safety**: In Goal Scheduling mode (`state.goalId != null`), `availableGoals` remains empty and `BulkGoalSelector` is not rendered, preserving the exact Goal Scheduling UI.
