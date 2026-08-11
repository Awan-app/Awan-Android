# MCP Bottom Sheets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert token creation input (Add Token) and token reveal/copy (Created Token Modal) dialogs into Material 3 `ModalBottomSheet` containers, while keeping delete warning and regenerate confirmation dialogs as `AwanDialog` alert popups.

**Architecture:** Presentation UI refactoring in `:feature:profile:impl`.

---

## Proposed Changes

### Feature Profile UI (`:feature:profile:impl`)

#### [MODIFY] `McpSettingsScreen.kt`

- Replace `Dialog` for `uiState.showAddTokenDialog` with `ModalBottomSheet`:
  - Use `@OptIn(ExperimentalMaterial3Api::class)`.
  - Pass `onDismissRequest = { onAction(McpSettingsAction.HideAddTokenDialog) }`.
  - Style sheet container with `AwanTheme.colors.surface`, horizontal padding `AwanTheme.spacing.lg`, and bottom padding `AwanTheme.spacing.xl`.
- Preserve `AwanDialog` for `uiState.deletingToken` (delete confirmation).
- Preserve `AwanDialog` for `uiState.regeneratingToken` (regenerate confirmation).

#### [MODIFY] `CreatedTokenModal.kt`

- Replace `Dialog` with `ModalBottomSheet`:
  - Use `@OptIn(ExperimentalMaterial3Api::class)`.
  - Pass `onDismissRequest = onDismiss` and `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
  - Render token copy details, warning banner, and copy button inside bottom sheet layout.

---

## Verification Plan

### Automated Tests
- Feature profile unit tests: `./gradlew :feature:profile:impl:testDebugUnitTest`
- Full project build & lint: `./gradlew assembleDebug testDebugUnitTest lint`

### Manual Verification
1. Open app, navigate to **Profile -> Settings -> MCP Integration**.
2. Click **Add Token**: verify bottom sheet slides up with text field to enter token name.
3. Submit token name: verify bottom sheet dismisses and **Created Token Bottom Sheet** slides up displaying the raw token and Copy button.
4. Regenerate a token: click regenerate in row -> verify confirmation popup appears as **AwanDialog** -> click confirm -> verify **Created Token Bottom Sheet** slides up displaying new raw token.
5. Delete a token: click delete in row -> verify delete confirmation popup remains an **AwanDialog** alert.
