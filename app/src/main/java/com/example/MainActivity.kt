package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.TokokuhAppRoot
import com.example.ui.theme.TokokuhTheme
import com.example.viewmodel.TokokuhViewModel
import com.example.viewmodel.TokokuhViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: TokokuhViewModel by viewModels {
        TokokuhViewModelFactory(this, (application as TokokuhApp).repository)
    }

    // Buffer for external Bluetooth HID Keyboard Barcode Scanners
    private val barcodeBuffer = StringBuilder()
    private var lastKeyTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TokokuhTheme {
                TokokuhAppRoot(viewModel = viewModel)
            }
        }
    }

    /**
     * Intercepts key events from external Bluetooth HID Barcode Scanners.
     * Barcode scanners typically send characters very quickly (<50ms between keys)
     * and finish with an Enter key (KEYCODE_ENTER).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            // If more than 300ms has elapsed since the last key, start a new scan buffer
            if (now - lastKeyTime > 300) {
                barcodeBuffer.setLength(0)
            }
            lastKeyTime = now

            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val scanned = barcodeBuffer.toString().trim()
                if (scanned.length >= 3) {
                    barcodeBuffer.setLength(0)
                    viewModel.onBarcodeScanned(scanned) { notFound ->
                        // handled by UI if needed
                    }
                    return true
                }
                barcodeBuffer.setLength(0)
            } else {
                val unicodeChar = event.unicodeChar
                if (unicodeChar != 0 && unicodeChar != '\n'.code && unicodeChar != '\r'.code) {
                    barcodeBuffer.append(unicodeChar.toChar())
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
