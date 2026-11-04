package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String,
    val cashierName: String,
    val shiftId: Long? = null,
    val subtotal: Long,
    val discountAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val totalAmount: Long,
    val paymentType: String,             // "CASH" or "NON_CASH"
    val paymentMethodDetails: String = "CASH", // "CASH", "QRIS", "TRANSFER"
    val amountPaid: Long,                // Uang diterima dari pelanggan
    val changeAmount: Long = 0L,         // Uang kembalian
    val paymentReference: String? = null,// Catatan/referensi manual non-tunai
    val status: String = "COMPLETED",    // "COMPLETED", "CANCELLED", "RETURNED_PARTIAL", "RETURNED_FULL"
    val cancellationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
