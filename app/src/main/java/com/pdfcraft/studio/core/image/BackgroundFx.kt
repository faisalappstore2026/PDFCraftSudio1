package com.pdfcraft.studio.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader

object BackgroundFx {

    fun process(
        src: Bitmap,
        blurPct: Float,
        opacityPct: Float,
        brightness: Float,
        overlayOn: Boolean,
        overlayColor: Long,
        overlayOpacityPct: Float,
        gradientColor2: Long? = null,
        gx0: Float = 0.5f,
        gy0: Float = 0f,
        gx1: Float = 0.5f,
        gy1: Float = 1f,
        maxSide: Int = 1400,
        blurType: String = "gaussian"
    ): Bitmap {
        if (src.isRecycled) return src
        val input = runCatching {
            src.copy(Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: return src
        val scaled = scaleMax(input, maxSide)
        val blurred = applyStyledBlur(scaled, blurType, blurPct)
        val w = blurred.width.coerceAtLeast(1)
        val h = blurred.height.coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val a = (opacityPct.coerceIn(0f, 100f) / 100f).coerceIn(0f, 1f)
        paint.alpha = (a * 255f).toInt().coerceIn(0, 255)
        val b = (brightness.coerceIn(-100f, 100f) / 100f)
        if (kotlin.math.abs(b) > 0.01f) {
            val add = b * 255f
            val cm = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, add,
                    0f, 1f, 0f, 0f, add,
                    0f, 0f, 1f, 0f, add,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(blurred, null, Rect(0, 0, w, h), paint)
        if (overlayOn) {
            val op = Paint(Paint.ANTI_ALIAS_FLAG)
            op.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            val oa = (overlayOpacityPct.coerceIn(0f, 100f) / 100f).coerceIn(0f, 1f)
            op.alpha = (oa * 255f).toInt().coerceIn(0, 255)
            if (gradientColor2 != null) {
                op.shader = LinearGradient(
                    gx0 * w, gy0 * h, gx1 * w, gy1 * h,
                    overlayColor.toInt(), gradientColor2.toInt(),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), op)
            } else {
                op.color = overlayColor.toInt()
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), op)
            }
        }
        if (blurred !== scaled && blurred !== src && blurred !== input && !blurred.isRecycled) {
            runCatching { blurred.recycle() }
        }
        if (scaled !== src && scaled !== input && scaled !== blurred && !scaled.isRecycled) {
            runCatching { scaled.recycle() }
        }
        if (input !== src && input !== out && input !== scaled && input !== blurred && !input.isRecycled) {
            runCatching { input.recycle() }
        }
        return out
    }

    private fun scaleMax(src: Bitmap, maxSide: Int): Bitmap {
        val m = maxOf(src.width, src.height)
        if (m <= maxSide) return src
        val s = maxSide.toFloat() / m
        val w = (src.width * s).toInt().coerceAtLeast(1)
        val h = (src.height * s).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun applyStyledBlur(src: Bitmap, type: String, pct: Float): Bitmap {
        val t = pct.coerceIn(0f, 100f)
        if (t < 1f && type != "pixelate" && type != "mosaic") return src
        return when (type) {
            "box", "average" -> cheapBlur(src, (t * 1.15f).coerceAtMost(100f))
            "motion", "directional" -> smear(src, t, rotate = 0f)
            "radial", "zoom" -> zoomSmear(src, t)
            "spin" -> spinSmear(src, t)
            "pixelate" -> pixelate(src, t, 10f)
            "mosaic" -> pixelate(src, t, 18f)
            "median" -> cheapBlur(src, t * 0.85f)
            "lens", "defocus", "bokeh" -> cheapBlur(src, (t * 1.25f).coerceAtMost(100f))
            "surface" -> mix(src, cheapBlur(src, t), 0.55f)
            "tiltshift" -> maskedBlur(src, t, radial = false)
            "selective", "background" -> maskedBlur(src, t, radial = true)
            "depth" -> maskedBlur(src, t, radial = false, invert = true)
            "edge" -> mix(src, cheapBlur(src, t), 0.35f)
            else -> cheapBlur(src, t)
        }
    }

    private fun pixelate(src: Bitmap, pct: Float, factor: Float): Bitmap {
        val t = (pct / 100f).coerceIn(0.05f, 1f)
        val block = (2f + t * factor).toInt().coerceAtLeast(2)
        val sw = (src.width / block).coerceAtLeast(1)
        val sh = (src.height / block).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src, sw, sh, false)
        val out = Bitmap.createScaledBitmap(small, src.width, src.height, false)
        if (small !== src && small !== out) runCatching { small.recycle() }
        return out
    }

    private fun smear(src: Bitmap, pct: Float, rotate: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val n = 9
        p.alpha = 255 / n
        val dist = pct / 100f * src.width * 0.07f
        for (i in -n / 2..n / 2) {
            c.save()
            c.translate(i * dist, 0f)
            if (rotate != 0f) c.rotate(rotate * i, src.width / 2f, src.height / 2f)
            c.drawBitmap(src, 0f, 0f, p)
            c.restore()
        }
        return out
    }

    private fun zoomSmear(src: Bitmap, pct: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val n = 8
        p.alpha = 255 / n
        val cx = src.width / 2f
        val cy = src.height / 2f
        for (i in 0 until n) {
            val s = 1f + i * (pct / 100f) * 0.05f
            c.save()
            c.scale(s, s, cx, cy)
            c.drawBitmap(src, 0f, 0f, p)
            c.restore()
        }
        return out
    }

    private fun spinSmear(src: Bitmap, pct: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val n = 8
        p.alpha = 255 / n
        val step = pct / 100f * 8f
        val cx = src.width / 2f
        val cy = src.height / 2f
        for (i in 0 until n) {
            c.save()
            c.rotate((i - n / 2f) * step, cx, cy)
            c.drawBitmap(src, 0f, 0f, p)
            c.restore()
        }
        return out
    }

    private fun mix(src: Bitmap, blur: Bitmap, amount: Float): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return blur
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        p.alpha = (amount.coerceIn(0f, 1f) * 255f).toInt()
        c.drawBitmap(blur, 0f, 0f, p)
        if (blur !== src && blur !== out) runCatching { blur.recycle() }
        return out
    }

    private fun maskedBlur(src: Bitmap, pct: Float, radial: Boolean, invert: Boolean = false): Bitmap {
        val blur = cheapBlur(src, pct)
        val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return blur
        val c = Canvas(out)
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val layer = c.saveLayer(0f, 0f, w, h, null)
        c.drawBitmap(blur, 0f, 0f, null)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        if (radial) {
            val r = (minOf(w, h) * if (invert) 0.75f else 0.42f)
            val inner = if (invert) 0x00FFFFFF else 0xFFFFFFFF.toInt()
            val outer = if (invert) 0xFFFFFFFF.toInt() else 0x00FFFFFF
            p.shader = RadialGradient(w / 2f, h / 2f, r, inner, outer, Shader.TileMode.CLAMP)
        } else {
            val y0 = if (invert) 0f else h * 0.25f
            val y1 = if (invert) h * 0.65f else h * 0.75f
            p.shader = LinearGradient(0f, y0, 0f, y1, 0xFFFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w, h, p)
        c.restoreToCount(layer)
        if (blur !== src && blur !== out) runCatching { blur.recycle() }
        return out
    }

    private fun cheapBlur(src: Bitmap, pct: Float): Bitmap {
        val t = (pct.coerceIn(0f, 100f) / 100f)
        val scale = (1f - t * 0.78f).coerceIn(0.1f, 1f)
        val sw = (src.width * scale).toInt().coerceAtLeast(6)
        val sh = (src.height * scale).toInt().coerceAtLeast(6)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val radius = (t * 12f).toInt().coerceIn(1, 12)
        val blurred = stackBlur(small, radius)
        val out = Bitmap.createScaledBitmap(blurred, src.width, src.height, true)
        if (small !== src && !small.isRecycled) runCatching { small.recycle() }
        if (blurred !== small && blurred !== out && !blurred.isRecycled) runCatching { blurred.recycle() }
        return out
    }

    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return sentBitmap
        if (radius < 1) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(maxOf(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        var yw = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0; gsum = 0; rsum = 0
            boutsum = 0; goutsum = 0; routsum = 0
            binsum = 0; ginsum = 0; rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0; gsum = 0; rsum = 0
            boutsum = 0; goutsum = 0; routsum = 0
            binsum = 0; ginsum = 0; rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
