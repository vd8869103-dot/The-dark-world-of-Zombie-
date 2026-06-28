package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class GameRepository(private val gameDao: GameDao) {

    fun getPlayerStats(slotId: Int): Flow<PlayerStats?> = gameDao.getPlayerStats(slotId)

    fun getInventoryItems(slotId: Int): Flow<List<InventoryItem>> = gameDao.getInventoryItems(slotId)

    suspend fun getPlayerStatsSync(slotId: Int): PlayerStats? = gameDao.getPlayerStatsSync(slotId)

    suspend fun getInventoryItemsSync(slotId: Int): List<InventoryItem> = gameDao.getInventoryItemsSync(slotId)

    suspend fun savePlayerStats(stats: PlayerStats) {
        gameDao.insertPlayerStats(stats.copy(lastSaved = System.currentTimeMillis()))
    }

    suspend fun saveInventoryItem(item: InventoryItem) {
        gameDao.insertInventoryItem(item)
    }

    suspend fun initializeNewGame(slotId: Int, name: String) {
        val defaultStats = PlayerStats(
            slotId = slotId,
            playerName = name,
            level = 1,
            xp = 0,
            coins = 100,
            maxHealth = 100f,
            currentHealth = 100f,
            maxStamina = 100f,
            currentStamina = 100f,
            activeWeapon = "sword",
            storyProgress = 1
        )
        gameDao.insertPlayerStats(defaultStats)

        val defaultItems = listOf(
            InventoryItem("${slotId}_sword", slotId, "sword", "Vedic Iron Sword", true, 1, 15f, 50),
            InventoryItem("${slotId}_bow", slotId, "bow", "Gandiva Bow", false, 1, 22f, 100),
            InventoryItem("${slotId}_chakra", slotId, "chakra", "Sudarshana Chakra", false, 1, 35f, 200),
            InventoryItem("${slotId}_damru", slotId, "damru", "Shiva's Damru", false, 1, 45f, 350),
            InventoryItem("${slotId}_trident", slotId, "trident", "Trishula (Trident)", false, 1, 65f, 500),
            InventoryItem("${slotId}_magic", slotId, "magic", "Astra Magic Powers", false, 1, 50f, 400)
        )
        gameDao.insertInventoryItems(defaultItems)
    }

    suspend fun earnCoinsAndXp(slotId: Int, coinsEarned: Int, xpEarned: Int) {
        val current = gameDao.getPlayerStatsSync(slotId) ?: return
        var newXp = current.xp + xpEarned
        var newLevel = current.level
        val xpNeeded = newLevel * 100
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel += 1
        }
        val updated = current.copy(
            level = newLevel,
            xp = newXp,
            coins = current.coins + coinsEarned,
            maxHealth = 100f + (newLevel - 1) * 10f,
            maxStamina = 100f + (newLevel - 1) * 5f,
            currentHealth = 100f + (newLevel - 1) * 10f,
            currentStamina = 100f + (newLevel - 1) * 5f
        )
        gameDao.insertPlayerStats(updated)
    }

    suspend fun buyOrUpgradeWeapon(slotId: Int, itemKey: String): Boolean {
        val currentStats = gameDao.getPlayerStatsSync(slotId) ?: return false
        val item = gameDao.getInventoryItemsSync(slotId).find { it.itemKey == itemKey } ?: return false

        if (!item.isUnlocked) {
            // Unlock weapon
            if (currentStats.coins >= item.upgradeCost) {
                val updatedStats = currentStats.copy(coins = currentStats.coins - item.upgradeCost)
                val updatedItem = item.copy(isUnlocked = true, upgradeCost = (item.upgradeCost * 1.5).toInt())
                gameDao.insertPlayerStats(updatedStats)
                gameDao.insertInventoryItem(updatedItem)
                return true
            }
        } else {
            // Upgrade weapon level
            val upgradePrice = item.upgradeCost
            if (currentStats.coins >= upgradePrice) {
                val updatedStats = currentStats.copy(coins = currentStats.coins - upgradePrice)
                val updatedItem = item.copy(
                    level = item.level + 1,
                    damage = item.damage + 8f,
                    upgradeCost = (upgradePrice * 1.6).toInt()
                )
                gameDao.insertPlayerStats(updatedStats)
                gameDao.insertInventoryItem(updatedItem)
                return true
            }
        }
        return false
    }

    suspend fun resetGame(slotId: Int) {
        gameDao.deletePlayerStats(slotId)
        gameDao.deleteInventoryItems(slotId)
        initializeNewGame(slotId, "Warrior")
    }
}
