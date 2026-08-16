# MCP Connection Details Refactor Design Spec

## Overview
This specification details the refactor of the Model Context Protocol (MCP) feature across all layers of the Awan Android application. The goal is to align with the new backend contract (`GET /api/v1/mcp/settings/connection-details`), unify the UI into a cohesive "Connect AI Assistant" screen displaying the MCP Server URL, Client ID, and configuration guides, and eradicate all obsolete token/API key management code across `:core:network`, `:core:data`, `:core:domain`, `:core:database`, and `:feature:profile`.

---

## 1. Backend Contract

### Endpoint
- **Method**: `GET`
- **Path**: `/api/v1/mcp/settings/connection-details` (Retrofit relative path: `v1/mcp/settings/connection-details`)
- **Authentication**: `Bearer <user_jwt_token>` (Injected via existing `AuthInterceptor`)

### Response (`200 OK`)
```json
{
  "mcpUrl": "https://awanproduction.up.railway.app/mcp",
  "clientId": "awan-mcp"
}
```

### Error Responses
- `401 Unauthorized`: Handled via standard auth interceptor / token authenticator flow.
- `AppError.Network` / other network failures: Handled via `safeApiCall` and `ProfileErrorMapper`.

---

## 2. Architecture & Layer Responsibilities

### 2.1 Core Network Layer (`:core:network`)
- **Remove**:
  - `ApiKeyResponseDto.kt`
  - `ApiKeySummaryDto.kt`
  - `CreateApiKeyRequestDto.kt`
  - Obsolete endpoints from `McpApiService` (`getApiKeys`, `createApiKey`, `revokeApiKey`)
- **Add**:
  - `McpConnectionDetailsDto.kt`:
    ```kotlin
    @Serializable
    data class McpConnectionDetailsDto(
        val mcpUrl: String,
        val clientId: String,
    )
    ```
- **Update**:
  - `McpApiService.kt`:
    ```kotlin
    @GET("v1/mcp/settings/connection-details")
    suspend fun getConnectionDetails(): McpConnectionDetailsDto
    ```

### 2.2 Core Database Layer (`:core:database`)
- **Remove**:
  - `McpTokenEntity.kt`
  - `McpTokenDao.kt`
  - References in `AwanDatabase.kt` and `DatabaseModule.kt`

### 2.3 Core Domain Layer (`:core:domain`)
- **Domain Model**:
  - Retain `McpConnectionDetails(val mcpUrl: String, val clientId: String)`
  - Delete `McpToken.kt`, `CreatedMcpToken.kt`
- **Repository Interface**:
  - `McpRepository.kt`:
    ```kotlin
    interface McpRepository {
        fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>>
    }
    ```
- **Use Cases**:
  - Retain `GetMcpConnectionDetailsUseCase.kt`
  - Delete `CreateMcpTokenUseCase.kt`, `DeleteMcpTokenUseCase.kt`, `GetMcpTokensUseCase.kt`, `RegenerateMcpTokenUseCase.kt`

### 2.4 Core Data Layer (`:core:data`)
- **Remote Data Source**:
  - `McpRemoteDataSource.kt` & `McpRemoteDataSourceImpl.kt`:
    ```kotlin
    suspend fun getConnectionDetails(): Result<McpConnectionDetailsDto>
    ```
- **Repository Implementation**:
  - `McpRepositoryImpl.kt`:
    - Calls `remoteDataSource.getConnectionDetails()`
    - Emits mapped `McpConnectionDetails` domain model wrapped in `Result`
- **Mappers & DI**:
  - Clean up `McpMappers.kt` (remove entity/token mappers, add `toDomain(): McpConnectionDetails`).
  - Update `McpDataModule.kt` bindings.

### 2.5 Presentation Layer (`:feature:profile`)
- **State & Action**:
  - `McpSettingsState.kt`:
    ```kotlin
    data class McpSettingsState(
        val connectionDetails: McpConnectionDetails? = null,
        val isLoading: Boolean = false,
        val error: UiText? = null,
    )
    ```
  - `McpSettingsAction.kt`:
    ```kotlin
    sealed interface McpSettingsAction {
        data object Refresh : McpSettingsAction
        data object DismissError : McpSettingsAction
    }
    ```
- **ViewModel**:
  - `McpSettingsViewModel.kt`: Injects `GetMcpConnectionDetailsUseCase` only, manages UI state with `StateFlow<McpSettingsState>`.
- **UI Screen (`McpSettingsScreen.kt`)**:
  - Unified Single Screen:
    1. Header / Navigation: Back button + Title ("Connect AI Assistant").
    2. Description Card: Brief explanation of MCP server connection and OAuth2 authentication.
    3. Connection Parameters Card:
       - **MCP Server URL** (`mcpUrl`) with monospace styling and copy button with toast.
       - **Client ID** (`clientId`) with monospace styling and copy button with toast.
    4. Client Guides & Snippets:
       - **Claude Desktop** config snippet with dynamic `mcpUrl` and `clientId`.
       - **Cursor / Custom Client Guide**.
       - **AI Setup Prompt** one-tap copy button.
    5. Loading and error states with retry support.
- **Components to Remove**:
  - `CreatedTokenModal.kt`
  - Token rows, add/delete/regenerate token dialogs and sheets.
- **Localization**:
  - Clean up and update `strings.xml` in English (`values/`) and Arabic (`values-ar/`).

---

## 3. Testing & Verification Plan

### Automated Tests
- `core:data`: Update `McpRemoteDataSourceImplTest` and `McpRepositoryImplTest` to test `getConnectionDetails` success and error paths.
- `core:domain`: Update `McpUseCasesTest` for `GetMcpConnectionDetailsUseCase`.
- `feature:profile`: Update `McpSettingsViewModelTest` to test connection details loading and error handling.
- Full verification: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
