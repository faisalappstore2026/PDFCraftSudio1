package com.pdfcraft.studio.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageHandler(private val contentResolver: ContentResolver) {

    suspend fun decode(uri: Uri, maxDimensionPx: Int = 1600): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, bounds)
                }

                val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun calculateSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        val longestSide = maxOf(width, height)
        while (longestSide / (sampleSize * 2) >= maxDimensionPx) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
