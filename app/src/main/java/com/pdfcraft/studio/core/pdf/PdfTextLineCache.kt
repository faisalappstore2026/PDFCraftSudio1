package com.pdfcraft.studio.core.pdf

import java.util.concurrent.ConcurrentHashMap

/**
 * Real line breaks of each text box as the editor preview laid them out (Compose
 * TextLayoutResult), keyed by box id. The PDF uses them so its lines break exactly like the
 * preview. An entry is only used when the box values still match (else: PDF fallback wrap).
 */
object PdfTextLineCache {
    class Entry(
        val text: String,
        val fontSp: Float,
        val boxWFrac: Float,
        val boxHFrac: Float,
        val rawX: Float,
        val rawY: Float,
        val rotDeg: Float,
        /** Position as drawn in the preview (after clamp / cap). */
        val effX: Float,
        val effY: Float,
        /** Font px divided by preview page width px (same ratio in the PDF). */
        val fontPerPageW: Float,
        /** Laid-out text height / preview page height. */
        val textHFrac: Float,
        /** Drawn box (row) size as page fractions (rotation pivot). */
        val rowWFrac: Float,
        val rowHFrac: Float,
        val starts: IntArray,
        val ends: IntArray,
        /** Font and line-width styles (font, bold, italic) at layout time. */
        val styleKey: Int = 0
    ) {
        /** Line ranges (end exclusive -> IntRange), or null when they do not fit this text. */
        fun ranges(len: Int): List<IntRange>? {
            if (starts.isEmpty() || starts.size != ends.size) return null
            val out = ArrayList<IntRange>(starts.size)
            var prev = 0
            for (i in starts.indices) {
                val s = starts[i]
                val e = ends[i]
                if (s < prev || s > len || e < s || e > len) return null
                out.add(IntRange(s, e - 1))
                prev = s
            }
            return out
        }
    }

    private val map = ConcurrentHashMap<String, Entry>()

    /** Hash of everything besides text/size that changes line breaks. */
    fun styleKeyOf(t: com.pdfcraft.studio.ui.editor.TextElement): Int =
        listOf<Any?>(t.fontId, t.fontRanges, t.boldRanges, t.italicRanges).hashCode()

    fun put(id: String, e: Entry) {
        if (map.size > 500) map.clear()
        map[id] = e
    }

    private fun near(a: Float, b: Float): Boolean = !a.isNaN() && !b.isNaN() && kotlin.math.abs(a - b) < 0.0005f

    /** Entry for this box if text, font, size, position and rotation still match. */
    fun matching(
        id: String,
        text: String,
        fontSp: Float,
        boxWFrac: Float,
        boxHFrac: Float,
        x: Float,
        y: Float,
        rotDeg: Float,
        styleKey: Int = 0
    ): Entry? {
        val e = map[id] ?: return null
        if (e.text != text) return null
        if (e.styleKey != styleKey) return null
        if (kotlin.math.abs(e.fontSp - fontSp) >= 0.01f) return null
        if (!near(e.boxWFrac, boxWFrac) || !near(e.boxHFrac, boxHFrac)) return null
        if (!near(e.rawX, x) || !near(e.rawY, y) || !near(e.rotDeg, rotDeg)) return null
        if (e.fontPerPageW.isNaN() || e.fontPerPageW <= 0f) return null
        if (e.effX.isNaN() || e.effY.isNaN()) return null
        return e
    }
}
