package com.pdfcraft.studio.core.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pdfcraft.studio.ui.editor.ImportedImage
import com.pdfcraft.studio.ui.editor.packEditorPages
import com.pdfcraft.studio.ui.editor.packPageSource
import com.pdfcraft.studio.ui.editor.TextElement
import com.pdfcraft.studio.core.text.FontCatalog
import com.pdfcraft.studio.ui.editor.ShadowRange
import com.pdfcraft.studio.ui.editor.TextGradient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI

/**
 * Builds a PDF from the current editor pages (images + text).
 * Page grid math MUST match the editor preview (PdfPagesPreview)
 * so images never crop at the bottom of a page.
 */
object PdfGenerator {

    data class Result(
        val success: Boolean,
        val fileName: String = "",
        val message: String = "",
        val linkWarning: String? = null,
        val savedUri: Uri? = null
    )

    
    private fun packImagesForPagesPdf(
        images: List<com.pdfcraft.studio.ui.editor.ImportedImage>,
        imagesPerRow: Int,
        spacingPx: Float,
        cellWidthPx: Float,
        gridHeightPx: Float,
        defaultAspect: Float
    ): List<List<com.pdfcraft.studio.ui.editor.ImportedImage>> {
        return packEditorPages(images, imagesPerRow, spacingPx, cellWidthPx, gridHeightPx, defaultAspect)
    }

    private fun mixedImageSlots(
        pageImages: List<ImportedImage>,
        perRow: Int,
        pageWidth: Int,
        spacing: Float,
        cellWidth: Float,
        cellAspect: Float,
        pageMarginDp: Int
    ): List<FloatArray>? {
        var any = false
        for (img in pageImages) {
            if (img.rowExempt) any = true
        }
        if (!any) return null
        val out = ArrayList<FloatArray>(pageImages.size)
        var n = 0
        while (n < pageImages.size) {
            out.add(floatArrayOf(0f, 0f, cellWidth, cellWidth))
            n = n + 1
        }
        val exempt = ArrayList<Int>()
        val flow = ArrayList<Int>()
        var k = 0
        while (k < pageImages.size) {
            if (pageImages[k].rowExempt) exempt.add(k) else flow.add(k)
            k = k + 1
        }
        fun place(idxs: List<Int>, rowCount: Int, startY: Float): Float {
            if (idxs.isEmpty()) return startY
            val r = rowCount.coerceAtLeast(1)
            val marginR = if (r == 1) 0f else pageMarginDp.toFloat() * (pageWidth / 360f)
            val gridW = (pageWidth.toFloat() - marginR * 2f).coerceAtLeast(1f)
            val cw = if (r <= 1) gridW else (gridW - spacing * (r - 1)) / r
            var yy = startY
            var c = 0
            while (c < idxs.size) {
                val count = if (idxs.size - c < r) idxs.size - c else r
                var maxH = 0f
                var col = 0
                while (col < count) {
                    val idx = idxs[c + col]
                    val img = pageImages[idx]
                    val b = img.bitmap
                    val aspect = if (r == 1 && b != null && b.height > 0) {
                        (b.width.toFloat() / b.height).coerceIn(0.2f, 5f)
                    } else {
                        (img.aspectRatioOverride ?: cellAspect).coerceIn(0.3f, 2.0f)
                    }
                    val h = cw / aspect
                    if (h > maxH) maxH = h
                    val left = if (r == 1) 0f else marginR + col * (cw + spacing)
                    out[idx] = floatArrayOf(left, yy, cw, h)
                    col = col + 1
                }
                yy += maxH + spacing
                c += count
            }
            return yy
        }
        var rowEx = perRow
        for (img in pageImages) {
            if (img.rowExempt && img.pinnedPerRow > 0) {
                rowEx = img.pinnedPerRow
                break
            }
        }
        val startEx = if (rowEx == 1) 0f else pageMarginDp.toFloat() * (pageWidth / 360f)
        val y2 = place(exempt, rowEx, startEx)
        place(flow, perRow, y2)
        return out
    }

    fun export(
        context: Context,
        fileName: String,
        images: List<ImportedImage>,
        textElements: List<TextElement>,
        imagesPerRow: Int,
        pageAspectRatio: Float,
        pageBackgroundColor: Long,
        imageSpacingDp: Int = 6,
        imageCellAspectRatio: Float = 0.526f,
        pageMarginDp: Int = 10,
        minPageCount: Int = 1,
        pageAspectRatioForPage: ((Int) -> Float)? = null,
        pageBackgroundColorForPage: ((Int) -> Long)? = null,
        pageBackgroundBitmapForPage: ((Int) -> android.graphics.Bitmap?)? = null,
        pageBackgroundKindForPage: ((Int) -> String)? = null,
        pageBackgroundColor2ForPage: ((Int) -> Long)? = null,
        pageBackgroundDirForPage: ((Int) -> String)? = null,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Result {
        val hasPageRaster = (0 until minPageCount.coerceAtLeast(1)).any {
            pageBackgroundBitmapForPage?.invoke(it) != null
        }
        if (images.isEmpty() && textElements.isEmpty() && !hasPageRaster && minPageCount <= 1) {
            return Result(false, message = "empty")
        }

        // Characters like / : * ? " < > | are not allowed in file names.
        val cleanName = fileName.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim().trimStart('.').ifBlank { "Project" }
        val safeName = if (cleanName.lowercase().endsWith(".pdf")) cleanName else "$cleanName.pdf"

        // Use a fixed logical width; height follows editor page aspect ratio
        val pageWidth = 1080
        val pageHeight = (pageWidth / pageAspectRatio.coerceAtLeast(0.1f)).roundToInt().coerceAtLeast(1)
        // Deterministic wrap: match editor 360dp logical page (not screenWidthDp).
        val wrapScale = 1f

        // Same margin model as editor (pageMarginDp / PAGE_INNER_PADDING)
        val perRow = imagesPerRow.coerceAtLeast(1)
        val margin = if (perRow == 1) 0f else pageMarginDp.toFloat() * (pageWidth / 360f)
        val spacing = imageSpacingDp.toFloat() * (pageWidth / 360f)
        val gridWidth = pageWidth - margin * 2f
        val gridHeight = pageHeight - margin * 2f
        val cellWidth = (gridWidth - spacing * (perRow - 1)) / perRow
        val cellAspect = imageCellAspectRatio.coerceAtLeast(0.1f)
        val cellHeight = cellWidth / cellAspect

        // CRITICAL: same formula as PdfPagesPreview — only full rows that fit
        val rowsPerPage = if (cellHeight > 0f) {
            (((gridHeight + spacing) / (cellHeight + spacing)).toInt()).coerceAtLeast(1)
        } else {
            1
        }
        // Auto-flow images by how many fit per page (same as editor preview).
        val imagesPerPage = (perRow * rowsPerPage).coerceAtLeast(1)
        val imagePages = packImagesForPagesPdf(
            images = images,
            imagesPerRow = perRow,
            spacingPx = spacing,
            cellWidthPx = cellWidth,
            gridHeightPx = gridHeight,
            defaultAspect = cellAspect
        )
        val pageSrc = packPageSource.copyOf()

        // Include every page that has text OR blank pages the user added in the editor.
        // Previously text-only page 2+ were dropped because export only used image chunks.
        val maxTextPageIndex = textElements.maxOfOrNull { it.pageIndex } ?: -1
        val totalPages = maxOf(
            imagePages.size,
            maxTextPageIndex + 1,
            minPageCount.coerceAtLeast(1),
            1
        )
        val pages = List(totalPages) { index ->
            imagePages.getOrElse(index) { emptyList() }
        }

        val linkRects = mutableListOf<LinkRect>()
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Drawing can run out of memory on big documents: fail with a message, never crash.
        // Same label as before, so every return@forEachIndexed keeps its meaning.
        val drawError = pages.forEachIndexedGuarded forEachIndexed@{ pageIndex, pageImages ->
            onProgress?.invoke(pageIndex + 1, pages.size)
            val docPage = if (pageIndex < pageSrc.size) pageSrc[pageIndex] else pageIndex
            val aspect = (if (docPage < 0) pageAspectRatio else (pageAspectRatioForPage?.invoke(docPage) ?: pageAspectRatio)).coerceAtLeast(0.1f)
            val thisPageHeight = (pageWidth / aspect).roundToInt().coerceAtLeast(1)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, thisPageHeight, pageIndex + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            val kind = pageBackgroundKindForPage?.invoke(docPage) ?: "SOLID"
            val c1 = pageBackgroundColorForPage?.invoke(docPage) ?: pageBackgroundColor
            val c2 = pageBackgroundColor2ForPage?.invoke(docPage) ?: 0xFF90CAF9L
            val dir = pageBackgroundDirForPage?.invoke(docPage) ?: "TTB"
            val bgBmp = pageBackgroundBitmapForPage?.invoke(docPage)
            when (kind) {
                "NONE" -> canvas.drawColor(0xFFFFFFFF.toInt())
                "IMAGE" -> {
                    if (bgBmp != null && !bgBmp.isRecycled) {
                        val dest = android.graphics.RectF(0f, 0f, pageWidth.toFloat(), thisPageHeight.toFloat())
                        canvas.drawBitmap(bgBmp, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    } else canvas.drawColor(c1.toInt())
                }
                "GRADIENT" -> {
                    val w = pageWidth.toFloat()
                    val h = thisPageHeight.toFloat()
                    val line = if (dir.startsWith("LINE:")) {
                        val p = dir.removePrefix("LINE:").split(",")
                        if (p.size >= 4) {
                            val a = p[0].toFloatOrNull() ?: 0.5f
                            val b = p[1].toFloatOrNull() ?: 0f
                            val c = p[2].toFloatOrNull() ?: 0.5f
                            val d = p[3].toFloatOrNull() ?: 1f
                            listOf(a * w, b * h, c * w, d * h)
                        } else listOf(0.5f * w, 0f, 0.5f * w, h)
                    } else when (dir) {
                        "LTR" -> listOf(0f, 0.5f * h, w, 0.5f * h)
                        "RTL" -> listOf(w, 0.5f * h, 0f, 0.5f * h)
                        "BTT" -> listOf(0.5f * w, h, 0.5f * w, 0f)
                        "TLBR" -> listOf(0f, 0f, w, h)
                        "BLTR" -> listOf(0f, h, w, 0f)
                        else -> listOf(0.5f * w, 0f, 0.5f * w, h)
                    }
                    val x0 = line[0]
                    val y0 = line[1]
                    val x1 = line[2]
                    val y1 = line[3]
                    val gp = Paint()
                    gp.shader = LinearGradient(x0, y0, x1, y1, c1.toInt(), c2.toInt(), Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, w, h, gp)
                }
                else -> {
                    canvas.drawColor(c1.toInt())
                    if (bgBmp != null && !bgBmp.isRecycled) {
                        val dest = RectF(0f, 0f, pageWidth.toFloat(), thisPageHeight.toFloat())
                        val bp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        bp.colorFilter = PorterDuffColorFilter(c1.toInt(), PorterDuff.Mode.MULTIPLY)
                        canvas.drawBitmap(bgBmp, null, dest, bp)
                    }
                }
            }


            val placed = mixedImageSlots(
                pageImages = pageImages,
                perRow = perRow,
                pageWidth = pageWidth,
                spacing = spacing,
                cellWidth = cellWidth,
                cellAspect = cellAspect,
                pageMarginDp = pageMarginDp
            )
            pageImages.forEachIndexed { idx, img ->
                val hit = if (placed == null) null else placed[idx]
                val row = idx / perRow
                val col = idx % perRow
                val bmpEarly = img.bitmap
                val thisAspect = if (perRow == 1 && bmpEarly != null && bmpEarly.height > 0) {
                    (bmpEarly.width.toFloat() / bmpEarly.height).coerceIn(0.2f, 5f)
                } else {
                    (img.aspectRatioOverride ?: cellAspect).coerceIn(0.3f, 2.0f)
                }
                val drawWidth = if (hit != null) hit[2] else cellWidth
                val thisH = if (hit != null) hit[3] else cellWidth / thisAspect
                val left0: Float
                val top0: Float
                if (hit != null) {
                    left0 = hit[0]
                    top0 = hit[1]
                } else if (perRow == 1) {
                    var y = 0f
                    for (i in 0 until idx) {
                        val prev = pageImages[i]
                        val pb = prev.bitmap
                        val pa = if (pb != null && pb.height > 0)
                            (pb.width.toFloat() / pb.height).coerceIn(0.2f, 5f)
                        else (prev.aspectRatioOverride ?: cellAspect).coerceIn(0.3f, 2.0f)
                        y += cellWidth / pa + spacing
                    }
                    left0 = 0f
                    top0 = y
                } else {
                    if (row >= rowsPerPage) return@forEachIndexed
                    left0 = margin + col * (cellWidth + spacing)
                    top0 = margin + row * (cellHeight + spacing)
                }
                val ox = img.dragOffsetXFrac * pageWidth
                val oy = img.dragOffsetYFrac * thisPageHeight
                var left = left0 + ox
                var top = top0 + oy
                left = left.coerceIn(0f, (pageWidth - drawWidth).coerceAtLeast(0f))
                top = top.coerceIn(0f, (thisPageHeight - thisH).coerceAtLeast(0f))
                val bmp = img.bitmap
                if (bmp != null && !bmp.isRecycled) {
                    val drawn = drawBitmapFit(
                        canvas = canvas,
                        bitmap = bmp,
                        dstLeft = left,
                        dstTop = top,
                        dstWidth = drawWidth,
                        dstHeight = thisH,
                        paint = paint,
                        cornerPercent = img.cornerRadiusPercent
                    )
                    if (drawn != null && img.numberLabel != null) {
                        drawNumberBadge(
                            canvas = canvas,
                            label = img.numberLabel ?: 0,
                            bounds = drawn,
                            xFrac = img.numberXFrac,
                            yFrac = img.numberYFrac,
                            sizeFrac = img.numberSizeFrac,
                            alpha = img.numberAlpha,
                            bgArgb = img.numberBgArgb,
                            fgArgb = img.numberFgArgb,
                            weight = img.numberWeight
                        )
                    }
                    val url = img.linkUrl?.trim()?.takeIf { it.isNotEmpty() }
                    if (drawn != null && url != null) {
                        linkRects.add(
                            LinkRect(
                                pageIndex = pageIndex,
                                left = drawn.left,
                                top = drawn.top,
                                right = drawn.right,
                                bottom = drawn.bottom,
                                pageHeight = thisPageHeight.toFloat(),
                                url = url
                            )
                        )
                    }
                }
            }

            // Text: only elements for this page index (supports partial color + bg ranges)
            // Newlines are drawn as separate lines (editor multiline must match PDF).
            textElements.filter { docPage >= 0 && it.pageIndex == docPage }.forEach { teRaw ->
                // Same position clamp as the editor: 8dp margin left / top, room for the
                // minimum box (24x20dp) right / bottom. Range never empty (maxOf).
                val pdfDp = pageWidth / 360f
                val pdfLoX = (8f * pdfDp / pageWidth.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                val pdfLoY = (8f * pdfDp / thisPageHeight.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                val pdfHiX = maxOf(pdfLoX, 1f - 32f * pdfDp / pageWidth.toFloat().coerceAtLeast(1f))
                val pdfHiY = maxOf(pdfLoY, 1f - 28f * pdfDp / thisPageHeight.toFloat().coerceAtLeast(1f))
                var te = if (teRaw.xFraction.isNaN() || teRaw.yFraction.isNaN()) teRaw else teRaw.copy(
                    xFraction = teRaw.xFraction.coerceIn(pdfLoX, pdfHiX),
                    yFraction = teRaw.yFraction.coerceIn(pdfLoY, pdfHiY)
                )
                textPaint.textSize = te.fontSizeSp * (pageWidth / 360f)
                // Preview line breaks (exact). Same font / page ratio and drawn position as the
                // preview; no own wrap / fit then. Missing or stale: fallback wrap below.
                val pdfLineCache = PdfTextLineCache.matching(
                    teRaw.id, teRaw.text, teRaw.fontSizeSp, teRaw.boxWidthFrac, teRaw.boxHeightFrac,
                    teRaw.xFraction, teRaw.yFraction, teRaw.rotationDeg,
                    PdfTextLineCache.styleKeyOf(teRaw)
                )
                if (pdfLineCache != null) {
                    textPaint.textSize = (pdfLineCache.fontPerPageW * pageWidth).coerceIn(1f, 2000f)
                    te = te.copy(
                        xFraction = pdfLineCache.effX.coerceIn(0f, 1f),
                        yFraction = pdfLineCache.effY.coerceIn(0f, 1f)
                    )
                }
                val baseTf = FontCatalog.resolveTypeface(context, te.fontId)
                textPaint.typeface = baseTf
                // Left / top dynamic cap (same as the editor): overflow past the margin is taken
                // off the box width / height so the right / bottom edge stays put. Rotated: no cap.
                if (pdfLineCache == null && kotlin.math.abs(te.rotationDeg) <= 0.5f && !teRaw.xFraction.isNaN() && !teRaw.yFraction.isNaN()) {
                    val ovL = (te.xFraction - teRaw.xFraction).coerceAtLeast(0f)
                    val ovT = (te.yFraction - teRaw.yFraction).coerceAtLeast(0f)
                    val minWf = 24f * pdfDp / pageWidth.toFloat().coerceAtLeast(1f)
                    val minHf = 20f * pdfDp / thisPageHeight.toFloat().coerceAtLeast(1f)
                    if (ovL > 0f && te.text.isNotEmpty()) {
                        val wrapK = (pageWidth * wrapScale * 0.90f).coerceAtLeast(1f)
                        val natWf = if (te.boxWidthFrac > 0.01f) {
                            te.boxWidthFrac
                        } else {
                            val roomF = (1f - te.xFraction).coerceIn(0.02f, 1f)
                            val natLines = wrapTextLines(te.text, textPaint, (roomF * wrapK).coerceAtLeast(textPaint.textSize))
                            (natLines.maxOfOrNull { lr ->
                                textPaint.measureText(te.text.substring(lr.first, lr.last + 1))
                            } ?: 0f) / wrapK + 0.002f
                        }
                        te = te.copy(boxWidthFrac = (natWf - ovL).coerceAtLeast(minWf))
                    }
                    if (ovT > 0f && te.text.isNotEmpty()) {
                        val natHf = if (te.boxHeightFrac > 0.01f) {
                            te.boxHeightFrac
                        } else {
                            val fmN = textPaint.fontMetrics
                            var lhN = fmN.descent - fmN.ascent + fmN.leading
                            if (lhN < textPaint.textSize * 0.8f) lhN = textPaint.textSize * 1.2f
                            val roomF = (1f - te.xFraction).coerceIn(0.02f, 1f)
                            val wN = if (te.boxWidthFrac > 0.01f) {
                                minOf(te.boxWidthFrac, roomF) * pageWidth * wrapScale * 0.90f
                            } else {
                                roomF * pageWidth * wrapScale * 0.90f
                            }
                            val nN = wrapTextLines(te.text, textPaint, wN.coerceAtLeast(textPaint.textSize)).size.coerceAtLeast(1)
                            (textPaint.textSize + (nN - 1) * lhN + fmN.descent) / thisPageHeight.toFloat().coerceAtLeast(1f)
                        }
                        te = te.copy(boxHeightFrac = (natHf - ovT).coerceAtLeast(minHf))
                    }
                }
                // Fit inside the box's real height in the PDF too (tablet wrapScale can make
                // more lines than the preview): shrink the PDF font only when needed.
                val pdfFitRoomFrac = (1f - te.xFraction).coerceIn(0.02f, 1f)
                val pdfRoomH = ((1f - te.yFraction) * thisPageHeight - 4f).coerceAtLeast(1f)
                val pdfAvailH = if (pdfLineCache != null) {
                    (pdfLineCache.textHFrac * thisPageHeight * 1.05f + textPaint.textSize * 0.2f).coerceAtLeast(1f)
                } else if (te.boxHeightFrac > 0.01f) {
                    minOf(pdfRoomH, (te.boxHeightFrac * thisPageHeight).coerceAtLeast(1f))
                } else {
                    pdfRoomH
                }
                if (te.text.isNotEmpty() && pdfLineCache == null) {
                    var fitGuard = 0
                    while (fitGuard < 12) {
                        val fitWrap = (
                            if (te.boxWidthFrac > 0.01f) {
                                minOf(minOf(te.boxWidthFrac, pdfFitRoomFrac) * pageWidth * wrapScale * 0.90f, pdfFitRoomFrac * pageWidth * 0.98f)
                            } else {
                                minOf(pdfFitRoomFrac * pageWidth * wrapScale * 0.90f, pdfFitRoomFrac * pageWidth * 0.98f)
                            }
                            ).coerceAtLeast(textPaint.textSize)
                        val fmFit = textPaint.fontMetrics
                        var lhFit = fmFit.descent - fmFit.ascent + fmFit.leading
                        if (lhFit < textPaint.textSize * 0.8f) lhFit = textPaint.textSize * 1.2f
                        val nFit = wrapTextLines(te.text, textPaint, fitWrap).size.coerceAtLeast(1)
                        val needH = textPaint.textSize + (nFit - 1) * lhFit + fmFit.descent
                        if (needH <= pdfAvailH || textPaint.textSize <= 1f || needH <= 0f) break
                        textPaint.textSize = (textPaint.textSize * (pdfAvailH / needH).coerceIn(0.5f, 0.97f))
                            .coerceAtLeast(1f)
                        fitGuard++
                    }
                }
                val baseX = te.xFraction * pageWidth
                val firstBaseline = te.yFraction * thisPageHeight + textPaint.textSize
                val fullText = te.text
                if (fullText.isEmpty()) return@forEach
                val isB = BooleanArray(fullText.length)
                val isI = BooleanArray(fullText.length)
                val isU = BooleanArray(fullText.length)
                val isS = BooleanArray(fullText.length)
                fun mark(arr: BooleanArray, ranges: List<IntRange>) {
                    ranges.forEach { r ->
                        val a = r.first.coerceIn(0, fullText.length)
                        val b = (r.last + 1).coerceIn(0, fullText.length)
                        for (k in a until b) arr[k] = true
                    }
                }
                mark(isB, te.boldRanges)
                mark(isI, te.italicRanges)
                mark(isU, te.underlineRanges)
                mark(isS, te.strikeRanges)
                val isQ = BooleanArray(fullText.length)
                mark(isQ, te.quoteRanges)
                val fids = Array(fullText.length) { te.fontId }
                te.fontRanges.forEach { fr ->
                    val a = fr.range.first.coerceIn(0, fullText.length)
                    val b = (fr.range.last + 1).coerceIn(0, fullText.length)
                    for (k in a until b) fids[k] = fr.fontId
                }

                val fg = LongArray(fullText.length) { te.textColorArgb }
                te.colorRanges.forEach { cr ->
                    val s = cr.range.first.coerceIn(0, fullText.length)
                    val e = (cr.range.last + 1).coerceIn(0, fullText.length)
                    for (i in s until e) fg[i] = cr.colorArgb
                }
                val bg = arrayOfNulls<Long>(fullText.length)
                if (te.bgColorArgb != null) {
                    for (i in fullText.indices) bg[i] = te.bgColorArgb
                }
                te.bgColorRanges.forEach { cr ->
                    val s = cr.range.first.coerceIn(0, fullText.length)
                    val e = (cr.range.last + 1).coerceIn(0, fullText.length)
                    for (i in s until e) bg[i] = cr.colorArgb
                }
                // Spaces inside a background RANGE keep their background.
                val bgKeep = BooleanArray(fullText.length)
                te.bgColorRanges.forEach { cr ->
                    val s = cr.range.first.coerceIn(0, fullText.length)
                    val e = (cr.range.last + 1).coerceIn(0, fullText.length)
                    for (i in s until e) bgKeep[i] = true
                }
                val bd = arrayOfNulls<Long>(fullText.length)
                if (te.borderColorArgb != null) {
                    for (i in fullText.indices) bd[i] = te.borderColorArgb
                }
                te.borderRanges.forEach { cr ->
                    val s = cr.range.first.coerceIn(0, fullText.length)
                    val e = (cr.range.last + 1).coerceIn(0, fullText.length)
                    for (i in s until e) bd[i] = cr.colorArgb
                }
                // Spread (glow) color per character.
                val spr = arrayOfNulls<Long>(fullText.length)
                if (te.spreadDp > 0.01f) {
                    if (te.spreadColorArgb != null) {
                        for (i in fullText.indices) spr[i] = te.spreadColorArgb
                    }
                    te.spreadRanges.forEach { cr ->
                        val s = cr.range.first.coerceIn(0, fullText.length)
                        val e = (cr.range.last + 1).coerceIn(0, fullText.length)
                        for (i in s until e) spr[i] = cr.colorArgb
                    }
                }

                val fm = textPaint.fontMetrics
                var lineH = fm.descent - fm.ascent + fm.leading
                if (lineH < textPaint.textSize * 0.8f) lineH = textPaint.textSize * 1.2f
                val rotDeg = te.rotationDeg
                if (kotlin.math.abs(rotDeg) > 0.05f) {
                    val longestLine = fullText.split('\n').maxByOrNull { it.length } ?: fullText
                    val approxW = textPaint.measureText(longestLine).coerceAtLeast(8f)
                    val approxH = (1 + fullText.count { ch -> ch == '\n' }) * lineH
                    canvas.save()
                    canvas.rotate(
                        rotDeg,
                        baseX + (if (pdfLineCache != null && pdfLineCache.rowWFrac > 0f) pdfLineCache.rowWFrac * pageWidth else approxW) / 2f,
                        te.yFraction * thisPageHeight +
                            (if (pdfLineCache != null && pdfLineCache.rowHFrac > 0f) pdfLineCache.rowHFrac * thisPageHeight else approxH) / 2f
                    )
                }

                // Box stays inside the page: never wrap past the right page border.
                val pageRoomFrac = (1f - te.xFraction).coerceIn(0.02f, 1f)
                val pageRoomPx = pageRoomFrac * pageWidth * 0.98f
                val wrapW = if (te.boxWidthFrac > 0.01f) {
                    minOf(minOf(te.boxWidthFrac, pageRoomFrac) * pageWidth * wrapScale * 0.90f, pageRoomPx)
                        .coerceAtLeast(textPaint.textSize)
                } else {
                    minOf(pageRoomFrac * pageWidth * wrapScale * 0.90f, pageRoomPx)
                        .coerceAtLeast(textPaint.textSize)
                }
                val lines = pdfLineCache?.ranges(fullText.length) ?: wrapTextLines(fullText, textPaint, wrapW)
                // Box grows with its content: never cut lines off at the set height.
                val boxBottom = 1e20f

                val bgPaint = Paint(textPaint).apply { style = Paint.Style.FILL }
                val scale = pageWidth / 360f
                val sh = arrayOfNulls<ShadowRange>(fullText.length)
                if (te.shadowRanges.isEmpty() &&
                    (te.shadowBlurPx > 0.01f || te.shadowOffsetXPx != 0f || te.shadowOffsetYPx != 0f)
                ) {
                    val whole = ShadowRange(0 until fullText.length, te.shadowColorArgb, te.shadowOffsetXPx, te.shadowOffsetYPx, te.shadowBlurPx)
                    for (si in fullText.indices) sh[si] = whole
                }
                te.shadowRanges.forEach { sr ->
                    val s = sr.range.first.coerceIn(0, fullText.length)
                    val e = (sr.range.last + 1).coerceIn(0, fullText.length)
                    for (si in s until e) sh[si] = sr
                }
                // Backgrounds first (merged per run), then all text and outlines on top.
                val bgPieces = ArrayList<FloatArray>()
                val bgPieceColors = ArrayList<Long>()
                val textOps = ArrayList<() -> Unit>()
                val glowOps = ArrayList<() -> Unit>()
                // Measured once per element (was once per line = O(lines^2)).
                val pdfMinBox = textPaint.measureText("000000000000000")
                val pdfLongestW = lines.maxOfOrNull { lr ->
                    textPaint.measureText(fullText.substring(lr.first, lr.last + 1))
                } ?: 0f
                // Gradient text / border colors: one LinearGradient over the whole text box.
                val gradL = baseX
                val gradT = te.yFraction * thisPageHeight
                val gradW = (
                    if (pdfLineCache != null && pdfLineCache.rowWFrac > 0f) pdfLineCache.rowWFrac * pageWidth
                    else pdfLongestW
                    ).coerceAtLeast(1f)
                val gradH = (
                    if (pdfLineCache != null && pdfLineCache.rowHFrac > 0f) pdfLineCache.rowHFrac * thisPageHeight
                    else lines.size.coerceAtLeast(1) * lineH
                    ).coerceAtLeast(1f)
                fun gradShader(c: Long): Shader? {
                    if (!TextGradient.isGradient(c)) return null
                    val gl = TextGradient.line(c)
                    return LinearGradient(
                        gradL + gl[0] * gradW, gradT + gl[1] * gradH,
                        gradL + gl[2] * gradW, gradT + gl[3] * gradH,
                        TextGradient.opaque1(c).toInt(), TextGradient.opaque2(c).toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
                lines.forEachIndexed { lineNo, range ->
                    val y = firstBaseline + lineNo * lineH
                    if (y > boxBottom) return@forEachIndexed
                    var x = baseX
                    var i = range.first
                    val lineEnd = range.last + 1
                    val lineStr = fullText.substring(range.first, lineEnd)
                    val lineW = textPaint.measureText(lineStr)
                    // Background skips leading/trailing spaces of each line.
                    var lineTs = range.first
                    while (lineTs < lineEnd && fullText[lineTs].isWhitespace() && !bgKeep[lineTs]) lineTs++
                    var lineTe = lineEnd
                    while (lineTe > lineTs && fullText[lineTe - 1].isWhitespace() && !bgKeep[lineTe - 1]) lineTe--
                    val boxW = if (fullText.isEmpty()) pdfMinBox else pdfLongestW
                    val align = te.textAlign.uppercase()
                    if (align == "CENTER") x = baseX + ((boxW - lineW) / 2f)
                    else if (align == "RIGHT") x = baseX + (boxW - lineW)
                    val justifyExtra =
                        if (align == "JUSTIFY" && lineNo < lines.lastIndex) {
                            val spaces = lineStr.count { ch -> ch == ' ' }
                            if (spaces > 0) ((boxW - lineW) / spaces).coerceAtLeast(0f) else 0f
                        } else 0f
                    val lineQuoted = (range.first until lineEnd).any { qi -> qi in isQ.indices && isQ[qi] }
                    if (lineQuoted) {
                        val bar = Paint(textPaint).apply {
                            color = 0xFF1976D2.toInt()
                            style = Paint.Style.FILL
                            shader = null
                        }
                        val top = y + fm.ascent
                        val bot = y + fm.descent
                        val qx = x
                        textOps.add { canvas.drawRect(qx - 8f * scale, top, qx - 5f * scale, bot, bar) }
                        x += 10f * scale
                    }
                    while (i < lineEnd) {
                        val cFg = fg[i]
                        val cBg = bg[i]
                        val cSh = sh[i]
                        var j = i + 1
                        while (j < lineEnd && fg[j] == cFg && bg[j] == cBg && bd[j] == bd[i] && sh[j] == cSh && spr[j] == spr[i] && isB[j] == isB[i] && isI[j] == isI[i] && isU[j] == isU[i] && isS[j] == isS[i] && fids[j] == fids[i]) j++
                        val segment = fullText.substring(i, j)
                        val w = textPaint.measureText(segment)
                        if (cBg != null) {
                            var s0 = maxOf(i, lineTs)
                            var e0 = minOf(j, lineTe)
                            while (s0 < e0 && fullText[s0].isWhitespace() && !bgKeep[s0]) s0++
                            while (e0 > s0 && fullText[e0 - 1].isWhitespace() && !bgKeep[e0 - 1]) e0--
                            if (s0 < e0) {
                                val bx0 = x + textPaint.measureText(fullText, i, s0)
                                val bx1 = bx0 + textPaint.measureText(fullText, s0, e0)
                                bgPieces.add(
                                    floatArrayOf(
                                        s0.toFloat(),
                                        e0.toFloat(),
                                        bx0,
                                        y - textPaint.textSize * 0.85f,
                                        bx1,
                                        y + textPaint.textSize * 0.2f
                                    )
                                )
                                bgPieceColors.add(cBg)
                            }
                        }
                        textPaint.color = cFg.toInt()
                        textPaint.shader = gradShader(cFg)
                        val italic = isI[i] || isQ[i]
                        val st = when {
                            isB[i] && italic -> Typeface.BOLD_ITALIC
                            isB[i] -> Typeface.BOLD
                            italic -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        textPaint.typeface = Typeface.create(FontCatalog.resolveTypeface(context, fids[i]), st)
                        textPaint.isUnderlineText = isU[i]
                        textPaint.isStrikeThruText = isS[i]
                        if (cSh != null) {
                            textPaint.setShadowLayer(
                                (cSh.blurPx * scale).coerceAtLeast(0.01f),
                                cSh.offsetXPx * scale,
                                cSh.offsetYPx * scale,
                                cSh.colorArgb.toInt()
                            )
                        } else {
                            textPaint.clearShadowLayer()
                        }
                        val cSp = spr[i]
                        if (cSp != null) {
                            val glowPaint = Paint(textPaint)
                            // A glow cannot be a gradient: gradient spread uses the middle color.
                            val cSpSolid = TextGradient.solid(cSp).toInt()
                            glowPaint.shader = null
                            glowPaint.color = cSpSolid
                            glowPaint.isUnderlineText = false
                            glowPaint.isStrikeThruText = false
                            glowPaint.setShadowLayer((te.spreadDp * scale).coerceAtLeast(0.01f), 0f, 0f, cSpSolid)
                            val gx = x
                            glowOps.add {
                                canvas.drawText(segment, gx, y, glowPaint)
                                canvas.drawText(segment, gx, y, glowPaint)
                            }
                        }
                        val cBd = bd[i]
                        if (cBd != null) {
                            val strokePaint = Paint(textPaint)
                            strokePaint.clearShadowLayer()
                            strokePaint.style = Paint.Style.STROKE
                            strokePaint.strokeJoin = Paint.Join.ROUND
                            strokePaint.strokeWidth = (te.borderWidthDp * scale).coerceAtLeast(0.5f)
                            strokePaint.color = cBd.toInt()
                            strokePaint.shader = gradShader(cBd)
                            strokePaint.isUnderlineText = false
                            strokePaint.isStrikeThruText = false
                            val sx = x
                            textOps.add { canvas.drawText(segment, sx, y, strokePaint) }
                        }
                        val fillPaint = Paint(textPaint)
                        val fx = x
                        textOps.add { canvas.drawText(segment, fx, y, fillPaint) }
                        x += w
                        if (justifyExtra > 0f) x += justifyExtra * segment.count { ch -> ch == ' ' }
                        i = j
                    }
                }

                // One rounded rect per contiguous background run (across wrapped lines).
                var pi = 0
                while (pi < bgPieces.size) {
                    val col = bgPieceColors[pi]
                    val fp = bgPieces[pi]
                    var l = fp[2]
                    var t = fp[3]
                    var r = fp[4]
                    var b = fp[5]
                    var lastEnd = fp[1].toInt()
                    var pj = pi + 1
                    while (pj < bgPieces.size && bgPieceColors[pj] == col) {
                        val nb = bgPieces[pj]
                        val ns = nb[0].toInt()
                        var joined = ns >= lastEnd
                        var q = lastEnd
                        while (joined && q < ns) {
                            if (q >= bg.size || bg[q] != col) joined = false
                            q++
                        }
                        if (!joined) break
                        l = minOf(l, nb[2])
                        t = minOf(t, nb[3])
                        r = maxOf(r, nb[4])
                        b = maxOf(b, nb[5])
                        lastEnd = nb[1].toInt()
                        pj++
                    }
                    bgPaint.color = col.toInt()
                    bgPaint.alpha = (((col shr 24) and 0xFFL).toFloat() * te.bgAlpha.coerceIn(0f, 1f))
                        .toInt().coerceIn(0, 255)
                    val rTop = t - te.bgPadTopDp * scale
                    val rBottom = b + te.bgPadBottomDp * scale
                    val rR = (te.bgCornerDp * scale).coerceIn(0f, ((rBottom - rTop) / 2f).coerceAtLeast(0f))
                    bgPaint.shader = if (TextGradient.isGradient(col)) {
                        // Gradient background: one linear gradient across this rounded rect.
                        val gl = TextGradient.line(col)
                        val rl = l - te.bgPadLeftDp * scale
                        val rw = (r + te.bgPadRightDp * scale) - rl
                        val rh = rBottom - rTop
                        LinearGradient(
                            rl + gl[0] * rw, rTop + gl[1] * rh,
                            rl + gl[2] * rw, rTop + gl[3] * rh,
                            TextGradient.opaque1(col).toInt(), TextGradient.opaque2(col).toInt(),
                            Shader.TileMode.CLAMP
                        )
                    } else {
                        null
                    }
                    canvas.drawRoundRect(
                        l - te.bgPadLeftDp * scale,
                        rTop,
                        r + te.bgPadRightDp * scale,
                        rBottom,
                        rR,
                        rR,
                        bgPaint
                    )
                    pi = pj
                }
                glowOps.forEach { op -> op() }
                // Last safety: glyphs never leave the box (background and glow are not clipped).
                canvas.save()
                canvas.clipRect(
                    baseX - 12f * scale,
                    te.yFraction * thisPageHeight - textPaint.textSize * 0.3f,
                    baseX + maxOf(wrapW, pdfLongestW) + textPaint.textSize,
                    te.yFraction * thisPageHeight + pdfAvailH + textPaint.textSize * 0.3f
                )
                textOps.forEach { op -> op() }
                canvas.restore()

                if (kotlin.math.abs(rotDeg) > 0.05f) canvas.restore()

                textPaint.clearShadowLayer()
                textPaint.shader = null

                te.linkRanges.forEach { lr ->
                    val url = lr.url.trim().takeIf { it.isNotEmpty() } ?: return@forEach
                    val s = lr.range.first.coerceIn(0, fullText.length)
                    val e = (lr.range.last + 1).coerceIn(0, fullText.length)
                    if (s >= e) return@forEach
                    lines.forEachIndexed { lineNo, range ->
                        val lineEnd = range.last + 1
                        val os = maxOf(s, range.first)
                        val oe = minOf(e, lineEnd)
                        if (os >= oe) return@forEachIndexed
                        val before = fullText.substring(range.first, os)
                        val linked = fullText.substring(os, oe)
                        val left = baseX + textPaint.measureText(before)
                        val width = textPaint.measureText(linked)
                        val y = firstBaseline + lineNo * lineH
                        if (y > boxBottom) return@forEachIndexed
                        val top = y - textPaint.textSize * 0.85f
                        val bottom = y + textPaint.textSize * 0.25f
                        linkRects.add(
                            LinkRect(
                                pageIndex = pageIndex,
                                left = left,
                                top = top,
                                right = left + width,
                                bottom = bottom,
                                pageHeight = thisPageHeight.toFloat(),
                                url = url
                            )
                        )
                    }
                }
            }

            pdf.finishPage(page)
        }
        if (drawError != null) {
            android.util.Log.e("PdfGenerator", "Export drawing failed", drawError)
            try { pdf.close() } catch (_: Throwable) {}
            return Result(
                false,
                message = if (drawError is OutOfMemoryError) "out_of_memory" else (drawError.message ?: "error")
            )
        }

        return try {
            val baos = ByteArrayOutputStream()
            pdf.writeTo(baos)
            pdf.close()
            var bytes = baos.toByteArray()
            var linkWarning: String? = null
            if (linkRects.isNotEmpty()) {
                val plain = bytes
                val stamped = try {
                    addLinkAnnotations(plain, linkRects, context)
                } catch (oom: OutOfMemoryError) {
                    // Not enough memory for the link pass: save the PDF without clickable links.
                    plain to "OutOfMemoryError: links were skipped"
                }
                bytes = stamped.first
                linkWarning = stamped.second
            }
            val uri = saveBytesToDocuments(context, safeName, bytes)
            if (uri != null) {
                Result(true, fileName = safeName, linkWarning = linkWarning, savedUri = uri)
            } else {
                Result(false, message = "save_failed")
            }
        } catch (e: Throwable) {
            android.util.Log.e("PdfGenerator", "Export save failed", e)
            try { pdf.close() } catch (_: Throwable) {}
            Result(
                false,
                message = if (e is OutOfMemoryError) "out_of_memory" else (e.message ?: "error")
            )
        }
    }

    /** forEachIndexed that stops at the first Throwable (incl. OutOfMemoryError) and returns it. */
    private inline fun <T> List<T>.forEachIndexedGuarded(action: (Int, T) -> Unit): Throwable? {
        try {
            for (i in indices) action(i, this[i])
        } catch (t: Throwable) {
            return t
        }
        return null
    }

    private fun saveBytesToDocuments(context: Context, fileName: String, bytes: ByteArray): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/PDFCraftStudio"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            android.util.Log.w("PdfGenerator", "Export write failed", e)
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            null
        }
    }


    /** Draw bitmap centered inside dst rect using Fit (no crop). */
    private data class LinkRect(
        val pageIndex: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val pageHeight: Float,
        val url: String
    )

    /** Draw bitmap centered inside dst rect using Fit (no crop). Returns drawn bounds. */
    
    
    private fun wrapTextLines(text: String, paint: Paint, maxWidth: Float): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        val n = text.length
        while (i <= n) {
            var e = i
            while (e < n && text[e] != '\n' && text[e] != '\r') e++
            wrapParagraph(text, i, e, paint, maxWidth, out)
            if (e >= n) break
            i = if (text[e] == '\r' && e + 1 < n && text[e + 1] == '\n') e + 2 else e + 1
        }
        if (out.isEmpty()) out.add(IntRange(0, -1))
        return out
    }

    private fun wrapParagraph(
        text: String,
        start: Int,
        end: Int,
        paint: Paint,
        maxWidth: Float,
        out: MutableList<IntRange>
    ) {
        if (start >= end) {
            out.add(IntRange(start, start - 1))
            return
        }
        if (maxWidth > 1e19f) {
            out.add(start until end)
            return
        }
        var i = start
        while (i < end) {
            if (paint.measureText(text, i, end) <= maxWidth) {
                out.add(i until end)
                return
            }
            var lo = i + 1
            var hi = end
            var fit = i + 1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (paint.measureText(text, i, mid) <= maxWidth) {
                    fit = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            var br = fit
            var s = fit - 1
            while (s > i) {
                if (text[s] == ' ') {
                    br = s + 1
                    break
                }
                s--
            }
            if (br <= i) br = (i + 1).coerceAtMost(end)
            // Never cut a character cluster: emoji + ZWJ / skin tone / variation selector,
            // letter + combining marks, surrogate pairs.
            if (br > i && br < end) {
                val bi = java.text.BreakIterator.getCharacterInstance()
                bi.setText(text)
                if (!bi.isBoundary(br)) {
                    val p = bi.preceding(br)
                    br = if (p > i) {
                        p
                    } else {
                        val f = bi.following(br)
                        if (f == java.text.BreakIterator.DONE || f > end) end else f
                    }
                }
            }
            out.add(i until br)
            i = br
        }
    }

    private fun drawNumberBadge(
        canvas: Canvas,
        label: Int,
        bounds: android.graphics.RectF,
        xFrac: Float,
        yFrac: Float,
        sizeFrac: Float,
        alpha: Float,
        bgArgb: Long = 0xE6000000L,
        fgArgb: Long = 0xFFFFFFFFL,
        weight: Float = 0.85f
    ) {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0f || h <= 0f) return
        val d = minOf(w, h) * sizeFrac.coerceIn(0.08f, 0.4f)
        val half = d / 2f
        val cx = (bounds.left + w * xFrac.coerceIn(0f, 1f)).coerceIn(bounds.left + half, bounds.right - half)
        val cy = (bounds.top + h * yFrac.coerceIn(0f, 1f)).coerceIn(bounds.top + half, bounds.bottom - half)
        val a = (alpha.coerceIn(0.15f, 1f) * 255f).toInt().coerceIn(40, 255)
        fun withAlpha(argb: Long): Int {
            val base = argb.toInt()
            val r = (base shr 16) and 0xFF
            val g = (base shr 8) and 0xFF
            val b = base and 0xFF
            val srcA = (base ushr 24) and 0xFF
            val outA = (srcA * a) / 255
            return android.graphics.Color.argb(outA, r, g, b)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = withAlpha(bgArgb)
        }
        canvas.drawCircle(cx, cy, half, fill)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = withAlpha(fgArgb)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = weight >= 0.67f
            textSize = d * 0.55f
            strokeWidth = if (weight < 0.34f) 0f else if (weight < 0.67f) 0.5f else 1.2f
            style = if (weight >= 0.85f) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
        }
        val text = label.toString()
        val fm = textPaint.fontMetrics
        // True vertical center using font metrics (works for 1, 10, 100...)
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, cx, textY, textPaint)
    }

    private fun drawBitmapFit(
        canvas: Canvas,
        bitmap: Bitmap,
        dstLeft: Float,
        dstTop: Float,
        dstWidth: Float,
        dstHeight: Float,
        paint: Paint,
        cornerPercent: Int = 0
    ): android.graphics.RectF? {
        if (bitmap.width <= 0 || bitmap.height <= 0 || dstWidth <= 0f || dstHeight <= 0f) return null
        val bmpAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val cellAspect = dstWidth / dstHeight
        val drawW: Float
        val drawH: Float
        if (bmpAspect > cellAspect) {
            drawW = dstWidth
            drawH = dstWidth / bmpAspect
        } else {
            drawH = dstHeight
            drawW = dstHeight * bmpAspect
        }
        val left = dstLeft + (dstWidth - drawW) / 2f
        val top = dstTop + (dstHeight - drawH) / 2f
        val dst = android.graphics.RectF(left, top, left + drawW, top + drawH)

        // Downscale to on-page pixel size so PdfDocument does not embed
        // full-resolution bitmaps (main cause of multi-MB PDFs from tiny JPEGs).
        val targetW = drawW.toInt().coerceAtLeast(1)
        val targetH = drawH.toInt().coerceAtLeast(1)
        val toDraw: Bitmap
        val recycleAfter: Boolean
        if (bitmap.width > targetW * 2 || bitmap.height > targetH * 2) {
            toDraw = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            recycleAfter = toDraw !== bitmap
        } else {
            toDraw = bitmap
            recycleAfter = false
        }
        val src = android.graphics.Rect(0, 0, toDraw.width, toDraw.height)
        val save = canvas.save()
        if (cornerPercent > 0) {
            val radius = minOf(drawW, drawH) * (cornerPercent.coerceIn(0, 100) / 100f) * 0.5f
            val path = Path()
            path.addRoundRect(dst, radius, radius, Path.Direction.CW)
            canvas.clipPath(path)
        }
        canvas.drawBitmap(toDraw, src, dst, paint)
        canvas.restoreToCount(save)
        if (recycleAfter && !toDraw.isRecycled) {
            toDraw.recycle()
        }
        return dst
    }

    private fun addLinkAnnotations(pdfBytes: ByteArray, links: List<LinkRect>, context: Context): Pair<ByteArray, String?> {
        if (links.isEmpty()) return pdfBytes to null
        return try {
            PDFBoxResourceLoader.init(context)
            PDDocument.load(ByteArrayInputStream(pdfBytes)).use { doc ->
                links.forEach { link ->
                    if (link.pageIndex !in 0 until doc.numberOfPages) return@forEach
                    val page = doc.getPage(link.pageIndex)
                    // Use the exact size this page was generated with instead of
                    // page.mediaBox — PDFBox-Android can misread an inherited /MediaBox
                    // on pages written by android.graphics.pdf.PdfDocument and silently
                    // fall back to Letter size (612x792), corrupting the link rect.
                    val pageW = 1080f
                    val pageH = link.pageHeight.coerceAtLeast(1f)
                    val scaleX = 1f
                    val scaleY = 1f

                    // Canvas (top-left, Y down) → PDF user space (bottom-left, Y up)
                    val left = link.left * scaleX
                    val right = link.right * scaleX
                    val top = link.top * scaleY
                    val bottom = link.bottom * scaleY

                    val llx = left.coerceIn(0f, pageW)
                    val lly = (pageH - bottom).coerceIn(0f, pageH)
                    val urx = right.coerceIn(0f, pageW)
                    val ury = (pageH - top).coerceIn(0f, pageH)
                    val w = (urx - llx).coerceAtLeast(1f)
                    val h = (ury - lly).coerceAtLeast(1f)

                    var uri = link.url.trim()
                    if (uri.isEmpty()) return@forEach
                    if (!uri.contains("://")) {
                        uri = "https://$uri"
                    }

                    val action = PDActionURI()
                    action.uri = uri

                    val annot = PDAnnotationLink()
                    annot.rectangle = PDRectangle(llx, lly, w, h)
                    annot.action = action
                    try {
                        val bs = com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary()
                        bs.setWidth(0f)
                        annot.borderStyle = bs
                    } catch (_: Exception) {
                        try {
                            val bs = com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary()
                            bs.width = 0f
                            annot.borderStyle = bs
                        } catch (_: Exception) { }
                    }

                    // CRITICAL: assign a new list back so ANNOTS is written into the page dict
                    // (plain .add on some PDFBox/Android PDFs does not persist)
                    val updated = ArrayList(page.annotations)
                    updated.add(annot)
                    page.annotations = updated
                }
                val out = ByteArrayOutputStream()
                doc.save(out)
                out.toByteArray() to null
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to add link annotations", e)
            pdfBytes to (e.javaClass.simpleName + ": " + (e.message ?: "unknown error"))
        }
    }
}
