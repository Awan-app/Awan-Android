package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R

@Composable
fun EmptyCategoriesState(onAddClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "categories_empty")
    val animY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = animY },
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(24.dp))
        AwanText(
            text = stringResource(R.string.profile_zone_category_empty),
            style = AwanTheme.styles.titleText.copy(
                textStyle = AwanTheme.styles.titleText.textStyle.copy(textAlign = TextAlign.Center)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        AwanText(
            text = stringResource(R.string.profile_daily_zones_no_zones_hint),
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(textAlign = TextAlign.Center)
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        AwanButton(onClick = onAddClick) {
            AwanText(text = stringResource(R.string.profile_category_add))
        }
    }
}
