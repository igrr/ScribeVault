package me.igrr.scribevault.data.ocr

import android.net.Uri
import kotlinx.coroutines.delay
import me.igrr.scribevault.domain.ocr.OcrProcessor
import me.igrr.scribevault.domain.ocr.OcrResult

/**
 * Stub implementation of OcrProcessor for testing and development.
 */
class StubOcrProcessor : OcrProcessor {
    /**
     * Simulates OCR processing by returning a fixed string after a delay.
     *
     * @param imageUri The URI of the image (ignored in this stub).
     * @return A fixed OCR result string.
     */
    override suspend fun processImage(imageUri: Uri): OcrResult {
        delay(2000) // Simulate network/processing delay
        return OcrResult(
            content = "This is a stub OCR result. Replace with actual ML Kit or Cloud OCR implementation.",
            tags = listOf("stub", "ocr"),
            title = "Stub OCR Result"
        )
    }
} 