package com.awan.feature.home.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R
import com.awan.feature.home.impl.ui.SessionDetailDialogState

private enum class SheetScreen { DETAIL, EDIT, DELETE, LOADING, ERROR }

private fun SessionDetailDialogState.currentScreen(): SheetScreen = when {
    isLoading -> SheetScreen.LOADING
    errorMessage != null -> SheetScreen.ERROR
    showDeleteConfirmDialog -> SheetScreen.DELETE
    isEditing -> SheetScreen.EDIT
    else -> SheetScreen.DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTaskDetailDialog(
    state: SessionDetailDialogState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onStartEditing: () -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onSaveEdits: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSelectDeleteTarget: (com.awan.feature.home.impl.ui.DeleteTargetType) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            shouldDismissOnBackPress = true,
        ),
        modifier = modifier,
    ) {
        val currentScreen = state.currentScreen()

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
                            AwanText(text = stringResource(R.string.retry))
                        }
                    }
                }

                SheetScreen.DELETE -> {
                    DeleteSessionTaskContent(
                        selectedTarget = state.deleteTargetType,
                        isDeleting = state.isDeleting,
                        onSelectTarget = onSelectDeleteTarget,
                        onConfirmDelete = onConfirmDelete,
                        onCancel = onCancelDelete,
                    )
                }

                SheetScreen.EDIT -> {
                    EditSessionTaskContent(
                        editTitle = state.editTitle,
                        editDescription = state.editDescription,
                        editDurationMinutes = state.editDurationMinutes,
                        isSaving = state.isSaving,
                        onTitleChange = onTitleChange,
                        onDescriptionChange = onDescriptionChange,
                        onDurationChange = onDurationChange,
                        onSave = onSaveEdits,
                        onCancel = onCancelEditing,
                    )
                }

                SheetScreen.DETAIL -> {
                    if (state.detail != null) {
                        UnifiedSessionTaskContent(
                            detail = state.detail,
                            onToggleStatus = onToggleStatus,
                            onToggleLock = onToggleLock,
                            onEditClick = onStartEditing,
                            onDeleteClick = onDeleteClick,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}
