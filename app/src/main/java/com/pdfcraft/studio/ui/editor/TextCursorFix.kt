package com.pdfcraft.studio.ui.editor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.abs

/**
 * fix44: with Text Tools line spacing the normal text field cursor is as tall as the whole
 * line. Then the field cursor is hidden and this font-height cursor is drawn instead.
 */
internal fun usesSpacedCursor(te: TextElement): Boolean = abs(te.lineSpacing - 1f) >= 0.005f

@Composable
internal fun spacedCursorBlinkOn(active: Boolean): Boolean {
    if (!active) return false
    val t by rememberInfiniteTransition(label = "fix44Cursor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "fix44CursorBlink"
    )
    return t < 0.5f
}

internal fun drawSpacedCursor(scope: DrawScope, lay: TextLayoutResult, offset: Int, fontPx: Float, color: Color) {
    val len = lay.layoutInput.text.length
    val off = offset.coerceIn(0, len)
    val line = lay.getLineForOffset(off)
    val x = lay.getHorizontalPosition(off, true)
    val base = lay.getLineBaseline(line)
    val top = base - fontPx * 0.95f
    val bottom = base + fontPx * 0.25f
    val w = 2f * scope.density
    scope.drawRect(color, Offset((x - w / 2f).coerceAtLeast(0f), top), Size(w, bottom - top))
}
