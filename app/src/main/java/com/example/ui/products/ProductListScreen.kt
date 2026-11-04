package com.example.ui.products

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.ProductEntity
import com.example.ui.components.TokokuhConfirmDialog
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.components.TokokuhStatusBadge
import com.example.ui.components.TokokuhTopBar
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
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProductListScreen(
    viewModel: TokokuhViewModel,
    initialPreFillBarcode: String? = null
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Produk, 1: Kategori
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }

    // Dialog states
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductDialog by remember { mutableStateOf(initialPreFillBarcode != null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var productForStockAdjust by remember { mutableStateOf<ProductEntity?>(null) }

    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredProducts = products.filter { product ->
        val matchCat = selectedCategoryFilter == null || product.categoryId == selectedCategoryFilter
        val matchQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery, ignoreCase = true)
        matchCat && matchQuery
    }

    ScaffoldWrapper(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        productToEdit = null
                        showProductDialog = true
                    } else {
                        categoryToEdit = null
                        showCategoryDialog = true
                    }
                },
                containerColor = TokokuhAccent,
                contentColor = Color(0xFF1E1E1E),
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TokokuhBackground)
        ) {
            TokokuhTopBar(title = "Manajemen Produk & Inventori")

            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TokokuhBackground,
                contentColor = TokokuhAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TokokuhAccent,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftar Produk (${products.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kategori (${categories.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // PRODUCTS TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("product_search_input"),
                        placeholder = { Text("Cari nama, SKU, atau barcode...", color = TokokuhTextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TokokuhAccent) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TokokuhTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary,
                            focusedContainerColor = TokokuhCard,
                            unfocusedContainerColor = TokokuhCard
                        ),
                        singleLine = true
                    )

                    // Categories chip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Semua") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TokokuhAccent,
                                    selectedLabelColor = Color(0xFF1E1E1E)
                                )
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.id,
                                onClick = {
                                    selectedCategoryFilter = if (selectedCategoryFilter == cat.id) null else cat.id
                                },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TokokuhAccent,
                                    selectedLabelColor = Color(0xFF1E1E1E)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredProducts.isEmpty()) {
                        TokokuhEmptyState(
                            icon = Icons.Default.Inventory,
                            title = "Tidak Ada Produk",
                            description = "Tambahkan produk pertama Anda dengan menekan tombol '+' di bawah.",
                            actionButtonText = "Tambah Produk Baru",
                            onActionClick = {
                                productToEdit = null
                                showProductDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val category = categories.find { it.id == product.categoryId }
                                ProductRowItem(
                                    product = product,
                                    categoryName = category?.name ?: "Tanpa Kategori",
                                    onEdit = {
                                        productToEdit = product
                                        showProductDialog = true
                                    },
                                    onAdjustStock = {
                                        productForStockAdjust = product
                                    },
                                    onDelete = {
                                        productToDelete = product
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // CATEGORIES TAB
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (categories.isEmpty()) {
                        TokokuhEmptyState(
                            icon = Icons.Default.Category,
                            title = "Belum Ada Kategori",
                            description = "Kategori membantu mengelompokkan produk di menu kasir.",
                            actionButtonText = "Tambah Kategori Baru",
                            onActionClick = {
                                categoryToEdit = null
                                showCategoryDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                CategoryRowItem(
                                    category = cat,
                                    productCount = products.count { it.categoryId == cat.id },
                                    onEdit = {
                                        categoryToEdit = cat
                                        showCategoryDialog = true
                                    },
                                    onDelete = {
                                        categoryToDelete = cat
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showProductDialog) {
        ProductFormDialog(
            initialProduct = productToEdit,
            preFillBarcode = initialPreFillBarcode,
            categories = categories,
            onDismiss = { showProductDialog = false },
            onSave = { name, sku, barcode, catId, cost, sell, stock, unit, imgUri, isActive, isUnlimited, minAlert ->
                viewModel.saveProduct(
                    id = productToEdit?.id ?: 0L,
                    name = name,
                    sku = sku,
                    barcode = barcode,
                    categoryId = catId,
                    costPrice = cost,
                    sellingPrice = sell,
                    stock = stock,
                    unit = unit,
                    imageUri = imgUri,
                    isActive = isActive,
                    isUnlimitedStock = isUnlimited,
                    minStockAlert = minAlert,
                    onSuccess = { showProductDialog = false }
                )
            }
        )
    }

    // Adjust Stock Dialog
    if (productForStockAdjust != null) {
        val prod = productForStockAdjust!!
        AdjustStockDialog(
            product = prod,
            onDismiss = { productForStockAdjust = null },
            onConfirm = { changeAmount, reason ->
                viewModel.adjustStock(prod.id, changeAmount, reason)
                productForStockAdjust = null
            }
        )
    }

    // Confirm Delete Product
    if (productToDelete != null) {
        val prod = productToDelete!!
        TokokuhConfirmDialog(
            title = "Hapus Produk?",
            message = "Apakah Anda yakin ingin menghapus '${prod.name}'? Riwayat transaksi lama akan tetap tersimpan.",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteProduct(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    // Add / Edit Category Dialog
    if (showCategoryDialog) {
        CategoryFormDialog(
            category = categoryToEdit,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, iconName, colorHex ->
                viewModel.saveCategory(categoryToEdit?.id ?: 0L, name, iconName, colorHex) {
                    showCategoryDialog = false
                }
            }
        )
    }

    // Confirm Delete Category
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        TokokuhConfirmDialog(
            title = "Hapus Kategori?",
            message = "Apakah Anda yakin ingin menghapus kategori '${cat.name}'? Produk di kategori ini tidak akan dihapus dan akan menjadi 'Tanpa Kategori'.",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCategory(cat)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}

@Composable
fun ProductRowItem(
    product: ProductEntity,
    categoryName: String,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
        border = BorderStroke(1.dp, TokokuhSecondary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image / Initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        color = TokokuhTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!product.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TokokuhStatusBadge(text = "Nonaktif", color = TokokuhError)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryName • Jual: ${CurrencyUtils.formatRupiah(product.sellingPrice)} (Modal: ${CurrencyUtils.formatRupiah(product.costPrice)})",
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stockColor = if (product.isUnlimitedStock) TokokuhSuccess
                    else if (product.stock <= 0) TokokuhError
                    else if (product.stock <= product.minStockAlert) TokokuhWarning
                    else TokokuhTextPrimary

                    val stockLabel = if (product.isUnlimitedStock) "Stok: Tak Terbatas"
                    else "Stok: ${product.stock} ${product.unit}"

                    Text(
                        text = stockLabel,
                        color = stockColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!product.barcode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BC: ${product.barcode}",
                            color = TokokuhTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TokokuhTextSecondary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(TokokuhCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Produk", color = TokokuhTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TokokuhAccent) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sesuaikan Stok (+/-)", color = TokokuhTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = TokokuhSuccess) },
                        onClick = {
                            menuExpanded = false
                            onAdjustStock()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus Produk", color = TokokuhError) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TokokuhError) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryRowItem(
    category: CategoryEntity,
    productCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TokokuhSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = TokokuhAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.name,
                        color = TokokuhTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$productCount Produk",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TokokuhAccent)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TokokuhError)
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity?,
    preFillBarcode: String? = null,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String, sku: String, barcode: String, categoryId: Long?,
        costPrice: Long, sellingPrice: Long, stock: Int, unit: String,
        imageUri: String?, isActive: Boolean, isUnlimitedStock: Boolean, minStockAlert: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: (preFillBarcode ?: "")) }
    var categoryId by remember { mutableStateOf(initialProduct?.categoryId) }
    var costPriceInput by remember { mutableStateOf((initialProduct?.costPrice ?: 0L).toString()) }
    var sellingPriceInput by remember { mutableStateOf((initialProduct?.sellingPrice ?: 0L).toString()) }
    var stockInput by remember { mutableStateOf((initialProduct?.stock ?: 0).toString()) }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var imageUri by remember { mutableStateOf(initialProduct?.imageUri) }
    var isActive by remember { mutableStateOf(initialProduct?.isActive ?: true) }
    var isUnlimitedStock by remember { mutableStateOf(initialProduct?.isUnlimitedStock ?: false) }
    var minStockAlertInput by remember { mutableStateOf((initialProduct?.minStockAlert ?: 5).toString()) }

    var showCameraScanner by remember { mutableStateOf(false) }

    // Android Photo Picker Launcher (zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Save to internal app storage for permanence
            try {
                val input = context.contentResolver.openInputStream(uri)
                val imagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
                val targetFile = File(imagesDir, "prod_${System.currentTimeMillis()}.png")
                FileOutputStream(targetFile).use { fos -> input?.copyTo(fos) }
                imageUri = targetFile.absolutePath
            } catch (_: Exception) { }
        }
    }

    if (showCameraScanner) {
        CameraScannerView(
            onBarcodeDetected = { scanned ->
                barcode = scanned
                showCameraScanner = false
            },
            onClose = { showCameraScanner = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (initialProduct == null) "Tambah Produk Baru" else "Edit Produk",
                color = TokokuhTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image Picker Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TokokuhSecondary)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Gambar Produk",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Ubah Gambar",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TokokuhAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pilih Foto dari Galeri (Opsional)", color = TokokuhTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // Nama Produk
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    )
                )

                // SKU & Barcode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Kode") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = TokokuhAccent)
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )
                }

                // Kategori Selector
                Text("Kategori Produk:", color = TokokuhTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = categoryId == null,
                            onClick = { categoryId = null },
                            label = { Text("Tanpa Kategori", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TokokuhAccent,
                                selectedLabelColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id },
                            label = { Text(cat.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TokokuhAccent,
                                selectedLabelColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }

                // Harga Jual & Harga Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPriceInput,
                        onValueChange = { sellingPriceInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Jual (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sell_price_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )

                    OutlinedTextField(
                        value = costPriceInput,
                        onValueChange = { costPriceInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Modal / HPP (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )
                }

                // Stok & Satuan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Stok") },
                        enabled = !isUnlimitedStock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )
                }

                // Unlimited Stock & Min Stock Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Stok Tidak Terbatas", color = TokokuhTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isUnlimitedStock,
                        onCheckedChange = { isUnlimitedStock = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TokokuhAccent, checkedTrackColor = TokokuhSecondary)
                    )
                }

                if (!isUnlimitedStock) {
                    OutlinedTextField(
                        value = minStockAlertInput,
                        onValueChange = { minStockAlertInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Peringatan Batas Stok Minimum") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        )
                    )
                }

                // Status Aktif Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status Produk Aktif", color = TokokuhTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TokokuhAccent, checkedTrackColor = TokokuhSecondary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = CurrencyUtils.parseRupiah(costPriceInput)
                        val sell = CurrencyUtils.parseRupiah(sellingPriceInput)
                        val st = if (isUnlimitedStock) 999999 else (stockInput.toIntOrNull() ?: 0)
                        val minAlert = minStockAlertInput.toIntOrNull() ?: 5
                        onSave(name, sku, barcode, categoryId, cost, sell, st, unit, imageUri, isActive, isUnlimitedStock, minAlert)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan Produk", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AdjustStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (changeAmount: Int, reason: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(true) } // true: tambah, false: kurangi
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sesuaikan Stok: ${product.name}", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Stok saat ini: ${product.stock} ${product.unit}", color = TokokuhAccent, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isAddition = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddition) TokokuhSuccess else TokokuhSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Tambah Stok")
                    }
                    Button(
                        onClick = { isAddition = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isAddition) TokokuhError else TokokuhSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("- Kurangi Stok")
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah ${if (isAddition) "Penambahan" else "Pengurangan"}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    )
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Penyesuaian (contoh: Barang Masuk, Rusak)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toIntOrNull() ?: 0
                    if (amt > 0) {
                        val change = if (isAddition) amt else -amt
                        onConfirm(change, reason.ifBlank { if (isAddition) "Penambahan Stok" else "Pengurangan Stok" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Terapkan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = TokokuhCard
    )
}

@Composable
fun CategoryFormDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (category == null) "Tambah Kategori" else "Edit Kategori", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, "category", null)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = TokokuhCard
    )
}

@Composable
fun ScaffoldWrapper(
    floatingActionButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp)
        ) {
            floatingActionButton()
        }
    }
}
