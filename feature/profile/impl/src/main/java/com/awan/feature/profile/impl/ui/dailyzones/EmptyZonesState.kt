package com.awan.feature.profile.impl.ui.dailyzones

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R

@Composable
fun EmptyZonesState(
    hasTemplate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val animY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { translationY = animY },
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        val title = if (hasTemplate) {
            stringResource(R.string.profile_routine_no_zones)
        } else {
            stringResource(R.string.profile_routine_no_routine_set)
        }

        val hint = if (hasTemplate) {
            stringResource(R.string.profile_routine_no_zones_hint)
        } else {
            stringResource(R.string.profile_routine_connect_hint)
        }

        AwanText(
            text = title,
            style = AwanTheme.styles.titleText.copy(
                color = AwanTheme.colors.textPrimary,
                textStyle = AwanTheme.styles.titleText.textStyle.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        AwanText(
            text = hint,
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            ),
            modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth()
        )
    }
}
