package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String, // format: "slot_itemKey"
    val slotId: Int,
    val itemKey: String,
    val name: String,
    val isUnlocked: Boolean,
    val level: Int = 1,
    val damage: Float = 10f,
    val upgradeCost: Int = 50
)
