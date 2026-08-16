package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val id: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val isFab: Boolean = false,
)

@Composable
fun AwanBottomNavBar(
    items: List<BottomNavItem>,
    selectedItemId: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Id of the tab a won item should fly to. Null leaves the anchor unregistered. */
    anchoredItemId: String? = null,
) {
    val navBarShape = RoundedCornerShape(22.dp)
    val navBarRimDepth = 4.dp
    val rewardAnchors = LocalRewardAnchors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 2D Gamification Nav Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp + navBarRimDepth),
            contentAlignment = Alignment.TopCenter
        ) {
            // Bottom 2D Rim Base Layer
            Box(
                modifier = Modifier
                    .offset(y = navBarRimDepth)
                    .fillMaxWidth()
                    .height(62.dp)
                    .clip(navBarShape)
                    .background(AwanTheme.colors.line)
            )

            // Front Panel Face Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = navBarShape,
                        spotColor = Color(0x1F000000)
                    )
                    .clip(navBarShape)
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 2.dp,
                        color = AwanTheme.colors.line,
                        shape = navBarShape
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        if (item.isFab) {
                            // Spacer slot with dedicated width for floating center button
                            Spacer(modifier = Modifier.size(60.dp))
                        } else {
                            val isSelected = item.id == selectedItemId
                            NavTabItem(
                                item = item,
                                isSelected = isSelected,
                                onClick = { onItemSelected(item) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (item.id == anchoredItemId) {
                                            Modifier.rewardAnchor(
                                                anchor = RewardAnchor.ProfileTab,
                                                anchors = rewardAnchors,
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                            )
                        }
                    }
                }
            }
        }

        // Square-Rounded 3D Primary Button (Duolingo Gamification Style)
        items.find { it.isFab }?.let { fabItem ->
            SquareRounded3dPrimaryButton(
                item = fabItem,
                onClick = onFabClick,
                modifier = Modifier.offset(y = (-14).dp)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val hapticClick = rememberHapticClick(onClick)

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isFocused -> 1.12f
            isSelected -> 1.06f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tabScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected || isFocused) AwanTheme.colors.sky else AwanTheme.colors.textSecondary.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 120),
        label = "tabIconColor"
    )

    val tileShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(width = 42.dp, height = 42.dp)
                .clip(tileShape)
                .then(
                    if (isSelected || isFocused) {
                        Modifier
                            .background(AwanTheme.colors.sky.copy(alpha = if (isFocused) 0.20f else 0.12f))
                            .border(
                                width = 1.5.dp,
                                color = AwanTheme.colors.sky.copy(alpha = if (isFocused) 0.60f else 0.35f),
                                shape = tileShape
                            )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Square-rounded 3D Game-style primary button (Duolingo Style):
 * Uses a square-rounded shape matching the 2D gamification theme of the nav bar body.
 */
@Composable
private fun SquareRounded3dPrimaryButton(
    item: BottomNavItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val hapticClick = rememberHapticClick(onClick, HapticFeedbackType.Confirm)

    val buttonShape = RoundedCornerShape(18.dp)
    val rimDepth = 4.dp
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonPressOffset"
    )

    val primaryColor = AwanTheme.colors.sky
    val rimColor = AwanTheme.colors.skyPressed

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 56.dp + rimDepth)
            .semantics {
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.TopCenter
    ) {
        // Bottom 3D Rim Base Layer
        Box(
            modifier = Modifier
                .offset(y = rimDepth)
                .size(56.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = buttonShape,
                    spotColor = Color(0x33000000)
                )
                .clip(buttonShape)
                .background(rimColor)
        )

        // Top Front Face Layer (Presses down on click)
        Box(
            modifier = Modifier
                .offset(y = pressOffsetY)
                .size(56.dp)
                .clip(buttonShape)
                .background(primaryColor)
                .border(
                    width = if (isFocused) 2.5.dp else 1.5.dp,
                    color = if (isFocused) AwanTheme.colors.sky else Color.White.copy(alpha = 0.4f),
                    shape = buttonShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.selectedIcon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
