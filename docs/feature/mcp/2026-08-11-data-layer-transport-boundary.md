# MCP data-layer transport boundary

## Summary

Keep Retrofit internal to `:core:network`. MCP's repository will coordinate an
internal remote data source and Room; it will not depend on `McpApiService` or
Retrofit response types.

## Changes

- Restore Retrofit to an implementation dependency in `:core:network` and remove it from `:core:data`.
- Make `McpApiService` return bodies and `Unit`, so Retrofit handles non-2xx responses internally.
- Add an internal `McpRemoteDataSource` that maps API calls through `safeApiCall` and the shared JSON error mapper.
- Bind it in `McpDataModule` and inject it into `McpRepositoryImpl`.
- Preserve the public MCP repository/use-case API, Room cache behavior, offline guards, and one-time raw-token handling.

## Verification

- Update repository tests to use a remote-source fake.
- Add focused remote-source success and error-mapping tests.
- Run the MCP data tests, `:core:data:compileDebugKotlin`, `:app:assembleDebug`, and `git diff --check`.

## Implementation notes (what actually differed)

- Retrofit is now internal to `:core:network`; the MCP repository uses an internal remote data source that owns the Retrofit service and shared `safeApiCall` mapping.
- Verified with the full `:core:data:testDebugUnitTest` suite. `:app:assembleDebug` is blocked only because this new worktree excludes the existing local `app/google-services.json`.
