package com.pdfcraft.studio

import android.app.Application

/**
 * Application entry point.
 *
 * Kept intentionally minimal in this stage. As future stages add real
 * dependencies (PDF engine, image cache, persistence, etc.) they should be
 * wired up here behind simple factory/provider functions rather than
 * scattering singletons across the codebase.
 */
class PdfCraftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        } catch (e: Throwable) {
            android.util.Log.w("PdfCraftApp", "PDFBox init failed", e)
        }
    }
}
