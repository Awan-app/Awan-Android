package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.helpers.ProfileHelper
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun ProfileHeaderCard(
    profile: Profile,
    uiState: ProfileState,
    onEditClick: () -> Unit,
) {
    val isDark = uiState.useDarkTheme

    val mascotBgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1B2B48) else Color(0xFFFFD93D),
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "mascotBg"
    )

    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(mascotBgColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val celestialColor by animateColorAsState(
                    targetValue = if (isDark) Color(0xFFE0E0E0) else Color(0xFFFFB74D),
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                    label = "celestialColor"
                )

                val celestialRotation by animateFloatAsState(
                    targetValue = if (isDark) -15f else 0f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                    label = "celestialRotation"
                )

                Icon(
                    imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = celestialColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .graphicsLayer {
                            rotationZ = celestialRotation
                        }
                )

                AwanMascot(
                    expression = if (isDark) MascotExpression.Idle else MascotExpression.Greet,
                    blinkEnabled = !isDark,
                    width = 64.dp,
                    modifier = Modifier.graphicsLayer {
                        val scale = if (!isDark) 1.1f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = ProfileHelper.getDisplayName(profile.firstName, profile.lastName, profile.email),
                    style = AwanTheme.styles.titleText.copy(
                        textStyle = AwanTheme.typography.title.copy(fontSize = 18.sp)
                    )
                )
                AwanText(
                    text = profile.email ?: "",
                    style = AwanTheme.styles.bodySecondaryText.copy(
                        textStyle = AwanTheme.typography.body.copy(fontSize = 12.sp)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(ProfileR.string.profile_edit),
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                icon = Icons.Default.Whatshot,
                value = profile.streak.toString(),
                label = stringResource(ProfileR.string.profile_streak),
                iconTint = AwanTheme.colors.zoneTangerine
            )
            StatItem(
                icon = Icons.Default.Star,
                value = profile.maxStreak.toString(),
                label = stringResource(ProfileR.string.profile_max_streak),
                iconTint = AwanTheme.colors.zoneSun
            )
            StatItem(
                icon = Icons.Default.Diamond,
                value = profile.points.toString(),
                label = stringResource(ProfileR.string.profile_total_points),
                iconTint = AwanTheme.colors.sky
            )
        }
    }
}
