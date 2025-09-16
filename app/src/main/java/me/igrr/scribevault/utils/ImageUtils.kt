package me.igrr.scribevault.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {
    /**
     * Downscales an image so that the long side <= [maxLongSidePx], then recompresses as JPEG.
     * Returns true if [destFile] was written successfully.
     */
    fun downscaleToMaxLongSide(
        context: Context,
        sourceUri: Uri,
        destFile: File,
        maxLongSidePx: Int = 2048,
        quality: Int = 80
    ): Boolean {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val longSide = max(width, height)
                if (longSide > maxLongSidePx) {
                    val scale = maxLongSidePx.toFloat() / longSide.toFloat()
                    val targetW = (width * scale).roundToInt().coerceAtLeast(1)
                    val targetH = (height * scale).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetW, targetH)
                }
                decoder.isMutableRequired = false
            }
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }
}


