package com.awan.feature.goals.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.goals.impl.R

@Composable
internal fun InboxEmptyState() {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 90.dp)
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.inbox_empty_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 18.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AwanText(
            text = stringResource(R.string.inbox_empty_subtitle),
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
