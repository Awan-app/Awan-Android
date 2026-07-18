# Architecture Diagrams

All architecture diagrams from the documentation, consolidated in one place.

---

## 1. Overall Architecture

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

---

## 2. Module Dependency Graph

```mermaid
graph TB
    APP[":app"]:::app

    FY_API["foryou:api"]:::feature
    FY_IMPL["foryou:impl"]:::feature
    BK_API["bookmarks:api"]:::feature
    BK_IMPL["bookmarks:impl"]:::feature
    INT_API["interests:api"]:::feature
    INT_IMPL["interests:impl"]:::feature
    TP_API["topic:api"]:::feature
    TP_IMPL["topic:impl"]:::feature
    SR_API["search:api"]:::feature
    SR_IMPL["search:impl"]:::feature

    DATA["core:data"]:::core
    DB["core:database"]:::core
    NET["core:network"]:::core
    DS["core:datastore"]:::core
    DOM["core:domain"]:::core
    MOD["core:model"]:::core
    UI["core:ui"]:::core
    DES["core:designsystem"]:::core
    NAV["core:navigation"]:::core

    SYNC["sync:work"]:::sync

    APP --> FY_IMPL
    APP --> BK_IMPL
    APP --> INT_IMPL
    APP --> TP_IMPL
    APP --> SR_IMPL

    FY_IMPL --> FY_API
    FY_IMPL --> TP_API
    FY_IMPL --> DATA
    FY_IMPL --> DOM

    BK_IMPL --> BK_API
    BK_IMPL --> TP_API
    BK_IMPL --> DATA

    INT_IMPL --> INT_API
    INT_IMPL --> TP_API
    INT_IMPL --> DATA
    INT_IMPL --> DOM

    TP_IMPL --> TP_API
    TP_IMPL --> DATA

    SR_IMPL --> SR_API
    SR_IMPL --> INT_API
    SR_IMPL --> TP_API
    SR_IMPL --> DATA
    SR_IMPL --> DOM

    FY_API --> NAV
    BK_API --> NAV
    INT_API --> NAV
    TP_API --> NAV
    SR_API --> NAV

    DATA --> DB
    DATA --> NET
    DATA --> DS
    DATA --> MOD
    DB --> MOD
    NET --> MOD
    DOM --> DATA
    DOM --> MOD
    UI --> MOD
    UI --> DES

    SYNC --> DATA

    classDef app fill:#CAFFBF,stroke:#000,stroke-width:2px;
    classDef feature fill:#FFD6A5,stroke:#000,stroke-width:2px;
    classDef core fill:#9BF6FF,stroke:#000,stroke-width:2px;
    classDef sync fill:#FDFFB6,stroke:#000,stroke-width:2px;
```

---

## 3. Navigation Graph

```mermaid
graph TB
    subgraph "Top-Level Destinations"
        FY["ForYouNavKey<br/>(start)"]
        BK["BookmarksNavKey"]
        INT["InterestsNavKey"]
    end

    subgraph "Detail Destinations"
        TOPIC["TopicNavKey(id)"]
        SEARCH["SearchNavKey"]
        SETTINGS["Settings Dialog"]
    end

    FY --> TOPIC
    FY --> SEARCH
    FY --> SETTINGS
    BK --> TOPIC
    BK --> SETTINGS
    INT --> TOPIC
    INT --> SETTINGS
    SEARCH --> TOPIC
    SEARCH --> INT

    style FY fill:#CAFFBF,stroke:#333
    style BK fill:#CAFFBF,stroke:#333
    style INT fill:#CAFFBF,stroke:#333
    style TOPIC fill:#FFD6A5,stroke:#333
    style SEARCH fill:#FFD6A5,stroke:#333
    style SETTINGS fill:#FFD6A5,stroke:#333
```

---

## 4. Data Flow: For You Screen

```mermaid
sequenceDiagram
    participant WM as WorkManager
    participant SW as SyncWorker
    participant Repo as OfflineFirstNewsRepository
    participant Net as RetrofitNiaNetwork
    participant Dao as NewsResourceDao
    participant VM as ForYouViewModel
    participant UI as ForYouScreen

    VM->>Repo: observeAllForFollowedTopics()
    Repo->>Dao: getNewsResources() (Flow)
    Dao-->>Repo: Cached data
    Repo-->>VM: Flow<List<UserNewsResource>>
    VM-->>UI: NewsFeedUiState.Loading

    WM->>SW: doWork()
    SW->>Repo: sync()
    Repo->>Net: getNewsResourceChangeList()
    Net-->>Repo: Changes since version N
    Repo->>Net: getNewsResources(changedIds)
    Net-->>Repo: Full resources
    Repo->>Dao: upsertNewsResources()
    Dao-->>Repo: Flow re-emits
    Repo-->>VM: Updated list
    VM-->>UI: NewsFeedUiState.Success
```

---

## 5. Sync Strategy

```mermaid
graph TB
    A["Read local version<br/>(DataStore)"] --> B["Fetch change list<br/>since version"]
    B --> C{"Changes exist?"}
    C -->|No| D["Done ✓"]
    C -->|Yes| E["Partition into<br/>deleted + updated"]
    E --> F["Delete local<br/>models"]
    E --> G["Fetch & upsert<br/>updated models"]
    F --> H["Update local<br/>version number"]
    G --> H
    H --> D

    style A fill:#E8F5E9
    style B fill:#E3F2FD
    style D fill:#C8E6C9
    style C fill:#FFF9C4
```

---

## 6. Unidirectional Data Flow

```mermaid
graph LR
    USER["User Events<br/>(tap, swipe)"] -->|function calls| VM["ViewModel"]
    VM -->|updates| STATE["StateFlow<br/>(UI State)"]
    STATE -->|observed by| UI["Composable"]
    UI -->|triggers| USER

    style USER fill:#FFCDD2
    style VM fill:#FFE0B2
    style STATE fill:#FFF9C4
    style UI fill:#C8E6C9
```

---

## 7. Repository Pattern

```mermaid
graph TB
    REPO["Repository Interface"]
    IMPL["OfflineFirst Implementation"]
    DAO["Room DAO"]
    NET["Network Data Source"]
    DS["DataStore"]
    HILT["Hilt @Binds"]

    REPO -.->|bound by| HILT
    HILT -.->|to| IMPL
    IMPL --> DAO
    IMPL --> NET
    IMPL --> DS

    style REPO fill:#C8E6C9
    style IMPL fill:#FFE0B2
    style DAO fill:#BBDEFB
    style NET fill:#FFCDD2
    style DS fill:#E1BEE7
    style HILT fill:#FFF9C4
```

---

## 8. Data Model Mapping

```mermaid
graph LR
    NET_MODEL["NetworkNewsResource<br/>(API DTO)"]
    ENTITY["NewsResourceEntity<br/>(Room Entity)"]
    POPULATED["PopulatedNewsResource<br/>(Room Relation)"]
    MODEL["NewsResource<br/>(Public Model)"]
    USER_MODEL["UserNewsResource<br/>(Enriched)"]

    NET_MODEL -->|"asEntity()"| ENTITY
    ENTITY -->|"Room query"| POPULATED
    POPULATED -->|"asExternalModel()"| MODEL
    MODEL -->|"+ UserData"| USER_MODEL

    style NET_MODEL fill:#FFCDD2
    style ENTITY fill:#BBDEFB
    style POPULATED fill:#BBDEFB
    style MODEL fill:#C8E6C9
    style USER_MODEL fill:#FFF9C4
```

---

## 9. Feature Module Structure

```mermaid
graph TB
    subgraph "feature:topic"
        subgraph "api"
            KEY["TopicNavKey"]
            NAV_EXT["navigateToTopic()"]
        end
        subgraph "impl"
            SCREEN["TopicScreen"]
            VM["TopicViewModel"]
            ENTRY["topicEntry()"]
        end
    end

    KEY --> NAV_EXT
    ENTRY --> KEY
    ENTRY --> SCREEN
    SCREEN --> VM

    style KEY fill:#FFE0B2
    style NAV_EXT fill:#FFE0B2
    style SCREEN fill:#FFD6A5
    style VM fill:#FFD6A5
    style ENTRY fill:#FFD6A5
```

---

## 10. Back Stack Architecture

```mermaid
graph TB
    subgraph "NavigationState"
        TLS["topLevelStack: [ForYou, Bookmarks]"]

        subgraph "Sub-Stacks"
            FY["ForYou: [ForYouNavKey, TopicNavKey('compose')]"]
            BK["Bookmarks: [BookmarksNavKey, TopicNavKey('kotlin')]"]
            INT["Interests: [InterestsNavKey]"]
        end
    end

    TLS -->|"currentTopLevelKey"| BK

    style TLS fill:#CAFFBF
    style BK fill:#FFCDD2,stroke:#333,stroke-width:3px
    style FY fill:#E8F5E9
    style INT fill:#E8F5E9
```

---

## 11. Build Parallelism

```mermaid
graph TB
    subgraph "Wave 1: Leaf Modules"
        M["core:model"]
        C["core:common"]
        DP["core:datastore-proto"]
        DS_M["core:designsystem"]
        AN["core:analytics"]
    end

    subgraph "Wave 2: Core Infrastructure"
        DB["core:database"]
        NET["core:network"]
        DST["core:datastore"]
        NAV["core:navigation"]
    end

    subgraph "Wave 3: Data + Domain + APIs"
        DATA["core:data"]
        DOM["core:domain"]
        APIs["feature:*:api"]
    end

    subgraph "Wave 4: Features"
        IMPLs["feature:*:impl"]
    end

    subgraph "Wave 5: App"
        APP[":app"]
    end

    M --> DB
    M --> NET
    C --> DST
    DP --> DST
    DB --> DATA
    NET --> DATA
    DST --> DATA
    NAV --> APIs
    DATA --> DOM
    DATA --> IMPLs
    APIs --> IMPLs
    IMPLs --> APP

    style M fill:#BDB2FF
    style APP fill:#CAFFBF
```

---

## 12. State Machine

```mermaid
stateDiagram-v2
    [*] --> Loading : ViewModel created
    Loading --> Success : Data received from Flow
    Loading --> Error : Exception caught
    Error --> Loading : Retry / New emission
    Success --> Success : Data updated
    Error --> Success : Recovery
```

---

## 13. Hilt Dependency Graph

```mermaid
graph TB
    SC["SingletonComponent"]

    subgraph "Singleton Scope"
        DATABASE["NiaDatabase"]
        RETROFIT["RetrofitNiaNetwork"]
        DATASTORE["NiaPreferencesDataSource"]
    end

    subgraph "Unscoped (per injection)"
        NEWS_REPO["OfflineFirstNewsRepository"]
        TOPICS_REPO["OfflineFirstTopicsRepository"]
        USER_REPO["OfflineFirstUserDataRepository"]
    end

    subgraph "ViewModel Scope"
        FY_VM["ForYouViewModel"]
        TP_VM["TopicViewModel"]
    end

    SC --> DATABASE
    SC --> RETROFIT
    SC --> DATASTORE

    DATABASE --> NEWS_REPO
    RETROFIT --> NEWS_REPO
    DATASTORE --> NEWS_REPO
    DATABASE --> TOPICS_REPO
    RETROFIT --> TOPICS_REPO

    NEWS_REPO --> FY_VM
    TOPICS_REPO --> TP_VM

    style SC fill:#CAFFBF
    style DATABASE fill:#BBDEFB
    style RETROFIT fill:#FFCDD2
    style DATASTORE fill:#E1BEE7
    style NEWS_REPO fill:#FFE0B2
    style FY_VM fill:#C8E6C9
    style TP_VM fill:#C8E6C9
```

---

*All diagrams use Mermaid syntax and render in any Mermaid-compatible viewer (GitHub, GitLab, VS Code, etc.).*
