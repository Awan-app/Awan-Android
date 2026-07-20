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
- Static analysis: `./gradlew detekt` (config: `detekt.yml` at repo root, with compose rules + formatting)

Gradle 9.4.1 with configuration cache enabled; daemon toolchain is JVM 21. AGP 9.2.1, Kotlin 2.4.0, compileSdk 37 / minSdk 24, Compose BOM 2026.06.01. Version catalog: `gradle/libs.versions.toml`.

## Git & Jira

Work is tracked in the `AWAN` Jira project (site: ezdo.atlassian.net). Before every commit or push, the relevant Jira issue ID (e.g. `AWAN-12`) must appear in either the branch name or the commit message — e.g. branch `feature/AWAN-12-kebab-description`, or commit message prefixed `AWAN-12: <summary>`.

If the issue ID isn't already known from context, search the `AWAN` Jira project for a matching issue by summary before committing. If no confident match is found, ask the user for the issue ID rather than guessing or omitting it.

## Architecture

The app follows **Now in Android (NiA) architecture**; the multi-module skeleton is in place — extend it, don't create parallel structures.

### Mandatory skills — invoke before writing code

- `nowinandroid-architecture` — modularization, convention plugins, Hilt, offline-first data layer, feature modules. Use for any structural/scaffolding/module work.
- `navigation-3` — all navigation (NavKey/EntryProvider/Navigator, scenes, conditional nav). This project uses Navigation 3, not navigation-compose.
- `styles` — all styling/theming uses the Jetpack Compose Styles API (component themes, `Modifier.styleable`), not hardcoded parameters.
- `android-presentation-mvi` — ViewModels, screen State/Action/Event, Root/Screen composable split.
- `android-data-layer` — repositories, data sources, DTOs/mappers, Room, offline-first.
- `android-compose-ui` — composables, recomposition, previews, design-system components.

### Current structure

- `build-logic/` convention plugins own shared Gradle config: `awan.android.application`, `awan.android.library`, `awan.android.compose`, `awan.android.hilt`, `awan.android.feature`. Module build files stay declarative — apply these instead of repeating config.
- `:core:*` modules: `common` (dispatchers, `Result`, `AppError`), `datastore` + `datastore-proto` (Proto DataStore prefs, encrypted token storage), `network` (Retrofit/OkHttp, auth interceptor + token authenticator), `design-system` (Awan components, Styles API themes, tokens), `navigation` (`Navigator`, `NavigationState`, `Route`).
- `:feature:*` modules with **api/impl split** (api = routes only, impl = EntryProvider + screens): splash, onboarding, auth, home, calendar, chat, goals, profile, profile-setup. Features depend on core and other features' `api` modules, never on their `impl`.
- `:app` hosts the Navigation 3 shell: `AwanApp`, `AwanAppState`, `TopLevelDestination`, `MainActivity`.
- Hilt DI throughout; UDF ViewModels exposing `StateFlow` of sealed UI state.
- Packages: `com.awan.app` (app), `com.awan.app.core.*` (core), `com.awan.feature.*` (features).

### Still to build (target NiA modules not yet present)

- `:core:model`, `:core:database` (Room), `:core:data` (repositories), `:core:ui`.
- Offline-first: Room is the single source of truth, UI observes it reactively, sync (Last-Write-Wins with server timestamps) runs in background via WorkManager.
- Local Conflict Engine module (see constraints below).

### Project-specific constraints

- **Local Conflict Engine** must be a pure-Kotlin module (no Android dependencies) so it's unit-testable and byte-identical with the iOS Swift engine against a shared QA test-vector suite. Algorithms are specified in the spec §9: interval-sweep overlap detection, Kahn's topological sort over `depends_on`, greedy slot-filling within zone windows respecting max focus-session length, Nightly Sweep with foreground catch-up.
- The **JSON Contract** (spec §8) is frozen: `goal_title`, `goal_deadline`, `tasks[]` with `id`, `title`, `zone`, `estimated_duration_minutes`, `priority` (low|medium|high), `deadline` (nullable), `depends_on`, `is_splittable`. Treat changes as breaking.
- **Zones**: four defaults (Study, Work, Play, Personal) with editable windows; every task carries a zone and the engine only places it inside that zone's window.
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
- Every string must be provided for all languages the app supports — add the key to each `res/values-*/strings.xml`, not just the default `res/values/strings.xml`.