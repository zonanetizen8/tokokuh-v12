package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "held_transactions")
data class HeldTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,                 // e.g. "Meja 1", "Pesanan Pak Budi", or timestamp
    val cashierName: String,
    val itemsJson: String,             // JSON serialized cart items
    val discountPercent: Double = 0.0,
    val discountNominal: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
