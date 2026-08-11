package com.awan.feature.profile.impl.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun CreatedTokenModal(
    createdToken: CreatedMcpToken,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedToastMessage = stringResource(ProfileR.string.profile_mcp_token_copied)
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        AwanCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(AwanTheme.spacing.xs),
            contentPadding = PaddingValues(AwanTheme.spacing.md)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
            ) {
                AwanText(
                    text = stringResource(ProfileR.string.profile_mcp_token_created_banner_title),
                    style = AwanTheme.styles.titleText
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AwanTheme.spacing.sm))
                        .background(AwanTheme.colors.destructive.copy(alpha = 0.1f))
                        .border(1.dp, AwanTheme.colors.destructive, RoundedCornerShape(AwanTheme.spacing.sm))
                        .padding(AwanTheme.spacing.md)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AwanTheme.colors.destructive,
                            modifier = Modifier.size(AwanTheme.spacing.lg)
                        )
                        AwanText(
                            text = stringResource(ProfileR.string.profile_mcp_token_created_banner_warning),
                            style = AwanTheme.styles.bodySecondaryText.let { it.copy(color = AwanTheme.colors.destructive) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AwanTheme.spacing.sm))
                        .background(AwanTheme.colors.disabledSurface)
                        .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(AwanTheme.spacing.sm))
                        .padding(AwanTheme.spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    AwanText(
                        text = createdToken.rawToken,
                        style = AwanTheme.styles.bodyText.let { it.copy(textStyle = it.textStyle.copy(fontFamily = FontFamily.Monospace)) },
                    )
                }

                AwanButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(createdToken.rawToken))
                        copied = true
                        Toast.makeText(context, copiedToastMessage, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = AwanButtonVariant.Secondary
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(AwanTheme.spacing.md)
                        )
                        AwanText(
                            text = if (copied) {
                                stringResource(ProfileR.string.profile_mcp_token_copied)
                            } else {
                                stringResource(ProfileR.string.profile_mcp_token_copy)
                            }
                        )
                    }
                }

                AwanButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AwanButtonVariant.Primary
                ) {
                    AwanText(stringResource(ProfileR.string.profile_close))
                }
            }
        }
    }
}
