package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object BackupRestoreManager {

    /**
     * Creates a complete JSON backup and writes to SAF output stream
     */
    suspend fun createJsonBackup(
        context: Context,
        database: TokokuhDatabase,
        outputStream: OutputStream
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app_name", "TOKOKUH")
            root.put("backup_version", 1)
            root.put("schema_version", 1)
            root.put("backup_created_at", System.currentTimeMillis())

            val meta = JSONObject()
            meta.put("developer", "Logis Denis Prabowo")
            meta.put("role", "Prompt Developer")
            meta.put("website", "https://www.tokokuh.com")
            meta.put("location", "Indramayu, Jawa Barat")
            root.put("application_metadata", meta)

            // Settings
            val settings = database.storeSettingsDao().getSettings() ?: StoreSettingsEntity()
            val settingsObj = JSONObject().apply {
                put("storeName", settings.storeName)
                put("storeAddress", settings.storeAddress)
                put("storePhone", settings.storePhone)
                put("storeWebsite", settings.storeWebsite)
                put("receiptHeaderMessage", settings.receiptHeaderMessage)
                put("receiptFooterMessage", settings.receiptFooterMessage)
                put("paperWidthMm", settings.paperWidthMm)
                put("printCopies", settings.printCopies)
                put("taxEnabled", settings.taxEnabled)
                put("taxName", settings.taxName)
                put("taxRatePercent", settings.taxRatePercent)
                put("isTaxInclusive", settings.isTaxInclusive)
                put("defaultMinStockAlert", settings.defaultMinStockAlert)
                put("selectedCashierName", settings.selectedCashierName)

                if (!settings.logoPath.isNullOrEmpty()) {
                    try {
                        val logoFile = File(settings.logoPath)
                        if (logoFile.exists()) {
                            put("logoBase64", Base64.encodeToString(logoFile.readBytes(), Base64.NO_WRAP))
                        }
                    } catch (_: Exception) { }
                }
            }
            root.put("settings", settingsObj)

            // Categories
            val categories = database.categoryDao().getAllCategories()
            val categoriesArray = JSONArray()
            for (c in categories) {
                categoriesArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("iconName", c.iconName)
                    put("colorHex", c.colorHex ?: "")
                })
            }
            root.put("categories", categoriesArray)

            // Products + Base64 images
            val products = database.productDao().getAllProducts()
            val productsArray = JSONArray()
            for (p in products) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("barcode", p.barcode)
                    put("categoryId", p.categoryId ?: -1L)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("stock", p.stock)
                    put("unit", p.unit)
                    put("isActive", p.isActive)
                    put("isUnlimitedStock", p.isUnlimitedStock)
                    put("minStockAlert", p.minStockAlert)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)

                    // Image Base64
                    if (!p.imageUri.isNullOrEmpty()) {
                        try {
                            val imgFile = File(p.imageUri)
                            if (imgFile.exists()) {
                                val bytes = imgFile.readBytes()
                                put("imageBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                            }
                        } catch (_: Exception) { }
                    }
                }
                productsArray.put(pObj)
            }
            root.put("products", productsArray)

            // Employees
            val employees = database.employeeDao().getAllEmployees()
            val employeesArray = JSONArray()
            for (e in employees) {
                employeesArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("role", e.role)
                    put("phone", e.phone ?: "")
                    put("isActive", e.isActive)
                    put("createdAt", e.createdAt)
                })
            }
            root.put("employees", employeesArray)

            // Transactions
            val transactions = database.transactionDao().getAllTransactions()
            val transactionsArray = JSONArray()
            for (t in transactions) {
                transactionsArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("transactionNumber", t.transactionNumber)
                    put("cashierName", t.cashierName)
                    put("shiftId", t.shiftId ?: -1L)
                    put("subtotal", t.subtotal)
                    put("discountAmount", t.discountAmount)
                    put("taxAmount", t.taxAmount)
                    put("totalAmount", t.totalAmount)
                    put("paymentType", t.paymentType)
                    put("paymentMethodDetails", t.paymentMethodDetails)
                    put("amountPaid", t.amountPaid)
                    put("changeAmount", t.changeAmount)
                    put("paymentReference", t.paymentReference ?: "")
                    put("status", t.status)
                    put("cancellationReason", t.cancellationReason ?: "")
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
            root.put("transactions", transactionsArray)

            // Transaction Items
            val items = database.transactionDao().getAllTransactionItems()
            val itemsArray = JSONArray()
            for (i in items) {
                itemsArray.put(JSONObject().apply {
                    put("id", i.id)
                    put("transactionId", i.transactionId)
                    put("productId", i.productId)
                    put("productName", i.productName)
                    put("productSku", i.productSku)
                    put("costPriceSnapshot", i.costPriceSnapshot)
                    put("sellingPriceSnapshot", i.sellingPriceSnapshot)
                    put("quantity", i.quantity)
                    put("returnedQuantity", i.returnedQuantity)
                    put("discountPerItem", i.discountPerItem)
                    put("subtotal", i.subtotal)
                })
            }
            root.put("transaction_items", itemsArray)

            // Returns
            val returns = database.transactionDao().getAllReturns()
            val returnsArray = JSONArray()
            for (r in returns) {
                returnsArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("transactionId", r.transactionId)
                    put("transactionNumber", r.transactionNumber)
                    put("returnNumber", r.returnNumber)
                    put("returnAmount", r.returnAmount)
                    put("reason", r.reason)
                    put("cashierName", r.cashierName)
                    put("returnedItemsSummary", r.returnedItemsSummary)
                    put("createdAt", r.createdAt)
                })
            }
            root.put("transaction_returns", returnsArray)

            // Shifts
            val shifts = database.shiftDao().getAllShifts()
            val shiftsArray = JSONArray()
            for (s in shifts) {
                shiftsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("cashierName", s.cashierName)
                    put("initialCash", s.initialCash)
                    put("openNote", s.openNote ?: "")
                    put("openedAt", s.openedAt)
                    put("closedAt", s.closedAt ?: -1L)
                    put("expectedCash", s.expectedCash ?: -1L)
                    put("actualCash", s.actualCash ?: -1L)
                    put("cashDifference", s.cashDifference ?: 0L)
                    put("closeNote", s.closeNote ?: "")
                    put("status", s.status)
                    put("totalCashSales", s.totalCashSales)
                    put("totalNonCashSales", s.totalNonCashSales)
                    put("totalExpensesCash", s.totalExpensesCash)
                    put("cashIn", s.cashIn)
                    put("cashOut", s.cashOut)
                })
            }
            root.put("shifts", shiftsArray)

            // Expenses
            val expenses = database.expenseDao().getAllExpenses()
            val expensesArray = JSONArray()
            for (exp in expenses) {
                expensesArray.put(JSONObject().apply {
                    put("id", exp.id)
                    put("title", exp.title)
                    put("category", exp.category)
                    put("amount", exp.amount)
                    put("paymentMethod", exp.paymentMethod)
                    put("shiftId", exp.shiftId ?: -1L)
                    put("cashierName", exp.cashierName)
                    put("note", exp.note ?: "")
                    put("createdAt", exp.createdAt)
                })
            }
            root.put("expenses", expensesArray)

            // Stock movements
            val movements = database.stockMovementDao().getAllMovements()
            val movementsArray = JSONArray()
            for (m in movements) {
                movementsArray.put(JSONObject().apply {
                    put("id", m.id)
                    put("productId", m.productId)
                    put("productName", m.productName)
                    put("previousStock", m.previousStock)
                    put("changeAmount", m.changeAmount)
                    put("currentStock", m.currentStock)
                    put("type", m.type)
                    put("referenceId", m.referenceId ?: "")
                    put("reason", m.reason ?: "")
                    put("createdAt", m.createdAt)
                })
            }
            root.put("stock_movements", movementsArray)

            // Held transactions
            val heldList = database.heldTransactionDao().getAllHeld()
            val heldArray = JSONArray()
            for (h in heldList) {
                heldArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("label", h.label)
                    put("cashierName", h.cashierName)
                    put("itemsJson", h.itemsJson)
                    put("discountPercent", h.discountPercent)
                    put("discountNominal", h.discountNominal)
                    put("createdAt", h.createdAt)
                })
            }
            root.put("held_transactions", heldArray)

            // Audit logs
            val logs = database.auditLogDao().getAllLogs()
            val logsArray = JSONArray()
            for (l in logs) {
                logsArray.put(JSONObject().apply {
                    put("id", l.id)
                    put("action", l.action)
                    put("cashierName", l.cashierName ?: "")
                    put("details", l.details)
                    put("createdAt", l.createdAt)
                })
            }
            root.put("audit_logs", logsArray)

            // Write JSON
            outputStream.use { stream ->
                stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
                stream.flush()
            }

            // Log audit
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    action = "BACKUP_DATA",
                    cashierName = settings.selectedCashierName,
                    details = "Backup data JSON berhasil dibuat (${products.size} produk, ${transactions.size} transaksi)"
                )
            )

            Result.success("Backup berhasil disimpan (${products.size} produk, ${transactions.size} transaksi)")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat backup: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    /**
     * Restores data from JSON backup input stream inside a database transaction
     */
    suspend fun restoreFromJson(
        context: Context,
        database: TokokuhDatabase,
        inputStream: InputStream
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)

            if (!root.has("app_name") || root.getString("app_name") != "TOKOKUH") {
                return@withContext Result.failure(Exception("File backup tidak valid atau bukan dari aplikasi TOKOKUH."))
            }

            // Parse settings
            var restoredSettings: StoreSettingsEntity? = null
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                var restoredLogoPath: String? = null
                if (sObj.has("logoBase64")) {
                    try {
                        val b64 = sObj.getString("logoBase64")
                        val bytes = Base64.decode(b64, Base64.NO_WRAP)
                        val dir = File(context.filesDir, "store_logo").apply { mkdirs() }
                        val destFile = File(dir, "store_logo_${System.currentTimeMillis()}.png")
                        FileOutputStream(destFile).use { fos -> fos.write(bytes) }
                        restoredLogoPath = destFile.absolutePath
                    } catch (_: Exception) { }
                }

                restoredSettings = StoreSettingsEntity(
                    id = 1,
                    storeName = sObj.optString("storeName", "TOKOKUH STORE"),
                    storeAddress = sObj.optString("storeAddress", ""),
                    storePhone = sObj.optString("storePhone", ""),
                    storeWebsite = sObj.optString("storeWebsite", "www.tokokuh.com"),
                    receiptHeaderMessage = sObj.optString("receiptHeaderMessage", ""),
                    receiptFooterMessage = sObj.optString("receiptFooterMessage", ""),
                    paperWidthMm = sObj.optInt("paperWidthMm", 58),
                    printCopies = sObj.optInt("printCopies", 1),
                    taxEnabled = sObj.optBoolean("taxEnabled", false),
                    taxName = sObj.optString("taxName", "PPN"),
                    taxRatePercent = sObj.optDouble("taxRatePercent", 11.0),
                    isTaxInclusive = sObj.optBoolean("isTaxInclusive", false),
                    defaultMinStockAlert = sObj.optInt("defaultMinStockAlert", 5),
                    selectedCashierName = sObj.optString("selectedCashierName", "Kasir Utama"),
                    logoPath = restoredLogoPath
                )
            }

            // Parse Categories
            val categoryList = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    categoryList.add(
                        CategoryEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "category"),
                            colorHex = obj.optString("colorHex", null)
                        )
                    )
                }
            }

            // Parse Products & restore Base64 images
            val imageDir = File(context.filesDir, "product_images").apply { mkdirs() }
            val productList = mutableListOf<ProductEntity>()
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    var imagePath: String? = null
                    if (obj.has("imageBase64")) {
                        try {
                            val b64 = obj.getString("imageBase64")
                            val bytes = Base64.decode(b64, Base64.NO_WRAP)
                            val destFile = File(imageDir, "prod_${obj.optLong("id")}_${System.currentTimeMillis()}.png")
                            FileOutputStream(destFile).use { fos -> fos.write(bytes) }
                            imagePath = destFile.absolutePath
                        } catch (_: Exception) { }
                    }

                    val catId = obj.optLong("categoryId", -1L)
                    productList.add(
                        ProductEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            sku = obj.optString("sku", ""),
                            barcode = obj.optString("barcode", ""),
                            categoryId = if (catId > 0) catId else null,
                            costPrice = obj.optLong("costPrice", 0L),
                            sellingPrice = obj.optLong("sellingPrice", 0L),
                            stock = obj.optInt("stock", 0),
                            unit = obj.optString("unit", "pcs"),
                            imageUri = imagePath,
                            isActive = obj.optBoolean("isActive", true),
                            isUnlimitedStock = obj.optBoolean("isUnlimitedStock", false),
                            minStockAlert = obj.optInt("minStockAlert", 5),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Employees
            val employeeList = mutableListOf<EmployeeEntity>()
            if (root.has("employees")) {
                val array = root.getJSONArray("employees")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    employeeList.add(
                        EmployeeEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            role = obj.optString("role", "Kasir"),
                            phone = obj.optString("phone", null),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Transactions
            val transactionList = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val shiftId = obj.optLong("shiftId", -1L)
                    transactionList.add(
                        TransactionEntity(
                            id = obj.optLong("id", 0),
                            transactionNumber = obj.getString("transactionNumber"),
                            cashierName = obj.optString("cashierName", "Kasir"),
                            shiftId = if (shiftId > 0) shiftId else null,
                            subtotal = obj.optLong("subtotal", 0L),
                            discountAmount = obj.optLong("discountAmount", 0L),
                            taxAmount = obj.optLong("taxAmount", 0L),
                            totalAmount = obj.optLong("totalAmount", 0L),
                            paymentType = obj.optString("paymentType", "CASH"),
                            paymentMethodDetails = obj.optString("paymentMethodDetails", "CASH"),
                            amountPaid = obj.optLong("amountPaid", 0L),
                            changeAmount = obj.optLong("changeAmount", 0L),
                            paymentReference = obj.optString("paymentReference", null),
                            status = obj.optString("status", "COMPLETED"),
                            cancellationReason = obj.optString("cancellationReason", null),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Items
            val itemList = mutableListOf<TransactionItemEntity>()
            if (root.has("transaction_items")) {
                val array = root.getJSONArray("transaction_items")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    itemList.add(
                        TransactionItemEntity(
                            id = obj.optLong("id", 0),
                            transactionId = obj.getLong("transactionId"),
                            productId = obj.getLong("productId"),
                            productName = obj.getString("productName"),
                            productSku = obj.optString("productSku", ""),
                            costPriceSnapshot = obj.optLong("costPriceSnapshot", 0L),
                            sellingPriceSnapshot = obj.optLong("sellingPriceSnapshot", 0L),
                            quantity = obj.optInt("quantity", 1),
                            returnedQuantity = obj.optInt("returnedQuantity", 0),
                            discountPerItem = obj.optLong("discountPerItem", 0L),
                            subtotal = obj.optLong("subtotal", 0L)
                        )
                    )
                }
            }

            // Returns
            val returnList = mutableListOf<TransactionReturnEntity>()
            if (root.has("transaction_returns")) {
                val array = root.getJSONArray("transaction_returns")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    returnList.add(
                        TransactionReturnEntity(
                            id = obj.optLong("id", 0),
                            transactionId = obj.getLong("transactionId"),
                            transactionNumber = obj.getString("transactionNumber"),
                            returnNumber = obj.getString("returnNumber"),
                            returnAmount = obj.getLong("returnAmount"),
                            reason = obj.getString("reason"),
                            cashierName = obj.getString("cashierName"),
                            returnedItemsSummary = obj.optString("returnedItemsSummary", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Shifts
            val shiftList = mutableListOf<ShiftEntity>()
            if (root.has("shifts")) {
                val array = root.getJSONArray("shifts")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val closedAt = obj.optLong("closedAt", -1L)
                    val expCash = obj.optLong("expectedCash", -1L)
                    val actCash = obj.optLong("actualCash", -1L)
                    shiftList.add(
                        ShiftEntity(
                            id = obj.optLong("id", 0),
                            cashierName = obj.getString("cashierName"),
                            initialCash = obj.optLong("initialCash", 0L),
                            openNote = obj.optString("openNote", null),
                            openedAt = obj.optLong("openedAt", System.currentTimeMillis()),
                            closedAt = if (closedAt > 0) closedAt else null,
                            expectedCash = if (expCash >= 0) expCash else null,
                            actualCash = if (actCash >= 0) actCash else null,
                            cashDifference = obj.optLong("cashDifference", 0L),
                            closeNote = obj.optString("closeNote", null),
                            status = obj.optString("status", "CLOSED"),
                            totalCashSales = obj.optLong("totalCashSales", 0L),
                            totalNonCashSales = obj.optLong("totalNonCashSales", 0L),
                            totalExpensesCash = obj.optLong("totalExpensesCash", 0L),
                            cashIn = obj.optLong("cashIn", 0L),
                            cashOut = obj.optLong("cashOut", 0L)
                        )
                    )
                }
            }

            // Expenses
            val expenseList = mutableListOf<ExpenseEntity>()
            if (root.has("expenses")) {
                val array = root.getJSONArray("expenses")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val sId = obj.optLong("shiftId", -1L)
                    expenseList.add(
                        ExpenseEntity(
                            id = obj.optLong("id", 0),
                            title = obj.getString("title"),
                            category = obj.getString("category"),
                            amount = obj.optLong("amount", 0L),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            shiftId = if (sId > 0) sId else null,
                            cashierName = obj.getString("cashierName"),
                            note = obj.optString("note", null),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Movements
            val movementList = mutableListOf<StockMovementEntity>()
            if (root.has("stock_movements")) {
                val array = root.getJSONArray("stock_movements")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    movementList.add(
                        StockMovementEntity(
                            id = obj.optLong("id", 0),
                            productId = obj.getLong("productId"),
                            productName = obj.getString("productName"),
                            previousStock = obj.getInt("previousStock"),
                            changeAmount = obj.getInt("changeAmount"),
                            currentStock = obj.getInt("currentStock"),
                            type = obj.getString("type"),
                            referenceId = obj.optString("referenceId", null),
                            reason = obj.optString("reason", null),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Transactional Replace in Room
            database.withTransaction {
                // Clear existing
                database.transactionDao().clearReturns()
                database.transactionDao().clearItems()
                database.transactionDao().clearTransactions()
                database.productDao().clearAll()
                database.categoryDao().clearAll()
                database.employeeDao().clearAll()
                database.shiftDao().clearAll()
                database.expenseDao().clearAll()
                database.stockMovementDao().clearAll()
                database.heldTransactionDao().clearAll()

                // Insert restored
                if (restoredSettings != null) database.storeSettingsDao().insertOrUpdateSettings(restoredSettings)
                if (categoryList.isNotEmpty()) database.categoryDao().insertCategories(categoryList)
                if (productList.isNotEmpty()) database.productDao().insertProducts(productList)
                if (employeeList.isNotEmpty()) database.employeeDao().insertEmployees(employeeList)
                if (transactionList.isNotEmpty()) database.transactionDao().insertTransactions(transactionList)
                if (itemList.isNotEmpty()) database.transactionDao().insertItems(itemList)
                if (returnList.isNotEmpty()) database.transactionDao().insertReturns(returnList)
                if (shiftList.isNotEmpty()) database.shiftDao().insertShifts(shiftList)
                if (expenseList.isNotEmpty()) database.expenseDao().insertExpenses(expenseList)
                if (movementList.isNotEmpty()) database.stockMovementDao().insertMovements(movementList)

                database.auditLogDao().insertLog(
                    AuditLogEntity(
                        action = "RESTORE_DATA",
                        cashierName = restoredSettings?.selectedCashierName ?: "System",
                        details = "Pemulihan database sukses (${productList.size} produk, ${transactionList.size} transaksi)"
                    )
                )
            }

            Result.success("Pemulihan data sukses! (${productList.size} produk, ${transactionList.size} transaksi)")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal memulihkan data: ${e.localizedMessage ?: "Format tidak cocok"}"))
        }
    }

    /**
     * Generates a complete Financial Report CSV file
     */
    suspend fun exportFinancialReportCsv(
        database: TokokuhDatabase,
        startTime: Long,
        endTime: Long,
        outputStream: OutputStream
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val transactions = database.transactionDao().getTransactionsBetween(startTime, endTime)
            val expenses = database.expenseDao().getExpensesBetween(startTime, endTime)
            val allItems = database.transactionDao().getAllTransactionItems()
            val txIds = transactions.map { it.id }.toSet()
            val relevantItems = allItems.filter { txIds.contains(it.transactionId) }

            // Calculations
            val completedTxs = transactions.filter { it.status != "CANCELLED" }
            val totalOmzet = completedTxs.sumOf { it.totalAmount }
            val totalItemsSold = relevantItems.filter { it.quantity > it.returnedQuantity }
                .sumOf { it.quantity - it.returnedQuantity }
            val totalHpp = relevantItems.sumOf { (it.quantity - it.returnedQuantity) * it.costPriceSnapshot }
            val grossProfit = totalOmzet - totalHpp
            val totalExpenses = expenses.sumOf { it.amount }
            val netProfit = grossProfit - totalExpenses

            val csv = StringBuilder()
            csv.append("LAPORAN KEUANGAN TOKOKUH - OFFLINE POS\n")
            csv.append("Website: https://www.tokokuh.com\n")
            csv.append("Periode: ${CurrencyUtils.formatDateTime(startTime)} s/d ${CurrencyUtils.formatDateTime(endTime)}\n")
            csv.append("Diekspor pada: ${CurrencyUtils.formatDateTime(System.currentTimeMillis())}\n\n")

            // Ringkasan
            csv.append("--- RINGKASAN FINANSIAL ---\n")
            csv.append("Total Transaksi,${completedTxs.size}\n")
            csv.append("Total Barang Terjual,${totalItemsSold}\n")
            csv.append("Total Omzet Penjualan,${totalOmzet}\n")
            csv.append("Total HPP (Harga Pokok Penjualan),${totalHpp}\n")
            csv.append("Laba Kotor,${grossProfit}\n")
            csv.append("Total Biaya Pengeluaran,${totalExpenses}\n")
            csv.append("Laba Bersih,${netProfit}\n\n")

            // Detail Transaksi
            csv.append("--- DAFTAR TRANSAKSI ---\n")
            csv.append("No Transaksi,Tanggal,Kasir,Metode,Status,Subtotal,Diskon,Pajak,Total\n")
            for (t in transactions) {
                csv.append("\"${t.transactionNumber}\",\"${CurrencyUtils.formatDateTime(t.createdAt)}\",\"${t.cashierName}\",\"${t.paymentType}-${t.paymentMethodDetails}\",\"${t.status}\",${t.subtotal},${t.discountAmount},${t.taxAmount},${t.totalAmount}\n")
            }
            csv.append("\n")

            // Detail Pengeluaran
            csv.append("--- DAFTAR PENGELUARAN ---\n")
            csv.append("Judul,Kategori,Metode,Kasir,Tanggal,Jumlah,Catatan\n")
            for (e in expenses) {
                csv.append("\"${e.title}\",\"${e.category}\",\"${e.paymentMethod}\",\"${e.cashierName}\",\"${CurrencyUtils.formatDateTime(e.createdAt)}\",${e.amount},\"${e.note ?: ""}\"\n")
            }

            outputStream.use { stream ->
                stream.write(csv.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            }

            Result.success("Laporan CSV berhasil dibuat (${completedTxs.size} transaksi)")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal ekspor CSV: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
