package me.igrr.scribevault.domain.ocr

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Interface for OCR processing.
 */

@Parcelize
data class OcrResult(
    val content: String,
    val tags: List<String>,
    val title: String
) : Parcelable

interface OcrProcessor {
    /**
     * Processes the given image URI and returns the recognized text.
     *
     * @param imageUri The URI of the image to process.
     * @return The recognized text, or null if processing fails.
     */
    suspend fun processImage(imageUri: Uri): OcrResult?
}