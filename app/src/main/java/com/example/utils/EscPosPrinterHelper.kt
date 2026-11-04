package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.R
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.UUID

object EscPosPrinterHelper {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Standard ESC/POS commands
    val CMD_INIT = byteArrayOf(0x1B, 0x40)
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val CMD_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    val CMD_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    val CMD_FEED_3 = byteArrayOf(0x1B, 0x64, 0x03)
    val CMD_FEED_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x00)

    private const val CHAR_WIDTH_58MM = 32

    /**
     * Converts a bitmap into an ESC/POS GS v 0 raster bit image byte stream
     * using Floyd-Steinberg or threshold dithering for crisp monochrome thermal printing.
     */
    fun bitmapToRasterEscPos(bitmap: Bitmap, targetWidth: Int = 384): ByteArray {
        // 58mm printers typically have 384 printable dots across width
        val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspect).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        val widthBytes = (targetWidth + 7) / 8
        val output = ByteArrayOutputStream()

        // GS v 0 m xL xH yL yH
        output.write(0x1D)
        output.write(0x76)
        output.write(0x30)
        output.write(0x00) // normal mode
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        output.write(targetHeight and 0xFF)
        output.write((targetHeight shr 8) and 0xFF)

        for (y in 0 until targetHeight) {
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < targetWidth) {
                        val pixel = scaled.getPixel(x, y)
                        val alpha = Color.alpha(pixel)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        // Invert or check brightness: dark pixel is printed (bit 1)
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (alpha > 50 && luminance < 160) {
                            byteVal = byteVal or (0x80 shr bit)
                        }
                    }
                }
                output.write(byteVal)
            }
        }
        return output.toByteArray()
    }

    /**
     * Formats 2-column line (Left text and Right text, padded to 32 chars)
     */
    fun formatTwoColumns(left: String, right: String, totalWidth: Int = CHAR_WIDTH_58MM): String {
        val spacesNeeded = totalWidth - left.length - right.length
        return if (spacesNeeded > 0) {
            left + " ".repeat(spacesNeeded) + right + "\n"
        } else {
            // If too long, wrap left and put right on right
            val trimmedLeft = if (left.length > totalWidth - right.length - 1) {
                left.substring(0, (totalWidth - right.length - 2).coerceAtLeast(0)) + " "
            } else left
            trimmedLeft + " ".repeat((totalWidth - trimmedLeft.length - right.length).coerceAtLeast(1)) + right + "\n"
        }
    }

    /**
     * Builds raw ESC/POS byte array for a complete receipt
     */
    fun buildReceiptBytes(
        context: Context,
        settings: StoreSettingsEntity,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))

        // 1. Initialize
        write(CMD_INIT)

        // 2. Optional Store Logo if exists
        try {
            if (!settings.logoPath.isNullOrEmpty()) {
                val logoFile = File(settings.logoPath)
                if (logoFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (bmp != null) {
                        write(CMD_ALIGN_CENTER)
                        write(bitmapToRasterEscPos(bmp, 280))
                        write(byteArrayOf(0x0A))
                    }
                }
            }
        } catch (_: Exception) { }

        // 3. Store Info (Centered)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_DOUBLE_SIZE)
        writeText("${settings.storeName}\n")
        write(CMD_NORMAL_SIZE)
        write(CMD_BOLD_OFF)

        if (settings.storeAddress.isNotBlank()) writeText("${settings.storeAddress}\n")
        if (settings.storePhone.isNotBlank()) writeText("Telp: ${settings.storePhone}\n")
        if (settings.storeWebsite.isNotBlank()) writeText("${settings.storeWebsite}\n")
        if (settings.receiptHeaderMessage.isNotBlank()) writeText("${settings.receiptHeaderMessage}\n")

        // Separator
        val separator = "-".repeat(CHAR_WIDTH_58MM) + "\n"
        val doubleSeparator = "=".repeat(CHAR_WIDTH_58MM) + "\n"
        writeText(separator)

        // 4. Transaction Meta
        write(CMD_ALIGN_LEFT)
        writeText(formatTwoColumns("No: ${transaction.transactionNumber}", ""))
        writeText(formatTwoColumns("Tgl: ${CurrencyUtils.formatDateTime(transaction.createdAt)}", ""))
        writeText(formatTwoColumns("Kasir: ${transaction.cashierName}", ""))
        if (transaction.status != "COMPLETED") {
            write(CMD_ALIGN_CENTER)
            write(CMD_BOLD_ON)
            writeText("*** STATUS: ${transaction.status} ***\n")
            write(CMD_BOLD_OFF)
            write(CMD_ALIGN_LEFT)
        }
        writeText(separator)

        // 5. Items
        for (item in items) {
            writeText("${item.productName}\n")
            val qtyUnitPrice = "  ${item.quantity} x ${CurrencyUtils.formatRupiah(item.sellingPriceSnapshot)}"
            val itemSubtotal = CurrencyUtils.formatRupiah(item.subtotal)
            writeText(formatTwoColumns(qtyUnitPrice, itemSubtotal))
            if (item.returnedQuantity > 0) {
                writeText(formatTwoColumns("  (Retur: ${item.returnedQuantity} pcs)", ""))
            }
        }
        writeText(separator)

        // 6. Totals
        writeText(formatTwoColumns("Subtotal", CurrencyUtils.formatRupiah(transaction.subtotal)))
        if (transaction.discountAmount > 0L) {
            writeText(formatTwoColumns("Diskon", "-${CurrencyUtils.formatRupiah(transaction.discountAmount)}"))
        }
        if (transaction.taxAmount > 0L) {
            writeText(formatTwoColumns("Pajak (${settings.taxName})", CurrencyUtils.formatRupiah(transaction.taxAmount)))
        }
        writeText(doubleSeparator)

        write(CMD_BOLD_ON)
        writeText(formatTwoColumns("TOTAL", CurrencyUtils.formatRupiah(transaction.totalAmount)))
        write(CMD_BOLD_OFF)

        val paymentLabel = if (transaction.paymentType == "CASH") "TUNAI" else "NON-TUNAI (${transaction.paymentMethodDetails})"
        writeText(formatTwoColumns("Bayar ($paymentLabel)", CurrencyUtils.formatRupiah(transaction.amountPaid)))
        if (transaction.paymentType == "CASH") {
            writeText(formatTwoColumns("Kembali", CurrencyUtils.formatRupiah(transaction.changeAmount)))
        }
        if (!transaction.paymentReference.isNullOrBlank()) {
            writeText(formatTwoColumns("Ref: ${transaction.paymentReference}", ""))
        }
        writeText(separator)

        // 7. Footer Message
        write(CMD_ALIGN_CENTER)
        if (settings.receiptFooterMessage.isNotBlank()) {
            writeText("${settings.receiptFooterMessage}\n")
        }

        // 8. MANDATORY OFFICIAL TOKOKUH CREDIT LOGO AT BOTTOM
        try {
            val creditBmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo_credit_struk)
            if (creditBmp != null) {
                write(CMD_ALIGN_CENTER)
                write(bitmapToRasterEscPos(creditBmp, 320))
                write(byteArrayOf(0x0A))
            }
        } catch (_: Exception) {
            // Fallback text if resource fails
            write(CMD_ALIGN_CENTER)
            write(CMD_BOLD_ON)
            writeText("TOKOKUH.com\n")
            write(CMD_BOLD_OFF)
        }

        write(CMD_ALIGN_CENTER)
        writeText("Powered by TOKOKUH POS\nwww.tokokuh.com\n")

        // 9. Feed and Cut
        write(CMD_FEED_3)
        write(CMD_FEED_AND_CUT)

        return stream.toByteArray()
    }

    /**
     * Builds raw ESC/POS test print bytes
     */
    fun buildTestPrintBytes(context: Context, settings: StoreSettingsEntity): ByteArray {
        val stream = ByteArrayOutputStream()
        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))

        write(CMD_INIT)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_DOUBLE_SIZE)
        writeText("TES PRINTER OK\n")
        write(CMD_NORMAL_SIZE)
        write(CMD_BOLD_OFF)
        writeText("${settings.storeName}\n")
        writeText("Lebar Kertas: 58mm\n")
        writeText("Koneksi Bluetooth Berhasil!\n")
        writeText("-".repeat(CHAR_WIDTH_58MM) + "\n")
        writeText("Waktu: ${CurrencyUtils.formatDateTime(System.currentTimeMillis())}\n")
        writeText("-".repeat(CHAR_WIDTH_58MM) + "\n")

        // Mandatory bottom logo
        try {
            val creditBmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo_credit_struk)
            if (creditBmp != null) {
                write(bitmapToRasterEscPos(creditBmp, 320))
                write(byteArrayOf(0x0A))
            }
        } catch (_: Exception) { }

        writeText("www.tokokuh.com\n")
        write(CMD_FEED_3)
        write(CMD_FEED_AND_CUT)
        return stream.toByteArray()
    }

    /**
     * Send byte array to Bluetooth Printer via RFCOMM Socket
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(
        deviceAddress: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext Result.failure(
            Exception("Bluetooth tidak didukung pada perangkat ini.")
        )
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth tidak aktif. Silakan aktifkan Bluetooth."))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery() // Always cancel discovery before connect
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()
            // Small pause for printer buffer
            Thread.sleep(300)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage ?: "Koneksi terputus"}"))
        } finally {
            try { outputStream?.close() } catch (_: Exception) { }
            try { socket?.close() } catch (_: Exception) { }
        }
    }
}
