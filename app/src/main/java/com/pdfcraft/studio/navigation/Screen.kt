package com.pdfcraft.studio.navigation

import android.util.Base64

/**
 * Central registry of navigation destinations.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object MyProjects : Screen("my_projects")
    data object PdfLibrary : Screen("pdf_library")
    data object Editor : Screen("editor?project={project}") {
        fun routeNew(): String = "editor?project="
        fun routeOpen(path: String): String {
            val encoded = android.util.Base64.encodeToString(
                path.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )
            return "editor?project=$encoded"
        }
        fun decodeProject(encoded: String): String? {
            if (encoded.isBlank()) return null
            return try {
                String(
                    android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                    Charsets.UTF_8
                )
            } catch (_: Exception) { null }
        }
    }

    /**
     * URI is Base64 (URL_SAFE) so content:// paths with '/' do not break the nav route.
     */
    data object PdfViewer : Screen("pdf_viewer/{uri}") {
        fun routeWithUri(uri: String): String {
            val encoded = Base64.encodeToString(
                uri.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP
            )
            return "pdf_viewer/$encoded"
        }

        fun decodeUriArg(encoded: String): String {
            return try {
                String(
                    Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP),
                    Charsets.UTF_8
                )
            } catch (_: Exception) {
                // Fallback for older encodes
                android.net.Uri.decode(encoded)
            }
        }
    }
}
