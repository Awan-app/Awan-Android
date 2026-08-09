# 2026-08-07 Inbox Screen — Implementation Plan

## Overview

Add an **Inbox** tab inside the Goals screen (second main tab). Inbox tasks are tasks where
`goalId == null`. The tab presents a searchable, filterable list of task cards with expandable
session rows.

## Architecture layers touched

| Layer | What changes |
|---|---|
| `:core:network` | Add `GET v1/tasks/inbox` to `TaskApiService`; new `InboxTasksResponse` DTO |
| `:core:data` | `TaskRemoteDataSource` + impl get `getInboxTasks()`; `TaskRepositoryImpl` delegates to it |
| `:core:domain` | `TaskRepository` gets `getInboxTasks()`; new `GetInboxTasksUseCase` |
| `:feature:goals:impl` | Top-level segmented control (Inbox ↔ Goals); new `InboxMvi`, `InboxViewModel`, `InboxScreen` |

## Derived task display status (computed from sessions, never persisted)

| Display label | Rule |
|---|---|
| Drafted | `sessions.isEmpty()` |
| Active | any session is SCHEDULED (regardless of timing) |
| Completed | at least one non-CANCELLED session is COMPLETED, and no non-CANCELLED session is not COMPLETED |
| Cancelled | all sessions are CANCELLED |

## Session display filters (display-only; never mutate session status)

- **Active now** — SCHEDULED session whose `start <= now <= end`
- **Missed** — SCHEDULED session whose `end < now`

## Search

Matches task title, description, and formatted session info (date + time).

## Strings

All strings live in `feature/goals/impl/res/values/strings.xml` (EN) and `values-ar/strings.xml` (AR).
New string keys use the `inbox_` prefix as per naming convention.

## Acceptance Criteria

- Drafted: task with no sessions
- Active: task with at least one SCHEDULED session
- Completed: all non-cancelled sessions are COMPLETED and at least one is COMPLETED
- Cancelled: all sessions are CANCELLED
- Active now and Missed are display-only derivatives of SCHEDULED sessions
- Loading / empty / failure states handled
- UI matches Awan design system depth, typography, color, mascot
- Both EN and AR strings provided
