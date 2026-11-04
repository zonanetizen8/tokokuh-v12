package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.StoreSettingsEntity
import com.example.ui.components.TokokuhConfirmDialog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: TokokuhViewModel,
    onNavigateToPrinter: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToShifts: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val settings by viewModel.storeSettings.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showStoreDialog by remember { mutableStateOf(false) }
    var showTaxDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // SAF Backup Launcher
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backupDataToUri(uri)
        }
    }

    // SAF Restore Launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(title = "Pengaturan Aplikasi")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Toko & Struk
            item {
                Text("Toko & Kasir", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                    border = BorderStroke(1.dp, TokokuhSecondary)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Store,
                            title = "Profil Toko & Struk",
                            subtitle = settings?.storeName ?: "Nama, alamat, header & footer struk",
                            onClick = { showStoreDialog = true },
                            testTag = "settings_store_profile"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.Percent,
                            title = "Pengaturan Pajak",
                            subtitle = if (settings?.taxEnabled == true) "${settings?.taxName} (${settings?.taxRatePercent}%) Aktif" else "Pajak Nonaktif",
                            onClick = { showTaxDialog = true },
                            testTag = "settings_tax"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.Print,
                            title = "Printer Thermal Bluetooth",
                            subtitle = settings?.lastPrinterName ?: "Hubungkan printer struk 58mm",
                            onClick = onNavigateToPrinter,
                            testTag = "settings_printer"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.People,
                            title = "Manajemen Karyawan",
                            subtitle = "Kasir, supervisor, dan hak akses",
                            onClick = onNavigateToEmployees,
                            testTag = "settings_employees"
                        )
                    }
                }
            }

            // Section 2: Operasional Kasir
            item {
                Text("Operasional & Keuangan", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                    border = BorderStroke(1.dp, TokokuhSecondary)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.LockClock,
                            title = "Buka & Tutup Shift",
                            subtitle = "Kelola modal laci dan selisih kas kasir",
                            onClick = onNavigateToShifts,
                            testTag = "settings_shifts"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.MoneyOff,
                            title = "Catat Biaya Pengeluaran",
                            subtitle = "Listrik, transportasi, kemasan, operasional",
                            onClick = onNavigateToExpenses,
                            testTag = "settings_expenses"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.History,
                            title = "Audit Log & Riwayat Aktivitas",
                            subtitle = "${auditLogs.size} catatan perubahan sistem tersimpan",
                            onClick = { showAuditLogsDialog = true },
                            testTag = "settings_audit_logs"
                        )
                    }
                }
            }

            // Section 3: Data & Cadangan
            item {
                Text("Penyimpanan & Cadangan Data", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                    border = BorderStroke(1.dp, TokokuhSecondary)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Backup,
                            title = "Backup Data Lengkap (JSON)",
                            subtitle = "Ekspor seluruh database produk, transaksi & foto",
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                backupLauncher.launch("TOKOKUH_BACKUP_$dateStr.json")
                            },
                            testTag = "settings_backup"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.Restore,
                            title = "Restore / Pulihkan Data",
                            subtitle = "Impor file backup JSON ke database lokal",
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            testTag = "settings_restore"
                        )
                    }
                }
            }

            // Section 4: Dukungan & Info
            item {
                Text("Tentang & Bantuan", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                    border = BorderStroke(1.dp, TokokuhSecondary)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Bantuan & Customer Service",
                            subtitle = "WhatsApp & Email resmi pengembang",
                            onClick = onNavigateToHelp,
                            testTag = "settings_help"
                        )
                        Divider(color = TokokuhSecondary)
                        SettingsMenuItem(
                            icon = Icons.Default.Info,
                            title = "Tentang Aplikasi TOKOKUH",
                            subtitle = "Versi 1.0.0 • Logis Denis Prabowo (Indramayu)",
                            onClick = onNavigateToAbout,
                            testTag = "settings_about"
                        )
                    }
                }
            }
        }
    }

    // Store & Receipt Profile Dialog
    if (showStoreDialog) {
        val context = LocalContext.current
        val cur = settings ?: StoreSettingsEntity()
        var storeName by remember { mutableStateOf(cur.storeName) }
        var storeAddress by remember { mutableStateOf(cur.storeAddress) }
        var storePhone by remember { mutableStateOf(cur.storePhone) }
        var storeWebsite by remember { mutableStateOf(cur.storeWebsite) }
        var headerMsg by remember { mutableStateOf(cur.receiptHeaderMessage) }
        var footerMsg by remember { mutableStateOf(cur.receiptFooterMessage) }
        var logoPath by remember { mutableStateOf(cur.logoPath) }

        val logoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val dir = File(context.filesDir, "store_logo").apply { mkdirs() }
                    val targetFile = File(dir, "store_logo_${System.currentTimeMillis()}.png")
                    FileOutputStream(targetFile).use { fos -> input?.copyTo(fos) }
                    logoPath = targetFile.absolutePath
                } catch (_: Exception) { }
            }
        }

        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Pengaturan Toko & Struk", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logo Usaha Picker & Preview
                    Text("Logo Usaha (Dicetak di Bagian Paling Atas Struk):", color = TokokuhTextSecondary, fontSize = 12.sp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TokokuhSecondary)
                            .clickable {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!logoPath.isNullOrEmpty() && File(logoPath!!).exists()) {
                            AsyncImage(
                                model = logoPath,
                                contentDescription = "Logo Usaha",
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(90.dp),
                                contentScale = ContentScale.Fit
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable {
                                        logoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Ganti Logo",
                                        tint = Color.White,
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable { logoPath = null }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus Logo",
                                        tint = TokokuhError,
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = TokokuhAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sentuh untuk Upload Logo Toko (JPG/PNG)",
                                    color = TokokuhAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rekomendasi rasio 1:1, akan dicetak di atas struk 58mm",
                                    color = TokokuhTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko / Usaha *") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_store_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = storeAddress,
                        onValueChange = { storeAddress = it },
                        label = { Text("Alamat Toko") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = storePhone,
                        onValueChange = { storePhone = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = storeWebsite,
                        onValueChange = { storeWebsite = it },
                        label = { Text("Website (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = headerMsg,
                        onValueChange = { headerMsg = it },
                        label = { Text("Pesan Header Struk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = footerMsg,
                        onValueChange = { footerMsg = it },
                        label = { Text("Pesan Footer Struk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            cur.copy(
                                storeName = storeName.ifBlank { "TOKOKUH STORE" },
                                storeAddress = storeAddress,
                                storePhone = storePhone,
                                storeWebsite = storeWebsite,
                                receiptHeaderMessage = headerMsg,
                                receiptFooterMessage = footerMsg,
                                logoPath = logoPath
                            )
                        ) {
                            showStoreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                    modifier = Modifier.testTag("save_store_profile_button")
                ) {
                    Text("Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStoreDialog = false }) { Text("Batal") }
            }
        )
    }

    // Tax Settings Dialog
    if (showTaxDialog) {
        val cur = settings ?: StoreSettingsEntity()
        var taxEnabled by remember { mutableStateOf(cur.taxEnabled) }
        var taxName by remember { mutableStateOf(cur.taxName) }
        var taxRateInput by remember { mutableStateOf(cur.taxRatePercent.toString()) }
        var isInclusive by remember { mutableStateOf(cur.isTaxInclusive) }

        AlertDialog(
            onDismissRequest = { showTaxDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Pengaturan Pajak (PPN / PB1)", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Aktifkan Perhitungan Pajak", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = taxEnabled,
                            onCheckedChange = { taxEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TokokuhAccent, checkedTrackColor = TokokuhSecondary)
                        )
                    }

                    if (taxEnabled) {
                        OutlinedTextField(
                            value = taxName,
                            onValueChange = { taxName = it },
                            label = { Text("Nama Pajak (contoh: PPN)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = taxRateInput,
                            onValueChange = { taxRateInput = it },
                            label = { Text("Persentase Pajak (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Pajak Sudah Termasuk di Harga", color = TokokuhTextPrimary, fontSize = 13.sp)
                                Text("Inklusif (harga di kasir sudah termasuk pajak)", color = TokokuhTextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isInclusive,
                                onCheckedChange = { isInclusive = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = TokokuhAccent, checkedTrackColor = TokokuhSecondary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = taxRateInput.toDoubleOrNull() ?: 11.0
                        viewModel.updateSettings(
                            cur.copy(
                                taxEnabled = taxEnabled,
                                taxName = taxName.ifBlank { "PPN" },
                                taxRatePercent = rate,
                                isTaxInclusive = isInclusive
                            )
                        ) {
                            showTaxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
                ) {
                    Text("Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTaxDialog = false }) { Text("Batal") }
            }
        )
    }

    // Audit Logs Dialog
    if (showAuditLogsDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            containerColor = TokokuhCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Audit Log Sistem (${auditLogs.size})", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (auditLogs.isEmpty()) {
                    Text("Belum ada riwayat aktivitas sistem.", color = TokokuhTextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs.take(50)) { log ->
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
                                        Text(log.action, color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(CurrencyUtils.formatDateTime(log.createdAt), color = TokokuhTextSecondary, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(log.details, color = TokokuhTextPrimary, fontSize = 12.sp)
                                    if (!log.cashierName.isNullOrBlank()) {
                                        Text("Oleh: ${log.cashierName}", color = TokokuhTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAuditLogsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        TokokuhConfirmDialog(
            title = "Pulihkan Data dari Backup JSON?",
            message = "PERINGATAN: Memulihkan file backup akan menggantikan seluruh data database saat ini dengan data dari file backup. Pastikan Anda sudah membuat cadangan sebelum melanjutkan.",
            confirmText = "Ya, Pulihkan Sekarang",
            isDestructive = true,
            onConfirm = {
                val uri = pendingRestoreUri!!
                showRestoreConfirmDialog = false
                viewModel.restoreDataFromUri(uri)
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            }
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TokokuhSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TokokuhAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TokokuhTextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
