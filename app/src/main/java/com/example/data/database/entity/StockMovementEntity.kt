package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val previousStock: Int,
    val changeAmount: Int,
    val currentStock: Int,
    val type: String,                   // SALE, SALE_RETURN, SALE_CANCEL, ADJUSTMENT_IN, ADJUSTMENT_OUT, INITIAL
    val referenceId: String? = null,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
