# AI Task Creation Endpoints — API Reference

Both endpoints return the same `TaskProposalResponse` shape — a **non-persisted proposal**. The client reviews the proposal and accepts individual tasks by posting the `draft` to `POST {{baseUrl}}/api/v1/tasks/with-sessions`.

**Auth:** All requests require `Authorization: Bearer {{accessToken}}`

---

## Table of Contents

1. [Create Task via AI](#1-create-task-via-ai)
2. [Image to Tasks](#2-image-to-tasks)
3. [Common Response Shape](#3-common-response-shape)
4. [Client Integration Flow](#4-client-integration-flow)
5. [Errors](#5-errors)

---

## 1. Create Task via AI

Convert a free-form natural language note into structured task proposals.

### Endpoint

```
POST {{baseUrl}}/api/v1/ai/task-create
Content-Type: application/json
Authorization: Bearer {{accessToken}}
```

### Request Body

```json
{
  "text": "Build login page with email and password validation, also set up DB schema. Gym Monday and Wednesday 6-8pm."
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `text` | string | yes | Free-form note, max **4000** characters. The AI parses this into one or more tasks with estimated durations, categories, and scheduling suggestions. |

### Response — 200 OK

```json
{
  "sourceSummary": null,
  "tasks": [
    {
      "draft": {
        "task": {
          "title": "Build login page",
          "description": "Create a login page with email and password fields including validation",
          "estimatedDuration": 60,
          "mandatory": true,
          "estimatedPoints": 30,
          "allowTaskSplitting": false,
          "goalId": null,
          "categoryId": "cat-uuid-here"
        },
        "sessions": []
      },
      "aiProposedSessions": [
        {
          "zoneId": "zone-uuid-here",
          "start": "2026-07-30T09:00:00",
          "end": "2026-07-30T10:00:00",
          "status": "SCHEDULED"
        }
      ],
      "reason": "Your morning Development zone on Jul 30 has room, and this is a high-priority task."
    },
    {
      "draft": {
        "task": {
          "title": "Set up database schema",
          "description": "Design and create the user accounts database schema",
          "estimatedDuration": 45,
          "mandatory": true,
          "estimatedPoints": 20,
          "allowTaskSplitting": false,
          "goalId": null,
          "categoryId": null
        },
        "sessions": []
      },
      "aiProposedSessions": [],
      "reason": "No suitable time slot was found in the planning horizon due to existing commitments."
    }
  ],
  "timestamp": "2026-07-30T08:30:00Z"
}
```

---

## 2. Image to Tasks

Upload an image (handwritten notes, whiteboard, screenshot, printed schedule) and extract actionable tasks.

### Endpoint

```
POST {{baseUrl}}/api/v1/ai/image-to-tasks
Content-Type: multipart/form-data
Authorization: Bearer {{accessToken}}
```

### Request — Multipart Form-Data Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `image` | file | yes | Image file. Supported: **PNG, JPEG, WebP, GIF**. Max size: **10 MB**. |
| `note` | text | no | Optional context or instructions, e.g. `"Focus on the top 3 items"` or `"Finish these by Friday"`. |

### Pipeline (two AI models)

1. **Vision model** (Google Gemini) reads the image → produces a plain-text report of what it sees (tasks, dates, times).
2. **Planning model** (OpenAI-compatible) converts that report into structured `TaskProposalResponse` — **same shape** as the task-create endpoint.

The vision model deliberately does **not** resolve relative dates ("Fri", "3pm") — the planning model handles that using the user's timezone.

### Response — 200 OK

```json
{
  "sourceSummary": "TASK 1: Buy groceries for the week\nTASK 2: Finish project report\nTASK 3: Call dentist for appointment at 3pm Friday",
  "tasks": [
    {
      "draft": {
        "task": {
          "title": "Buy groceries",
          "description": "Buy groceries for the week",
          "estimatedDuration": 45,
          "mandatory": false,
          "estimatedPoints": 10,
          "allowTaskSplitting": false,
          "goalId": null,
          "categoryId": null
        },
        "sessions": []
      },
      "aiProposedSessions": [],
      "reason": "This is a routine errand; it can fit into any available free slot."
    },
    {
      "draft": {
        "task": {
          "title": "Finish project report",
          "description": "Complete and submit the project report",
          "estimatedDuration": 120,
          "mandatory": true,
          "estimatedPoints": 50,
          "allowTaskSplitting": true,
          "goalId": null,
          "categoryId": null
        },
        "sessions": []
      },
      "aiProposedSessions": [
        {
          "zoneId": "zone-uuid-here",
          "start": "2026-07-30T14:00:00",
          "end": "2026-07-30T16:00:00",
          "status": "SCHEDULED"
        }
      ],
      "reason": "Your afternoon focus block on Jul 30 has enough room for this 2-hour task."
    },
    {
      "draft": {
        "task": {
          "title": "Call dentist",
          "description": "Call dentist to schedule an appointment at 3pm Friday",
          "estimatedDuration": 10,
          "mandatory": false,
          "estimatedPoints": 5,
          "allowTaskSplitting": false,
          "goalId": null,
          "categoryId": null
        },
        "sessions": [
          {
            "zoneId": null,
            "start": "2026-08-01T15:00:00",
            "end": "2026-08-01T15:10:00",
            "status": "SCHEDULED"
          }
        ]
      },
      "aiProposedSessions": [],
      "reason": "The source text explicitly states 'at 3pm Friday', so that time was preserved as-is."
    }
  ],
  "timestamp": "2026-07-30T08:30:00Z"
}
```

### When the image has no tasks

```json
{
  "sourceSummary": "NO TASKS FOUND",
  "tasks": [],
  "timestamp": "2026-07-30T08:30:00Z"
}
```

---

## 3. Common Response Shape

Both endpoints return `TaskProposalResponse`:

| Field | Type | Description |
|---|---|---|
| `sourceSummary` | string \| null | `null` for typed notes. For images, the raw vision model report — **show this in the UI** so the user can verify the AI correctly read their image. |
| `tasks` | array | List of proposed tasks. Each item: |
| `tasks[].draft` | `TaskWithSessionsRequest` | The exact body to POST to `{{baseUrl}}/api/v1/tasks/with-sessions` to accept this task. No client-side mapping needed. |
| `tasks[].draft.task` | object | `title` (string), `description` (string \| null), `estimatedDuration` (int, minutes), `mandatory` (bool), `estimatedPoints` (int), `allowTaskSplitting` (bool), `goalId` (UUID \| null), `categoryId` (UUID \| null) |
| `tasks[].draft.sessions` | array | Timing the **source explicitly stated** (e.g. "Gym Mon 6-8pm"). Empty if no timing was mentioned. |
| `tasks[].aiProposedSessions` | array | AI's own **scheduling suggestion** grounded in real calendar availability. Empty when no suitable slot exists. |
| `tasks[].reason` | string | Why the AI proposed those times, or why none were found. Never blank. |
| `timestamp` | string (ISO 8601) | When the response was generated. |

**Session item shape** (used in both `draft.sessions` and `aiProposedSessions`):

| Field | Type | Description |
|---|---|---|
| `zoneId` | UUID \| null | The time zone this session falls in. `null` when unresolvable. |
| `start` | string (ISO datetime) | Session start. |
| `end` | string (ISO datetime) | Session end. |
| `status` | string | Always `"SCHEDULED"`. |

---

## 4. Client Integration Flow

```
User types a note or uploads an image
              │
              ▼
  POST /api/v1/ai/task-create       POST /api/v1/ai/image-to-tasks
  (JSON body: {text})                (multipart: image + optional note)
              │                                    │
              └──────────┬─────────────────────────┘
                         ▼
            TaskProposalResponse (200)
                         │
                         ▼
          Show proposal to user in UI
       ┌─────────────────┼─────────────────┐
       │                  │                  │
  User edits        User accepts      User rejects
  draft fields      a task            all tasks
       │                  │                  │
       │                  ▼                  │
       │       POST /api/v1/tasks/           │
       │       with-sessions                 │
       │       Body: task.draft              │
       │                  │                  │
       │           Task created              │
       │           (201 Created)             │
       └──────────────────┘                  └── Done (discard)
```

### Two scheduling channels — keep them separate

Each `ProposedTask` has two independent session lists:

- **`draft.sessions`** — Timing the user's source **explicitly stated** ("Call dentist at 3pm Friday"). Treat these as **fixed** — the user asked for these times.
- **`aiProposedSessions`** — AI's suggestion based on availability. Present these as a **recommendation** the user can accept, modify, or ignore.

### How to accept a task

No transformation needed. `draft` is literally the request body. Just POST it:

```
POST {{baseUrl}}/api/v1/tasks/with-sessions
Content-Type: application/json
Authorization: Bearer {{accessToken}}

<task.draft>
```

The user can also pick which sessions to keep:
- Use only `draft.sessions` → user's own timing
- Use only `aiProposedSessions` → AI suggestion
- Merge both → both timings become sessions
- Use neither `[]` → task is created without sessions (unscheduled)

### UI recommendations

| Feature | Suggestion |
|---|---|
| Loading state | Model calls take **2–10 seconds**. Show a beautiful loading animation using an animated version of the mascot creating or waiting something, create one if it doesn't exist. Alternate loading text with beautiful animations to keep the user assured the task is running. |
| Image preview | Show the uploaded image above or next to the extracted tasks for comparison. |
| Task cards | Render each `ProposedTask` as a card showing the title, description, duration, points, category, and sessions. Reuse the same theme, look, and feel of the app. Also reuse components that already exist in the app.|
| Session badges | Show `draft.sessions` as normal badges and `aiProposedSessions` as recommended badges (e.g., with a sparkle icon). |
| Draft Editing | The editing for draft tasks should be as straight forward as possible. It should alow the user to edit all the details easily. |
| Accept flow | An acceptance button somewhere in the screen, keeping it always visible. Make it sticky on the screen. |
| Category null | When `categoryId` is `null`, show an unassigned badge. |

### Loading states (full list)

| State | UI Treatment |
|---|---|
| Uploading image | Progress bar or indeterminate spinner on the image |
| Error (timeout/server) | Retry button + error message |
| Success (empty tasks) | "No actionable tasks found" with option to retry with a different image/text, also advise the user to try to take a better photo or write a better problem |
| Success (tasks found) | Show proposal cards |

---

## 5. Errors

### Common errors (both endpoints)

| Status | Error Code | Reason |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Missing, invalid, or expired JWT token. |
| 503 | `AI_UNAVAILABLE` | AI service is unavailable or returned unparseable output. Retry. |

### Endpoint-specific errors

#### Create Task via AI

| Status | Error Code | Reason |
|---|---|---|
| 422 | `VALIDATION_ERROR` | Text is blank or exceeds 4000 characters. |

#### Image to Tasks

| Status | Error Code | Reason |
|---|---|---|
| 400 | `INVALID_OPERATION` | No file provided, file is empty, or file exceeds 10MB. |
| 415 | `UNSUPPORTED_IMAGE_TYPE` | Unsupported format. Only PNG, JPEG, WebP, and GIF accepted. |

### Error response shape

```json
{
  "message": "Human-readable error description",
  "statusCode": 401,
  "errorCode": "AUTHENTICATION_FAILED",
  "info": {},
  "timestamp": "2026-07-30T08:30:00Z"
}
```

Validation errors include field-level details in `info.errors`:

```json
{
  "message": "Validation failed",
  "statusCode": 422,
  "errorCode": "VALIDATION_ERROR",
  "info": {
    "errors": [
      {
        "field": "text",
        "message": "Text must be at most 4000 characters",
        "rejectedValue": "..."
      }
    ]
  },
  "timestamp": "2026-07-30T08:30:00Z"
}
```

---

## Quick Reference

| | Create Task via AI | Image to Tasks |
|---|---|---|
| **Method** | `POST` | `POST` |
| **URL** | `{{baseUrl}}/api/v1/ai/task-create` | `{{baseUrl}}/api/v1/ai/image-to-tasks` |
| **Content-Type** | `application/json` | `multipart/form-data` |
| **Request** | `{"text": "..."}` | `image` (file) + `note` (text, optional) |
| **Response** | `TaskProposalResponse` (200) | `TaskProposalResponse` (200) |
| **`sourceSummary`** | Always `null` | Vision report text |
| **Accepts via** | `POST /api/v1/tasks/with-sessions` with `draft` | Same |
