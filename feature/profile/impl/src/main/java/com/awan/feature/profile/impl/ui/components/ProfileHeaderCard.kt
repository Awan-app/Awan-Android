package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanUserAvatar
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.helpers.ProfileHelper
import com.awan.feature.profile.impl.presentation.ProfileState

@Composable
fun ProfileHeaderCard(
    profile: Profile,
    uiState: ProfileState,
    onEditClick: () -> Unit,
    onPictureClick: () -> Unit = {},
) {
    AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(
                        enabled = profile.profilePictureUrl != null,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPictureClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AwanUserAvatar(
                    profilePictureUrl = profile.profilePictureUrl,
                    frameImageUrl = uiState.equippedFrameImageUrl,
                    isDark = uiState.useDarkTheme,
                    modifier = Modifier.fillMaxSize(),
                )
                if (uiState.isUploadingPicture) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(AwanTheme.colors.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = AwanTheme.colors.sky,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = ProfileHelper.getDisplayName(profile.firstName, profile.lastName, profile.email)
                        .ifBlank { stringResource(ProfileR.string.profile_user_placeholder) },
                    style = AwanTheme.styles.titleText.copy(
                        textStyle = AwanTheme.typography.title.copy(fontSize = 18.sp),
                    ),
                )
                AwanText(
                    text = profile.email ?: "",
                    style = AwanTheme.styles.bodySecondaryText.copy(
                        textStyle = AwanTheme.typography.body.copy(fontSize = 12.sp),
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.clip(CircleShape).background(AwanTheme.colors.sky.copy(alpha = 0.1f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(ProfileR.string.profile_edit),
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(
                icon = Icons.Default.Whatshot,
                value = (profile.streak ?: 0).toString(),
                label = stringResource(ProfileR.string.profile_streak),
                iconTint = AwanTheme.colors.zoneTangerine,
            )
            StatItem(
                icon = Icons.Default.Star,
                value = (profile.maxStreak ?: 0).toString(),
                label = stringResource(ProfileR.string.profile_max_streak),
                iconTint = AwanTheme.colors.zoneSun,
            )
            StatItem(
                icon = Icons.Default.Diamond,
                value = (profile.points ?: 0).toString(),
                label = stringResource(ProfileR.string.profile_total_points),
                iconTint = AwanTheme.colors.sky,
            )
        }
    }
}
