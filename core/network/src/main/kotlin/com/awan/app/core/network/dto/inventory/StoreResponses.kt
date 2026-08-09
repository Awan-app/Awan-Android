package com.awan.app.core.network.dto.inventory

import kotlinx.serialization.Serializable

@Serializable
data class StoreItemResponse(
    val id: String,
    val name: String,
    val description: String = "",
    val image: String? = null,
    val info: String? = null,
    val price: Int = 0,
    val version: String = "",
    val type: String,
)

@Serializable
data class InventoryItemResponse(
    val id: String,
    val item: StoreItemResponse,
    val boughtAt: String,
)

@Serializable
data class EquippedItemResponse(
    val type: String,
    val item: StoreItemResponse,
    val equippedAt: String,
)
