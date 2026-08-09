package com.awan.feature.marketplace.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.marketplace.impl.R
import com.awan.feature.marketplace.impl.presentation.MarketplaceAction
import com.awan.feature.marketplace.impl.presentation.MarketplaceViewModel

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isCollectionMode by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
        topBar = {
            MarketplaceHeader(
                title = if (isCollectionMode) stringResource(R.string.my_collection_title) else stringResource(R.string.marketplace_title),
                points = state.points
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AwanChip(
                    label = stringResource(R.string.marketplace_title),
                    tone = if (!isCollectionMode) AwanChipTone.Sky else AwanChipTone.Neutral,
                    active = !isCollectionMode,
                    onClick = { isCollectionMode = false },
                    leading = {}
                )
                AwanChip(
                    label = stringResource(R.string.my_collection_title),
                    tone = if (isCollectionMode) AwanChipTone.Sky else AwanChipTone.Neutral,
                    active = isCollectionMode,
                    onClick = { isCollectionMode = true },
                    leading = {}
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CategoryTabs(
                selectedCategory = state.selectedCategory,
                onCategorySelected = { viewModel.onAction(MarketplaceAction.SelectCategory(it)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isCollectionMode) {
                MarketplaceSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onAction(MarketplaceAction.UpdateSearchQuery(it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.isLoading && state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AwanTheme.colors.sky)
                }
            } else {
                val displayItems = if (isCollectionMode) {
                    state.ownedItems.filter { item ->
                        (state.selectedCategory == null || item.type == state.selectedCategory)
                    }
                } else {
                    state.filteredItems
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayItems, key = { it.id }) { item ->
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

    state.selectedItem?.let { item ->
        ItemDetailsBottomSheet(
            item = item,
            isOwned = item.id in state.ownedItemIds,
            isEquipped = item.id in state.equippedItemIds,
            currentPoints = state.points,
            onBuyClick = { viewModel.onAction(MarketplaceAction.BuyItem(item)) },
            onEquipClick = { viewModel.onAction(MarketplaceAction.EquipItem(item)) },
            onDismiss = { viewModel.onAction(MarketplaceAction.SelectItem(null)) },
            isProcessing = state.isBuying || state.isEquipping
        )
    }
}

@Composable
fun MarketplaceHeader(
    title: String,
    points: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AwanText(
            text = title,
            style = AwanTheme.typography.title
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AwanTheme.colors.surface)
                .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            AwanText(text = "🪙", style = AwanTheme.typography.body)
            AwanText(
                text = points.toString(),
                style = AwanTheme.typography.heading.copy(
                    color = Color(0xFFCA8A04),
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
fun CategoryTabs(
    selectedCategory: StoreItemType?,
    onCategorySelected: (StoreItemType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AwanChip(
            label = stringResource(R.string.category_all),
            tone = if (selectedCategory == null) AwanChipTone.Sky else AwanChipTone.Neutral,
            active = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            leading = {}
        )

        StoreItemType.entries.forEach { type ->
            AwanChip(
                label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                tone = if (selectedCategory == type) AwanChipTone.Sky else AwanChipTone.Neutral,
                active = selectedCategory == type,
                onClick = { onCategorySelected(type) },
                leading = {}
            )
        }
    }
}

@Composable
fun MarketplaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AwanTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.search_placeholder),
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AwanTheme.colors.surface)
            .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(AwanTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            AwanRemoteImage(
                url = item.image,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AwanTheme.colors.sky),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AwanText(
            text = item.name,
            style = AwanTheme.typography.heading.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isOwned) {
                AwanText(
                    text = stringResource(R.string.item_owned),
                    style = AwanTheme.typography.body.copy(
                        fontSize = 12.sp,
                        color = AwanTheme.colors.textSecondary
                    )
                )
            } else {
                AwanText(text = "🪙", style = AwanTheme.typography.body.copy(fontSize = 12.sp))
                AwanText(
                    text = item.price.toString(),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 12.sp,
                        color = Color(0xFFCA8A04)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsBottomSheet(
    item: StoreItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    currentPoints: Int,
    onBuyClick: () -> Unit,
    onEquipClick: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AwanTheme.colors.surface,
        contentColor = AwanTheme.colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AwanRemoteImage(
                url = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AwanTheme.colors.background)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AwanText(text = item.name, style = AwanTheme.typography.title)
                AwanText(
                    text = item.type.name,
                    style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textSecondary)
                )
            }

            AwanText(
                text = item.description,
                style = AwanTheme.typography.body,
                modifier = Modifier.fillMaxWidth()
            )

            val canAfford = currentPoints >= item.price

            AwanButton(
                onClick = { if (isOwned) onEquipClick() else onBuyClick() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && (isOwned || canAfford),
                variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary
            ) {
                val label = when {
                    isEquipped -> stringResource(R.string.action_equipped)
                    isOwned -> stringResource(R.string.action_equip)
                    else -> stringResource(R.string.action_buy, item.price)
                }
                AwanText(text = label)
            }
        }
    }
}
