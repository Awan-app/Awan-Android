# Environmental Inventory Screen Enhancements — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance the existing inventory screen with new-item animations, an elliptical radial gradient bottom sheet, rarity chip badges, cost display, default-item unequip support, and "Pick"/"Already Equipped" button labels — after merging the latest develop branch.

**Architecture:** The develop branch restructured inventory around a unified Store system (`StoreRepository`, `StoreDao`, `StoreItem`/`OwnedItem`/`EquippedItem`). This plan merges develop into the feature branch, adapts the enhanced grid/sheet UI to use develop's data layer, then adds the new features. All changes respect Clean Architecture layering (`presentation → domain ← data`).

**Tech Stack:** Kotlin 2.4.0, Jetpack Compose (BOM 2026.06.01), Room (destructive fallback, version 1), Hilt DI, Navigation 3, Material 3, Coil3.

## Global Constraints

- Database version stays at **1** with destructive fallback — no migrations.
- ViewModels consume **only use cases** from `:core:domain` — never repositories, DAOs, or data sources.
- All visible strings in English (`values/strings.xml`) **and** Arabic (`values-ar/strings.xml`).
- String key format: `inventory_<string_name>`.
- Feature module `api/impl` split: `:feature:inventory:api` exports only `InventoryRoute`.
- Domain models in `:core:model`. Use cases and repository contracts in `:core:domain`. Implementations in `:core:data`.
- Commit messages prefixed with the Jira issue ID (to be confirmed, likely `AWAN-137`).

---

### Task 0: Merge develop into the feature branch and resolve conflicts

**Files:**
- Modify: all conflicting files from `git merge origin/develop`

**Interfaces:**
- Consumes: `origin/develop` branch state
- Produces: clean working tree with develop's data/domain/network layer + our branch's UI code adapted to compile against it

**Merge strategy:**
- Take develop's entire data/domain/network stack (the `StoreRepository`/`StoreDao`/`StoreItem` system).
- Adapt our branch's enhanced `InventoryScreen.kt` to use develop's models (`OwnedItem`, `StoreItem`, `StoreItemType`) instead of our old models (`OwnedCustomization`, `CustomizationType`, `CustomizationRarity`).
- Keep our branch's enhanced UI structure (grid + details sheet + edge shadows + filters + controls bottom sheet).
- Preserve `CustomizationRarity` from `:core:domain/inventory/model/` as it exists on develop for rarity parsing.

- [ ] **Step 1: Merge origin/develop and identify conflicts**

```bash
git merge origin/develop --no-edit
```

Expected: Conflict in `InventoryScreen.kt` (and possibly `InventoryPreviews.kt`).

- [ ] **Step 2: Resolve InventoryScreen.kt**

Take our branch's enhanced UI but update all type references:
- `OwnedCustomization` → `OwnedItem` (with nested `StoreItem` access patterns)
- `CustomizationType` → `StoreItemType`
- `CustomizationRarity` → keep using `CustomizationRarity.fromInfo(item.info)`
- `state.customizations` → `state.items` + `state.equippedItemIds`
- `customization.isEquipped` → `item.id in equippedItemIds`
- `customization.imageUrl` → `item.image`
- `customization.name` → `item.name`
- `customization.description` → `item.description`
- `customization.rarity` → `CustomizationRarity.fromInfo(item.info)`
- `customization.type` → `item.type`

- [ ] **Step 3: Resolve InventoryState.kt**

Merge the two: keep develop's `InventoryState` structure (using `OwnedItem`, `equippedItemIds: Set<String>`, `StoreItemType`) but add back our rarity filtering, rarity-aware sorting, and `InventorySection` grouping. Add the `ToggleRarity` action back to `InventoryAction.kt`.

- [ ] **Step 4: Resolve InventoryViewModel.kt**

Take develop's ViewModel structure (using `GetInventoryUseCase`, `GetEquippedItemsUseCase`, `RefreshMarketplaceUseCase`, `EquipItemUseCase`) but add back our rarity filtering action handling.

- [ ] **Step 5: Resolve InventoryPreviews.kt**

Update previews to use develop's `OwnedItem`/`StoreItem` constructors.

- [ ] **Step 6: Resolve InventoryViewModelTest.kt**

Update tests to use develop's `FakeStoreRepository` and `StoreItem`/`OwnedItem` models. Add back rarity filter tests.

- [ ] **Step 7: Verify merge compiles**

```bash
./gradlew :feature:inventory:impl:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Run tests**

```bash
./gradlew :feature:inventory:impl:testDebugUnitTest
```

Expected: All tests pass.

- [ ] **Step 9: Commit merge**

```bash
git add -A
git commit -m "merge: resolve develop merge into inventory feature branch"
```

---

### Task 1: Add `isSeen` column to `OwnedItemEntity` and mark-seen DAO method

**Files:**
- Modify: `core/database/src/main/kotlin/com/awan/app/core/database/model/OwnedItemEntity.kt`
- Modify: `core/database/src/main/kotlin/com/awan/app/core/database/dao/StoreDao.kt`

**Interfaces:**
- Consumes: existing `OwnedItemEntity`, `StoreDao`
- Produces: `OwnedItemEntity.isSeen: Boolean`, `StoreDao.markAllSeen()`, `StoreDao.observeUnseenCount(): Flow<Int>`

- [ ] **Step 1: Add `isSeen` column to `OwnedItemEntity`**

```kotlin
// core/database/src/main/kotlin/com/awan/app/core/database/model/OwnedItemEntity.kt
@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val boughtAt: String,
    val expiryTime: Long = 0L,
    val isSeen: Boolean = false,
)
```

- [ ] **Step 2: Add DAO methods to `StoreDao`**

```kotlin
// Add to StoreDao.kt
@Query("UPDATE owned_items SET isSeen = 1 WHERE isSeen = 0")
suspend fun markAllOwnedItemsSeen()

@Query("SELECT COUNT(*) FROM owned_items WHERE isSeen = 0")
fun observeUnseenOwnedCount(): Flow<Int>
```

- [ ] **Step 3: Update `replaceOwnedItems` to preserve seen state**

The current `replaceOwnedItems` deletes all and re-inserts. To preserve `isSeen`, we need to mark existing items as seen before replacing. Actually, we want NEW items to be `isSeen = false`. The approach: when refreshing inventory, new items that weren't previously in the DB should get `isSeen = false`, existing items should keep their `isSeen` state.

Add a new transaction to `StoreDao`:

```kotlin
@Transaction
suspend fun replaceOwnedItemsPreservingSeen(items: List<OwnedItemEntity>) {
    val existingIds = getOwnedItemIds()
    deleteAllOwnedItems()
    val preserved = items.map { item ->
        if (item.id in existingIds) item.copy(isSeen = true) else item
    }
    upsertOwnedItems(preserved)
}

@Query("SELECT id FROM owned_items")
suspend fun getOwnedItemIds(): List<String>
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :core:database:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/database/
git commit -m "AWAN-137: add isSeen column to OwnedItemEntity and mark-seen DAO methods"
```

---

### Task 2: Add `isSeen` to domain model and create new use cases

**Files:**
- Modify: `core/model/src/main/kotlin/com/awan/app/core/model/StoreItem.kt` (add `isSeen` to `OwnedItem`)
- Create: `core/domain/src/main/kotlin/com/awan/app/core/domain/marketplace/usecase/MarkInventorySeenUseCase.kt`
- Create: `core/domain/src/main/kotlin/com/awan/app/core/domain/marketplace/usecase/UnequipItemUseCase.kt`
- Modify: `core/domain/src/main/kotlin/com/awan/app/core/domain/marketplace/repository/StoreRepository.kt` (add `markInventorySeen()` and update `unequipItem` if needed)

**Interfaces:**
- Consumes: `StoreRepository`, `OwnedItem`
- Produces: `MarkInventorySeenUseCase`, `UnequipItemUseCase`, `OwnedItem.isSeen: Boolean`

- [ ] **Step 1: Add `isSeen` to `OwnedItem` domain model**

```kotlin
// core/model/src/main/kotlin/com/awan/app/core/model/StoreItem.kt
data class OwnedItem(
    val id: String,
    val item: StoreItem,
    val boughtAt: String,
    val isSeen: Boolean = true,
)
```

- [ ] **Step 2: Add `markInventorySeen()` to `StoreRepository` contract**

```kotlin
// In StoreRepository.kt, add:
suspend fun markInventorySeen()
```

- [ ] **Step 3: Create `MarkInventorySeenUseCase`**

```kotlin
package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.domain.marketplace.repository.StoreRepository
import javax.inject.Inject

class MarkInventorySeenUseCase @Inject constructor(
    private val repository: StoreRepository,
) {
    suspend operator fun invoke() = repository.markInventorySeen()
}
```

- [ ] **Step 4: Create `UnequipItemUseCase`** (if it doesn't already exist on develop — check first)

```kotlin
package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.StoreItemType
import javax.inject.Inject

class UnequipItemUseCase @Inject constructor(
    private val repository: StoreRepository,
) {
    suspend operator fun invoke(type: StoreItemType): Result<Unit> = repository.unequipItem(type)
}
```

- [ ] **Step 5: Verify compilation**

```bash
./gradlew :core:domain:compileDebugKotlin :core:model:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add core/model/ core/domain/
git commit -m "AWAN-137: add isSeen to OwnedItem, MarkInventorySeenUseCase, UnequipItemUseCase"
```

---

### Task 3: Wire `isSeen` and `markInventorySeen()` in the data layer

**Files:**
- Modify: `core/data/src/main/kotlin/com/awan/app/core/data/marketplace/mapper/StoreMappers.kt`
- Modify: `core/data/src/main/kotlin/com/awan/app/core/data/marketplace/repository/StoreRepositoryImpl.kt`

**Interfaces:**
- Consumes: `StoreDao.markAllOwnedItemsSeen()`, `StoreDao.replaceOwnedItemsPreservingSeen()`, `OwnedItemEntity.isSeen`
- Produces: `StoreRepositoryImpl.markInventorySeen()`, `OwnedItem.isSeen` mapped from entity

- [ ] **Step 1: Update mapper to carry `isSeen` through**

```kotlin
// In StoreMappers.kt, update OwnedItemEntity.asExternalModel:
fun OwnedItemEntity.asExternalModel(items: List<StoreItem>): OwnedItem? {
    val storeItem = items.find { it.id == itemId } ?: return null
    return OwnedItem(
        id = id,
        item = storeItem,
        boughtAt = boughtAt,
        isSeen = isSeen,
    )
}
```

- [ ] **Step 2: Implement `markInventorySeen()` in `StoreRepositoryImpl`**

```kotlin
override suspend fun markInventorySeen() = withContext(ioDispatcher) {
    storeDao.markAllOwnedItemsSeen()
}
```

- [ ] **Step 3: Update `refreshInventory()` to use `replaceOwnedItemsPreservingSeen`**

Replace `storeDao.replaceOwnedItems(entities)` with `storeDao.replaceOwnedItemsPreservingSeen(entities)` so new items get `isSeen = false` and existing items keep `isSeen = true`.

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :core:data:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/data/
git commit -m "AWAN-137: wire isSeen through data layer and use seen-preserving refresh"
```

---

### Task 4: Update `InventoryState`, `InventoryAction`, and `InventoryViewModel` for new features

**Files:**
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/presentation/InventoryState.kt`
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/presentation/InventoryAction.kt`
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/presentation/InventoryViewModel.kt`

**Interfaces:**
- Consumes: `MarkInventorySeenUseCase`, `UnequipItemUseCase`, `GetEquippedItemsUseCase`, `OwnedItem.isSeen`
- Produces: `InventoryState` with `unseenItemIds: Set<String>`, `InventoryAction.Unequip(type)`, `InventoryAction.MarkSeen`, `InventoryAction.OpenDetails(itemId)`, `InventoryAction.CloseDetails`

- [ ] **Step 1: Add new actions to `InventoryAction.kt`**

```kotlin
sealed interface InventoryAction {
    data object Refresh : InventoryAction
    data class SelectType(val type: StoreItemType?) : InventoryAction
    data class ToggleRarity(val rarity: CustomizationRarity) : InventoryAction
    data class SetSort(val sort: InventorySort) : InventoryAction
    data class Equip(val itemId: String) : InventoryAction
    data class Unequip(val type: StoreItemType) : InventoryAction
    data class OpenDetails(val itemId: String) : InventoryAction
    data object CloseDetails : InventoryAction
    data object MarkSeen : InventoryAction
}
```

- [ ] **Step 2: Add new state fields to `InventoryState.kt`**

```kotlin
data class InventoryState(
    val items: List<OwnedItem> = emptyList(),
    val equippedItemIds: Set<String> = emptySet(),
    val selectedType: StoreItemType? = null,
    val selectedRarities: Set<CustomizationRarity> = emptySet(),
    val sort: InventorySort = InventorySort.RARITY,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val equippingItemId: String? = null,
    val unequippingType: StoreItemType? = null,
    val detailsItemId: String? = null,
    val unseenItemIds: Set<String> = emptySet(),
    val error: UiText? = null,
) {
    val sections: List<InventorySection>
        get() = inventorySections(items, equippedItemIds, selectedType, selectedRarities, sort)

    val detailsItem: OwnedItem?
        get() = items.firstOrNull { it.item.id == detailsItemId }
}
```

Add `RARITY` back to `InventorySort`:

```kotlin
enum class InventorySort {
    RARITY,
    NEWEST,
    NAME,
}
```

Update `inventorySections` to accept rarity filter and sort by rarity:

```kotlin
internal fun inventorySections(
    items: List<OwnedItem>,
    equippedItemIds: Set<String>,
    selectedType: StoreItemType?,
    selectedRarities: Set<CustomizationRarity>,
    sort: InventorySort,
): List<InventorySection> = items
    .asSequence()
    .filter { selectedType == null || it.item.type == selectedType }
    .filter {
        selectedRarities.isEmpty() ||
            CustomizationRarity.fromInfo(it.item.info) in selectedRarities
    }
    .map { ownedItem ->
        InventoryItem(
            itemId = ownedItem.item.id,
            name = ownedItem.item.name,
            description = ownedItem.item.description,
            imageUrl = ownedItem.item.image,
            type = ownedItem.item.type,
            rarity = CustomizationRarity.fromInfo(ownedItem.item.info),
            price = ownedItem.item.price,
            isEquipped = ownedItem.item.id in equippedItemIds,
            isSeen = ownedItem.isSeen,
            acquiredAt = ownedItem.boughtAt,
        )
    }
    .groupBy { it.type }
    .toSortedMap(compareBy { it.ordinal })
    .map { (type, items) -> InventorySection(type, items.sortedFor(sort)) }
```

Update `InventoryItem` to include all needed fields:

```kotlin
data class InventoryItem(
    val itemId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val type: StoreItemType,
    val rarity: CustomizationRarity,
    val price: Int,
    val isEquipped: Boolean,
    val isSeen: Boolean,
    val acquiredAt: String,
)
```

- [ ] **Step 3: Update `InventoryViewModel` with new use cases and action handling**

```kotlin
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventory: GetInventoryUseCase,
    private val getEquippedItems: GetEquippedItemsUseCase,
    private val refreshMarketplace: RefreshMarketplaceUseCase,
    private val equipItem: EquipItemUseCase,
    private val unequipItem: UnequipItemUseCase,
    private val markInventorySeen: MarkInventorySeenUseCase,
    private val observeConnectivity: ObserveNetworkConnectivityUseCase,
) : ViewModel() {
    // ... existing init block ...

    fun onAction(action: InventoryAction) {
        when (action) {
            // ... existing actions ...
            is InventoryAction.Unequip -> unequip(action.type)
            is InventoryAction.OpenDetails -> _state.update { it.copy(detailsItemId = action.itemId) }
            InventoryAction.CloseDetails -> _state.update { it.copy(detailsItemId = null) }
            InventoryAction.MarkSeen -> markSeen()
        }
    }

    private fun unequip(type: StoreItemType) {
        if (!_state.value.isOnline || _state.value.unequippingType != null) return
        viewModelScope.launch {
            _state.update { it.copy(unequippingType = type, error = null) }
            when (val result = unequipItem(type)) {
                is Result.Error -> _state.update { it.copy(unequippingType = null, error = result.error.toUiText()) }
                is Result.Success -> {
                    _state.update { it.copy(unequippingType = null) }
                    refreshMarketplace()
                }
                Result.Loading -> _state.update { it.copy(unequippingType = null) }
            }
        }
    }

    private fun markSeen() {
        viewModelScope.launch {
            markInventorySeen()
            _state.update { it.copy(unseenItemIds = emptySet()) }
        }
    }
}
```

Also update `init` to track unseen item IDs:

```kotlin
init {
    viewModelScope.launch {
        getInventory().collect { items ->
            _state.update {
                it.copy(
                    items = items,
                    isLoading = false,
                    unseenItemIds = items.filter { item -> !item.isSeen }.map { item -> item.item.id }.toSet(),
                )
            }
        }
    }
    // ... rest of init ...
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :feature:inventory:impl:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/presentation/
git commit -m "AWAN-137: add unequip, mark-seen, rarity filter, and details state to inventory presentation"
```

---

### Task 5: Enhance `InventoryScreen.kt` with new-item animation, default card, and updated grid

**Files:**
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/ui/InventoryScreen.kt`

**Interfaces:**
- Consumes: `InventoryState.unseenItemIds`, `InventoryAction.Unequip`, `InventoryAction.MarkSeen`, `InventoryAction.OpenDetails`, `CascadeItem`, `SparkleBurst`
- Produces: Enhanced grid with new-item animations, default item cards per section, details bottom sheet management

- [ ] **Step 1: Add new-item animation to grid cards**

Use `CascadeItem` for staggered entrance of unseen items, with `SparkleBurst` overlay for extra flair. After animation completes (delay based on item count × stagger), dispatch `MarkSeen`.

```kotlin
// In InventoryContent, wrap unseen items with CascadeItem:
items(section.items, key = { it.itemId }) { item ->
    val isNew = item.itemId in state.unseenItemIds
    CascadeItem(index = /* item index */, visible = true) {
        CustomizationCard(
            item = item,
            isNew = isNew,
            // ... rest of params
        )
    }
}

// LaunchedEffect to mark seen after animation settles:
LaunchedEffect(state.unseenItemIds) {
    if (state.unseenItemIds.isNotEmpty()) {
        delay(state.unseenItemIds.size * 55L + 500L) // stagger + settle
        onAction(InventoryAction.MarkSeen)
    }
}
```

- [ ] **Step 2: Add Default card at the start of each type section**

```kotlin
// Before each section's items, add a Default card:
item(key = "default-${section.type}") {
    DefaultItemCard(
        type = section.type,
        isCurrentlyDefault = section.items.none { it.isEquipped },
        isOnline = state.isOnline,
        isUnequipping = state.unequippingType == section.type,
        onUnequip = { onAction(InventoryAction.Unequip(section.type)) },
    )
}
```

```kotlin
@Composable
private fun DefaultItemCard(
    type: StoreItemType,
    isCurrentlyDefault: Boolean,
    isOnline: Boolean,
    isUnequipping: Boolean,
    onUnequip: () -> Unit,
) {
    val canUnequip = isOnline && !isCurrentlyDefault && !isUnequipping
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        selected = isCurrentlyDefault,
        onClick = if (canUnequip) onUnequip else null,
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            AwanMascot(expression = MascotExpression.Idle, width = 48.dp)
            if (isUnequipping) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.Center), strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(AwanTheme.spacing.xs))
        AwanText(stringResource(R.string.inventory_default), style = AwanTheme.styles.bodyText, maxLines = 1)
        AwanText(
            stringResource(if (isCurrentlyDefault) R.string.inventory_item_equipped_state else R.string.inventory_item_not_equipped_state),
            style = AwanTheme.styles.metaText,
        )
    }
}
```

- [ ] **Step 3: Update `CustomizationCard` to show new-item glow**

Add a subtle animated glow/pulse border for unseen items using a `SparkleBurst` or animated border alpha.

- [ ] **Step 4: Move details sheet state management to ViewModel**

Replace the local `detailsItemId` state with ViewModel-driven `state.detailsItemId`:

```kotlin
// Remove: var detailsItemId by rememberSaveable { mutableStateOf<String?>(null) }
// Use: state.detailsItem from ViewModel

state.detailsItem?.let { ownedItem ->
    ModalBottomSheet(onDismissRequest = { onAction(InventoryAction.CloseDetails) }) {
        CustomizationDetailsSheet(
            item = ownedItem,
            state = state,
            onEquip = { onAction(InventoryAction.Equip(ownedItem.item.id)) },
        )
    }
}
```

- [ ] **Step 5: Verify compilation**

```bash
./gradlew :feature:inventory:impl:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/ui/
git commit -m "AWAN-137: add new-item animation, default cards, and ViewModel-driven details sheet"
```

---

### Task 6: Redesign `CustomizationDetailsSheet` with elliptical gradient, rarity chip, cost, and Pick button

**Files:**
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/ui/InventoryScreen.kt` (the `CustomizationDetailsSheet` composable)

**Interfaces:**
- Consumes: `InventoryItem.rarity`, `InventoryItem.price`, `AwanChip`, `AwanChipTone`, rarity accent colors
- Produces: Redesigned details sheet with elliptical radial gradient, rarity chip badge, cost display, "Pick"/"Already Equipped" button

- [ ] **Step 1: Replace `detailsSheetEdgeShadow` with elliptical radial gradient**

Remove the existing `detailsSheetEdgeShadow` modifier. Replace with a `drawWithCache` modifier that draws an elliptical radial gradient:

```kotlin
private fun Modifier.ellipticalRarityGradient(
    accent: Color,
    alpha: Float = 0.30f,
) = drawWithCache {
    // Ellipse: wider than tall, anchored at bottom-center of the composable
    // The gradient extends higher on the sides than in the center
    val center = Offset(size.width / 2f, size.height)
    val horizontalRadius = size.width * 0.9f
    val verticalRadius = size.height * 0.6f

    val brush = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
        center = center,
        radius = maxOf(horizontalRadius, verticalRadius),
    )

    onDrawBehind {
        // Scale to create elliptical shape: wider horizontally
        val scaleX = horizontalRadius / verticalRadius
        drawContext.canvas.save()
        drawContext.canvas.scale(scaleX, 1f, center.x, center.y)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = verticalRadius,
            ),
        )
        drawContext.canvas.restore()
    }
}
```

- [ ] **Step 2: Add rarity chip badge**

Map rarity to `AwanChipTone`:

```kotlin
@Composable
private fun rarityChipTone(rarity: CustomizationRarity): AwanChipTone = when (rarity) {
    CustomizationRarity.COMMON -> AwanChipTone.Sky
    CustomizationRarity.UNCOMMON -> AwanChipTone.Neutral
    CustomizationRarity.RARE -> AwanChipTone.Sky
    CustomizationRarity.EPIC -> AwanChipTone.Violet
    CustomizationRarity.LEGENDARY -> AwanChipTone.Tangerine
    CustomizationRarity.UNKNOWN -> AwanChipTone.Neutral
}
```

Add the chip in the details sheet layout:

```kotlin
AwanChip(
    label = rarityLabel(rarity),
    tone = rarityChipTone(rarity),
)
```

- [ ] **Step 3: Add cost display**

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
        Icons.Default.Payments, // or a coin icon
        contentDescription = null,
        tint = AwanTheme.colors.zoneSun,
        modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(AwanTheme.spacing.xxs))
    AwanText(
        stringResource(R.string.inventory_details_cost, item.price),
        style = AwanTheme.styles.bodyText,
    )
}
```

- [ ] **Step 4: Update button labels to "Pick" / "Already Equipped"**

```kotlin
AwanButton(
    onClick = onEquip,
    modifier = Modifier.fillMaxWidth(),
    enabled = state.isOnline && !isEquipped,
    isLoading = state.equippingItemId == item.itemId,
    variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary,
) {
    AwanText(
        stringResource(
            if (isEquipped) R.string.inventory_already_equipped
            else R.string.inventory_pick,
        ),
    )
}
```

- [ ] **Step 5: Assemble the updated details sheet**

```kotlin
@Composable
internal fun CustomizationDetailsSheet(
    item: OwnedItem,
    state: InventoryState,
    onEquip: () -> Unit,
) {
    val rarity = CustomizationRarity.fromInfo(item.item.info)
    val accent = rarityAccent(rarity)
    val isEquipped = item.item.id in state.equippedItemIds

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Elliptical gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .ellipticalRarityGradient(accent = accent),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(AwanTheme.shapes.card),
                contentAlignment = Alignment.Center,
            ) {
                CustomizationArt(item = item, modifier = Modifier.fillMaxSize())
            }
            // Title
            AwanText(item.item.name, style = AwanTheme.styles.headingText)
            // Rarity chip badge
            AwanChip(label = rarityLabel(rarity), tone = rarityChipTone(rarity))
            // Description
            AwanText(item.item.description, style = AwanTheme.styles.bodySecondaryText)
            // Cost
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = AwanTheme.colors.zoneSun, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(AwanTheme.spacing.xxs))
                AwanText(stringResource(R.string.inventory_details_cost, item.item.price), style = AwanTheme.styles.bodyText)
            }
            // Pick / Already Equipped button
            AwanButton(
                onClick = onEquip,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !isEquipped,
                isLoading = state.equippingItemId == item.item.id,
                variant = if (isEquipped) AwanButtonVariant.Secondary else AwanButtonVariant.Primary,
            ) {
                AwanText(stringResource(if (isEquipped) R.string.inventory_already_equipped else R.string.inventory_pick))
            }
        }
    }
}
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :feature:inventory:impl:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add feature/inventory/impl/
git commit -m "AWAN-137: redesign details sheet with elliptical gradient, rarity chip, cost, and Pick button"
```

---

### Task 7: Add new string resources (English + Arabic)

**Files:**
- Modify: `feature/inventory/impl/src/main/res/values/strings.xml`
- Modify: `feature/inventory/impl/src/main/res/values-ar/strings.xml`

**Interfaces:**
- Consumes: n/a
- Produces: localized strings for default item, pick, already equipped, cost, rarity sort

- [ ] **Step 1: Add English strings**

```xml
<string name="inventory_default">Default</string>
<string name="inventory_pick">Pick</string>
<string name="inventory_already_equipped">Already Equipped</string>
<string name="inventory_details_cost">%1$d points</string>
<string name="inventory_sort_rarity">Rarity</string>
```

- [ ] **Step 2: Add Arabic strings**

```xml
<string name="inventory_default">الافتراضي</string>
<string name="inventory_pick">اختيار</string>
<string name="inventory_already_equipped">مجهّز بالفعل</string>
<string name="inventory_details_cost">%1$d نقاط</string>
<string name="inventory_sort_rarity">الندرة</string>
```

- [ ] **Step 3: Commit**

```bash
git add feature/inventory/impl/src/main/res/
git commit -m "AWAN-137: add English and Arabic strings for inventory enhancements"
```

---

### Task 8: Update previews and tests

**Files:**
- Modify: `feature/inventory/impl/src/main/java/com/awan/feature/inventory/impl/ui/InventoryPreviews.kt`
- Modify: `feature/inventory/impl/src/test/java/com/awan/feature/inventory/impl/presentation/InventoryViewModelTest.kt`

**Interfaces:**
- Consumes: all updated models and state
- Produces: updated previews showing new features, updated ViewModel tests covering unequip, mark-seen, rarity filtering

- [ ] **Step 1: Update previews**

Add previews for:
- Grid with unseen items (new-item glow)
- Default card in section
- Details sheet with rarity chip, cost, "Already Equipped"
- Details sheet with "Pick" enabled

- [ ] **Step 2: Update ViewModel tests**

Add tests for:
- `MarkSeen` action clears `unseenItemIds`
- `Unequip` action calls use case and refreshes
- Rarity filtering works with `ToggleRarity`
- `RARITY` sort orders correctly
- `OpenDetails` / `CloseDetails` manage `detailsItemId`

- [ ] **Step 3: Run tests**

```bash
./gradlew :feature:inventory:impl:testDebugUnitTest
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add feature/inventory/impl/
git commit -m "AWAN-137: update inventory previews and ViewModel tests for enhanced features"
```

---

### Task 9: Final build verification

**Files:**
- n/a (verification only)

- [ ] **Step 1: Run full build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run lint**

```bash
./gradlew :feature:inventory:impl:lintDebug
```

Expected: No new errors.

- [ ] **Step 3: Run all inventory tests**

```bash
./gradlew :feature:inventory:impl:testDebugUnitTest
```

Expected: All tests pass.

## Verification Plan

### Automated Tests
- `./gradlew :feature:inventory:impl:testDebugUnitTest` — ViewModel tests for all new actions
- `./gradlew :feature:inventory:impl:lintDebug` — lint check
- `./gradlew assembleDebug` — full build

### Manual Verification
- Confirm grid shows Default card as first item in each type section
- Confirm tapping Default card unequips the current item
- Confirm new items animate with cascade + glow on screen open
- Confirm details sheet shows elliptical radial gradient, rarity chip, cost, and "Pick"/"Already Equipped"
- Confirm "Pick" button equips item instantly; "Already Equipped" is dimmed/disabled
- Confirm filter and sorting controls remain functional
