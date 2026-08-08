package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * Error state shown when the goals network request fails.
 * Includes a cloud emoji, error message, and a Retry button.
 */
@Composable
internal fun GoalsErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(text = stringResource(R.string.goals_error_emoji), style = AwanTheme.typography.display.copy(fontSize = 52.sp))
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.goals_error_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 16.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        AwanButton(
            onClick = onRetry,
            variant = AwanButtonVariant.Primary,
        ) {
            AwanText(
                text = stringResource(R.string.goals_error_retry),
                style = AwanTheme.typography.button,
            )
        }
    }
}
