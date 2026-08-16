package com.awan.app.core.network.dto.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StoreItemTypeDto {
    @SerialName("FRAME") FRAME,
    @SerialName("SKIN") SKIN,
    @SerialName("THEME") THEME,
    @SerialName("ICON") ICON
}

@Serializable
data class StoreItemDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("info") val info: String? = null,
    @SerialName("price") val price: Int = 0,
    @SerialName("version") val version: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("rarity") val rarity: String? = null,
)

@Serializable
data class OwnedItemDto(
    @SerialName("id") val id: String,
    @SerialName("item") val item: StoreItemDto,
    @SerialName("boughtAt") val boughtAt: String
)

@Serializable
data class EquippedItemDto(
    @SerialName("type") val type: StoreItemTypeDto,
    @SerialName("item") val item: StoreItemDto,
    @SerialName("equippedAt") val equippedAt: String
)
