package com.awan.app.core.domain.inventory.model

enum class CustomizationRarity(
    val rank: Int,
) {
    UNKNOWN(0),
    COMMON(1),
    UNCOMMON(2),
    RARE(3),
    EPIC(4),
    LEGENDARY(5),
    ;

    companion object {
        private val rarityLabel = Regex("^\\s*rarity\\s*:\\s*(\\w+)\\s*$", RegexOption.IGNORE_CASE)

        fun fromInfo(info: String?): CustomizationRarity =
            rarityLabel.matchEntire(info ?: return UNKNOWN)
                ?.groupValues
                ?.getOrNull(1)
                ?.uppercase()
                ?.let { value -> entries.firstOrNull { it.name == value } }
                ?: UNKNOWN
    }
}
