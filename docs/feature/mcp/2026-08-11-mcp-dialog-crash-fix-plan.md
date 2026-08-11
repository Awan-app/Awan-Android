# MCP Dialog Crash Fix & Padding Optimization Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve the `java.lang.IllegalArgumentException: maxWidth must be >= than minWidth` crash occurring when opening `Dialog` containers (e.g. Add Token dialog) and reduce dialog content paddings to provide ample space for contents.

**Root Cause:**
When a `Dialog` layout passes tight horizontal constraints to `AwanCard` and `AwanButton`, `StyleOuterNode` subtracts internal padding (e.g. 16dp/32dp) from incoming `maxWidth`. When `maxWidth` drops below padding width, subtracting padding yields a negative `maxWidth` while `minWidth` is clamped to `0`, causing `Constraints(minWidth = 0, maxWidth = -18)` to throw `IllegalArgumentException`.

---

## Proposed Changes

### Core Design System (`:core:design-system`)

#### [MODIFY] `AwanButton.kt`
Guarantee that `safeConstraints` passed to `measurables[1].measure(safeConstraints)` never has `maxWidth < minWidth` or invalid bounds:

```kotlin
val minTouchPx = minTouchSize.roundToPx()
val safeMaxWidth = constraints.maxWidth.coerceAtLeast(0)
val safeMaxHeight = constraints.maxHeight.coerceAtLeast(0)

val minW = if (safeMaxWidth > 0) maxOf(constraints.minWidth, minTouchPx).coerceAtMost(safeMaxWidth) else 0
val minH = if (safeMaxHeight > 0) maxOf(constraints.minHeight, minTouchPx).coerceAtMost(safeMaxHeight) else 0

val safeConstraints = Constraints(
    minWidth = minW,
    maxWidth = maxOf(safeMaxWidth, minW),
    minHeight = minH,
    maxHeight = maxOf(safeMaxHeight, minH),
)
```

#### [MODIFY] `AwanCard.kt`
Apply matching constraint safety to `AwanCard`:

```kotlin
val safeMaxWidth = constraints.maxWidth.coerceAtLeast(0)
val safeMaxHeight = constraints.maxHeight.coerceAtLeast(0)
val minW = constraints.minWidth.coerceIn(0, safeMaxWidth)
val minH = constraints.minHeight.coerceIn(0, safeMaxHeight)

val safeConstraints = Constraints(
    minWidth = minW,
    maxWidth = maxOf(safeMaxWidth, minW),
    minHeight = minH,
    maxHeight = maxOf(safeMaxHeight, minH)
)
```

---

### Feature Profile UI (`:feature:profile:impl`)

#### [MODIFY] `McpSettingsScreen.kt` & `CreatedTokenModal.kt`
Reduce dialog card outer padding and inner `contentPadding` for optimal layout space:

- Change `showAddTokenDialog` `AwanCard`:
  - Outer modifier padding: `AwanTheme.spacing.sm` (12dp) instead of `spacing.md` (16dp).
  - Inner `contentPadding`: `PaddingValues(AwanTheme.spacing.md)` (16dp) instead of `spacing.xl` (32dp).
- Change `CreatedTokenModal` `AwanCard`:
  - Outer modifier padding: `AwanTheme.spacing.sm` (12dp).
  - Inner `contentPadding`: `PaddingValues(AwanTheme.spacing.md)` (16dp).

---

## Verification Plan

### Automated Tests
- Design system tests: `./gradlew :core:design-system:testDebugUnitTest`
- Feature profile tests: `./gradlew :feature:profile:impl:testDebugUnitTest`
- Full project build & lint: `./gradlew assembleDebug testDebugUnitTest lint`

### Manual Verification
1. Open app, navigate to **Profile -> Settings -> MCP Integration**.
2. Click **Add Token**: verify dialog opens cleanly without crashing and input field + action buttons fit comfortably.
3. Create a token: verify `CreatedTokenModal` displays raw token and action buttons cleanly without layout clipping or crashes.
