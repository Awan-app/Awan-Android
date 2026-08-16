package com.awan.app.core.domain.inventory.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomizationRarityTest {

    @Test
    fun `parses labelled rarity case insensitively`() {
        assertEquals(CustomizationRarity.EPIC, CustomizationRarity.fromInfo("Rarity: epic"))
        assertEquals(CustomizationRarity.LEGENDARY, CustomizationRarity.fromInfo(" rarity : LEGENDARY "))
    }

    @Test
    fun `uses unknown for absent or unsupported rarity`() {
        assertEquals(CustomizationRarity.UNKNOWN, CustomizationRarity.fromInfo(null))
        assertEquals(CustomizationRarity.UNKNOWN, CustomizationRarity.fromInfo("Limited edition"))
    }
}
