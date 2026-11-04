package com.example.ui.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TokokuhStatusBadge
import com.example.ui.components.TokokuhTopBar
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhSuccess
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.viewmodel.TokokuhViewModel

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

@SuppressLint("MissingPermission")
@Composable
fun PrinterScreen(
    viewModel: TokokuhViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()

    var pairedDevices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var hasBluetoothPermission by remember { mutableStateOf(false) }

    // Bluetooth permission launcher
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }
        hasBluetoothPermission = granted
        if (granted) {
            pairedDevices = loadPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            hasBluetoothPermission = true
            pairedDevices = loadPairedDevices()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(
            title = "Pengaturan Printer Thermal",
            actions = {
                IconButton(
                    onClick = { pairedDevices = loadPairedDevices() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Segarkan", tint = TokokuhAccent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Printer Card
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TokokuhSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = TokokuhAccent)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Printer Aktif Saat Ini", color = TokokuhTextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = settings?.lastPrinterName ?: "Belum Ada Printer Dipilih",
                                        color = TokokuhTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            if (!settings?.lastPrinterAddress.isNullOrEmpty()) {
                                TokokuhStatusBadge(text = "Tersimpan", color = TokokuhSuccess)
                            }
                        }

                        if (!settings?.lastPrinterAddress.isNullOrEmpty()) {
                            Text(
                                text = "Alamat MAC: ${settings?.lastPrinterAddress}",
                                color = TokokuhTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Divider(color = TokokuhSecondary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Format Kertas: 58mm (Default)", color = TokokuhTextSecondary, fontSize = 13.sp)

                            Button(
                                onClick = {
                                    val addr = settings?.lastPrinterAddress
                                    if (!addr.isNullOrEmpty()) {
                                        viewModel.testPrint(addr)
                                    }
                                },
                                enabled = !settings?.lastPrinterAddress.isNullOrEmpty() && !isPrinting,
                                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("test_print_button")
                            ) {
                                Text(
                                    text = if (isPrinting) "Mencetak..." else "Tes Cetak Struk",
                                    color = Color(0xFF1E1E1E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Status message banner
            if (!printerStatus.isNullOrEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TokokuhSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = printerStatus!!,
                            color = TokokuhAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Paired Devices List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Perangkat Bluetooth Terpasang (Paired)",
                        color = TokokuhTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (pairedDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tidak ada perangkat Bluetooth printer yang dipasangkan (paired).",
                                color = TokokuhTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Pengaturan Bluetooth HP")
                            }
                        }
                    }
                }
            } else {
                items(pairedDevices) { device ->
                    val isSelected = settings?.lastPrinterAddress == device.address
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cur = settings ?: return@clickable
                                viewModel.updateSettings(
                                    cur.copy(lastPrinterAddress = device.address, lastPrinterName = device.name)
                                ) {}
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) TokokuhAccent.copy(alpha = 0.12f) else TokokuhCard
                        ),
                        border = BorderStroke(1.dp, if (isSelected) TokokuhAccent else TokokuhSecondary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = device.name,
                                    color = if (isSelected) TokokuhAccent else TokokuhTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = device.address,
                                    color = TokokuhTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Terpilih",
                                    tint = TokokuhAccent
                                )
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val cur = settings ?: return@OutlinedButton
                                        viewModel.updateSettings(
                                            cur.copy(lastPrinterAddress = device.address, lastPrinterName = device.name)
                                        ) {}
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pilih", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun loadPairedDevices(): List<BluetoothDeviceInfo> {
    val list = mutableListOf<BluetoothDeviceInfo>()
    try {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && adapter.isEnabled) {
            val bonded = adapter.bondedDevices
            if (bonded != null) {
                for (d in bonded) {
                    list.add(BluetoothDeviceInfo(name = d.name ?: "Unknown Device", address = d.address))
                }
            }
        }
    } catch (_: Exception) { }
    return list
}
