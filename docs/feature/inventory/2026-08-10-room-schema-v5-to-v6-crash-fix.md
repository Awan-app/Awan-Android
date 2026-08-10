# Room schema v5 to v6 crash fix

## Problem

The current marketplace entities replaced the schema-5 inventory entities without changing the Room database version. Existing installations therefore fail at startup with a Room identity-hash mismatch.

## Fix

- Bump `AwanDatabase` from version 5 to 6.
- Add and register an idempotent `MIGRATION_5_6` that creates the current marketplace tables.
- Keep the old inventory table untouched because its data model cannot be safely mapped to the marketplace model.
- Ignore blank remote-image URLs so empty backend/test values do not start an OkHttp request.

## Verification

Run the database unit tests, design-system instrumentation test, and app debug assembly.

## Implementation notes (what actually differed)

- Bumped Room to version 6, registered `MIGRATION_5_6`, regenerated the version-6 schema, and guarded blank remote-image URLs.
- `:core:database:testDebugUnitTest` passed all 4 tests, and `:core:design-system:compileDebugAndroidTestKotlin` plus `:app:assembleDebug` passed.
- Connected instrumentation built but could not complete because the attached device disappeared during the first run and the second run timed out; no connected-test result is claimed.
