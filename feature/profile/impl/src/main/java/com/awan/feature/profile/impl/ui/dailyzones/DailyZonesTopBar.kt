package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZonesTopBar(
    onBackClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AwanText(text = "Daily zones", style = AwanTheme.styles.titleText)
                AwanText(
                    text = "Shape your week your way.",
                    style = AwanTheme.styles.metaText
                )
            }
        },
        navigationIcon = {
            AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = AwanTheme.colors.textPrimary
                )
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                Icon(
                    painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AwanTheme.colors.background
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}
