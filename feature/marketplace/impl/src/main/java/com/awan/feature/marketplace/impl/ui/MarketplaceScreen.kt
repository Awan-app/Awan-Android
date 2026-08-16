package com.awan.feature.marketplace.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.StoreItem
import com.awan.feature.marketplace.impl.R
import com.awan.feature.marketplace.impl.presentation.MarketplaceAction
import com.awan.feature.marketplace.impl.presentation.MarketplaceViewModel
import com.awan.feature.marketplace.impl.ui.components.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var itemToBuy by remember { mutableStateOf<StoreItem?>(null) }
    var itemToEquip by remember { mutableStateOf<StoreItem?>(null) }
    var itemToUnequip by remember { mutableStateOf<StoreItem?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
        topBar = {
            MarketplaceTopBar(pointsCount = state.points)
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MarketplaceSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onAction(MarketplaceAction.UpdateSearchQuery(it)) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryTabs(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.onAction(MarketplaceAction.SelectCategory(it)) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.isLoading && state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AwanTheme.colors.sky)
                    }
                }
            } else if (state.filteredItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        AwanText(
                            text = stringResource(R.string.marketplace_empty_items),
                            style = AwanTheme.styles.bodySecondaryText
                        )
                    }
                }
            } else {
                items(state.filteredItems, key = { it.id }) { item ->
                    StoreItemCard(
                        item = item,
                        isOwned = item.id in state.ownedItemIds,
                        isEquipped = item.id in state.equippedItemIds,
                        accessToken = state.accessToken,
                        onClick = { viewModel.onAction(MarketplaceAction.SelectItem(item)) }
                    )
                }
            }
        }
    }

    state.selectedItem?.let { item ->
        ItemDetailsBottomSheet(
            item = item,
            isOwned = item.id in state.ownedItemIds,
            isEquipped = item.id in state.equippedItemIds,
            currentPoints = state.points,
            accessToken = state.accessToken,
            onBuyClick = { itemToBuy = item },
            onEquipClick = { itemToEquip = item },
            onUnequipClick = { itemToUnequip = item },
            onDismiss = { viewModel.onAction(MarketplaceAction.SelectItem(null)) },
            isProcessing = state.isBuying || state.isEquipping
        )
    }

    // Confirmation Dialogs
    itemToBuy?.let { item ->
        AwanConfirmDialog(
            title = stringResource(R.string.marketplace_confirm_buy_title),
            body = stringResource(R.string.marketplace_confirm_buy_body, item.name, item.price),
            confirmLabel = stringResource(R.string.marketplace_action_confirm),
            dismissLabel = stringResource(R.string.marketplace_action_cancel),
            onConfirm = {
                viewModel.onAction(MarketplaceAction.BuyItem(item))
                itemToBuy = null
            },
            onDismiss = { itemToBuy = null }
        )
    }

    itemToEquip?.let { item ->
        AwanConfirmDialog(
            title = stringResource(R.string.marketplace_confirm_equip_title),
            body = stringResource(R.string.marketplace_confirm_equip_body, item.name),
            confirmLabel = stringResource(R.string.marketplace_action_confirm),
            dismissLabel = stringResource(R.string.marketplace_action_cancel),
            onConfirm = {
                viewModel.onAction(MarketplaceAction.EquipItem(item))
                itemToEquip = null
            },
            onDismiss = { itemToEquip = null }
        )
    }

    itemToUnequip?.let { item ->
        AwanConfirmDialog(
            title = stringResource(R.string.marketplace_confirm_unequip_title),
            body = stringResource(R.string.marketplace_confirm_unequip_body, item.name),
            confirmLabel = stringResource(R.string.marketplace_action_confirm),
            dismissLabel = stringResource(R.string.marketplace_action_cancel),
            onConfirm = {
                viewModel.onAction(MarketplaceAction.UnequipItem(item.type))
                itemToUnequip = null
            },
            onDismiss = { itemToUnequip = null }
        )
    }

    state.error?.let { errorRes ->
        AwanConfirmDialog(
            title = stringResource(R.string.marketplace_title),
            body = stringResource(errorRes),
            confirmLabel = "OK",
            onConfirm = { viewModel.onAction(MarketplaceAction.DismissError) },
            onDismiss = { viewModel.onAction(MarketplaceAction.DismissError) }
        )
    }
}
