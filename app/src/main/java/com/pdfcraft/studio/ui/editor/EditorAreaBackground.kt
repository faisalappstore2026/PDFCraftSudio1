package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * fix52: the four Color Picker options (1..4) and their saved looks are removed.
 * The areas keep their fixed defaults: pages area 0xFFEEEEEE, top bar white
 * (black items, EditorTopBarColors.kt), undo / redo badge = design colour, numbers white.
 */
internal object EditorAreaBg {
    const val PAGES = 1
    const val TOP_BAR = 2
    const val BADGE = 3
    const val NUMBER = 4
    private val PAGES_BG = Color(0xFFEEEEEE)

    fun isSet(area: Int): Boolean = area == PAGES || area == TOP_BAR

    fun colorOr(area: Int, fallback: Color): Color = when (area) {
        PAGES -> PAGES_BG
        TOP_BAR -> Color.White
        else -> fallback
    }

    fun draw(m: Modifier, area: Int): Modifier = when (area) {
        PAGES -> m.background(PAGES_BG)
        TOP_BAR -> m.background(Color.White)
        else -> m
    }
}

/** fix52: fixed default background of an editor area (nothing for the badge). */
internal fun Modifier.editorAreaBackground(area: Int): Modifier = EditorAreaBg.draw(this, area)
