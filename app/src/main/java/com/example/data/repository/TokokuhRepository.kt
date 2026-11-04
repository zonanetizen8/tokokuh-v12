package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.TokokuhDatabase
import com.example.data.database.entity.AuditLogEntity
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.EmployeeEntity
import com.example.data.database.entity.ExpenseEntity
import com.example.data.database.entity.HeldTransactionEntity
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.ShiftEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import com.example.data.database.entity.TransactionReturnEntity
import com.example.utils.CurrencyUtils
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class CartItem(
    val product: ProductEntity,
    var quantity: Int,
    var discountPerItem: Long = 0L
) {
    val subtotal: Long
        get() = ((product.sellingPrice - discountPerItem) * quantity).coerceAtLeast(0L)
}

class TokokuhRepository(
    private val context: Context,
    private val db: TokokuhDatabase
) {
    // Flows
    val productsFlow: Flow<List<ProductEntity>> = db.productDao().getAllProductsFlow()
    val activeProductsFlow: Flow<List<ProductEntity>> = db.productDao().getActiveProductsFlow()
    val lowStockProductsFlow: Flow<List<ProductEntity>> = db.productDao().getLowStockProductsFlow()
    val categoriesFlow: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategoriesFlow()
    val transactionsFlow: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactionsFlow()
    val activeShiftFlow: Flow<ShiftEntity?> = db.shiftDao().getActiveShiftFlow()
    val allShiftsFlow: Flow<List<ShiftEntity>> = db.shiftDao().getAllShiftsFlow()
    val expensesFlow: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpensesFlow()
    val employeesFlow: Flow<List<EmployeeEntity>> = db.employeeDao().getAllEmployeesFlow()
    val activeEmployeesFlow: Flow<List<EmployeeEntity>> = db.employeeDao().getActiveEmployeesFlow()
    val stockMovementsFlow: Flow<List<StockMovementEntity>> = db.stockMovementDao().getAllMovementsFlow()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogsFlow()
    val heldTransactionsFlow: Flow<List<HeldTransactionEntity>> = db.heldTransactionDao().getAllHeldFlow()
    val storeSettingsFlow: Flow<StoreSettingsEntity?> = db.storeSettingsDao().getSettingsFlow()

    suspend fun getSettings(): StoreSettingsEntity {
        return db.storeSettingsDao().getSettings() ?: StoreSettingsEntity().also {
            db.storeSettingsDao().insertOrUpdateSettings(it)
        }
    }

    suspend fun updateSettings(settings: StoreSettingsEntity) {
        db.storeSettingsDao().insertOrUpdateSettings(settings)
    }

    // Product operations
    suspend fun saveProduct(
        id: Long,
        name: String,
        sku: String,
        barcode: String,
        categoryId: Long?,
        costPrice: Long,
        sellingPrice: Long,
        stock: Int,
        unit: String,
        imageUri: String?,
        isActive: Boolean,
        isUnlimitedStock: Boolean,
        minStockAlert: Int,
        cashierName: String
    ): Long = db.withTransaction {
        if (id == 0L) {
            val entity = ProductEntity(
                name = name,
                sku = sku,
                barcode = barcode,
                categoryId = categoryId,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stock = stock,
                unit = unit,
                imageUri = imageUri,
                isActive = isActive,
                isUnlimitedStock = isUnlimitedStock,
                minStockAlert = minStockAlert
            )
            val newId = db.productDao().insertProduct(entity)
            db.stockMovementDao().insertMovement(
                StockMovementEntity(
                    productId = newId,
                    productName = name,
                    previousStock = 0,
                    changeAmount = stock,
                    currentStock = stock,
                    type = "INITIAL",
                    reason = "Produk baru ditambahkan"
                )
            )
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "TAMBAH_PRODUK",
                    cashierName = cashierName,
                    details = "Menambahkan produk '$name' (Harga Jual: ${CurrencyUtils.formatRupiah(sellingPrice)}, Stok: $stock)"
                )
            )
            newId
        } else {
            val old = db.productDao().getProductById(id)
            val entity = ProductEntity(
                id = id,
                name = name,
                sku = sku,
                barcode = barcode,
                categoryId = categoryId,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stock = stock,
                unit = unit,
                imageUri = imageUri ?: old?.imageUri,
                isActive = isActive,
                isUnlimitedStock = isUnlimitedStock,
                minStockAlert = minStockAlert,
                updatedAt = System.currentTimeMillis()
            )
            db.productDao().updateProduct(entity)

            if (old != null && old.stock != stock) {
                val diff = stock - old.stock
                db.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        productId = id,
                        productName = name,
                        previousStock = old.stock,
                        changeAmount = diff,
                        currentStock = stock,
                        type = if (diff > 0) "ADJUSTMENT_IN" else "ADJUSTMENT_OUT",
                        reason = "Penyesuaian stok manual"
                    )
                )
            }
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "EDIT_PRODUK",
                    cashierName = cashierName,
                    details = "Mengubah produk '$name'"
                )
            )
            id
        }
    }

    suspend fun adjustProductStock(
        productId: Long,
        changeAmount: Int,
        reason: String,
        cashierName: String
    ) = db.withTransaction {
        val prod = db.productDao().getProductById(productId) ?: return@withTransaction
        val newStock = (prod.stock + changeAmount).coerceAtLeast(0)
        db.productDao().updateStock(productId, newStock)
        db.stockMovementDao().insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = prod.name,
                previousStock = prod.stock,
                changeAmount = changeAmount,
                currentStock = newStock,
                type = if (changeAmount >= 0) "ADJUSTMENT_IN" else "ADJUSTMENT_OUT",
                reason = reason
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "PENYESUAIAN_STOK",
                cashierName = cashierName,
                details = "Stok '${prod.name}' diubah dari ${prod.stock} menjadi $newStock ($reason)"
            )
        )
    }

    suspend fun deleteProduct(product: ProductEntity, cashierName: String) = db.withTransaction {
        db.productDao().deleteProduct(product)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "HAPUS_PRODUK",
                cashierName = cashierName,
                details = "Menghapus produk '${product.name}'"
            )
        )
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return db.productDao().getProductByBarcode(barcode)
    }

    // Category operations
    suspend fun saveCategory(id: Long, name: String, iconName: String, colorHex: String?): Long {
        return if (id == 0L) {
            db.categoryDao().insertCategory(CategoryEntity(name = name, iconName = iconName, colorHex = colorHex))
        } else {
            db.categoryDao().updateCategory(CategoryEntity(id = id, name = name, iconName = iconName, colorHex = colorHex))
            id
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        db.categoryDao().deleteCategory(category)
    }

    // POS Checkout Transaction
    suspend fun processCheckout(
        items: List<CartItem>,
        discountOverall: Long,
        taxPercent: Double,
        isTaxInclusive: Boolean,
        taxEnabled: Boolean,
        paymentType: String,
        paymentMethodDetails: String,
        amountPaid: Long,
        paymentReference: String?,
        cashierName: String
    ): Result<TransactionEntity> = db.withTransaction {
        if (items.isEmpty()) {
            return@withTransaction Result.failure(Exception("Keranjang belanja kosong."))
        }

        val rawSubtotal = items.sumOf { it.subtotal }
        val discountedSubtotal = (rawSubtotal - discountOverall).coerceAtLeast(0L)

        // Tax calculation
        val calculatedTax = if (taxEnabled && taxPercent > 0.0) {
            if (isTaxInclusive) {
                // inclusive: tax is inside discountedSubtotal
                (discountedSubtotal - (discountedSubtotal / (1.0 + (taxPercent / 100.0)))).toLong()
            } else {
                // exclusive: tax added on top
                (discountedSubtotal * (taxPercent / 100.0)).toLong()
            }
        } else {
            0L
        }

        val finalTotal = if (isTaxInclusive || !taxEnabled) discountedSubtotal else discountedSubtotal + calculatedTax

        if (paymentType == "CASH" && amountPaid < finalTotal) {
            return@withTransaction Result.failure(
                Exception("Uang pembayaran kurang! Total: ${CurrencyUtils.formatRupiah(finalTotal)}, diterima: ${CurrencyUtils.formatRupiah(amountPaid)}")
            )
        }

        val changeAmount = if (paymentType == "CASH") (amountPaid - finalTotal).coerceAtLeast(0L) else 0L
        val activeShift = db.shiftDao().getActiveShift()
        val txNumber = CurrencyUtils.generateTransactionNumber()

        val transactionEntity = TransactionEntity(
            transactionNumber = txNumber,
            cashierName = cashierName,
            shiftId = activeShift?.id,
            subtotal = rawSubtotal,
            discountAmount = discountOverall,
            taxAmount = calculatedTax,
            totalAmount = finalTotal,
            paymentType = paymentType,
            paymentMethodDetails = paymentMethodDetails,
            amountPaid = amountPaid,
            changeAmount = changeAmount,
            paymentReference = paymentReference,
            status = "COMPLETED"
        )
        val txId = db.transactionDao().insertTransaction(transactionEntity)

        // Save items & deduct stock
        for (cartItem in items) {
            val prod = db.productDao().getProductById(cartItem.product.id) ?: cartItem.product

            val itemEntity = TransactionItemEntity(
                transactionId = txId,
                productId = prod.id,
                productName = prod.name,
                productSku = prod.sku,
                costPriceSnapshot = prod.costPrice,      // Snapshot HPP
                sellingPriceSnapshot = prod.sellingPrice,
                quantity = cartItem.quantity,
                returnedQuantity = 0,
                discountPerItem = cartItem.discountPerItem,
                subtotal = cartItem.subtotal
            )
            db.transactionDao().insertItem(itemEntity)

            // Reduce stock
            if (!prod.isUnlimitedStock) {
                val newStock = (prod.stock - cartItem.quantity)
                db.productDao().updateStock(prod.id, newStock)
                db.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        productId = prod.id,
                        productName = prod.name,
                        previousStock = prod.stock,
                        changeAmount = -cartItem.quantity,
                        currentStock = newStock,
                        type = "SALE",
                        referenceId = txNumber,
                        reason = "Penjualan No. $txNumber"
                    )
                )

                // Trigger low stock notification if threshold crossed
                if (newStock <= prod.minStockAlert) {
                    NotificationHelper.showLowStockNotification(context, prod.name, newStock)
                }
            }
        }

        // Update active shift if present
        if (activeShift != null) {
            val updatedShift = if (paymentType == "CASH") {
                activeShift.copy(totalCashSales = activeShift.totalCashSales + finalTotal)
            } else {
                activeShift.copy(totalNonCashSales = activeShift.totalNonCashSales + finalTotal)
            }
            db.shiftDao().updateShift(updatedShift)
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "TRANSAKSI_BERHASIL",
                cashierName = cashierName,
                details = "Transaksi $txNumber berhasil (${items.size} item, Total: ${CurrencyUtils.formatRupiah(finalTotal)}, Bayar: $paymentType)"
            )
        )

        val savedTx = transactionEntity.copy(id = txId)
        Result.success(savedTx)
    }

    suspend fun getTransactionItems(txId: Long): List<TransactionItemEntity> {
        return db.transactionDao().getItemsByTransactionId(txId)
    }

    // Retur Transaksi
    suspend fun processReturn(
        transactionId: Long,
        returnedItems: Map<Long, Int>, // itemId -> returnQty
        reason: String,
        cashierName: String
    ): Result<String> = db.withTransaction {
        val tx = db.transactionDao().getTransactionById(transactionId)
            ?: return@withTransaction Result.failure(Exception("Transaksi tidak ditemukan."))

        val items = db.transactionDao().getItemsByTransactionId(transactionId)
        var totalReturnAmount = 0L
        val summaryList = mutableListOf<String>()

        for (item in items) {
            val qtyToReturn = returnedItems[item.id] ?: 0
            if (qtyToReturn > 0) {
                val allowedQty = item.quantity - item.returnedQuantity
                val actualReturnQty = qtyToReturn.coerceAtMost(allowedQty)
                if (actualReturnQty > 0) {
                    val unitPrice = item.sellingPriceSnapshot - item.discountPerItem
                    val returnItemAmount = unitPrice * actualReturnQty
                    totalReturnAmount += returnItemAmount

                    // Update item returned quantity
                    db.transactionDao().updateItem(
                        item.copy(returnedQuantity = item.returnedQuantity + actualReturnQty)
                    )

                    // Return stock
                    val prod = db.productDao().getProductById(item.productId)
                    if (prod != null && !prod.isUnlimitedStock) {
                        val restoredStock = prod.stock + actualReturnQty
                        db.productDao().updateStock(prod.id, restoredStock)
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                productId = prod.id,
                                productName = prod.name,
                                previousStock = prod.stock,
                                changeAmount = actualReturnQty,
                                currentStock = restoredStock,
                                type = "SALE_RETURN",
                                referenceId = tx.transactionNumber,
                                reason = "Retur barang ($reason)"
                            )
                        )
                    }
                    summaryList.add("${item.productName} ($actualReturnQty pcs)")
                }
            }
        }

        if (totalReturnAmount <= 0L) {
            return@withTransaction Result.failure(Exception("Tidak ada kuantitas retur yang valid."))
        }

        val returnNumber = CurrencyUtils.generateReturnNumber()
        db.transactionDao().insertReturn(
            TransactionReturnEntity(
                transactionId = tx.id,
                transactionNumber = tx.transactionNumber,
                returnNumber = returnNumber,
                returnAmount = totalReturnAmount,
                reason = reason,
                cashierName = cashierName,
                returnedItemsSummary = summaryList.joinToString(", ")
            )
        )

        // Check if all items fully returned
        val updatedItems = db.transactionDao().getItemsByTransactionId(transactionId)
        val allReturned = updatedItems.all { it.quantity == it.returnedQuantity }
        db.transactionDao().updateTransaction(
            tx.copy(
                status = if (allReturned) "RETURNED_FULL" else "RETURNED_PARTIAL",
                updatedAt = System.currentTimeMillis()
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "RETUR_TRANSAKSI",
                cashierName = cashierName,
                details = "Retur $returnNumber senilai ${CurrencyUtils.formatRupiah(totalReturnAmount)} (${summaryList.joinToString(", ")})"
            )
        )

        Result.success("Retur barang berhasil dicatat ($returnNumber). Nilai pengembalian: ${CurrencyUtils.formatRupiah(totalReturnAmount)}")
    }

    // Pembatalan Transaksi
    suspend fun cancelTransaction(
        transactionId: Long,
        reason: String,
        cashierName: String
    ): Result<String> = db.withTransaction {
        val tx = db.transactionDao().getTransactionById(transactionId)
            ?: return@withTransaction Result.failure(Exception("Transaksi tidak ditemukan."))

        if (tx.status == "CANCELLED") {
            return@withTransaction Result.failure(Exception("Transaksi sudah dibatalkan sebelumnya."))
        }

        val items = db.transactionDao().getItemsByTransactionId(transactionId)
        for (item in items) {
            val netQty = item.quantity - item.returnedQuantity
            if (netQty > 0) {
                val prod = db.productDao().getProductById(item.productId)
                if (prod != null && !prod.isUnlimitedStock) {
                    val restoredStock = prod.stock + netQty
                    db.productDao().updateStock(prod.id, restoredStock)
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = prod.id,
                            productName = prod.name,
                            previousStock = prod.stock,
                            changeAmount = netQty,
                            currentStock = restoredStock,
                            type = "SALE_CANCEL",
                            referenceId = tx.transactionNumber,
                            reason = "Pembatalan transaksi ($reason)"
                        )
                    )
                }
            }
        }

        db.transactionDao().updateTransaction(
            tx.copy(
                status = "CANCELLED",
                cancellationReason = reason,
                updatedAt = System.currentTimeMillis()
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "PEMBATALAN_TRANSAKSI",
                cashierName = cashierName,
                details = "Transaksi ${tx.transactionNumber} dibatalkan karena: $reason. Stok barang telah dikembalikan."
            )
        )

        Result.success("Transaksi ${tx.transactionNumber} berhasil dibatalkan dan stok dikembalikan.")
    }

    // Shift Operations
    suspend fun openShift(
        cashierName: String,
        initialCash: Long,
        note: String?
    ): Result<Long> = db.withTransaction {
        val active = db.shiftDao().getActiveShift()
        if (active != null) {
            return@withTransaction Result.failure(
                Exception("Shift kasir '${active.cashierName}' masih aktif. Harap tutup shift sebelumnya terlebih dahulu.")
            )
        }
        val entity = ShiftEntity(
            cashierName = cashierName,
            initialCash = initialCash,
            openNote = note,
            openedAt = System.currentTimeMillis(),
            status = "OPEN"
        )
        val id = db.shiftDao().insertShift(entity)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "BUKA_SHIFT",
                cashierName = cashierName,
                details = "Membuka shift kasir dengan modal awal ${CurrencyUtils.formatRupiah(initialCash)}"
            )
        )
        Result.success(id)
    }

    suspend fun recordCashMovementInShift(
        shiftId: Long,
        isCashIn: Boolean,
        amount: Long,
        reason: String,
        cashierName: String
    ) = db.withTransaction {
        val shift = db.shiftDao().getShiftById(shiftId) ?: return@withTransaction
        val updated = if (isCashIn) {
            shift.copy(cashIn = shift.cashIn + amount)
        } else {
            shift.copy(cashOut = shift.cashOut + amount)
        }
        db.shiftDao().updateShift(updated)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = if (isCashIn) "TAMBAH_KAS_LACI" else "AMBIL_KAS_LACI",
                cashierName = cashierName,
                details = "${if (isCashIn) "Penambahan" else "Pengambilan"} kas laci ${CurrencyUtils.formatRupiah(amount)} ($reason)"
            )
        )
    }

    suspend fun closeShift(
        shiftId: Long,
        actualCash: Long,
        closeNote: String?,
        cashierName: String
    ): Result<ShiftEntity> = db.withTransaction {
        val shift = db.shiftDao().getShiftById(shiftId)
            ?: return@withTransaction Result.failure(Exception("Shift tidak ditemukan."))

        // Calculate expected cash in drawer
        // Modal awal + Penjualan Tunai - Pengeluaran Tunai + Penambahan Kas - Pengambilan Kas
        val expectedCash = (shift.initialCash + shift.totalCashSales - shift.totalExpensesCash + shift.cashIn - shift.cashOut).coerceAtLeast(0L)
        val difference = actualCash - expectedCash

        val closed = shift.copy(
            closedAt = System.currentTimeMillis(),
            expectedCash = expectedCash,
            actualCash = actualCash,
            cashDifference = difference,
            closeNote = closeNote,
            status = "CLOSED"
        )
        db.shiftDao().updateShift(closed)

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "TUTUP_SHIFT",
                cashierName = cashierName,
                details = "Shift ditutup. Kas seharusnya: ${CurrencyUtils.formatRupiah(expectedCash)}, Aktual: ${CurrencyUtils.formatRupiah(actualCash)}, Selisih: ${CurrencyUtils.formatRupiah(difference)}"
            )
        )

        Result.success(closed)
    }

    // Expenses
    suspend fun addExpense(
        title: String,
        category: String,
        amount: Long,
        paymentMethod: String,
        note: String?,
        cashierName: String
    ): Long = db.withTransaction {
        val activeShift = db.shiftDao().getActiveShift()
        val expense = ExpenseEntity(
            title = title,
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            shiftId = activeShift?.id,
            cashierName = cashierName,
            note = note
        )
        val id = db.expenseDao().insertExpense(expense)

        if (paymentMethod == "CASH" && activeShift != null) {
            db.shiftDao().updateShift(
                activeShift.copy(totalExpensesCash = activeShift.totalExpensesCash + amount)
            )
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "CATAT_PENGELUARAN",
                cashierName = cashierName,
                details = "Pengeluaran '$title' ($category) senilai ${CurrencyUtils.formatRupiah(amount)} ($paymentMethod)"
            )
        )
        id
    }

    suspend fun deleteExpense(expense: ExpenseEntity, cashierName: String) = db.withTransaction {
        db.expenseDao().deleteExpense(expense)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "HAPUS_PENGELUARAN",
                cashierName = cashierName,
                details = "Menghapus pengeluaran '${expense.title}' (${CurrencyUtils.formatRupiah(expense.amount)})"
            )
        )
    }

    // Employees
    suspend fun saveEmployee(id: Long, name: String, role: String, phone: String?): Long {
        return if (id == 0L) {
            db.employeeDao().insertEmployee(EmployeeEntity(name = name, role = role, phone = phone))
        } else {
            val old = db.employeeDao().getEmployeeById(id)
            db.employeeDao().updateEmployee(
                EmployeeEntity(id = id, name = name, role = role, phone = phone, isActive = old?.isActive ?: true)
            )
            id
        }
    }

    suspend fun toggleEmployeeActive(employee: EmployeeEntity) {
        db.employeeDao().updateEmployee(employee.copy(isActive = !employee.isActive))
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        db.employeeDao().deleteEmployee(employee)
    }

    // Held Transactions (Tahan Transaksi)
    suspend fun holdTransaction(
        label: String,
        cashierName: String,
        items: List<CartItem>,
        discountNominal: Long
    ): Long {
        val jsonArray = JSONArray()
        for (item in items) {
            jsonArray.put(JSONObject().apply {
                put("productId", item.product.id)
                put("quantity", item.quantity)
                put("discountPerItem", item.discountPerItem)
            })
        }
        val entity = HeldTransactionEntity(
            label = label.ifBlank { "Pesanan ${CurrencyUtils.formatTime(System.currentTimeMillis())}" },
            cashierName = cashierName,
            itemsJson = jsonArray.toString(),
            discountNominal = discountNominal
        )
        return db.heldTransactionDao().insertHeld(entity)
    }

    suspend fun deleteHeldTransaction(id: Long) {
        db.heldTransactionDao().deleteHeld(id)
    }
}
