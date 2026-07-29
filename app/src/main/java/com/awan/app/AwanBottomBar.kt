package com.awan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.core.navigation.Route

private val AddButtonSize = 52.dp

/** Home · Calendar · (+) · Goals · Profile — the `+` opens the add-task sheet, it isn't a route. */
@Composable
fun AwanBottomBar(
    destinations: List<TopLevelDestination>,
    currentTopLevelKey: Route,
    onNavigate: (Route) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val half = destinations.size / 2
    NavigationBar(modifier = modifier) {
        destinations.take(half).forEach { DestinationItem(it, currentTopLevelKey, onNavigate) }
        AddTaskItem(onAddTask)
        destinations.drop(half).forEach { DestinationItem(it, currentTopLevelKey, onNavigate) }
    }
}

@Composable
private fun RowScope.DestinationItem(
    destination: TopLevelDestination,
    currentTopLevelKey: Route,
    onNavigate: (Route) -> Unit,
) {
    val label = stringResource(destination.labelRes)
    NavigationBarItem(
        selected = currentTopLevelKey == destination.route,
        onClick = { onNavigate(destination.route) },
        icon = { Icon(imageVector = destination.icon, contentDescription = label) },
        label = { Text(label) },
    )
}

@Composable
private fun RowScope.AddTaskItem(onAddTask: () -> Unit) {
    val label = stringResource(R.string.navigation_add_task)
    NavigationBarItem(
        selected = false,
        onClick = onAddTask,
        icon = {
            Box(
                modifier = Modifier
                    .size(AddButtonSize)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = label,
                    tint = AwanTheme.colors.onSky,
                )
            }
        },
    )
}
