package com.awan.feature.inventory.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanRemoteImage
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.model.StoreItemType
import com.awan.feature.inventory.impl.R
import com.awan.feature.inventory.impl.presentation.InventoryAction
import com.awan.feature.inventory.impl.presentation.InventoryItem
import com.awan.feature.inventory.impl.presentation.InventorySort
import com.awan.feature.inventory.impl.presentation.InventoryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    onBack: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        containerColor = AwanTheme.colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { AwanText(stringResource(R.string.inventory_title), style = AwanTheme.styles.titleText) },
                navigationIcon = {
                    AwanIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.inventory_back),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    AwanIconButton(
                        onClick = { onAction(InventoryAction.Refresh) },
                        enabled = !state.isRefreshing,
                        contentDescription = stringResource(R.string.inventory_refresh),
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> InventoryLoading(modifier = Modifier.padding(padding))
            state.items.isEmpty() && !state.isOnline -> InventoryEmpty(
                title = stringResource(R.string.inventory_offline_title),
                body = stringResource(R.string.inventory_offline_body),
                modifier = Modifier.padding(padding),
            )
            state.items.isEmpty() && state.error != null -> InventoryError(
                message = state.error.asString(),
                onRetry = { onAction(InventoryAction.Refresh) },
                modifier = Modifier.padding(padding),
            )
            state.items.isEmpty() -> InventoryEmpty(
                title = stringResource(R.string.inventory_empty_title),
                body = stringResource(R.string.inventory_empty_body),
                modifier = Modifier.padding(padding),
            )
            else -> InventoryContent(state, onAction, Modifier.padding(padding))
        }
    }
}

@Composable
private fun InventoryContent(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        item {
            InventoryFilters(state, onAction)
        }
        state.error?.let { error ->
            item { AwanText(error.asString(), style = AwanTheme.styles.bodySecondaryText) }
        }
        if (!state.isOnline) {
            item { AwanText(stringResource(R.string.inventory_offline_equip), style = AwanTheme.styles.metaText) }
        }
        if (state.sections.isEmpty()) {
            item {
                InventoryEmpty(
                    title = stringResource(R.string.inventory_filtered_empty_title),
                    body = stringResource(R.string.inventory_filtered_empty_body),
                )
            }
        } else {
            state.sections.forEach { section ->
                item(key = "header-${section.type}") {
                    AwanText(typeLabel(section.type), style = AwanTheme.styles.headingText)
                }
                items(section.items.size, key = { index -> section.items[index].itemId }) { index ->
                    CustomizationCard(
                        item = section.items[index],
                        isOnline = state.isOnline,
                        isEquipping = state.equippingItemId == section.items[index].itemId,
                        onEquip = { onAction(InventoryAction.Equip(section.items[index].itemId)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryFilters(state: InventoryState, onAction: (InventoryAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
        AwanText(stringResource(R.string.inventory_filter_type), style = AwanTheme.styles.metaText)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            item {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { onAction(InventoryAction.SelectType(null)) },
                    label = { AwanText(stringResource(R.string.inventory_all_types)) },
                )
            }
            items(StoreItemType.entries.size) { index ->
                val type = StoreItemType.entries[index]
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onAction(InventoryAction.SelectType(type)) },
                    label = { AwanText(typeLabel(type)) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AwanText(stringResource(R.string.inventory_sort), style = AwanTheme.styles.metaText, modifier = Modifier.weight(1f))
            InventorySort.entries.forEach { sort ->
                TextButton(onClick = { onAction(InventoryAction.SetSort(sort)) }) {
                    AwanText(
                        text = sortLabel(sort),
                        style = if (state.sort == sort) AwanTheme.styles.bodyText else AwanTheme.styles.metaText,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizationCard(
    item: InventoryItem,
    isOnline: Boolean,
    isEquipping: Boolean,
    onEquip: () -> Unit,
) {
    AwanCard(modifier = Modifier.fillMaxWidth(), selected = item.isEquipped) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.imageUrl == null) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AwanTheme.colors.sky)
                } else {
                    AwanRemoteImage(
                        url = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(AwanTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(item.name, style = AwanTheme.styles.bodyText)
                AwanText(typeLabel(item.type), style = AwanTheme.styles.metaText)
            }
            AwanButton(
                onClick = onEquip,
                enabled = isOnline && !item.isEquipped,
                isLoading = isEquipping,
                variant = if (item.isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary,
            ) {
                AwanText(
                    stringResource(
                        if (item.isEquipped) R.string.inventory_equipped else R.string.inventory_equip,
                    ),
                )
            }
        }
    }
}

@Composable
private fun InventoryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun InventoryEmpty(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AwanTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 96.dp)
        Spacer(Modifier.height(AwanTheme.spacing.sm))
        AwanText(title, style = AwanTheme.styles.headingText)
        AwanText(body, style = AwanTheme.styles.bodySecondaryText)
    }
}

@Composable
private fun InventoryError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(AwanTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 96.dp)
        Spacer(Modifier.height(AwanTheme.spacing.sm))
        AwanText(stringResource(R.string.inventory_error_title), style = AwanTheme.styles.headingText)
        AwanText(message, style = AwanTheme.styles.bodySecondaryText)
        Spacer(Modifier.height(AwanTheme.spacing.sm))
        AwanButton(onClick = onRetry) { AwanText(stringResource(R.string.inventory_retry)) }
    }
}

@Composable
private fun typeLabel(type: StoreItemType): String = stringResource(
    when (type) {
        StoreItemType.FRAME -> R.string.inventory_type_frame
        StoreItemType.SKIN -> R.string.inventory_type_skin
        StoreItemType.THEME -> R.string.inventory_type_theme
        StoreItemType.ICON -> R.string.inventory_type_icon
    },
)

@Composable
private fun sortLabel(sort: InventorySort): String = stringResource(
    when (sort) {
        InventorySort.NEWEST -> R.string.inventory_sort_newest
        InventorySort.NAME -> R.string.inventory_sort_name
    },
)
