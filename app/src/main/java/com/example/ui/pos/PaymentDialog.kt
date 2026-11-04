package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhError
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhSuccess
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.utils.CurrencyUtils

@Composable
fun PaymentDialog(
    totalAmount: Long,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSubmitPayment: (paymentType: String, paymentDetails: String, amountPaid: Long, reference: String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cash, 1: Non-Cash
    var cashPaidInput by remember { mutableStateOf(totalAmount.toString()) }
    var nonCashMethod by remember { mutableStateOf("QRIS") } // QRIS or TRANSFER
    var nonCashReference by remember { mutableStateOf("") }

    val cashPaid = remember(cashPaidInput) {
        CurrencyUtils.parseRupiah(cashPaidInput)
    }

    val changeAmount by remember {
        derivedStateOf {
            (cashPaid - totalAmount).coerceAtLeast(0L)
        }
    }

    val isCashValid by remember {
        derivedStateOf { cashPaid >= totalAmount }
    }

    // Cash presets
    val presets = remember(totalAmount) {
        val list = mutableListOf<Long>()
        list.add(totalAmount) // Pas
        val roundUp10k = ((totalAmount + 9999) / 10000) * 10000
        if (roundUp10k > totalAmount && !list.contains(roundUp10k)) list.add(roundUp10k)
        val roundUp50k = ((totalAmount + 49999) / 50000) * 50000
        if (roundUp50k > totalAmount && !list.contains(roundUp50k)) list.add(roundUp50k)
        val roundUp100k = ((totalAmount + 99999) / 100000) * 100000
        if (roundUp100k > totalAmount && !list.contains(roundUp100k)) list.add(roundUp100k)
        list
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pembayaran",
                    color = TokokuhTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Tagihan: ${CurrencyUtils.formatRupiah(totalAmount)}",
                    color = TokokuhAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Selection: Tunai vs Non-Tunai
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TokokuhSecondary,
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
                                Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TUNAI", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NON-TUNAI", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // TUNAI
                    Text(
                        text = "Uang Diterima dari Pelanggan:",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = cashPaidInput,
                        onValueChange = { cashPaidInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_cash_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TokokuhTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        leadingIcon = {
                            Text("Rp", color = TokokuhAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        ),
                        singleLine = true
                    )

                    // Presets
                    Text(
                        text = "Nominal Cepat:",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (preset in presets.take(4)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (cashPaid == preset) TokokuhAccent.copy(alpha = 0.2f) else TokokuhSecondary,
                                border = BorderStroke(1.dp, if (cashPaid == preset) TokokuhAccent else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { cashPaidInput = preset.toString() }
                            ) {
                                Text(
                                    text = if (preset == totalAmount) "Uang Pas" else CurrencyUtils.formatRupiah(preset).replace("Rp", ""),
                                    color = if (cashPaid == preset) TokokuhAccent else TokokuhTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // Kembalian Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCashValid) Color(0xFF1B3820) else Color(0xFF3E1818)
                        ),
                        border = BorderStroke(1.dp, if (isCashValid) TokokuhSuccess else TokokuhError)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isCashValid) "Kembalian Pelanggan:" else "Uang Kurang:",
                                color = if (isCashValid) TokokuhSuccess else TokokuhError,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isCashValid) CurrencyUtils.formatRupiah(changeAmount) else CurrencyUtils.formatRupiah(totalAmount - cashPaid),
                                color = if (isCashValid) Color.White else TokokuhError,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                } else {
                    // NON-TUNAI (QRIS / TRANSFER)
                    Text(
                        text = "Metode Non-Tunai:",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (nonCashMethod == "QRIS") TokokuhAccent.copy(alpha = 0.2f) else TokokuhSecondary,
                            border = BorderStroke(1.dp, if (nonCashMethod == "QRIS") TokokuhAccent else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nonCashMethod = "QRIS" }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = TokokuhAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("QRIS", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (nonCashMethod == "TRANSFER") TokokuhAccent.copy(alpha = 0.2f) else TokokuhSecondary,
                            border = BorderStroke(1.dp, if (nonCashMethod == "TRANSFER") TokokuhAccent else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nonCashMethod = "TRANSFER" }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = TokokuhAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("TRANSFER", color = TokokuhTextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nonCashReference,
                        onValueChange = { nonCashReference = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_ref_input"),
                        label = { Text("No. Referensi / Bank / Catatan (Opsional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokokuhAccent,
                            unfocusedBorderColor = TokokuhSecondary
                        ),
                        singleLine = true
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TokokuhSecondary
                    ) {
                        Text(
                            text = "Pencatatan manual kasir. Pastikan dana sudah masuk ke rekening atau aplikasi QRIS toko sebelum konfirmasi.",
                            color = TokokuhTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        if (isCashValid) {
                            onSubmitPayment("CASH", "CASH", cashPaid, null)
                        }
                    } else {
                        onSubmitPayment("NON_CASH", nonCashMethod, totalAmount, nonCashReference.ifBlank { null })
                    }
                },
                enabled = !isProcessing && (selectedTab != 0 || isCashValid),
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("submit_payment_button")
            ) {
                Text(
                    text = if (isProcessing) "Memproses..." else "Konfirmasi Bayar",
                    color = Color(0xFF1E1E1E),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhTextPrimary),
                border = BorderStroke(1.dp, TokokuhSecondary)
            ) {
                Text("Batal")
            }
        }
    )
}
