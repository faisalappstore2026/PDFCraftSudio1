package com.pdfcraft.studio.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class ImageCompressor {

    data class Result(val bitmap: Bitmap, val approxSizeBytes: Int)

    private val minQuality = 35
    private val minDimensionPx = 150
    private val maxScaleSteps = 6
    private val scaleFactor = 0.8f

    fun compressToTarget(original: Bitmap, targetBytes: Int): Result {
        val originalBytes = encode(original, quality = 92)
        if (originalBytes.size <= targetBytes) {
            return Result(original, originalBytes.size)
        }

        var workingBitmap = original

        repeat(maxScaleSteps + 1) { step ->
            val fit = searchQuality(workingBitmap, targetBytes)
            if (fit != null) return fit

            if (step < maxScaleSteps) {
                val nextWidth = (workingBitmap.width * scaleFactor).toInt()
                val nextHeight = (workingBitmap.height * scaleFactor).toInt()
                if (nextWidth >= minDimensionPx && nextHeight >= minDimensionPx) {
                    workingBitmap = Bitmap.createScaledBitmap(workingBitmap, nextWidth, nextHeight, true)
                }
            }
        }

        val fallbackBytes = encode(workingBitmap, minQuality)
        val decoded = BitmapFactory.decodeByteArray(fallbackBytes, 0, fallbackBytes.size) ?: workingBitmap
        return Result(decoded, fallbackBytes.size)
    }

    private fun searchQuality(bitmap: Bitmap, targetBytes: Int): Result? {
        val atFloorQuality = encode(bitmap, minQuality)
        if (atFloorQuality.size > targetBytes) return null

        var low = minQuality
        var high = 95
        var bestBytes = atFloorQuality
        var iterations = 0

        while (low <= high && iterations < 6) {
            val mid = (low + high) / 2
            val candidate = encode(bitmap, mid)
            if (candidate.size <= targetBytes) {
                bestBytes = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
            iterations++
        }

        val decoded = BitmapFactory.decodeByteArray(bestBytes, 0, bestBytes.size) ?: bitmap
        return Result(decoded, bestBytes.size)
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        return stream.toByteArray()
    }
}
