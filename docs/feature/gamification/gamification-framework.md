# Gamification — Frontend Guide

This is the mobile developer's reference for the gamification features: **points**, **streak**,
**daily wheel**, **store**, and **inventory**. It explains the user-facing behavior, the endpoints to
call, and the exact JSON each returns.

All endpoints are under `/api/v1` and need `Authorization: Bearer <accessToken>`.

---

## 1. The big picture

| Feature | What it is | Where the user sees it |
|---|---|---|
| **Points** | Currency earned by finishing sessions and spinning the daily wheel, spent in the store. | Profile / wallet, store checkout |
| **Streak** | Consecutive days with at least one completed session. | Profile / home banner |
| **Daily wheel** | A once-per-day spin that pays coins or a free item. | Daily gift screen |
| **Store** | Catalog of cosmetic items, each with a price in points. | Store screen |
| **Inventory** | Items the user has bought or won. | Profile / inventory screen |
| **Equipped items** | The cosmetics currently shown on the user's profile / avatar — one per type. | Profile / avatar |

Important rules:

1. **A session gives rewards only the first time it's completed.** Completing it again later gives
   nothing — you'll still get the response, just with `awarded: false`.
2. **Points only ever change through session completion, the daily wheel, and store purchases.**
   There is no "add points" endpoint.
3. **Session status only changes through dedicated endpoints** (`complete` / `uncomplete` /
   `cancel`). The old generic status-update endpoint is deprecated — see Section 3.
4. **Equipping or unequipping never changes points.** It only picks which owned item of each type
   is shown on the profile.
5. **The daily wheel's outcome is decided by the server.** The app animates toward the result it is
   given; it never picks the wedge or credits the prize itself — see Section 5.

---

## 2. Earning points & streak (completing a session)

### Endpoint

```
POST /api/v1/sessions/{sessionId}/complete
```

No request body.

### Behavior

- Marks the session as completed.
- On the session's **first** completion, the user earns:
  - **Points** = the task's `estimatedPoints` (if the task has no points, none are earned).
  - **Streak** — this counts as an "activity day" (see Section 4).
- Every completion returns the same shape; the `reward` tells you whether anything was actually
  awarded.

### Response — 200 OK

```json
{
  "session": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "start": "2026-07-22T09:00:00",
    "end": "2026-07-22T10:00:00",
    "status": "COMPLETED",
    "locked": false,
    "firstCompletedAt": "2026-07-22T09:00:00Z",
    "zoneId": "550e8400-e29b-41d4-a716-446655440002",
    "taskId": "550e8400-e29b-41d4-a716-446655440099"
  },
  "reward": {
    "points": { "awarded": true, "amount": 25, "oldValue": 150, "newValue": 175 },
    "streak": { "updated": true, "oldValue": 5, "newValue": 6, "maxStreakBroken": true, "maxStreakOld": 6, "maxStreakNew": 7 }
  }
}
```

### The `reward` object — how to animate/celebrate

| Field | Meaning |
|---|---|
| `reward.points.awarded` | `true` = points were actually added. **Show the "+25 points" toast/confetti only when this is true.** |
| `reward.points.amount` | Points added (`0` when `awarded` is false). |
| `reward.points.oldValue` / `newValue` | Balance before / after. |
| `reward.streak.updated` | `true` = the visible streak changed. |
| `reward.streak.newValue` | New streak number to display. |
| `reward.streak.maxStreakBroken` | `true` = new personal best. **Good moment for a special celebration.** |
| `reward.streak.maxStreakNew` | New best streak. |

**Frontend tip:** always read `awarded`/`updated` — don't assume completing a session always gives
points. On re-completion you'll get `awarded: false`, `updated: false`, and the values will be
unchanged.

### Errors

| HTTP | Code | When |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in / expired token. |
| 404 | `SESSION_NOT_FOUND` | Session doesn't exist or isn't the user's. |
| 400 | `INVALID_OPERATION` | Session can't be completed from its current state (e.g. already cancelled). |

---

## 3. Session status — what changed

Sessions now change status only through **dedicated endpoints**. There is no generic "set the
status" endpoint anymore, and creating or editing a session never sets its status.

### How a session's status changes

| Action | Endpoint | Result |
|---|---|---|
| Mark as done (earns points/streak) | `POST /api/v1/sessions/{sessionId}/complete` | `COMPLETED` |
| Undo a completion (no points are taken back) | `POST /api/v1/sessions/{sessionId}/uncomplete` | `SCHEDULED` |
| Call it off | `POST /api/v1/sessions/{sessionId}/cancel` | `CANCELLED` |

### What is deprecated / ignored

- **`PATCH /api/v1/sessions/{sessionId}/status`** is deprecated. If you still call it, it just
  forwards to the matching endpoint above (`COMPLETED` → complete, `SCHEDULED` → uncomplete,
  `CANCELLED` → cancel). **Migrate to the dedicated endpoints** — the old one will be removed.
- **`status` in the create/edit request bodies is ignored.** Sessions are always created
  `SCHEDULED`, and editing a session only changes its times — never its status. Remove `status`
  from your payloads.

### Rules to know

- A completed session can only go back to `SCHEDULED` (`uncomplete`). It can't be cancelled.
- A cancelled session stays cancelled — it can't be reopened or completed.
- `uncomplete` and `cancel` return a plain `SessionResponse` (no `reward`). Rewards only come from
  `complete`.

### Errors

| HTTP | Code | When |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in / expired token. |
| 404 | `SESSION_NOT_FOUND` | Session doesn't exist or isn't the user's. |
| 400 | `INVALID_OPERATION` | Illegal transition (e.g. completing a cancelled session). |

---

## 4. Streak

The streak counts **consecutive days with at least one completed session**. Completing several
sessions on the same day still counts as one day.

### Rules the user will feel

- Complete something today → streak grows by 1 (if you were active yesterday too).
- Complete nothing for a full day → streak resets to 0.
- **No manual reset anywhere** — it only resets by missing a day.

### `GET /api/v1/gamification/progress`

The one call for the profile / streak banner.

**Response — 200 OK:**

```json
{
  "points": 175,
  "streak": 6,
  "maxStreak": 7
}
```

| Field | Meaning |
|---|---|
| `points` | Total points available to spend. |
| `streak` | Current streak (`0` if the user missed a day). |
| `maxStreak` | Best streak ever. |

### `GET /api/v1/gamification/activity-dates?startDate=...&endDate=...`

Used to render a "streak calendar" (which days were active). Dates are `YYYY-MM-DD`.

**Query parameters:**

| Param | Required | Example |
|---|---|---|
| `startDate` | yes | `2026-07-01` |
| `endDate` | yes | `2026-07-31` |

**Response — 200 OK:**

```json
["2026-07-01", "2026-07-02", "2026-07-03"]
```

Empty array (`[]`) means no activity in that range.

---

## 5. Daily wheel

A once-per-day spin. The user taps "spin", the wheel animates, and it lands on a wedge that pays
either coins or a free store item.

**The server decides the outcome, not the app.** You send an empty `POST` and get back which wedge
was won; you animate the wheel so it stops there. Never pick the wedge locally, and never credit
coins or items locally — the response is the only source of truth.

### Rules the user will feel

- One spin per day. The day rolls over at **midnight in the user's own timezone**, same as
  everything else in the app.
- **Every spin wins something.** There is no "you lost" wedge.
- Missing a day costs nothing — yesterday's unspun gift is simply gone, and today's spin is
  unaffected. The wheel has no streak of its own and does not touch the session streak.

### `GET /api/v1/gamification/wheel/config`

Call this when the daily-gift screen opens. It gives you the wheel layout *and* whether the spin is
still available.

**Response — 200 OK:**

```json
{
  "segments": [
    { "segmentId": "SEG_1", "coins": 1, "payoutType": "COINS" },
    { "segmentId": "SEG_2", "coins": 5, "payoutType": "COINS" },
    { "segmentId": "SEG_3", "coins": 10, "payoutType": "COINS" },
    { "segmentId": "SEG_4", "coins": 20, "payoutType": "COINS" },
    { "segmentId": "SEG_5", "coins": 50, "payoutType": "COINS" },
    { "segmentId": "SEG_6", "coins": 100, "payoutType": "COINS" },
    { "segmentId": "SEG_ITEM", "coins": 0, "payoutType": "ITEM" }
  ],
  "claimedToday": true,
  "lastClaim": {
    "segmentId": "SEG_4",
    "coinsAwarded": 20,
    "itemId": null,
    "itemName": null,
    "claimDate": "2026-07-22",
    "claimedAt": "2026-07-22T09:14:03.221Z"
  }
}
```

| Field | Meaning |
|---|---|
| `segments` | The wedges, **in wheel order**. The order is stable across calls, so wedge *i* is always the same wedge — safe to lay out once. |
| `segments[].segmentId` | Stable id (`SEG_1` … `SEG_ITEM`). This is what a spin result matches against. |
| `segments[].coins` | Coins this wedge pays. `0` on the item wedge — render it as the item/gift wedge, not as "0 coins". |
| `segments[].payoutType` | `COINS` or `ITEM`. |
| `claimedToday` | `true` → already spun today, disable the button. |
| `lastClaim` | Most recent claim, or `null` if the user has never spun. Handy for "you won 20 coins yesterday". |

The odds are **not** in the response. Weights are server-side only, so don't try to show or compute
probabilities.

### `POST /api/v1/gamification/wheel/spin`

No request body. No query parameters.

**Response — 200 OK (coins):**

```json
{
  "segmentId": "SEG_2",
  "payoutType": "COINS",
  "coinsAwarded": 5,
  "newBalance": 180,
  "item": null
}
```

**Response — 200 OK (item):**

```json
{
  "segmentId": "SEG_ITEM",
  "payoutType": "ITEM",
  "coinsAwarded": 0,
  "newBalance": 175,
  "item": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Aurora Frame",
    "description": "A glowing animated frame for your avatar",
    "image": "https://cdn.example.com/frames/aurora.png",
    "info": "Rarity: epic",
    "price": 200,
    "version": "1.0",
    "type": "FRAME"
  }
}
```

| Field | Meaning |
|---|---|
| `segmentId` | The wedge to land the animation on. Always one of the `segmentId`s from the config. |
| `payoutType` | `COINS` or `ITEM`. **Branch on this**, not on the wedge's configured `payoutType` — see the note below. |
| `coinsAwarded` | Coins credited. `0` when `payoutType` is `ITEM`. |
| `newBalance` | Wallet balance after the spin. Use it directly — no need to re-call `/progress`. |
| `item` | The won item when `payoutType` is `ITEM`, otherwise `null`. Same `ItemResponse` shape the store uses. |

**About the item wedge.** The item is picked at random from the items the user does *not* already
own, and it lands in the inventory for free — `price` in the response is the catalog price, not a
charge. If the user already owns everything, the spin quietly resolves to a coin wedge instead: you
will get `SEG_4` / `COINS` / `20`, never `SEG_ITEM` with a `null` item. That's why you should read
`payoutType` from the spin response rather than looking the wedge up in the config — for that one
case the two disagree, and the response is right.

After an item win the inventory has changed, so refresh `GET /api/v1/store/inventory` (Section 7) if it's
already on screen.

### Errors

| HTTP | `errorCode` | What to do |
|---|---|---|
| 409 | `DAILY_GIFT_ALREADY_CLAIMED` | Already spun today. `info.claimDate` is the day. Disable the button and show `lastClaim`. **Nothing is credited on a rejected spin** — don't re-add coins. |
| 401 | `AUTHENTICATION_FAILED` | Not logged in. |
| 404 | `USER_NOT_FOUND` | User not found. |

A 409 is the normal answer to a double tap, so treat it as a state correction rather than a failure:
flip the button to its claimed state instead of showing an error dialog.

---

## 6. Store

### `GET /api/v1/store/items?type=FRAME`

Lists everything available to buy. `type` is optional (`FRAME`, `SKIN`, `THEME`, `ICON`); omit it
to get the whole catalog.

**Response — 200 OK:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Gold Frame",
    "description": "A shiny gold frame for your profile.",
    "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
    "info": null,
    "price": 100,
    "version": "1.0",
    "type": "FRAME"
  }
]
```

| Field | Meaning |
|---|---|
| `id` | Use this to buy the item. |
| `name` / `description` | Display text. |
| `image` | Image URL (may be `null`). |
| `price` | Cost in points. |
| `type` | Category: `FRAME`, `SKIN`, `THEME`, `ICON`. |

**Tip:** compare `price` against the `points` from `/progress` to enable/disable the Buy button.

---

## 7. Inventory

### `GET /api/v1/store/inventory`

Everything the user already owns — bought from the store or won on the daily wheel. Wheel wins look
identical to purchases here (`boughtAt` is set either way), so the inventory alone doesn't tell you
how an item was acquired.

**Response — 200 OK:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440011",
    "item": {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Gold Frame",
      "description": "A shiny gold frame for your profile.",
      "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
      "info": null,
      "price": 100,
      "version": "1.0",
      "type": "FRAME"
    },
    "boughtAt": "2026-07-22T10:00:00Z"
  }
]
```

### `POST /api/v1/store/items/{itemId}/buy`

Buys an item. No request body. Deducts the price from points and adds the item to inventory.

**Response — 200 OK:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440011",
  "item": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Gold Frame",
    "description": "A shiny gold frame for your profile.",
    "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
    "info": null,
    "price": 100,
    "version": "1.0",
    "type": "FRAME"
  },
  "boughtAt": "2026-07-22T10:00:00Z"
}
```

### Buy errors to handle in the UI

| HTTP | Code | What to tell the user |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in. |
| 404 | `ITEM_NOT_FOUND` | Item doesn't exist (shouldn't happen from the catalog). |
| 409 | `ITEM_ALREADY_OWNED` | "You already own this item" — disable Buy for owned items (compare with inventory). |
| 400 | `INSUFFICIENT_POINTS` | "Not enough points." The response `info` includes `currentPoints` and `requestedPoints`. |

---

## 8. Equipping items

You can pick which owned cosmetic is shown for each type (`FRAME`, `SKIN`, `THEME`, `ICON`). There
is **one slot per type** — so at most 4 equipped items at once. Equipping never costs points; it
only changes what's displayed.

### `GET /api/v1/store/equipped`

Lists everything currently equipped — the source of truth for rendering the profile/avatar.

**Response — 200 OK:**

```json
[
  {
    "type": "FRAME",
    "item": {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Gold Frame",
      "description": "A shiny gold frame for your profile.",
      "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
      "info": null,
      "price": 100,
      "version": "1.0",
      "type": "FRAME"
    },
    "equippedAt": "2026-07-22T12:00:00Z"
  }
]
```

| Field | Meaning |
|---|---|
| `type` | Which slot this fills: `FRAME`, `SKIN`, `THEME`, `ICON`. |
| `item` | The equipped item (same shape as inventory items). |
| `equippedAt` | When it was equipped. |

Empty array (`[]`) means nothing is equipped yet.

### `POST /api/v1/store/items/{itemId}/equip`

Equips an owned item. No request body.

- The item must be in the user's **inventory** — otherwise `ITEM_NOT_OWNED` (409).
- One slot per type: equipping an item whose type is already occupied **silently replaces** the old
  one. No error, the old item just goes back to being "owned but not equipped".
- Re-equipping the same item is a **no-op** — safe to call again, returns the same slot.

**Response — 200 OK:** the `EquippedItemResponse` above for that slot.

### `DELETE /api/v1/store/equipped/{type}`

Clears the slot for a type. Path param is the `ItemType` (`FRAME`, `SKIN`, `THEME`, `ICON`).

**Response — 204:** No content.

Idempotent — clearing an empty slot still returns `204`, so the client doesn't need to check first.

### Equip/unequip errors to handle in the UI

| HTTP | Code | What to tell the user |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in. |
| 404 | `ITEM_NOT_FOUND` | Item doesn't exist (shouldn't happen from the catalog). |
| 404 | `USER_NOT_FOUND` | User not found. |
| 409 | `ITEM_NOT_OWNED` | "You don't own this item" — only enable equip for items in the inventory. |
| 400 | `TYPE_MISMATCH` | Invalid `type` value in the delete path (not one of the four). |

### Where to get the equipped state

`GET /api/v1/users/me` returns an `equippedItems` array with the same `EquippedItemResponse`
shape. It's convenient for pre-filling the profile/avatar and for the "which item of each type is
active" indicator — no separate call needed. `/store/equipped` and the profile always agree.
