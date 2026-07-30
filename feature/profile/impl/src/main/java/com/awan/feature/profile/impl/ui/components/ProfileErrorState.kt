package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun ProfileErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AwanText(
            text = errorMessage,
            style = AwanTheme.styles.errorText,
        )
        AwanButton(
            onClick = onRetry,
            modifier = Modifier.wrapContentWidth()
        ) {
            AwanText(text = stringResource(ProfileR.string.profile_retry))
        }
    }
}
