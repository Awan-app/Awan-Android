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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.window.Dialog
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanDialog
import com.awan.app.core.designsystem.AwanErrorSnackbar
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.presentation.McpSettingsAction
import com.awan.feature.profile.impl.presentation.McpSettingsState
import com.awan.feature.profile.impl.ui.components.CreatedTokenModal

@Composable
fun McpSettingsScreen(
    uiState: McpSettingsState,
    onAction: (McpSettingsAction) -> Unit,
    onInfoClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedToastMessage = stringResource(ProfileR.string.profile_mcp_token_copied)

    if (uiState.createdToken != null) {
        CreatedTokenModal(
            createdToken = uiState.createdToken,
            onDismiss = { onAction(McpSettingsAction.DismissCreatedModal) }
        )
    }

    if (uiState.showAddTokenDialog) {
        Dialog(onDismissRequest = { onAction(McpSettingsAction.HideAddTokenDialog) }) {
            AwanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AwanTheme.spacing.md),
                contentPadding = PaddingValues(AwanTheme.spacing.xl)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
                ) {
                    AwanText(
                        text = stringResource(ProfileR.string.profile_mcp_add_token),
                        style = AwanTheme.styles.titleText
                    )
                    AwanTextField(
                        value = uiState.newTokenName,
                        onValueChange = { onAction(McpSettingsAction.UpdateNewTokenName(it)) },
                        placeholder = stringResource(ProfileR.string.profile_mcp_token_name_hint),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
                    ) {
                        AwanButton(
                            onClick = { onAction(McpSettingsAction.HideAddTokenDialog) },
                            modifier = Modifier.weight(1f),
                            variant = AwanButtonVariant.Quiet
                        ) {
                            AwanText(stringResource(ProfileR.string.profile_cancel))
                        }
                        AwanButton(
                            onClick = {
                                if (uiState.newTokenName.isNotBlank()) {
                                    onAction(McpSettingsAction.CreateToken(uiState.newTokenName))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.newTokenName.isNotBlank() && !uiState.isCreating
                        ) {
                            if (uiState.isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(AwanTheme.spacing.md), color = AwanTheme.colors.surface)
                            } else {
                                AwanText(stringResource(ProfileR.string.profile_mcp_add_token))
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.deletingToken != null) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_mcp_token_delete_confirm_title),
            body = stringResource(ProfileR.string.profile_mcp_token_delete_confirm_body),
            primaryLabel = stringResource(ProfileR.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(McpSettingsAction.DeleteToken(uiState.deletingToken.id))
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { onAction(McpSettingsAction.HideDeleteDialog) },
            onDismiss = { onAction(McpSettingsAction.HideDeleteDialog) }
        )
    }

    if (uiState.regeneratingToken != null) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_mcp_token_regenerate_confirm_title),
            body = stringResource(ProfileR.string.profile_mcp_token_regenerate_confirm_body),
            primaryLabel = stringResource(ProfileR.string.profile_zone_confirm),
            primaryVariant = AwanButtonVariant.Primary,
            onPrimary = {
                onAction(McpSettingsAction.RegenerateToken(uiState.regeneratingToken.id))
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { onAction(McpSettingsAction.HideRegenerateDialog) },
            onDismiss = { onAction(McpSettingsAction.HideRegenerateDialog) }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanBackButton(onClick = onBackClick)
                AwanText(
                    text = stringResource(ProfileR.string.profile_mcp_title),
                    style = AwanTheme.styles.titleText
                )
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(ProfileR.string.profile_mcp_cd_info),
                        tint = AwanTheme.colors.sky
                    )
                }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AwanTheme.spacing.lg, vertical = AwanTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
            ) {
                // Connection Details Card
                AwanCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(AwanTheme.spacing.md)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_mcp_connection_title),
                            style = AwanTheme.styles.headingText
                        )

                        val details = uiState.connectionDetails
                        val mcpUrl = details?.mcpUrl ?: "https://mcp.awan.app/v1"
                        val clientId = details?.clientId ?: "awan-android-client"

                        // MCP URL Row
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
                                AwanText(
                                    text = mcpUrl,
                                    style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) },
                                    modifier = Modifier.weight(1f)
                                )
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
                                        tint = AwanTheme.colors.textSecondary,
                                        modifier = Modifier.size(AwanTheme.spacing.md)
                                    )
                                }
                            }
                        }

                        // Client ID Row
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
                                AwanText(
                                    text = clientId,
                                    style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) },
                                    modifier = Modifier.weight(1f)
                                )
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
                                        tint = AwanTheme.colors.textSecondary,
                                        modifier = Modifier.size(AwanTheme.spacing.md)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tokens Card
                AwanCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(AwanTheme.spacing.md)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AwanText(
                                text = stringResource(ProfileR.string.profile_mcp_tokens_title),
                                style = AwanTheme.styles.headingText
                            )
                            AwanButton(
                                onClick = { onAction(McpSettingsAction.ShowAddTokenDialog) },
                                variant = AwanButtonVariant.Quiet
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(AwanTheme.spacing.md)
                                    )
                                    AwanText(stringResource(ProfileR.string.profile_mcp_add_token))
                                }
                            }
                        }

                        if (uiState.tokens.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AwanTheme.spacing.md),
                                contentAlignment = Alignment.Center
                            ) {
                                AwanText(
                                    text = "No tokens added yet",
                                    style = AwanTheme.styles.bodySecondaryText
                                )
                            }
                        } else {
                            uiState.tokens.forEach { token ->
                                TokenItemRow(
                                    token = token,
                                    onRegenerate = { onAction(McpSettingsAction.ShowRegenerateDialog(token)) },
                                    onDelete = { onAction(McpSettingsAction.ShowDeleteDialog(token)) }
                                )
                            }
                        }

                        // Security notice
                        AwanText(
                            text = stringResource(
                                ProfileR.string.profile_mcp_token_notice_formatted,
                                stringResource(ProfileR.string.profile_mcp_token_obscured_notice),
                                stringResource(ProfileR.string.profile_mcp_token_copy_disabled)
                            ),
                            style = AwanTheme.styles.captionText,
                            modifier = Modifier.padding(top = AwanTheme.spacing.xxs)
                        )
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

@Composable
private fun TokenItemRow(
    token: McpToken,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AwanTheme.spacing.sm))
            .background(AwanTheme.colors.disabledSurface)
            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(AwanTheme.spacing.sm))
            .padding(AwanTheme.spacing.sm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = token.name,
                    style = AwanTheme.styles.bodyText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(ProfileR.string.profile_mcp_cd_regenerate_token, token.name),
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(ProfileR.string.profile_mcp_cd_delete_token, token.name),
                            tint = AwanTheme.colors.destructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = token.maskedToken,
                    style = AwanTheme.styles.captionText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) }
                )
                AwanText(
                    text = token.createdAt,
                    style = AwanTheme.styles.captionText
                )
            }
        }
    }
}
