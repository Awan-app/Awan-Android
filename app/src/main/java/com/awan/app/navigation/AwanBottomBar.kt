package com.awan.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

private val TOP_LEVEL_ROUTES = mapOf<Route, NavBarItem>(
    Route.Home to NavBarItem(icon = Icons.Default.Home, label = "Home"),
    Route.Arena to NavBarItem(icon = Icons.Default.Person, label = "Arena"),
    Route.Calender to NavBarItem(icon = Icons.Default.DateRange, label = "Calender"),
    Route.Settings to NavBarItem(icon = Icons.Default.Settings, label = "Settings")
)

private data class NavBarItem(
    val icon: ImageVector,
    val label: String
)

@Composable
fun AwanBottomBar(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            ),
        shape = CircleShape,
        color = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TOP_LEVEL_ROUTES.forEach { (key, value) ->
                val isSelected = backStack.contains(key) && backStack.lastOrNull() == key
                
                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        Color.Transparent
                    }
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(itemBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (backStack.contains(key)) {
                                    while (backStack.lastOrNull() != key) {
                                        backStack.removeLastOrNull()
                                    }
                                } else {
                                    if (key == Route.Home) {
                                        backStack.clear()
                                        backStack.add(Route.Home)
                                    } else {
                                        backStack.clear()
                                        backStack.add(Route.Home)
                                        backStack.add(key)
                                    }
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = value.icon,
                        contentDescription = value.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = value.label,
                            color = contentColor,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
