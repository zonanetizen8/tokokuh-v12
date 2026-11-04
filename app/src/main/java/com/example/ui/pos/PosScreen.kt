package com.example.ui.pos

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.TransactionEntity
import com.example.ui.components.TokokuhConfirmDialog
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.components.TokokuhStatusBadge
import com.example.ui.scanner.CameraScannerView
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
fun PosScreen(
    viewModel: TokokuhViewModel,
    onNavigateToAddProductWithBarcode: ((String) -> Unit)? = null
) {
    val filteredProducts by viewModel.filteredPosProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val overallDiscount by viewModel.overallDiscount.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()
    val heldTransactions by viewModel.heldTransactions.collectAsState()
    val isProcessingCheckout by viewModel.isProcessingCheckout.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val lastCompletedTx by viewModel.lastTransaction.collectAsState()
    val lastCompletedTxItems by viewModel.lastTransactionItems.collectAsState()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var barcodeNotFoundDialog by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val totalCartCount = cartItems.sumOf { it.quantity }

    Box(modifier = Modifier.fillMaxSize().background(TokokuhBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (totalCartCount > 0) 80.dp else 16.dp)
        ) {
            // Search Bar & Barcode Scanner Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_search_input"),
                    placeholder = { Text("Cari produk atau barcode...", color = TokokuhTextSecondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TokokuhAccent)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TokokuhTextSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary,
                        focusedContainerColor = TokokuhCard,
                        unfocusedContainerColor = TokokuhCard
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            // If user scanned with external HID keyboard or typed barcode
                            if (searchQuery.isNotBlank()) {
                                viewModel.onBarcodeScanned(searchQuery) { barcode ->
                                    barcodeNotFoundDialog = barcode
                                }
                            }
                        }
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TokokuhAccent,
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { showCameraScanner = true }
                        .testTag("pos_barcode_scanner_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Pindai Barcode",
                            tint = Color(0xFF1E1E1E),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Categories Chip Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.selectedCategoryId.value = null },
                        label = { Text("Semua", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TokokuhAccent,
                            selectedLabelColor = Color(0xFF1E1E1E),
                            containerColor = TokokuhCard,
                            labelColor = TokokuhTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategoryId == null,
                            borderColor = TokokuhSecondary,
                            selectedBorderColor = TokokuhAccent
                        )
                    )
                }

                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = {
                            viewModel.selectedCategoryId.value =
                                if (selectedCategoryId == category.id) null else category.id
                        },
                        label = { Text(category.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TokokuhAccent,
                            selectedLabelColor = Color(0xFF1E1E1E),
                            containerColor = TokokuhCard,
                            labelColor = TokokuhTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategoryId == category.id,
                            borderColor = TokokuhSecondary,
                            selectedBorderColor = TokokuhAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TokokuhEmptyState(
                        icon = Icons.Default.Search,
                        title = "Produk Tidak Ditemukan",
                        description = if (searchQuery.isNotBlank())
                            "Tidak ada produk dengan nama/barcode '$searchQuery'."
                        else "Belum ada produk aktif di toko ini."
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val itemInCart = cartItems.find { it.product.id == product.id }
                        PosProductCard(
                            product = product,
                            quantityInCart = itemInCart?.quantity ?: 0,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Bottom Persistent Cart Bar
        if (totalCartCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable { showCartSheet = true }
                    .testTag("pos_bottom_cart_bar"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TokokuhAccent),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E1E)
                        ) {
                            Text(
                                text = "$totalCartCount",
                                color = TokokuhAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Keranjang Belanja",
                                color = Color(0xFF1E1E1E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(cartSubtotal - overallDiscount),
                                color = Color(0xFF1E1E1E),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E1E)
                    ) {
                        Text(
                            text = "Lihat & Bayar",
                            color = TokokuhAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Camera Barcode Scanner View Overlay
    if (showCameraScanner) {
        CameraScannerView(
            onBarcodeDetected = { barcode ->
                showCameraScanner = false
                viewModel.onBarcodeScanned(barcode) { notFoundBarcode ->
                    barcodeNotFoundDialog = notFoundBarcode
                }
            },
            onClose = { showCameraScanner = false }
        )
    }

    // Barcode Not Found Dialog
    if (barcodeNotFoundDialog != null) {
        val barcode = barcodeNotFoundDialog!!
        TokokuhConfirmDialog(
            title = "Barcode Belum Terdaftar",
            message = "Barcode '$barcode' belum ada di database produk. Apakah Anda ingin menambahkan produk baru dengan barcode ini?",
            confirmText = "Tambah Produk Baru",
            cancelText = "Batal",
            onConfirm = {
                val b = barcode
                barcodeNotFoundDialog = null
                onNavigateToAddProductWithBarcode?.invoke(b)
            },
            onDismiss = { barcodeNotFoundDialog = null }
        )
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        CartBottomSheet(
            cartItems = cartItems,
            overallDiscount = overallDiscount,
            settings = settings,
            heldTransactions = heldTransactions,
            onIncrement = { viewModel.incrementCartQuantity(it) },
            onDecrement = { viewModel.decrementCartQuantity(it) },
            onRemove = { viewModel.removeFromCart(it) },
            onSetItemDiscount = { id, disc -> viewModel.setItemDiscount(id, disc) },
            onSetOverallDiscount = { viewModel.setOverallDiscount(it) },
            onClearCart = { viewModel.clearCart() },
            onHoldTransaction = { viewModel.holdCurrentTransaction(it) },
            onResumeHeldTransaction = { viewModel.resumeHeldTransaction(it) },
            onDeleteHeldTransaction = { viewModel.deleteHeldTransaction(it) },
            onProceedToPayment = { showPaymentDialog = true },
            onDismiss = { showCartSheet = false }
        )
    }

    // Payment Dialog
    if (showPaymentDialog) {
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
        val totalToPay = if (isTaxInclusive || !taxEnabled) discountedSubtotal else discountedSubtotal + calculatedTax

        PaymentDialog(
            totalAmount = totalToPay,
            isProcessing = isProcessingCheckout,
            onDismiss = { showPaymentDialog = false },
            onSubmitPayment = { paymentType, paymentDetails, amountPaid, reference ->
                viewModel.processCheckout(
                    paymentType = paymentType,
                    paymentMethodDetails = paymentDetails,
                    amountPaid = amountPaid,
                    paymentReference = reference,
                    onSuccess = { tx ->
                        showPaymentDialog = false
                        showReceiptDialog = true
                    }
                )
            }
        )
    }

    // Receipt Preview Dialog
    if (showReceiptDialog && lastCompletedTx != null) {
        ReceiptPreviewDialog(
            transaction = lastCompletedTx!!,
            items = lastCompletedTxItems,
            settings = settings,
            isPrinting = isPrinting,
            printerStatus = printerStatus,
            onPrint = {
                viewModel.printTransactionReceipt(lastCompletedTx!!, lastCompletedTxItems)
            },
            onDismiss = { showReceiptDialog = false }
        )
    }
}

@Composable
fun PosProductCard(
    product: ProductEntity,
    quantityInCart: Int,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("pos_product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
        border = BorderStroke(
            1.dp,
            if (quantityInCart > 0) TokokuhAccent else TokokuhSecondary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image or Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(TokokuhSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = product.name.take(2).uppercase(),
                        color = TokokuhAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cart quantity badge on top right
                if (quantityInCart > 0) {
                    Surface(
                        shape = CircleShape,
                        color = TokokuhAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "$quantityInCart",
                            color = Color(0xFF1E1E1E),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyUtils.formatRupiah(product.sellingPrice),
                    color = TokokuhAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stockText = if (product.isUnlimitedStock) "Tak Terbatas" else "Stok: ${product.stock}"
                    val stockColor = if (product.isUnlimitedStock) TokokuhTextSecondary
                    else if (product.stock <= 0) TokokuhError
                    else if (product.stock <= product.minStockAlert) TokokuhWarning
                    else TokokuhTextSecondary

                    Text(
                        text = stockText,
                        color = stockColor,
                        fontSize = 11.sp
                    )

                    Surface(
                        shape = CircleShape,
                        color = TokokuhAccent.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah",
                                tint = TokokuhAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
