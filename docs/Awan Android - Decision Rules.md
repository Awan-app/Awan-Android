# Awan Android - Decision Rules

These are the default implementation rules for Awan Android. Change them only with an explicit project decision.

## Fixed stack

- Dependency injection: Hilt with constructor injection and the existing `awan.android.hilt` convention plugin.
- Networking: Retrofit and OkHttp, with Kotlin serialization.
- Navigation: Navigation 3 using serializable `NavKey` routes, `NavBackStack`, `NavEntry`, and `NavDisplay`.
- Preferences: Proto DataStore.
- UI: Jetpack Compose with Compose Styles and components from `:core:design-system`.
- Errors: the existing `Result<T>` and `AppError` in `:core:common`.

Do not add a second library or wrapper for any of these concerns.

## Modules and dependencies

- Reuse an existing module before creating one. A single class or speculative boundary does not justify a module.
- `:feature:*` modules may depend on `:core:*`; features never depend on other features.
- Keep module builds declarative with the existing convention plugins: `awan.android.application`, `awan.android.library`, `awan.android.compose`, `awan.android.hilt`, and `awan.android.feature`.
- Keep versions and dependency aliases in `gradle/libs.versions.toml`.
- Keep the Local Conflict Engine pure Kotlin with no Android dependencies.

## Presentation and Compose

- Use one state model and explicit actions; add one-time events only for effects such as navigation or snackbars.
- ViewModels own application state and business decisions. Screen composables render immutable state and forward actions.
- Keep Root and Screen composables together: Root obtains the ViewModel and collects state; Screen accepts state and callbacks and remains previewable.
- Put reusable styling in `:core:design-system` and use Compose Styles instead of hardcoded component styling.
- Preserve accessibility basics: string resources, meaningful descriptions or semantics, adequate touch targets, and reduced-motion behavior.

## Data and errors

- Separate network DTOs, persistence models, domain models, and UI models; map between them at layer boundaries.
- A data source wraps one source. A repository coordinates sources or owns a durable domain contract; do not create pass-through repositories.
- For offline-first features, Room is the source of truth: network results are persisted, UI observes database flows, and background sync has a foreground catch-up path.
- Return the existing typed `Result<T>`/`AppError` for expected failures. Map exceptions once at the owning boundary and always rethrow `CancellationException`.
- Do not define duplicate result or error hierarchies in features.

## Navigation and DI

- Define serializable `NavKey` routes and assemble entries at the app boundary.
- Pass IDs or small scalar arguments, then load destination data; do not pass complex objects through navigation.
- Keep feature-to-feature navigation behind callbacks or app-level routing so features remain independent.
- Use Hilt modules only when constructor injection cannot provide the dependency.

## Tests

- Local JVM tests use JUnit Jupiter on the JUnit Platform, AssertK, Turbine, and `kotlinx-coroutines-test`.
- Android instrumentation remains on AndroidJUnit4 and Compose test rules.
- Prefer small fakes over mocks, direct `SavedStateHandle` construction, and focused tests for ViewModels and non-trivial domain or data logic.
- Add integration or end-to-end coverage only for a real cross-component risk; use the smallest test that would catch the regression.
