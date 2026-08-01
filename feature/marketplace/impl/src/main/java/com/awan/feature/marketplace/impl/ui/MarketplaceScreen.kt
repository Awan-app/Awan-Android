package com.awan.feature.marketplace.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.annotation.SuppressLint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanTheme

@SuppressLint("HardcodedText")
@Composable
fun MarketplaceScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Market Place",
            style = AwanTheme.typography.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AwanTheme.colors.textPrimary
        )
    }
}
