package com.awan.feature.marketplace.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.marketplace.impl.R

@Composable
fun MarketplaceTopBar(
    pointsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AwanText(
            text = stringResource(R.string.marketplace_title),
            style = AwanTheme.styles.titleText.textStyle.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        AwanPointsBadge(
            pointsCount = pointsCount
        )
    }
}
