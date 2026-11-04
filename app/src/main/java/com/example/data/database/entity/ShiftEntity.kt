package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashierName: String,
    val initialCash: Long,             // Modal awal
    val openNote: String? = null,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val expectedCash: Long? = null,    // Kas yang seharusnya di laci
    val actualCash: Long? = null,      // Kas fisik aktual yang dihitung kasir
    val cashDifference: Long? = null,  // Selisih (actual - expected)
    val closeNote: String? = null,
    val status: String = "OPEN",       // "OPEN", "CLOSED"
    val totalCashSales: Long = 0L,
    val totalNonCashSales: Long = 0L,
    val totalExpensesCash: Long = 0L,
    val cashIn: Long = 0L,             // Penambahan kas laci
    val cashOut: Long = 0L             // Pengambilan kas laci
)
