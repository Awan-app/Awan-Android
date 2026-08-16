package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.goals.impl.R

@Composable
internal fun InboxErrorState(onRetry: () -> Unit) {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(text = stringResource(R.string.goals_error_emoji), style = AwanTheme.typography.display.copy(fontSize = 48.sp))
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.inbox_error_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 16.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AwanButton(
            onClick = onRetry,
            variant = AwanButtonVariant.Primary,
        ) {
            AwanText(
                text = stringResource(R.string.inbox_error_retry),
                style = AwanTheme.typography.button,
            )
        }
    }
}
