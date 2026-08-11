package com.awan.feature.profile.impl.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun McpInfoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedToastMessage = stringResource(ProfileR.string.profile_mcp_token_copied)

    val claudeSnippet = """
        {
          "mcpServers": {
            "awan": {
              "command": "npx",
              "args": [
                "-y",
                "@awan/mcp-server",
                "--url", "https://backend-production-c701.up.railway.app/api/v1/mcp",
                "--token", "YOUR_API_TOKEN"
              ]
            }
          }
        }
    """.trimIndent()

    val cursorSnippet = """
        {
          "mcp": {
            "servers": {
              "awan": {
                "url": "https://backend-production-c701.up.railway.app/api/v1/mcp",
                "headers": {
                  "Authorization": "Bearer YOUR_API_TOKEN"
                }
              }
            }
          }
        }
    """.trimIndent()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanBackButton(onClick = onBackClick)
                AwanText(
                    text = stringResource(ProfileR.string.profile_mcp_info_title),
                    style = AwanTheme.styles.titleText
                )
            }
        },
        containerColor = AwanTheme.colors.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AwanTheme.spacing.lg, vertical = AwanTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
        ) {
            // Connection Details Card
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

                    // MCP Server URL Row
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
                                AwanText(
                                    text = mcpUrl,
                                    style = AwanTheme.styles.bodyText,
                                    modifier = Modifier.weight(1f)
                                )
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

                    // OAuth Client ID Row
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
                                AwanText(
                                    text = clientId,
                                    style = AwanTheme.styles.bodyText,
                                    modifier = Modifier.weight(1f)
                                )
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

            // Setup steps card
            AwanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AwanTheme.spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
                    AwanText(
                        text = "Setup Instructions",
                        style = AwanTheme.styles.headingText
                    )
                    AwanText(
                        text = stringResource(ProfileR.string.profile_mcp_info_step1),
                        style = AwanTheme.styles.bodyText
                    )
                    AwanText(
                        text = stringResource(ProfileR.string.profile_mcp_info_step2),
                        style = AwanTheme.styles.bodyText
                    )
                    AwanText(
                        text = stringResource(ProfileR.string.profile_mcp_info_step3),
                        style = AwanTheme.styles.bodyText
                    )
                }
            }

            // Claude Desktop Guide
            AwanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AwanTheme.spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AwanText(
                            text = "Claude Desktop (claude_desktop_config.json)",
                            style = AwanTheme.styles.headingText
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(claudeSnippet))
                                Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_snippet),
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(AwanTheme.spacing.md)
                            )
                        }
                    }
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
                                style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) }
                            )
                        }
                    }
                }
            }

            // Cursor Guide
            AwanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AwanTheme.spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AwanText(
                            text = "Cursor IDE Setup",
                            style = AwanTheme.styles.headingText
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(cursorSnippet))
                                Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_snippet),
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(AwanTheme.spacing.md)
                            )
                        }
                    }
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
                                text = cursorSnippet,
                                style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
