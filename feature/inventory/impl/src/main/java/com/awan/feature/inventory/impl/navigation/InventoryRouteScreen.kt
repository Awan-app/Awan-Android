package com.awan.feature.inventory.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.inventory.impl.presentation.InventoryViewModel
import com.awan.feature.inventory.impl.ui.InventoryScreen

@Composable
fun InventoryRouteScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InventoryScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}
