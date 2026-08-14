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
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.goals.impl.R

@Composable
internal fun GoalsEmptyState(
    modifier: Modifier = Modifier,
) {
    val spacing = AwanTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 100.dp)
        Spacer(modifier = Modifier.height(spacing.lg))
        AwanText(
            text = stringResource(R.string.goals_empty_active_title),
            style = AwanTheme.styles.headingText.textStyle.copy(
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        AwanText(
            text = stringResource(R.string.goals_empty_active_subtitle),
            style = AwanTheme.styles.bodySecondaryText.textStyle.copy(
                textAlign = TextAlign.Center,
            ),
        )
    }
}
