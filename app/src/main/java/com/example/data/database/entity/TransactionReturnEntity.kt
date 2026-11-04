package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_returns",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val transactionNumber: String,
    val returnNumber: String,
    val returnAmount: Long,
    val reason: String,
    val cashierName: String,
    val returnedItemsSummary: String,  // Detail item yang diretur
    val createdAt: Long = System.currentTimeMillis()
)
