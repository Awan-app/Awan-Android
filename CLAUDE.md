# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Awan — native Android client for an AI-assisted adaptive scheduling app. A cloud "AI Architect" (LLM behind a backend's POST request) decomposes goals into a strict JSON Contract of tasks; an on-device **Local Conflict Engine** does 100% of placement/overlap/dependency math. The AI never writes start times; the engine never guesses intent.

Reference docs live in `docs/reference/` (architecture, layers, feature guide) and `docs/navigation_guide.md` (Navigation 3 patterns used here).

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.awan.app.ExampleUnitTest"`
- Instrumented tests (device/emulator required): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lint`

Gradle 9.4.1 with configuration cache enabled; daemon toolchain is JVM 21. AGP 9.2.1, Kotlin 2.4.0, compileSdk 37 / minSdk 26, Compose BOM 2026.06.01. Version catalog: `gradle/libs.versions.toml`.

## Git & Jira

Work is tracked in the `AWAN` Jira project (site: ezdo.atlassian.net). Before every commit or push, the relevant Jira issue ID (e.g. `AWAN-12`) must appear in either the branch name or the commit message — e.g. branch `feature/AWAN-12-kebab-description`, or commit message prefixed `AWAN-12: <summary>`.

If the issue ID isn't already known from context, search the `AWAN` Jira project for a matching issue by summary before committing. If no confident match is found, ask the user for the issue ID rather than guessing or omitting it.

## Architecture

The app follows **Now in Android (NiA) architecture** with **Clean Architecture layering**; the multi-module skeleton is in place — extend it, don't create parallel structures.

### Clean Architecture — non-negotiable

Dependency direction is always `presentation → domain ← data`. Domain depends on nothing but `:core:model` and `:core:common`.

- **ViewModels never touch a repository.** A ViewModel depends only on use cases from `:core:domain`. If a screen needs data, add or reuse a use case — never inject a repository, DAO, DataSource, Retrofit service, or DataStore into presentation code.
- **Repository contracts live in `:core:domain`**, in `<feature>/repository/`. `:core:data` only holds `…RepositoryImpl`, data sources, DTOs, mappers, and the Hilt `@Binds` module wiring impl → contract. A repository interface in `:core:data` is a bug.
- **Use cases live in `:core:domain`**, in `<feature>/usecase/`. One public `operator fun invoke` per use case; keep them thin — they orchestrate repositories and domain rules, they don't hold UI state. A use case in `:core:data` or in a feature module is a bug.
- **Domain models live in `:core:model`** (or `:core:domain/<feature>/model/` when feature-scoped). Domain types never expose Room entities, DTOs, or Retrofit/Proto types; mapping happens in `:core:data`.
- `:core:domain` stays Android-free in spirit — no Compose, no Android framework types in signatures.
- Reference vertical to copy: `core/domain/auth/` (contract + models + use cases) paired with `core/data/auth/` (impl + remote data source).

### Mandatory skills — invoke before writing code

- `nowinandroid-architecture` — modularization, convention plugins, Hilt, offline-first data layer, feature modules. Use for any structural/scaffolding/module work.
- `navigation-3` — all navigation (NavKey/EntryProvider/Navigator, scenes, conditional nav). This project uses Navigation 3, not navigation-compose.
- `styles` — all styling/theming uses the Jetpack Compose Styles API (component themes, `Modifier.styleable`), not hardcoded parameters.
- `android-presentation-mvi` — ViewModels, screen State/Action/Event, Root/Screen composable split. ViewModels consume use cases only.
- `android-data-layer` — repository impls, data sources, DTOs/mappers, Room, offline-first. Contracts stay in `:core:domain`.
- `android-compose-ui` — composables, recomposition, previews, design-system components.

### Current structure

- `build-logic/` convention plugins own shared Gradle config: `awan.android.application`, `awan.android.library`, `awan.jvm.library`, `awan.android.compose`, `awan.android.hilt`, `awan.android.feature`, `awan.android.room`, `awan.android.navigation`. Module build files stay declarative — apply these instead of repeating config.
- `:core:*` modules: `model` (domain models), `domain` (repository contracts + use cases), `data` (repository impls, data sources, DTOs/mappers), `database` (Room), `common` (dispatchers, `Result`, `AppError`), `datastore` + `datastore-proto` (Proto DataStore prefs, encrypted token storage), `network` (Retrofit/OkHttp, auth interceptor + token authenticator), `design-system` (Awan components, Styles API themes, tokens), `navigation` (`Navigator`, `NavigationState`, `Route`).
- `:feature:*` modules with **api/impl split** (api = routes only, impl = EntryProvider + screens): splash, onboarding, auth, home, calendar, chat, goals, profile, marketplace, ai-tasks. Features depend on core and other features' `api` modules, never on their `impl`. Exception: `:feature:add-task` has no split — it's a state-driven sheet, not a navigation destination, so it exports no Route and only `:app` consumes it.
- `:app` hosts the Navigation 3 shell: `AwanApp`, `AwanAppState`, `TopLevelDestination`, `MainActivity`.
- Hilt DI throughout; UDF ViewModels exposing `StateFlow` of sealed UI state.
- Packages: `com.awan.app` (app), `com.awan.app.core.*` (core), `com.awan.feature.*` (features).

### Offline-first contract — non-negotiable

Room is the single read model. Show local data first, then refresh it from the network when online.
Every repository holding server-backed product data uses exactly these four shapes:

| Shape | Signature | Rules |
|---|---|---|
| **Observe** | `fun observeX(...): Flow<T>` | Room only. Never the network, never a connectivity check. **Never call a `suspend` DAO method inside `map`** — a Flow re-emits only for the tables its own queries touch, so a one-shot read inside `map` silently goes stale. Span tables with one `@Query` (subqueries count) or `combine()` of DAO Flows. |
| **One-shot read** | `suspend fun getX(...): Result<T>` | Only where observing is impossible. Online → remote → write local → read Room; offline → read Room; Room empty → `Result.Error`. Reference: `ProfileRepositoryImpl.getProfile()`. |
| **Refresh** | `suspend fun refreshX(...): Result<Unit>` | Connectivity check first. If **any** GET fails, return `Result.Error` and write nothing — a partial refresh must never reach Room. On success, exactly one call to `replaceX(scope, items)`. |
| **Mutate** | `suspend fun createX/updateX/deleteX(...): Result<T>` | Connectivity check first, returning `AppError.Network`. On success, exactly one local write — write-through of the response, or `refreshX()`. **A mutation must never return `Result.Success` without a local write.** |

- **Every refresh replaces; it never merges.** `replaceX(scope, items)` deletes the rows in scope and
  inserts the response, in one transaction. An upsert-only refresh is a bug: it can never remove what
  another device deleted, and the user sees a record that no longer exists.
- **Scope must be provable from the endpoint.** A complete list (`GET v1/templates`) is authoritative
  for the whole table. A date-ranged response is authoritative for those dates only. **A paginated or
  filtered response is authoritative for nothing** — never delete from one. `listGoals` is page 0 and
  excludes the Inbox goal; replacing from it deletes the user's Inbox.
- **All Room writes for a feature live in one `<Feature>LocalDataSource`** in `core/data/<feature>/local/`.
  Repositories and `OfflineSyncCoordinator` call it; nothing else writes those tables. Reads may use
  DAO Flows directly. One writer per table group is what makes "no deleted data survives" structural.
- **No offline write queue.** Writes require connectivity; reads work fully offline.
- Refresh runs on screen open and after every write, and both bypass the TTL. Only `SyncWorker`
  honours it. Freshness is per-row: entities carry `expiryTime`, stamped **only inside `replaceX`**,
  from `SyncTtl` (schedule 15 min, goals 30 min, profile/categories/templates 1 h). Background
  refresh runs through `SyncWorker` (WorkManager) driven by `OfflineSyncCoordinator`.
- A refresh failure never blanks the screen — cached data stays, the error is surfaced beside it.

**Reviewing this — three greps:** a DAO injected into a repository that isn't its own local data
source; any `upsert` in `OfflineSyncCoordinator` (must be zero — it may only call `replace*`); any
mutation returning the remote `Result` without a local write.

Current state and the traps in the not-yet-migrated tables (categories' FK, goals' pagination):
`docs/feature/offline-first/2026-08-09-replace-on-refresh.md`.

### Room migrations — silent data loss if skipped

`AwanDatabase` is at **version 4**, with real migrations and schemas exported to `core/database/schemas/`.

Any entity change needs a `Migration` **and** a version bump. `fallbackToDestructiveMigration(dropAllTables = true)` is still enabled as a backstop, so a missing or wrong migration **wipes the user's database instead of failing loudly** — it will look fine on a clean install and destroy data on upgrade. Migrations have needed follow-up fixes before; add the column to both the entity and the migration SQL, and check the exported schema JSON.

### Still to build

- `:core:ui` (shared stateless UI + UI models).
- Local Conflict Engine module (see constraints below).

### Project-specific constraints

- **Local Conflict Engine** must be a pure-Kotlin module (no Android dependencies) so it's unit-testable and byte-identical with the iOS Swift engine against a shared QA test-vector suite. Algorithms are specified in the spec §9: interval-sweep overlap detection, Kahn's topological sort over `depends_on`, greedy slot-filling within zone windows respecting max focus-session length, Nightly Sweep with foreground catch-up.
- The **JSON Contract** (spec §8) is frozen: `goal_title`, `goal_deadline`, `tasks[]` with `id`, `title`, `zone`, `estimated_duration_minutes`, `priority` (low|medium|high), `deadline` (nullable), `depends_on`, `is_splittable`. Treat changes as breaking.
- **Zones**: four defaults (Study, Work, Play, Personal) with editable windows; every task carries a zone and the engine only places it inside that zone's window.
- **Session status changes only through the dedicated endpoints** — `POST v1/sessions/{id}/complete|uncomplete|cancel`. The generic `PUT v1/sessions/{id}` moves times only and must never carry a status; `status` in create/edit bodies is ignored by the backend. Only `complete` returns a reward.
- **Points and streak are server-owned.** Never compute a balance client-side. They change only via session completion, the daily wheel, and store purchases; read them back from the response or `GET v1/gamification/progress`. A reward's `awarded`/`updated` flags — not its amounts — say whether anything was actually earned.
- No change is committed without user approval — conflicts surface an Intelligent Nudge (Skip / Double Up / Reschedule / Approve).
- Nightly Sweep runs via WorkManager but must always catch up on app foreground; never assume the background job ran.

## Feature plans

Every plan written for a feature — initial implementation, a refactor, a bugfix, any change — is saved to `docs/feature/<feature_name>/` before work starts.

- Filename: `YYYY-MM-DD-<what-the-plan-covers>.md`, e.g. `2026-07-20-implementation-plan.md`, `2026-08-03-zone-windows-refactor.md`. The date prefix sorts the folder chronologically — the newest file is the current state of the feature.
- Plans are immutable once written. A change of direction gets a **new** dated plan, never an edit to an older one; the older plan stays as the record of what was true then.
- When the work described by a plan is finished, append an **`## Implementation notes (what actually differed)`** section to the bottom of that plan — this is the only edit an existing plan ever gets. Record: build/test/lint status and how it was verified; anything that only showed up at runtime, with the root cause and why it would be easy to reintroduce; and every deviation from the plan as written. See `docs/feature/onboarding/2026-07-20-motion-and-zones-plan.md` for the shape.
- Before debugging or refactoring a feature, read its folder newest-first: latest plan for current behavior, older ones for how it got there. The implementation notes are usually the highest-value part — they hold the traps.

## Localization

- Every UI-facing string (and any string that will be rendered to the user) lives in `strings.xml` — never hardcode a display string in Kotlin/Compose.
- Key naming: `${module_name}_${string_name}`, e.g. `onboarding_day_bounds_title`. `module_name` is the feature/core module the string belongs to.
- The app ships English (`res/values/`) and Arabic (`res/values-ar/`). Every key goes in both, in the module that owns it — never only the default.