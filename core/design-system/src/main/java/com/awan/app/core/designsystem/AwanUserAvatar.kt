package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal const val AwanUserAvatarFrameTestTag = "awan-user-avatar-frame"

@Composable
fun AwanUserAvatar(
    profilePictureUrl: String?,
    frameImageUrl: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isDark) AwanTheme.colors.skyMidday else AwanTheme.colors.zoneSun,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "avatarBackground",
    )
    val avatarSemantics = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
    }

    Box(
        modifier = modifier
            .then(avatarSemantics),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            if (profilePictureUrl != null) {
                AwanRemoteImage(url = profilePictureUrl, modifier = Modifier.fillMaxSize())
            } else {
                val celestialColor by animateColorAsState(
                    targetValue = if (isDark) AwanTheme.colors.textPrimary else AwanTheme.colors.zoneTangerine,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                    label = "avatarCelestial",
                )
                Icon(
                    imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = celestialColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .graphicsLayer { rotationZ = if (isDark) -15f else 0f },
                )
                AwanMascot(
                    expression = if (isDark) MascotExpression.Idle else MascotExpression.Greet,
                    blinkEnabled = !isDark,
                    width = 64.dp,
                    modifier = Modifier.graphicsLayer {
                        val scale = if (isDark) 1f else 1.1f
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
        if (frameImageUrl != null) {
            AwanRemoteImage(
                url = frameImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().semantics { testTag = AwanUserAvatarFrameTestTag },
            )
        }
    }
}

@Preview(name = "Awan user avatar · Mock frame", showBackground = true)
@Composable
fun AwanUserAvatarPreview() {
    AwanTheme {
        AwanUserAvatar(
            profilePictureUrl = "https://cdn.example.com/avatars/demo.png",
            frameImageUrl = "https://cdn.example.com/frames/gold.png",
            isDark = false,
            contentDescription = "Preview user avatar",
            modifier = Modifier.size(96.dp),
        )
    }
}
