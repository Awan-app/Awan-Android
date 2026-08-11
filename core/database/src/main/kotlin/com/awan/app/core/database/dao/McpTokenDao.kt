package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.awan.app.core.database.model.McpTokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface McpTokenDao {
    @Query("SELECT * FROM mcp_tokens ORDER BY createdAt DESC")
    fun getMcpTokens(): Flow<List<McpTokenEntity>>

    @Upsert
    suspend fun upsertMcpTokens(tokens: List<McpTokenEntity>)

    @Query("DELETE FROM mcp_tokens WHERE id = :id")
    suspend fun deleteMcpToken(id: String)

    @Query("DELETE FROM mcp_tokens")
    suspend fun clearAll()
}
