package com.awan.app.core.domain.inventory.model

import com.awan.app.core.model.StoreItemRarity

enum class CustomizationRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    UNKNOWN;

    companion object {
        fun fromStoreItemRarity(rarity: StoreItemRarity): CustomizationRarity = when (rarity) {
            StoreItemRarity.COMMON -> COMMON
            StoreItemRarity.UNCOMMON -> UNCOMMON
            StoreItemRarity.RARE -> RARE
            StoreItemRarity.EPIC -> EPIC
            StoreItemRarity.LEGENDARY -> LEGENDARY
        }

        fun fromInfo(info: String?): CustomizationRarity {
            if (info == null) return UNKNOWN
            val lower = info.lowercase()
            return when {
                lower.contains("legendary") -> LEGENDARY
                lower.contains("epic") -> EPIC
                lower.contains("rare") -> RARE
                lower.contains("uncommon") -> UNCOMMON
                lower.contains("common") -> COMMON
                else -> UNKNOWN
            }
        }
    }
}
