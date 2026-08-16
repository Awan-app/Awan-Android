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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
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
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanErrorSnackbar
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.presentation.McpSettingsAction
import com.awan.feature.profile.impl.presentation.McpSettingsState

@Composable
fun McpSettingsScreen(
    uiState: McpSettingsState,
    onAction: (McpSettingsAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val urlCopiedMessage = stringResource(ProfileR.string.profile_mcp_url_copied)
    val clientIdCopiedMessage = stringResource(ProfileR.string.profile_mcp_client_id_copied)
    val claudeConfigCopiedMessage = stringResource(ProfileR.string.profile_mcp_claude_config_copied)
    val aiPromptCopiedMessage = stringResource(ProfileR.string.profile_mcp_ai_prompt_copied)

    val mcpUrl = uiState.connectionDetails?.mcpUrl ?: "https://awanproduction.up.railway.app/mcp"
    val clientId = uiState.connectionDetails?.clientId ?: "awan-mcp"

    val claudeSnippet = """
        {
          "mcpServers": {
            "awan": {
              "url": "$mcpUrl",
              "clientId": "$clientId"
            }
          }
        }
    """.trimIndent()

    val aiSetupPrompt = stringResource(
        ProfileR.string.profile_mcp_ai_setup_prompt,
        mcpUrl,
        clientId
    )

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
                    text = stringResource(ProfileR.string.profile_mcp_title),
                    style = AwanTheme.styles.titleText
                )
            }
        },
        containerColor = AwanTheme.colors.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.connectionDetails == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AwanTheme.colors.sky)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AwanTheme.spacing.lg, vertical = AwanTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
                ) {
                    // Overview banner card
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(AwanTheme.spacing.md)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(AwanTheme.spacing.sm))
                                    .background(AwanTheme.colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = AwanTheme.colors.sky,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            AwanText(
                                text = stringResource(ProfileR.string.profile_mcp_overview_desc),
                                style = AwanTheme.styles.bodyText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Connection Details Card
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(AwanTheme.spacing.md)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
                        ) {
                            AwanText(
                                text = stringResource(ProfileR.string.profile_mcp_connection_title),
                                style = AwanTheme.styles.headingText
                            )

                            // MCP Server URL field
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
                                    SelectionContainer(modifier = Modifier.weight(1f)) {
                                        AwanText(
                                            text = mcpUrl,
                                            style = AwanTheme.styles.bodyText.let {
                                                it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace))
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(mcpUrl))
                                            Toast.makeText(context, urlCopiedMessage, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_url),
                                            tint = AwanTheme.colors.textSecondary,
                                            modifier = Modifier.size(AwanTheme.spacing.md)
                                        )
                                    }
                                }
                            }

                            // OAuth Client ID field
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
                                    SelectionContainer(modifier = Modifier.weight(1f)) {
                                        AwanText(
                                            text = clientId,
                                            style = AwanTheme.styles.bodyText.let {
                                                it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace))
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(clientId))
                                            Toast.makeText(context, clientIdCopiedMessage, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(ProfileR.string.profile_mcp_cd_copy_client_id),
                                            tint = AwanTheme.colors.textSecondary,
                                            modifier = Modifier.size(AwanTheme.spacing.md)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // AI Configuration Guide Card
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(AwanTheme.spacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AwanText(
                                    text = stringResource(ProfileR.string.profile_mcp_guide_title),
                                    style = AwanTheme.styles.headingText
                                )
                                AwanButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(aiSetupPrompt))
                                        Toast.makeText(context, aiPromptCopiedMessage, Toast.LENGTH_SHORT).show()
                                    },
                                    variant = AwanButtonVariant.Quiet
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AwanTheme.colors.sky,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        AwanText(stringResource(ProfileR.string.profile_mcp_copy_ai_prompt))
                                    }
                                }
                            }

                            // Claude Desktop Snippet Container
                            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AwanText(
                                        text = stringResource(ProfileR.string.profile_mcp_claude_title),
                                        style = AwanTheme.styles.bodySecondaryText
                                    )
                                    AwanButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(claudeSnippet))
                                            Toast.makeText(context, claudeConfigCopiedMessage, Toast.LENGTH_SHORT).show()
                                        },
                                        variant = AwanButtonVariant.Quiet
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = AwanTheme.colors.sky,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            AwanText(stringResource(ProfileR.string.profile_mcp_copy_config))
                                        }
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
                                            style = AwanTheme.styles.bodyText.let {
                                                it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AwanTheme.spacing.lg),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AwanErrorSnackbar(
                        message = uiState.error.asString(),
                        onDismiss = { onAction(McpSettingsAction.DismissError) }
                    )
                }
            }
        }
    }
}
