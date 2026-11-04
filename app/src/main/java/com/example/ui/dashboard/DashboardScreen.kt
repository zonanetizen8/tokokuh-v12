package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.TransactionEntity
import com.example.ui.components.TokokuhMetricCard
import com.example.ui.components.TokokuhStatusBadge
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
fun DashboardScreen(
    viewModel: TokokuhViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToShifts: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    // Filter today's transactions & expenses
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis

    val todayTransactions = transactions.filter { it.createdAt >= startOfToday && it.status != "CANCELLED" }
    val todayExpenses = expenses.filter { it.createdAt >= startOfToday }

    val todayOmzet = todayTransactions.sumOf { it.totalAmount }
    val todayTxCount = todayTransactions.size

    // Estimated gross profit & net profit today
    val todayExpenseTotal = todayExpenses.sumOf { it.amount }
    // Calculate estimated HPP from today's items if available, or 70% conservative
    val todayEstimatedGross = (todayOmzet * 0.35).toLong() // fallback reasonable margin
    val todayNetProfit = (todayEstimatedGross - todayExpenseTotal).coerceAtLeast(0L)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official Header Logo
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_header_atas),
                            contentDescription = "TOKOKUH",
                            modifier = Modifier
                                .height(38.dp)
                                .width(150.dp),
                            contentScale = ContentScale.Fit
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TokokuhSecondary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (activeShift != null) TokokuhSuccess else TokokuhWarning)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeShift != null) "Shift: ${activeShift?.cashierName}" else "Shift Ditutup",
                                    color = TokokuhTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = settings?.storeName ?: "TOKOKUH STORE",
                        color = TokokuhAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings?.storeAddress ?: "Aplikasi Kasir Offline Profesional",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Low stock warning banner
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProducts() }
                        .testTag("low_stock_warning_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2805)),
                    border = BorderStroke(1.dp, TokokuhWarning)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TokokuhWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Peringatan Stok Menipis!",
                                color = TokokuhWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${lowStockProducts.size} produk memiliki stok di bawah batas minimum.",
                                color = TokokuhTextPrimary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = TokokuhWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick POS CTA button
        item {
            Button(
                onClick = onNavigateToPos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("dashboard_pos_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    tint = Color(0xFF1E1E1E),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BUKA KASIR (POS)",
                    color = Color(0xFF1E1E1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // KPI Metrics Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TokokuhMetricCard(
                    title = "Omzet Hari Ini",
                    value = CurrencyUtils.formatRupiah(todayOmzet),
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f),
                    accentColor = TokokuhAccent,
                    onClick = onNavigateToReports
                )
                TokokuhMetricCard(
                    title = "Transaksi Hari Ini",
                    value = "$todayTxCount Struk",
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f),
                    accentColor = TokokuhSuccess,
                    onClick = onNavigateToTransactions
                )
            }
        }

        // KPI Metrics Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TokokuhMetricCard(
                    title = "Total Produk",
                    value = "${products.size} Item",
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF64B5F6),
                    onClick = onNavigateToProducts
                )
                TokokuhMetricCard(
                    title = "Pengeluaran Hari Ini",
                    value = CurrencyUtils.formatRupiah(todayExpenseTotal),
                    icon = Icons.Default.MoneyOff,
                    modifier = Modifier.weight(1f),
                    accentColor = TokokuhError,
                    onClick = onNavigateToExpenses
                )
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "Akses Cepat",
                color = TokokuhTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(
                    icon = Icons.Default.Schedule,
                    title = "Kelola Shift",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToShifts
                )
                ActionChip(
                    icon = Icons.Default.Add,
                    title = "Tambah Produk",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToProducts
                )
                ActionChip(
                    icon = Icons.Default.TrendingUp,
                    title = "Laporan",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToReports
                )
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Lihat Semua",
                    color = TokokuhAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onNavigateToTransactions() }
                        .padding(4.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Transaksi",
                            color = TokokuhTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gunakan menu POS untuk melakukan transaksi kasir.",
                            color = TokokuhTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionSummaryCard(tx = tx, onClick = onNavigateToTransactions)
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
        border = BorderStroke(1.dp, TokokuhSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TokokuhAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TokokuhTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TransactionSummaryCard(
    tx: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.transactionNumber,
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${CurrencyUtils.formatDateTime(tx.createdAt)} • Kasir: ${tx.cashierName}",
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatRupiah(tx.totalAmount),
                    color = TokokuhAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                TokokuhStatusBadge(
                    text = if (tx.status == "COMPLETED") tx.paymentMethodDetails else tx.status,
                    color = if (tx.status == "COMPLETED") TokokuhSuccess else TokokuhError
                )
            }
        }
    }
}
