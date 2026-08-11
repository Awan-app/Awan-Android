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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val copiedToastMessage = stringResource(ProfileR.string.profile_mcp_token_copied)
    var showAddTokenDialog by remember { mutableStateOf(false) }
    var newTokenName by remember { mutableStateOf("") }
    var deletingToken by remember { mutableStateOf<McpToken?>(null) }
    var regeneratingToken by remember { mutableStateOf<McpToken?>(null) }

    if (uiState.createdToken != null) {
        CreatedTokenModal(
            createdToken = uiState.createdToken,
            onDismiss = { onAction(McpSettingsAction.DismissCreatedModal) }
        )
    }

    if (showAddTokenDialog) {
        Dialog(onDismissRequest = { showAddTokenDialog = false }) {
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
                        value = newTokenName,
                        onValueChange = { newTokenName = it },
                        placeholder = stringResource(ProfileR.string.profile_mcp_token_name_hint),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
                    ) {
                        AwanButton(
                            onClick = {
                                showAddTokenDialog = false
                                newTokenName = ""
                            },
                            modifier = Modifier.weight(1f),
                            variant = AwanButtonVariant.Quiet
                        ) {
                            AwanText(stringResource(ProfileR.string.profile_cancel))
                        }
                        AwanButton(
                            onClick = {
                                if (newTokenName.isNotBlank()) {
                                    onAction(McpSettingsAction.CreateToken(newTokenName))
                                    showAddTokenDialog = false
                                    newTokenName = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newTokenName.isNotBlank() && !uiState.isCreating
                        ) {
                            if (uiState.isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AwanTheme.colors.surface)
                            } else {
                                AwanText(stringResource(ProfileR.string.profile_mcp_add_token))
                            }
                        }
                    }
                }
            }
        }
    }

    if (deletingToken != null) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_mcp_token_delete_confirm_title),
            body = stringResource(ProfileR.string.profile_mcp_token_delete_confirm_body),
            primaryLabel = stringResource(ProfileR.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(McpSettingsAction.DeleteToken(deletingToken!!.id))
                deletingToken = null
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { deletingToken = null },
            onDismiss = { deletingToken = null }
        )
    }

    if (regeneratingToken != null) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_mcp_token_regenerate_confirm_title),
            body = stringResource(ProfileR.string.profile_mcp_token_regenerate_confirm_body),
            primaryLabel = stringResource(ProfileR.string.profile_zone_confirm),
            primaryVariant = AwanButtonVariant.Primary,
            onPrimary = {
                onAction(McpSettingsAction.RegenerateToken(regeneratingToken!!.id))
                regeneratingToken = null
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { regeneratingToken = null },
            onDismiss = { regeneratingToken = null }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        contentDescription = "MCP Setup Info",
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Details Card
                AwanCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(AwanTheme.spacing.md)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_mcp_connection_title),
                            style = AwanTheme.styles.headingText
                        )

                        val details = uiState.connectionDetails
                        val mcpUrl = details?.mcpUrl ?: "https://mcp.awan.app/v1"
                        val clientId = details?.clientId ?: "awan-android-client"

                        // MCP URL Row
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AwanText(
                                text = stringResource(ProfileR.string.profile_mcp_url_label),
                                style = AwanTheme.styles.captionText
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AwanTheme.colors.disabledSurface)
                                    .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MCP URL", mcpUrl))
                                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = AwanTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Client ID Row
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AwanText(
                                text = stringResource(ProfileR.string.profile_mcp_client_id_label),
                                style = AwanTheme.styles.captionText
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AwanTheme.colors.disabledSurface)
                                    .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Client ID", clientId))
                                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Client ID",
                                        tint = AwanTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                onClick = { showAddTokenDialog = true },
                                variant = AwanButtonVariant.Quiet
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AwanText(stringResource(ProfileR.string.profile_mcp_add_token))
                                }
                            }
                        }

                        if (uiState.tokens.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
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
                                    onRegenerate = { regeneratingToken = token },
                                    onDelete = { deletingToken = token }
                                )
                            }
                        }

                        // Security notice
                        AwanText(
                            text = stringResource(ProfileR.string.profile_mcp_token_obscured_notice) + ". " +
                                    stringResource(ProfileR.string.profile_mcp_token_copy_disabled),
                            style = AwanTheme.styles.captionText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(AwanTheme.colors.disabledSurface)
            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = token.name,
                    style = AwanTheme.styles.bodyText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Token",
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
                            contentDescription = "Delete Token",
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
