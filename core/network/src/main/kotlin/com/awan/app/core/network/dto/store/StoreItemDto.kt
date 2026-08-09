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
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("image") val image: String,
    @SerialName("info") val info: String? = null,
    @SerialName("price") val price: Int,
    @SerialName("version") val version: String,
    @SerialName("type") val type: StoreItemTypeDto
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
