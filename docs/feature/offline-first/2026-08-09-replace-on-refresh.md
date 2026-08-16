# Replace-on-refresh: one enforced offline-first rule

Extends `2026-08-06-offline-first-ssot-plan.md` (AWAN-205) rather than superseding it. That plan
declared Room the single read model; this one closes the gaps where the implementation did not carry
it through, and turns the intent into a contract that can be reviewed mechanically.

## Context

Reported symptom: a zone template edited in profile appears there instantly but never reaches Home
until the user changes the day. Two independent causes:

1. **Zone mutations never reach Room.** All 12 mutations in `ZonesRepositoryImpl` call the network and
   return — yet its own `getZonesForDate`/`getEffectiveZones`, and Home, read Room. Profile looks
   correct only because it reads the network straight into in-memory state.
2. **Home's Flow cannot see zones.** `HomeRepositoryImpl.getDaySchedule` maps over
   `sessionDao.observeSessionsForDate(...)` and resolves zones with suspend one-shots *inside* the
   `map`. Room invalidates that Flow for the `sessions` table only, so building a new Flow — i.e.
   changing the day — is the only thing that re-reads zones.

The deletion half is systemic. Of five sync functions only `syncScheduleRange` removes anything;
`syncGoals`, `syncCategories`, `syncProfile` and `syncZonesAndTemplates` are upsert-only, so a
record deleted on another device never disappears here. `ZonesRepositoryImpl.deleteZone/deleteTemplate/
deleteOverride` and `HomeRepositoryImpl.deleteSession/deleteTask` delete on the server and leave the
Room row behind.

## Decisions

- Refresh on **screen open and after every write**; the background worker is unchanged.
- This change covers **zones + Home + Home's local deletes**. Categories, goals and tasks are
  deferred — see Traps below, because two of them fail badly if fixed the obvious way.
- **No offline write queue.** Mutations stay connectivity-gated, as AWAN-205 committed.

## The contract

Written into `CLAUDE.md` under "Offline-first contract". Four method shapes — observe (Room Flow
only, never a suspend DAO call inside `map`), one-shot read (network → local → Room, Room fallback),
refresh (all-or-nothing, then one `replaceX`), mutate (connectivity first, exactly one local write on
success). Every refresh replaces within a scope that is provable from the endpoint; a paginated or
filtered response is authoritative for nothing. All Room writes for a feature live in one
`<Feature>LocalDataSource`.

## Plan

**Phase 0 — DAO surface.** `ZoneDao.observeEffectiveZonesForDate(date, dayOfWeek)`: one `@Query`
whose subqueries span `zones`, `template_overrides` and `template_days_of_week`, encoding the
existing resolution rule. One query rather than a `combine` so Room's invalidation tracker covers all
three tables. Plus `TemplateDao.deleteAllTemplates()` and `TemplateOverrideDao.deleteAllOverrides()`.
No entity changes anywhere in this plan — **no migration, no version bump**.

**Phase 1 — Home reacts.** `getDaySchedule` becomes a `combine` of the sessions Flow and the new zone
Flow; `resolveZonesForDate` collapses to a pure mapper. `ZonesRepositoryImpl.resolveZoneEntitiesForDate`
uses the same DAO method, deleting logic duplicated across both repositories.

**Phase 2 — one writer.** New `core/data/zones/local/ZonesLocalDataSource(+Impl)` with
`replaceAll(templates, overrides, expiryTime)` in `database.withTransaction { }`, modelled on
`CalendarLocalDataSource`. It deletes all templates and overrides first; `zones` and
`template_days_of_week` CASCADE from both, so stale zones and day assignments go with them. The
DTO→entity mapping moves out of `OfflineSyncCoordinator.syncZonesAndTemplates`, which shrinks to
online check → TTL gate → two GETs → return false if either failed → `replaceAll`.

**Phase 3 — mutations land in Room.** `ZonesRepositoryImpl.refreshZones()` (both GETs → `replaceAll`),
appended to each of the 12 mutations via the existing `suspendOnSuccess` helper. One shared refresh
beats 12 bespoke write-throughs and is the only version where the three `delete*` mutations are
correct with no extra code.

**Phase 4 — refresh on screen open.** `refreshZones()` on `ZonesRepository` + `RefreshZonesUseCase`;
`refreshSchedule(date)` + `RefreshDayScheduleUseCase` delegating to `syncScheduleRange(date, date,
forceRefresh = true)`. Home refreshes on init and on date change; profile zone screens on load.
Screen-open refreshes force, bypassing the TTL; only the worker honours it. Refresh never blanks the
screen — cached data renders immediately and a failure leaves it in place.

**Home's local deletes.** `deleteSession` → `sessionDao.deleteSession`; `deleteTask` →
`taskDao.deleteTask` (sessions CASCADE) + `deleteAllDependenciesForTask`; `updateTaskDetails` →
upsert the merged entity; `updateSessionLock` → `cacheSession`. All four also lack the connectivity
guard the contract requires.

## Traps for whoever does the deferred tables

- **Categories.** `tasks.categoryId` is a `NO_ACTION` FK, so `deleteAllCategories()` inside a replace
  throws a constraint error whenever a task references a deleted category. Correct shape, in one
  transaction: `UPDATE tasks SET categoryId = NULL WHERE categoryId NOT IN (:ids)` → delete → upsert.
  Guard the empty-`ids` case — Room expands `IN ()` into invalid SQLite.
- **Goals.** `listGoals(includeInbox = false)` returns a `PageResponse` and `GoalRemoteDataSourceImpl`
  keeps only `.content`. **Replacing from it deletes the user's Inbox goal** and everything past page
  0. Needs pagination surfaced first; until then, upsert only. This is AWAN-205's own rule about
  partial responses.
- **Tasks.** `syncScheduleRange` cannot infer task deletion — the response is date-scoped and
  unscheduled tasks have no sessions in range. The right hook is the already-written, never-called
  `TaskDao.replaceTasksForGoal`, wired to `GET /goals/{id}`.
- **Profile.** Single row, no deletion semantics. Upsert is correct; nothing to do.
- **`deleteAllTemplates` blast radius.** Only `zones` and `template_days_of_week` reference
  `templates` today. Anything added later with an FK to `templates` is silently deleted by
  `replaceAll` too.

## Implementation notes (what actually differed)

### Verification

`./gradlew assembleDebug testDebugUnitTest lint --rerun-tasks` — all green. `connectedDebugAndroidTest`
**not run**: the DAO tests, including the invalidation test this whole design rests on, need a device.

### Deviations from the plan

- **Two one-method interfaces were added that the plan did not call for**, both forced by testability:
  `ScheduleSynchronizer` (the one `OfflineSyncCoordinator` method `HomeRepositoryImpl` needs — without
  it every Home repository test has to build the coordinator's fifteen dependencies) and
  `LocalDataCleaner` (wrapping `database.clearAllTables()`, because `AwanDatabase` is an abstract Room
  class a JVM test cannot construct, and the project has no mocking library).
- **The profile zone ViewModels do not call `RefreshZonesUseCase` on load.** They already read the
  network directly, and Home refreshes on its own open, so adding it there costs two GETs and changes
  nothing the user can see. If those screens ever move to Room reads, it has to go back in.
- **`HomeRepositoryImpl.updateSessionLock` and `updateTaskDetails` write through from the response**,
  not from the request values — the server is authoritative and the response is already in hand.

### Traps for whoever touches this next

- **`ZoneDao.observeEffectiveZonesForDate` is load-bearing and its correctness is a Room behaviour, not
  ours.** Everything reactive about Home depends on Room's invalidation tracker collecting
  `template_overrides` and `template_days_of_week` from the *subqueries*. If that ever stops holding,
  Home silently goes back to only updating on a day change — the failure is invisible without the
  androidTest. Do not "simplify" that query into per-table lookups.
- **`ZonesRepositoryImpl`'s 12 mutations each end in `.suspendOnSuccess { refreshZones() }`.** A
  thirteenth mutation that forgets it is invisible at runtime and fails `ZonesRepositoryImplTest`,
  which is parameterised over the whole list. Add the new mutation to that list.
- **`replaceAll` deletes all templates and overrides first.** Only `zones` and `template_days_of_week`
  hang off them today; anything added later with an FK to `templates` is deleted by it too.

## Verification

`./gradlew assembleDebug testDebugUnitTest lint`, plus `connectedDebugAndroidTest` for the DAO tests.
On a device: edit a zone in profile → Home updates without a day change; delete a template on one
device → it is gone on the other rather than merged back; delete a session, force-quit, reopen
offline → still gone; airplane mode → everything still renders from cache and writes report the
network error; install over an existing build → data survives.
