package com.awan.app.core.domain.inventory.model

enum class CustomizationRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    UNKNOWN;

    companion object {
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
