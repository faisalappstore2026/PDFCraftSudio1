package com.pdfcraft.studio.core.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink

/**
 * A single clickable region on a page, expressed as fractions (0f..1f) of the
 * page's displayed width/height, top-left origin — matches how PdfViewerScreen
 * measures taps against the rendered bitmap.
 */
data class PdfLinkRegion(
    val pageIndex: Int,
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
    val url: String
)

/** Reads Link annotations back out of a PDF this app previously generated. */
object PdfLinkExtractor {

    // Must match PdfGenerator's fixed logical page width.
    private const val PAGE_WIDTH_POINTS = 1080f

    /** Bigger PDFs are not loaded into PDFBox for links (memory). The viewer still opens. */
    private const val MAX_LINK_SCAN_BYTES = 30L * 1024L * 1024L

    /**
     * [pageHeightsPoints] must be the authoritative per-page height (in points)
     * as reported by android.graphics.pdf.PdfRenderer — NOT page.mediaBox, which
     * PDFBox-Android can misread on pages originally written by
     * android.graphics.pdf.PdfDocument (see PdfGenerator's addLinkAnnotations).
     */
    fun extract(
        context: Context,
        uri: Uri,
        pageHeightsPoints: List<Float>,
        pageWidthsPoints: List<Float> = emptyList()
    ): List<PdfLinkRegion> {
        val regions = mutableListOf<PdfLinkRegion>()
        val size = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Throwable) {
            -1L
        }
        if (size > MAX_LINK_SCAN_BYTES) {
            android.util.Log.i("PdfLinkExtractor", "Skip links, file too big: " + size)
            return regions
        }
        try {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { doc ->
                    for (pageIndex in 0 until doc.numberOfPages) {
                        val pageH = pageHeightsPoints.getOrNull(pageIndex)?.coerceAtLeast(1f)
                            ?: continue
                        val page = doc.getPage(pageIndex)
                        val pageW = pageWidthsPoints.getOrNull(pageIndex)?.takeIf { it > 1f }
                            ?: PAGE_WIDTH_POINTS

                        page.annotations
                            .filterIsInstance<PDAnnotationLink>()
                            .forEach { annot ->
                                val action = annot.action as? PDActionURI ?: return@forEach
                                val rect = annot.rectangle ?: return@forEach
                                val url = action.uri ?: return@forEach

                                regions.add(
                                    PdfLinkRegion(
                                        pageIndex = pageIndex,
                                        leftFraction = (rect.lowerLeftX / pageW).coerceIn(0f, 1f),
                                        topFraction = (1f - rect.upperRightY / pageH).coerceIn(0f, 1f),
                                        rightFraction = (rect.upperRightX / pageW).coerceIn(0f, 1f),
                                        bottomFraction = (1f - rect.lowerLeftY / pageH).coerceIn(0f, 1f),
                                        url = url
                                    )
                                )
                            }
                    }
                }
            }
        } catch (e: Throwable) {
            // Includes OutOfMemoryError: no links, but the viewer keeps working.
            android.util.Log.e("PdfLinkExtractor", "Failed to extract link annotations", e)
            regions.clear()
        }
        return regions
    }
}
