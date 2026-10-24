package com.pdfcraft.studio.ui.editor

import com.pdfcraft.studio.ui.common.colorpicker.PageBackgroundSpec
import com.pdfcraft.studio.ui.common.colorpicker.PageBgKind

/**
 * Text tool colors (text, background, border, spread) can hold a 2-color gradient.
 * The gradient rides inside the same Long color value, so ranges, undo, copy and save/load
 * keep working unchanged and old projects stay solid (backward compatible):
 *   bits 0..31  = color 1 (ARGB, exactly as before)
 *   bits 32..55 = color 2 (RGB; uses the alpha of color 1)
 *   bits 56..61 = direction, 64 steps of 5.625 degrees (in box fractions)
 *   bit  62     = gradient flag
 * Compose Color(Long) and Long.toInt() read only the low 32 bits, so code that does not know
 * about gradients simply sees color 1.
 */
object TextGradient {
    private const val FLAG = 1L shl 62
    private const val STEPS = 64

    fun isGradient(c: Long?): Boolean = c != null && (c and FLAG) != 0L

    fun color1(c: Long): Long = c and 0xFFFFFFFFL

    fun color2(c: Long): Long =
        if (isGradient(c)) (c and 0xFF000000L) or ((c ushr 32) and 0xFFFFFFL) else color1(c)

    fun opaque1(c: Long): Long = color1(c) or 0xFF000000L

    fun opaque2(c: Long): Long = color2(c) or 0xFF000000L

    fun alpha(c: Long): Float = (((c ushr 24) and 0xFFL).toFloat() / 255f).coerceIn(0f, 1f)

    fun angleDeg(c: Long): Float = ((c ushr 56) and 0x3FL).toFloat() * (360f / STEPS)

    fun encode(c1: Long, c2: Long, angleDeg: Float): Long {
        var a = if (angleDeg.isNaN()) 0f else angleDeg % 360f
        if (a < 0f) a += 360f
        val step = kotlin.math.round(a / (360f / STEPS)).toLong() and 0x3FL
        return (c1 and 0xFFFFFFFFL) or ((c2 and 0xFFFFFFL) shl 32) or (step shl 56) or FLAG
    }

    /** Direction of a gradient line given in box fractions. */
    fun angleOfLine(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        if (kotlin.math.abs(dx) < 1e-4f && kotlin.math.abs(dy) < 1e-4f) return 0f
        return Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /** Gradient line in box fractions (x0, y0, x1, y1), always covering the whole box. */
    fun line(c: Long): FloatArray {
        val rad = Math.toRadians(angleDeg(c).toDouble())
        val cx = kotlin.math.cos(rad).toFloat()
        val sy = kotlin.math.sin(rad).toFloat()
        val half = 0.5f * (kotlin.math.abs(cx) + kotlin.math.abs(sy))
        val dx = cx * half
        val dy = sy * half
        return floatArrayOf(0.5f - dx, 0.5f - dy, 0.5f + dx, 0.5f + dy)
    }

    /** One solid color where a gradient cannot be drawn (glow): middle of the two colors. */
    fun solid(c: Long): Long {
        if (!isGradient(c)) return c
        val a = color1(c)
        val b = color2(c)
        val r = (((a shr 16) and 0xFFL) + ((b shr 16) and 0xFFL)) / 2L
        val g = (((a shr 8) and 0xFFL) + ((b shr 8) and 0xFFL)) / 2L
        val bl = ((a and 0xFFL) + (b and 0xFFL)) / 2L
        return (a and 0xFF000000L) or (r shl 16) or (g shl 8) or bl
    }
}

/** Current text tool color -> initial state of the color window (gradient preselected). */
fun textColorToSpec(c: Long?, fallback: Long): PageBackgroundSpec {
    val v = c ?: fallback
    if (TextGradient.isGradient(v)) {
        val l = TextGradient.line(v)
        return PageBackgroundSpec(
            kind = PageBgKind.GRADIENT,
            color = TextGradient.color1(v),
            color2 = TextGradient.color2(v),
            gx0 = l[0],
            gy0 = l[1],
            gx1 = l[2],
            gy1 = l[3]
        )
    }
    return PageBackgroundSpec(kind = PageBgKind.SOLID, color = TextGradient.color1(v))
}

/** Color window result -> text tool color value (solid ARGB or packed gradient). */
fun textColorFromSpec(spec: PageBackgroundSpec): Long {
    return if (spec.kind == PageBgKind.GRADIENT) {
        TextGradient.encode(
            spec.color,
            spec.color2,
            TextGradient.angleOfLine(spec.gx0, spec.gy0, spec.gx1, spec.gy1)
        )
    } else {
        spec.color and 0xFFFFFFFFL
    }
}
