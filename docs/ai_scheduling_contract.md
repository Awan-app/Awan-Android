# AI Goal Scheduling — API Contract 



## 1. Request a Schedule Proposal (No Persistence)

This endpoint asks the AI to generate a schedule proposal for all tasks in a goal. **It does not write anything to the database.** 

> [!NOTE]
> This endpoint can take up to 10 seconds to return as it waits for the LLM.

**Endpoint:** `POST /api/v1/ai/schedule`  
**Auth:** Bearer Token Required

### Request Body
```json
{
  "goalId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Response Body
The response is split into three lists:
1. `proposedSessions`: Placements inside a matched zone. Ready to be confirmed.
2. `suggestions`: Placements that require a compromise (either `NO_ZONE` or `OVERLAP`).
3. `unscheduledTasks`: Tasks that genuinely could not fit anywhere.

```json
{
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  
  "proposedSessions": [
    {
      "taskId": "770e8400-e29b-41d4-a716-446655440001",
      "taskTitle": "Design Database Schema",
      "zoneId": "990e8400-e29b-41d4-a716-446655440002",
      "start": "2026-08-01T09:00:00",
      "end": "2026-08-01T10:00:00"
    }
  ],
  
  "suggestions": [
    {
      "taskId": "880e8400-e29b-41d4-a716-446655440003",
      "taskTitle": "Write API Documentation",
      "zoneId": null,
      "start": "2026-08-02T14:00:00",
      "end": "2026-08-02T15:30:00",
      "suggestionType": "NO_ZONE",
      "reason": "No matching zone was free. Placed in unzoned free time on Sunday afternoon.",
      "overlapInfo": null
    },
    {
      "taskId": "990e8400-e29b-41d4-a716-446655440004",
      "taskTitle": "Deploy to Staging",
      "zoneId": "990e8400-e29b-41d4-a716-446655440002",
      "start": "2026-08-03T10:00:00",
      "end": "2026-08-03T11:00:00",
      "suggestionType": "OVERLAP",
      "reason": "No free time found. Overlapping 'Team sync' (optional, 5 pts) — cheapest conflict available.",
      "overlapInfo": {
        "taskTitle": "Team sync",
        "start": "2026-08-03T10:30:00",
        "end": "2026-08-03T11:30:00",
        "mandatory": false,
        "points": 5
      }
    }
  ],
  
  "unscheduledTasks": [
    {
      "taskId": "aa0e8400-e29b-41d4-a716-446655440005",
      "taskTitle": "Client Meeting",
      "message": "The only remaining slots require overlapping a locked session."
    }
  ]
}
```

> **Mobile UI Suggestion:** Render `proposedSessions` normally. For items in `suggestions`, show a warning indicator (e.g. ⚠️) and display the AI's `reason`. If `overlapInfo` is present, explicitly show the user what will be displaced before they tap "Confirm".

---

## 2. Confirm the Schedule (Persists to Database)

Once the user reviews the proposal, the client sends back an array of the sessions the user actually wants to keep. The client can include any mix of `proposedSessions` and accepted `suggestions`.

**Endpoint:** `POST /api/v1/ai/schedule/confirm`  
**Auth:** Bearer Token Required

### Request Body
You construct this array by mapping the selected sessions from the proposal into `ConfirmSessionItem` objects.
> Note: For `NO_ZONE` suggestions, `zoneId` must be sent as `null`.

```json
{
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  "sessions": [
    {
      "taskId": "770e8400-e29b-41d4-a716-446655440001",
      "zoneId": "990e8400-e29b-41d4-a716-446655440002",
      "start": "2026-08-01T09:00:00",
      "end": "2026-08-01T10:00:00"
    },
    {
      "taskId": "880e8400-e29b-41d4-a716-446655440003",
      "zoneId": null,
      "start": "2026-08-02T14:00:00",
      "end": "2026-08-02T15:30:00"
    }
  ]
}
```

### Response Body
The server responds with the actually persisted sessions, which now include their real database `id` (the `sessionId`).

```json
[
  {
    "id": "bb0e8400-e29b-41d4-a716-446655440006",
    "taskId": "770e8400-e29b-41d4-a716-446655440001",
    "zoneId": "990e8400-e29b-41d4-a716-446655440002",
    "start": "2026-08-01T09:00:00",
    "end": "2026-08-01T10:00:00"
  },
  {
    "id": "cc0e8400-e29b-41d4-a716-446655440007",
    "taskId": "880e8400-e29b-41d4-a716-446655440003",
    "zoneId": null,
    "start": "2026-08-02T14:00:00",
    "end": "2026-08-02T15:30:00"
  }
]
```
