package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import java.io.File
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhSuccess
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary
import com.example.utils.CurrencyUtils

@Composable
fun ReceiptPreviewDialog(
    transaction: TransactionEntity,
    items: List<TransactionItemEntity>,
    settings: StoreSettingsEntity?,
    isPrinting: Boolean,
    printerStatus: String?,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val storeSettings = settings ?: StoreSettingsEntity()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TokokuhCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Struk Pembayaran",
                    color = TokokuhTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TokokuhSuccess.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TokokuhSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lunas",
                            color = TokokuhSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Paper simulation (White background for realistic preview)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Logo at the very top of receipt if uploaded
                        if (!storeSettings.logoPath.isNullOrEmpty() && File(storeSettings.logoPath).exists()) {
                            AsyncImage(
                                model = storeSettings.logoPath,
                                contentDescription = "Logo Usaha",
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(55.dp)
                                    .padding(bottom = 6.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Store Info
                        Text(
                            text = storeSettings.storeName,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                        if (storeSettings.storeAddress.isNotBlank()) {
                            Text(
                                text = storeSettings.storeAddress,
                                color = Color(0xFF555555),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (storeSettings.storePhone.isNotBlank()) {
                            Text(
                                text = "Telp: ${storeSettings.storePhone}",
                                color = Color(0xFF555555),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (storeSettings.receiptHeaderMessage.isNotBlank()) {
                            Text(
                                text = storeSettings.receiptHeaderMessage,
                                color = Color(0xFF555555),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        ReceiptDashedLine()

                        // Transaction Info
                        ReceiptLine(label = "No. Struk:", value = transaction.transactionNumber)
                        ReceiptLine(label = "Waktu:", value = CurrencyUtils.formatDateTime(transaction.createdAt))
                        ReceiptLine(label = "Kasir:", value = transaction.cashierName)
                        ReceiptLine(label = "Metode:", value = "${transaction.paymentType} (${transaction.paymentMethodDetails})")

                        ReceiptDashedLine()

                        // Items list
                        for (item in items) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.productName,
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${CurrencyUtils.formatRupiah(item.sellingPriceSnapshot)}",
                                        color = Color(0xFF666666),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = CurrencyUtils.formatRupiah(item.subtotal),
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (item.returnedQuantity > 0) {
                                    Text(
                                        text = "(Diretur: ${item.returnedQuantity} pcs)",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        ReceiptDashedLine()

                        // Totals
                        ReceiptLine(label = "Subtotal", value = CurrencyUtils.formatRupiah(transaction.subtotal))
                        if (transaction.discountAmount > 0L) {
                            ReceiptLine(label = "Diskon", value = "-${CurrencyUtils.formatRupiah(transaction.discountAmount)}")
                        }
                        if (transaction.taxAmount > 0L) {
                            ReceiptLine(label = "Pajak (${storeSettings.taxName})", value = CurrencyUtils.formatRupiah(transaction.taxAmount))
                        }

                        ReceiptDoubleLine()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(transaction.totalAmount),
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ReceiptLine(label = "Bayar", value = CurrencyUtils.formatRupiah(transaction.amountPaid))
                        if (transaction.paymentType == "CASH") {
                            ReceiptLine(label = "Kembalian", value = CurrencyUtils.formatRupiah(transaction.changeAmount))
                        }
                        if (!transaction.paymentReference.isNullOrBlank()) {
                            ReceiptLine(label = "Ref / Catatan", value = transaction.paymentReference)
                        }

                        ReceiptDashedLine()

                        // Footer Message
                        if (storeSettings.receiptFooterMessage.isNotBlank()) {
                            Text(
                                text = storeSettings.receiptFooterMessage,
                                color = Color(0xFF555555),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // MANDATORY OFFICIAL TOKOKUH CREDIT LOGO AT BOTTOM OF RECEIPT
                        Image(
                            painter = painterResource(id = R.drawable.logo_credit_struk),
                            contentDescription = "TOKOKUH Credit",
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(32.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Powered by TOKOKUH POS\nwww.tokokuh.com",
                            color = Color(0xFF777777),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!printerStatus.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TokokuhSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = printerStatus,
                            color = TokokuhAccent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrint,
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = TokokuhAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("print_receipt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = Color(0xFF1E1E1E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPrinting) "Mencetak..." else "Cetak Bluetooth",
                    color = Color(0xFF1E1E1E),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TokokuhTextPrimary),
                border = BorderStroke(1.dp, TokokuhSecondary),
                modifier = Modifier.testTag("dismiss_receipt_button")
            ) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF555555), fontSize = 11.sp)
        Text(text = value, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReceiptDashedLine() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        color = Color(0xFFCCCCCC),
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ReceiptDoubleLine() {
    Text(
        text = "==================================",
        color = Color(0xFF999999),
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
