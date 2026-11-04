package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,              // Listrik, Operasional, Transportasi, Kemasan, Gaji, dll
    val amount: Long,
    val paymentMethod: String = "CASH",// "CASH", "NON_CASH"
    val shiftId: Long? = null,
    val cashierName: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
