# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Awan — native Android client for an AI-assisted adaptive scheduling app. A cloud "AI Architect" (LLM behind a backend's POST request) decomposes goals into a strict JSON Contract of tasks; an on-device **Local Conflict Engine** does 100% of placement/overlap/dependency math. The AI never writes start times; the engine never guesses intent.

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.msayeh.awan.ExampleUnitTest"`
- Instrumented tests (device/emulator required): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lint`

Gradle 9.4.1 with configuration cache enabled; daemon toolchain is JVM 21. AGP 9.2.1, Kotlin 2.2.10, compileSdk 36 / minSdk 24, Compose BOM 2026.02.01. Version catalog: `gradle/libs.versions.toml`.

## Architecture

Current state: fresh single-`:app` Compose template. It is being restructured to **Now in Android (NiA) architecture** — do not add code to the flat template structure; build toward the target below.

### Mandatory skills — invoke before writing code

- `nowinandroid-architecture` — modularization, convention plugins, Hilt, offline-first data layer, feature modules. Use for any structural/scaffolding/module work.
- `navigation-3` — all navigation (NavKey/EntryProvider/Navigator, scenes, conditional nav). This project uses Navigation 3, not navigation-compose.
- `styles` — all styling/theming uses the Jetpack Compose Styles API (component themes, `Modifier.styleable`), not hardcoded parameters.
- `android-presentation-mvi` — ViewModels, screen State/Action/Event, Root/Screen composable split.
- `android-data-layer` — repositories, data sources, DTOs/mappers, Room, offline-first.
- `android-compose-ui` — composables, recomposition, previews, design-system components.

### Target structure (NiA)

- `build-logic/` convention plugins own shared Gradle config; module build files stay declarative.
- `:core:*` modules (model, database, network, data, datastore, designsystem, ui, common) and `:feature:*` modules; features depend on core, never on each other.
- Hilt DI throughout; UDF ViewModels exposing `StateFlow` of sealed UI state.
- Offline-first: Room is the single source of truth, UI observes it reactively, sync (Last-Write-Wins with server timestamps) runs in background via WorkManager.
- Package root: `com.awan.app`.

### Project-specific constraints

- **Local Conflict Engine** must be a pure-Kotlin module (no Android dependencies) so it's unit-testable and byte-identical with the iOS Swift engine against a shared QA test-vector suite. Algorithms are specified in the spec §9: interval-sweep overlap detection, Kahn's topological sort over `depends_on`, greedy slot-filling within zone windows respecting max focus-session length, Nightly Sweep with foreground catch-up.
- The **JSON Contract** (spec §8) is frozen: `goal_title`, `goal_deadline`, `tasks[]` with `id`, `title`, `zone`, `estimated_duration_minutes`, `priority` (low|medium|high), `deadline` (nullable), `depends_on`, `is_splittable`. Treat changes as breaking.
- **Zones**: four defaults (Study, Work, Play, Personal) with editable windows; every task carries a zone and the engine only places it inside that zone's window.
- No change is committed without user approval — conflicts surface an Intelligent Nudge (Skip / Double Up / Reschedule / Approve).
- Nightly Sweep runs via WorkManager but must always catch up on app foreground; never assume the background job ran.