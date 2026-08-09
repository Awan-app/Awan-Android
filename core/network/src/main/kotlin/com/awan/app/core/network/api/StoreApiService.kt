package com.awan.app.core.network.api

import com.awan.app.core.network.dto.inventory.EquippedItemResponse
import com.awan.app.core.network.dto.inventory.InventoryItemResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreApiService {
    @GET("v1/store/inventory")
    suspend fun getInventory(): List<InventoryItemResponse>

    @GET("v1/store/equipped")
    suspend fun getEquipped(): List<EquippedItemResponse>

    @POST("v1/store/items/{itemId}/equip")
    suspend fun equip(@Path("itemId") itemId: String): EquippedItemResponse
}
