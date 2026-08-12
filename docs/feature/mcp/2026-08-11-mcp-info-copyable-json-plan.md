# MCP Info Screen Copyable JSON Snippets Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable text selection (`SelectionContainer`) for JSON configuration snippets in `McpInfoScreen` so users can highlight and copy any arbitrary sub-string, and verify copy icon functionality on the Claude Desktop configuration section.

**Architecture:** Presentation UI update in `:feature:profile:impl`.

---

## Proposed Changes

### Feature Profile UI (`:feature:profile:impl`)

#### [MODIFY] `McpInfoScreen.kt`

1. Import `androidx.compose.foundation.text.selection.SelectionContainer`.
2. Wrap `AwanText` displaying `claudeSnippet` inside `SelectionContainer`.
3. Wrap `AwanText` displaying `cursorSnippet` inside `SelectionContainer`.
4. Ensure the copy icon (`IconButton` with `Icons.Default.ContentCopy`) on the Claude section header uses `LocalClipboardManager.current` to copy the full `claudeSnippet` with a Toast feedback notification.

```kotlin
// Claude Desktop Guide Box
Box(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(AwanTheme.spacing.xs))
        .background(AwanTheme.colors.disabledSurface)
        .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(AwanTheme.spacing.xs))
        .padding(AwanTheme.spacing.sm)
) {
    SelectionContainer {
        AwanText(
            text = claudeSnippet,
            style = AwanTheme.styles.bodyText.let {
                it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace))
            }
        )
    }
}
```

---

## Verification Plan

### Automated Tests
- Feature profile unit tests: `./gradlew :feature:profile:impl:testDebugUnitTest`
- Full project build & lint: `./gradlew assembleDebug testDebugUnitTest lint`

### Manual Verification
1. Open app, navigate to **Profile -> Settings -> MCP Integration -> Info (i)**.
2. Long-press on any line of the `claude_desktop_config.json` text block: verify text handles appear allowing arbitrary text selection and copying.
3. Click the copy icon on the Claude Desktop section header: verify full JSON snippet is copied to clipboard with Toast feedback.
