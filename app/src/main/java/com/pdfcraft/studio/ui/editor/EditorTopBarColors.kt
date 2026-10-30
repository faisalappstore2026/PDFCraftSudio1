package com.pdfcraft.studio.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** fix47: on a light Color Picker 2 top area (white by default) all top-bar text and icons are black. */
internal fun editorTopBarDesign(d: MenuDesign): MenuDesign {
    val c = EditorAreaBg.colorOr(EditorAreaBg.TOP_BAR, Color.Black)
    return if (c.luminance() > 0.5f) d.copy(topBarFg = Color.Black) else d
}
