# AWAN-205: Offline-first single source of truth

## Outcome

For authenticated product data, Room is the sole read model. The network is used only to refresh Room or to perform an online mutation; a successful mutation writes its returned server state to Room before UI state changes. Authentication, theme/language preferences, and transient in-progress drafts are out of scope for the offline mutation lock.

## Constraints

- Keep the existing module direction: presentation -> domain <- data.
- Preserve the current design system, Navigation 3 routing, MVI ownership, and user-visible translations in both `values` and `values-ar`.
- Add every new third-party dependency through a `build-logic` convention plugin. Feature/app/data `build.gradle.kts` files may apply that convention, but must not declare the new WorkManager or Hilt-Work libraries directly.
- Do not invent API contracts. The Postman Awan collection is authoritative for endpoint shapes.
- No write queue or optimistic offline edits. When offline, add, edit, delete, status change, move, reorder, onboarding submission, AI confirmation, profile/server preferences, template/override/zone/routine mutations are unavailable.

## Database and local reads

1. Upgrade `AwanDatabase` to version 2 with a tested migration from schema v1.
2. Persist the missing server-backed data needed by the UI:
   - categories;
   - sessions, including task/zone relationships, time range, status, and lock state;
   - cached schedule dates so an empty-but-refreshed day is distinguishable from an uncached day.
3. Make task `goalId` nullable so an Inbox task is representable and persist its optional category relationship. Preserve referential integrity and use DAO transactions for coherent schedule replacement/upsert.
4. Expose local `Flow`/suspend queries from DAOs and repositories for every product read. A repository `get*`, `observe*`, or UI use case must not call Retrofit directly.
5. Do not infer a server deletion from a missing item in a paginated or partial response. The task-range response is authoritative only for the requested dates: replace scheduled sessions for exactly those dates (including empty days), then mark each as cached. Leave task records intact unless a deletion response explicitly confirms it.

## Sync and connectivity

1. Add a domain-facing connectivity monitor backed by validated `ConnectivityManager` capabilities. It must report online only for a validated internet-capable network.
2. Add an `OfflineSyncCoordinator` in the data layer. It refreshes profile/preferences, paginated goals and their task data, categories, templates, overrides, zones, and the task/session schedule into Room. Upsert returned server IDs rather than retaining client placeholders.
3. Fetch Today through Today + 6 days on startup/foreground/reconnect/manual refresh, plus a date when the user opens it. Use `GET /v1/tasks/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`; it is an inclusive date-keyed map whose entries contain a task plus sessions for that date.
4. Add network-constrained periodic and immediate WorkManager sync. Use WorkManager 2.11.2 and Hilt Work 1.4.0 through a new build-logic convention plugin. Configure `HiltWorkerFactory` in `AwanApplication` and remove WorkManager's default initializer so there is one configuration path.
5. Background sync failures must leave the last Room snapshot visible and must not corrupt/clear cache.

## Online mutations and UI

1. Every server mutation first checks connectivity in the shared domain/data path and returns the existing typed `AppError.Network` while offline. Do not rely solely on disabled UI controls.
2. On an online mutation, persist the returned server entity/entities in Room before exposing success. Do not keep synthetic session/task data or a separate in-memory schedule cache.
3. Convert Home, Calendar, goals, Add Task, AI task/goal confirmation, profile, zones/templates/routines, and onboarding ViewModels to derive product state from Room flows and to request sync rather than fetch as a read side effect. Keep auth outside this migration.
4. Add one app-root offline banner. Disable or hide every server-editing entry point in offline mode, with an accessible explanation where a disabled control remains visible. Audit at least:
   - global and Home Add Task;
   - Add Task submit and AI create/confirm/accept/retry actions;
   - goal creation/edit/delete and AI goal confirmation;
   - Home task/session status, move, drag/reorder, and delete actions;
   - profile name/picture/server preference editors;
   - zones, templates, overrides, routines, and onboarding submit controls.
   Theme/language and unsent local draft fields remain usable.
5. Home's add action opens the existing Quick Add with the selected date, zone, and slot prefilled. Its online request must use `POST /v1/tasks/with-sessions`; omission/null `goalId` creates an Inbox task. Do not create a fake local session.
6. Home's status/move calls use the returned session record to update Room. Reorder performs sequential remote updates, stops at first failure, never optimistically mutates Room, displays the error, then refreshes the affected date.

## Verification

- Add focused unit tests for connectivity guarding, Room-only repository reads, date-range replacement including empty dates, successful mutation persistence, and the v1-to-v2 migration.
- Add UI/ViewModel tests for offline edit locks and Home action behavior where practical.
- Run relevant module tests and `:app:assembleDebug`; report any environment limitation rather than weakening checks.

## Postman contract notes

- `GET /v1/tasks/range` is inclusive and returns a `LocalDate -> TaskWithSessionsResponse[]` map; each task carries sessions only for that date.
- `POST /v1/tasks/with-sessions` accepts an optional/null `goalId` for Inbox and returns the task plus created sessions.
- `PUT /v1/sessions/{sessionId}` returns the updated session; only persisted lifecycle statuses are `SCHEDULED`, `COMPLETED`, and `CANCELLED`.
- `GET /v1/zones/date/{date}` resolves override first, then weekday template, then an empty list.
