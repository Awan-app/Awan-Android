# Now in Android — Architecture Documentation

> **Complete engineering handbook** for understanding, extending, and building upon the Now in Android architecture.

---

## Introduction

Now in Android (NiA) is Google's official reference application demonstrating modern Android development best practices. It is a fully functional app that delivers news about Android development, built with Jetpack Compose, Kotlin, Hilt, Room, DataStore, WorkManager, Navigation 3, and Material 3.

This documentation explains **how the project works internally**, **why each architectural decision was made**, and **how a developer can extend the project** by adding new features.

---

## Architecture Overview

```mermaid
graph TB
    subgraph "UI Layer"
        UI["Composables<br/>(Jetpack Compose)"]
        VM["ViewModels<br/>(State Holders)"]
    end

    subgraph "Domain Layer"
        UC["Use Cases"]
    end

    subgraph "Data Layer"
        REPO["Repositories"]
        LOCAL["Local Data Sources<br/>(Room + DataStore)"]
        REMOTE["Remote Data Sources<br/>(Retrofit)"]
    end

    UI -- "observes state" --> VM
    VM -- "calls" --> UC
    UC -- "reads from" --> REPO
    VM -- "calls" --> REPO
    REPO -- "reads/writes" --> LOCAL
    REPO -- "fetches from" --> REMOTE

    style UI fill:#E8F5E9,stroke:#2E7D32
    style VM fill:#E8F5E9,stroke:#2E7D32
    style UC fill:#FFF3E0,stroke:#E65100
    style REPO fill:#E3F2FD,stroke:#1565C0
    style LOCAL fill:#E3F2FD,stroke:#1565C0
    style REMOTE fill:#E3F2FD,stroke:#1565C0
```

**Key principles:**

- **Unidirectional Data Flow (UDF)**: Events flow down, data flows up
- **Reactive streams**: All data exposed as Kotlin `Flow`
- **Offline-first**: Local storage is source of truth, network syncs in background
- **Feature modularization**: Each feature owns its UI, ViewModel, and navigation
- **Dependency inversion**: Upper layers depend on abstractions, not implementations

---

## High-Level Module Map

```mermaid
graph TB
    subgraph ":app"
        APP["app<br/>Scaffold, Navigation Host,<br/>Top-Level Destinations"]
    end

    subgraph ":feature"
        direction LR
        FY["foryou<br/>(api + impl)"]
        BK["bookmarks<br/>(api + impl)"]
        INT["interests<br/>(api + impl)"]
        TP["topic<br/>(api + impl)"]
        SR["search<br/>(api + impl)"]
        ST["settings<br/>(impl)"]
    end

    subgraph ":core"
        direction LR
        DATA["data"]
        DB["database"]
        DS["datastore"]
        NET["network"]
        DOM["domain"]
        MOD["model"]
        UI_CORE["ui"]
        DES["designsystem"]
        NAV["navigation"]
        COM["common"]
        ANA["analytics"]
        NOT["notifications"]
        TEST["testing"]
    end

    subgraph ":sync"
        SYNC["work"]
    end

    APP -.-> FY
    APP -.-> BK
    APP -.-> INT
    APP -.-> TP
    APP -.-> SR
    APP -.-> ST

    FY --> DATA
    FY --> UI_CORE
    BK --> DATA
    INT --> DATA
    TP --> DATA
    SR --> DATA

    DATA --> DB
    DATA --> NET
    DATA --> DS
    DB --> MOD
    NET --> MOD
    DOM --> DATA

    classDef app fill:#CAFFBF,stroke:#000,stroke-width:2px;
    classDef feature fill:#FFD6A5,stroke:#000,stroke-width:2px;
    classDef core fill:#9BF6FF,stroke:#000,stroke-width:2px;
    classDef sync fill:#FDFFB6,stroke:#000,stroke-width:2px;

    class APP app
    class FY,BK,INT,TP,SR,ST feature
    class DATA,DB,DS,NET,DOM,MOD,UI_CORE,DES,NAV,COM,ANA,NOT,TEST core
    class SYNC sync
```

---

## Application Flow

```mermaid
sequenceDiagram
    participant User
    participant Splash
    participant MainActivity
    participant NiaApp
    participant ForYouScreen
    participant ViewModel
    participant Repository
    participant Network
    participant Room

    User->>Splash: Launch app
    Splash->>MainActivity: Splash completes when UserData loads
    MainActivity->>NiaApp: setContent with NiaTheme
    NiaApp->>ForYouScreen: NavDisplay renders start destination
    ForYouScreen->>ViewModel: Collect feedState
    ViewModel->>Repository: observeAllForFollowedTopics()
    Repository->>Room: Query news resources (Flow)
    Room-->>Repository: Emit cached data
    Repository-->>ViewModel: Flow of UserNewsResource
    ViewModel-->>ForYouScreen: NewsFeedUiState.Success

    Note over NiaApp: WorkManager triggers sync in background
    Network-->>Repository: New data from API
    Repository->>Room: Upsert new records
    Room-->>Repository: Updated Flow emission
    Repository-->>ViewModel: Updated list
    ViewModel-->>ForYouScreen: UI re-renders
```

---

## Navigation Flow

```mermaid
graph LR
    subgraph "Top-Level Destinations (Bottom Nav)"
        ForYou["For You"]
        Bookmarks["Bookmarks"]
        Interests["Interests"]
    end

    subgraph "Detail Destinations"
        Topic["Topic Detail"]
        Search["Search"]
        Settings["Settings Dialog"]
    end

    ForYou --> Topic
    ForYou --> Search
    Bookmarks --> Topic
    Interests --> Topic
    Interests --> Search
    ForYou --> Settings
    Bookmarks --> Settings
    Interests --> Settings

    style ForYou fill:#CAFFBF,stroke:#333
    style Bookmarks fill:#CAFFBF,stroke:#333
    style Interests fill:#CAFFBF,stroke:#333
    style Topic fill:#FFD6A5,stroke:#333
    style Search fill:#FFD6A5,stroke:#333
    style Settings fill:#FFD6A5,stroke:#333
```

---

## Documentation Index

| # | Chapter | Description |
|---|---------|-------------|
| 1 | [Project Overview](01-project-overview.md) | Why this project exists, its goals, and architectural principles |
| 2 | [Project Structure](02-project-structure.md) | Every folder explained with dependency direction diagrams |
| 3 | [Module Architecture](03-module-architecture.md) | Deep dive into core, feature, and data modules |
| 4 | [Clean Architecture](04-clean-architecture.md) | Layers, repository pattern, data flow, and sequence diagrams |
| 5 | [UI Layer](05-ui-layer.md) | Compose architecture, ViewModel, state, events, and effects |
| 6 | [Domain Layer](06-domain-layer.md) | Use cases, business rules, threading, and testing |
| 7 | [Data Layer](07-data-layer.md) | Repository implementation, offline-first, sync strategy |
| 8 | [Navigation](08-navigation.md) | **Comprehensive** Navigation 3 guide — the most detailed chapter |
| 9 | [Feature Development Guide](09-feature-development-guide.md) | Step-by-step tutorial for adding new features |
| 10 | [State Management](10-state-management.md) | StateFlow, UI state modeling, sealed hierarchies |
| 11 | [Dependency Injection](11-dependency-injection.md) | Hilt modules, bindings, scopes, and testing DI |
| 12 | [Build Logic](12-build-logic.md) | Convention plugins, version catalog, composite builds |
| 13 | [Gradle Modules](13-gradle-modules.md) | Module dependencies, API vs implementation |
| 14 | [Testing](14-testing.md) | Unit tests, UI tests, fakes, Compose testing |
| 15 | [Best Practices](15-best-practices.md) | Architectural principles, common mistakes, scaling advice |
| — | [Architecture Diagrams](architecture-diagrams.md) | All diagrams in one place |

---

## Quick Reference

| Technology | Usage |
|---|---|
| **Kotlin** | Primary language |
| **Jetpack Compose** | UI toolkit (Material 3) |
| **Navigation 3** | Type-safe navigation with `NavKey`, `NavDisplay`, `entryProvider` |
| **Hilt** | Dependency injection |
| **Room** | Local database |
| **DataStore (Proto)** | User preferences |
| **Retrofit** | Network requests |
| **WorkManager** | Background sync |
| **Coroutines + Flow** | Async and reactive programming |
| **Kotlin Serialization** | Serialization of nav keys and network models |

---

*This documentation is based on the latest version of the [Now in Android](https://github.com/android/nowinandroid) repository.*
