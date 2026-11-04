package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.TokokuhDatabase
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.ShiftEntity
import com.example.data.repository.CartItem
import com.example.data.repository.TokokuhRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: TokokuhDatabase
    private lateinit var repository: TokokuhRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TokokuhDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TokokuhRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TOKOKUH", appName)
    }

    @Test
    fun testProductLifecycleAndStockMovement() = runBlocking {
        // 1. Add Product
        val prodId = repository.saveProduct(
            id = 0L,
            name = "Kopi Gula Aren",
            sku = "KGA-01",
            barcode = "8991234567890",
            categoryId = null,
            costPrice = 8000L,
            sellingPrice = 15000L,
            stock = 20,
            unit = "cup",
            imageUri = null,
            isActive = true,
            isUnlimitedStock = false,
            minStockAlert = 5,
            cashierName = "Budi"
        )
        assertTrue(prodId > 0)

        val product = db.productDao().getProductById(prodId)
        assertNotNull(product)
        assertEquals("Kopi Gula Aren", product!!.name)
        assertEquals(20, product.stock)
        assertEquals(15000L, product.sellingPrice)

        // 2. Adjust Stock
        repository.adjustProductStock(prodId, 10, "Restok barang", "Budi")
        val updatedProduct = db.productDao().getProductById(prodId)
        assertEquals(30, updatedProduct!!.stock)

        // 3. Check barcode query
        val foundByBarcode = repository.getProductByBarcode("8991234567890")
        assertNotNull(foundByBarcode)
        assertEquals(prodId, foundByBarcode!!.id)
    }

    @Test
    fun testPosCheckoutAndStockDeduction() = runBlocking {
        val prodId = repository.saveProduct(
            id = 0L,
            name = "Mie Goreng Spesial",
            sku = "MGS-01",
            barcode = "8999888777",
            categoryId = null,
            costPrice = 7000L,
            sellingPrice = 12000L,
            stock = 15,
            unit = "porsi",
            imageUri = null,
            isActive = true,
            isUnlimitedStock = false,
            minStockAlert = 3,
            cashierName = "Budi"
        )

        val prod = db.productDao().getProductById(prodId)!!
        val cartItems = listOf(CartItem(product = prod, quantity = 2))

        val result = repository.processCheckout(
            items = cartItems,
            discountOverall = 2000L,
            taxPercent = 0.0,
            isTaxInclusive = false,
            taxEnabled = false,
            paymentType = "CASH",
            paymentMethodDetails = "CASH",
            amountPaid = 30000L,
            paymentReference = null,
            cashierName = "Budi"
        )

        assertTrue(result.isSuccess)
        val tx = result.getOrNull()!!
        assertEquals(24000L, tx.subtotal) // 2 x 12.000
        assertEquals(2000L, tx.discountAmount)
        assertEquals(22000L, tx.totalAmount)
        assertEquals(8000L, tx.changeAmount) // 30.000 - 22.000 = 8.000

        // Verify stock deducted by 2
        val remainingStock = db.productDao().getProductById(prodId)!!.stock
        assertEquals(13, remainingStock) // 15 - 2 = 13

        // Verify items saved
        val savedItems = db.transactionDao().getItemsByTransactionId(tx.id)
        assertEquals(1, savedItems.size)
        assertEquals(7000L, savedItems[0].costPriceSnapshot) // Snapshot HPP
    }

    @Test
    fun testTransactionReturnRestoresStock() = runBlocking {
        val prodId = repository.saveProduct(
            id = 0L,
            name = "Teh Manis Dingin",
            sku = "TMD-01",
            barcode = "899111222333",
            categoryId = null,
            costPrice = 2000L,
            sellingPrice = 5000L,
            stock = 10,
            unit = "cup",
            imageUri = null,
            isActive = true,
            isUnlimitedStock = false,
            minStockAlert = 2,
            cashierName = "Andi"
        )

        val prod = db.productDao().getProductById(prodId)!!
        val checkoutRes = repository.processCheckout(
            items = listOf(CartItem(product = prod, quantity = 3)),
            discountOverall = 0L,
            taxPercent = 0.0,
            isTaxInclusive = false,
            taxEnabled = false,
            paymentType = "CASH",
            paymentMethodDetails = "CASH",
            amountPaid = 15000L,
            paymentReference = null,
            cashierName = "Andi"
        )
        val tx = checkoutRes.getOrNull()!!
        assertEquals(7, db.productDao().getProductById(prodId)!!.stock) // 10 - 3 = 7

        // Return 1 cup
        val items = db.transactionDao().getItemsByTransactionId(tx.id)
        val returnRes = repository.processReturn(
            transactionId = tx.id,
            returnedItems = mapOf(items[0].id to 1),
            reason = "Salah pesan rasa",
            cashierName = "Andi"
        )
        assertTrue(returnRes.isSuccess)

        // Verify stock restored to 8
        assertEquals(8, db.productDao().getProductById(prodId)!!.stock)
    }

    @Test
    fun testShiftLifecycle() = runBlocking {
        // Open shift
        val openRes = repository.openShift("Logis", 150000L, "Shift pagi")
        assertTrue(openRes.isSuccess)
        val shiftId = openRes.getOrNull()!!

        val active = db.shiftDao().getActiveShift()
        assertNotNull(active)
        assertEquals(150000L, active!!.initialCash)

        // Close shift
        val closeRes = repository.closeShift(shiftId, 150000L, "Semua pas", "Logis")
        assertTrue(closeRes.isSuccess)

        val closed = db.shiftDao().getShiftById(shiftId)!!
        assertEquals("CLOSED", closed.status)
        assertEquals(0L, closed.cashDifference)
    }
}
