package com.awan.feature.inventory.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.feature.inventory.impl.R
import com.awan.feature.inventory.impl.presentation.InventoryAction
import com.awan.feature.inventory.impl.presentation.InventorySort
import com.awan.feature.inventory.impl.presentation.InventoryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    onBack: () -> Unit,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var detailsItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailsItem = state.customizations.firstOrNull { it.itemId == detailsItemId }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Scaffold(
            containerColor = AwanTheme.colors.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        AwanText(
                            stringResource(R.string.inventory_title),
                            style = AwanTheme.styles.titleText,
                        )
                    },
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
                            onClick = { controlsVisible = true },
                            contentDescription = stringResource(R.string.inventory_controls),
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                        }
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
                state.isLoading && state.customizations.isEmpty() -> InventoryLoading(modifier = Modifier.padding(padding))
                state.customizations.isEmpty() && !state.isOnline -> InventoryEmpty(
                    title = stringResource(R.string.inventory_offline_title),
                    body = stringResource(R.string.inventory_offline_body),
                    modifier = Modifier.padding(padding),
                )
                state.customizations.isEmpty() && state.error != null -> InventoryError(
                    message = state.error.asString(),
                    onRetry = { onAction(InventoryAction.Refresh) },
                    modifier = Modifier.padding(padding),
                )
                state.customizations.isEmpty() -> InventoryEmpty(
                    title = stringResource(R.string.inventory_empty_title),
                    body = stringResource(R.string.inventory_empty_body),
                    modifier = Modifier.padding(padding),
                )
                else -> InventoryContent(
                    state = state,
                    onAction = onAction,
                    onOpenDetails = { detailsItemId = it },
                    modifier = Modifier.padding(padding),
                )
            }
        }

        if (controlsVisible) {
            ModalBottomSheet(onDismissRequest = { controlsVisible = false }) {
                InventoryControlsSheet(state = state, onAction = onAction)
            }
        }

        detailsItem?.let { customization ->
            ModalBottomSheet(onDismissRequest = { detailsItemId = null }) {
                CustomizationDetailsSheet(
                    customization = customization,
                    state = state,
                    onEquip = { onAction(InventoryAction.Equip(customization.itemId)) },
                )
            }
        }
    }
}

@Composable
private fun InventoryContent(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        state.error?.let { error ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                AwanText(error.asString(), style = AwanTheme.styles.bodySecondaryText)
            }
        }
        if (!state.isOnline) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AwanText(stringResource(R.string.inventory_offline_equip), style = AwanTheme.styles.metaText)
            }
        }
        if (state.sections.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InventoryEmpty(
                    title = stringResource(R.string.inventory_filtered_empty_title),
                    body = stringResource(R.string.inventory_filtered_empty_body),
                )
            }
        } else {
            state.sections.forEach { section ->
                item(key = "header-${section.type}", span = { GridItemSpan(maxLineSpan) }) {
                    AwanText(typeLabel(section.type), style = AwanTheme.styles.headingText)
                }
                items(section.items, key = { it.itemId }) { customization ->
                    CustomizationCard(
                        customization = customization,
                        isOnline = state.isOnline,
                        isEquipping = state.equippingItemId == customization.itemId,
                        onEquip = { onAction(InventoryAction.Equip(customization.itemId)) },
                        onDetails = { onOpenDetails(customization.itemId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryControlsSheet(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        item {
            AwanText(stringResource(R.string.inventory_controls), style = AwanTheme.styles.headingText)
        }
        item { InventoryFilters(state = state, onAction = onAction) }
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
            items(CustomizationType.entries.size) { index ->
                val type = CustomizationType.entries[index]
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onAction(InventoryAction.SelectType(type)) },
                    label = { AwanText(typeLabel(type)) },
                )
            }
        }
        AwanText(stringResource(R.string.inventory_filter_rarity), style = AwanTheme.styles.metaText)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            items(CustomizationRarity.entries.size) { index ->
                val rarity = CustomizationRarity.entries[index]
                FilterChip(
                    selected = rarity in state.selectedRarities,
                    onClick = { onAction(InventoryAction.ToggleRarity(rarity)) },
                    label = { AwanText(rarityLabel(rarity)) },
                )
            }
        }
        AwanText(stringResource(R.string.inventory_sort), style = AwanTheme.styles.metaText)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            items(InventorySort.entries.size) { index ->
                val sort = InventorySort.entries[index]
                FilterChip(
                    selected = state.sort == sort,
                    onClick = { onAction(InventoryAction.SetSort(sort)) },
                    label = { AwanText(sortLabel(sort)) },
                )
            }
        }
    }
}

@Composable
private fun CustomizationCard(
    customization: OwnedCustomization,
    isOnline: Boolean,
    isEquipping: Boolean,
    onEquip: () -> Unit,
    onDetails: () -> Unit,
) {
    val canEquip = isOnline && !customization.isEquipped && !isEquipping
    val itemStateDescription = stringResource(
        if (customization.isEquipped) {
            R.string.inventory_item_equipped_state
        } else {
            R.string.inventory_item_not_equipped_state
        },
    )
    AwanCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory-card-${customization.itemId}")
            .semantics {
                stateDescription = itemStateDescription
            },
        selected = customization.isEquipped,
        onClick = if (canEquip) onEquip else null,
        contentPadding = PaddingValues(AwanTheme.spacing.sm),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CustomizationArt(
                customization = customization,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            AwanIconButton(
                onClick = onDetails,
                contentDescription = stringResource(R.string.inventory_view_details, customization.name),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
            }
            if (isEquipping) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                )
            }
        }
        Spacer(Modifier.height(AwanTheme.spacing.xs))
        AwanText(customization.name, style = AwanTheme.styles.bodyText, maxLines = 1)
        AwanText(
            rarityLabel(customization.rarity),
            style = AwanTheme.styles.metaText.copy(color = rarityAccent(customization.rarity)),
        )
    }
}

@Composable
internal fun CustomizationDetailsSheet(
    customization: OwnedCustomization,
    state: InventoryState,
    onEquip: () -> Unit,
) {
    val isEquipped = customization.isEquipped
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(AwanTheme.shapes.card)
                    .testTag("inventory-details-artwork"),
            ) {
                CustomizationArt(
                    customization = customization,
                    modifier = Modifier.fillMaxSize(),
                    showRarityBackdrop = false,
                )
            }
            AwanText(customization.name, style = AwanTheme.styles.headingText)
            AwanText(
                stringResource(R.string.inventory_details_rarity, rarityLabel(customization.rarity)),
                style = AwanTheme.styles.metaText.copy(color = rarityAccent(customization.rarity)),
            )
            AwanText(
                stringResource(R.string.inventory_details_type, typeLabel(customization.type)),
                style = AwanTheme.styles.metaText,
            )
            AwanText(customization.description, style = AwanTheme.styles.bodySecondaryText)
            AwanButton(
                onClick = onEquip,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !isEquipped,
                isLoading = state.equippingItemId == customization.itemId,
                variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary,
            ) {
                AwanText(
                    stringResource(if (isEquipped) R.string.inventory_equipped else R.string.inventory_equip),
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .detailsSheetEdgeShadow(
                    accent = rarityAccent(customization.rarity),
                    artworkAspectRatio = 1.4f,
                    horizontalPadding = AwanTheme.spacing.md,
                    topPadding = AwanTheme.spacing.sm,
                    edgeDepth = AwanTheme.spacing.lg,
                    bottomBlendDepth = AwanTheme.spacing.sm,
                ),
        )
    }
}

@Composable
private fun CustomizationArt(
    customization: OwnedCustomization,
    modifier: Modifier,
    showRarityBackdrop: Boolean = true,
) {
    val accent = rarityAccent(customization.rarity)
    val backdropModifier = if (showRarityBackdrop) {
        Modifier.background(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.4f), AwanTheme.colors.surface),
            ),
        )
    } else {
        Modifier.background(AwanTheme.colors.surface)
    }
    Box(
        modifier = modifier
            .clip(AwanTheme.shapes.card)
            .then(backdropModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (customization.imageUrl == null) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent)
        } else {
            AwanRemoteImage(
                url = customization.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun Modifier.detailsSheetEdgeShadow(
    accent: Color,
    artworkAspectRatio: Float,
    horizontalPadding: Dp,
    topPadding: Dp,
    edgeDepth: Dp,
    bottomBlendDepth: Dp,
) = drawWithCache {
    val artworkWidth = (size.width - (horizontalPadding.toPx() * 2f)).coerceAtLeast(0f)
    val artworkBottom = (topPadding.toPx() + artworkWidth / artworkAspectRatio)
        .coerceAtMost(size.height)
    val edgeSize = edgeDepth.toPx().coerceAtMost(size.width / 2f)
    val bottomBlendSize = bottomBlendDepth.toPx()
    val visibleBottomBlendSize = (size.height - artworkBottom)
        .coerceAtLeast(0f)
        .coerceAtMost(bottomBlendSize)
    val edgeColor = accent.copy(alpha = 0.24f)
    val leftBrush = Brush.horizontalGradient(
        colors = listOf(edgeColor, Color.Transparent),
        startX = 0f,
        endX = edgeSize,
    )
    val rightBrush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, edgeColor),
        startX = size.width - edgeSize,
        endX = size.width,
    )
    val topBrush = Brush.verticalGradient(
        colors = listOf(edgeColor, Color.Transparent),
        startY = 0f,
        endY = edgeSize,
    )
    val bottomBrush = Brush.verticalGradient(
        colors = listOf(edgeColor, Color.Transparent),
        startY = artworkBottom,
        endY = artworkBottom + bottomBlendSize,
    )

    onDrawWithContent {
        drawContent()
        drawRect(leftBrush, topLeft = Offset.Zero, size = Size(edgeSize, artworkBottom))
        drawRect(
            rightBrush,
            topLeft = Offset(size.width - edgeSize, 0f),
            size = Size(edgeSize, artworkBottom),
        )
        drawRect(topBrush, topLeft = Offset.Zero, size = Size(size.width, edgeSize))
        drawRect(
            bottomBrush,
            topLeft = Offset(0f, artworkBottom),
            size = Size(size.width, visibleBottomBlendSize),
        )
    }
}

@Composable
private fun rarityAccent(rarity: CustomizationRarity): Color = when (rarity) {
    CustomizationRarity.UNKNOWN -> AwanTheme.colors.textSecondary
    CustomizationRarity.COMMON -> AwanTheme.colors.zoneBlue
    CustomizationRarity.UNCOMMON -> AwanTheme.colors.zoneGreen
    CustomizationRarity.RARE -> AwanTheme.colors.sky
    CustomizationRarity.EPIC -> AwanTheme.colors.zoneViolet
    CustomizationRarity.LEGENDARY -> AwanTheme.colors.zoneSun
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
        modifier = modifier
            .fillMaxWidth()
            .padding(AwanTheme.spacing.lg),
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
        modifier = modifier
            .fillMaxSize()
            .padding(AwanTheme.spacing.lg),
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
private fun typeLabel(type: CustomizationType): String = stringResource(
    when (type) {
        CustomizationType.FRAME -> R.string.inventory_type_frame
        CustomizationType.SKIN -> R.string.inventory_type_skin
        CustomizationType.THEME -> R.string.inventory_type_theme
        CustomizationType.ICON -> R.string.inventory_type_icon
        CustomizationType.UNKNOWN -> R.string.inventory_type_unknown
    },
)

@Composable
private fun rarityLabel(rarity: CustomizationRarity): String = stringResource(
    when (rarity) {
        CustomizationRarity.COMMON -> R.string.inventory_rarity_common
        CustomizationRarity.UNCOMMON -> R.string.inventory_rarity_uncommon
        CustomizationRarity.RARE -> R.string.inventory_rarity_rare
        CustomizationRarity.EPIC -> R.string.inventory_rarity_epic
        CustomizationRarity.LEGENDARY -> R.string.inventory_rarity_legendary
        CustomizationRarity.UNKNOWN -> R.string.inventory_rarity_unknown
    },
)

@Composable
private fun sortLabel(sort: InventorySort): String = stringResource(
    when (sort) {
        InventorySort.RARITY -> R.string.inventory_sort_rarity
        InventorySort.NEWEST -> R.string.inventory_sort_newest
        InventorySort.NAME -> R.string.inventory_sort_name
    },
)
