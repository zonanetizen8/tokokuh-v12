package com.example.ui.employees

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.EmployeeEntity
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
import com.example.ui.theme.TokokuhSuccess
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.viewmodel.TokokuhViewModel

@Composable
fun EmployeeScreen(
    viewModel: TokokuhViewModel
) {
    val employees by viewModel.employees.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }

    val activeCashier = settings?.selectedCashierName ?: "Pilih Kasir"

    ScaffoldWrapper(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    employeeToEdit = null
                    showEmployeeDialog = true
                },
                containerColor = TokokuhAccent,
                contentColor = Color(0xFF1E1E1E),
                modifier = Modifier.testTag("add_employee_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Karyawan")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TokokuhBackground)
        ) {
            TokokuhTopBar(title = "Manajemen Karyawan")

            // Active Cashier Banner
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TokokuhAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TokokuhAccent)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Kasir Aktif Bertugas Sekarang:", color = TokokuhTextSecondary, fontSize = 12.sp)
                        Text(activeCashier, color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Nama kasir ini akan tercatat di setiap struk dan laporan shift.", color = TokokuhTextSecondary, fontSize = 11.sp)
                    }
                }
            }

            if (employees.isEmpty()) {
                TokokuhEmptyState(
                    icon = Icons.Default.Person,
                    title = "Belum Ada Karyawan",
                    description = "Daftarkan nama kasir atau supervisor toko Anda.",
                    actionButtonText = "Tambah Karyawan Baru",
                    onActionClick = {
                        employeeToEdit = null
                        showEmployeeDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(employees, key = { it.id }) { emp ->
                        val isCurrentCashier = emp.name == activeCashier
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentCashier) TokokuhAccent.copy(alpha = 0.1f) else TokokuhCard
                            ),
                            border = BorderStroke(1.dp, if (isCurrentCashier) TokokuhAccent else TokokuhSecondary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    IconButton(
                                        onClick = {
                                            if (emp.isActive) {
                                                viewModel.selectActiveCashier(emp.name)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentCashier) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Pilih Kasir",
                                            tint = if (isCurrentCashier) TokokuhAccent else TokokuhTextSecondary
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(emp.name, color = TokokuhTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TokokuhStatusBadge(text = emp.role, color = TokokuhAccent)
                                        }
                                        if (!emp.phone.isNullOrBlank()) {
                                            Text("Telp: ${emp.phone}", color = TokokuhTextSecondary, fontSize = 12.sp)
                                        }
                                        if (!emp.isActive) {
                                            Text("Status: Nonaktif", color = TokokuhError, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            employeeToEdit = emp
                                            showEmployeeDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TokokuhAccent)
                                    }
                                    IconButton(onClick = { employeeToDelete = emp }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TokokuhError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Employee Dialog
    if (showEmployeeDialog) {
        EmployeeFormDialog(
            employee = employeeToEdit,
            onDismiss = { showEmployeeDialog = false },
            onSave = { name, role, phone ->
                viewModel.saveEmployee(employeeToEdit?.id ?: 0L, name, role, phone) {
                    showEmployeeDialog = false
                }
            }
        )
    }

    // Confirm Delete
    if (employeeToDelete != null) {
        val emp = employeeToDelete!!
        TokokuhConfirmDialog(
            title = "Hapus Karyawan?",
            message = "Apakah Anda yakin ingin menghapus '${emp.name}'?",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteEmployee(emp)
                employeeToDelete = null
            },
            onDismiss = { employeeToDelete = null }
        )
    }
}

@Composable
fun EmployeeFormDialog(
    employee: EmployeeEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var role by remember { mutableStateOf(employee?.role ?: "Kasir") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }

    val roles = listOf("Kasir", "Supervisor", "Owner")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text(if (employee == null) "Tambah Karyawan Baru" else "Edit Karyawan", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Karyawan *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input"),
                    singleLine = true
                )

                Text("Jabatan / Peran:", color = TokokuhTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(roles) { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TokokuhAccent,
                                selectedLabelColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, role, phone.ifBlank { null })
                },
                enabled = name.isNotBlank(),
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
