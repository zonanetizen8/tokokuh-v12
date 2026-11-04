package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long? = null,
    val costPrice: Long = 0L,       // Harga Modal (HPP snapshot)
    val sellingPrice: Long = 0L,    // Harga Jual
    val stock: Int = 0,
    val unit: String = "pcs",       // pcs, kg, pack, botol, cup, porsi, box, dll
    val imageUri: String? = null,
    val isActive: Boolean = true,
    val isUnlimitedStock: Boolean = false,
    val minStockAlert: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
