package com.awan.app.core.network.dto.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The catalog item shape shared by the store, the inventory and a wheel item win. Only the wheel
 * consumes it today — the store and inventory are not built yet — so the client keeps every field
 * the backend sends but maps just what the win animation needs.
 */
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
)
