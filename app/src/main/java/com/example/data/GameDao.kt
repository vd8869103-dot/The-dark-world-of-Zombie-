package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_stats WHERE slotId = :slotId")
    fun getPlayerStats(slotId: Int): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE slotId = :slotId")
    suspend fun getPlayerStatsSync(slotId: Int): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStats)

    @Query("SELECT * FROM inventory_items WHERE slotId = :slotId")
    fun getInventoryItems(slotId: Int): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE slotId = :slotId")
    suspend fun getInventoryItemsSync(slotId: Int): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Query("DELETE FROM player_stats WHERE slotId = :slotId")
    suspend fun deletePlayerStats(slotId: Int)

    @Query("DELETE FROM inventory_items WHERE slotId = :slotId")
    suspend fun deleteInventoryItems(slotId: Int)
}
