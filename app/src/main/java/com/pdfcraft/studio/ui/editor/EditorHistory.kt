package com.pdfcraft.studio.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import com.pdfcraft.studio.core.image.ImageSizeOption

data class EditorMemento(
    val images: List<ImportedImage>,
    val texts: List<TextElement>,
    val minPageCount: Int,
    val pageAspectRatio: Float,
    val isPageLandscape: Boolean,
    val pageMarginDp: Int,
    val pageAspectOverrides: Map<Int, Float>,
    val pageBackgroundColor: Long,
    val pageBackgroundColorOverrides: Map<Int, Long>,
    val pageBackgroundBitmap: Bitmap?,
    val pageBackgroundBitmapOverrides: Map<Int, Bitmap>,
    val pageBackgroundKind: String,
    val pageBackgroundKindOverrides: Map<Int, String>,
    val pageBackgroundColor2: Long,
    val pageBackgroundColor2Overrides: Map<Int, Long>,
    val pageBackgroundGradientDir: String,
    val pageBackgroundGradientDirOverrides: Map<Int, String>,
    val pageBackgroundImageUri: Uri?,
    val pdfPageRasterIndices: Set<Int>,
    val imagesPerRow: Int,
    val imageSpacingDp: Int,
    val imageCellAspectRatio: Float,
    val layoutCellAspectRatio: Float,
    val imageCornerRadiusPercent: Int,
    val selectedImageSizeOption: ImageSizeOption,
    val numberingAlpha: Float,
    val numberingSizeFrac: Float,
    val numberingXFrac: Float,
    val numberingYFrac: Float,
    val numberingBgArgb: Long,
    val numberingFgArgb: Long,
    val numberingWeight: Float,
    val pageNumberPosition: EditorViewModel.PageNumberPosition,
    val pageNumberStyle: EditorViewModel.PageNumberStyle
)

class EditorHistory(private val onCounts: (Int, Int) -> Unit) {
    private val undo = ArrayDeque<EditorMemento>()
    private val redo = ArrayDeque<EditorMemento>()
    private var coalescing = false
    private var coalesceKey: String? = null
    private val max = 40

    val undoCount: Int get() = undo.size
    val redoCount: Int get() = redo.size

    fun record(before: EditorMemento) {
        coalescing = false
        coalesceKey = null
        pushUndo(before)
        redo.clear()
        emit()
    }

    fun beginCoalesce(before: EditorMemento, key: String) {
        if (coalescing && coalesceKey == key) return
        coalescing = true
        coalesceKey = key
        pushUndo(before)
        redo.clear()
        emit()
    }

    fun endCoalesce() {
        coalescing = false
        coalesceKey = null
    }

    fun undo(current: EditorMemento): EditorMemento? {
        coalescing = false
        coalesceKey = null
        if (undo.isEmpty()) return null
        redo.addLast(current)
        val prev = undo.removeLast()
        emit()
        return prev
    }

    fun redo(current: EditorMemento): EditorMemento? {
        coalescing = false
        coalesceKey = null
        if (redo.isEmpty()) return null
        undo.addLast(current)
        val next = redo.removeLast()
        emit()
        return next
    }

    fun undoAll(current: EditorMemento): EditorMemento? {
        coalescing = false
        if (undo.isEmpty()) return null
        redo.addLast(current)
        while (undo.size > 1) redo.addLast(undo.removeLast())
        val first = undo.removeLast()
        emit()
        return first
    }

    fun redoAll(current: EditorMemento): EditorMemento? {
        coalescing = false
        if (redo.isEmpty()) return null
        undo.addLast(current)
        while (redo.size > 1) undo.addLast(redo.removeLast())
        val last = redo.removeLast()
        emit()
        return last
    }

    fun clear() {
        undo.clear()
        redo.clear()
        coalescing = false
        coalesceKey = null
        emit()
    }

    fun seed(m: EditorMemento, undoCount: Int, redoCount: Int) {
        undo.clear()
        redo.clear()
        // Saved steps cannot be rebuilt after reopen, so start with empty history
        // (no copies of the current state that make Undo/Redo look active).
        coalescing = false
        coalesceKey = null
        emit()
    }

    private fun pushUndo(m: EditorMemento) {
        if (undo.isNotEmpty() && sameState(undo.last(), m)) return
        undo.addLast(m)
        while (undo.size > max) undo.removeFirst()
    }

    /** Cheap check first (same instances), full compare only when all elements are identical. */
    private fun sameState(a: EditorMemento, b: EditorMemento): Boolean {
        if (a === b) return true
        if (a.texts.size != b.texts.size || a.images.size != b.images.size) return false
        for (i in a.texts.indices) if (a.texts[i] !== b.texts[i]) return false
        for (i in a.images.indices) if (a.images[i] !== b.images[i]) return false
        return a.copy(texts = b.texts, images = b.images) == b
    }

    private fun emit() {
        onCounts(undo.size, redo.size)
    }
}
