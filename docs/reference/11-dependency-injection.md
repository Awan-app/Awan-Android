# Chapter 11 — Dependency Injection

## Overview

NiA uses **Hilt** for dependency injection throughout the app. Hilt is the recommended DI framework for Android, built on top of Dagger with Android-specific conventions.

---

## Hilt Setup

The `nowinandroid.hilt` convention plugin configures Hilt for any module:

```kotlin
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("dagger.hilt.android.plugin")
            dependencies {
                "implementation"(libs.findLibrary("hilt.android").get())
                "ksp"(libs.findLibrary("hilt.compiler").get())
            }
        }
    }
}
```

---

## Module Types

### @Binds Modules (Interface Binding)

Used when binding an interface to its implementation:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsTopicRepository(
        topicsRepository: OfflineFirstTopicsRepository,
    ): TopicsRepository

    @Binds
    internal abstract fun bindsNewsResourceRepository(
        newsRepository: OfflineFirstNewsRepository,
    ): NewsRepository

    @Binds
    internal abstract fun bindsUserDataRepository(
        userDataRepository: OfflineFirstUserDataRepository,
    ): UserDataRepository
}
```

**Why `@Binds` over `@Provides`:**
- More efficient — Hilt generates less code
- Cleaner — one line per binding
- The implementation class must have `@Inject constructor`

### @Provides Modules

Used when construction logic is needed:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesNiaDatabase(
        @ApplicationContext context: Context,
    ): NiaDatabase = Room.databaseBuilder(
        context,
        NiaDatabase::class.java,
        "nia-database",
    ).build()
}
```

---

## Scoping

| Scope | Lifecycle | Usage in NiA |
|-------|-----------|--------------|
| `@Singleton` | Application process | Database, DataStore, Network client |
| `@ViewModelScoped` | ViewModel | (Rarely used — dependencies are typically Singleton) |
| Unscoped | Created each time | Repositories, Use Cases |

**Note:** In NiA, most repositories are **unscoped** (new instance per injection). This is intentional — repositories are lightweight wrappers around DAOs and data sources, which are themselves singletons.

---

## Constructor Injection

Most classes use `@Inject constructor`:

```kotlin
internal class OfflineFirstNewsRepository @Inject constructor(
    private val niaPreferencesDataSource: NiaPreferencesDataSource,
    private val newsResourceDao: NewsResourceDao,
    private val topicDao: TopicDao,
    private val network: NiaNetworkDataSource,
    private val notifier: Notifier,
) : NewsRepository { /* ... */ }
```

Hilt automatically provides all dependencies because:
- DAOs come from `@Provides` methods in the database module
- `NiaNetworkDataSource` is bound via `@Binds`
- `NiaPreferencesDataSource` has `@Inject constructor`

---

## ViewModel Injection

### Standard ViewModel

```kotlin
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userDataRepository: UserDataRepository,
    // ...
) : ViewModel()
```

### Assisted Injection (ViewModel with Runtime Arguments)

For ViewModels that need arguments not available at compile time (like `topicId`):

```kotlin
@HiltViewModel(assistedFactory = TopicViewModel.Factory::class)
class TopicViewModel @AssistedInject constructor(
    private val userDataRepository: UserDataRepository,
    topicsRepository: TopicsRepository,
    @Assisted val topicId: String,          // Runtime argument
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(topicId: String): TopicViewModel
    }
}
```

**Usage in entry provider:**

```kotlin
entry<TopicNavKey> { key ->
    TopicScreen(
        viewModel = hiltViewModel<TopicViewModel, TopicViewModel.Factory>(
            key = key.id,
        ) { factory -> factory.create(key.id) },
    )
}
```

---

## DI Architecture

```mermaid
graph TB
    subgraph "Hilt Component Hierarchy"
        SC["SingletonComponent"]
        AC["ActivityComponent"]
        VMC["ViewModelComponent"]
    end

    subgraph "Modules"
        DM["DataModule<br/>(@Binds repos)"]
        DBM["DatabaseModule<br/>(@Provides database)"]
        NM["NetworkModule<br/>(@Provides Retrofit)"]
    end

    SC --> AC --> VMC
    DM -->|@InstallIn| SC
    DBM -->|@InstallIn| SC
    NM -->|@InstallIn| SC

    style SC fill:#CAFFBF
    style AC fill:#C8E6C9
    style VMC fill:#E8F5E9
    style DM fill:#FFE0B2
    style DBM fill:#FFE0B2
    style NM fill:#FFE0B2
```

---

## Testing with Hilt

### Replacing Modules in Tests

NiA uses `core:data-test` module with fake implementations:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class TestDataModule {
    @Binds
    abstract fun bindsTopicsRepository(
        fakeTopicsRepository: TestTopicsRepository,
    ): TopicsRepository
}
```

In tests, the test module replaces the production module:

```kotlin
@HiltAndroidTest
@UninstallModules(DataModule::class)  // Remove production bindings
class ForYouScreenTest {
    // TestDataModule provides fakes automatically
}
```

### Testing ViewModels Without Hilt

For unit tests, construct ViewModels directly with fakes:

```kotlin
class TopicViewModelTest {
    private val userDataRepository = TestUserDataRepository()
    private val topicsRepository = TestTopicsRepository()

    private lateinit var viewModel: TopicViewModel

    @Before
    fun setup() {
        viewModel = TopicViewModel(
            userDataRepository = userDataRepository,
            topicsRepository = topicsRepository,
            userNewsResourceRepository = TestUserNewsResourceRepository(),
            topicId = "test-topic-id",
        )
    }
}
```

---

## Qualifier Annotations

NiA uses custom qualifiers for coroutine dispatchers:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val niaDispatcher: NiaDispatchers)

enum class NiaDispatchers { Default, IO }
```

**Usage:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Dispatcher(IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}

// In SyncWorker
class SyncWorker @AssistedInject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
)
```

---

## Summary

Hilt DI in NiA follows a clean pattern: `@Binds` for interface bindings, `@Provides` for construction logic, `@AssistedInject` for runtime-parameterized ViewModels, and custom qualifiers for dispatchers.

## Key Takeaways

- `@Binds` preferred over `@Provides` for interface bindings
- Most bindings are in `SingletonComponent`
- Repositories are unscoped — DAOs and data sources are singletons
- `@AssistedInject` enables ViewModels with runtime arguments
- Tests replace modules with `@UninstallModules` or construct directly with fakes

## Best Practices

- Use `@Binds` whenever possible — cleaner and more efficient
- Mark implementations `internal` — only expose interfaces
- Use qualifier annotations for dispatchers, not magic strings
- Convention plugin `nowinandroid.hilt` ensures consistent setup

## Common Mistakes

- **Using `@Provides` when `@Binds` would work** — Extra boilerplate
- **Forgetting `@Inject constructor` on implementation classes** — Hilt can't create them
- **Scoping repositories as `@Singleton`** — Usually unnecessary, adds memory pressure
- **Not using `@AssistedInject` for ViewModels with arguments** — Leads to workarounds with `SavedStateHandle`
