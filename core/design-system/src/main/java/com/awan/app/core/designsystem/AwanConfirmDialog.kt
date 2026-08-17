package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A bottom sheet confirmation dialog following the Skyward design language:
 * A rounded bottom sheet with a friendly icon badge, clear heading, subtitle,
 * and full-width primary/secondary action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwanConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmVariant: AwanButtonVariant = AwanButtonVariant.Primary,
    dismissLabel: String? = null,
    onDismissClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AwanTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.line.copy(alpha = 0.6f)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Icon Badge
            if (icon != null) {
                icon()
            } else {
                val isDestructive = confirmVariant == AwanButtonVariant.Destructive
                val badgeColor = if (isDestructive) {
                    AwanTheme.colors.destructive.copy(alpha = 0.12f)
                } else {
                    AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f)
                }
                val iconTint = if (isDestructive) {
                    AwanTheme.colors.destructive
                } else {
                    AwanTheme.colors.zoneTangerine
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // Title
            AwanText(
                text = title,
                style = AwanTheme.typography.heading.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )

            // Subtitle / Body
            if (body.isNotBlank()) {
                AwanText(
                    text = body,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = AwanTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Primary action
                AwanButton(
                    onClick = onConfirm,
                    variant = confirmVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AwanText(
                        text = confirmLabel,
                        style = AwanTheme.typography.button.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                // Secondary action
                if (dismissLabel != null) {
                    AwanButton(
                        onClick = onDismissClick ?: onDismiss,
                        variant = AwanButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AwanText(
                            text = dismissLabel,
                            style = AwanTheme.typography.button.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
