package com.awan.feature.profile.impl.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val copiedToastMessage = stringResource(ProfileR.string.profile_mcp_token_copied)

    val claudeSnippet = """
        {
          "mcpServers": {
            "awan": {
              "command": "npx",
              "args": [
                "-y",
                "@awan/mcp-server",
                "--url", "https://mcp.awan.app/v1",
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
                "url": "https://mcp.awan.app/v1",
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Setup steps card
            AwanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AwanTheme.spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Claude Config", claudeSnippet))
                                Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Claude Snippet",
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AwanTheme.colors.disabledSurface)
                            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        AwanText(
                            text = claudeSnippet,
                            style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) }
                        )
                    }
                }
            }

            // Cursor Guide
            AwanCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AwanTheme.spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Cursor Config", cursorSnippet))
                                Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Cursor Snippet",
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AwanTheme.colors.disabledSurface)
                            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
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
