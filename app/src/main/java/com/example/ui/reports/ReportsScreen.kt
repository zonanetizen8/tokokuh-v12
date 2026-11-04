package com.example.ui.reports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.TransactionItemEntity
import com.example.ui.components.TokokuhMetricCard
import com.example.ui.components.TokokuhTopBar
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhError
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhSuccess
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.ui.theme.TokokuhWarning
import com.example.utils.CurrencyUtils
import com.example.viewmodel.TokokuhViewModel
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: TokokuhViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedPeriod by remember { mutableStateOf("TODAY") } // TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, ALL
    var allItemsList by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }

    // CSV SAF export launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val (startTime, endTime) = calculateTimeRange(selectedPeriod)
            viewModel.exportCsvReport(uri, startTime, endTime)
        }
    }

    // Determine time range based on selectedPeriod
    val (startTime, endTime) = calculateTimeRange(selectedPeriod)

    val periodTransactions = transactions.filter {
        it.createdAt in startTime..endTime && it.status != "CANCELLED"
    }
    val periodExpenses = expenses.filter {
        it.createdAt in startTime..endTime
    }

    // Filter items related to these transactions
    val periodTxIds = periodTransactions.map { it.id }.toSet()
    val periodItems = allItemsList.filter { periodTxIds.contains(it.transactionId) }

    // Financial Metrics Calculation
    val totalOmzet = periodTransactions.sumOf { it.totalAmount }
    val totalDiscounts = periodTransactions.sumOf { it.discountAmount }
    val totalTaxes = periodTransactions.sumOf { it.taxAmount }
    val cashSales = periodTransactions.filter { it.paymentType == "CASH" }.sumOf { it.totalAmount }
    val nonCashSales = periodTransactions.filter { it.paymentType == "NON_CASH" }.sumOf { it.totalAmount }

    // HPP based on exact snapshot cost price
    val totalHpp = periodItems.sumOf { (it.quantity - it.returnedQuantity) * it.costPriceSnapshot }
    val totalItemsSold = periodItems.sumOf { (it.quantity - it.returnedQuantity) }

    val grossProfit = totalOmzet - totalHpp
    val totalExpenseAmount = periodExpenses.sumOf { it.amount }
    val netProfit = grossProfit - totalExpenseAmount

    // Product Ranking
    val itemsByProduct = periodItems.groupBy { it.productId }
    val productSalesRank = itemsByProduct.map { (prodId, items) ->
        val name = items.firstOrNull()?.productName ?: "Produk #$prodId"
        val totalQty = items.sumOf { it.quantity - it.returnedQuantity }
        val totalVal = items.sumOf { it.subtotal }
        Triple(name, totalQty, totalVal)
    }.sortedByDescending { it.second }

    // Load items for calculations
    LaunchedEffect(Unit) {
        // Fetch all items from DB for report compilation
        val app = viewModel.products.value // just trigger
        // Let's gather items from transactions
        val itemsAcc = mutableListOf<TransactionItemEntity>()
        for (tx in transactions) {
            itemsAcc.addAll(viewModel.getTransactionItemsForDetail(tx.id))
        }
        allItemsList = itemsAcc
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(
            title = "Laporan Finansial & Omzet",
            actions = {
                Button(
                    onClick = {
                        val fileName = "Laporan_TOKOKUH_${selectedPeriod}_${System.currentTimeMillis()}.csv"
                        exportCsvLauncher.launch(fileName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor CSV", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )

        // Period Selector
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val periods = listOf(
                "TODAY" to "Hari Ini",
                "YESTERDAY" to "Kemarin",
                "THIS_WEEK" to "Minggu Ini",
                "THIS_MONTH" to "Bulan Ini",
                "ALL" to "Semua Riwayat"
            )
            items(periods) { (key, label) ->
                FilterChip(
                    selected = selectedPeriod == key,
                    onClick = { selectedPeriod = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokokuhAccent,
                        selectedLabelColor = Color(0xFF1E1E1E)
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Net Profit Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netProfit >= 0) Color(0xFF1B3820) else Color(0xFF3E1818)
                    ),
                    border = BorderStroke(1.dp, if (netProfit >= 0) TokokuhSuccess else TokokuhError)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LABA BERSIH (NET PROFIT)",
                                color = if (netProfit >= 0) TokokuhSuccess else TokokuhError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = if (netProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (netProfit >= 0) TokokuhSuccess else TokokuhError
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupiah(netProfit),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rumus: Laba Kotor (${CurrencyUtils.formatRupiah(grossProfit)}) - Biaya Operasional (${CurrencyUtils.formatRupiah(totalExpenseAmount)})",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Financial Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TokokuhMetricCard(
                        title = "Total Omzet",
                        value = CurrencyUtils.formatRupiah(totalOmzet),
                        icon = Icons.Default.AttachMoney,
                        modifier = Modifier.weight(1f),
                        accentColor = TokokuhAccent
                    )
                    TokokuhMetricCard(
                        title = "Laba Kotor",
                        value = CurrencyUtils.formatRupiah(grossProfit),
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f),
                        accentColor = TokokuhSuccess
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TokokuhMetricCard(
                        title = "HPP (Modal)",
                        value = CurrencyUtils.formatRupiah(totalHpp),
                        icon = Icons.Default.ShoppingBag,
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF64B5F6)
                    )
                    TokokuhMetricCard(
                        title = "Biaya Operasional",
                        value = CurrencyUtils.formatRupiah(totalExpenseAmount),
                        icon = Icons.Default.MoneyOff,
                        modifier = Modifier.weight(1f),
                        accentColor = TokokuhError
                    )
                }
            }

            // Sales breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                    border = BorderStroke(1.dp, TokokuhSecondary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Rincian Penjualan", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Divider(color = TokokuhSecondary)

                        ReportDetailRow("Total Transaksi Berhasil", "${periodTransactions.size} Transaksi")
                        ReportDetailRow("Total Barang Terjual", "$totalItemsSold Item")
                        ReportDetailRow("Penjualan Tunai (Cash)", CurrencyUtils.formatRupiah(cashSales))
                        ReportDetailRow("Penjualan Non-Tunai (QRIS / Bank)", CurrencyUtils.formatRupiah(nonCashSales))
                        if (totalDiscounts > 0L) {
                            ReportDetailRow("Total Diskon Diberikan", "-${CurrencyUtils.formatRupiah(totalDiscounts)}", TokokuhError)
                        }
                        if (totalTaxes > 0L) {
                            ReportDetailRow("Total Pajak Dipungut", CurrencyUtils.formatRupiah(totalTaxes))
                        }
                    }
                }
            }

            // Top Products
            item {
                Text("Peringkat Produk Terlaris", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (productSalesRank.isEmpty()) {
                item {
                    Text("Belum ada penjualan pada periode ini.", color = TokokuhTextSecondary, fontSize = 13.sp)
                }
            } else {
                items(productSalesRank.take(5)) { (name, qty, revenue) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                        border = BorderStroke(1.dp, TokokuhSecondary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(name, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Terjual: $qty pcs", color = TokokuhTextSecondary, fontSize = 12.sp)
                            }
                            Text(CurrencyUtils.formatRupiah(revenue), color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailRow(label: String, value: String, valueColor: Color = TokokuhTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TokokuhTextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun calculateTimeRange(period: String): Pair<Long, Long> {
    val now = Calendar.getInstance()
    val endTime = now.timeInMillis

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return when (period) {
        "TODAY" -> cal.timeInMillis to endTime
        "YESTERDAY" -> {
            val startYesterday = cal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
            val endYesterday = Calendar.getInstance().apply {
                timeInMillis = startYesterday
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            startYesterday to endYesterday
        }
        "THIS_WEEK" -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.timeInMillis to endTime
        }
        "THIS_MONTH" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis to endTime
        }
        else -> 0L to endTime
    }
}
