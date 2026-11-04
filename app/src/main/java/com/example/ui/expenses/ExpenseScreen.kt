package com.example.ui.expenses

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.database.entity.ExpenseEntity
import com.example.ui.components.TokokuhConfirmDialog
import com.example.ui.components.TokokuhEmptyState
import com.example.ui.components.TokokuhStatusBadge
import com.example.ui.components.TokokuhTopBar
import com.example.ui.products.ScaffoldWrapper
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhError
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.utils.CurrencyUtils
import com.example.viewmodel.TokokuhViewModel

@Composable
fun ExpenseScreen(
    viewModel: TokokuhViewModel
) {
    val expenses by viewModel.expenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    ScaffoldWrapper(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TokokuhAccent,
                contentColor = Color(0xFF1E1E1E),
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Catat Pengeluaran")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TokokuhBackground)
        ) {
            TokokuhTopBar(title = "Pencatatan Biaya & Pengeluaran")

            val totalExpenses = expenses.sumOf { it.amount }

            // Header summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                border = BorderStroke(1.dp, TokokuhSecondary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Pengeluaran Dicatat", color = TokokuhTextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupiah(totalExpenses),
                            color = TokokuhError,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(TokokuhError.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoneyOff, contentDescription = null, tint = TokokuhError)
                    }
                }
            }

            if (expenses.isEmpty()) {
                TokokuhEmptyState(
                    icon = Icons.Default.MoneyOff,
                    title = "Belum Ada Pengeluaran",
                    description = "Catat biaya operasional toko seperti listrik, kemasan, atau transportasi.",
                    actionButtonText = "Catat Pengeluaran Baru",
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseRowItem(
                            expense = expense,
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat, amt, method, note ->
                viewModel.addExpense(title, cat, amt, method, note) {
                    showAddDialog = false
                }
            }
        )
    }

    // Confirm Delete
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        TokokuhConfirmDialog(
            title = "Hapus Pengeluaran?",
            message = "Apakah Anda yakin ingin menghapus '${exp.title}' (${CurrencyUtils.formatRupiah(exp.amount)})?",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteExpense(exp)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@Composable
fun ExpenseRowItem(
    expense: ExpenseEntity,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${expense.category} • ${CurrencyUtils.formatDateTime(expense.createdAt)} • ${expense.cashierName}",
                    color = TokokuhTextSecondary,
                    fontSize = 12.sp
                )
                if (!expense.note.isNullOrBlank()) {
                    Text("Catatan: ${expense.note}", color = TokokuhTextSecondary, fontSize = 11.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyUtils.formatRupiah(expense.amount),
                    color = TokokuhError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                TokokuhStatusBadge(text = expense.paymentMethod, color = TokokuhAccent)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TokokuhTextSecondary)
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amount: Long, paymentMethod: String, note: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Operasional") }
    var amountInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    val categories = listOf("Operasional", "Listrik & Air", "Transportasi", "Kemasan & Plastik", "Gaji", "Bahan Baku", "Lain-lain")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Catat Pengeluaran Baru", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Pengeluaran (contoh: Token Listrik) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    singleLine = true
                )

                Text("Kategori:", color = TokokuhTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TokokuhAccent,
                                selectedLabelColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Biaya (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    singleLine = true
                )

                Text("Metode Pembayaran:", color = TokokuhTextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paymentMethod == "CASH",
                        onClick = { paymentMethod = "CASH" },
                        label = { Text("Uang Laci (CASH)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TokokuhAccent,
                            selectedLabelColor = Color(0xFF1E1E1E)
                        )
                    )
                    FilterChip(
                        selected = paymentMethod == "NON_CASH",
                        onClick = { paymentMethod = "NON_CASH" },
                        label = { Text("Non-Tunai / Rekening") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TokokuhAccent,
                            selectedLabelColor = Color(0xFF1E1E1E)
                        )
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = CurrencyUtils.parseRupiah(amountInput)
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, selectedCategory, amt, paymentMethod, note.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank() && CurrencyUtils.parseRupiah(amountInput) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent)
            ) {
                Text("Simpan Pengeluaran", color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
