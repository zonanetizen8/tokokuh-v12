package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.HeldTransactionEntity
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.repository.CartItem
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhError
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    overallDiscount: Long,
    settings: StoreSettingsEntity?,
    heldTransactions: List<HeldTransactionEntity>,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onSetItemDiscount: (Long, Long) -> Unit,
    onSetOverallDiscount: (Long) -> Unit,
    onClearCart: () -> Unit,
    onHoldTransaction: (String) -> Unit,
    onResumeHeldTransaction: (HeldTransactionEntity) -> Unit,
    onDeleteHeldTransaction: (Long) -> Unit,
    onProceedToPayment: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showHoldDialog by remember { mutableStateOf(false) }
    var holdLabelInput by remember { mutableStateOf("") }
    var showHeldListDialog by remember { mutableStateOf(false) }
    var showOverallDiscountDialog by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf(overallDiscount.toString()) }

    var selectedItemForDiscount by remember { mutableStateOf<CartItem?>(null) }
    var itemDiscountInput by remember { mutableStateOf("") }

    val rawSubtotal = cartItems.sumOf { it.subtotal }
    val discountedSubtotal = (rawSubtotal - overallDiscount).coerceAtLeast(0L)

    val taxPercent = settings?.taxRatePercent ?: 0.0
    val taxEnabled = settings?.taxEnabled ?: false
    val isTaxInclusive = settings?.isTaxInclusive ?: false

    val calculatedTax = if (taxEnabled && taxPercent > 0.0) {
        if (isTaxInclusive) {
            (discountedSubtotal - (discountedSubtotal / (1.0 + (taxPercent / 100.0)))).toLong()
        } else {
            (discountedSubtotal * (taxPercent / 100.0)).toLong()
        }
    } else {
        0L
    }

    val finalTotal = if (isTaxInclusive || !taxEnabled) discountedSubtotal else discountedSubtotal + calculatedTax

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = TokokuhAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keranjang Belanja",
                        color = TokokuhTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = TokokuhSecondary
                    ) {
                        Text(
                            text = "${cartItems.sumOf { it.quantity }}",
                            color = TokokuhAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    if (heldTransactions.isNotEmpty()) {
                        IconButton(
                            onClick = { showHeldListDialog = true },
                            modifier = Modifier.testTag("show_held_list_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Daftar Transaksi Ditahan",
                                tint = TokokuhAccent
                            )
                        }
                    }
                    if (cartItems.isNotEmpty()) {
                        IconButton(
                            onClick = onClearCart,
                            modifier = Modifier.testTag("clear_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Kosongkan Keranjang",
                                tint = TokokuhError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TokokuhEmptyState(
                        icon = Icons.Default.ShoppingCart,
                        title = "Keranjang Masih Kosong",
                        description = "Sentuh item produk di layar untuk menambahkannya ke keranjang.",
                        actionButtonText = if (heldTransactions.isNotEmpty()) "Lihat Pesanan Ditahan (${heldTransactions.size})" else null,
                        onActionClick = { showHeldListDialog = true }
                    )
                }
            } else {
                // Cart Items List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrement = { onIncrement(item.product.id) },
                            onDecrement = { onDecrement(item.product.id) },
                            onRemove = { onRemove(item.product.id) },
                            onOpenDiscount = {
                                selectedItemForDiscount = item
                                itemDiscountInput = item.discountPerItem.toString()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = TokokuhSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                // Action Row: Tahan Transaksi & Diskon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHoldDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hold_transaction_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhAccent),
                        border = BorderStroke(1.dp, TokokuhAccent)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tahan Pesanan", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            discountInput = overallDiscount.toString()
                            showOverallDiscountDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overall_discount_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhTextPrimary),
                        border = BorderStroke(1.dp, TokokuhSecondary)
                    ) {
                        Icon(Icons.Default.Discount, contentDescription = null, modifier = Modifier.size(16.dp), tint = TokokuhAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (overallDiscount > 0) "Diskon: -${CurrencyUtils.formatRupiah(overallDiscount)}" else "Tambah Diskon",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Totals Breakdown
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = TokokuhTextSecondary, fontSize = 13.sp)
                    Text(CurrencyUtils.formatRupiah(rawSubtotal), color = TokokuhTextPrimary, fontSize = 13.sp)
                }

                if (overallDiscount > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon Transaksi", color = TokokuhTextSecondary, fontSize = 13.sp)
                        Text("-${CurrencyUtils.formatRupiah(overallDiscount)}", color = TokokuhError, fontSize = 13.sp)
                    }
                }

                if (taxEnabled && calculatedTax > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Pajak (${settings.taxName} ${settings.taxRatePercent}%${if (isTaxInclusive) " Inklusif" else ""})",
                            color = TokokuhTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(CurrencyUtils.formatRupiah(calculatedTax), color = TokokuhTextPrimary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Tagihan", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(CurrencyUtils.formatRupiah(finalTotal), color = TokokuhAccent, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onProceedToPayment()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("checkout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                ) {
                    Text(
                        text = "BAYAR ${CurrencyUtils.formatRupiah(finalTotal)}",
                        color = Color(0xFF1E1E1E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Hold Dialog
    if (showHoldDialog) {
        AlertDialog(
            onDismissRequest = { showHoldDialog = false },
            title = { Text("Tahan Transaksi Ini?", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Beri nama/label transaksi agar mudah dilanjutkan nanti (contoh: Meja 1, Pesanan Bu Ani).",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = holdLabelInput,
                        onValueChange = { holdLabelInput = it },
                        label = { Text("Label Transaksi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hold_label_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onHoldTransaction(holdLabelInput.ifBlank { "Pesanan ${CurrencyUtils.formatTime(System.currentTimeMillis())}" })
                        showHoldDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                ) {
                    Text("Tahan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showHoldDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = TokokuhCard
        )
    }

    // List of Held Orders Dialog
    if (showHeldListDialog) {
        AlertDialog(
            onDismissRequest = { showHeldListDialog = false },
            title = { Text("Daftar Transaksi Ditahan", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (heldTransactions.isEmpty()) {
                    Text("Tidak ada transaksi yang sedang ditahan.", color = TokokuhTextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(heldTransactions) { held ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = TokokuhSecondary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(held.label, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Kasir: ${held.cashierName} • ${CurrencyUtils.formatTime(held.createdAt)}", color = TokokuhTextSecondary, fontSize = 12.sp)
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onResumeHeldTransaction(held)
                                                showHeldListDialog = false
                                            }
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan", tint = TokokuhAccent)
                                        }
                                        IconButton(onClick = { onDeleteHeldTransaction(held.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TokokuhError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHeldListDialog = false }) {
                    Text("Tutup")
                }
            },
            containerColor = TokokuhCard
        )
    }

    // Overall Discount Dialog
    if (showOverallDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showOverallDiscountDialog = false },
            title = { Text("Diskon Transaksi (Rp)", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Diskon (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val disc = CurrencyUtils.parseRupiah(discountInput)
                        onSetOverallDiscount(disc)
                        showOverallDiscountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                ) {
                    Text("Terapkan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOverallDiscountDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = TokokuhCard
        )
    }

    // Item Discount Dialog
    if (selectedItemForDiscount != null) {
        val item = selectedItemForDiscount!!
        AlertDialog(
            onDismissRequest = { selectedItemForDiscount = null },
            title = { Text("Diskon per Item: ${item.product.name}", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = itemDiscountInput,
                    onValueChange = { itemDiscountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Potongan Harga Satuan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val disc = CurrencyUtils.parseRupiah(itemDiscountInput)
                        onSetItemDiscount(item.product.id, disc)
                        selectedItemForDiscount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                ) {
                    Text("Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedItemForDiscount = null }) {
                    Text("Batal")
                }
            },
            containerColor = TokokuhCard
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onOpenDiscount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhSecondary)
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
                    text = item.product.name,
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.formatRupiah(item.product.sellingPrice),
                        color = TokokuhAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.discountPerItem > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(-${CurrencyUtils.formatRupiah(item.discountPerItem)})",
                            color = TokokuhError,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = "Subtotal: ${CurrencyUtils.formatRupiah(item.subtotal)}",
                    color = TokokuhTextSecondary,
                    fontSize = 11.sp
                )
            }

            // Quantity stepper & actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDiscount,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Discount,
                        contentDescription = "Diskon Item",
                        tint = if (item.discountPerItem > 0) TokokuhAccent else TokokuhTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TokokuhCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = TokokuhTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TokokuhAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
