package com.pdfcraft.studio.ui.editor

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfcraft.studio.core.image.BackgroundFx
import com.pdfcraft.studio.core.image.ImageCompressor
import com.pdfcraft.studio.core.image.ImageHandler
import com.pdfcraft.studio.ui.common.colorpicker.GradientDir
import com.pdfcraft.studio.ui.common.colorpicker.PageBackgroundSpec
import com.pdfcraft.studio.ui.common.colorpicker.encodeGradientLine
import com.pdfcraft.studio.ui.common.colorpicker.parseGradientLine
import com.pdfcraft.studio.ui.common.colorpicker.PageBgKind
import com.pdfcraft.studio.core.image.ImageSizeOption
import com.pdfcraft.studio.core.settings.ImageSizePreferences
import com.pdfcraft.studio.core.text.AppFont
import com.pdfcraft.studio.core.text.FontCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pdfcraft.studio.core.project.ProjectStore
import com.pdfcraft.studio.core.session.SessionStore
import java.io.File

data class ImportedImage(
    val id: String,
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val approxSizeBytes: Int? = null,
    val linkUrl: String? = null,
    /** 1-based label; null = no numbering icon */
    val numberLabel: Int? = null,
    /** Center of icon inside image, 0f..1f */
    val numberXFrac: Float = 0.5f,
    val numberYFrac: Float = 0.5f,
    /** Relative icon diameter vs min(image side), \~0.08..0.35 */
    val numberSizeFrac: Float = 0.18f,
    /** 0f..1f */
    val numberAlpha: Float = 0.9f,
    /** Icon circle fill ARGB */
    val numberBgArgb: Long = 0xE6000000L,
    /** Icon digit color ARGB */
    val numberFgArgb: Long = 0xFFFFFFFFL,
    /** 0f=Thin .. 1f=Bold */
    val numberWeight: Float = 0.85f,
    /** Extra shift from grid cell, as fraction of page width/height (-1..1). */
    val dragOffsetXFrac: Float = 0f,
    val dragOffsetYFrac: Float = 0f,
    /** 0..100 clip radius for this image only. */
    val cornerRadiusPercent: Int = 0,
    /** Null = use global imageCellAspectRatio (Set/Resize Images). */
    val aspectRatioOverride: Float? = null,
    val ownerPageIndex: Int? = null,
    val absXFrac: Float = 0f,
    val absYFrac: Float = 0f,
    val absWFrac: Float = 0f,
    val absHFrac: Float = 0f,
    /** True = this image stays on its page and ignores Images Per Row changes. */
    val rowExempt: Boolean = false,
    val pinnedPage: Int = 0,
    val pinnedPerRow: Int = 0,
    /** True = start a new page before this image. Used by Duplicate Pages. */
    val forceNewPage: Boolean = false
)

internal var packPageSource: IntArray = IntArray(0)

fun packEditorPages(
    images: List<ImportedImage>,
    imagesPerRow: Int,
    spacing: Float,
    cellWidth: Float,
    gridHeight: Float,
    defaultAspect: Float
): List<List<ImportedImage>> {
    if (images.isEmpty()) {
        packPageSource = IntArray(0)
        return emptyList()
    }
    val perRow = imagesPerRow.coerceAtLeast(1)
    val pinned = images.filter { it.rowExempt }
    val flowing = if (pinned.isEmpty()) images else images.filter { !it.rowExempt }
    fun imgAspect(img: ImportedImage, rowCount: Int): Float {
        val b = img.bitmap
        return if (rowCount == 1 && b != null && b.height > 0) {
            (b.width.toFloat() / b.height).coerceIn(0.2f, 5f)
        } else {
            (img.aspectRatioOverride ?: defaultAspect).coerceIn(0.3f, 2.0f)
        }
    }
    fun plain(list: List<ImportedImage>): List<MutableList<ImportedImage>> {
        if (list.isEmpty()) return emptyList()
        if (perRow == 1) return list.map { mutableListOf(it) }
        val pages = mutableListOf<MutableList<ImportedImage>>()
        var page = mutableListOf<ImportedImage>()
        var usedH = 0f
        fun imgH(img: ImportedImage): Float = cellWidth / imgAspect(img, perRow)
        fun rowH(row: List<ImportedImage>): Float = row.maxOf { imgH(it) }
        val row = mutableListOf<ImportedImage>()
        fun flushRow() {
            if (row.isEmpty()) return
            val h = rowH(row)
            if (page.isNotEmpty() && usedH + spacing + h > gridHeight + 0.5f) {
                pages.add(page)
                page = mutableListOf()
                usedH = 0f
            }
            val g = if (page.isEmpty()) 0f else spacing
            page.addAll(row)
            usedH += g + h
            row.clear()
        }
        for (img in list) {
            if (img.forceNewPage && (page.isNotEmpty() || row.isNotEmpty())) {
                flushRow()
                if (page.isNotEmpty()) {
                    pages.add(page)
                    page = mutableListOf()
                    usedH = 0f
                }
            }
            row.add(img)
            if (row.size == perRow) flushRow()
        }
        flushRow()
        if (page.isNotEmpty()) pages.add(page)
        return pages
    }
    if (pinned.isEmpty()) {
        val plainPages = plain(images)
        packPageSource = IntArray(plainPages.size) { it }
        return plainPages
    }
    val gridW = if (perRow <= 1) cellWidth else cellWidth * perRow + spacing * (perRow - 1f)
    fun cwFor(rowCount: Int): Float {
        val n = if (rowCount < 1) 1 else rowCount
        return if (n <= 1) gridW else (gridW - spacing * (n - 1)) / n
    }
    fun rowH(row: List<ImportedImage>, rowCount: Int, cw: Float): Float {
        var maxH = 0f
        for (img in row) {
            val h = cw / imgAspect(img, rowCount)
            if (h > maxH) maxH = h
        }
        return maxH
    }
    var maxPage = 0
    for (img in pinned) {
        val p = img.pinnedPage.coerceAtLeast(0)
        if (p > maxPage) maxPage = p
    }
    for (img in flowing) {
        val p = img.pinnedPage.coerceAtLeast(0)
        if (p > maxPage) maxPage = p
    }
    val slots = MutableList(maxPage + 1) { mutableListOf<ImportedImage>() }
    val used = MutableList(maxPage + 1) { 0f }
    val exemptBy = HashMap<Int, MutableList<ImportedImage>>()
    for (img in pinned) {
        val p = img.pinnedPage.coerceIn(0, maxPage)
        val bucket = exemptBy[p]
        if (bucket == null) exemptBy[p] = mutableListOf(img) else bucket.add(img)
    }
    for (entry in exemptBy) {
        val p = entry.key
        val list = entry.value
        val first = list.first()
        val r = if (first.pinnedPerRow > 0) first.pinnedPerRow else perRow
        val cw = cwFor(r)
        var i = 0
        while (i < list.size) {
            val count = if (list.size - i < r) list.size - i else r
            val row = ArrayList(list.subList(i, i + count))
            val h = rowH(row, r, cw)
            val g = if (used[p] <= 0.5f) 0f else spacing
            used[p] = used[p] + g + h
            slots[p].addAll(row)
            i += count
        }
    }
    // FLOW_PULL_UP: selected images fill earlier selected pages before later ones.
    val flowOrder = ArrayList<ImportedImage>(flowing.size)
    flowOrder.addAll(flowing)
    flowOrder.sortBy { it.pinnedPage }
    val selected = ArrayList<Int>()
    val seenSel = HashSet<Int>()
    for (img in flowOrder) {
        val p = img.pinnedPage.coerceIn(0, maxPage)
        if (seenSel.add(p)) selected.add(p)
    }
    selected.sort()
    val extras = HashMap<Int, MutableList<MutableList<ImportedImage>>>()
    val flowCw = cwFor(perRow)
    var q = 0
    var sidx = 0
    while (sidx < selected.size) {
        val p = selected[sidx]
        while (q < flowOrder.size) {
            val count = if (flowOrder.size - q < perRow) flowOrder.size - q else perRow
            val row = ArrayList(flowOrder.subList(q, q + count))
            val h = rowH(row, perRow, flowCw)
            val gap = if (used[p] <= 0.5f) 0f else spacing
            val room = used[p] <= 0.5f || used[p] + gap + h <= gridHeight + 0.5f
            if (!room) break
            used[p] = used[p] + gap + h
            slots[p].addAll(row)
            q += count
        }
        val lastSelected = sidx == selected.size - 1
        if (lastSelected && q < flowOrder.size) {
            val news = mutableListOf<MutableList<ImportedImage>>()
            var page = mutableListOf<ImportedImage>()
            var uh = 0f
            while (q < flowOrder.size) {
                val count = if (flowOrder.size - q < perRow) flowOrder.size - q else perRow
                val row = ArrayList(flowOrder.subList(q, q + count))
                val h = rowH(row, perRow, flowCw)
                if (page.isNotEmpty() && uh + spacing + h > gridHeight + 0.5f) {
                    news.add(page)
                    page = mutableListOf()
                    uh = 0f
                }
                val g = if (page.isEmpty()) 0f else spacing
                page.addAll(row)
                uh += g + h
                q += count
            }
            if (page.isNotEmpty()) news.add(page)
            extras[p] = news
        }
        sidx += 1
    }
    val out = mutableListOf<List<ImportedImage>>()
    val src = mutableListOf<Int>()
    var i = 0
    while (i < slots.size) {
        out.add(slots[i].toList())
        src.add(i)
        val extra = extras[i]
        if (extra != null) {
            for (pg in extra) {
                out.add(pg.toList())
                src.add(-1)
            }
        }
        i += 1
    }
    packPageSource = src.toIntArray()
    return out
}

data class ColorRange(
    val range: IntRange,
    val colorArgb: Long
)

data class FontRange(
    val range: IntRange,
    val fontId: String
)

data class LinkRange(
    val range: IntRange,
    val url: String
)

data class ShadowRange(
    val range: IntRange,
    val colorArgb: Long,
    val offsetXPx: Float,
    val offsetYPx: Float,
    val blurPx: Float
)

data class TextElement(
    val id: String,
    val pageIndex: Int,
    val text: String = "",
    val xFraction: Float = 0.1f,
    val yFraction: Float = 0.1f,
    val boldRanges: List<IntRange> = emptyList(),
    val italicRanges: List<IntRange> = emptyList(),
    val underlineRanges: List<IntRange> = emptyList(),
    val strikeRanges: List<IntRange> = emptyList(),
    val quoteRanges: List<IntRange> = emptyList(),
    val colorRanges: List<ColorRange> = emptyList(),
    val bgColorRanges: List<ColorRange> = emptyList(),
    val linkRanges: List<LinkRange> = emptyList(),
    val shadowRanges: List<ShadowRange> = emptyList(),
    val fontId: String = FontCatalog.ID_DEFAULT,
    val fontRanges: List<FontRange> = emptyList(),
    val fontSizeSp: Float = 16f,
    val textColorArgb: Long = 0xFF000000L,
    val bgColorArgb: Long? = null,
    val shadowColorArgb: Long = 0x80000000L,
    val shadowOffsetXPx: Float = 0f,
    val shadowOffsetYPx: Float = 0f,
    val shadowBlurPx: Float = 0f,
    val textAlign: String = "LEFT",
    val rotationDeg: Float = 0f,
    /** 0 = size follows the text. Otherwise a fraction of the page. */
    val boxWidthFrac: Float = 0f,
    val boxHeightFrac: Float = 0f
)

private fun adjustRangesForEdit(
    oldText: String,
    newText: String,
    ranges: List<IntRange>
): List<IntRange> {
    var prefix = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefix < minLen && oldText[prefix] == newText[prefix]) prefix++

    var suffix = 0
    while (
        suffix < (minLen - prefix) &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++

    val oldEditEnd = oldText.length - suffix
    val lengthDelta = newText.length - oldText.length

    return ranges.mapNotNull { range ->
        when {
            range.last < prefix -> range
            range.first >= oldEditEnd -> (range.first + lengthDelta)..(range.last + lengthDelta)
            else -> null
        }
    }
}

private fun toggleStyleRange(
    existing: List<IntRange>,
    start: Int,
    end: Int,
    textLength: Int
): List<IntRange> {
    if (textLength <= 0) return existing

    val flags = BooleanArray(textLength)
    existing.forEach { range ->
        for (i in range.first.coerceAtLeast(0)..range.last.coerceAtMost(textLength - 1)) {
            flags[i] = true
        }
    }

    val allOn = (start until end).all { flags.getOrElse(it) { false } }
    for (i in start until end) {
        if (i in flags.indices) flags[i] = !allOn
    }

    val result = mutableListOf<IntRange>()
    var rangeStart = -1
    for (i in flags.indices) {
        if (flags[i]) {
            if (rangeStart == -1) rangeStart = i
        } else if (rangeStart != -1) {
            result.add(rangeStart until i)
            rangeStart = -1
        }
    }
    if (rangeStart != -1) result.add(rangeStart until flags.size)
    return result
}


private fun applyColorRange(
    existing: List<ColorRange>,
    start: Int,
    end: Int,
    colorArgb: Long,
    textLength: Int
): List<ColorRange> {
    if (textLength <= 0 || start >= end) return existing
    val s = start.coerceIn(0, textLength)
    val e = end.coerceIn(0, textLength)
    if (s >= e) return existing

    val result = mutableListOf<ColorRange>()
    for (cr in existing) {
        val rs = cr.range.first
        val re = cr.range.last + 1
        if (re <= s || rs >= e) {
            result.add(cr)
        } else {
            if (rs < s) {
                result.add(ColorRange(rs until s, cr.colorArgb))
            }
            if (re > e) {
                result.add(ColorRange(e until re, cr.colorArgb))
            }
        }
    }
    result.add(ColorRange(s until e, colorArgb))
    val sorted = result.sortedBy { it.range.first }
    val merged = mutableListOf<ColorRange>()
    for (cr in sorted) {
        if (merged.isEmpty()) {
            merged.add(cr)
        } else {
            val last = merged.last()
            if (last.colorArgb == cr.colorArgb && last.range.last + 1 >= cr.range.first) {
                merged[merged.lastIndex] = ColorRange(
                    last.range.first..(maxOf(last.range.last, cr.range.last)),
                    cr.colorArgb
                )
            } else {
                merged.add(cr)
            }
        }
    }
    return merged
}

private fun applyFontRange(
    existing: List<FontRange>,
    start: Int,
    end: Int,
    fontId: String,
    textLength: Int
): List<FontRange> {
    if (textLength <= 0 || start >= end) return existing
    val s = start.coerceIn(0, textLength)
    val e = end.coerceIn(0, textLength)
    if (s >= e) return existing
    val result = mutableListOf<FontRange>()
    for (fr in existing) {
        val rs = fr.range.first
        val re = fr.range.last + 1
        if (re <= s || rs >= e) {
            result.add(fr)
        } else {
            if (rs < s) result.add(FontRange(rs until s, fr.fontId))
            if (re > e) result.add(FontRange(e until re, fr.fontId))
        }
    }
    result.add(FontRange(s until e, fontId))
    val sorted = result.sortedBy { it.range.first }
    val merged = mutableListOf<FontRange>()
    for (fr in sorted) {
        if (merged.isEmpty()) merged.add(fr)
        else {
            val last = merged.last()
            if (last.fontId == fr.fontId && last.range.last + 1 >= fr.range.first) {
                merged[merged.lastIndex] = FontRange(
                    last.range.first..(maxOf(last.range.last, fr.range.last)),
                    fr.fontId
                )
            } else merged.add(fr)
        }
    }
    return merged
}

private fun adjustFontRangesForEdit(
    oldText: String,
    newText: String,
    ranges: List<FontRange>
): List<FontRange> {
    val mapped = adjustColorRangesForEdit(
        oldText,
        newText,
        ranges.map { ColorRange(it.range, 0L) }
    )
    return mapped.mapIndexed { i, cr -> FontRange(cr.range, ranges.getOrNull(i)?.fontId ?: "") }
        .filter { it.fontId.isNotBlank() }
}

private fun adjustColorRangesForEdit(
    oldText: String,
    newText: String,
    ranges: List<ColorRange>
): List<ColorRange> {
    var prefix = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefix < minLen && oldText[prefix] == newText[prefix]) prefix++

    var suffix = 0
    while (
        suffix < (minLen - prefix) &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++

    val oldEditEnd = oldText.length - suffix
    val lengthDelta = newText.length - oldText.length

    return ranges.mapNotNull { cr ->
        val range = cr.range
        when {
            range.last < prefix -> cr
            range.first >= oldEditEnd -> ColorRange(
                (range.first + lengthDelta)..(range.last + lengthDelta),
                cr.colorArgb
            )
            else -> null
        }
    }
}


private fun applyLinkRange(
    existing: List<LinkRange>,
    start: Int,
    end: Int,
    url: String,
    textLength: Int
): List<LinkRange> {
    if (start >= end || url.isBlank()) return existing
    val s = start.coerceIn(0, textLength)
    val e = end.coerceIn(0, textLength)
    if (s >= e) return existing
    val result = mutableListOf<LinkRange>()
    for (lr in existing) {
        val a = lr.range.first
        val b = lr.range.last
        if (b < s || a >= e) {
            result.add(lr)
        } else {
            if (a < s) result.add(LinkRange(a until s, lr.url))
            if (b >= e) result.add(LinkRange(e..b, lr.url))
        }
    }
    result.add(LinkRange(s until e, url.trim()))
    return result.filter { it.range.first <= it.range.last }
}

private fun adjustLinkRangesForEdit(
    oldText: String,
    newText: String,
    ranges: List<LinkRange>
): List<LinkRange> {
    if (ranges.isEmpty()) return ranges
    var prefix = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefix < minLen && oldText[prefix] == newText[prefix]) prefix++
    var suffix = 0
    while (
        suffix < (minLen - prefix) &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++
    val oldEditEnd = oldText.length - suffix
    val newEditEnd = newText.length - suffix
    val delta = newEditEnd - oldEditEnd
    val out = mutableListOf<LinkRange>()
    for (lr in ranges) {
        var a = lr.range.first
        var b = lr.range.last
        if (b < prefix) {
            out.add(lr)
        } else if (a >= oldEditEnd) {
            out.add(LinkRange((a + delta)..(b + delta), lr.url))
        } else if (a >= prefix && b < oldEditEnd) {
            // range fully inside edit — drop
        } else {
            if (a < prefix) a = a
            else a = newEditEnd
            if (b >= oldEditEnd) b = b + delta
            else b = newEditEnd - 1
            if (a <= b) out.add(LinkRange(a..b, lr.url))
        }
    }
    return out
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val imageHandler = ImageHandler(application.contentResolver)
    private val imageCompressor = ImageCompressor()
    private val imageSizePreferences = ImageSizePreferences(application)

    val importedImages: SnapshotStateList<ImportedImage> = mutableStateListOf()

    var undoCount: Int by mutableStateOf(0)
        private set
    var redoCount: Int by mutableStateOf(0)
        private set
    private var historyBusy = false
    private val editorHistory = EditorHistory { u, r ->
        undoCount = u
        redoCount = r
    }

    private fun captureMemento(): EditorMemento = EditorMemento(
        images = importedImages.toList(),
        texts = textElements.toList(),
        minPageCount = minPageCount,
        pageAspectRatio = pageAspectRatio,
        isPageLandscape = isPageLandscape,
        pageMarginDp = pageMarginDp,
        pageAspectOverrides = pageAspectOverrides.toMap(),
        pageBackgroundColor = pageBackgroundColor,
        pageBackgroundColorOverrides = pageBackgroundColorOverrides.toMap(),
        pageBackgroundBitmap = pageBackgroundBitmap,
        pageBackgroundBitmapOverrides = pageBackgroundBitmapOverrides.toMap(),
        pageBackgroundKind = pageBackgroundKind,
        pageBackgroundKindOverrides = pageBackgroundKindOverrides.toMap(),
        pageBackgroundColor2 = pageBackgroundColor2,
        pageBackgroundColor2Overrides = pageBackgroundColor2Overrides.toMap(),
        pageBackgroundGradientDir = pageBackgroundGradientDir,
        pageBackgroundGradientDirOverrides = pageBackgroundGradientDirOverrides.toMap(),
        pageBackgroundImageUri = pageBackgroundImageUri,
        pdfPageRasterIndices = pdfPageRasterIndices.toSet(),
        imagesPerRow = imagesPerRow,
        imageSpacingDp = imageSpacingDp,
        imageCellAspectRatio = imageCellAspectRatio,
        layoutCellAspectRatio = layoutCellAspectRatio,
        imageCornerRadiusPercent = imageCornerRadiusPercent,
        selectedImageSizeOption = selectedImageSizeOption,
        numberingAlpha = numberingAlpha,
        numberingSizeFrac = numberingSizeFrac,
        numberingXFrac = numberingXFrac,
        numberingYFrac = numberingYFrac,
        numberingBgArgb = numberingBgArgb,
        numberingFgArgb = numberingFgArgb,
        numberingWeight = numberingWeight,
        pageNumberPosition = pageNumberPosition,
        pageNumberStyle = pageNumberStyle
    )

    private fun restoreMemento(m: EditorMemento) {
        historyBusy = true
        importedImages.clear()
        importedImages.addAll(m.images)
        textElements.clear()
        textElements.addAll(m.texts)
        minPageCount = m.minPageCount
        pageAspectRatio = m.pageAspectRatio
        isPageLandscape = m.isPageLandscape
        pageMarginDp = m.pageMarginDp
        pageAspectOverrides.clear()
        pageAspectOverrides.putAll(m.pageAspectOverrides)
        pageBackgroundColor = m.pageBackgroundColor
        pageBackgroundColorOverrides.clear()
        pageBackgroundColorOverrides.putAll(m.pageBackgroundColorOverrides)
        pageBackgroundBitmap = m.pageBackgroundBitmap
        pageBackgroundBitmapOverrides.clear()
        pageBackgroundBitmapOverrides.putAll(m.pageBackgroundBitmapOverrides)
        pageBackgroundKind = m.pageBackgroundKind
        pageBackgroundKindOverrides.clear()
        pageBackgroundKindOverrides.putAll(m.pageBackgroundKindOverrides)
        pageBackgroundColor2 = m.pageBackgroundColor2
        pageBackgroundColor2Overrides.clear()
        pageBackgroundColor2Overrides.putAll(m.pageBackgroundColor2Overrides)
        pageBackgroundGradientDir = m.pageBackgroundGradientDir
        pageBackgroundGradientDirOverrides.clear()
        pageBackgroundGradientDirOverrides.putAll(m.pageBackgroundGradientDirOverrides)
        pageBackgroundImageUri = m.pageBackgroundImageUri
        pdfPageRasterIndices.clear()
        pdfPageRasterIndices.addAll(m.pdfPageRasterIndices)
        imagesPerRow = m.imagesPerRow
        imageSpacingDp = m.imageSpacingDp
        imageCellAspectRatio = m.imageCellAspectRatio
        layoutCellAspectRatio = m.layoutCellAspectRatio
        imageCornerRadiusPercent = m.imageCornerRadiusPercent
        selectedImageSizeOption = m.selectedImageSizeOption
        numberingAlpha = m.numberingAlpha
        numberingSizeFrac = m.numberingSizeFrac
        numberingXFrac = m.numberingXFrac
        numberingYFrac = m.numberingYFrac
        numberingBgArgb = m.numberingBgArgb
        numberingFgArgb = m.numberingFgArgb
        numberingWeight = m.numberingWeight
        pageNumberPosition = m.pageNumberPosition
        pageNumberStyle = m.pageNumberStyle
        historyBusy = false
    }

    private fun recordEdit() {
        if (historyBusy) return
        editorHistory.record(captureMemento())
    }

    
    fun seedHistory(undoCount: Int, redoCount: Int) {
        if (undoCount <= 0 && redoCount <= 0) return
        editorHistory.seed(captureMemento(), undoCount, redoCount)
    }

    fun restorePageSelections(
        size: Collection<Int>,
        bg: Collection<Int>,
        del: Collection<Int>,
        dup: Collection<Int>,
        num: Collection<Int>
    ) {
        pageSizeSelection.clear(); pageSizeSelection.addAll(size)
        pageBgColorSelection.clear(); pageBgColorSelection.addAll(bg)
        pageDeleteSelection.clear(); pageDeleteSelection.addAll(del)
        pageDuplicateSelection.clear(); pageDuplicateSelection.addAll(dup)
        pageNumberingSelection.clear(); pageNumberingSelection.addAll(num)
    }

private fun beginCoalescedEdit(key: String = "edit") {
        if (historyBusy) return
        editorHistory.beginCoalesce(captureMemento(), key)
    }

    fun endCoalescedEdit() {
        editorHistory.endCoalesce()
    }

    fun undoOne() {
        if (historyBusy) return
        val m = editorHistory.undo(captureMemento()) ?: return
        restoreMemento(m)
    }

    fun redoOne() {
        if (historyBusy) return
        val m = editorHistory.redo(captureMemento()) ?: return
        restoreMemento(m)
    }

    fun undoAll() {
        if (historyBusy) return
        val m = editorHistory.undoAll(captureMemento()) ?: return
        restoreMemento(m)
    }

    fun redoAll() {
        if (historyBusy) return
        val m = editorHistory.redoAll(captureMemento()) ?: return
        restoreMemento(m)
    }

    private fun clearHistory() {
        editorHistory.clear()
    }



    var selectedImageSizeOption: ImageSizeOption by mutableStateOf(imageSizePreferences.getSavedOption())
        private set

    var imagesPerRow: Int by mutableStateOf(3)
        private set

    var imageSpacingDp: Int by mutableStateOf(6)
        private set

    var imageCellAspectRatio: Float by mutableStateOf(1.192929f)
        private set
    /** Grid/fallback ratio. Not changed while Resize Images targets selected images. */
    var layoutCellAspectRatio: Float by mutableStateOf(1.192929f)
        private set

    var imageCornerRadiusPercent: Int by mutableStateOf(0)
        private set

    // ---- Page Tools state ----
    var pageAspectRatio: Float by mutableStateOf(0.589f)
    /** Per-page aspect overrides; missing key uses [pageAspectRatio]. */
    val pageAspectOverrides = mutableStateMapOf<Int, Float>()
    /** Page indices selected in Set Page Size tool. */
    val pageSizeSelection = mutableStateListOf<Int>()
    private var pageSizeSnap: PageSizeSnap? = null

    var isPageLandscape: Boolean by mutableStateOf(false)
        private set

    var pageMarginDp: Int by mutableStateOf(10)
        private set

    var pageBackgroundColor: Long by mutableStateOf(imageSizePreferences.getPageBackgroundColor())
    val pageBackgroundColorOverrides = mutableStateMapOf<Int, Long>()
    val pageBgColorSelection = mutableStateListOf<Int>()
    val pageDeleteSelection = mutableStateListOf<Int>()
    val pageDuplicateSelection = mutableStateListOf<Int>()

    var pageBackgroundImageUri: Uri? by mutableStateOf(null)
        private set

    var pageBackgroundBitmap: Bitmap? by mutableStateOf(null)
    val pageBackgroundBitmapOverrides = mutableStateMapOf<Int, Bitmap>()
    private val pdfPageRasterIndices = hashSetOf<Int>()
    private val pdfPageRasters = mutableMapOf<Int, Bitmap>()
    var pageBackgroundKind: String by mutableStateOf("SOLID")
    val pageBackgroundKindOverrides = mutableStateMapOf<Int, String>()
    var pageBackgroundColor2: Long by mutableStateOf(0xFF90CAF9L)
    val pageBackgroundColor2Overrides = mutableStateMapOf<Int, Long>()
    var pageBackgroundGradientDir: String by mutableStateOf("TTB")
    val pageBackgroundGradientDirOverrides = mutableStateMapOf<Int, String>()
    var pageBackgroundPhoto: Bitmap? by mutableStateOf(null)
    var pageBgBlur: Float by mutableStateOf(0f)
    var pageBgImgOpacity: Float by mutableStateOf(100f)
    var pageBgBrightness: Float by mutableStateOf(0f)
    var pageBgOverlayOn: Boolean by mutableStateOf(false)
    var pageBgOverlayColor: Long by mutableStateOf(0xFFFFFFFFL)
    var pageBgOverlayOpacity: Float by mutableStateOf(25f)
    var pageBgBlurType: String by mutableStateOf("gaussian")

    enum class PageNumberPosition { NONE, LEFT, CENTER, RIGHT }
    enum class PageNumberStyle { ARABIC, ROMAN_LOWER, ROMAN_UPPER, ALPHA_LOWER, ALPHA_UPPER }

    var pageNumberPosition: PageNumberPosition by mutableStateOf(PageNumberPosition.NONE)
        private set

    var pageNumberStyle: PageNumberStyle by mutableStateOf(PageNumberStyle.ARABIC)
        private set

    var minPageCount: Int by mutableStateOf(1)
        private set

    var currentProjectFile: File? by mutableStateOf(null)
        private set
    var currentProjectName: String? by mutableStateOf(null)
        private set
    var currentDraftFile: File? by mutableStateOf(null)
        private set
    @Volatile
    private var lastDraftStamp: Long = 0L
    @Volatile
    private var suppressDraftSave: Boolean = false

    var isImporting: Boolean by mutableStateOf(false)
        private set
    var importingPdf: Boolean by mutableStateOf(false)
        private set

    val selectedImageIds: SnapshotStateList<String> = mutableStateListOf()

    var selectionMode: Boolean by mutableStateOf(false)
        private set

    var singleMenuImageId: String? by mutableStateOf(null)
        private set

    var multipleActionsVisible: Boolean by mutableStateOf(false)
        private set

    var reorderMode: Boolean by mutableStateOf(false)
    var pendingReplaceImageId: String? by mutableStateOf(null)
    /** 0=none, 1=move, 2=swap */
    var imagePositionMode: Int by mutableStateOf(0)
    var imagePositionSourceId: String? by mutableStateOf(null)

    private var clipboardImages: List<ImportedImage> by mutableStateOf(emptyList())

    val hasClipboardImages: Boolean
        get() = clipboardImages.isNotEmpty()

    val textElements: SnapshotStateList<TextElement> = mutableStateListOf()

    var addTextMode: Boolean by mutableStateOf(false)
        private set

    var selectedTextId: String? by mutableStateOf(null)
        private set

    var focusedTextId: String? by mutableStateOf(null)
        private set

    var pendingFocusTextId: String? by mutableStateOf(null)
        private set
    private var newTextBoxId: String? = null

    var currentSelection: TextRange by mutableStateOf(TextRange.Zero)
        private set

    var pinnedTextId: String? by mutableStateOf(null)
        private set
    var pinnedSelection: TextRange by mutableStateOf(TextRange.Zero)
        private set

    var holdTextSelection: Boolean by mutableStateOf(false)
        private set

    fun pinTextSelectionIfAny() {
        holdTextSelection = true
        val id = focusedTextId ?: selectedTextId ?: return
        if (!currentSelection.collapsed) {
            pinnedTextId = id
            pinnedSelection = currentSelection
        }
    }

    fun releaseTextSelectionHold() {
        holdTextSelection = false
        if (focusedTextId == null) {
            currentSelection = TextRange.Zero
            pinnedTextId = null
            pinnedSelection = TextRange.Zero
        }
    }

    fun hasLiveTextRange(): Boolean {
        val id = focusedTextId ?: selectedTextId
        return id != null && !currentSelection.collapsed
    }

    fun selectionForEdit(): TextRange {
        if (!currentSelection.collapsed) return currentSelection
        val id = focusedTextId ?: selectedTextId ?: pinnedTextId
        if (id != null && id == pinnedTextId && !pinnedSelection.collapsed) return pinnedSelection
        return currentSelection
    }

    fun hasTextRangeForEdit(): Boolean {
        val id = focusedTextId ?: selectedTextId ?: pinnedTextId
        return id != null && !selectionForEdit().collapsed
    }

    val availableFonts: SnapshotStateList<AppFont> = mutableStateListOf()

    var lastFontImportMessage: String? by mutableStateOf(null)
        private set

    init {
        refreshAvailableFonts()
    }

    fun refreshAvailableFonts() {
        availableFonts.clear()
        availableFonts.addAll(FontCatalog.allFonts(getApplication()))
    }


    fun onProjectSaved(file: File) {
        currentDraftFile?.let { ProjectStore.deleteProject(it) }
        currentDraftFile = null
        currentProjectFile = file
        currentProjectName = file.nameWithoutExtension
        lastDraftStamp = contentStamp()
    }

    fun isDraftSession(): Boolean = ProjectStore.isDraftFile(currentDraftFile)

    private fun isEmptyEditor(): Boolean {
        val hasImage = importedImages.any { !it.id.startsWith("spacer_") }
        return !hasImage &&
            textElements.isEmpty() &&
            minPageCount <= 1 &&
            pageBackgroundBitmap == null &&
            pageBackgroundBitmapOverrides.isEmpty()
    }

    private fun contentStamp(): Long {
        var h = minPageCount.toLong()
        h = h * 31 + imagesPerRow + imageSpacingDp + imageCornerRadiusPercent
        h = h * 31 + pageAspectRatio.toRawBits().toLong()
        h = h * 31 + pageBackgroundColor
        h = h * 31 + pageMarginDp
        importedImages.forEach { img ->
            h = h * 31 + img.id.hashCode()
            h = h * 31 + img.dragOffsetXFrac.toRawBits().toLong()
            h = h * 31 + img.dragOffsetYFrac.toRawBits().toLong()
            h = h * 31 + img.cornerRadiusPercent
            h = h * 31 + if (img.rowExempt) 1 else 0
            h = h * 31 + if (img.forceNewPage) 1 else 0
            h = h * 31 + img.pinnedPage + img.pinnedPerRow
            h = h * 31 + (img.aspectRatioOverride?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (img.numberLabel ?: 0)
            h = h * 31 + (img.linkUrl?.hashCode() ?: 0)
        }
        textElements.forEach { te ->
            h = h * 31 + te.text.hashCode()
            h = h * 31 + te.pageIndex
            h = h * 31 + te.xFraction.toRawBits().toLong()
            h = h * 31 + te.yFraction.toRawBits().toLong()
            h = h * 31 + te.fontSizeSp.toRawBits().toLong()
            h = h * 31 + te.boxWidthFrac.toRawBits().toLong()
            h = h * 31 + te.boxHeightFrac.toRawBits().toLong()
            h = h * 31 + te.rotationDeg.toRawBits().toLong()
            h = h * 31 + te.fontId.hashCode()
        }
        h = h * 31 + pageBackgroundKind.hashCode()
        h = h * 31 + pageBackgroundColor2
        h = h * 31 + pageBackgroundGradientDir.hashCode()
        pageBackgroundColorOverrides.forEach { (k, c) ->
            h = h * 31 + k
            h = h * 31 + c
        }
        pageBackgroundKindOverrides.forEach { (k, kind) ->
            h = h * 31 + k
            h = h * 31 + kind.hashCode()
        }
        pageBackgroundColor2Overrides.forEach { (k, c) ->
            h = h * 31 + k
            h = h * 31 + c
        }
        pageBackgroundGradientDirOverrides.forEach { (k, d) ->
            h = h * 31 + k
            h = h * 31 + d.hashCode()
        }
        h = h * 31 + pageBgBlur.toRawBits().toLong()
        h = h * 31 + pageBgImgOpacity.toRawBits().toLong()
        h = h * 31 + pageBgBrightness.toRawBits().toLong()
        h = h * 31 + if (pageBgOverlayOn) 1 else 0
        h = h * 31 + pageBgOverlayColor
        h = h * 31 + pageBgOverlayOpacity.toRawBits().toLong()
        val photo = pageBackgroundPhoto
        if (photo != null && !photo.isRecycled) {
            h = h * 31 + System.identityHashCode(photo)
            h = h * 31 + photo.width
            h = h * 31 + photo.height
        }
        val defBg = pageBackgroundBitmap
        if (defBg != null && !defBg.isRecycled) {
            h = h * 31 + System.identityHashCode(defBg)
            h = h * 31 + defBg.width
            h = h * 31 + defBg.height
        }
        pageBackgroundBitmapOverrides.forEach { (k, b) ->
            h = h * 31 + k
            if (!b.isRecycled) {
                h = h * 31 + System.identityHashCode(b)
                h = h * 31 + b.width
                h = h * 31 + b.height
            }
        }
        return h
    }

    @Synchronized
    fun saveDraftBlocking(appContext: Context) {
        if (suppressDraftSave) return
        if (isEmptyEditor()) return
        val stamp = contentStamp()
        if (stamp == lastDraftStamp && currentDraftFile?.exists() == true) return
        val result = ProjectStore.saveDraft(appContext, this, currentDraftFile)
        if (result.success && result.path.isNotBlank()) {
            currentDraftFile = File(result.path)
            lastDraftStamp = stamp
            SessionStore.setEditor(appContext, result.path)
        }
    }

    fun saveDraftAsync(appContext: Context) {
        val ctx = appContext.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            saveDraftBlocking(ctx)
        }
    }

    /** Back from editor: keep work in My Projects, no recover popup. */
    fun leaveEditorAndKeepWork(appContext: Context) {
        suppressDraftSave = true
        val ctx = appContext.applicationContext
        if (currentProjectFile != null && !ProjectStore.isDraftFile(currentProjectFile)) {
            val name = currentProjectName?.ifBlank { null }
                ?: currentProjectFile!!.nameWithoutExtension
            ProjectStore.save(ctx, this, name, overwriteFile = currentProjectFile)
            currentDraftFile?.let { f ->
                if (f.exists() && ProjectStore.isDraftFile(f)) runCatching { f.delete() }
            }
            currentDraftFile = null
            return
        }
        if (isEmptyEditor()) {
            currentDraftFile?.let { f ->
                if (f.exists() && ProjectStore.isDraftFile(f)) runCatching { f.delete() }
            }
            currentDraftFile = null
            return
        }
        lastDraftStamp = 0L
        val result = ProjectStore.saveDraft(ctx, this, currentDraftFile)
        val path = if (result.success && result.path.isNotBlank()) result.path
            else currentDraftFile?.absolutePath
        if (!path.isNullOrBlank()) {
            ProjectStore.promoteDraftToProjects(ctx, path)
        }
        currentDraftFile = null
    }

    fun applyLoadedProject(data: ProjectStore.LoadedProject, file: File) {
        clearHistory()
        importedImages.clear()
        importedImages.addAll(data.images)
        textElements.clear()
        textElements.addAll(data.texts)
        minPageCount = data.minPageCount.coerceAtLeast(1)
        pageAspectRatio = data.pageAspectRatio
        isPageLandscape = data.isPageLandscape
        pageMarginDp = data.pageMarginDp
        pageBackgroundColor = data.pageBackgroundColor
        pageNumberPosition = runCatching {
            PageNumberPosition.valueOf(data.pageNumberPosition)
        }.getOrDefault(PageNumberPosition.NONE)
        pageNumberStyle = runCatching {
            PageNumberStyle.valueOf(data.pageNumberStyle)
        }.getOrDefault(PageNumberStyle.ARABIC)
        pageAspectOverrides.clear()
        pageAspectOverrides.putAll(data.pageAspectOverrides)
        pageBackgroundColorOverrides.clear()
        pageBackgroundColorOverrides.putAll(data.pageBackgroundColorOverrides)
        pageBackgroundBitmap = data.pageBackgroundBitmap
        pageBackgroundBitmapOverrides.clear()
        pageBackgroundBitmapOverrides.putAll(data.pageBackgroundBitmapOverrides)
        pageBackgroundKind = data.pageBackgroundKind
        pageBackgroundColor2 = data.pageBackgroundColor2
        pageBackgroundGradientDir = data.pageBackgroundGradientDir
        pageBackgroundKindOverrides.clear()
        pageBackgroundKindOverrides.putAll(data.pageBackgroundKindOverrides)
        pageBackgroundColor2Overrides.clear()
        pageBackgroundColor2Overrides.putAll(data.pageBackgroundColor2Overrides)
        pageBackgroundGradientDirOverrides.clear()
        pageBackgroundGradientDirOverrides.putAll(data.pageBackgroundGradientDirOverrides)
        pageBackgroundPhoto = data.pageBackgroundPhoto
        pageBgBlur = data.pageBgBlur
        pageBgImgOpacity = data.pageBgImgOpacity
        pageBgBrightness = data.pageBgBrightness
        pageBgOverlayOn = data.pageBgOverlayOn
        pageBgOverlayColor = data.pageBgOverlayColor
        pageBgOverlayOpacity = data.pageBgOverlayOpacity
        pageBgBlurType = data.pageBgBlurType
        pdfPageRasters.clear()
        pdfPageRasterIndices.clear()
        if (data.pdfPageRasters.isNotEmpty()) {
            pdfPageRasters.putAll(data.pdfPageRasters)
            pdfPageRasterIndices.addAll(data.pdfPageRasters.keys)
        } else {
            pageBackgroundBitmapOverrides.keys.forEach { idx ->
                val kind = pageBackgroundKindOverrides[idx] ?: pageBackgroundKind
                if (kind != "IMAGE") {
                    pdfPageRasterIndices.add(idx)
                    val bmp = pageBackgroundBitmapOverrides[idx]
                    if (bmp != null) pdfPageRasters[idx] = bmp
                }
            }
        }
        imagesPerRow = data.imagesPerRow.coerceAtLeast(1)
        imageSpacingDp = data.imageSpacingDp
        imageCellAspectRatio = data.imageCellAspectRatio
        layoutCellAspectRatio = data.layoutCellAspectRatio
        imageCornerRadiusPercent = data.imageCornerRadiusPercent.coerceIn(0, 100)
        selectedImageSizeOption = data.selectedImageSizeOption
        numberingAlpha = data.numberingAlpha
        numberingSizeFrac = data.numberingSizeFrac
        numberingXFrac = data.numberingXFrac
        numberingYFrac = data.numberingYFrac
        numberingBgArgb = data.numberingBgArgb
        numberingFgArgb = data.numberingFgArgb
        numberingWeight = data.numberingWeight
        if (pageBackgroundPhoto != null) {
            rebakePhotoOnPages(emptyList(), inspectBackground(0))
        }
        if (ProjectStore.isDraftFile(file)) {
            currentDraftFile = file
            currentProjectFile = null
        } else {
            currentProjectFile = file
            currentDraftFile = null
        }
        currentProjectName = data.name
        lastDraftStamp = contentStamp()
        selectedImageIds.clear()
        selectedTextId = null
        focusedTextId = null
        pinnedTextId = null
        addTextMode = false
        refreshAvailableFonts()
    }


    private var pdfImportGen: Int = 0

    fun importPdfFromUri(context: Context, uriString: String) {
        clearHistory()
        val gen = pdfImportGen + 1
        pdfImportGen = gen
        importedImages.clear()
        textElements.clear()
        selectedTextId = null
        focusedTextId = null
        pinnedTextId = null
        pageBackgroundBitmapOverrides.clear()
        pdfPageRasterIndices.clear()
        pdfPageRasters.clear()
        pageAspectOverrides.clear()
        pageBackgroundKind = "SOLID"
        pageBackgroundKindOverrides.clear()
        pageBackgroundColor = 0xFFFFFFFFL
        pageBackgroundColorOverrides.clear()
        pageBackgroundBitmap = null
        minPageCount = 1
        importingPdf = true
        isImporting = true
        viewModelScope.launch {
            try {
            val uri = Uri.parse(uriString)
            val pages = withContext(Dispatchers.IO) { renderPdfPages(context, uri) }
            if (gen != pdfImportGen) return@launch
            importedImages.clear()
            textElements.clear()
            pageBackgroundBitmapOverrides.clear()
            pdfPageRasterIndices.clear()
            pdfPageRasters.clear()
            pageAspectOverrides.clear()
            pageBackgroundKind = "SOLID"
            pageBackgroundKindOverrides.clear()
            pageBackgroundColor = 0xFFFFFFFFL
            pageBackgroundColorOverrides.clear()
            pageBackgroundBitmap = null
            pages.forEachIndexed { idx, img ->
                val src = img.bitmap ?: return@forEachIndexed
                val copy = src.copy(Bitmap.Config.ARGB_8888, false)
                pageBackgroundBitmapOverrides[idx] = copy
                pdfPageRasters[idx] = copy
                pdfPageRasterIndices.add(idx)
                if (copy.height > 0) {
                    pageAspectOverrides[idx] = copy.width.toFloat() / copy.height.toFloat()
                }
            }
            val firstBmp = pageBackgroundBitmapOverrides[0]
            if (firstBmp != null && firstBmp.height > 0) {
                val ratio = firstBmp.width.toFloat() / firstBmp.height.toFloat()
                pageAspectRatio = ratio
                imageCellAspectRatio = ratio
                layoutCellAspectRatio = ratio
            }
            minPageCount = pages.size.coerceAtLeast(1)
            updateImagesPerRow(1)
            updateImageSpacing(0)
            updatePageMarginDp(0)
            pageBackgroundColor = 0xFFFFFFFFL
            currentProjectFile = null
            currentProjectName = null
            lastDraftStamp = contentStamp()
            } finally {
                if (gen == pdfImportGen) {
                    isImporting = false
                    importingPdf = false
                }
            }
        }
    }

    private fun renderPdfPages(context: Context, uri: Uri): List<ImportedImage> {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return emptyList()
        val renderer = PdfRenderer(pfd)
        val out = ArrayList<ImportedImage>(renderer.pageCount)
        try {
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val maxW = 1080
                    val scale = maxW.toFloat() / page.width.coerceAtLeast(1)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    out.add(
                        ImportedImage(
                            id = "pdfpage_" + i + "_" + System.nanoTime(),
                            imageUri = uri,
                            bitmap = bmp
                        )
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            renderer.close()
            pfd.close()
        }
        return out
    }

    fun enterAddTextMode() {
        addTextMode = true
        selectedTextId = null
    }

    fun addTextAt(pageIndex: Int, xFraction: Float, yFraction: Float) {
        beginCoalescedEdit("newText")
        val id = "text_" + System.currentTimeMillis() + "_" + textElements.size
        textElements.add(
            TextElement(
                id = id,
                pageIndex = pageIndex,
                xFraction = xFraction,
                yFraction = yFraction
            )
        )
        selectedTextId = id
        addTextMode = false
        pendingFocusTextId = id
        newTextBoxId = id
    }

    fun selectText(id: String) {
        selectedTextId = id
    }

    fun deselectText() {
        selectedTextId = null
        focusedTextId = null
        holdTextSelection = false
        pinnedTextId = null
        pinnedSelection = TextRange.Zero
        currentSelection = TextRange.Zero
    }

    fun consumePendingFocus() {
        pendingFocusTextId = null
    }

    fun onTextFocused(id: String) {
        focusedTextId = id
        selectedTextId = id
    }

    fun restoreTextFocus(id: String) {
        if (id.isBlank()) return
        pendingFocusTextId = id
        focusedTextId = id
        selectedTextId = id
    }

    fun onTextUnfocused(id: String) {
        if (id == newTextBoxId) newTextBoxId = null
        endCoalescedEdit()
        if (focusedTextId == id) {
            focusedTextId = null
        }
    }

    fun updateTextValue(id: String, newText: String, newSelection: TextRange) {
        val textKey = if (id == newTextBoxId) "newText" else "text"
        val index = textElements.indexOfFirst { it.id == id }
        if (index >= 0) {
            val current = textElements[index]
            if (newText != current.text) {
                beginCoalescedEdit(textKey)
            }
            val adjustedBold = if (newText != current.text) {
                adjustRangesForEdit(current.text, newText, current.boldRanges)
            } else {
                current.boldRanges
            }
            val adjustedItalic = if (newText != current.text) {
                adjustRangesForEdit(current.text, newText, current.italicRanges)
            } else {
                current.italicRanges
            }
            val adjustedUnderline = if (newText != current.text) {
                adjustRangesForEdit(current.text, newText, current.underlineRanges)
            } else {
                current.underlineRanges
            }
            val adjustedStrike = if (newText != current.text) {
                adjustRangesForEdit(current.text, newText, current.strikeRanges)
            } else {
                current.strikeRanges
            }
            val adjustedQuote = if (newText != current.text) {
                adjustRangesForEdit(current.text, newText, current.quoteRanges)
            } else {
                current.quoteRanges
            }
            val adjustedFonts = if (newText != current.text) {
                adjustFontRangesForEdit(current.text, newText, current.fontRanges)
            } else {
                current.fontRanges
            }
            val adjustedColors = if (newText != current.text) {
                adjustColorRangesForEdit(current.text, newText, current.colorRanges)
            } else {
                current.colorRanges
            }
            val adjustedBgColors = if (newText != current.text) {
                adjustColorRangesForEdit(current.text, newText, current.bgColorRanges)
            } else {
                current.bgColorRanges
            }
            val adjustedLinks = if (newText != current.text) {
                adjustLinkRangesForEdit(current.text, newText, current.linkRanges)
            } else {
                current.linkRanges
            }
            val adjustedShadows = if (newText != current.text) {
                adjustShadowRangesForEdit(current.text, newText, current.shadowRanges)
            } else {
                current.shadowRanges
            }
            val textChanged = newText != current.text
            textElements[index] = current.copy(
                text = newText,
                boldRanges = adjustedBold,
                italicRanges = adjustedItalic,
                underlineRanges = adjustedUnderline,
                strikeRanges = adjustedStrike,
                quoteRanges = adjustedQuote,
                colorRanges = adjustedColors,
                fontRanges = adjustedFonts,
                linkRanges = adjustedLinks,
                bgColorRanges = adjustedBgColors,
                shadowRanges = adjustedShadows
            )
            if (!newSelection.collapsed) {
                currentSelection = newSelection
                pinnedTextId = id
                pinnedSelection = newSelection
            } else if (textChanged || !holdTextSelection) {
                currentSelection = newSelection
                pinnedTextId = null
                pinnedSelection = TextRange.Zero
            }
        }
    }

    fun setTextRotation(id: String, deg: Float) {
        beginCoalescedEdit("textRot")
        val index = textElements.indexOfFirst { it.id == id }
        if (index < 0) return
        val n = ((deg % 360f) + 360f) % 360f
        textElements[index] = textElements[index].copy(rotationDeg = n)
    }

    fun setTextHandleSize(id: String, sizeSp: Float) {
        beginCoalescedEdit("textScale")
        val index = textElements.indexOfFirst { it.id == id }
        if (index < 0) return
        textElements[index] = textElements[index].copy(fontSizeSp = sizeSp.coerceIn(6f, 96f))
    }

    fun setTextBoxSize(id: String, boxWidthFrac: Float, boxHeightFrac: Float) {
        beginCoalescedEdit("textBox")
        val index = textElements.indexOfFirst { it.id == id }
        if (index < 0) return
        textElements[index] = textElements[index].copy(
            boxWidthFrac = boxWidthFrac.coerceIn(0.02f, 1.8f),
            boxHeightFrac = boxHeightFrac.coerceIn(0.02f, 1.8f)
        )
    }

    fun moveText(id: String, xFraction: Float, yFraction: Float) {
        beginCoalescedEdit("moveText")
        val index = textElements.indexOfFirst { it.id == id }
        if (index >= 0) {
            textElements[index] = textElements[index].copy(
                xFraction = xFraction,
                yFraction = yFraction
            )
        }
    }

    fun activeTextAlign(): String {
        val i = activeTextIndex()
        if (i < 0) return "LEFT"
        return textElements[i].textAlign.ifBlank { "LEFT" }.uppercase()
    }

    fun updateSelectedTextAlign(align: String) {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val a = align.uppercase()
        val ok = if (a == "LEFT" || a == "CENTER" || a == "RIGHT" || a == "JUSTIFY") a else "LEFT"
        textElements[index] = textElements[index].copy(textAlign = ok)
    }

    private fun activeTextIndex(): Int {
        val id = focusedTextId ?: selectedTextId ?: return -1
        return textElements.indexOfFirst { it.id == id }
    }

    fun toggleBoldForSelection() {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val selection = selectionForEdit()
        if (selection.collapsed) return

        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return

        val newRanges = toggleStyleRange(element.boldRanges, start, end, element.text.length)
        textElements[index] = element.copy(boldRanges = newRanges)
    }

    fun toggleItalicForSelection() {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val selection = selectionForEdit()
        if (selection.collapsed) return

        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return

        val newRanges = toggleStyleRange(element.italicRanges, start, end, element.text.length)
        textElements[index] = element.copy(italicRanges = newRanges)
    }

    fun toggleUnderlineForSelection() {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val selection = selectionForEdit()
        if (selection.collapsed) return
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return
        textElements[index] = element.copy(
            underlineRanges = toggleStyleRange(element.underlineRanges, start, end, element.text.length)
        )
    }

    fun toggleStrikeForSelection() {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val selection = selectionForEdit()
        if (selection.collapsed) return
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return
        textElements[index] = element.copy(
            strikeRanges = toggleStyleRange(element.strikeRanges, start, end, element.text.length)
        )
    }

    fun isSelectionBold(): Boolean {
        val index = activeTextIndex()
        if (index < 0) return false
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return false
        return (start until end).all { i ->
            element.boldRanges.any { i in it }
        }
    }

    fun isSelectionItalic(): Boolean {
        val index = activeTextIndex()
        if (index < 0) return false
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return false
        return (start until end).all { i ->
            element.italicRanges.any { i in it }
        }
    }
fun isSelectionUnderline(): Boolean {
        val index = activeTextIndex()
        if (index < 0) return false
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return false
        return (start until end).all { i ->
            element.underlineRanges.any { i in it }
        }
    }
fun isSelectionStrike(): Boolean {
        val index = activeTextIndex()
        if (index < 0) return false
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val element = textElements[index]
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return false
        return (start until end).all { i ->
            element.strikeRanges.any { i in it }
        }
    }

    fun currentTextFontId(): String {
        val index = activeTextIndex()
        if (index < 0) return FontCatalog.ID_DEFAULT
        val element = textElements[index]
        val selection = selectionForEdit()
        if (!selection.collapsed) {
            val i = selection.min.coerceIn(0, (element.text.length - 1).coerceAtLeast(0))
            element.fontRanges.firstOrNull { i in it.range }?.let { return it.fontId }
        }
        return element.fontId
    }

    fun deleteSelectedText() {
        recordEdit()
        val id = focusedTextId ?: selectedTextId ?: return
        textElements.removeAll { it.id == id }
        if (focusedTextId == id) focusedTextId = null
        if (selectedTextId == id) selectedTextId = null
        pendingFocusTextId = null
    }

    fun applyFontToSelectedText(font: AppFont) {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return
        val element = textElements[index]
        val selection = selectionForEdit()
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (!selection.collapsed && start < end) {
            textElements[index] = element.copy(
                fontRanges = applyFontRange(element.fontRanges, start, end, font.id, element.text.length)
            )
        } else {
            textElements[index] = element.copy(fontId = font.id, fontRanges = emptyList())
        }
    }

    fun importFontFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FontCatalog.importFont(getApplication(), uri)
            }
            if (result != null) {
                refreshAvailableFonts()
                applyFontToSelectedText(result)
                lastFontImportMessage = "Imported: ${result.displayName}"
            } else {
                lastFontImportMessage = "Could not import font"
            }
        }
    }

    fun consumeFontImportMessage() {
        lastFontImportMessage = null
    }

    fun selectImageSizeOption(option: ImageSizeOption) {
        recordEdit()
        selectedImageSizeOption = option
        imageSizePreferences.saveOption(option)
    }


    /** Page Size slider 1..100 → aspect (same as PageSizeSlider). */
    private fun pageSizePercentToRatio(percent: Int): Float {
        val t = percent.coerceIn(1, 100) / 100f
        return 0.4f + t * (2.5f - 0.4f)
    }

    /** Set Image Length slider 1..100 → cell aspect (same as ImageShapeSlider). */
    private fun imageLengthPercentToRatio(percent: Int): Float {
        val minR = 0.3f
        val maxR = 2.0f
        val t = (percent.coerceIn(1, 100) - 1) / 99f
        return minR + t * (maxR - minR)
    }

    /**
     * Import Images ratio presets.
     * Currently only "portrait" (9:16) applies settings; others reserved.
     */
    fun applyImportRatioPreset(key: String) {
        recordEdit()
        when (key) {
            "portrait" -> {
                // 9:16 — Page Size 8, Per Row 4, Spacing 7, Image Length 15
                pageAspectRatio = pageSizePercentToRatio(8)
                pageAspectOverrides.clear()
                imagesPerRow = 4
                imageSpacingDp = 7
                imageCellAspectRatio = imageLengthPercentToRatio(15).also { layoutCellAspectRatio = it }
            }
            "landscape" -> {
                // 16:9 — Page Size 11, Per Row 3, Spacing 6, Image Length 84
                pageAspectRatio = pageSizePercentToRatio(11)
                pageAspectOverrides.clear()
                imagesPerRow = 3
                imageSpacingDp = 6
                imageCellAspectRatio = imageLengthPercentToRatio(84).also { layoutCellAspectRatio = it }
            }
            "square" -> {
                // 1:1 — Page Size 13, Per Row 4, Spacing 6, Image Length 53
                pageAspectRatio = pageSizePercentToRatio(13)
                pageAspectOverrides.clear()
                imagesPerRow = 4
                imageSpacingDp = 6
                imageCellAspectRatio = imageLengthPercentToRatio(53).also { layoutCellAspectRatio = it }
            }
        }
    }


    // ---- Drag Images (offset from grid cell; stays on same page) ----
    var dragModeActive: Boolean by mutableStateOf(false)
    var dragPageIndex: Int by mutableStateOf(0)
    val dragSelectedIds: SnapshotStateList<String> = mutableStateListOf()

    fun startDragImagesOnPage(pageIndex: Int) {
        dragPageIndex = pageIndex.coerceAtLeast(0)
        dragSelectedIds.clear()
        dragModeActive = false
    }

    fun setDragImageSelection(ids: Collection<String>) {
        dragSelectedIds.clear()
        dragSelectedIds.addAll(ids)
    }

    fun enterDragMoveMode() {
        if (dragSelectedIds.isEmpty()) return
        dragModeActive = true
    }

    fun exitDragImages() {
        endCoalescedEdit()
        dragModeActive = false
        dragSelectedIds.clear()
    }

    fun layoutPages(): List<List<ImportedImage>> {
        val grid = importedImages.filter { it.ownerPageIndex == null }
        val per = imagesPerRow.coerceAtLeast(1)
        val pageWidth = 360f
        val aspect = pageAspectRatio.coerceAtLeast(0.1f)
        val pageHeight = pageWidth / aspect
        val pad = if (per == 1) 0f else 10f
        val spacing = imageSpacingDp.toFloat()
        val gridW = (pageWidth - pad * 2f).coerceAtLeast(1f)
        val gridH = (pageHeight - pad * 2f).coerceAtLeast(1f)
        val cellW = if (per <= 1) gridW else (gridW - spacing * (per - 1)) / per
        return packEditorPages(grid, per, spacing, cellW, gridH, imageCellAspectRatio)
    }

    fun layoutPageCount(): Int {
        val imagePages = layoutPages().size
        val textPages = (textElements.maxOfOrNull { it.pageIndex } ?: -1) + 1
        return maxOf(imagePages, textPages, minPageCount, 1)
    }

    fun layoutImagesOnPage(pageIndex: Int): List<ImportedImage> {
        val pages = layoutPages()
        if (pageIndex < 0 || pageIndex >= pages.size) return emptyList()
        return pages[pageIndex].filter { !it.id.startsWith("spacer_") }
    }

    var perRowEditActive: Boolean by mutableStateOf(false)

    fun beginPerRowEdit(selectedIds: Collection<String>) {
        recordEdit()
        val selected = selectedIds.toHashSet()
        val pagesNow = layoutPages()
        val pageOf = HashMap<String, Int>()
        var pi = 0
        while (pi < pagesNow.size) {
            val list = pagesNow[pi]
            var j = 0
            while (j < list.size) {
                pageOf[list[j].id] = pi
                j = j + 1
            }
            pi = pi + 1
        }
        val srcNow = packPageSource
        val currentRow = imagesPerRow
        var idx = 0
        while (idx < importedImages.size) {
            val img = importedImages[idx]
            if (img.ownerPageIndex == null && !img.id.startsWith("spacer_")) {
                val on = selected.contains(img.id)
                val visual = pageOf[img.id]
                var page = img.pinnedPage
                if (visual != null) {
                    if (srcNow.isEmpty()) page = visual
                    else if (visual < srcNow.size && srcNow[visual] >= 0) page = srcNow[visual]
                }
                if (on) {
                    importedImages[idx] = img.copy(rowExempt = false, pinnedPage = page, pinnedPerRow = 0)
                } else {
                    val keepRow = if (img.rowExempt && img.pinnedPerRow > 0) img.pinnedPerRow else currentRow
                    importedImages[idx] = img.copy(rowExempt = true, pinnedPage = page, pinnedPerRow = keepRow)
                }
            }
            idx = idx + 1
        }
        perRowEditActive = true
    }

    fun exitPerRowEdit() {
        endCoalescedEdit()
        perRowEditActive = false
    }

    /** Images currently laid out on [pageIndex] (skips spacers). */
    fun imagesOnPage(pageIndex: Int): List<ImportedImage> {
        val abs = importedImages.filter { it.ownerPageIndex == pageIndex }
        val grid = importedImages.filter { it.ownerPageIndex == null && !it.id.startsWith("spacer_") }
        val per = imagesPerPageCapacity().coerceAtLeast(1)
        val start = pageIndex.coerceAtLeast(0) * per
        val slice = if (start >= grid.size) emptyList() else grid.subList(start, minOf(start + per, grid.size))
        return abs + slice
    }

    fun imagesOnPages(pageIndices: Collection<Int>): List<ImportedImage> {
        if (pageIndices.isEmpty()) return emptyList()
        return pageIndices.sorted().flatMap { imagesOnPage(it) }.distinctBy { it.id }
    }

    private fun groupDragCenter(): Pair<Float, Float> {
        val imgs = dragSelectedIds.mapNotNull { id -> importedImages.firstOrNull { it.id == id } }
        if (imgs.isEmpty()) return 0.5f to 0.5f
        val cx = imgs.map {
            if (it.ownerPageIndex != null) it.absXFrac + it.absWFrac / 2f
            else 0.5f + it.dragOffsetXFrac
        }.average().toFloat()
        val cy = imgs.map {
            if (it.ownerPageIndex != null) it.absYFrac + it.absHFrac / 2f
            else 0.5f + it.dragOffsetYFrac
        }.average().toFloat()
        return cx to cy
    }

    fun dragGroupXPercent(): Float =
        ((groupDragCenter().first).coerceIn(0f, 1f) * 100f).coerceIn(0f, 100f)

    fun dragGroupYPercent(): Float =
        ((groupDragCenter().second).coerceIn(0f, 1f) * 100f).coerceIn(0f, 100f)


    private fun gridCellRectFrac(img: ImportedImage): FloatArray {
        val grid = importedImages.filter { it.ownerPageIndex == null && !it.id.startsWith("spacer_") }
        val idx = grid.indexOfFirst { it.id == img.id }.coerceAtLeast(0)
        val per = imagesPerRow.coerceAtLeast(1)
        val cap = imagesPerPageCapacity().coerceAtLeast(1)
        val onPage = idx % cap
        val col = onPage % per
        val row = onPage / per
        val aspect = pageAspectRatio.coerceAtLeast(0.1f)
        val marginX = if (per == 1) 0f else pageMarginDp / 360f
        val marginY = if (per == 1) 0f else pageMarginDp / (360f / aspect)
        val spacingX = imageSpacingDp / 360f
        val spacingY = imageSpacingDp / (360f / aspect)
        val gridW = (1f - 2f * marginX).coerceAtLeast(0.05f)
        val cellW = ((gridW - spacingX * (per - 1)) / per).coerceAtLeast(0.02f)
        val cellAspect = (img.aspectRatioOverride ?: layoutCellAspectRatio).coerceAtLeast(0.1f)
        val cellH = (cellW / cellAspect * aspect).coerceIn(0.02f, 1f)
        val left = marginX + col * (cellW + spacingX)
        val top = marginY + row * (cellH + spacingY)
        return floatArrayOf(left, top, cellW, cellH)
    }

    private fun clampedGridDrag(img: ImportedImage, ox: Float, oy: Float): Pair<Float, Float> {
        return ox.coerceIn(-1f, 1f) to oy.coerceIn(-1f, 1f)
    }

    fun nudgeDragImages(dx: Float, dy: Float) {
        beginCoalescedEdit("nudge")
        if (dragSelectedIds.isEmpty()) return
        val step = 0.007f
        val ids = dragSelectedIds.toList()
        for (id in ids) {
            val idx = importedImages.indexOfFirst { it.id == id }
            if (idx < 0) continue
            val img = importedImages[idx]
            importedImages[idx] = if (img.ownerPageIndex != null) {
                img.copy(
                    absXFrac = (img.absXFrac + dx * step).coerceIn(0f, 1f - img.absWFrac.coerceIn(0.02f, 1f)),
                    absYFrac = (img.absYFrac + dy * step).coerceIn(0f, 1f - img.absHFrac.coerceIn(0.02f, 1f))
                )
            } else {
                run {
                    val (nx, ny) = clampedGridDrag(img, img.dragOffsetXFrac + dx * step, img.dragOffsetYFrac + dy * step)
                    img.copy(dragOffsetXFrac = nx, dragOffsetYFrac = ny)
                }
            }
        }
    }

    fun centerDragImages() {
        recordEdit()
        if (dragSelectedIds.isEmpty()) return
        val (cx, cy) = groupDragCenter()
        val dx = 0.5f - cx
        val dy = 0.5f - cy
        for (id in dragSelectedIds.toList()) {
            val idx = importedImages.indexOfFirst { it.id == id }
            if (idx < 0) continue
            val img = importedImages[idx]
            importedImages[idx] = if (img.ownerPageIndex != null) {
                img.copy(
                    absXFrac = (img.absXFrac + dx).coerceIn(0f, (1f - img.absWFrac).coerceAtLeast(0f)),
                    absYFrac = (img.absYFrac + dy).coerceIn(0f, (1f - img.absHFrac).coerceAtLeast(0f))
                )
            } else {
                val (nx, ny) = clampedGridDrag(img, img.dragOffsetXFrac + dx, img.dragOffsetYFrac + dy)
                img.copy(dragOffsetXFrac = nx, dragOffsetYFrac = ny)
            }
        }
    }

    fun setDragGroupPositionPercent(xPercent: Float?, yPercent: Float?) {
        beginCoalescedEdit("dragPct")
        if (dragSelectedIds.isEmpty()) return
        val (cx, cy) = groupDragCenter()
        val tx = if (xPercent != null) xPercent.coerceIn(0f, 100f) / 100f else cx
        val ty = if (yPercent != null) yPercent.coerceIn(0f, 100f) / 100f else cy
        val dx = tx - cx
        val dy = ty - cy
        for (id in dragSelectedIds.toList()) {
            val idx = importedImages.indexOfFirst { it.id == id }
            if (idx < 0) continue
            val img = importedImages[idx]
            importedImages[idx] = if (img.ownerPageIndex != null) {
                img.copy(
                    absXFrac = (img.absXFrac + dx).coerceIn(0f, (1f - img.absWFrac).coerceAtLeast(0f)),
                    absYFrac = (img.absYFrac + dy).coerceIn(0f, (1f - img.absHFrac).coerceAtLeast(0f))
                )
            } else {
                val (nx, ny) = clampedGridDrag(img, img.dragOffsetXFrac + dx, img.dragOffsetYFrac + dy)
                img.copy(dragOffsetXFrac = nx, dragOffsetYFrac = ny)
            }
        }
    }

    fun dragImagesByFrac(dxFrac: Float, dyFrac: Float) {
        beginCoalescedEdit("nudge")
        if (dragSelectedIds.isEmpty()) return
        for (id in dragSelectedIds.toList()) {
            val idx = importedImages.indexOfFirst { it.id == id }
            if (idx < 0) continue
            val img = importedImages[idx]
            importedImages[idx] = if (img.ownerPageIndex != null) {
                img.copy(
                    absXFrac = (img.absXFrac + dxFrac).coerceIn(0f, (1f - img.absWFrac).coerceAtLeast(0f)),
                    absYFrac = (img.absYFrac + dyFrac).coerceIn(0f, (1f - img.absHFrac).coerceAtLeast(0f))
                )
            } else {
                img.copy(
                    dragOffsetXFrac = (img.dragOffsetXFrac + dxFrac).coerceIn(-1f, 1f),
                    dragOffsetYFrac = (img.dragOffsetYFrac + dyFrac).coerceIn(-1f, 1f)
                )
            }
        }
    }

    fun syncDragOffset(id: String, ox: Float, oy: Float) {
        val idx = importedImages.indexOfFirst { it.id == id }
        if (idx < 0) return
        val img = importedImages[idx]
        if (img.ownerPageIndex != null) return
        if (kotlin.math.abs(img.dragOffsetXFrac - ox) < 0.0004f &&
            kotlin.math.abs(img.dragOffsetYFrac - oy) < 0.0004f
        ) return
        importedImages[idx] = img.copy(dragOffsetXFrac = ox, dragOffsetYFrac = oy)
    }

    fun ensureDragSelection(id: String) {
        if (id in dragSelectedIds) return
        dragSelectedIds.clear()
        dragSelectedIds.add(id)
    }

    fun updateImagesPerRow(count: Int) {
        beginCoalescedEdit("perRow")
        imagesPerRow = count.coerceIn(1, 20)
    }

    fun updateImageSpacing(dp: Int) {
        beginCoalescedEdit("spacing")
        imageSpacingDp = dp.coerceIn(0, 20)
    }

    var resizeEditActive: Boolean by mutableStateOf(false)
    private val resizeTargetIds: SnapshotStateList<String> = mutableStateListOf()

    fun beginResizeEdit(ids: Collection<String>) {
        resizeTargetIds.clear()
        resizeTargetIds.addAll(ids.filter { !it.startsWith("spacer_") })
        val first = resizeTargetIds.firstOrNull()?.let { id ->
            importedImages.firstOrNull { it.id == id }
        }
        imageCellAspectRatio = (first?.aspectRatioOverride ?: layoutCellAspectRatio).coerceIn(0.3f, 2.0f)
        resizeEditActive = resizeTargetIds.isNotEmpty()
    }

    fun exitResizeEdit() {
        endCoalescedEdit()
        resizeEditActive = false
        resizeTargetIds.clear()
    }

    fun updateImageCellAspectRatio(ratio: Float) {
        beginCoalescedEdit("resize")
        val r = ratio.coerceIn(0.3f, 2.0f)
        imageCellAspectRatio = r
        if (resizeTargetIds.isNotEmpty()) {
            resizeTargetIds.forEach { id ->
                val idx = importedImages.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    importedImages[idx] = importedImages[idx].copy(aspectRatioOverride = r)
                }
            }
        } else {
            layoutCellAspectRatio = r
        }
    }

    var cornersEditActive: Boolean by mutableStateOf(false)
    private val cornerTargetIds: SnapshotStateList<String> = mutableStateListOf()

    fun beginCornerEdit(ids: Collection<String>) {
        cornerTargetIds.clear()
        cornerTargetIds.addAll(ids.filter { !it.startsWith("spacer_") })
        val first = cornerTargetIds.firstOrNull()?.let { id ->
            importedImages.firstOrNull { it.id == id }
        }
        imageCornerRadiusPercent = first?.cornerRadiusPercent?.coerceIn(0, 100) ?: 0
        cornersEditActive = cornerTargetIds.isNotEmpty()
    }

    fun exitCornerEdit() {
        endCoalescedEdit()
        cornersEditActive = false
        cornerTargetIds.clear()
    }

    fun updateImageCornerRadiusPercent(percent: Int) {
        beginCoalescedEdit("corners")
        val p = percent.coerceIn(0, 100)
        imageCornerRadiusPercent = p
        val ids = if (cornerTargetIds.isNotEmpty()) cornerTargetIds
            else importedImages.map { it.id }.filter { !it.startsWith("spacer_") }
        ids.forEach { id ->
            val idx = importedImages.indexOfFirst { it.id == id }
            if (idx >= 0) {
                importedImages[idx] = importedImages[idx].copy(cornerRadiusPercent = p)
            }
        }
    }

    fun importImages(uris: List<Uri>, replaceId: String? = null, startPageIndex: Int = 0) {
        recordEdit()
        val targetBytes = selectedImageSizeOption.targetBytes
        isImporting = true
        pendingReplaceImageId = null

        val imageIds = mutableListOf<String>()

        if (replaceId != null) {
            // Replace one existing image in-place (no reflow).
            val idx = importedImages.indexOfFirst { it.id == replaceId }
            val placeAt = if (idx >= 0) {
                importedImages.removeAt(idx)
                idx
            } else {
                importedImages.size
            }
            uris.forEachIndexed { index, uri ->
                val imageId = "" + uri + "_" + System.currentTimeMillis() + "_" + index
                if (index == 0) {
                    importedImages.add(placeAt, ImportedImage(id = imageId, imageUri = uri))
                } else {
                    // Extra uris after a replace: fill first available slots after this one
                    placeNewImageAtFirstEmpty(imageId, uri, placeAt + index)
                }
                imageIds.add(imageId)
            }
        } else {
            // Fill first available empty slots starting from selected page.
            // Empty = spacer_ slot, or append past current list (next free cell / page).
            // Never insert at the front in a way that shifts existing real images.
            val perPage = imagesPerPageCapacity().coerceAtLeast(1)
            val pageIdx = startPageIndex.coerceAtLeast(0)
            val startSlot = pageIdx * perPage
            minPageCount = maxOf(minPageCount, pageIdx + 1)
            while (importedImages.size < startSlot) {
                val spacerId = "spacer_" + System.nanoTime() + "_" + importedImages.size
                importedImages.add(
                    ImportedImage(id = spacerId, imageUri = null, bitmap = null)
                )
            }
            var cursor = startSlot
            uris.forEachIndexed { index, uri ->
                val imageId = "" + uri + "_" + System.currentTimeMillis() + "_" + index
                cursor = placeNewImageAtFirstEmpty(imageId, uri, cursor)
                imageIds.add(imageId)
            }
        }

        viewModelScope.launch {
            uris.forEachIndexed { index, uri ->
                if (index >= imageIds.size) return@forEachIndexed
                val imageId = imageIds[index]
                val decoded = imageHandler.decode(uri)
                val imageIndex = importedImages.indexOfFirst { it.id == imageId }

                if (decoded != null && imageIndex >= 0) {
                    if (targetBytes == null) {
                        importedImages[imageIndex] = importedImages[imageIndex].copy(bitmap = decoded)
                    } else {
                        val result = withContext(Dispatchers.Default) {
                            imageCompressor.compressToTarget(decoded, targetBytes)
                        }
                        importedImages[imageIndex] = importedImages[imageIndex].copy(
                            bitmap = result.bitmap,
                            approxSizeBytes = result.approxSizeBytes
                        )
                    }
                }
            }
            isImporting = false
        }
    }

    /**
     * Place a new image at the first empty slot at or after [fromIndex].
     * Empty = spacer_ entry (replace in place) or end of list (append).
     * Returns the next index to continue searching from.
     */
    private fun placeNewImageAtFirstEmpty(imageId: String, uri: Uri, fromIndex: Int): Int {
        val start = fromIndex.coerceAtLeast(0)
        for (i in start until importedImages.size) {
            if (importedImages[i].id.startsWith("spacer_")) {
                importedImages[i] = ImportedImage(id = imageId, imageUri = uri)
                return i + 1
            }
        }
        importedImages.add(ImportedImage(id = imageId, imageUri = uri))
        return importedImages.size
    }

    fun openImageMenu(id: String) {
        // Ignore empty spacer slots (used only to offset import start page)
        if (id.startsWith("spacer_")) return
        if (imagePositionMode != 0 && imagePositionSourceId != null) {
            val src = imagePositionSourceId!!
            if (id != src) {
                when (imagePositionMode) {
                    1 -> moveSingleImageTo(src, id)
                    2 -> swapImages(src, id)
                }
            }
            imagePositionMode = 0
            imagePositionSourceId = null
            singleMenuImageId = null
            return
        }
        // Multi-select via long-press removed
        selectionMode = false
        selectedImageIds.clear()
        multipleActionsVisible = false
        singleMenuImageId = if (singleMenuImageId == id) null else id
    }

    fun dismissImageMenu() {
        singleMenuImageId = null
    }

    fun longPressImage(id: String) {
        // Long-press multi-select removed — intentionally no-op
    }

    private fun toggleSelection(id: String) {
        if (selectedImageIds.contains(id)) {
            selectedImageIds.remove(id)
        } else {
            selectedImageIds.add(id)
        }
    }

    
    fun replaceImageBitmap(id: String, newBitmap: android.graphics.Bitmap, sizeBytes: Int? = null) {
        recordEdit()
        val idx = importedImages.indexOfFirst { it.id == id }
        if (idx < 0) return
        val old = importedImages[idx]
        val bytes = sizeBytes ?: run {
            try {
                val stream = java.io.ByteArrayOutputStream()
                newBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                stream.size()
            } catch (_: Exception) {
                if (!newBitmap.isRecycled) newBitmap.byteCount else old.approxSizeBytes
            }
        }
        importedImages[idx] = old.copy(
            bitmap = newBitmap,
            approxSizeBytes = bytes,
            imageUri = null
        )
    }

    fun rotateImageBitmap(id: String, degrees: Float): Boolean {
        recordEdit()
        val img = getImage(id) ?: return false
        val src = img.bitmap ?: return false
        if (src.isRecycled) return false
        if (degrees % 360f == 0f) return true
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        val rotated = android.graphics.Bitmap.createBitmap(
            src, 0, 0, src.width, src.height, matrix, true
        )
        replaceImageBitmap(id, rotated)
        return true
    }

    fun cropImageBitmap(
        id: String,
        leftFrac: Float,
        topFrac: Float,
        rightFrac: Float,
        bottomFrac: Float
    ): Boolean {
        recordEdit()
        val img = getImage(id) ?: return false
        val src = img.bitmap ?: return false
        if (src.isRecycled) return false
        val l = (leftFrac.coerceIn(0f, 1f) * src.width).toInt().coerceIn(0, src.width - 1)
        val t = (topFrac.coerceIn(0f, 1f) * src.height).toInt().coerceIn(0, src.height - 1)
        val r = (rightFrac.coerceIn(0f, 1f) * src.width).toInt().coerceIn(l + 1, src.width)
        val b = (bottomFrac.coerceIn(0f, 1f) * src.height).toInt().coerceIn(t + 1, src.height)
        val w = r - l
        val h = b - t
        if (w < 2 || h < 2) return false
        val cropped = android.graphics.Bitmap.createBitmap(src, l, t, w, h)
        replaceImageBitmap(id, cropped)
        return true
    }

    
    fun setImageLinkUrl(id: String, url: String?) {
        recordEdit()
        val idx = importedImages.indexOfFirst { it.id == id }
        if (idx < 0) return
        val cleaned = url?.trim()?.takeIf { it.isNotEmpty() }
        val normalized = cleaned?.let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it
            else "https://$it"
        }
        val old = importedImages[idx]
        importedImages[idx] = old.copy(linkUrl = normalized)
        singleMenuImageId = null
    }

    
    val pageNumberingSelection = mutableStateListOf<Int>()
    val numberingTargetIds = mutableStateListOf<String>()
    var numberingEditMode: Boolean by mutableStateOf(false)
    var numberingAlpha: Float by mutableStateOf(0.9f)
    var numberingSizeFrac: Float by mutableStateOf(0.18f)
    var numberingXFrac: Float by mutableStateOf(0.5f)
    var numberingYFrac: Float by mutableStateOf(0.5f)
    var numberingBgArgb: Long by mutableStateOf(0xFF7C4DFFL)
    var numberingFgArgb: Long by mutableStateOf(0xFFFFFFFFL)
    var numberingWeight: Float by mutableStateOf(0.67f)
    /** true = style screen (colors + thickness) before position edit */
    var numberingStyleScreen: Boolean by mutableStateOf(false)
    /** 0 = transparency/size panel, 1 = move panel */
    var numberingEditStep: Int by mutableStateOf(0)

    init {
        // restore last applied numbering style as defaults
        numberingBgArgb = imageSizePreferences.getNumberingBg()
        numberingFgArgb = imageSizePreferences.getNumberingFg()
        numberingWeight = imageSizePreferences.getNumberingWeight()
        numberingAlpha = imageSizePreferences.getNumberingAlpha()
        numberingSizeFrac = imageSizePreferences.getNumberingSize()
    }

    fun clearPageNumberingSelection() { pageNumberingSelection.clear() }

    fun togglePageNumberingSelection(pageIndex: Int) {
        if (pageNumberingSelection.contains(pageIndex)) pageNumberingSelection.remove(pageIndex)
        else pageNumberingSelection.add(pageIndex)
    }

    fun toggleSelectAllPagesForNumbering(pageCount: Int) {
        val count = pageCount.coerceAtLeast(1)
        val all = pageNumberingSelection.size >= count && (0 until count).all { it in pageNumberingSelection }
        pageNumberingSelection.clear()
        if (!all) pageNumberingSelection.addAll(0 until count)
    }

    fun beginNumberingSelection(pages: Collection<Int>, ids: Collection<String>) {
        val clean = ids.filter { it.isNotBlank() && !it.startsWith("spacer_") }
        if (clean.isEmpty()) return
        pageNumberingSelection.clear()
        pageNumberingSelection.addAll(pages.filter { it >= 0 })
        numberingTargetIds.clear()
        numberingTargetIds.addAll(clean)
        numberingStyleScreen = true
    }

    fun startNumberingEdit(imagesPerPage: Int) {
        if (numberingTargetIds.isNotEmpty()) {
            val wanted = numberingTargetIds.toHashSet()
            val onChosenPages = HashSet<String>()
            for (p in pageNumberingSelection) {
                for (img in layoutImagesOnPage(p)) {
                    if (!img.id.startsWith("spacer_")) onChosenPages.add(img.id)
                }
            }
            for (i in importedImages.indices) {
                val img = importedImages[i]
                if (img.id in onChosenPages && img.id !in wanted && img.numberLabel != null) {
                    importedImages[i] = img.copy(numberLabel = null)
                }
            }
            var n = 1
            for (i in importedImages.indices) {
                val img = importedImages[i]
                if (img.id in wanted && !img.id.startsWith("spacer_")) {
                    importedImages[i] = img.copy(
                        numberLabel = n,
                        numberXFrac = numberingXFrac,
                        numberYFrac = numberingYFrac,
                        numberSizeFrac = numberingSizeFrac,
                        numberAlpha = numberingAlpha,
                        numberBgArgb = numberingBgArgb,
                        numberFgArgb = numberingFgArgb,
                        numberWeight = numberingWeight
                    )
                    n += 1
                }
            }
            numberingEditMode = true
            numberingEditStep = 0
            return
        }
        val selected = pageNumberingSelection.filter { it >= 0 }.toSortedSet()
        if (selected.isEmpty()) return
        var n = 1
        val total = documentPageCount()
        for (p in selected) {
            if (p !in 0 until total) continue
            for (i in imageRangeForPage(p, total)) {
                val img = importedImages[i]
                importedImages[i] = img.copy(
                    numberLabel = n++,
                    numberXFrac = numberingXFrac,
                    numberYFrac = numberingYFrac,
                    numberSizeFrac = numberingSizeFrac,
                    numberAlpha = numberingAlpha,
                    numberBgArgb = numberingBgArgb,
                    numberFgArgb = numberingFgArgb,
                    numberWeight = numberingWeight
                )
            }
        }
        numberingEditMode = true
        numberingEditStep = 0
    }

    fun updateNumberingLiveStyle() {
        beginCoalescedEdit("numStyle")
        for (i in importedImages.indices) {
            val img = importedImages[i]
            if (img.numberLabel != null) {
                importedImages[i] = img.copy(
                    numberXFrac = numberingXFrac,
                    numberYFrac = numberingYFrac,
                    numberSizeFrac = numberingSizeFrac,
                    numberAlpha = numberingAlpha,
                    numberBgArgb = numberingBgArgb,
                    numberFgArgb = numberingFgArgb,
                    numberWeight = numberingWeight
                )
            }
        }
    }

    fun nudgeNumbering(dx: Float, dy: Float) {
        beginCoalescedEdit("numNudge")
        // Small fractional step so hold-to-repeat feels smooth (same idea as Drag Images).
        val step = 0.01f
        numberingXFrac = (numberingXFrac + dx * step).coerceIn(0f, 1f)
        numberingYFrac = (numberingYFrac + dy * step).coerceIn(0f, 1f)
        updateNumberingLiveStyle()
    }

    fun centerNumbering() {
        recordEdit()
        numberingXFrac = 0.5f
        numberingYFrac = 0.5f
        updateNumberingLiveStyle()
    }

    
    fun openNumberingStyleScreen() {
        if (pageNumberingSelection.isEmpty()) return
        numberingStyleScreen = true
    }

    fun confirmNumberingStyleAndEdit(imagesPerPage: Int) {
        numberingStyleScreen = false
        startNumberingEdit(imagesPerPage)
    }

    fun cancelNumberingStyleScreen() {
        numberingStyleScreen = false
        numberingTargetIds.clear()
    }

    fun finishNumberingEdit() {
        endCoalescedEdit()
        updateNumberingLiveStyle()
        imageSizePreferences.saveNumberingStyle(
            numberingBgArgb, numberingFgArgb, numberingWeight, numberingAlpha, numberingSizeFrac
        )
        numberingEditMode = false
        numberingStyleScreen = false
        numberingEditStep = 0
        pageNumberingSelection.clear()
        numberingTargetIds.clear()
    }

    fun backNumberingEditToStyle() {
        updateNumberingLiveStyle()
        numberingEditMode = false
        numberingEditStep = 0
        numberingStyleScreen = true
    }

    fun advanceNumberingEditStep() {
        numberingEditStep = 1
    }

    fun backNumberingEditStep() {
        numberingEditStep = 0
    }

    fun getImage(id: String): ImportedImage? =
        importedImages.firstOrNull { it.id == id }

    fun enterSingleReorder(id: String) {
        singleMenuImageId = null
        selectedImageIds.clear()
        selectedImageIds.add(id)
        reorderMode = true
    }

    fun cutSingle(id: String) {
        val image = importedImages.firstOrNull { it.id == id }
        if (image != null) {
            clipboardImages = listOf(image)
            importedImages.remove(image)
        }
        singleMenuImageId = null
    }

    fun copySingle(id: String) {
        val image = importedImages.firstOrNull { it.id == id }
        if (image != null) {
            clipboardImages = listOf(image)
        }
        singleMenuImageId = null
    }

    fun pasteImages() {
        recordEdit()
        if (clipboardImages.isNotEmpty()) {
            importedImages.addAll(clipboardImages)
        }
        singleMenuImageId = null
    }

    fun deleteSingle(id: String) {
        recordEdit()
        deleteImages(listOf(id), keepSpace = false)
    }

    fun deleteSingleKeepSpace(id: String) {
        recordEdit()
        deleteImages(listOf(id), keepSpace = true)
    }

    fun deleteSingleFillSpace(id: String) {
        recordEdit()
        deleteImages(listOf(id), keepSpace = false)
    }

    fun deleteImages(ids: Collection<String>, keepSpace: Boolean) {
        recordEdit()
        val idSet = ids.filter { it.isNotBlank() && !it.startsWith("spacer_") }.toSet()
        if (idSet.isEmpty()) {
            singleMenuImageId = null
            return
        }
        if (keepSpace) {
            // KEEP_SPACE_SLOT: same list slot, same page and same per-row lock.
            for (i in importedImages.indices) {
                val img = importedImages[i]
                if (img.id in idSet) {
                    val rowUsed = if (img.rowExempt && img.pinnedPerRow > 0) img.pinnedPerRow else imagesPerRow
                    val keepAspect = img.aspectRatioOverride ?: if (rowUsed <= 1) {
                        val b = img.bitmap
                        if (b != null && b.height > 0) (b.width.toFloat() / b.height.toFloat()).coerceIn(0.2f, 5f) else null
                    } else {
                        null
                    }
                    importedImages[i] = img.copy(
                        id = "spacer_" + System.nanoTime() + "_" + i,
                        imageUri = null,
                        bitmap = null,
                        linkUrl = null,
                        numberLabel = null,
                        aspectRatioOverride = keepAspect
                    )
                }
            }
        } else {
            importedImages.removeAll { it.id in idSet }
        }
        selectedImageIds.removeAll { it in idSet }
        if (singleMenuImageId in idSet) singleMenuImageId = null
        dragSelectedIds.removeAll { it in idSet }
    }

    fun finishMultipleSelection() {
        multipleActionsVisible = true
    }

    fun closeMultipleActions() {
        multipleActionsVisible = false
    }

    fun getSelectedImages(): List<ImportedImage> =
        importedImages.filter { it.id in selectedImageIds }

    fun cancelSelection() {
        selectionMode = false
        selectedImageIds.clear()
        multipleActionsVisible = false
    }

    fun cutSelected() {
        clipboardImages = getSelectedImages()
        importedImages.removeAll { it.id in selectedImageIds }
        multipleActionsVisible = false
        cancelSelection()
    }

    fun copySelected() {
        clipboardImages = getSelectedImages()
        multipleActionsVisible = false
        cancelSelection()
    }

    fun deleteSelected() {
        importedImages.removeAll { it.id in selectedImageIds }
        multipleActionsVisible = false
        cancelSelection()
    }

    
    fun startReplaceImage(id: String) {
        pendingReplaceImageId = id
        singleMenuImageId = null
    }

    fun startImageMove(id: String) {
        imagePositionSourceId = id
        imagePositionMode = 1
        singleMenuImageId = null
    }

    fun startImageSwap(id: String) {
        imagePositionSourceId = id
        imagePositionMode = 2
        singleMenuImageId = null
    }

    fun cancelImagePositionMode() {
        imagePositionMode = 0
        imagePositionSourceId = null
    }

    fun swapImages(idA: String, idB: String) {
        recordEdit()
        if (idA == idB) return
        val i = importedImages.indexOfFirst { it.id == idA }
        val j = importedImages.indexOfFirst { it.id == idB }
        if (i < 0 || j < 0) return
        val tmp = importedImages[i]
        importedImages[i] = importedImages[j]
        importedImages[j] = tmp
    }

    /**
     * Move [sourceId] to [targetId]'s original list position (insertion,
     * not replacement). The destination image is kept — it (and every
     * image between source and destination) shifts over by one slot.
     * Source is not duplicated. Works across pages (flat image list order).
     */
    fun moveSingleImageTo(sourceId: String, targetId: String) {
        recordEdit()
        if (sourceId == targetId) return

        val sourceIndex = importedImages.indexOfFirst { it.id == sourceId }
        if (sourceIndex < 0) return

        // Capture the destination's index BEFORE removing the source —
        // it must not be recalculated after removal.
        val targetIndex = importedImages.indexOfFirst { it.id == targetId }
        if (targetIndex < 0) return

        // Remove ONLY the source. The destination is never touched/removed.
        val sourceItem = importedImages.removeAt(sourceIndex)

        // Insert the source at the destination's original index. This
        // correctly places it in that exact slot and shifts everything
        // between source and destination by one, in either direction.
        val insertAt = targetIndex.coerceIn(0, importedImages.size)
        importedImages.add(insertAt, sourceItem)
    }

    fun moveSelectedImagesTo(targetId: String) {
        recordEdit()
        val idsToMove = selectedImageIds.toList()
        if (idsToMove.isEmpty() || targetId in idsToMove) return

        val itemsToMove = importedImages.filter { it.id in idsToMove }
        importedImages.removeAll { it.id in idsToMove }

        val targetIndex = importedImages.indexOfFirst { it.id == targetId }
        val insertAt = if (targetIndex < 0) importedImages.size else targetIndex
        importedImages.addAll(insertAt, itemsToMove)
    }

    fun finishReorder() {
        reorderMode = false
        cancelSelection()
    }

    // ---- Page Tools actions ----

    fun addNewPage() {
        recordEdit()
        minPageCount = minPageCount + 1
    }

    fun deleteLastPage() {
        if (minPageCount <= 1) return
        minPageCount = minPageCount - 1
    }

    fun clearPageDeleteSelection() {
        pageDeleteSelection.clear()
    }

    fun togglePageDeleteSelection(pageIndex: Int) {
        if (pageDeleteSelection.contains(pageIndex)) {
            pageDeleteSelection.remove(pageIndex)
        } else {
            pageDeleteSelection.add(pageIndex)
        }
    }

    fun toggleSelectAllPagesForDelete(pageCount: Int) {
        val count = pageCount.coerceAtLeast(1)
        val allSelected = pageDeleteSelection.size >= count &&
            (0 until count).all { it in pageDeleteSelection }
        if (allSelected) {
            pageDeleteSelection.clear()
        } else {
            pageDeleteSelection.clear()
            pageDeleteSelection.addAll(0 until count)
        }
    }

    
    fun clearPageDuplicateSelection() {
        pageDuplicateSelection.clear()
    }

    fun togglePageDuplicateSelection(pageIndex: Int) {
        if (pageDuplicateSelection.contains(pageIndex)) {
            pageDuplicateSelection.remove(pageIndex)
        } else {
            pageDuplicateSelection.add(pageIndex)
        }
    }

    fun toggleSelectAllPagesForDuplicate(pageCount: Int) {
        val count = pageCount.coerceAtLeast(1)
        val allSelected = pageDuplicateSelection.size >= count &&
            (0 until count).all { it in pageDuplicateSelection }
        if (allSelected) {
            pageDuplicateSelection.clear()
        } else {
            pageDuplicateSelection.clear()
            pageDuplicateSelection.addAll(0 until count)
        }
    }

    fun duplicateSelectedPages(imagesPerPage: Int) {
        recordEdit()
        // DUP_PAGE_END: copy selected pages onto new pages after the last page.
        val startTotal = documentPageCount()
        val selected = pageDuplicateSelection.filter { it in 0 until startTotal }.sorted()
        if (selected.isEmpty()) return
        val per = imagesPerPage.coerceAtLeast(1)
        val exemptMode = importedImages.any { it.rowExempt }
        val layoutNow = if (!exemptMode) layoutPages() else emptyList()
        val useLayout = !exemptMode && layoutNow.size == startTotal
        var nextIndex = startTotal
        var maxPin = importedImages.maxOfOrNull { it.pinnedPage.coerceAtLeast(0) } ?: 0
        val jobs = ArrayList<Triple<Int, Int, List<ImportedImage>>>()
        for (src in selected) {
            val newIndex = if (exemptMode) {
                val idx = maxOf(nextIndex, maxPin + 1)
                maxPin = idx
                idx
            } else {
                nextIndex
            }
            val slice = if (useLayout) {
                layoutNow[src].toList()
            } else {
                val range = imageRangeForPage(src, startTotal, per)
                if (range.isEmpty()) emptyList() else importedImages.subList(range.first, range.last + 1).toList()
            }
            jobs.add(Triple(src, newIndex, slice))
            nextIndex = newIndex + 1
        }
        for (job in jobs) {
            val src = job.first
            val newIndex = job.second
            val slice = job.third
            val seen = HashSet<String>()
            val copies = ArrayList<ImportedImage>()
            for (img in slice) {
                seen.add(img.id)
                copies.add(duplicateImageForPage(img, newIndex, exemptMode))
            }
            val extras = importedImages.filter { it.ownerPageIndex == src && it.id !in seen }
            for (img in extras) {
                copies.add(duplicateImageForPage(img, newIndex, exemptMode))
            }
            val gridAt = copies.indexOfFirst { it.ownerPageIndex == null }
            if (!exemptMode && gridAt >= 0) {
                copies[gridAt] = copies[gridAt].copy(forceNewPage = true)
            }
            if (copies.isNotEmpty()) importedImages.addAll(copies)
            val textsOnPage = textElements.filter { it.pageIndex == src }
            for (te in textsOnPage) {
                textElements.add(
                    te.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        pageIndex = newIndex
                    )
                )
            }
            pageAspectOverrides[src]?.let { pageAspectOverrides[newIndex] = it }
            pageBackgroundColorOverrides[src]?.let { pageBackgroundColorOverrides[newIndex] = it }
            pageBackgroundBitmapOverrides[src]?.let { pageBackgroundBitmapOverrides[newIndex] = it }
            pageBackgroundKindOverrides[src]?.let { pageBackgroundKindOverrides[newIndex] = it }
            pageBackgroundColor2Overrides[src]?.let { pageBackgroundColor2Overrides[newIndex] = it }
            pageBackgroundGradientDirOverrides[src]?.let { pageBackgroundGradientDirOverrides[newIndex] = it }
            minPageCount = maxOf(minPageCount, newIndex + 1)
        }
        pageDuplicateSelection.clear()
    }

    private fun duplicateImageForPage(img: ImportedImage, newIndex: Int, exemptMode: Boolean): ImportedImage {
        val absolute = img.ownerPageIndex != null
        val grid = !absolute
        val newId = if (img.id.startsWith("spacer_")) {
            "spacer_" + java.util.UUID.randomUUID().toString()
        } else {
            java.util.UUID.randomUUID().toString()
        }
        return img.copy(
            id = newId,
            ownerPageIndex = if (absolute) newIndex else img.ownerPageIndex,
            forceNewPage = false,
            rowExempt = if (exemptMode && grid) true else img.rowExempt,
            pinnedPage = if (exemptMode && grid) newIndex else img.pinnedPage,
            pinnedPerRow = if (exemptMode && grid) {
                if (img.pinnedPerRow > 0) img.pinnedPerRow else imagesPerRow
            } else {
                img.pinnedPerRow
            }
        )
    }

    
    /**
     * Move page [fromIndex] so it lands at [toIndex].
     * All page content (images, text, links, backgrounds) moves with the page.
     */
    /**
     * Move source page so it lands exactly at [toIndex].
     * Example (5 pages, source Page1 → dest Page4):
     *   before: 1, 2, 3, 4, 5
     *   after:  2, 3, 4, 1, 5
     * i.e. page1 content → position 4; pages 2–4 shift one step toward the old source.
     * Content (images, text, links, backgrounds) moves with each page via reorderPages.
     */
    fun movePageTo(fromIndex: Int, toIndex: Int, imagesPerPage: Int) {
        val total = documentPageCount()
        if (fromIndex == toIndex) return
        if (fromIndex !in 0 until total || toIndex !in 0 until total) return

        val order = (0 until total).toMutableList()
        val item = order.removeAt(fromIndex)
        // Insert at destination index in the list AFTER removal so final index == toIndex.
        // Do NOT use toIndex-1 when from < to — that placed the page one slot early.
        order.add(toIndex.coerceIn(0, order.size), item)
        reorderPages(order, imagesPerPage)
    }

    /**
     * Reorder pages by [newOrder] where newOrder[newPos] = oldPos.
     * Each page is treated as a fixed-capacity slot block so empty pages stay empty
     * and images/text/links/backgrounds move with their page.
     */
    fun reorderPages(newOrder: List<Int>, imagesPerPage: Int) {
        recordEdit()
        val total = documentPageCount()
        if (newOrder.size != total || newOrder.toSet() != (0 until total).toSet()) return
        val perPage = imagesPerPage.coerceAtLeast(1)

        // Snapshot every page as exactly [perPage] slots (pad with spacers).
        // This keeps empty pages and mid-page gaps (Keep Space) intact across moves.
        val pageSlots: List<List<ImportedImage>> = (0 until total).map { p ->
            val start = p * perPage
            List(perPage) { slot ->
                val idx = start + slot
                if (idx < importedImages.size) {
                    importedImages[idx]
                } else {
                    ImportedImage(
                        id = "spacer_" + System.nanoTime() + "_" + p + "_" + slot,
                        imageUri = null,
                        bitmap = null
                    )
                }
            }
        }

        val rebuilt = newOrder.flatMap { oldPos -> pageSlots[oldPos] }
        importedImages.clear()
        importedImages.addAll(rebuilt)

        // newOrder[newPos] = oldPos  =>  old page index maps to new page index
        val oldToNew = newOrder.mapIndexed { newPos, oldPos -> oldPos to newPos }.toMap()

        // Text elements (and their links/styles) follow their page
        val newTexts = textElements.map { te ->
            te.copy(pageIndex = oldToNew[te.pageIndex] ?: te.pageIndex)
        }
        textElements.clear()
        textElements.addAll(newTexts)

        fun remapFloat(map: MutableMap<Int, Float>) {
            val old = map.toMap()
            map.clear()
            old.forEach { (k, v) ->
                val nk = oldToNew[k]
                if (nk != null) map[nk] = v
            }
        }
        fun remapLong(map: MutableMap<Int, Long>) {
            val old = map.toMap()
            map.clear()
            old.forEach { (k, v) ->
                val nk = oldToNew[k]
                if (nk != null) map[nk] = v
            }
        }
        fun remapBmp(map: MutableMap<Int, android.graphics.Bitmap>) {
            val old = map.toMap()
            map.clear()
            old.forEach { (k, v) ->
                val nk = oldToNew[k]
                if (nk != null) map[nk] = v
            }
        }
        remapFloat(pageAspectOverrides)
        remapLong(pageBackgroundColorOverrides)
        remapBmp(pageBackgroundBitmapOverrides)

        // Keep document page count stable
        minPageCount = maxOf(minPageCount, total)
    }

    fun deleteSelectedPages(imagesPerPage: Int) {
        recordEdit()
        pageSizeSnap = null
        // DELETE_VISUAL_PAGE: remove the page the user sees, not a different image slice.
        val packed = layoutPages()
        val src = packPageSource.copyOf()
        val total = layoutPageCount()
        val toDelete = pageDeleteSelection.filter { it in 0 until total }.toSortedSet()
        if (toDelete.isEmpty()) return
        if (toDelete.size >= total) {
            importedImages.clear()
            textElements.clear()
            minPageCount = 1
            pageAspectOverrides.clear()
            pageBackgroundColorOverrides.clear()
            pageBackgroundBitmapOverrides.clear()
            pageBackgroundKindOverrides.clear()
            pageBackgroundColor2Overrides.clear()
            pageBackgroundGradientDirOverrides.clear()
            pageDeleteSelection.clear()
            return
        }
        val deleteIds = HashSet<String>()
        val deleteDoc = HashSet<Int>()
        for (v in toDelete) {
            if (v < packed.size) {
                for (img in packed[v]) deleteIds.add(img.id)
            }
            val doc = if (v < src.size) src[v] else v
            if (doc >= 0) deleteDoc.add(doc)
        }
        for (img in importedImages) {
            val owner = img.ownerPageIndex
            if (owner != null && owner in deleteDoc) deleteIds.add(img.id)
        }
        val keptImages = ArrayList<ImportedImage>()
        for (img in importedImages) {
            if (img.id in deleteIds) continue
            val owner = img.ownerPageIndex
            val newOwner = if (owner == null) null else owner - deleteDoc.count { it < owner }
            val newPin = (img.pinnedPage - deleteDoc.count { it < img.pinnedPage }).coerceAtLeast(0)
            if (newOwner != owner || newPin != img.pinnedPage) {
                keptImages.add(img.copy(ownerPageIndex = newOwner, pinnedPage = newPin))
            } else {
                keptImages.add(img)
            }
        }
        importedImages.clear()
        importedImages.addAll(keptImages)
        val keptText = ArrayList<com.pdfcraft.studio.ui.editor.TextElement>()
        for (te in textElements) {
            if (te.pageIndex in deleteDoc) continue
            val shift = deleteDoc.count { it < te.pageIndex }
            keptText.add(if (shift > 0) te.copy(pageIndex = te.pageIndex - shift) else te)
        }
        textElements.clear()
        textElements.addAll(keptText)
        fun shiftLong(map: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Long>) {
            val old = map.toMap()
            map.clear()
            for ((k, v) in old) {
                if (k in deleteDoc) continue
                map[k - deleteDoc.count { it < k }] = v
            }
        }
        fun shiftFloat(map: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>) {
            val old = map.toMap()
            map.clear()
            for ((k, v) in old) {
                if (k in deleteDoc) continue
                map[k - deleteDoc.count { it < k }] = v
            }
        }
        fun shiftStr(map: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, String>) {
            val old = map.toMap()
            map.clear()
            for ((k, v) in old) {
                if (k in deleteDoc) continue
                map[k - deleteDoc.count { it < k }] = v
            }
        }
        fun shiftBmp(map: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, android.graphics.Bitmap>) {
            val old = map.toMap()
            map.clear()
            for ((k, v) in old) {
                if (k in deleteDoc) continue
                map[k - deleteDoc.count { it < k }] = v
            }
        }
        shiftFloat(pageAspectOverrides)
        shiftLong(pageBackgroundColorOverrides)
        shiftBmp(pageBackgroundBitmapOverrides)
        shiftStr(pageBackgroundKindOverrides)
        shiftLong(pageBackgroundColor2Overrides)
        shiftStr(pageBackgroundGradientDirOverrides)
        minPageCount = maxOf(1, minPageCount - toDelete.size)
        pageDeleteSelection.clear()
    }

    fun aspectRatioForPage(pageIndex: Int): Float {
        if (imagesPerRow == 1) {
            val img = importedImages.filter { it.ownerPageIndex == null }.getOrNull(pageIndex)
            val b = img?.bitmap
            if (b != null && b.height > 0) {
                return (b.width.toFloat() / b.height).coerceIn(0.2f, 5f)
            }
        }
        return pageAspectOverrides[pageIndex] ?: pageAspectRatio
    }

    /**
     * How many images fit on one page — SAME math as PdfPagesPreview / PdfGenerator.
     * Uses page aspect, margin, spacing, cell aspect, imagesPerRow.
     */
    fun imagesPerPageCapacity(
        imagesPerRowOverride: Int = imagesPerRow,
        pageAspect: Float = pageAspectRatio,
        spacingDp: Int = imageSpacingDp,
        cellAspect: Float = imageCellAspectRatio,
        marginDp: Int = pageMarginDp
    ): Int {
        if (imagesPerRowOverride.coerceAtLeast(1) == 1) return 1
        val pageWidthDp = 360f
        val aspect = pageAspect.coerceAtLeast(0.1f)
        val pageHeightDp = pageWidthDp / aspect
        val pad = marginDp.toFloat().coerceAtLeast(0f)
        val gridW = (pageWidthDp - pad * 2f).coerceAtLeast(1f)
        val gridH = (pageHeightDp - pad * 2f).coerceAtLeast(1f)
        val spacing = spacingDp.toFloat().coerceAtLeast(0f)
        val perRow = imagesPerRowOverride.coerceAtLeast(1)
        val cellW = (gridW - spacing * (perRow - 1)) / perRow
        val cellH = cellW / cellAspect.coerceAtLeast(0.1f)
        val rows = if (cellH > 0f) {
            (((gridH + spacing) / (cellH + spacing)).toInt()).coerceAtLeast(1)
        } else {
            1
        }
        return (perRow * rows).coerceAtLeast(1)
    }

    /**
     * Visible page count for pickers + tools:
     * max( pages needed for images at real capacity, text pages, minPageCount ).
     * Does NOT use the old wrong imagesPerRow*2 shortcut.
     */
    fun currentPageCountEstimate(imagesPerPage: Int = imagesPerPageCapacity()): Int {
        val perPage = imagesPerPage.coerceAtLeast(1)
        val imagePages = if (importedImages.isEmpty()) 0
            else (importedImages.size + perPage - 1) / perPage
        val textPages = (textElements.maxOfOrNull { it.pageIndex } ?: -1) + 1
        return maxOf(imagePages, textPages, minPageCount, 1)
    }

    fun documentPageCount(): Int = currentPageCountEstimate(imagesPerPageCapacity())

    fun hasExportableContent(): Boolean {
        if (importedImages.isNotEmpty() || textElements.isNotEmpty()) return true
        if (pageBackgroundBitmap != null) return true
        if (pageBackgroundBitmapOverrides.isNotEmpty()) return true
        return minPageCount > 1
    }

    /** Image indices on a layout page (fixed capacity slices, not even-split). */
    fun imageRangeForPage(
        pageIndex: Int,
        pageCount: Int = documentPageCount(),
        perPage: Int = imagesPerPageCapacity()
    ): IntRange {
        val n = importedImages.size
        val capacity = perPage.coerceAtLeast(1)
        if (n == 0 || pageIndex < 0 || pageIndex >= pageCount.coerceAtLeast(1)) {
            return IntRange.EMPTY
        }
        val start = pageIndex * capacity
        if (start >= n) return IntRange.EMPTY
        val end = minOf(start + capacity, n)
        return start until end
    }

    fun selectAllPagesForSize(pageCount: Int) {
        pageSizeSelection.clear()
        pageSizeSelection.addAll(0 until pageCount.coerceAtLeast(1))
    }

    /** First click selects all; second click clears all. */
    fun toggleSelectAllPagesForSize(pageCount: Int) {
        val count = pageCount.coerceAtLeast(1)
        val allSelected = pageSizeSelection.size >= count &&
            (0 until count).all { it in pageSizeSelection }
        if (allSelected) {
            pageSizeSelection.clear()
        } else {
            pageSizeSelection.clear()
            pageSizeSelection.addAll(0 until count)
        }
    }

    fun clearPageSizeSelection() {
        pageSizeSelection.clear()
    }

    fun togglePageSizeSelection(pageIndex: Int) {
        if (pageSizeSelection.contains(pageIndex)) {
            pageSizeSelection.remove(pageIndex)
        } else {
            pageSizeSelection.add(pageIndex)
        }
    }

    fun isPageSizeSelected(pageIndex: Int): Boolean =
        pageSizeSelection.contains(pageIndex)

    fun beginPageSizeEdit() {
        pageSizeSnap = null
    }

    private data class PageSizeSnap(
        val pageIds: List<List<String>>,
        val aspects: Map<Int, Float>,
        val colors: Map<Int, Long>,
        val bitmaps: Map<Int, Bitmap>,
        val kinds: Map<Int, String>,
        val color2: Map<Int, Long>,
        val dirs: Map<Int, String>,
        val textPages: Map<String, Int>,
        val owners: Map<String, Int>
    )

    private fun pageGridHeight(aspect: Float): Float {
        val pageWidth = 360f
        val pageHeight = pageWidth / aspect.coerceAtLeast(0.1f)
        val pad = if (imagesPerRow.coerceAtLeast(1) == 1) 0f else pageMarginDp.toFloat()
        return (pageHeight - pad * 2f).coerceAtLeast(1f)
    }

    private fun pageCellWidth(): Float {
        val pageWidth = 360f
        val pad = if (imagesPerRow.coerceAtLeast(1) == 1) 0f else pageMarginDp.toFloat()
        val gridW = (pageWidth - pad * 2f).coerceAtLeast(1f)
        val per = imagesPerRow.coerceAtLeast(1)
        val spacing = imageSpacingDp.toFloat()
        return if (per <= 1) gridW else (gridW - spacing * (per - 1)) / per
    }

    private fun rowPixelHeight(row: List<ImportedImage>, per: Int, cw: Float): Float {
        var maxH = 1f
        for (img in row) {
            val b = img.bitmap
            val aspect = if (per == 1 && b != null && b.height > 0) {
                (b.width.toFloat() / b.height.toFloat()).coerceIn(0.2f, 5f)
            } else {
                (img.aspectRatioOverride ?: imageCellAspectRatio).coerceIn(0.3f, 2f)
            }
            val h = cw / aspect
            if (h > maxH) maxH = h
        }
        return maxH
    }

    private fun imagesThatFit(images: List<ImportedImage>, height: Float): Int {
        if (images.isEmpty()) return 0
        val per = imagesPerRow.coerceAtLeast(1)
        if (per == 1) return 1
        val cw = pageCellWidth()
        val spacing = imageSpacingDp.toFloat()
        var used = 0f
        var count = 0
        var i = 0
        while (i < images.size) {
            val n = if (images.size - i < per) images.size - i else per
            val row = images.subList(i, i + n)
            val h = rowPixelHeight(row, per, cw)
            val gap = if (count == 0) 0f else spacing
            if (count > 0 && used + gap + h > height + 0.5f) break
            used += gap + h
            count += n
            i += n
        }
        if (count <= 0) {
            val n = if (images.size < per) images.size else per
            return n
        }
        return count
    }

    fun applyPageSizeToSelection(ratio: Float) {
        beginCoalescedEdit("pageSize")
        val r = ratio.coerceIn(0.4f, 8f)
        if (pageSizeSelection.isEmpty()) {
            pageSizeSnap = null
            pageAspectRatio = r
            pageAspectOverrides.clear()
            return
        }
        if (pageSizeSnap == null) {
            val packed = layoutPages()
            val textPages = HashMap<String, Int>()
            for (te in textElements) textPages[te.id] = te.pageIndex
            val owners = HashMap<String, Int>()
            for (img in importedImages) {
                val owner = img.ownerPageIndex
                if (owner != null) owners[img.id] = owner
            }
            pageSizeSnap = PageSizeSnap(
                pageIds = packed.map { page -> page.map { it.id } },
                aspects = pageAspectOverrides.toMap(),
                colors = pageBackgroundColorOverrides.toMap(),
                bitmaps = pageBackgroundBitmapOverrides.toMap(),
                kinds = pageBackgroundKindOverrides.toMap(),
                color2 = pageBackgroundColor2Overrides.toMap(),
                dirs = pageBackgroundGradientDirOverrides.toMap(),
                textPages = textPages,
                owners = owners
            )
        }
        val snap = pageSizeSnap ?: return
        // PAGE_SIZE_FLOW: shrink keeps earlier rows, overflow becomes new pages after that page.
        val byId = HashMap<String, ImportedImage>()
        for (img in importedImages) byId[img.id] = img
        val selected = pageSizeSelection.toSet()
        val outPages = ArrayList<ArrayList<ImportedImage>>()
        val oldToNew = HashMap<Int, Int>()
        val smallH = pageGridHeight(r)
        val defH = pageGridHeight(pageAspectRatio)
        for (old in snap.pageIds.indices) {
            val imgs = ArrayList<ImportedImage>()
            for (id in snap.pageIds[old]) {
                val img = byId[id]
                if (img != null) imgs.add(img)
            }
            if (old !in selected) {
                oldToNew[old] = outPages.size
                outPages.add(imgs)
                continue
            }
            var rest = imgs
            val keepN = imagesThatFit(rest, smallH)
            oldToNew[old] = outPages.size
            outPages.add(ArrayList(rest.take(keepN)))
            rest = ArrayList(rest.drop(keepN))
            while (rest.isNotEmpty()) {
                var take = imagesThatFit(rest, defH)
                if (take <= 0) take = 1
                if (take > rest.size) take = rest.size
                outPages.add(ArrayList(rest.take(take)))
                rest = ArrayList(rest.drop(take))
            }
        }
        val exemptMode = importedImages.any { it.rowExempt }
        val used = HashSet<String>()
        val rebuilt = ArrayList<ImportedImage>()
        for (pi in outPages.indices) {
            val page = outPages[pi]
            for (j in page.indices) {
                val img = page[j]
                if (!used.add(img.id)) continue
                val breakHere = !exemptMode && pi > 0 && j == 0
                rebuilt.add(
                    img.copy(
                        forceNewPage = breakHere,
                        rowExempt = if (exemptMode) true else img.rowExempt,
                        pinnedPage = if (exemptMode) pi else img.pinnedPage,
                        pinnedPerRow = if (exemptMode && img.pinnedPerRow <= 0) imagesPerRow else img.pinnedPerRow
                    )
                )
            }
        }
        for (img in importedImages) {
            if (img.id in used) continue
            val oldOwner = snap.owners[img.id]
            val newOwner = if (oldOwner == null) img.ownerPageIndex else oldToNew[oldOwner] ?: img.ownerPageIndex
            val newPin = if (img.rowExempt) oldToNew[img.pinnedPage] ?: img.pinnedPage else img.pinnedPage
            if (newOwner != img.ownerPageIndex || newPin != img.pinnedPage) {
                rebuilt.add(img.copy(ownerPageIndex = newOwner, pinnedPage = newPin, forceNewPage = false))
            } else {
                rebuilt.add(img.copy(forceNewPage = false))
            }
        }
        importedImages.clear()
        importedImages.addAll(rebuilt)
        for (i in textElements.indices) {
            val te = textElements[i]
            val old = snap.textPages[te.id] ?: continue
            val ni = oldToNew[old] ?: continue
            if (ni != te.pageIndex) textElements[i] = te.copy(pageIndex = ni)
        }
        fun putFloat(src: Map<Int, Float>, selectedValue: Float?) {
            pageAspectOverrides.clear()
            for ((k, v) in src) {
                val ni = oldToNew[k] ?: continue
                pageAspectOverrides[ni] = v
            }
            if (selectedValue != null) {
                for (old in selected) {
                    val ni = oldToNew[old] ?: continue
                    pageAspectOverrides[ni] = selectedValue
                }
            }
        }
        putFloat(snap.aspects, r)
        pageBackgroundColorOverrides.clear()
        for ((k, v) in snap.colors) {
            val ni = oldToNew[k] ?: continue
            pageBackgroundColorOverrides[ni] = v
        }
        pageBackgroundBitmapOverrides.clear()
        for ((k, v) in snap.bitmaps) {
            val ni = oldToNew[k] ?: continue
            pageBackgroundBitmapOverrides[ni] = v
        }
        pageBackgroundKindOverrides.clear()
        for ((k, v) in snap.kinds) {
            val ni = oldToNew[k] ?: continue
            pageBackgroundKindOverrides[ni] = v
        }
        pageBackgroundColor2Overrides.clear()
        for ((k, v) in snap.color2) {
            val ni = oldToNew[k] ?: continue
            pageBackgroundColor2Overrides[ni] = v
        }
        pageBackgroundGradientDirOverrides.clear()
        for ((k, v) in snap.dirs) {
            val ni = oldToNew[k] ?: continue
            pageBackgroundGradientDirOverrides[ni] = v
        }
        val inserted = (outPages.size - snap.pageIds.size).coerceAtLeast(0)
        for (old in selected) {
            if (old in oldToNew) continue
            if (old >= snap.pageIds.size) {
                pageAspectOverrides[old + inserted] = r
            }
        }
        minPageCount = maxOf(minPageCount, outPages.size, 1)
    }

    fun updatePageAspectRatio(ratio: Float) {
        beginCoalescedEdit("pageSize")
        val r = ratio.coerceIn(0.4f, 8f)
        pageAspectRatio = r
        isPageLandscape = r > 1f
    }

    enum class PageOrientation { NORMAL, PORTRAIT, LANDSCAPE, SQUARE }

    fun updatePageOrientation(orientation: PageOrientation) {
        recordEdit()
        val ratio = when (orientation) {
            PageOrientation.NORMAL -> 0.589f
            PageOrientation.PORTRAIT -> 9f / 16f
            PageOrientation.LANDSCAPE -> 16f / 9f
            PageOrientation.SQUARE -> 1f
        }
        pageAspectRatio = ratio
        isPageLandscape = ratio > 1f
        // Set Page Size overrides must not block orientation
        pageAspectOverrides.clear()
        pageSizeSelection.clear()
    }

    /** Keep old Boolean API working if anything still calls it. */
    fun updatePageOrientation(landscape: Boolean) {
        recordEdit()
        updatePageOrientation(
            if (landscape) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT
        )
    }

    fun updatePageMarginDp(dp: Int) {
        recordEdit()
        pageMarginDp = dp.coerceIn(0, 48)
    }

    fun backgroundColorForPage(pageIndex: Int): Long {
        return pageBackgroundColorOverrides[pageIndex] ?: pageBackgroundColor
    }

    fun selectAllPagesForBgColor(pageCount: Int) {
        pageBgColorSelection.clear()
        pageBgColorSelection.addAll(0 until pageCount.coerceAtLeast(1))
    }

    fun togglePageBgColorSelection(pageIndex: Int) {
        if (pageBgColorSelection.contains(pageIndex)) {
            pageBgColorSelection.remove(pageIndex)
        } else {
            pageBgColorSelection.add(pageIndex)
        }
    }


    fun clearPageBgColorSelection() {
        pageBgColorSelection.clear()
    }

    fun toggleSelectAllPagesForBgColor(pageCount: Int) {
        val count = pageCount.coerceAtLeast(1)
        val allSelected = pageBgColorSelection.size >= count &&
            (0 until count).all { it in pageBgColorSelection }
        if (allSelected) {
            pageBgColorSelection.clear()
        } else {
            pageBgColorSelection.clear()
            pageBgColorSelection.addAll(0 until count)
        }
    }

    fun applyBackgroundColorToSelection(colorArgb: Long) {
        recordEdit()
        if (pageBgColorSelection.isEmpty()) {
            pageBackgroundColor = colorArgb
            pageBackgroundColorOverrides.clear()
            imageSizePreferences.savePageBackgroundColor(colorArgb)
            // solid color replaces image on global path
            clearUserBackgroundBitmaps(keepRasters = true)
        } else {
            pageBgColorSelection.forEach { idx ->
                pageBackgroundColorOverrides[idx] = colorArgb
                restorePdfRaster(idx)
            }
        }
    }

    fun updatePageBackgroundColor(colorArgb: Long) {
        beginCoalescedEdit("pageBgColor")
        pageBackgroundColor = colorArgb
        imageSizePreferences.savePageBackgroundColor(colorArgb)
    }

    fun setPageBackgroundFromUri(uri: Uri?) {
        recordEdit()
        if (uri == null) {
            clearPageBackgroundImage()
            return
        }
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                imageHandler.decode(uri, maxDimensionPx = 1600)
            } ?: return@launch
            if (pageBgColorSelection.isEmpty()) {
                pageBackgroundImageUri = uri
                val prev = pageBackgroundBitmap
                pageBackgroundBitmap = bmp
                if (prev !== bmp) recycleOrphan(prev)
                pdfPageRasterIndices.toList().forEach { idx ->
                    replaceOverride(idx, imageBgForPage(bmp, idx))
                }
            } else {
                pageBgColorSelection.forEach { idx ->
                    replaceOverride(idx, imageBgForPage(bmp, idx))
                    pageBackgroundColorOverrides.remove(idx)
                }
            }
        }
    }

    fun clearPageBackgroundImage() {
        recordEdit()
        if (pageBgColorSelection.isEmpty()) {
            clearUserBackgroundBitmaps(keepRasters = true)
        } else {
            pageBgColorSelection.forEach { restorePdfRaster(it) }
        }
    }

    fun backgroundBitmapForPage(pageIndex: Int): Bitmap? {
        return pageBackgroundBitmapOverrides[pageIndex] ?: pageBackgroundBitmap
    }

    fun backgroundColor2ForPage(pageIndex: Int): Long {
        return pageBackgroundColor2Overrides[pageIndex] ?: pageBackgroundColor2
    }

    fun backgroundDirForPage(pageIndex: Int): String {
        return pageBackgroundGradientDirOverrides[pageIndex] ?: pageBackgroundGradientDir
    }

    fun resolvedKind(pageIndex: Int): String {
        return pageBackgroundKindOverrides[pageIndex] ?: pageBackgroundKind
    }

    fun pickerBackgroundBitmap(pageIndex: Int): Bitmap? {
        val photo = pageBackgroundPhoto
        if (photo != null && !photo.isRecycled) return photo
        if (resolvedKind(pageIndex) != "IMAGE") return null
        if (pageIndex in pdfPageRasterIndices) return null
        return backgroundBitmapForPage(pageIndex)
    }

    fun inspectBackground(pageIndex: Int): PageBackgroundSpec {
        val kind = runCatching { PageBgKind.valueOf(resolvedKind(pageIndex)) }
            .getOrDefault(PageBgKind.SOLID)
        val dir = runCatching { GradientDir.valueOf(backgroundDirForPage(pageIndex)) }
            .getOrDefault(GradientDir.TTB)
        val line = parseGradientLine(backgroundDirForPage(pageIndex))
        return PageBackgroundSpec(
            kind = kind,
            color = backgroundColorForPage(pageIndex),
            color2 = backgroundColor2ForPage(pageIndex),
            dir = dir,
            gx0 = line[0],
            gy0 = line[1],
            gx1 = line[2],
            gy1 = line[3],
            blur = pageBgBlur,
            imgOpacity = pageBgImgOpacity,
            brightness = pageBgBrightness,
            overlayOn = pageBgOverlayOn,
            overlayColor = pageBgOverlayColor,
            overlayOpacity = pageBgOverlayOpacity
        )
    }

    fun decodeBackgroundPreview(uri: Uri, onReady: (Bitmap) -> Unit) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                imageHandler.decode(uri, maxDimensionPx = 1600)
            } ?: return@launch
            onReady(bmp)
        }
    }

    private fun compositeUserBgUnderPdf(user: Bitmap, pdf: Bitmap): Bitmap {
        val w = pdf.width.coerceAtLeast(1)
        val h = pdf.height.coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val dest = Rect(0, 0, w, h)
        canvas.drawBitmap(user, null, dest, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        canvas.drawBitmap(pdf, null, dest, paint)
        return out
    }

    private fun imageBgForPage(user: Bitmap, pageIndex: Int): Bitmap {
        val orig = pdfPageRasters[pageIndex]
        return if (orig != null) compositeUserBgUnderPdf(user, orig) else user
    }

    fun originalPdfRasters(): Map<Int, Bitmap> = pdfPageRasters.toMap()

    private fun recycleOrphan(old: Bitmap?) {
        if (old == null || old.isRecycled) return
        if (old === pageBackgroundBitmap) return
        if (pdfPageRasters.values.any { it === old }) return
        if (old === pageBackgroundPhoto) return
        if (pageBackgroundBitmapOverrides.values.any { it === old }) return
        // Compose Image may still be drawing this bitmap — do not recycle.
    }

    private fun replaceOverride(idx: Int, next: Bitmap?) {
        val old = pageBackgroundBitmapOverrides[idx]
        if (next == null) pageBackgroundBitmapOverrides.remove(idx)
        else pageBackgroundBitmapOverrides[idx] = next
        if (old !== next) recycleOrphan(old)
    }

    private fun restorePdfRaster(idx: Int) {
        val orig = pdfPageRasters[idx]
        if (orig != null) replaceOverride(idx, orig) else replaceOverride(idx, null)
    }

    private fun clearUserBackgroundBitmaps(keepRasters: Boolean) {
        val prev = pageBackgroundBitmap
        pageBackgroundBitmap = null
        pageBackgroundImageUri = null
        recycleOrphan(prev)
        if (keepRasters) {
            pageBackgroundBitmapOverrides.keys.toList().forEach { idx ->
                if (idx in pdfPageRasterIndices || pdfPageRasters.containsKey(idx)) restorePdfRaster(idx)
                else replaceOverride(idx, null)
            }
        } else {
            pageBackgroundBitmapOverrides.keys.toList().forEach { replaceOverride(it, null) }
        }
    }

    fun applyPageBackgroundToSelection(spec: PageBackgroundSpec, bitmap: Bitmap?) {
        recordEdit()
        val pages = pageBgColorSelection.toList()
        pageBgBlur = spec.blur.coerceIn(0f, 100f)
        pageBgImgOpacity = spec.imgOpacity.coerceIn(0f, 100f)
        pageBgBrightness = spec.brightness.coerceIn(-100f, 100f)
        pageBgOverlayOn = spec.overlayOn
        pageBgOverlayColor = spec.overlayColor
        pageBgOverlayOpacity = spec.overlayOpacity.coerceIn(0f, 100f)
        pageBgBlurType = spec.blurType.ifBlank { "gaussian" }
        if (bitmap == null) {
            val prev = pageBackgroundPhoto
            pageBackgroundPhoto = null
            recycleOrphan(prev)
            pageBgBlur = 0f
            pageBgImgOpacity = 100f
            pageBgBrightness = 0f
            pageBgOverlayOn = false
        } else if (bitmap !== pageBackgroundPhoto) {
            val prev = pageBackgroundPhoto
            pageBackgroundPhoto = bitmap
            recycleOrphan(prev)
        }
        fun applyGlobal() {
            val photoNow = pageBackgroundPhoto
            pageBackgroundKind = if (photoNow != null) PageBgKind.IMAGE.name else spec.kind.name
            pageBackgroundColor = spec.color
            pageBackgroundColor2 = spec.color2
            pageBackgroundGradientDir = encodeGradientLine(spec.gx0, spec.gy0, spec.gx1, spec.gy1)
            pageBackgroundKindOverrides.clear()
            pageBackgroundColorOverrides.clear()
            pageBackgroundColor2Overrides.clear()
            pageBackgroundGradientDirOverrides.clear()
            if (photoNow != null) rebakePhotoOnPages(emptyList(), spec)
            else clearUserBackgroundBitmaps(keepRasters = true)
        }
        fun applyOne(idx: Int) {
            val photoNow = pageBackgroundPhoto
            pageBackgroundKindOverrides[idx] =
                if (photoNow != null) PageBgKind.IMAGE.name else spec.kind.name
            pageBackgroundColorOverrides[idx] = spec.color
            pageBackgroundColor2Overrides[idx] = spec.color2
            pageBackgroundGradientDirOverrides[idx] = encodeGradientLine(spec.gx0, spec.gy0, spec.gx1, spec.gy1)
            if (photoNow != null) rebakePhotoOnPages(listOf(idx), spec)
            else restorePdfRaster(idx)
        }
        if (pages.isEmpty()) applyGlobal() else pages.forEach { applyOne(it) }
    }

    private fun rebakePhotoOnPages(pages: List<Int>, spec: PageBackgroundSpec) {
        val src = pageBackgroundPhoto ?: return
        if (src.isRecycled) return
        val blur = spec.blur
        val opacity = spec.imgOpacity
        val bright = spec.brightness
        val overlayOn = spec.overlayOn
        val overlayColor = spec.overlayColor
        val overlayOpacity = spec.overlayOpacity
        val g2 = if (spec.kind == PageBgKind.GRADIENT) spec.color2 else null
        val gx0 = spec.gx0
        val gy0 = spec.gy0
        val gx1 = spec.gx1
        val gy1 = spec.gy1
        val blurType = spec.blurType
        val targets = if (pages.isEmpty()) {
            val n = documentPageCount().coerceAtLeast(1)
            if (pdfPageRasterIndices.isNotEmpty()) pdfPageRasterIndices.toList() else (0 until n).toList()
        } else pages.toList()
        viewModelScope.launch {
            val processed = withContext(Dispatchers.Default) {
                runCatching {
                    BackgroundFx.process(
                        src = src,
                        blurPct = blur,
                        opacityPct = opacity,
                        brightness = bright,
                        overlayOn = overlayOn,
                        overlayColor = overlayColor,
                        overlayOpacityPct = overlayOpacity,
                        gradientColor2 = g2,
                        gx0 = gx0, gy0 = gy0, gx1 = gx1, gy1 = gy1,
                        maxSide = 1200,
                        blurType = blurType
                    )
                }.getOrNull()
            } ?: return@launch
            if (processed.isRecycled) return@launch
            if (pageBackgroundPhoto !== src) return@launch
            val prevBmp = pageBackgroundBitmap
            pageBackgroundBitmap = processed
            targets.forEach { idx ->
                replaceOverride(idx, imageBgForPage(processed, idx))
            }
            if (prevBmp !== processed) recycleOrphan(prevBmp)
        }
    }



    fun updatePageNumberPosition(pos: PageNumberPosition) {
        recordEdit()
        pageNumberPosition = pos
    }

    fun updatePageNumberStyle(style: PageNumberStyle) {
        recordEdit()
        pageNumberStyle = style
    }

    fun formatPageNumber(pageIndexZeroBased: Int): String {
        val n = pageIndexZeroBased + 1
        return when (pageNumberStyle) {
            PageNumberStyle.ARABIC -> n.toString()
            PageNumberStyle.ROMAN_LOWER -> toRoman(n).lowercase()
            PageNumberStyle.ROMAN_UPPER -> toRoman(n)
            PageNumberStyle.ALPHA_LOWER -> toAlpha(n).lowercase()
            PageNumberStyle.ALPHA_UPPER -> toAlpha(n)
        }
    }

    private fun toRoman(num: Int): String {
        if (num <= 0) return ""
        val values = listOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = listOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        var n = num
        val sb = StringBuilder()
        for (i in values.indices) {
            while (n >= values[i]) {
                sb.append(symbols[i])
                n -= values[i]
            }
        }
        return sb.toString()
    }

    private fun toAlpha(num: Int): String {
        var n = num
        val sb = StringBuilder()
        while (n > 0) {
            n--
            sb.insert(0, ('A' + (n % 26)).toChar())
            n /= 26
        }
        return sb.toString()
    }

    fun updateSelectedTextSize(sizeSp: Float) {
        beginCoalescedEdit("textSize")
        val id = focusedTextId ?: selectedTextId ?: return
        val index = textElements.indexOfFirst { it.id == id }
        if (index < 0) return
        val clamped = sizeSp.coerceIn(6f, 96f)
        textElements[index] = textElements[index].copy(fontSizeSp = clamped)
    }

    fun selectedTextSizeSp(): Float {
        val id = focusedTextId ?: selectedTextId ?: return 16f
        return textElements.firstOrNull { it.id == id }?.fontSizeSp ?: 16f
    }


    fun applyBulletListToSelection(): Boolean = applyListPrefixToSelection(numbered = false)
    fun applyNumberedListToSelection(): Boolean = applyListPrefixToSelection(numbered = true)

    private fun stripListPrefix(line: String): String {
        if (line.startsWith("• ")) return line.substring(2)
        if (line.startsWith("•")) return line.substring(1).trimStart()
        var i = 0
        while (i < line.length && line[i].isDigit()) i++
        if (i > 0 && i < line.length && line[i] == '.') {
            var j = i + 1
            if (j < line.length && line[j] == ' ') j++
            return line.substring(j)
        }
        return line
    }

    private fun lineBounds(text: String, start: Int, end: Int): Pair<Int, Int> {
        var s = start.coerceIn(0, text.length)
        var e = end.coerceIn(0, text.length)
        if (e < s) { val tmp = s; s = e; e = tmp }
        if (s > 0 && s < text.length && text[s] != '\n') {
            val prev = text.lastIndexOf('\n', s - 1)
            s = if (prev < 0) 0 else prev + 1
        }
        if (e > 0 && e < text.length && text[e] != '\n') {
            val nx = text.indexOf('\n', e)
            e = if (nx < 0) text.length else nx
        } else if (e > s && e < text.length && text[e - 1] == '\n') {
            e -= 1
        }
        return s to e
    }

    private fun spliceText(index: Int, start: Int, end: Int, insertion: String) {
        val element = textElements[index]
        val old = element.text
        val s = start.coerceIn(0, old.length)
        val e = end.coerceIn(0, old.length)
        val newText = old.substring(0, s) + insertion + old.substring(e)
        val delta = insertion.length - (e - s)
        fun mapR(r: IntRange): IntRange? {
            val a = r.first
            val b = r.last
            return when {
                b < s -> r
                a >= e -> (a + delta)..(b + delta)
                else -> {
                    val na = if (a < s) a else s
                    val nb = if (b >= e) b + delta else (s + insertion.length - 1)
                    if (na <= nb) na..nb else null
                }
            }
        }
        fun mapRs(list: List<IntRange>) = list.mapNotNull { mapR(it) }
        fun mapCr(list: List<ColorRange>) = list.mapNotNull { cr -> mapR(cr.range)?.let { ColorRange(it, cr.colorArgb) } }
        fun mapFr(list: List<FontRange>) = list.mapNotNull { fr -> mapR(fr.range)?.let { FontRange(it, fr.fontId) } }
        fun mapLr(list: List<LinkRange>) = list.mapNotNull { lr -> mapR(lr.range)?.let { LinkRange(it, lr.url) } }
        fun mapSr(list: List<ShadowRange>) = list.mapNotNull { sr ->
            mapR(sr.range)?.let { ShadowRange(it, sr.colorArgb, sr.offsetXPx, sr.offsetYPx, sr.blurPx) }
        }
        textElements[index] = element.copy(
            text = newText,
            boldRanges = mapRs(element.boldRanges),
            italicRanges = mapRs(element.italicRanges),
            underlineRanges = mapRs(element.underlineRanges),
            strikeRanges = mapRs(element.strikeRanges),
            quoteRanges = mapRs(element.quoteRanges),
            colorRanges = mapCr(element.colorRanges),
            bgColorRanges = mapCr(element.bgColorRanges),
            fontRanges = mapFr(element.fontRanges),
            linkRanges = mapLr(element.linkRanges),
            shadowRanges = mapSr(element.shadowRanges)
        )
        currentSelection = TextRange(s, s + insertion.length)
        pinnedTextId = element.id
        pinnedSelection = currentSelection
    }

    private fun applyListPrefixToSelection(numbered: Boolean): Boolean {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return false
        val element = textElements[index]
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val text = element.text
        val (ls, le) = lineBounds(text, selection.min, selection.max)
        val chunk = if (ls < le) text.substring(ls, le) else ""
        val rawLines = chunk.split('\n')
        val nonempty = rawLines.filter { it.isNotEmpty() }
        val allBullet = nonempty.isNotEmpty() && nonempty.all { it.startsWith("•") }
        val numRe = Regex("^\\d+\\.\\s?")
        val allNum = nonempty.isNotEmpty() && nonempty.all { numRe.containsMatchIn(it) }
        val stripOnly = if (numbered) allNum else allBullet
        val newLines = rawLines.mapIndexed { i, line ->
            val body = stripListPrefix(line)
            if (line.isEmpty()) line
            else if (stripOnly) body
            else if (numbered) "${i + 1}. $body"
            else "• $body"
        }
        spliceText(index, ls, le, newLines.joinToString("\n"))
        return true
    }

    fun applyQuoteToSelection(): Boolean {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return false
        val element = textElements[index]
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val (ls, le) = lineBounds(element.text, selection.min, selection.max)
        if (ls >= le) return false
        val newQuotes = toggleStyleRange(element.quoteRanges, ls, le, element.text.length)
        textElements[index] = element.copy(quoteRanges = newQuotes)
        currentSelection = TextRange(ls, le)
        pinnedTextId = element.id
        pinnedSelection = currentSelection
        return true
    }

    fun applyDividerToSelection(): Boolean {
        recordEdit()
        val index = activeTextIndex()
        if (index < 0) return false
        val element = textElements[index]
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val text = element.text
        val a = selection.min.coerceIn(0, text.length)
        val b = selection.max.coerceIn(0, text.length)
        val selected = if (a < b) text.substring(a, b) else ""
        val onlyLine = selected.isNotBlank() && selected.trim().all { it == '─' || it == '-' || it == '—' || it == '\n' }
        if (onlyLine) {
            spliceText(index, a, b, "")
            return true
        }
        val divider = "────────────"
        val insertAt = b
        val needLead = insertAt > 0 && text.getOrNull(insertAt - 1) != '\n'
        val needTrail = insertAt < text.length && text.getOrNull(insertAt) != '\n'
        val piece = buildString {
            if (needLead) append('\n')
            append(divider)
            if (needTrail || insertAt == text.length) append('\n')
        }
        spliceText(index, insertAt, insertAt, piece)
        return true
    }

    fun applyLinkToSelection(url: String): Boolean {
        recordEdit()
        val clean = url.trim()
        if (clean.isEmpty()) return false
        val index = activeTextIndex()
        if (index < 0) return false
        val element = textElements[index]
        val selection = selectionForEdit()
        if (selection.collapsed) return false
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)
        if (start >= end) return false
        val linkBlue = 0xFF1976D2L
        val newLinks = applyLinkRange(element.linkRanges, start, end, clean, element.text.length)
        val newColors = applyColorRange(element.colorRanges, start, end, linkBlue, element.text.length)
        textElements[index] = element.copy(linkRanges = newLinks, colorRanges = newColors)
        return true
    }

    fun updateSelectedTextColor(colorArgb: Long) {
        beginCoalescedEdit("textColor")
        val index = activeTextIndex()
        if (index < 0) return
        val element = textElements[index]
        val selection = selectionForEdit()
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)

        if (!selection.collapsed && start < end) {
            // Apply color only to the selected range (same behavior as Bold)
            val newRanges = applyColorRange(
                element.colorRanges,
                start,
                end,
                colorArgb,
                element.text.length
            )
            textElements[index] = element.copy(colorRanges = newRanges)
        } else {
            // No selection → apply to entire text element
            textElements[index] = element.copy(
                textColorArgb = colorArgb,
                colorRanges = emptyList()
            )
        }
    }

    fun updateSelectedTextBgColor(colorArgb: Long?) {
        beginCoalescedEdit("textBg")
        val index = activeTextIndex()
        if (index < 0) return
        val element = textElements[index]
        if (colorArgb == null) {
            textElements[index] = element.copy(bgColorArgb = null, bgColorRanges = emptyList())
            return
        }
        val selection = selectionForEdit()
        val start = selection.min.coerceIn(0, element.text.length)
        val end = selection.max.coerceIn(0, element.text.length)

        if (!selection.collapsed && start < end) {
            val newRanges = applyColorRange(
                element.bgColorRanges,
                start,
                end,
                colorArgb,
                element.text.length
            )
            textElements[index] = element.copy(bgColorRanges = newRanges)
        } else {
            textElements[index] = element.copy(
                bgColorArgb = colorArgb,
                bgColorRanges = emptyList()
            )
        }
    }

    private fun applyShadowRange(
        existing: List<ShadowRange>, start: Int, end: Int,
        colorArgb: Long, offsetXPx: Float, offsetYPx: Float, blurPx: Float, textLength: Int
    ): List<ShadowRange> {
        if (textLength <= 0 || start >= end) return existing
        val s = start.coerceIn(0, textLength)
        val e = end.coerceIn(0, textLength)
        if (s >= e) return existing
        val result = mutableListOf<ShadowRange>()
        for (sr in existing) {
            val rs = sr.range.first
            val re = sr.range.last + 1
            if (re <= s || rs >= e) result.add(sr)
            else {
                if (rs < s) result.add(sr.copy(range = rs until s))
                if (re > e) result.add(sr.copy(range = e until re))
            }
        }
        result.add(ShadowRange(s until e, colorArgb, offsetXPx, offsetYPx, blurPx))
        return result.sortedBy { it.range.first }
    }

    private fun adjustShadowRangesForEdit(oldText: String, newText: String, ranges: List<ShadowRange>): List<ShadowRange> {
        return ranges.mapNotNull { sr ->
            val mapped = adjustColorRangesForEdit(oldText, newText, listOf(ColorRange(sr.range, 0L)))
            val r = mapped.firstOrNull()?.range ?: return@mapNotNull null
            sr.copy(range = r)
        }
    }

    fun updateSelectedTextShadow(
        colorArgb: Long? = null,
        offsetXPx: Float? = null,
        offsetYPx: Float? = null,
        blurPx: Float? = null
    ) {
        beginCoalescedEdit("textShadow")
        val index = activeTextIndex()
        if (index < 0) return
        val cur = textElements[index]
        val selection = selectionForEdit()
        val color = colorArgb ?: cur.shadowColorArgb
        val ox = offsetXPx ?: cur.shadowOffsetXPx
        val oy = offsetYPx ?: cur.shadowOffsetYPx
        val blur = blurPx ?: cur.shadowBlurPx
        val start = selection.min.coerceIn(0, cur.text.length)
        val end = selection.max.coerceIn(0, cur.text.length)
        if (!selection.collapsed && start < end) {
            textElements[index] = cur.copy(
                shadowRanges = applyShadowRange(cur.shadowRanges, start, end, color, ox, oy, blur, cur.text.length)
            )
        }
    }

    fun activeTextElement(): TextElement? {
        val id = focusedTextId ?: selectedTextId ?: return null
        return textElements.firstOrNull { it.id == id }
    }

    fun selectedTextColorArgb(): Long {
        val index = activeTextIndex()
        if (index < 0) return 0xFF000000L
        val element = textElements[index]
        val selection = selectionForEdit()
        if (!selection.collapsed) {
            val start = selection.min.coerceIn(0, element.text.length)
            val end = selection.max.coerceIn(0, element.text.length)
            if (start < end) {
                val colorsInSel = element.colorRanges
                    .filter { it.range.first < end && it.range.last + 1 > start }
                    .map { it.colorArgb }
                    .distinct()
                if (colorsInSel.size == 1) return colorsInSel[0]
            }
        }
        return element.textColorArgb
    }

    fun selectedTextBgColorArgb(): Long? {
        val index = activeTextIndex()
        if (index < 0) return null
        val element = textElements[index]
        val selection = selectionForEdit()
        if (!selection.collapsed) {
            val start = selection.min.coerceIn(0, element.text.length)
            val end = selection.max.coerceIn(0, element.text.length)
            if (start < end) {
                val colorsInSel = element.bgColorRanges
                    .filter { it.range.first < end && it.range.last + 1 > start }
                    .map { it.colorArgb }
                    .distinct()
                if (colorsInSel.size == 1) return colorsInSel[0]
            }
        }
        return element.bgColorArgb
    }
}
