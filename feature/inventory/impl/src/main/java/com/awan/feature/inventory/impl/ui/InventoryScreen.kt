package com.awan.feature.inventory.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanRemoteImage
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.inventory.impl.R
import com.awan.feature.inventory.impl.presentation.InventoryAction
import com.awan.feature.inventory.impl.presentation.InventoryItem
import com.awan.feature.inventory.impl.presentation.InventorySort
import com.awan.feature.inventory.impl.presentation.InventoryState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    onBack: () -> Unit,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background),
                )
            },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(InventoryAction.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.isLoading && state.items.isEmpty() -> InventoryLoading(
                        modifier = Modifier.fillMaxSize()
                    )

                    state.items.isEmpty() && !state.isOnline -> InventoryEmpty(
                        title = stringResource(R.string.inventory_offline_title),
                        body = stringResource(R.string.inventory_offline_body),
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.items.isEmpty() && state.error != null -> InventoryError(
                        message = state.error.asString(),
                        onRetry = { onAction(InventoryAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.items.isEmpty() -> InventoryEmpty(
                        title = stringResource(R.string.inventory_empty_title),
                        body = stringResource(R.string.inventory_empty_body),
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> InventoryContent(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (controlsVisible) {
            ModalBottomSheet(onDismissRequest = { controlsVisible = false }) {
                InventoryControlsSheet(state = state, onAction = onAction)
            }
        }

        state.detailsItem?.let { item ->
            val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                sheetState = detailsSheetState,
                onDismissRequest = { onAction(InventoryAction.CloseDetails) },
            ) {
                CustomizationDetailsSheet(
                    item = item,
                    state = state,
                    onEquip = { onAction(InventoryAction.Equip(item.item.id)) },
                )
            }
        }
    }
}

@Composable
private fun InventoryContent(
    state: InventoryState,
    onAction: (InventoryAction) -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(state.unseenItemIds) {
        if (state.unseenItemIds.isNotEmpty()) {
            // Allow the 1500ms acquisition animation (+ stagger) to complete fully
            // before clearing unseenItemIds from the in-memory state.
            delay((state.unseenItemIds.size * 80L + 2000L).milliseconds)
            onAction(InventoryAction.MarkSeen)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm
        ),
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
                AwanText(
                    stringResource(R.string.inventory_offline_equip),
                    style = AwanTheme.styles.metaText
                )
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
            state.sections.forEachIndexed { index, section ->
                if (index > 0) {
                    item(key = "divider-${section.type}", span = { GridItemSpan(maxLineSpan) }) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AwanTheme.spacing.xl, bottom = AwanTheme.spacing.md),
                            color = AwanTheme.colors.line,
                            thickness = 1.dp,
                        )
                    }
                }
                item(key = "header-${section.type}", span = { GridItemSpan(maxLineSpan) }) {
                    AwanText(
                        typeLabel(section.type),
                        style = AwanTheme.styles.headingText,
                        modifier = Modifier.padding(bottom = AwanTheme.spacing.xs),
                    )
                }
                item(key = "default-${section.type}") {
                    DefaultItemCard(
                        type = section.type,
                        isCurrentlyDefault = !state.isTypeEquipped(section.type),
                        isOnline = state.isOnline,
                        isUnequipping = state.unequippingType == section.type,
                        onUnequip = { onAction(InventoryAction.Unequip(section.type)) },
                    )
                }
                items(section.items, key = { it.itemId }) { item ->
                    val isNew = item.itemId in state.unseenItemIds
                    if (isNew) {
                        val unseenIndex =
                            state.unseenItemIds.toList().indexOf(item.itemId).coerceAtLeast(0)
                        NewItemAcquisitionEffect(index = unseenIndex, itemId = item.itemId) {
                            CustomizationCard(
                                item = item,
                                isOnline = state.isOnline,
                                isEquipping = state.equippingItemId == item.itemId,
                                onEquip = { onAction(InventoryAction.Equip(item.itemId)) },
                                onInfo = { onAction(InventoryAction.OpenDetails(item.itemId)) },
                            )
                        }
                    } else {
                        CustomizationCard(
                            item = item,
                            isOnline = state.isOnline,
                            isEquipping = state.equippingItemId == item.itemId,
                            onEquip = { onAction(InventoryAction.Equip(item.itemId)) },
                            onInfo = { onAction(InventoryAction.OpenDetails(item.itemId)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultItemCard(
    type: StoreItemType,
    isCurrentlyDefault: Boolean,
    isOnline: Boolean,
    isUnequipping: Boolean,
    onUnequip: () -> Unit,
) {
    val canUnequip = isOnline && !isCurrentlyDefault && !isUnequipping
    val defaultDescription = stringResource(
        if (isCurrentlyDefault) R.string.inventory_item_equipped_state
        else R.string.inventory_item_not_equipped_state,
    )
    AwanCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory-default-${type.name.lowercase()}")
            .semantics { stateDescription = defaultDescription },
        selected = isCurrentlyDefault,
        onClick = if (canUnequip) onUnequip else null,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(AwanTheme.colors.surface)
                .gridItemArtworkGradient(rarityAccent(CustomizationRarity.UNCOMMON)),
            contentAlignment = Alignment.Center,
        ) {
            AwanMascot(
                expression = MascotExpression.Idle,
                width = 80.dp,
            )
            if (isUnequipping) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
        ) {
            AwanText(
                stringResource(R.string.inventory_default),
                style = AwanTheme.styles.bodyText,
                maxLines = 1,
            )
            AwanText(
                rarityLabel(CustomizationRarity.UNCOMMON),
                style = AwanTheme.styles.metaText.copy(color = rarityAccent(CustomizationRarity.UNCOMMON)),
            )
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
        contentPadding = PaddingValues(
            horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        item {
            AwanText(
                stringResource(R.string.inventory_controls), style = AwanTheme.styles.headingText
            )
        }
        item { InventoryFilters(state = state, onAction = onAction) }
    }
}

@Composable
private fun InventoryFilters(state: InventoryState, onAction: (InventoryAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        // 1. Sort Control (Relocated above rarity)
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            AwanText(
                stringResource(R.string.inventory_sort),
                style = AwanTheme.styles.metaText,
            )
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

        // 2. Rarity Control
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            AwanText(
                stringResource(R.string.inventory_filter_rarity),
                style = AwanTheme.styles.metaText,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                items(CustomizationRarity.entries.size) { index ->
                    val rarity = CustomizationRarity.entries[index]
                    val selected = rarity in state.selectedRarities
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(InventoryAction.ToggleRarity(rarity)) },
                        label = { AwanText(rarityLabel(rarity)) },
                    )
                }
            }
        }

        // 3. Category / Slot Type Control (Relocated from top screen, third list option with same padding/spacing)
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            AwanText(
                stringResource(R.string.inventory_filter_type),
                style = AwanTheme.styles.metaText,
            )
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
        }
    }
}

@Composable
private fun CustomizationCard(
    item: InventoryItem,
    isOnline: Boolean,
    isEquipping: Boolean,
    onEquip: () -> Unit,
    onInfo: () -> Unit,
) {
    val canEquip = isOnline && !item.isEquipped && !isEquipping
    val equippedDescription = stringResource(
        if (item.isEquipped) R.string.inventory_item_equipped_state
        else R.string.inventory_item_not_equipped_state,
    )
    AwanCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = equippedDescription },
        selected = item.isEquipped,
        onClick = if (canEquip) onEquip else null,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            CustomizationArt(
                imageUrl = item.imageUrl,
                rarity = item.rarity,
                modifier = Modifier.fillMaxSize(),
                showRarityGradient = true,
            )
            AwanIconButton(
                onClick = onInfo,
                contentDescription = stringResource(R.string.inventory_view_details, item.name),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
        ) {
            AwanText(item.name, style = AwanTheme.styles.bodyText, maxLines = 1)
            AwanText(
                rarityLabel(item.rarity),
                style = AwanTheme.styles.metaText.copy(color = rarityAccent(item.rarity)),
            )
        }
    }
}

@Composable
internal fun CustomizationDetailsSheet(
    item: OwnedItem,
    state: InventoryState,
    onEquip: () -> Unit,
) {
    val isEquipped = item.item.id in state.equippedItemIds
    val rarity = CustomizationRarity.fromInfo(item.item.info)
    val accent = rarityAccent(rarity)

    // Outermost Box: the gradient is drawn BEHIND everything via bowlGradientBackdrop.
    // The image and text content sit on top and are never tinted by the gradient.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bowlGradientBackdrop(accent = accent)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ── Image section ──────────────────────────────────────────────
            // • Fills all horizontal space (no fillMaxWidth(0.80f), no padding)
            // • No rounded corners (no clip)
            // • Pure white background so the gradient behind it is hidden
            // • Slightly reduced height: aspectRatio(1.75f) ≈ 0.8× of old 1.4f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.75f)
                    .background(Color.White)
                    .testTag("inventory-details-artwork"),
                contentAlignment = Alignment.Center,
            ) {
                if (item.item.image.isBlank()) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent)
                } else {
                    AwanRemoteImage(
                        url = item.item.image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.85f),
                    )
                }
            }

            // ── Text / action content ──────────────────────────────────────
            // Horizontal padding only applies to the text area below the image.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AwanTheme.spacing.md,
                        vertical = AwanTheme.spacing.sm,
                    ),
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AwanText(item.item.name, style = AwanTheme.styles.headingText)
                    Spacer(modifier = Modifier.weight(1f))
                    AwanChip(
                        label = rarityLabel(rarity),
                        tone = rarityChipTone(rarity),
                    )
                }
                AwanText(item.item.description, style = AwanTheme.styles.bodySecondaryText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = AwanTheme.colors.zoneSun,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(AwanTheme.spacing.xxs))
                    AwanText(
                        stringResource(R.string.inventory_details_cost, item.item.price),
                        style = AwanTheme.styles.bodyText,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AwanText(
                        stringResource(R.string.inventory_details_type, typeLabel(item.item.type)),
                        style = AwanTheme.styles.metaText,
                    )
                }
                Spacer(modifier = Modifier.height(AwanTheme.spacing.lg))
                AwanButton(
                    onClick = onEquip,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isOnline && !isEquipped,
                    isLoading = state.equippingItemId == item.item.id,
                    variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary,
                ) {
                    AwanText(
                        stringResource(
                            if (isEquipped) R.string.inventory_already_equipped else R.string.inventory_equip,
                        ),
                    )
                }
            }
        }
    }
}

private fun rarityChipTone(rarity: CustomizationRarity): AwanChipTone = when (rarity) {
    CustomizationRarity.COMMON -> AwanChipTone.Neutral
    CustomizationRarity.UNCOMMON -> AwanChipTone.Neutral
    CustomizationRarity.RARE -> AwanChipTone.Sky
    CustomizationRarity.EPIC -> AwanChipTone.Violet
    CustomizationRarity.LEGENDARY -> AwanChipTone.Tangerine
    CustomizationRarity.UNKNOWN -> AwanChipTone.Neutral
}

@Composable
private fun CustomizationArt(
    imageUrl: String?,
    rarity: CustomizationRarity,
    modifier: Modifier,
    showRarityGradient: Boolean = true,
) {
    val accent = rarityAccent(rarity)
    val gradientModifier = if (showRarityGradient) {
        Modifier.gridItemArtworkGradient(accent)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .background(AwanTheme.colors.surface)
            .then(gradientModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent)
        } else {
            AwanRemoteImage(
                url = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // Slightly smaller than the container so the gradient is visible
                // around the image edges — giving it a floating appearance.
                modifier = Modifier.fillMaxSize(0.85f),
            )
        }
    }
}

private fun Modifier.gridItemArtworkGradient(accent: Color): Modifier = drawWithCache {
    val gradientBrush = Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.40f to Color.Transparent,              // 60% from bottom (40% from top)
        0.80f to accent.copy(alpha = 0.25f),     // 20% breakpoint from bottom
        1.0f to accent.copy(alpha = 0.45f),      // 0% bottom edge (intense)
        startY = 0f,
        endY = size.height,
    )
    // Draw the gradient BEHIND the image content (onDrawBehind) so the image
    // is never tinted or covered by the gradient colors.
    onDrawBehind {
        drawRect(brush = gradientBrush)
    }
}

/**
 * Bottom-to-top edge gradient backdrop for the customization details bottom sheet.
 *
 * Implements a 2D edge mask:
 * - **Horizontal distribution**:
 *   - 0.00f -> full accent intensity (alpha 0.45f)
 *   - 0.10f -> full accent intensity (alpha 0.45f)
 *   - 0.30f -> transparent
 *   - 0.70f -> transparent
 *   - 0.90f -> full accent intensity (alpha 0.45f)
 *   - 1.00f -> full accent intensity (alpha 0.45f)
 * - **Vertical mask**:
 *   - 0.00f -> transparent
 *   - 1.00f -> opaque
 *   Applied using BlendMode.DstIn inside an isolated canvas saveLayer.
 */
private fun Modifier.bowlGradientBackdrop(
    accent: Color,
): Modifier = drawWithCache {
    // Lowering the gradient alpha by 40% to make it significantly softer.
    val maxAlpha = 0.27f 
    val fullAccent = accent.copy(alpha = maxAlpha)
    
    // By using 3 smaller radial points (left, center, right), we can keep the 
    // glow wide across the bottom but severely restrict how high it travels vertically.
    val edgeRadius = size.width * 0.45f // Slightly increased
    val centerRadius = size.width * 0.35f

    // Relaxed fade-out stops so the glow is clearly visible and doesn't disappear too early
    val radialStops = arrayOf(
        0.00f to fullAccent,
        0.40f to fullAccent,
        0.70f to accent.copy(alpha = maxAlpha * 0.5f),
        0.90f to accent.copy(alpha = maxAlpha * 0.15f),
        1.00f to Color.Transparent
    )

    val leftRadial = Brush.radialGradient(
        colorStops = radialStops,
        center = Offset(0f, size.height),
        radius = edgeRadius
    )

    val rightRadial = Brush.radialGradient(
        colorStops = radialStops,
        center = Offset(size.width, size.height),
        radius = edgeRadius
    )

    val centerRadial = Brush.radialGradient(
        colorStops = radialStops,
        center = Offset(size.width * 0.5f, size.height),
        radius = centerRadius
    )

    onDrawBehind {
        drawRect(brush = centerRadial)
        drawRect(brush = leftRadial)
        drawRect(brush = rightRadial)
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
        AwanText(
            stringResource(R.string.inventory_error_title), style = AwanTheme.styles.headingText
        )
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

private const val CardBackdropAlpha = 0.4f
private const val SheetGradientAlpha = 0.30f

