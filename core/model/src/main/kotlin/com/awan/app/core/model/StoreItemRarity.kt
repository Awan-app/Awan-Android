package com.awan.app.core.model

enum class StoreItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY;

    companion object {
        fun fromString(value: String?): StoreItemRarity {
            if (value == null) return COMMON
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: COMMON
        }
    }
}
