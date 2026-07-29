package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val id: String,
    val label: String,
    val icon: String,
)

@Composable
fun AwanBottomNavBar(
    items: List<BottomNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = barShape,
                spotColor = Color(0xFF94A3B8),
            )
            .clip(barShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFFE2E8F0), barShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedItemId
                BottomNavItemCell(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemSelected(item.id) },
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemCell(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(200),
        label = "navItemScale",
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
        label = "navItemTextColor",
    )

    val itemShape = RoundedCornerShape(99.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(4.dp, itemShape, spotColor = Color(0xFF2563EB))
                        .clip(itemShape)
                        .background(Color(0xFFEFF6FF))
                        .border(1.5.dp, Color(0xFFBFDBFE), itemShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AwanText(
                text = item.icon,
                style = AwanTheme.typography.body.copy(fontSize = 16.sp),
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                AwanText(
                    text = item.label,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 13.5.sp,
                        color = textColor,
                    ),
                )
            }
        }
    }
}
