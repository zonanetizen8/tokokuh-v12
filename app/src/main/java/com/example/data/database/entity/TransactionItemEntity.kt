package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val productSku: String = "",
    val costPriceSnapshot: Long,      // Snapshot harga modal (HPP) saat transaksi terjadi
    val sellingPriceSnapshot: Long,   // Snapshot harga jual saat transaksi
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val discountPerItem: Long = 0L,
    val subtotal: Long
)
