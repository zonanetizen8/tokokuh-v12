package com.example.ui.shifts

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.ShiftEntity
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.components.TokokuhStatusBadge
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

@Composable
fun ShiftScreen(
    viewModel: TokokuhViewModel
) {
    val activeShift by viewModel.activeShift.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val employees by viewModel.activeEmployees.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showCashMovementDialog by remember { mutableStateOf(false) }
    var isCashIn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(title = "Manajemen Shift Kasir")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Shift Card
            item {
                if (activeShift == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                        border = BorderStroke(1.dp, TokokuhSecondary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(TokokuhSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TokokuhWarning, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak Ada Shift Aktif",
                                color = TokokuhTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Buka shift baru sebelum kasir memulai melayani transaksi penjualan.",
                                color = TokokuhTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showOpenShiftDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("open_shift_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Shift Sekarang", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val shift = activeShift!!
                    val expectedCash = (shift.initialCash + shift.totalCashSales - shift.totalExpensesCash + shift.cashIn - shift.cashOut).coerceAtLeast(0L)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3820)),
                        border = BorderStroke(1.dp, TokokuhSuccess)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = TokokuhSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SHIFT SEDANG AKTIF", color = TokokuhSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                TokokuhStatusBadge(text = shift.cashierName, color = TokokuhAccent)
                            }

                            Divider(color = TokokuhSuccess.copy(alpha = 0.3f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dibuka Sejak:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(CurrencyUtils.formatDateTime(shift.openedAt), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Modal Awal Laci:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(CurrencyUtils.formatRupiah(shift.initialCash), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Penjualan Tunai:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(CurrencyUtils.formatRupiah(shift.totalCashSales), color = TokokuhAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Penjualan Non-Tunai:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(CurrencyUtils.formatRupiah(shift.totalNonCashSales), color = Color.White, fontSize = 12.sp)
                            }
                            if (shift.totalExpensesCash > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pengeluaran Tunai Laci:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("-${CurrencyUtils.formatRupiah(shift.totalExpensesCash)}", color = TokokuhError, fontSize = 12.sp)
                                }
                            }
                            if (shift.cashIn > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kas Masuk Tambahan:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("+${CurrencyUtils.formatRupiah(shift.cashIn)}", color = TokokuhSuccess, fontSize = 12.sp)
                                }
                            }
                            if (shift.cashOut > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kas Keluar (Ditarik):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("-${CurrencyUtils.formatRupiah(shift.cashOut)}", color = TokokuhWarning, fontSize = 12.sp)
                                }
                            }

                            Divider(color = TokokuhSuccess.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Estimasi Kas di Laci:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(CurrencyUtils.formatRupiah(expectedCash), color = TokokuhAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons: Kas Masuk, Kas Keluar, Tutup Shift
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isCashIn = true
                                        showCashMovementDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kas Masuk", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isCashIn = false
                                        showCashMovementDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kas Keluar", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { showCloseShiftDialog = true },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1E1E1E), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tutup Shift", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Past Shifts History
            item {
                Text("Riwayat Shift Sebelumnya", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            val closedShifts = allShifts.filter { it.status == "CLOSED" }
            if (closedShifts.isEmpty()) {
                item {
                    Text("Belum ada riwayat shift yang ditutup.", color = TokokuhTextSecondary, fontSize = 13.sp)
                }
            } else {
                items(closedShifts) { shift ->
                    ShiftHistoryCard(shift = shift)
                }
            }
        }
    }

    // Open Shift Dialog
    if (showOpenShiftDialog) {
        OpenShiftDialog(
            employees = employees.map { it.name },
            initialCashier = settings?.selectedCashierName ?: "Pilih Kasir",
            onDismiss = { showOpenShiftDialog = false },
            onConfirm = { cashier, initialCash, note ->
                viewModel.openShift(initialCash, note) {
                    showOpenShiftDialog = false
                }
            }
        )
    }

    // Close Shift Dialog
    if (showCloseShiftDialog && activeShift != null) {
        val shift = activeShift!!
        val expectedCash = (shift.initialCash + shift.totalCashSales - shift.totalExpensesCash + shift.cashIn - shift.cashOut).coerceAtLeast(0L)
        CloseShiftDialog(
            shift = shift,
            expectedCash = expectedCash,
            onDismiss = { showCloseShiftDialog = false },
            onConfirm = { actualCash, note ->
                viewModel.closeShift(shift.id, actualCash, note) {
                    showCloseShiftDialog = false
                }
            }
        )
    }

    // Cash Movement Dialog (Kas Masuk / Keluar)
    if (showCashMovementDialog && activeShift != null) {
        CashMovementDialog(
            isCashIn = isCashIn,
            onDismiss = { showCashMovementDialog = false },
            onConfirm = { amount, reason ->
                viewModel.recordCashMovement(activeShift!!.id, isCashIn, amount, reason)
                showCashMovementDialog = false
            }
        )
    }
}

@Composable
fun ShiftHistoryCard(shift: ShiftEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
        border = BorderStroke(1.dp, TokokuhSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kasir: ${shift.cashierName}",
                    color = TokokuhTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                val diff = shift.cashDifference ?: 0L
                val diffColor = if (diff == 0L) TokokuhSuccess else if (diff < 0) TokokuhError else TokokuhWarning
                val diffText = if (diff == 0L) "Pas (Rp0)" else if (diff > 0) "+${CurrencyUtils.formatRupiah(diff)}" else CurrencyUtils.formatRupiah(diff)

                TokokuhStatusBadge(text = "Selisih: $diffText", color = diffColor)
            }

            Text(
                text = "Buka: ${CurrencyUtils.formatDateTime(shift.openedAt)} s/d ${if (shift.closedAt != null) CurrencyUtils.formatDateTime(shift.closedAt) else "-"}",
                color = TokokuhTextSecondary,
                fontSize = 12.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Penjualan Tunai: ${CurrencyUtils.formatRupiah(shift.totalCashSales)}", color = TokokuhTextSecondary, fontSize = 12.sp)
                Text("Kas Fisik: ${CurrencyUtils.formatRupiah(shift.actualCash ?: 0L)}", color = TokokuhAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (!shift.closeNote.isNullOrBlank()) {
                Text(
                    text = "Catatan: ${shift.closeNote}",
                    color = TokokuhTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun OpenShiftDialog(
    employees: List<String>,
    initialCashier: String,
    onDismiss: () -> Unit,
    onConfirm: (cashierName: String, initialCash: Long, note: String?) -> Unit
) {
    var selectedCashier by remember { mutableStateOf(if (employees.contains(initialCashier)) initialCashier else (employees.firstOrNull() ?: "Kasir")) }
    var initialCashInput by remember { mutableStateOf("100000") }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Buka Shift Kasir Baru", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pilih Kasir Bertugas:", color = TokokuhTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(employees) { name ->
                        FilterChip(
                            selected = selectedCashier == name,
                            onClick = { selectedCashier = name },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TokokuhAccent,
                                selectedLabelColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = initialCashInput,
                    onValueChange = { initialCashInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Modal Awal Uang Laci (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Catatan Buka Shift (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = CurrencyUtils.parseRupiah(initialCashInput)
                    onConfirm(selectedCashier, cash, noteInput.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Mulai Shift", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CloseShiftDialog(
    shift: ShiftEntity,
    expectedCash: Long,
    onDismiss: () -> Unit,
    onConfirm: (actualCash: Long, note: String?) -> Unit
) {
    var actualCashInput by remember { mutableStateOf(expectedCash.toString()) }
    var closeNoteInput by remember { mutableStateOf("") }

    val actualCash = CurrencyUtils.parseRupiah(actualCashInput)
    val diff = actualCash - expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Tutup Shift Kasir", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kasir: ${shift.cashierName}", color = TokokuhAccent, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kas Seharusnya di Laci:", color = TokokuhTextSecondary)
                    Text(CurrencyUtils.formatRupiah(expectedCash), color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = actualCashInput,
                    onValueChange = { actualCashInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Hitung Uang Fisik Aktual di Laci (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TokokuhAccent,
                        unfocusedBorderColor = TokokuhSecondary
                    ),
                    singleLine = true
                )

                // Selisih
                val diffColor = if (diff == 0L) TokokuhSuccess else if (diff < 0) TokokuhError else TokokuhWarning
                val diffText = if (diff == 0L) "Pas (Tidak ada selisih)" else if (diff > 0) "Lebih: +${CurrencyUtils.formatRupiah(diff)}" else "Kurang: ${CurrencyUtils.formatRupiah(diff)}"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, diffColor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Status Selisih Kas:", color = diffColor, fontSize = 12.sp)
                        Text(diffText, color = diffColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value = closeNoteInput,
                    onValueChange = { closeNoteInput = it },
                    label = { Text("Catatan Tutup Shift (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash, closeNoteInput.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Tutup Shift & Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CashMovementDialog(
    isCashIn: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, reason: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(if (isCashIn) "Kas Masuk Laci" else "Kas Keluar (Tarik Uang)", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isCashIn) "Penambahan uang modal cadangan ke laci kasir." else "Pengambilan uang tunai dari laci (misal setoran ke brankas/owner).",
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah Uang (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Keterangan *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = CurrencyUtils.parseRupiah(amountInput)
                    if (amt > 0) onConfirm(amt, reasonInput.ifBlank { if (isCashIn) "Penambahan kas" else "Pengambilan kas" })
                },
                enabled = CurrencyUtils.parseRupiah(amountInput) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Simpan", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
