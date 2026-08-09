package com.awan.app.core.domain.inventory.model

enum class CustomizationType {
    FRAME,
    SKIN,
    THEME,
    ICON,
    UNKNOWN,
    ;

    companion object {
        fun fromWireValue(value: String): CustomizationType =
            entries.firstOrNull { it.name == value.trim().uppercase() } ?: UNKNOWN
    }
}
