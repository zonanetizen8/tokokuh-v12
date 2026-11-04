package com.example

import com.example.utils.CurrencyUtils
import com.example.utils.EscPosPrinterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("Rp10.000", CurrencyUtils.formatRupiah(10000L))
        assertEquals("Rp0", CurrencyUtils.formatRupiah(0L))
        assertEquals("Rp1.250.000", CurrencyUtils.formatRupiah(1250000L))
        assertEquals(50000L, CurrencyUtils.parseRupiah("Rp50.000"))
        assertEquals(100000L, CurrencyUtils.parseRupiah("100.000"))
    }

    @Test
    fun testTransactionNumberFormat() {
        val txNum = CurrencyUtils.generateTransactionNumber()
        assertTrue(txNum.startsWith("TRX-"))
        assertEquals(19, txNum.length) // "TRX-" (4) + "YYYYMMDD" (8) + "-" (1) + "XXXXXX" (6) = 19 chars

        val retNum = CurrencyUtils.generateReturnNumber()
        assertTrue(retNum.startsWith("RET-"))
    }

    @Test
    fun testEscPosTwoColumnsFormatting() {
        val line = EscPosPrinterHelper.formatTwoColumns("Item A", "Rp10.000", 32)
        assertEquals(33, line.length) // 32 characters + '\n'
        assertTrue(line.startsWith("Item A"))
        assertTrue(line.endsWith("Rp10.000\n"))
    }

    @Test
    fun testFinancialCalculations() {
        val omzet = 500000L
        val hpp = 300000L
        val grossProfit = omzet - hpp
        val expenses = 50000L
        val netProfit = grossProfit - expenses

        assertEquals(200000L, grossProfit)
        assertEquals(150000L, netProfit)
    }

    @Test
    fun testShiftVarianceCalculation() {
        val initialCash = 100000L
        val cashSales = 250000L
        val cashExpenses = 30000L
        val cashIn = 20000L
        val cashOut = 50000L

        // Expected cash in drawer:
        // Modal awal + penjualan tunai - pengeluaran tunai + penambahan kas - pengambilan kas
        val expectedCash = initialCash + cashSales - cashExpenses + cashIn - cashOut
        assertEquals(290000L, expectedCash)

        val actualCash = 290000L // Exact
        val diff = actualCash - expectedCash
        assertEquals(0L, diff)

        val actualCashShort = 285000L // Kurang 5.000
        val diffShort = actualCashShort - expectedCash
        assertEquals(-5000L, diffShort)
    }
}
