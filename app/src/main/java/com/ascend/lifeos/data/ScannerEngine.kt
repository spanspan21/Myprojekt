package com.ascend.lifeos.data

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Primary barcode scanning via Google's on-device ML Kit code scanner
 * (fast, free, no camera permission, own bottom-sheet UI). Callers provide a
 * fallback (embedded ZXing) for devices without Play services / the module.
 */
object ScannerEngine {
    fun scan(ctx: Context, onResult: (String) -> Unit, onFallback: () -> Unit) {
        val opts = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build()
        runCatching {
            GmsBarcodeScanning.getClient(ctx, opts).startScan()
                .addOnSuccessListener { barcode ->
                    val v = barcode.rawValue
                    if (v.isNullOrBlank()) onFallback() else onResult(v)
                }
                .addOnFailureListener { onFallback() }
            // Cancellation (user closed the sheet) intentionally does nothing.
        }.onFailure { onFallback() }
    }
}
