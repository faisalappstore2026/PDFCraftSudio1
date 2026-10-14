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
        val normal = Result(decoded, fallbackBytes.size)
        if (normal.approxSizeBytes <= targetBytes) return normal
        if (targetBytes >= 5 * 1024) return normal
        return squeezeUnder(decoded, targetBytes)
    }

    private fun squeezeUnder(start: Bitmap, targetBytes: Int): Result {
        var working = start
        var guard = 0
        while (guard < 12) {
            var quality = 33
            while (quality >= 1) {
                val bytes = encode(working, quality)
                if (bytes.size <= targetBytes) {
                    val small = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: working
                    return Result(small, bytes.size)
                }
                quality = quality - 8
            }
            val nextW = (working.width * 0.7f).toInt()
            val nextH = (working.height * 0.7f).toInt()
            if (nextW < 16) {
                val tiny = encode(working, 1)
                val small = BitmapFactory.decodeByteArray(tiny, 0, tiny.size) ?: working
                return Result(small, tiny.size)
            }
            if (nextH < 16) {
                val tiny = encode(working, 1)
                val small = BitmapFactory.decodeByteArray(tiny, 0, tiny.size) ?: working
                return Result(small, tiny.size)
            }
            working = Bitmap.createScaledBitmap(working, nextW, nextH, true)
            guard = guard + 1
        }
        val tiny = encode(working, 1)
        val small = BitmapFactory.decodeByteArray(tiny, 0, tiny.size) ?: working
        return Result(small, tiny.size)
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
