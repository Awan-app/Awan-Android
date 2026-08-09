package com.awan.feature.marketplace.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    var mascotExpression by remember { mutableStateOf(MascotExpression.Idle) }

    // Success Animations Logic
    LaunchedEffect(state.isBuying, state.isEquipping) {
        if (!state.isBuying && !state.isEquipping && state.error == null) {
            mascotExpression = MascotExpression.Celebrate
            delay(3.seconds)
            mascotExpression = MascotExpression.Idle
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
        topBar = {
            MarketplaceTopBar(mascotExpression = mascotExpression)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background Clouds
            AwanCloudsHorizon(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = (-80).dp)
                    .zIndex(-1f)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MarketplacePointsCard(
                    points = state.points,
                    onAddClick = onNavigateToHome
                )

                MarketplaceSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onAction(MarketplaceAction.UpdateSearchQuery(it)) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                CategoryTabs(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.onAction(MarketplaceAction.SelectCategory(it)) }
                )

                if (state.isLoading && state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AwanTheme.colors.sky)
                    }
                } else {
                    if (state.filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            AwanText(
                                text = stringResource(R.string.marketplace_empty_items),
                                style = AwanTheme.styles.bodySecondaryText
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 8.dp,
                                bottom = 80.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.filteredItems, key = { it.id }) { item ->
                                StoreItemCard(
                                    item = item,
                                    isOwned = item.id in state.ownedItemIds,
                                    isEquipped = item.id in state.equippedItemIds,
                                    onClick = { viewModel.onAction(MarketplaceAction.SelectItem(item)) }
                                )
                            }
                        }
                    }
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
            onBuyClick = { itemToBuy = item },
            onEquipClick = { itemToEquip = item },
            onDismiss = { viewModel.onAction(MarketplaceAction.SelectItem(null)) },
            isProcessing = state.isBuying || state.isEquipping
        )
    }

    // Confirmation Dialogs using AwanConfirmDialog
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
