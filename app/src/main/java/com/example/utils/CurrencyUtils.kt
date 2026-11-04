package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    private val rupiahSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        currencySymbol = "Rp"
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }

    private val rupiahFormatter = DecimalFormat("Rp#,###", rupiahSymbols)

    /**
     * Formats Long amount to IDR Rupiah format: Rp10.000
     */
    fun formatRupiah(amount: Long): String {
        return if (amount == 0L) {
            "Rp0"
        } else if (amount < 0L) {
            "-Rp" + rupiahFormatter.format(-amount).replace("Rp", "")
        } else {
            rupiahFormatter.format(amount)
        }
    }

    /**
     * Safely parse text input to Long currency amount
     */
    fun parseRupiah(input: String): Long {
        val cleanString = input.replace("[^0-9]".toRegex(), "")
        return cleanString.toLongOrNull() ?: 0L
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        return sdf.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(millis))
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yy", Locale("id", "ID"))
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
        return sdf.format(Date(millis))
    }

    /**
     * Generate unique transaction number TRX-YYYYMMDD-XXXXXX
     */
    fun generateTransactionNumber(): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomPart = (100000..999999).random()
        return "TRX-$datePart-$randomPart"
    }

    fun generateReturnNumber(): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomPart = (1000..9999).random()
        return "RET-$datePart-$randomPart"
    }
}
