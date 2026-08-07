package com.awan.feature.home.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTaskDetailDialog(
    state: SessionDetailDialogState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            when {
                state.isLoading -> {
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

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AwanText(
                            text = "☁️",
                            style = AwanTheme.typography.display.copy(fontSize = 40.sp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AwanText(
                            text = state.errorMessage.asString(),
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

                state.detail != null -> {
                    UnifiedSessionTaskContent(
                        detail = state.detail,
                        onDismiss = onDismiss,
                        onToggleStatus = onToggleStatus,
                        onToggleLock = onToggleLock,
                    )
                }
            }
        }
    }
}
