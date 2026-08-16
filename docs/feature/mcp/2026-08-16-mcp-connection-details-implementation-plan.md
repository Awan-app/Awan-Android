# MCP Connection Details Refactor Implementation Plan

Refactor the MCP integration across all layers to match the new backend contract (`GET /api/v1/mcp/settings/connection-details`), unify the UI into a clean, modern "Connect AI Assistant" screen, and eradicate all legacy token/API-key management code.

## Proposed Changes

### 1. Core Network Layer (`:core:network`)
- Delete `ApiKeyResponseDto.kt`, `ApiKeySummaryDto.kt`, `CreateApiKeyRequestDto.kt`.
- Create `McpConnectionDetailsDto.kt`.
- Update `McpApiService.kt` with `@GET("v1/mcp/settings/connection-details") suspend fun getConnectionDetails(): McpConnectionDetailsDto`.

### 2. Core Database Layer (`:core:database`)
- Delete `McpTokenEntity.kt`, `McpTokenDao.kt`.
- Remove `McpTokenEntity` and `mcpTokenDao()` from `AwanDatabase.kt` and `DatabaseModule.kt`.

### 3. Core Domain Layer (`:core:domain`)
- Retain `McpConnectionDetails.kt`.
- Delete `McpToken.kt`, `CreatedMcpToken.kt`.
- Delete `CreateMcpTokenUseCase.kt`, `DeleteMcpTokenUseCase.kt`, `GetMcpTokensUseCase.kt`, `RegenerateMcpTokenUseCase.kt`.
- Retain & update `GetMcpConnectionDetailsUseCase.kt` and `McpRepository.kt`.
- Update `McpUseCasesTest.kt`.

### 4. Core Data Layer (`:core:data`)
- Update `McpRemoteDataSource.kt` and `McpRemoteDataSourceImpl.kt`.
- Update `McpMappers.kt` and `McpRepositoryImpl.kt`.
- Update unit tests: `McpRemoteDataSourceImplTest.kt` and `McpRepositoryImplTest.kt`.

### 5. Presentation Layer (`:feature:profile`)
- Delete `CreatedTokenModal.kt`, `McpSettingsDateFormatterTest.kt`.
- Update `McpSettingsState.kt`, `McpSettingsAction.kt`, `McpSettingsViewModel.kt`.
- Redesign `McpSettingsScreen.kt` according to the approved visual mock.
- Update `strings.xml` and `values-ar/strings.xml`.
- Update `McpSettingsViewModelTest.kt`.

## Verification Plan
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

## Implementation notes (what actually differed)
- **Build/Test/Verification**:
  - `./gradlew testDebugUnitTest`: Passed all 790 tasks across all modules (`:core:network`, `:core:database`, `:core:domain`, `:core:data`, `:feature:profile`, `:app`).
  - `./gradlew assembleDebug`: Passed successfully with debug APK assembled.
- **Runtime/Architecture Traps & Notes**:
  - `McpInfoScreen.kt` was completely removed in favor of the unified `McpSettingsScreen.kt` (Option 1). `McpInfoRouteScreen.kt` and `ProfileEntryProvider.kt` were updated to route cleanly to `McpSettingsRouteScreen`.
  - Obsolete `CreatedTokenModal.kt` and token tests were removed cleanly without residual references.
  - All token entities, DAOs, DTOs, and Use Cases were removed.
- **Deviations from initial plan**:
  - None; implemented exactly according to Option 1 single-screen layout and new contract specifications.
