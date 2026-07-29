# Awan API Documentation

**Smart Productivity & Task Management Backend**

**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Authentication](#authentication)
2. [Onboarding](#onboarding)
3. [User Management](#user-management)
4. [Goals](#goals)
5. [Tasks](#tasks)
6. [Sessions](#sessions)
7. [Templates](#templates)
8. [Template Overrides](#template-overrides)
9. [Zones](#zones)
10. [Categories](#categories)
11. [AI Scheduling](#ai-scheduling)
12. [AI Goal Decomposition](#ai-goal-decomposition)
13. [System](#system)
14. [Error Handling](#error-handling)

---

## Authentication

The Awan API uses **OTP-based email authentication** with no passwords required. Protected endpoints require the `Authorization: Bearer {{accessToken}}` header.

### Token Management

- **Access tokens** expire after 24 hours
- **Refresh tokens** are used for token rotation (valid for 30 days)
- Each refresh invalidates the old token and issues a new pair
- If a revoked token is reused, **ALL tokens for that user are revoked** (security measure)

### OTP Request

**Endpoint:** `POST /v1/auth/otp/request`

Request a one-time password sent via email.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (200):**
```json
{
  "expiresInSeconds": 300,
  "resendAvailableInSeconds": 30
}
```

**Error Codes:**
- `VALIDATION_ERROR` (422) – Invalid email format
- `OTP_RATE_LIMIT_EXCEEDED` (429) – Max 3 requests per 10 minutes

---

### OTP Verify

**Endpoint:** `POST /v1/auth/otp/verify`

Verify OTP code to authenticate. Creates a new user on first login.

**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "deviceId": "c9751c89-b2cc-4957-928f-6ba5a00eb15a"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresIn": 86400,
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "isNew": true
  }
}
```

**Error Codes:**
- `VALIDATION_ERROR` (422) – Invalid email/code format
- `OTP_EXPIRED_OR_NOT_FOUND` (404) – OTP expired or never requested
- `OTP_INVALID_CODE` (401) – Wrong code
- `OTP_LOCKED` (423) – Max 5 wrong attempts

**Notes:**
- `isNew: true` indicates the user must complete onboarding
- `deviceId` should be unique and persisted locally on the client
- OTP is valid for 5 minutes

---

### Refresh Token

**Endpoint:** `POST /v1/auth/refresh`

Rotate refresh token to get a new access token and refresh token pair.

**Request Body:**
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "deviceId": "c9751c89-b2cc-4957-928f-6ba5a00eb15a"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresIn": 86400,
  "refreshToken": "new-refresh-token-uuid"
}
```

**Error Codes:**
- `VALIDATION_ERROR` (422) – Invalid input
- `REFRESH_TOKEN_INVALID` (401) – Token not found in DB
- `REFRESH_TOKEN_EXPIRED` (401) – Token expired (30 days)
- `REFRESH_TOKEN_REUSE_DETECTED` (401) – Token already used → ALL tokens revoked

**Best Practice:** Store the new `refreshToken` after every refresh call.

---

### Logout

**Endpoint:** `POST /v1/auth/logout`

Logout by revoking the refresh token for a specific device.

**Request Body:**
```json
{
  "deviceId": "c9751c89-b2cc-4957-928f-6ba5a00eb15a"
}
```

**Response:** `204 No Content`

**Error Codes:**
- `AUTHENTICATION_FAILED` (401) – Missing/invalid access token
- `VALIDATION_ERROR` (422) – Invalid deviceId

---

## Onboarding

First-time user setup. Must be completed when `isNew: true` is returned from OTP Verify. Once completed, subsequent calls will fail with `ONBOARDING_ALREADY_COMPLETED`.

### Complete Onboarding

**Endpoint:** `POST /v1/onboarding`

Complete initial onboarding for new users and set up preferences.

**Request Body:**
```json
{
  "firstName": "Abdelrahman",
  "lastName": "Emad",
  "birthDate": "2002-06-16",
  "timezone": "Africa/Cairo",
  "preferredSessionDuration": 30,
  "bufferBetweenSessions": 10,
  "wakeupTime": "07:30:00",
  "sleepTime": "23:00:00",
  "schedulingType": "BALANCED"
}
```

**Field Details:**
- `firstName` (string, required) – Max 50 chars
- `lastName` (string, required) – Max 50 chars
- `birthDate` (LocalDate, required) – Must be in the past (format: YYYY-MM-DD)
- `timezone` (string, required) – IANA timezone (e.g., `Africa/Cairo`, `America/New_York`)
- `preferredSessionDuration` (int, required) – Minutes per session (≥ 0)
- `bufferBetweenSessions` (int, required) – Minutes between sessions (≥ 0)
- `wakeupTime` (LocalTime, required) – Format: HH:mm:ss
- `sleepTime` (LocalTime, required) – Must be after wakeupTime (format: HH:mm:ss)
- `schedulingType` (enum, optional) – `BALANCED`, `EASIEST_FIRST`, `HARDEST_FIRST` (default: BALANCED)

**Response (200):** Full `UserProfileResponse`

**Error Codes:**
- `AUTHENTICATION_FAILED` (401) – Missing/invalid access token
- `VALIDATION_ERROR` (422) – Missing/invalid fields
- `ONBOARDING_ALREADY_COMPLETED` (400) – User already onboarded
- `INVALID_TIMEZONE` (400) – Invalid timezone string
- `INVALID_SLEEP_SCHEDULE` (400) – wakeupTime must be before sleepTime

### Check Onboarding Status

**Endpoint:** `GET /v1/users/me/is-new`

Whether the authenticated user still needs onboarding. Used on cold start to decide between Home and
the onboarding flow when the local completion flag is absent (e.g. reinstall or a new device).

**Response (200):**
```json
{
  "isNew": true
}
```

**Field Details:**
- `isNew` (boolean) – `true` when onboarding has not been completed yet

**Error Codes:**
- `AUTHENTICATION_FAILED` (401) – Missing/invalid access token

---

## User Management

### Profile Management

#### Get Profile

**Endpoint:** `GET /v1/users/me`

Get the authenticated user's full profile and preferences.

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "Abdelrahman",
  "lastName": "Emad",
  "birthDate": "2002-06-16",
  "points": 150,
  "streak": 5,
  "maxStreak": 12,
  "preferences": {
    "timezone": "Africa/Cairo",
    "preferredSessionDuration": 30,
    "bufferBetweenSessions": 10,
    "wakeupTime": "07:30:00",
    "sleepTime": "23:00:00",
    "schedulingType": "BALANCED"
  }
}
```

---

#### Update Name

**Endpoint:** `PATCH /v1/users/me/profile/name`

Update the user's first and last name.

**Request Body:**
```json
{
  "firstName": "Omar",
  "lastName": "Sami"
}
```

**Response (200):** Updated `UserProfileResponse`

---

#### Update Birth Date

**Endpoint:** `PATCH /v1/users/me/profile/birth-date`

Update the user's birth date.

**Request Body:**
```json
{
  "birthDate": "2005-01-01"
}
```

**Response (200):** Updated `UserProfileResponse`

---

#### Update Profile (Partial)

**Endpoint:** `PATCH /v1/users/me`

Partially update any profile fields. Only provided fields are updated.

**Request Body (all optional):**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "timezone": "America/New_York",
  "preferredSessionDuration": 45,
  "bufferBetweenSessions": 15
}
```

---

### Preferences Management

#### Update Timezone

**Endpoint:** `PATCH /v1/users/me/preferences/timezone`

Update the user's preferred IANA timezone.

**Request Body:**
```json
{
  "timezone": "America/Chicago"
}
```

---

#### Update Session Settings

**Endpoint:** `PATCH /v1/users/me/preferences/session`

Update preferred session duration and buffer time.

**Request Body:**
```json
{
  "preferredSessionDuration": 60,
  "bufferBetweenSessions": 15
}
```

**Constraints:**
- `preferredSessionDuration` ≥ 10 minutes
- `bufferBetweenSessions` ≥ 0 minutes

---

#### Update Sleep Schedule

**Endpoint:** `PATCH /v1/users/me/preferences/sleep-schedule`

Update wake-up and sleep times.

**Request Body:**
```json
{
  "wakeupTime": "07:30:00",
  "sleepTime": "23:00:00"
}
```

**Constraints:**
- wakeupTime must be before sleepTime

---

#### Update Scheduling Type

**Endpoint:** `PATCH /v1/users/me/preferences/scheduling-type`

Update the scheduling algorithm type.

**Request Body:**
```json
{
  "schedulingType": "BALANCED"
}
```

**Options:**
- `BALANCED` – Balance task difficulty
- `EASIEST_FIRST` – Schedule easy tasks first
- `HARDEST_FIRST` – Schedule difficult tasks first

---

### Gamification

#### Increment Streak

**Endpoint:** `PATCH /v1/users/me/streak/increment`

Increment the user's current streak by 1. Updates maxStreak if exceeded.

**Request Body:** Empty

**Response (200):**
```json
{
  "points": 150,
  "streak": 6,
  "maxStreak": 12
}
```

---

#### Reset Streak

**Endpoint:** `PATCH /v1/users/me/streak/reset`

Reset the user's current streak to 0. Max streak is preserved.

**Request Body:** Empty

**Response (200):** Updated `UserProgressResponse`

---

#### Award Points

**Endpoint:** `PATCH /v1/users/me/points/award`

Award points to the user.

**Request Body:**
```json
{
  "points": 50
}
```

**Constraints:**
- Points must be positive (> 0)

---

#### Deduct Points

**Endpoint:** `PATCH /v1/users/me/points/deduct`

Deduct points from the user.

**Request Body:**
```json
{
  "points": 10
}
```

**Error Codes:**
- `INSUFFICIENT_POINTS` (400) – User doesn't have enough points

---

## Goals

Goals are containers for tasks. Each user has an auto-created Inbox goal for unassigned tasks.

**Goal Statuses:** `ACTIVE` (default), `ACHIEVED`

### Create Goal

**Endpoint:** `POST /v1/goals`

Create a new goal with optional tasks.

**Request Body:**
```json
{
  "title": "Learn Spring Boot",
  "description": "Master Spring Boot and build a REST API",
  "targetDate": "2026-12-31",
  "tasks": [
    {
      "tempId": "tmp-1",
      "title": "Study JPA basics",
      "description": "Understand ORM and JPA annotations",
      "estimatedDuration": 60,
      "mandatory": true,
      "estimatedPoints": 30,
      "allowTaskSplitting": false,
      "dependsOnTempIds": [],
      "categoryId": "optional-category-uuid"
    }
  ]
}
```

**Field Details:**
- `title` (string, required) – Max 255 chars
- `description` (string, optional) – Max 2000 chars
- `targetDate` (LocalDate, optional) – Must be in the future (format: YYYY-MM-DD)
- `tasks` (array, optional) – Max 50 tasks, each with:
  - `tempId` (string, required) – Unique client-generated ID for dependencies
  - `title` (string, required) – Max 255 chars
  - `estimatedDuration` (int, optional) – Minutes (defaults to user's preferredSessionDuration if not provided, must be ≥ 1 if given)
  - `mandatory` (boolean, optional)
  - `estimatedPoints` (int, optional)
  - `allowTaskSplitting` (boolean, optional)
  - `categoryId` (UUID, optional)
  - `dependsOnTempIds` (string[], optional) – References to other tempIds in batch

**Response (201):**
```json
{
  "id": "goal-uuid-here",
  "title": "Learn Spring Boot",
  "description": "Master Spring Boot and build a REST API",
  "status": "ACTIVE",
  "targetDate": "2026-12-31",
  "createdAt": "2026-07-19T10:30:00Z",
  "inbox": false,
  "tasks": []
}
```

**Error Codes:**
- `VALIDATION_ERROR` (422) – Missing title or future date constraint
- `DUPLICATE_TEMP_ID` (409) – Duplicate tempId in batch
- `UNKNOWN_TEMP_ID` (400) – dependsOnTempIds references non-existent tempId

---

### List Goals

**Endpoint:** `GET /v1/goals`

List user's goals with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `status` | ACTIVE / ACHIEVED | — | Filter by goal status |
| `includeInbox` | boolean | false | Include the Inbox goal |
| `expand` | boolean | false | Include full task objects |
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Items per page |
| `sort` | string | — | Sort field and direction (e.g., createdAt,desc) |

**Response (200):**
```json
{
  "content": [
    {
      "id": "goal-uuid",
      "title": "Goal Title",
      "status": "ACTIVE",
      "tasks": []
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### Get Inbox

**Endpoint:** `GET /v1/goals/inbox`

Get the auto-created Inbox goal. Creates it if it doesn't exist.

**Response (200):** `GoalInfoResponse` with `inbox: true`

---

### Get Goal

**Endpoint:** `GET /v1/goals/{goalId}`

Get a single goal by ID.

**Query Parameters:**
- `expand` (boolean, default: false) – Include full task objects

**Response (200):** `GoalInfoResponse`

**Error Codes:**
- `GOAL_NOT_FOUND` (404) – Goal not found or doesn't belong to user

---

### Get Goal Tasks

**Endpoint:** `GET /v1/goals/{goalId}/tasks`

List all tasks belonging to a specific goal.

**Response (200):** Array of `TaskInfoResponse`

---

### Bulk Add Tasks

**Endpoint:** `POST /v1/goals/{goalId}/tasks/bulk`

Bulk add up to 50 tasks to a goal in one request. Supports cross-reference dependencies.

**Request Body:**
```json
{
  "tasks": [
    {
      "tempId": "tmp-1",
      "title": "Study JPA basics",
      "estimatedDuration": 60,
      "mandatory": true,
      "estimatedPoints": 30,
      "dependsOnRefs": []
    },
    {
      "tempId": "tmp-2",
      "title": "Build REST API",
      "estimatedDuration": 120,
      "dependsOnRefs": ["tmp-1"]
    }
  ]
}
```

**Response (201):** Array of `TaskInfoResponse`

**Error Codes:**
- `TASK_CYCLIC_DEPENDENCY` (400) – Dependency would create a cycle
- `UNKNOWN_TEMP_ID` (400) – Referenced tempId not found

---

### Update Goal

**Endpoint:** `PATCH /v1/goals/{goalId}`

Update an existing goal. Only provided fields are updated.

**Request Body (all optional):**
```json
{
  "title": "Master Spring Boot & React",
  "description": "Updated description",
  "status": "ACTIVE",
  "targetDate": "2027-01-15"
}
```

**Response (200):** Updated `GoalInfoResponse`

**Note:** The Inbox goal cannot be edited.

---

### Delete Goal

**Endpoint:** `DELETE /v1/goals/{goalId}`

Delete a goal and all its tasks (cascaded).

**Response:** `204 No Content`

**Note:** The Inbox goal cannot be deleted.

---

## Tasks

Tasks are work items that belong to a goal. Each task can have multiple sessions (scheduled occurrences) and dependencies on other tasks within the same goal.

**Task Statuses:** `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### Get Tasks by Date

**Endpoint:** `GET /v1/tasks/date/{date}`

Get all tasks that have sessions on a specific date.

**Path Parameters:**
- `date` (LocalDate, required) – Format: YYYY-MM-DD

**Response (200):**
```json
[
  {
    "task": {
      "id": "task-uuid-1",
      "title": "Task A",
      "estimatedDuration": 60,
      "status": "SCHEDULED",
      "goalId": "goal-uuid",
      "category": {
        "id": "cat-uuid",
        "name": "Category Name"
      }
    },
    "sessions": [
      {
        "id": "session-uuid-1",
        "start": "2026-07-22T09:00:00",
        "end": "2026-07-22T10:00:00",
        "status": "SCHEDULED",
        "locked": false,
        "zoneId": "zone-uuid-1"
      }
    ]
  }
]
```

---

### Get Tasks by Date Range

**Endpoint:** `GET /v1/tasks/range`

Get all tasks with sessions within a date range, grouped by date.

**Query Parameters:**
- `startDate` (LocalDate, required) – Format: YYYY-MM-DD
- `endDate` (LocalDate, required) – Format: YYYY-MM-DD

**Response (200):** Map of LocalDate to TaskWithSessionsResponse[]

**Constraints:**
- `endDate` must be after or equal to `startDate`

---

### Create Task

**Endpoint:** `POST /v1/tasks`

Create a new task. If goalId is null/omitted, the task lands in the Inbox.

**Request Body:**
```json
{
  "title": "Study Spring Data JPA",
  "description": "Learn JPA repositories and query methods",
  "estimatedDuration": 120,
  "mandatory": true,
  "estimatedPoints": 50,
  "allowTaskSplitting": false,
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  "categoryId": "optional-category-uuid"
}
```

**Field Details:**
- `title` (string, required) – Max 255 chars
- `description` (string, optional) – Max 2000 chars
- `estimatedDuration` (int, optional) – Minutes (defaults to preferredSessionDuration)
- `mandatory` (boolean, optional) – Default: false
- `estimatedPoints` (int, optional) – Default: 0
- `allowTaskSplitting` (boolean, optional) – Default: false
- `categoryId` (UUID, optional)
- `goalId` (UUID, optional) – Null/omitted → Inbox

**Response (201):** `TaskInfoResponse`

---

### Create Task with Sessions

**Endpoint:** `POST /v1/tasks/with-sessions`

Create a task with pre-scheduled sessions in a single request.

**Request Body:**
```json
{
  "task": {
    "title": "Prepare presentation",
    "description": "Create slides and rehearse",
    "estimatedDuration": 120,
    "goalId": null
  },
  "sessions": [
    {
      "zoneId": "optional-zone-uuid",
      "start": "2026-07-22T09:00:00",
      "end": "2026-07-22T10:00:00",
      "status": "SCHEDULED"
    },
    {
      "start": "2026-07-23T09:00:00",
      "end": "2026-07-23T10:00:00"
    }
  ]
}
```

**Session Fields:**
- `zoneId` (UUID, optional) – Zone to associate with session
- `start` (LocalDateTime, required) – Format: YYYY-MM-DDTHH:mm:ss
- `end` (LocalDateTime, required) – Must be after start
- `status` (enum, optional) – `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `SKIPPED` (default: SCHEDULED)

**Response (201):** `TaskWithSessionsResponse`

---

### Get Task Sessions

**Endpoint:** `GET /v1/tasks/{taskId}/sessions`

List all sessions for a given task.

**Response (200):** Array of `SessionResponse`

---

### Get Task

**Endpoint:** `GET /v1/tasks/{taskId}`

Get a single task by ID.

**Response (200):** `TaskInfoResponse`

---

### Update Task

**Endpoint:** `PATCH /v1/tasks/{taskId}`

Update an existing task. Only provided fields are updated.

**Request Body (all optional):**
```json
{
  "title": "Updated task title",
  "description": "Updated description",
  "estimatedDuration": 90,
  "status": "IN_PROGRESS",
  "mandatory": false,
  "estimatedPoints": 75,
  "allowTaskSplitting": true,
  "categoryId": "new-category-uuid"
}
```

---

### Move Task

**Endpoint:** `PATCH /v1/tasks/{taskId}/move`

Move a task to a different goal.

**Request Body:**
```json
{
  "goalId": "target-goal-uuid"
}
```

**Constraints:**
- A task with dependency links cannot be moved (remove dependencies first)

---

### Delete Task

**Endpoint:** `DELETE /v1/tasks/{taskId}`

Delete a task.

**Query Parameters:**
- `cascade` (boolean, default: false) – If true, removes dependency links from dependent tasks before deleting

**Response:** `204 No Content`

**Error Codes:**
- `INVALID_OPERATION` (400) – Task has dependents and cascade=false

---

### Add Dependency

**Endpoint:** `POST /v1/tasks/{taskId}/dependencies`

Make `{taskId}` depend on `dependsOnTaskId`.

**Request Body:**
```json
{
  "dependsOnTaskId": "dependency-task-uuid"
}
```

**Constraints:**
- Both tasks must belong to the same goal
- A task cannot depend on itself
- Circular dependencies are rejected

**Response (201):** No body

---

### Remove Dependency

**Endpoint:** `DELETE /v1/tasks/{taskId}/dependencies/{dependsOnTaskId}`

Remove a dependency link between two tasks.

**Response:** `204 No Content`

---

### List Dependencies

**Endpoint:** `GET /v1/tasks/{taskId}/dependencies`

List all tasks that `{taskId}` depends on (prerequisites).

**Response (200):** Array of `TaskInfoResponse`

---

### List Dependents

**Endpoint:** `GET /v1/tasks/{taskId}/dependents`

List all tasks that depend on `{taskId}`.

**Response (200):** Array of `TaskInfoResponse`

---

## Sessions

Sessions are concrete scheduled occurrences within a task (and optionally a zone).

**Session Statuses:** `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

**Locked sessions cannot be updated, have their status changed, or be deleted.**

### Get Sessions by Date

**Endpoint:** `GET /v1/sessions/date/{date}`

Get all sessions for a specific date, sorted by start time ascending.

**Path Parameters:**
- `date` (LocalDate, required) – Format: YYYY-MM-DD

**Response (200):** Array of `SessionResponse`

---

### Get Sessions by Date Range

**Endpoint:** `GET /v1/sessions/range`

Get all sessions within a date range, grouped by date.

**Query Parameters:**
- `startDate` (LocalDate, required)
- `endDate` (LocalDate, required)

**Response (200):** Map of LocalDate to SessionResponse[]

---

### Get Session

**Endpoint:** `GET /v1/sessions/{sessionId}`

Get a single session by ID.

**Response (200):** `SessionResponse`

---

### Update Session

**Endpoint:** `PUT /v1/sessions/{sessionId}`

Update a session's time range and optionally its status.

**Request Body:**
```json
{
  "start": "2026-07-21T10:00:00",
  "end": "2026-07-21T12:00:00",
  "status": "IN_PROGRESS"
}
```

**Response (200):** Updated `SessionResponse`

**Error Codes:**
- `VALIDATION_ERROR` (400) – Session is locked

---

### Update Session Status

**Endpoint:** `PATCH /v1/sessions/{sessionId}/status`

Update only the status of a session.

**Query Parameters:**
- `status` (string, required) – `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`

**Response (200):** Updated `SessionResponse`

---

### Lock Session

**Endpoint:** `PATCH /v1/sessions/{sessionId}/lock`

Lock a session to prevent modifications.

**Response (200):** `SessionResponse` with `locked: true`

---

### Unlock Session

**Endpoint:** `PATCH /v1/sessions/{sessionId}/unlock`

Unlock a session to allow modifications.

**Response (200):** `SessionResponse` with `locked: false`

---

### Delete Session

**Endpoint:** `DELETE /v1/sessions/{sessionId}`

Delete a session. Locked sessions cannot be deleted.

**Response:** `204 No Content`

---

## Templates

Weekly recurring schedule templates. Each template defines a set of days-of-week it applies to and contains time-block zones.

**Day Conflict Rule:** A day-of-week can only belong to ONE template. Creating/updating a template with a day already assigned to another template is rejected.

### Create Template

**Endpoint:** `POST /v1/templates`

Create a new weekly template with optional zones.

**Request Body:**
```json
{
  "name": "Work Week",
  "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "zones": [
    {
      "name": "Morning Focus",
      "startTime": "09:00:00",
      "endTime": "11:00:00",
      "color": "#4CAF50"
    },
    {
      "name": "Afternoon Work",
      "startTime": "13:00:00",
      "endTime": "17:00:00",
      "color": "#2196F3"
    }
  ]
}
```

**Field Details:**
- `name` (string, required)
- `daysOfWeek` (Set of DayOfWeek, optional) – Values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
- `zones` (array, optional) – Max 50 zones, each with:
  - `name` (string, required)
  - `startTime` (LocalTime, required) – Format: HH:mm:ss
  - `endTime` (LocalTime, required) – Must be after startTime
  - `color` (string, optional) – Hex color code

**Response (200):** `TemplateResponse`

**Error Codes:**
- `DAY_ALREADY_ASSIGNED` (409) – Days already used by another template
- `ZONE_OVERLAP` (409) – Zone time ranges overlap

---

### List Templates

**Endpoint:** `GET /v1/templates`

List all templates for the authenticated user.

**Response (200):** Array of `TemplateResponse`

---

### Get Template

**Endpoint:** `GET /v1/templates/{templateId}`

Get a single template by ID with its zones.

**Response (200):** `TemplateResponse`

---

### Update Template

**Endpoint:** `PUT /v1/templates/{templateId}`

Update template name and days-of-week. Zones are managed separately.

**Request Body:**
```json
{
  "name": "Updated Work Week",
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"]
}
```

**Response (200):** Updated `TemplateResponse`

---

### Delete Template

**Endpoint:** `DELETE /v1/templates/{templateId}`

Delete a template and all its zones (cascaded).

**Response:** `204 No Content`

---

### Add Zone to Template

**Endpoint:** `POST /v1/templates/{templateId}/zones`

Add a new zone to an existing template.

**Request Body:**
```json
{
  "name": "Evening Review",
  "startTime": "18:00:00",
  "endTime": "19:00:00",
  "color": "#FF9800"
}
```

**Response (200):** `ZoneResponse`

**Error Codes:**
- `ZONE_OVERLAP` (409) – Zone time range overlaps with existing zone

---

### Get Template Zones

**Endpoint:** `GET /v1/templates/{templateId}/zones`

Get all zones for a template.

**Response (200):** Array of `ZoneResponse`

---

### Bulk Update Template Zones

**Endpoint:** `PUT /v1/templates/{templateId}/zones`

Replace all zones for a template in bulk. Zones not included in the request are deleted.

**Request Body:**
```json
{
  "zones": [
    {
      "id": "existing-zone-uuid",
      "name": "Updated Morning Focus",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "color": "#4CAF50"
    },
    {
      "name": "New Afternoon Slot",
      "startTime": "14:00:00",
      "endTime": "16:00:00",
      "color": "#2196F3"
    }
  ]
}
```

**Response (200):** Array of `ZoneResponse`

---

## Template Overrides

One-off schedule overrides for a specific date. When both a template and an override exist for the same date, the override takes priority. Each override has its own zones independent of the template's zones.

### Create Override

**Endpoint:** `POST /v1/template-overrides`

Create a one-off override for a specific date with optional zones.

**Request Body:**
```json
{
  "name": "Holiday Schedule",
  "dateOfDay": "2026-12-25",
  "zones": [
    {
      "name": "Leisure Morning",
      "startTime": "10:00:00",
      "endTime": "12:00:00",
      "color": "#E91E63"
    }
  ]
}
```

**Response (200):** `TemplateOverrideResponse`

---

### List Overrides

**Endpoint:** `GET /v1/template-overrides`

List all template overrides for the authenticated user.

**Response (200):** Array of `TemplateOverrideResponse`

---

### Get Override

**Endpoint:** `GET /v1/template-overrides/{overrideId}`

Get a single override by ID with its zones.

**Response (200):** `TemplateOverrideResponse`

---

### Update Override

**Endpoint:** `PUT /v1/template-overrides/{overrideId}`

Update the name and/or date of an override. Zones are managed separately.

**Request Body:**
```json
{
  "name": "Updated Holiday Schedule",
  "dateOfDay": "2026-12-26"
}
```

**Response (200):** Updated `TemplateOverrideResponse`

---

### Delete Override

**Endpoint:** `DELETE /v1/template-overrides/{overrideId}`

Delete an override and all its zones (cascaded).

**Response:** `204 No Content`

---

### Add Zone to Override

**Endpoint:** `POST /v1/template-overrides/{overrideId}/zones`

Add a new zone to an existing override.

**Request Body:**
```json
{
  "name": "Extra Slot",
  "startTime": "20:00:00",
  "endTime": "21:00:00",
  "color": "#00BCD4"
}
```

**Response (200):** `ZoneResponse`

---

### Get Override Zones

**Endpoint:** `GET /v1/template-overrides/{overrideId}/zones`

Get all zones for an override.

**Response (200):** Array of `ZoneResponse`

---

### Bulk Update Override Zones

**Endpoint:** `PUT /v1/template-overrides/{templateOverrideId}/zones`

Replace all zones for an override in bulk.

**Request Body:**
```json
{
  "zones": [
    {
      "id": "existing-zone-uuid",
      "name": "Updated Morning Slot",
      "startTime": "10:00:00",
      "endTime": "13:00:00",
      "color": "#E91E63"
    },
    {
      "name": "New Evening Slot",
      "startTime": "19:00:00",
      "endTime": "21:00:00",
      "color": "#9C27B0"
    }
  ]
}
```

**Response (200):** Array of `ZoneResponse`

---

## Zones

Time blocks that belong to either a Template (recurring) or a Template Override (one-off). Zones define the user's available time slots.

**Zone Resolution:** When fetching zones by date:
1. Check if a Template Override exists for that date → return its zones
2. Otherwise, if a Template matches the day-of-week → return its zones
3. Otherwise → return empty list

### Get Zone

**Endpoint:** `GET /v1/zones/{zoneId}`

Get a single zone by ID.

**Response (200):**
```json
{
  "id": "zone-uuid",
  "name": "Morning Focus",
  "startTime": "09:00:00",
  "endTime": "11:00:00",
  "color": "#4CAF50",
  "templateId": "template-uuid",
  "templateOverrideId": null,
  "category": {
    "id": "cat-uuid",
    "name": "Category Name"
  }
}
```

---

### Get Zone Sessions

**Endpoint:** `GET /v1/zones/{zoneId}/sessions`

List all sessions for a given zone.

**Response (200):** Array of `SessionResponse`

---

### Get Zones by Date

**Endpoint:** `GET /v1/zones/date/{date}`

Get the effective zones for a specific date, sorted by startTime ascending.

**Path Parameters:**
- `date` (LocalDate, required) – Format: YYYY-MM-DD

**Response (200):** Array of `ZoneResponse`

---

### Update Zone

**Endpoint:** `PUT /v1/zones/{zoneId}`

Update a zone's name, time range, and color.

**Request Body:**
```json
{
  "name": "Updated Focus Time",
  "startTime": "10:00:00",
  "endTime": "12:00:00",
  "color": "#673AB7"
}
```

**Response (200):** Updated `ZoneResponse`

**Error Codes:**
- `ZONE_OVERLAP` (409) – Time range overlaps with existing zone

---

### Delete Zone

**Endpoint:** `DELETE /v1/zones/{zoneId}`

Delete a zone.

**Response:** `204 No Content`

---

## Categories

Category management for zones and tasks. Categories are auto-created when zones are created but can also be managed independently. Each category is user-scoped.

### Create Category

**Endpoint:** `POST /v1/categories`

Create a new category.

**Request Body:**
```json
{
  "name": "Work"
}
```

**Response (201):**
```json
{
  "id": "category-uuid",
  "name": "Work"
}
```

---

### List Categories

**Endpoint:** `GET /v1/categories`

List all categories for the authenticated user.

**Response (200):** Array of `CategoryResponse`

---

### Get Category

**Endpoint:** `GET /v1/categories/{categoryId}`

Get a single category by ID.

**Response (200):** `CategoryResponse`

---

### Update Category

**Endpoint:** `PUT /v1/categories/{categoryId}`

Update a category name.

**Request Body:**
```json
{
  "name": "Updated Name"
}
```

**Response (200):** Updated `CategoryResponse`

---

## AI Scheduling

AI-powered scheduling endpoints. The system uses an AI model to intelligently schedule tasks into available time zones.

**Scheduling Considerations:**
- User's preferred session duration and buffer time
- Wake-up and sleep times
- Available zones from templates/overrides
- Existing booked (locked) sessions
- Task dependencies and priorities
- User's scheduling type (BALANCED, EASIEST_FIRST, HARDEST_FIRST)

### Schedule Goal

**Endpoint:** `POST /v1/schedule`

Schedule all unscheduled tasks for a goal using the AI scheduling engine.

**Request Body:**
```json
{
  "goalId": "goal-uuid-here"
}
```

**Response (200):**
```json
{
  "goalId": "goal-uuid",
  "scheduledSessions": [
    {
      "sessionId": "session-uuid-1",
      "taskId": "task-uuid-1",
      "zoneId": "zone-uuid-1",
      "start": "2026-07-22T09:00:00",
      "end": "2026-07-22T10:00:00"
    }
  ],
  "unscheduledTasks": [
    {
      "taskId": "task-uuid-2",
      "taskTitle": "Task Title",
      "reason": "INSUFFICIENT_TIME",
      "message": "Task duration exceeds available zone capacity"
    }
  ]
}
```

**Failure Reasons:**
- `INSUFFICIENT_TIME` – Task duration exceeds zone capacity
- `DEPENDENCY_CONFLICT` – A dependency could not be scheduled
- `NO_SUITABLE_ZONE` – No zone matches requirements
- `EXCEEDS_DAILY_CAPACITY` – Would exceed daily limits

**Error Codes:**
- `GOAL_NOT_FOUND` (404)
- `AI_UNAVAILABLE` (503)

---

### Schedule Task

**Endpoint:** `POST /v1/schedule/task`

Schedule a single unscheduled task.

**Request Body:**
```json
{
  "taskId": "task-uuid-here",
  "horizonDays": 14
}
```

**Field Details:**
- `taskId` (UUID, required)
- `horizonDays` (int, optional) – Number of days to look ahead (default: 14)

**Response (200):** `TaskScheduleResponse` with scheduled sessions and unscheduled tasks info

---

## AI Goal Decomposition

AI-powered conversational goal decomposition. Users chat with an AI assistant to break down a high-level goal into structured tasks.

### Goal Decompose Chat

**Endpoint:** `POST /v1/ai/goal-decompose`

Send a message in the goal decomposition conversation.

**Request Body (first message):**
```json
{
  "sessionId": null,
  "message": "I want to build a web application with React and Spring Boot"
}
```

**Request Body (follow-up):**
```json
{
  "sessionId": "existing-session-uuid",
  "message": "The app should have user authentication and a dashboard"
}
```

**Response (200):**
```json
{
  "sessionId": "session-uuid",
  "blocks": [
    {
      "type": "text",
      "text": "Great! Let me understand more about your app..."
    },
    {
      "type": "question",
      "text": "What is the primary use case?",
      "options": ["E-commerce", "Social Media", "Productivity", "Other"]
    }
  ],
  "hasProposal": false,
  "timestamp": "2026-07-22T10:00:00Z"
}
```

**Response with Proposal:**
```json
{
  "sessionId": "session-uuid",
  "blocks": [
    {
      "type": "proposal",
      "proposal": {
        "title": "Build Web Application",
        "description": "React frontend + Spring Boot backend",
        "targetDate": "2026-12-31",
        "tasks": [
          {
            "title": "Setup development environment",
            "estimatedDuration": 120,
            "estimatedPoints": 30
          }
        ]
      }
    }
  ],
  "hasProposal": true,
  "timestamp": "2026-07-22T10:00:00Z"
}
```

**Content Block Types:**
- `type: "text"` – Informational message with `text`
- `type: "question"` – Question with `text` and `options`
- `type: "proposal"` – Goal decomposition ready for confirmation

---

### Confirm Decomposition

**Endpoint:** `POST /v1/ai/goal-decompose/{sessionId}/confirm`

Confirm the proposal and create the goal with all tasks.

**Path Parameters:**
- `sessionId` (UUID, required)

**Response (200):** `GoalInfoResponse` with created goal and tasks

---

### Create Task via AI

**Endpoint:** `POST /v1/ai/task-create`

Create a task with AI-enriched details based on title and description.

**Request Body:**
```json
{
  "title": "Build login page",
  "description": "Create a login page with email and password fields"
}
```

**Response (201):** `TaskInfoResponse` with AI-estimated:
- `estimatedDuration`
- `estimatedPoints`
- `mandatory`
- `allowTaskSplitting`
- `category`

---

### Get Decomposition Transcript

**Endpoint:** `GET /v1/ai/goal-decompose/{sessionId}`

Get the full transcript of a decomposition session.

**Response (200):**
```json
{
  "sessionId": "session-uuid",
  "status": "ACTIVE",
  "messages": [
    {
      "role": "user",
      "blocks": [{"type": "text", "text": "I want to build an app"}]
    },
    {
      "role": "assistant",
      "blocks": [{"type": "question", "text": "What kind of app?"}]
    }
  ],
  "hasProposal": true,
  "confirmedGoalId": null,
  "createdAt": "2026-07-22T10:00:00Z",
  "updatedAt": "2026-07-22T10:05:00Z"
}
```

**Status Values:**
- `ACTIVE` – Session ongoing
- `CONFIRMED` – Proposal confirmed and goal created
- `CANCELLED` – Session cancelled

---

### Cancel Decomposition

**Endpoint:** `POST /v1/ai/goal-decompose/{sessionId}/cancel`

Cancel an active decomposition session.

**Response:** `204 No Content`

---

## System

### Health Check

**Endpoint:** `GET /v1/test`

Health check endpoint. No authentication required.

**Query Parameters (optional):**
- `error` (string) – If provided, throws a test exception

**Response (200):** `"Working!!"`

---

## Error Handling

All errors return a consistent JSON structure:

```json
{
  "message": "Human-readable message",
  "statusCode": 400,
  "errorCode": "ERROR_CODE",
  "info": {},
  "timestamp": "2024-01-01T12:00:00"
}
```

**Validation Errors** include field-level details:

```json
{
  "message": "Validation failed",
  "statusCode": 422,
  "errorCode": "VALIDATION_ERROR",
  "info": {
    "errors": [
      {
        "field": "email",
        "message": "must be a well-formed email address",
        "rejectedValue": "bad"
      }
    ]
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Request body validation failed |
| `MALFORMED_REQUEST_BODY` | 400 | Invalid or missing JSON body |
| `MISSING_PARAMETER` | 400 | Required query/param missing |
| `TYPE_MISMATCH` | 400 | Parameter type mismatch |
| `ROUTE_NOT_FOUND` | 404 | Endpoint does not exist |
| `METHOD_NOT_ALLOWED` | 405 | HTTP method not supported |
| `AUTHENTICATION_FAILED` | 401 | Missing/invalid/expired JWT |
| `ACCESS_DENIED` | 403 | No permission |
| `USER_NOT_FOUND` | 404 | User not found |
| `GOAL_NOT_FOUND` | 404 | Goal not found / not owned |
| `TASK_NOT_FOUND` | 404 | Task not found / not owned |
| `TASK_CYCLIC_DEPENDENCY` | 400 | Dependency would create a cycle |
| `ZONE_NOT_FOUND` | 404 | Zone not found / not owned |
| `ZONE_OVERLAP` | 409 | Zone time range overlaps |
| `INSUFFICIENT_POINTS` | 400 | Not enough points to deduct |
| `OTP_RATE_LIMIT_EXCEEDED` | 429 | Too many OTP requests |
| `OTP_INVALID_CODE` | 401 | Wrong OTP code |
| `OTP_LOCKED` | 423 | Too many wrong attempts |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh token expired |
| `REFRESH_TOKEN_REUSE_DETECTED` | 401 | Token reuse – all sessions revoked |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |

---

## Collection Variables

The Postman collection includes these variables for easy API testing:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `baseUrl` | `http://localhost:8080/api` | Base URL (paths append `/v1/...`) |
| `accessToken` | (empty) | Set after OTP Verify or Refresh |
| `goalId` | (empty) | Goal UUID for testing |
| `taskId` | (empty) | Task UUID for testing |
| `templateId` | (empty) | Template UUID for testing |
| `overrideId` | (empty) | Override UUID for testing |
| `zoneId` | (empty) | Zone UUID for testing |
| `sessionId` | (empty) | Session UUID for testing |
| `date` | `2026-07-22` | Sample date for testing |
| `categoryId` | (empty) | Category UUID for testing |
| `templateOverrideId` | (empty) | Template override UUID for testing |

---

## Quick Start Guide

### 1. Authenticate a User

```bash
# Request OTP
POST /v1/auth/otp/request
{
  "email": "user@example.com"
}

# Verify OTP (check email for code)
POST /v1/auth/otp/verify
{
  "email": "user@example.com",
  "code": "123456",
  "deviceId": "unique-device-id"
}
# Response contains: accessToken, refreshToken, user (with isNew flag)
```

### 2. Complete Onboarding (if isNew: true)

```bash
POST /v1/onboarding
Authorization: Bearer {accessToken}
{
  "firstName": "John",
  "lastName": "Doe",
  "birthDate": "1990-01-15",
  "timezone": "America/New_York",
  "preferredSessionDuration": 60,
  "bufferBetweenSessions": 10,
  "wakeupTime": "07:00:00",
  "sleepTime": "23:00:00",
  "schedulingType": "BALANCED"
}
```

### 3. Create a Goal with Tasks

```bash
POST /v1/goals
Authorization: Bearer {accessToken}
{
  "title": "Learn React",
  "description": "Master React and build projects",
  "targetDate": "2026-12-31",
  "tasks": [
    {
      "tempId": "tmp-1",
      "title": "Learn JSX basics",
      "estimatedDuration": 120,
      "mandatory": true,
      "estimatedPoints": 50,
      "allowTaskSplitting": false
    },
    {
      "tempId": "tmp-2",
      "title": "Build a Todo app",
      "estimatedDuration": 180,
      "dependsOnTempIds": ["tmp-1"]
    }
  ]
}
```

### 4. Set Up a Weekly Template

```bash
POST /v1/templates
Authorization: Bearer {accessToken}
{
  "name": "Work Week",
  "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "zones": [
    {
      "name": "Morning Focus",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "color": "#4CAF50"
    },
    {
      "name": "Afternoon Work",
      "startTime": "13:00:00",
      "endTime": "17:00:00",
      "color": "#2196F3"
    }
  ]
}
```

### 5. Schedule a Goal

```bash
POST /v1/schedule
Authorization: Bearer {accessToken}
{
  "goalId": "{goalId}"
}
# Returns scheduled sessions and any unscheduled tasks with reasons
```

### 6. View Your Schedule

```bash
GET /v1/tasks/date/2026-07-22
Authorization: Bearer {accessToken}
# Returns all tasks with sessions for that date

GET /v1/sessions/range?startDate=2026-07-20&endDate=2026-07-26
Authorization: Bearer {accessToken}
# Returns sessions grouped by date
```

---

## Authentication Best Practices

1. **Store tokens securely** – Use secure storage appropriate for your platform
2. **Refresh before expiry** – Refresh access tokens before they expire (24h)
3. **Handle token reuse** – If refresh token reuse is detected, all sessions are revoked; user must re-authenticate
4. **Persist deviceId** – Store the deviceId locally to maintain session continuity
5. **Logout properly** – Call logout endpoint when user quits to revoke the refresh token

---

## Rate Limiting

- **OTP Requests:** Maximum 3 per 10 minutes per email
- **OTP Attempts:** Maximum 5 wrong attempts before account lock (same email)

---

## Notes

- All timestamps are in ISO 8601 format
- All IDs are UUIDs (v4)
- LocalDate format: `YYYY-MM-DD`
- LocalTime format: `HH:mm:ss`
- LocalDateTime format: `YYYY-MM-DDTHH:mm:ss`
- IANA timezone examples: `Africa/Cairo`, `America/New_York`, `Europe/London`, `Asia/Tokyo`

---

**Last Updated:** July 24, 2026  
**API Version:** v1
