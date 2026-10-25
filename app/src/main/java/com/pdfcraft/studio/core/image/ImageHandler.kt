package com.pdfcraft.studio.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

                val decoded = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
                // Camera photos store rotation in EXIF: turn the pixels upright once.
                if (decoded == null) null else applyExifOrientation(decoded, readExifOrientation(uri))
            } catch (e: OutOfMemoryError) {
                android.util.Log.w("ImageHandler", "Image too large to decode", e)
                null
            } catch (e: Exception) {
                null
            }
        }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f)
                m.postScale(-1f, 1f)
            }
            else -> return src
        }
        return try {
            val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            if (out !== src) src.recycle()
            out
        } catch (e: OutOfMemoryError) {
            android.util.Log.w("ImageHandler", "No memory to rotate photo, kept as is", e)
            src
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
