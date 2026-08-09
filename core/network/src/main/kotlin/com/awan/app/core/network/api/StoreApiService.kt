package com.awan.app.core.network.api

import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreApiService {

    @GET("v1/store/items")
    suspend fun getStoreItems(
        @Query("type") type: String? = null
    ): List<StoreItemDto>

    @GET("v1/store/inventory")
    suspend fun getInventory(): List<OwnedItemDto>

    @POST("v1/store/items/{itemId}/buy")
    suspend fun buyItem(
        @Path("itemId") itemId: String
    )

    @GET("v1/store/equipped")
    suspend fun getEquippedItems(): List<EquippedItemDto>

    @POST("v1/store/items/{itemId}/equip")
    suspend fun equipItem(
        @Path("itemId") itemId: String
    )

    @DELETE("v1/store/items/{itemId}/equip")
    suspend fun unequipItem(
        @Path("itemId") itemId: String
    )
}
