package com.awan.app.core.data.marketplace

import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
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

        assertEquals("owned-2", entitySeen.id)
        assertTrue(entitySeen.isSeen)
    }
}
