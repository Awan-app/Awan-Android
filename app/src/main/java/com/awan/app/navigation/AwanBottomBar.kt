package com.awan.app.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.awan.app.R
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

private val TOP_LEVEL_ROUTES = mapOf<Route, NavBarItem>(
    Route.Home to NavBarItem(icon = Icons.Default.Home, label = R.string.navigation_home),
    Route.Arena to NavBarItem(icon = Icons.Default.Person, label = R.string.navigation_arena),
    Route.Calender to NavBarItem(icon = Icons.Default.DateRange, label = R.string.navigation_calendar),
    Route.Settings to NavBarItem(icon = Icons.Default.Settings, label = R.string.navigation_settings)
)

private data class NavBarItem(
    val icon: ImageVector,
    @StringRes val label: Int,
)

@Composable
fun AwanBottomBar(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.navigationDivider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.navigationBar)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TOP_LEVEL_ROUTES.forEach { (key, value) ->
                val isSelected = backStack.contains(key) && backStack.lastOrNull() == key
                val label = stringResource(value.label)
                val interactionSource = remember(key) { MutableInteractionSource() }
                val styleState = rememberUpdatedStyleState(interactionSource) {
                    it.isSelected = isSelected
                }
                val iconColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.meta

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
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
                        .styleable(styleState, AwanTheme.styles.navigationItem),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = value.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    AwanText(text = label, maxLines = 1)
                }
            }
        }
    }
}
