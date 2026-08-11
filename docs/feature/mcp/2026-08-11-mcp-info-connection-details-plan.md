# MCP Info Screen Connection Details & Copy Icon Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Server Connection Details card at the top of `McpInfoScreen` displaying the MCP Server URL and OAuth Client ID with copy buttons, and ensure copy icon tinting on Claude Desktop section matches `AwanTheme.colors.sky` (primary blue).

**Architecture:** Presentation UI update in `:feature:profile:impl`.

---

## Proposed Changes

### Feature Profile UI (`:feature:profile:impl`)

#### [MODIFY] `McpInfoScreen.kt`

1. Add Server Connection Details card at top of content column:
   - **MCP Server URL**: `https://backend-production-c701.up.railway.app/api/v1/mcp` with copy button and `SelectionContainer`.
   - **OAuth Client ID**: `awan-android-client` with copy button and `SelectionContainer`.
2. Update copy icon tinting across Claude Desktop section, Cursor section, and Connection Details to use `AwanTheme.colors.sky` (primary blue) explicitly.

```kotlin
// Server Connection Details Card at top of Column
AwanCard(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(AwanTheme.spacing.md)
) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        AwanText(
            text = stringResource(ProfileR.string.profile_mcp_connection_title),
            style = AwanTheme.styles.headingText
        )

        val mcpUrl = "https://backend-production-c701.up.railway.app/api/v1/mcp"
        val clientId = "awan-android-client"

        // MCP Server URL
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
            AwanText(
                text = stringResource(ProfileR.string.profile_mcp_url_label),
                style = AwanTheme.styles.captionText
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AwanTheme.spacing.xs))
                    .background(AwanTheme.colors.disabledSurface)
                    .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(AwanTheme.spacing.xs))
                    .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    AwanText(text = mcpUrl, style = AwanTheme.styles.bodyText, modifier = Modifier.weight(1f))
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(mcpUrl))
                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_url),
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(AwanTheme.spacing.md)
                    )
                }
            }
        }

        // Client ID
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
            AwanText(
                text = stringResource(ProfileR.string.profile_mcp_client_id_label),
                style = AwanTheme.styles.captionText
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AwanTheme.spacing.xs))
                    .background(AwanTheme.colors.disabledSurface)
                    .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(AwanTheme.spacing.xs))
                    .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    AwanText(text = clientId, style = AwanTheme.styles.bodyText, modifier = Modifier.weight(1f))
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(clientId))
                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_client_id),
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(AwanTheme.spacing.md)
                    )
                }
            }
        }
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
2. Verify top section displays Connection Details (Server URL and Client ID) with copy buttons.
3. Click copy buttons for Server URL and Client ID: verify toast appears and text is copied.
4. Verify copy icon tint on Claude Desktop section is styled with primary sky blue.
