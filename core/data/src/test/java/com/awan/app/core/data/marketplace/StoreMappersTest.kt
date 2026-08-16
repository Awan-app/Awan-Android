package com.awan.app.core.data.marketplace

import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemRarity
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.network.dto.store.StoreItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreMappersTest {

    private val sampleStoreItem = StoreItem(
        id = "item-1",
        name = "Nebula Frame",
        description = "Cosmic avatar frame",
        image = "https://example.com/frame.png",
        info = "Rarity: rare",
        price = 150,
        version = "1.0",
        type = StoreItemType.FRAME
    )

    @Test
    fun `OwnedItemEntity asExternalModel maps isSeen true and false correctly`() {
        val entityUnseen = OwnedItemEntity(
            id = "owned-1",
            itemId = "item-1",
            boughtAt = "2026-08-15T00:00:00Z",
            isSeen = false
        )
        val entitySeen = OwnedItemEntity(
            id = "owned-2",
            itemId = "item-1",
            boughtAt = "2026-08-15T00:00:00Z",
            isSeen = true
        )

        val modelUnseen = entityUnseen.asExternalModel(listOf(sampleStoreItem))
        val modelSeen = entitySeen.asExternalModel(listOf(sampleStoreItem))

        assertNotNull(modelUnseen)
        assertEquals("owned-1", modelUnseen!!.id)
        assertEquals(sampleStoreItem, modelUnseen.item)
        assertFalse(modelUnseen.isSeen)

        assertNotNull(modelSeen)
        assertEquals("owned-2", modelSeen!!.id)
        assertTrue(modelSeen.isSeen)
    }

    @Test
    fun `OwnedItemEntity asExternalModel returns null when store item is missing`() {
        val entity = OwnedItemEntity(
            id = "owned-1",
            itemId = "non-existent-item",
            boughtAt = "2026-08-15T00:00:00Z",
            isSeen = false
        )

        val model = entity.asExternalModel(listOf(sampleStoreItem))
        assertNull(model)
    }

    @Test
    fun `OwnedItem asEntity maps isSeen correctly`() {
        val modelUnseen = OwnedItem(
            id = "owned-1",
            item = sampleStoreItem,
            boughtAt = "2026-08-15T00:00:00Z",
            isSeen = false
        )
        val modelSeen = OwnedItem(
            id = "owned-2",
            item = sampleStoreItem,
            boughtAt = "2026-08-15T00:00:00Z",
            isSeen = true
        )

        val entityUnseen = modelUnseen.asEntity(expiryTime = 12345L)
        val entitySeen = modelSeen.asEntity(expiryTime = 12345L)

        assertEquals("owned-1", entityUnseen.id)
        assertEquals("item-1", entityUnseen.itemId)
        assertEquals(12345L, entityUnseen.expiryTime)
        assertFalse(entityUnseen.isSeen)
    }

    @Test
    fun `asStoreItemRarity maps valid strings case-insensitively and falls back to COMMON for unknown or null`() {
        assertEquals(StoreItemRarity.COMMON, "COMMON".asStoreItemRarity())
        assertEquals(StoreItemRarity.COMMON, "common".asStoreItemRarity())
        assertEquals(StoreItemRarity.UNCOMMON, "UNCOMMON".asStoreItemRarity())
        assertEquals(StoreItemRarity.RARE, "rare".asStoreItemRarity())
        assertEquals(StoreItemRarity.EPIC, "EPIC".asStoreItemRarity())
        assertEquals(StoreItemRarity.LEGENDARY, "Legendary".asStoreItemRarity())

        // Defensive fallbacks
        assertEquals(StoreItemRarity.COMMON, null.asStoreItemRarity())
        assertEquals(StoreItemRarity.COMMON, "".asStoreItemRarity())
        assertEquals(StoreItemRarity.COMMON, "UNKNOWN".asStoreItemRarity())
        assertEquals(StoreItemRarity.COMMON, "MYTHIC".asStoreItemRarity())
    }

    @Test
    fun `StoreItemDto asExternalModel resolves relative image URL and maps rarity`() {
        val dto = StoreItemDto(
            id = "frame-1",
            name = "Twilight Frame",
            description = "A twilight frame",
            image = "/images/store/twilight_light_03.png",
            info = "twilight_03",
            price = 820,
            version = "2.0",
            type = "FRAME",
            rarity = "RARE"
        )

        val model = dto.asExternalModel()
        assertNotNull(model)
        assertEquals("frame-1", model!!.id)
        assertEquals(StoreItemRarity.RARE, model.rarity)
        assertEquals(StoreItemType.FRAME, model.type)
        assertTrue(model.image.endsWith("/images/store/twilight_light_03.png"))
        assertFalse(model.image.contains("/api/images"))
    }

    @Test
    fun `StoreItemEntity to and from StoreItem preserves rarity and image`() {
        val domainItem = sampleStoreItem.copy(rarity = StoreItemRarity.LEGENDARY)
        val entity = domainItem.asEntity(expiryTime = 9999L)

        assertEquals("LEGENDARY", entity.rarity)
        assertEquals(sampleStoreItem.image, entity.image)

        val mappedBack = entity.asExternalModel()
        assertNotNull(mappedBack)
        assertEquals(StoreItemRarity.LEGENDARY, mappedBack!!.rarity)
    }
}
