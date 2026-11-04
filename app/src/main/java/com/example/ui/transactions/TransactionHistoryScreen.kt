package com.example.ui.transactions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.components.TokokuhStatusBadge
import com.example.ui.components.TokokuhTopBar
import com.example.ui.pos.ReceiptPreviewDialog
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

@Composable
fun TransactionHistoryScreen(
    viewModel: TokokuhViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, COMPLETED, CANCELLED, RETURNED
    var selectedPaymentFilter by remember { mutableStateOf("ALL") } // ALL, CASH, NON_CASH

    // Detail & action dialog states
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showReceiptPreview by remember { mutableStateOf(false) }

    var showReturnDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReasonInput by remember { mutableStateOf("") }

    val filteredTransactions = transactions.filter { tx ->
        val matchQuery = searchQuery.isBlank() ||
                tx.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                tx.cashierName.contains(searchQuery, ignoreCase = true)

        val matchStatus = when (selectedStatusFilter) {
            "COMPLETED" -> tx.status == "COMPLETED"
            "CANCELLED" -> tx.status == "CANCELLED"
            "RETURNED" -> tx.status == "RETURNED_PARTIAL" || tx.status == "RETURNED_FULL"
            else -> true
        }

        val matchPayment = when (selectedPaymentFilter) {
            "CASH" -> tx.paymentType == "CASH"
            "NON_CASH" -> tx.paymentType == "NON_CASH"
            else -> true
        }

        matchQuery && matchStatus && matchPayment
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(title = "Riwayat Transaksi & Retur")

        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nomor struk atau kasir...", color = TokokuhTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TokokuhAccent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TokokuhTextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("tx_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TokokuhAccent,
                unfocusedBorderColor = TokokuhSecondary,
                focusedContainerColor = TokokuhCard,
                unfocusedContainerColor = TokokuhCard
            ),
            singleLine = true
        )

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text("Semua Status") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokokuhAccent,
                        selectedLabelColor = Color(0xFF1E1E1E)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "COMPLETED",
                    onClick = { selectedStatusFilter = "COMPLETED" },
                    label = { Text("Selesai") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokokuhSuccess,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "RETURNED",
                    onClick = { selectedStatusFilter = "RETURNED" },
                    label = { Text("Retur") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokokuhWarning,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "CANCELLED",
                    onClick = { selectedStatusFilter = "CANCELLED" },
                    label = { Text("Dibatalkan") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokokuhError,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTransactions.isEmpty()) {
            TokokuhEmptyState(
                icon = Icons.Default.Receipt,
                title = "Tidak Ada Transaksi",
                description = "Belum ada riwayat transaksi yang cocok dengan filter."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCardItem(
                        tx = tx,
                        onClick = {
                            selectedTransaction = tx
                            showDetailDialog = true
                        }
                    )
                }
            }
        }
    }

    // Load items when selectedTransaction changes
    LaunchedEffect(selectedTransaction) {
        val tx = selectedTransaction
        if (tx != null) {
            transactionItems = viewModel.getTransactionItemsForDetail(tx.id)
        }
    }

    // Detail Dialog
    if (showDetailDialog && selectedTransaction != null) {
        val tx = selectedTransaction!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tx.transactionNumber, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TokokuhStatusBadge(
                            text = tx.status,
                            color = when (tx.status) {
                                "COMPLETED" -> TokokuhSuccess
                                "CANCELLED" -> TokokuhError
                                else -> TokokuhWarning
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${CurrencyUtils.formatDateTime(tx.createdAt)} • Kasir: ${tx.cashierName}",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Daftar Item Belanja:", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    for (item in transactionItems) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = TokokuhSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.productName, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(CurrencyUtils.formatRupiah(item.subtotal), color = TokokuhAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${item.quantity} pcs x ${CurrencyUtils.formatRupiah(item.sellingPriceSnapshot)}",
                                        color = TokokuhTextSecondary,
                                        fontSize = 12.sp
                                    )
                                    if (item.returnedQuantity > 0) {
                                        Text(
                                            "Diretur: ${item.returnedQuantity} pcs",
                                            color = TokokuhError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = TokokuhSecondary)

                    // Financial Summary
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", color = TokokuhTextSecondary, fontSize = 13.sp)
                        Text(CurrencyUtils.formatRupiah(tx.subtotal), color = TokokuhTextPrimary, fontSize = 13.sp)
                    }
                    if (tx.discountAmount > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diskon:", color = TokokuhTextSecondary, fontSize = 13.sp)
                            Text("-${CurrencyUtils.formatRupiah(tx.discountAmount)}", color = TokokuhError, fontSize = 13.sp)
                        }
                    }
                    if (tx.taxAmount > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pajak:", color = TokokuhTextSecondary, fontSize = 13.sp)
                            Text(CurrencyUtils.formatRupiah(tx.taxAmount), color = TokokuhTextPrimary, fontSize = 13.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Transaksi:", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(CurrencyUtils.formatRupiah(tx.totalAmount), color = TokokuhAccent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Metode Pembayaran:", color = TokokuhTextSecondary, fontSize = 13.sp)
                        Text("${tx.paymentType} (${tx.paymentMethodDetails})", color = TokokuhTextPrimary, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Jumlah Bayar:", color = TokokuhTextSecondary, fontSize = 13.sp)
                        Text(CurrencyUtils.formatRupiah(tx.amountPaid), color = TokokuhTextPrimary, fontSize = 13.sp)
                    }
                    if (tx.paymentType == "CASH") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kembalian:", color = TokokuhTextSecondary, fontSize = 13.sp)
                            Text(CurrencyUtils.formatRupiah(tx.changeAmount), color = TokokuhTextPrimary, fontSize = 13.sp)
                        }
                    }
                    if (!tx.paymentReference.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ref Non-Tunai:", color = TokokuhTextSecondary, fontSize = 13.sp)
                            Text(tx.paymentReference, color = TokokuhTextPrimary, fontSize = 13.sp)
                        }
                    }
                    if (!tx.cancellationReason.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TokokuhError.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Alasan Pembatalan: ${tx.cancellationReason}",
                                color = TokokuhError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons: Cetak Ulang, Retur, Batalkan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showDetailDialog = false
                                showReceiptPreview = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Struk", color = Color(0xFF1E1E1E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (tx.status != "CANCELLED") {
                            OutlinedButton(
                                onClick = {
                                    showDetailDialog = false
                                    showReturnDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhWarning),
                                border = BorderStroke(1.dp, TokokuhWarning),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retur", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    showDetailDialog = false
                                    showCancelDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhError),
                                border = BorderStroke(1.dp, TokokuhError),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Batal", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetailDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Receipt Preview Dialog (Reprint)
    if (showReceiptPreview && selectedTransaction != null) {
        ReceiptPreviewDialog(
            transaction = selectedTransaction!!,
            items = transactionItems,
            settings = settings,
            isPrinting = isPrinting,
            printerStatus = printerStatus,
            onPrint = {
                viewModel.printTransactionReceipt(selectedTransaction!!, transactionItems)
            },
            onDismiss = { showReceiptPreview = false }
        )
    }

    // Return Items Dialog
    if (showReturnDialog && selectedTransaction != null) {
        val tx = selectedTransaction!!
        val returnQuantities = remember { mutableStateMapOf<Long, Int>() }
        var returnReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Retur Barang - ${tx.transactionNumber}", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tentukan jumlah barang yang dikembalikan. Stok produk akan otomatis dikembalikan ke inventori.",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp
                    )

                    for (item in transactionItems) {
                        val maxAllowed = item.quantity - item.returnedQuantity
                        if (maxAllowed > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = TokokuhSecondary)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.productName, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Bisa diretur: maks $maxAllowed pcs", color = TokokuhTextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = (returnQuantities[item.id] ?: 0).toString(),
                                            onValueChange = { input ->
                                                val q = input.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                                returnQuantities[item.id] = q.coerceIn(0, maxAllowed)
                                            },
                                            label = { Text("Qty Retur") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = { returnQuantities[item.id] = maxAllowed },
                                            colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                                        ) {
                                            Text("Semua", color = Color(0xFF1E1E1E), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        label = { Text("Alasan Pengembalian / Retur *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                val hasAnyReturn = returnQuantities.values.any { it > 0 }
                Button(
                    onClick = {
                        if (hasAnyReturn) {
                            viewModel.processReturn(
                                transactionId = tx.id,
                                returnedItems = returnQuantities,
                                reason = returnReason.ifBlank { "Retur barang pelanggan" },
                                onSuccess = {
                                    showReturnDialog = false
                                }
                            )
                        }
                    },
                    enabled = hasAnyReturn,
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhWarning)
                ) {
                    Text("Konfirmasi Retur", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReturnDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Cancel Transaction Dialog
    if (showCancelDialog && selectedTransaction != null) {
        val tx = selectedTransaction!!
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Batalkan Transaksi?", color = TokokuhError, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Membatalkan transaksi ${tx.transactionNumber} senilai ${CurrencyUtils.formatRupiah(tx.totalAmount)}. Seluruh stok produk akan otomatis dikembalikan ke inventori.",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Alasan Pembatalan (Wajib)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReasonInput.isNotBlank()) {
                            viewModel.cancelTransaction(tx.id, cancelReasonInput) {
                                showCancelDialog = false
                            }
                        }
                    },
                    enabled = cancelReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhError)
                ) {
                    Text("Ya, Batalkan Transaksi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun TransactionCardItem(
    tx: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_card_${tx.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
        border = BorderStroke(1.dp, TokokuhSecondary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.transactionNumber,
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${CurrencyUtils.formatDateTime(tx.createdAt)} • Kasir: ${tx.cashierName}",
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Metode: ${tx.paymentType} (${tx.paymentMethodDetails})",
                    color = TokokuhTextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatRupiah(tx.totalAmount),
                    color = TokokuhAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                TokokuhStatusBadge(
                    text = tx.status,
                    color = when (tx.status) {
                        "COMPLETED" -> TokokuhSuccess
                        "CANCELLED" -> TokokuhError
                        else -> TokokuhWarning
                    }
                )
            }
        }
    }
}
