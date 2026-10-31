package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * fix55: Add Border Line tool. A static design (1..300, same designs as StaticBorder)
 * with its own thickness and inset, saved per image / text box id in prefs "element_borders".
 * Drawn in the editor and in the exported PDF.
 */
data class ElementFrame(val design: Int, val thickness: Float = 1f, val insetDp: Float = 0f)

object ElementBorders {
    const val MIN_THICK = 0.4f
    const val MAX_THICK = 4f
    const val MIN_INSET = -12f
    const val MAX_INSET = 24f
    val frames = mutableStateMapOf<String, ElementFrame>()
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences("element_borders", Context.MODE_PRIVATE)
        for ((k, v) in p.all) {
            val s = (v as? String)?.split(';') ?: continue
            if (s.size < 3) continue
            val d = s[0].toIntOrNull() ?: continue
            if (d < 1 || d > StaticBorder.COUNT) continue
            frames[k] = ElementFrame(
                d,
                (s[1].toFloatOrNull() ?: 1f).coerceIn(MIN_THICK, MAX_THICK),
                (s[2].toFloatOrNull() ?: 0f).coerceIn(MIN_INSET, MAX_INSET)
            )
        }
    }

    fun get(id: String): ElementFrame? = frames[id]

    /** [f] null or design 0 removes the border from these ids. */
    fun apply(context: Context, ids: Collection<String>, f: ElementFrame?) {
        ensureLoaded(context)
        val e = context.applicationContext.getSharedPreferences("element_borders", Context.MODE_PRIVATE).edit()
        for (id in ids) {
            if (f == null || f.design < 1) {
                frames.remove(id)
                e.remove(id)
            } else {
                frames[id] = f
                e.putString(id, f.design.toString() + ";" + f.thickness + ";" + f.insetDp)
            }
        }
        e.apply()
    }
}

/** fix55: 1 = Image Tools, 2 = Text Tools opened the Add Border Line window. */
object BorderLineTool {
    var mode by mutableIntStateOf(0)
}

/** fix55: the 10 line styles of the 300 static designs; design n has style (n - 1) % 10. */
internal const val FRAME_STYLES = 10
internal fun frameDesignFor(style: Int, k: Int): Int = k * FRAME_STYLES + style + 1

internal fun frameColors(n: Int): List<Color> {
    val p = (n - 1) / 10
    val h = ((p * 37) % 360).toFloat()
    val sat = if (p % 3 == 1) 0.55f else 0.9f
    val v = if (p % 4 == 3) 0.7f else 1f
    return listOf(Color.hsv(h, sat, v), Color.hsv((h + 60f) % 360f, sat, v), Color.hsv((h + 180f) % 360f, sat * 0.8f, 1f))
}

/** fix55: draws design [n] in a w x h box. [base] = line width px, [shift] > 0 moves it inside, < 0 outside. */
internal fun DrawScope.drawFrameDesign(n: Int, base: Float, shift: Float, corner: Float, w: Float, h: Float) {
    if (n < 1 || n > StaticBorder.COUNT || w < 2f || h < 2f) return
    val style = (n - 1) % FRAME_STYLES
    val cs = frameColors(n)
    fun line(inset0: Float, width: Float, b: Brush, effect: PathEffect? = null, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Butt) {
        val inset = inset0 + shift
        drawRoundRect(
            brush = b,
            topLeft = Offset(inset, inset),
            size = Size((w - inset * 2f).coerceAtLeast(1f), (h - inset * 2f).coerceAtLeast(1f)),
            cornerRadius = CornerRadius((corner - inset).coerceAtLeast(0f)),
            alpha = alpha,
            style = Stroke(width = width, cap = cap, pathEffect = effect)
        )
    }
    val c1 = SolidColor(cs[0])
    val c2 = SolidColor(cs[1])
    val lin = Brush.linearGradient(listOf(cs[0], cs[1], cs[2]), Offset.Zero, Offset(w, h))
    when (style) {
        0 -> line(base / 2f, base, c1)
        1 -> { line(base / 2f, base, c1); line(base * 2.5f, base / 2f, c2) }
        2 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(base * 4f, base * 2f), 0f))
        3 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(0.01f, base * 2.2f), 0f), cap = StrokeCap.Round)
        4 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(base * 5f, base * 1.5f, 0.01f, base * 1.5f), 0f), cap = StrokeCap.Round)
        5 -> line(base / 2f, base, lin)
        6 -> line(base / 2f, base, Brush.sweepGradient(listOf(cs[0], cs[1], cs[2], cs[0]), Offset(w / 2f, h / 2f)))
        7 -> { line(base * 0.8f, base * 1.6f, c1); line(base * 2.6f, base / 2f, c2) }
        8 -> line(base / 2f, base, lin, PathEffect.dashPathEffect(floatArrayOf(base * 8f, base * 3f), 0f))
        else -> { line(base * 1.5f, base * 3f, c1, alpha = 0.2f); line(base, base * 2f, c1, alpha = 0.45f); line(base / 2f, base, c1) }
    }
}

/** fix55: draws the saved border of element [id] (if any) over this content in the editor. */
@Composable
internal fun Modifier.elementFrame(id: String, corner: Dp): Modifier {
    ElementBorders.ensureLoaded(LocalContext.current)
    val f = ElementBorders.frames[id] ?: return this
    return this.drawWithContent {
        drawContent()
        drawFrameDesign(f.design, 2.dp.toPx() * f.thickness, f.insetDp.dp.toPx(), corner.toPx(), size.width, size.height)
    }
}

/** fix55: same border on the PDF page canvas. [pxPerDp] = page px per editor dp. */
internal fun drawElementFrameOnCanvas(
    context: Context,
    canvas: android.graphics.Canvas,
    id: String,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    pxPerDp: Float,
    cornerPx: Float = 0f
) {
    ElementBorders.ensureLoaded(context)
    val f = ElementBorders.frames[id] ?: return
    val w = right - left
    val h = bottom - top
    if (w < 2f || h < 2f) return
    val save = canvas.save()
    try {
        canvas.translate(left, top)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(w, h)) {
            drawFrameDesign(f.design, 2f * pxPerDp * f.thickness, f.insetDp * pxPerDp, cornerPx, w, h)
        }
    } catch (t: Throwable) { // keep the page even if the border fails
    } finally {
        canvas.restoreToCount(save)
    }
}
