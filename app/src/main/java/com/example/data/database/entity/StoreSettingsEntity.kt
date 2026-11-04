package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val storeWebsite: String = "",
    val receiptHeaderMessage: String = "Selamat Datang & Selamat Berbelanja",
    val receiptFooterMessage: String = "Barang yang sudah dibeli tidak dapat ditukar kembali tanpa struk",
    val paperWidthMm: Int = 58,
    val printCopies: Int = 1,
    val taxEnabled: Boolean = false,
    val taxName: String = "PPN",
    val taxRatePercent: Double = 11.0,
    val isTaxInclusive: Boolean = false,
    val defaultMinStockAlert: Int = 5,
    val logoPath: String? = null,
    val selectedCashierName: String = "",
    val lastPrinterAddress: String? = null,
    val lastPrinterName: String? = null
)
