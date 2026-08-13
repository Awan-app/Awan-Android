package com.awan.feature.home.impl.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R
import com.awan.feature.home.impl.ui.SessionDetailDialogState
import kotlinx.coroutines.launch

private enum class SheetScreen { DETAIL, DISMISS_WARNING, DELETE, LOADING, ERROR }

private fun SessionDetailDialogState.currentScreen(showDismissWarningScreen: Boolean): SheetScreen = when {
    isLoading -> SheetScreen.LOADING
    errorMessage != null -> SheetScreen.ERROR
    showDismissWarningScreen -> SheetScreen.DISMISS_WARNING
    showDeleteConfirmDialog -> SheetScreen.DELETE
    else -> SheetScreen.DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTaskDetailDialog(
    state: SessionDetailDialogState,
    onDismiss: () -> Unit,
    onSaveChanges: () -> Unit = {},
    onRetry: () -> Unit,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onStartMinutesChange: (Int) -> Unit = {},
    onEndMinutesChange: (Int) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var isExplicitDismissing by remember { mutableStateOf(false) }
    var showDismissWarningScreen by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) {
                if (isExplicitDismissing) {
                    true
                } else {
                    showDismissWarningScreen = true
                    false
                }
            } else {
                true
            }
        },
    )

    val handleDismissAttempt = {
        showDismissWarningScreen = true
    }

    val executeDismiss: () -> Unit = {
        isExplicitDismissing = true
        coroutineScope.launch {
            try {
                sheetState.hide()
            } catch (_: Exception) {
            } finally {
                onDismiss()
            }
        }
    }

    BackHandler(enabled = true) {
        if (showDismissWarningScreen) {
            showDismissWarningScreen = false
        } else {
            handleDismissAttempt()
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleDismissAttempt,
        sheetState = sheetState,
        containerColor = AwanTheme.colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.50f),
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
        ),
        modifier = modifier,
    ) {
        val currentScreen = state.currentScreen(showDismissWarningScreen)

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = slideInHorizontally(
                    initialOffsetX = { width -> if (forward) width else -width },
                ) + fadeIn()
                val exit = slideOutHorizontally(
                    targetOffsetX = { width -> if (forward) -width else width },
                ) + fadeOut()
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            label = "SheetScreenTransition",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) { screen ->
            when (screen) {
                SheetScreen.LOADING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                color = AwanTheme.colors.sky,
                                strokeWidth = 3.dp,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AwanText(
                                text = stringResource(R.string.home_session_detail_loading),
                                style = AwanTheme.typography.body.copy(
                                    fontSize = 14.sp,
                                    color = AwanTheme.colors.textSecondary,
                                ),
                            )
                        }
                    }
                }

                SheetScreen.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AwanText(
                            text = state.errorMessage?.asString() ?: "",
                            style = AwanTheme.typography.body.copy(
                                fontSize = 14.sp,
                                color = AwanTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AwanButton(
                            onClick = onRetry,
                            variant = AwanButtonVariant.Primary,
                        ) {
                            AwanText(
                                text = stringResource(R.string.retry),
                                style = AwanTheme.typography.button,
                            )
                        }
                    }
                }

                SheetScreen.DISMISS_WARNING -> {
                    DismissWarningContent(
                        onKeepEditing = { showDismissWarningScreen = false },
                        onDiscardAndClose = executeDismiss,
                    )
                }

                SheetScreen.DELETE -> {
                    DeleteSessionTaskContent(
                        isDeleting = state.isDeleting,
                        onConfirmDelete = onConfirmDelete,
                        onCancel = onCancelDelete,
                    )
                }

                SheetScreen.DETAIL -> {
                    if (state.detail != null) {
                        UnifiedSessionTaskContent(
                            detail = state.detail,
                            editStartMinutes = state.editStartMinutes,
                            editEndMinutes = state.editEndMinutes,
                            editDurationMinutes = state.editDurationMinutes,
                            onToggleStatus = onToggleStatus,
                            onToggleLock = onToggleLock,
                            onStartMinutesChange = onStartMinutesChange,
                            onEndMinutesChange = onEndMinutesChange,
                            onDurationChange = onDurationChange,
                            onDeleteClick = onDeleteClick,
                            onConfirmClose = {
                                onSaveChanges()
                                executeDismiss()
                            },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DismissWarningContent(
    onKeepEditing: () -> Unit,
    onDiscardAndClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Friendly Alert Icon Badge
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AwanTheme.colors.zoneTangerine.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = AwanTheme.colors.zoneTangerine,
                modifier = Modifier.size(26.dp),
            )
        }

        // Title
        AwanText(
            text = stringResource(R.string.home_dismiss_warning_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AwanTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )

        // Friendly Subtitle
        AwanText(
            text = stringResource(R.string.home_dismiss_warning_subtitle),
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = AwanTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Primary Keep Editing Button
            AwanButton(
                onClick = onKeepEditing,
                variant = AwanButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(
                    text = stringResource(R.string.home_dismiss_warning_keep_editing),
                    style = AwanTheme.typography.button.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Secondary Discard & Close Button
            AwanButton(
                onClick = onDiscardAndClose,
                variant = AwanButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(
                    text = stringResource(R.string.home_dismiss_warning_close_anyway),
                    style = AwanTheme.typography.button.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.destructive,
                    ),
                )
            }
        }
    }
}
