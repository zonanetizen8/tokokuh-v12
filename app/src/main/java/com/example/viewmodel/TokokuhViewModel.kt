package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.CartItem
import com.example.data.repository.TokokuhRepository
import com.example.utils.BackupRestoreManager
import com.example.utils.EscPosPrinterHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

class TokokuhViewModel(
    private val context: Context,
    private val repository: TokokuhRepository
) : ViewModel() {

    // Repository Flows
    val products = repository.productsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val activeProducts = repository.activeProductsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val lowStockProducts = repository.lowStockProductsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repository.categoriesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val transactions = repository.transactionsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val activeShift = repository.activeShiftFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val allShifts = repository.allShiftsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = repository.expensesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val employees = repository.employeesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val activeEmployees = repository.activeEmployeesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val stockMovements = repository.stockMovementsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val auditLogs = repository.auditLogsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val heldTransactions = repository.heldTransactionsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val storeSettings = repository.storeSettingsFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // POS Search & Filter State
    val searchQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Long?>(null)

    // Filtered Products for POS
    val filteredPosProducts = combine(activeProducts, searchQuery, selectedCategoryId) { list, query, catId ->
        list.filter { product ->
            val matchCategory = catId == null || product.categoryId == catId
            val matchQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true) ||
                    product.sku.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _overallDiscount = MutableStateFlow(0L)
    val overallDiscount: StateFlow<Long> = _overallDiscount.asStateFlow()

    // Calculated Cart Totals
    val cartSubtotal: StateFlow<Long> = _cartItems.combine(_overallDiscount) { items, _ ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    // Checkout & Receipt state
    private val _isProcessingCheckout = MutableStateFlow(false)
    val isProcessingCheckout: StateFlow<Boolean> = _isProcessingCheckout.asStateFlow()

    private val _lastTransaction = MutableStateFlow<TransactionEntity?>(null)
    val lastTransaction: StateFlow<TransactionEntity?> = _lastTransaction.asStateFlow()

    private val _lastTransactionItems = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val lastTransactionItems: StateFlow<List<TransactionItemEntity>> = _lastTransactionItems.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _printerStatus = MutableStateFlow<String?>(null)
    val printerStatus: StateFlow<String?> = _printerStatus.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Cart Management
    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            // check stock limitation
            if (!product.isUnlimitedStock && item.quantity >= product.stock) {
                _userMessage.value = "Stok '${product.name}' tidak mencukupi (Tersisa: ${product.stock})"
                return
            }
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            if (!product.isUnlimitedStock && product.stock <= 0) {
                _userMessage.value = "Stok '${product.name}' habis!"
                return
            }
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
    }

    fun incrementCartQuantity(productId: Long) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (!item.product.isUnlimitedStock && item.quantity >= item.product.stock) {
                _userMessage.value = "Stok '${item.product.name}' tidak mencukupi (Tersisa: ${item.product.stock})"
                return
            }
            current[index] = item.copy(quantity = item.quantity + 1)
            _cartItems.value = current
        }
    }

    fun decrementCartQuantity(productId: Long) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun setItemDiscount(productId: Long, discountPerItem: Long) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(discountPerItem = discountPerItem.coerceAtLeast(0L))
            _cartItems.value = current
        }
    }

    fun setOverallDiscount(discount: Long) {
        _overallDiscount.value = discount.coerceAtLeast(0L)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _overallDiscount.value = 0L
    }

    // Barcode Scanner Action
    fun onBarcodeScanned(barcode: String, onNotFound: (String) -> Unit) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product != null) {
                addToCart(product)
                _userMessage.value = "Ditambahkan: ${product.name}"
            } else {
                onNotFound(barcode.trim())
            }
        }
    }

    // Hold & Resume Transactions
    fun holdCurrentTransaction(label: String) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _userMessage.value = "Keranjang kosong, tidak bisa menahan transaksi."
            return
        }
        viewModelScope.launch {
            val currentCashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.holdTransaction(
                label = label,
                cashierName = currentCashier,
                items = items,
                discountNominal = _overallDiscount.value
            )
            clearCart()
            _userMessage.value = "Transaksi berhasil ditahan ('$label')."
        }
    }

    fun resumeHeldTransaction(held: HeldTransactionEntity) {
        viewModelScope.launch {
            try {
                val array = JSONArray(held.itemsJson)
                val resumedItems = mutableListOf<CartItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val prodId = obj.getLong("productId")
                    val qty = obj.getInt("quantity")
                    val disc = obj.optLong("discountPerItem", 0L)
                    val prod = products.value.find { it.id == prodId }
                    if (prod != null) {
                        resumedItems.add(CartItem(product = prod, quantity = qty, discountPerItem = disc))
                    }
                }
                if (resumedItems.isNotEmpty()) {
                    _cartItems.value = resumedItems
                    _overallDiscount.value = held.discountNominal
                    repository.deleteHeldTransaction(held.id)
                    _userMessage.value = "Melanjutkan transaksi '${held.label}'"
                } else {
                    _userMessage.value = "Produk pada transaksi ini tidak lagi ditemukan."
                }
            } catch (e: Exception) {
                _userMessage.value = "Gagal memuat transaksi ditahan."
            }
        }
    }

    fun deleteHeldTransaction(heldId: Long) {
        viewModelScope.launch {
            repository.deleteHeldTransaction(heldId)
            _userMessage.value = "Transaksi yang ditahan telah dihapus."
        }
    }

    // Checkout
    fun processCheckout(
        paymentType: String,
        paymentMethodDetails: String,
        amountPaid: Long,
        paymentReference: String?,
        onSuccess: (TransactionEntity) -> Unit
    ) {
        if (_isProcessingCheckout.value) return // Prevent double click submission
        _isProcessingCheckout.value = true

        viewModelScope.launch {
            val settings = storeSettings.value ?: repository.getSettings()
            val cashier = settings.selectedCashierName

            val result = repository.processCheckout(
                items = _cartItems.value,
                discountOverall = _overallDiscount.value,
                taxPercent = settings.taxRatePercent,
                isTaxInclusive = settings.isTaxInclusive,
                taxEnabled = settings.taxEnabled,
                paymentType = paymentType,
                paymentMethodDetails = paymentMethodDetails,
                amountPaid = amountPaid,
                paymentReference = paymentReference,
                cashierName = cashier
            )

            result.fold(
                onSuccess = { tx ->
                    _lastTransaction.value = tx
                    _lastTransactionItems.value = repository.getTransactionItems(tx.id)
                    clearCart()
                    _isProcessingCheckout.value = false
                    _userMessage.value = "Transaksi ${tx.transactionNumber} Berhasil!"
                    onSuccess(tx)
                },
                onFailure = { error ->
                    _isProcessingCheckout.value = false
                    _userMessage.value = error.localizedMessage ?: "Gagal memproses transaksi."
                }
            )
        }
    }

    // Print Receipt via Bluetooth
    fun printTransactionReceipt(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        targetAddress: String? = null
    ) {
        val printerAddress = targetAddress ?: storeSettings.value?.lastPrinterAddress
        if (printerAddress.isNullOrEmpty()) {
            _printerStatus.value = "Printer Bluetooth belum dipilih. Silakan atur printer di menu Pengaturan Printer."
            return
        }

        viewModelScope.launch {
            _isPrinting.value = true
            _printerStatus.value = "Mengirim struk ke printer Bluetooth..."
            val settings = storeSettings.value ?: repository.getSettings()
            val bytes = EscPosPrinterHelper.buildReceiptBytes(context, settings, transaction, items)
            val result = EscPosPrinterHelper.printToBluetoothDevice(printerAddress, bytes)
            _isPrinting.value = false
            result.fold(
                onSuccess = {
                    _printerStatus.value = "Struk berhasil dicetak!"
                },
                onFailure = { error ->
                    _printerStatus.value = "Gagal cetak: ${error.localizedMessage}"
                }
            )
        }
    }

    fun testPrint(targetAddress: String) {
        viewModelScope.launch {
            _isPrinting.value = true
            _printerStatus.value = "Mengirim tes cetak..."
            val settings = storeSettings.value ?: repository.getSettings()
            val bytes = EscPosPrinterHelper.buildTestPrintBytes(context, settings)
            val result = EscPosPrinterHelper.printToBluetoothDevice(targetAddress, bytes)
            _isPrinting.value = false
            result.fold(
                onSuccess = { _printerStatus.value = "Tes cetak berhasil!" },
                onFailure = { error -> _printerStatus.value = error.localizedMessage }
            )
        }
    }

    // Product CRUD
    fun saveProduct(
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.saveProduct(
                id = id,
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
                minStockAlert = minStockAlert,
                cashierName = cashier
            )
            _userMessage.value = "Produk '$name' berhasil disimpan."
            onSuccess()
        }
    }

    fun adjustStock(productId: Long, changeAmount: Int, reason: String) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.adjustProductStock(productId, changeAmount, reason, cashier)
            _userMessage.value = "Stok produk berhasil diperbarui."
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.deleteProduct(product, cashier)
            _userMessage.value = "Produk '${product.name}' telah dihapus."
        }
    }

    // Category CRUD
    fun saveCategory(id: Long, name: String, iconName: String, colorHex: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveCategory(id, name, iconName, colorHex)
            _userMessage.value = "Kategori '$name' berhasil disimpan."
            onSuccess()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.value = "Kategori '${category.name}' telah dihapus."
        }
    }

    // Shift Operations
    fun openShift(initialCash: Long, note: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            val res = repository.openShift(cashier, initialCash, note)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Shift kasir '$cashier' berhasil dibuka."
                    onSuccess()
                },
                onFailure = { error ->
                    _userMessage.value = error.localizedMessage
                }
            )
        }
    }

    fun recordCashMovement(shiftId: Long, isCashIn: Boolean, amount: Long, reason: String) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.recordCashMovementInShift(shiftId, isCashIn, amount, reason, cashier)
            _userMessage.value = "${if (isCashIn) "Kas Masuk" else "Kas Keluar"} berhasil dicatat."
        }
    }

    fun closeShift(shiftId: Long, actualCash: Long, note: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            val res = repository.closeShift(shiftId, actualCash, note, cashier)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Shift kasir berhasil ditutup."
                    onSuccess()
                },
                onFailure = { error ->
                    _userMessage.value = error.localizedMessage
                }
            )
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Long, paymentMethod: String, note: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.addExpense(title, category, amount, paymentMethod, note, cashier)
            _userMessage.value = "Pengeluaran '$title' berhasil dicatat."
            onSuccess()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            repository.deleteExpense(expense, cashier)
            _userMessage.value = "Pengeluaran telah dihapus."
        }
    }

    // Returns & Cancellations
    fun processReturn(
        transactionId: Long,
        returnedItems: Map<Long, Int>,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            val res = repository.processReturn(transactionId, returnedItems, reason, cashier)
            res.fold(
                onSuccess = { msg ->
                    _userMessage.value = msg
                    onSuccess()
                },
                onFailure = { error ->
                    _userMessage.value = error.localizedMessage
                }
            )
        }
    }

    fun cancelTransaction(transactionId: Long, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cashier = storeSettings.value?.selectedCashierName ?: "Kasir"
            val res = repository.cancelTransaction(transactionId, reason, cashier)
            res.fold(
                onSuccess = { msg ->
                    _userMessage.value = msg
                    onSuccess()
                },
                onFailure = { error ->
                    _userMessage.value = error.localizedMessage
                }
            )
        }
    }

    // Employee Management
    fun saveEmployee(id: Long, name: String, role: String, phone: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveEmployee(id, name, role, phone)
            _userMessage.value = "Karyawan '$name' berhasil disimpan."
            onSuccess()
        }
    }

    fun toggleEmployeeActive(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.toggleEmployeeActive(employee)
            _userMessage.value = "Status '${employee.name}' diperbarui."
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            _userMessage.value = "Karyawan '${employee.name}' telah dihapus."
        }
    }

    fun selectActiveCashier(name: String) {
        viewModelScope.launch {
            val current = storeSettings.value ?: repository.getSettings()
            repository.updateSettings(current.copy(selectedCashierName = name))
            _userMessage.value = "Kasir aktif diubah ke '$name'."
        }
    }

    // Store & Receipt Settings
    fun updateSettings(settings: StoreSettingsEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            _userMessage.value = "Pengaturan berhasil disimpan."
            onSuccess()
        }
    }

    // Backup & Restore
    fun backupDataToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _userMessage.value = "Gagal membuka file tujuan backup."
                    return@launch
                }
                val res = BackupRestoreManager.createJsonBackup(context, (context.applicationContext as com.example.TokokuhApp).database, outputStream)
                res.fold(
                    onSuccess = { _userMessage.value = it },
                    onFailure = { _userMessage.value = it.localizedMessage }
                )
            } catch (e: Exception) {
                _userMessage.value = "Error backup: ${e.localizedMessage}"
            }
        }
    }

    fun restoreDataFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _userMessage.value = "Gagal membuka file backup."
                    return@launch
                }
                val res = BackupRestoreManager.restoreFromJson(context, (context.applicationContext as com.example.TokokuhApp).database, inputStream)
                res.fold(
                    onSuccess = { _userMessage.value = it },
                    onFailure = { _userMessage.value = it.localizedMessage }
                )
            } catch (e: Exception) {
                _userMessage.value = "Error restore: ${e.localizedMessage}"
            }
        }
    }

    fun exportCsvReport(uri: Uri, start: Long, end: Long) {
        viewModelScope.launch {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _userMessage.value = "Gagal membuka file tujuan ekspor CSV."
                    return@launch
                }
                val res = BackupRestoreManager.exportFinancialReportCsv((context.applicationContext as com.example.TokokuhApp).database, start, end, outputStream)
                res.fold(
                    onSuccess = { _userMessage.value = it },
                    onFailure = { _userMessage.value = it.localizedMessage }
                )
            } catch (e: Exception) {
                _userMessage.value = "Error ekspor CSV: ${e.localizedMessage}"
            }
        }
    }

    suspend fun getTransactionItemsForDetail(txId: Long): List<TransactionItemEntity> {
        return repository.getTransactionItems(txId)
    }
}

class TokokuhViewModelFactory(
    private val context: Context,
    private val repository: TokokuhRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TokokuhViewModel(context, repository) as T
    }
}
