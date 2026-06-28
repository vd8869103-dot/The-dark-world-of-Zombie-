package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val slotId: Int = 1,
    val playerName: String = "Hero",
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 100,
    val maxHealth: Float = 100f,
    val currentHealth: Float = 100f,
    val maxStamina: Float = 100f,
    val currentStamina: Float = 100f,
    val activeWeapon: String = "sword",
    val storyProgress: Int = 1,
    val lastSaved: Long = System.currentTimeMillis()
)
