package com.pdfcraft.studio.core.text

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream

/**
 * Built-in + user-imported fonts. Custom fonts are copied into app-private
 * storage so they keep working after the original picker URI expires.
 */
data class AppFont(
    val id: String,
    val displayName: String,
    val isCustom: Boolean = false,
    /** Absolute path inside app filesDir for custom fonts; null for system fonts. */
    val filePath: String? = null
)

object FontCatalog {

    const val ID_DEFAULT = "default"
    const val ID_SERIF = "serif"
    const val ID_MONOSPACE = "monospace"
    const val ID_SANS_SERIF = "sans-serif"
    const val ID_CURSIVE = "cursive"

    val systemFonts: List<AppFont> = listOf(
        AppFont(ID_DEFAULT, "Default"),
        AppFont(ID_SANS_SERIF, "Sans Serif"),
        AppFont(ID_SERIF, "Serif"),
        AppFont(ID_MONOSPACE, "Monospace"),
        AppFont(ID_CURSIVE, "Cursive")
    )

    fun fontsDir(context: Context): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadCustomFonts(context: Context): List<AppFont> {
        val dir = fontsDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true)) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { file ->
                AppFont(
                    id = "custom_${file.name}",
                    displayName = file.nameWithoutExtension,
                    isCustom = true,
                    filePath = file.absolutePath
                )
            }
            ?: emptyList()
    }

    fun allFonts(context: Context): List<AppFont> =
        systemFonts + loadCustomFonts(context)

    /**
     * Copies a user-picked font into app storage. Returns the new [AppFont]
     * or null if the file could not be read / was not a supported font.
     */
    fun importFont(context: Context, uri: Uri): AppFont? {
        return try {
            val resolver = context.contentResolver
            val nameHint = resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
            val rawName = nameHint?.takeIf { it.isNotBlank() }
                ?: "font_${System.currentTimeMillis()}.ttf"
            val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val lower = safeName.lowercase()
            val finalName = when {
                lower.endsWith(".ttf") || lower.endsWith(".otf") -> safeName
                else -> "$safeName.ttf"
            }

            val dest = File(fontsDir(context), finalName)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // Validate that Android can actually load the typeface
            val typeface = Typeface.createFromFile(dest)
            if (typeface == null) {
                dest.delete()
                return null
            }

            AppFont(
                id = "custom_${dest.name}",
                displayName = dest.nameWithoutExtension,
                isCustom = true,
                filePath = dest.absolutePath
            )
        } catch (_: Exception) {
            null
        }
    }

    fun deleteCustomFont(context: Context, font: AppFont): Boolean {
        if (!font.isCustom || font.filePath == null) return false
        return try {
            File(font.filePath).delete()
        } catch (_: Exception) {
            false
        }
    }

    fun resolveComposeFontFamily(font: AppFont): FontFamily {
        if (font.filePath != null) {
            return try {
                val typeface = Typeface.createFromFile(font.filePath)
                FontFamily(typeface)
            } catch (_: Exception) {
                FontFamily.Default
            }
        }
        return when (font.id) {
            ID_SERIF -> FontFamily.Serif
            ID_MONOSPACE -> FontFamily.Monospace
            ID_CURSIVE -> FontFamily.Cursive
            ID_SANS_SERIF -> FontFamily.SansSerif
            else -> FontFamily.Default
        }
    }

    fun resolveComposeFontFamily(fontId: String, customFonts: List<AppFont>): FontFamily {
        val font = systemFonts.firstOrNull { it.id == fontId }
            ?: customFonts.firstOrNull { it.id == fontId }
            ?: return FontFamily.Default
        return resolveComposeFontFamily(font)
    }

    fun resolveTypeface(context: Context, fontId: String): Typeface {
        val font = allFonts(context).firstOrNull { it.id == fontId }
        val path = font?.filePath
        if (path != null) {
            return try {
                Typeface.createFromFile(path)
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        }
        return when (fontId) {
            ID_SERIF -> Typeface.SERIF
            ID_MONOSPACE -> Typeface.MONOSPACE
            ID_SANS_SERIF -> Typeface.SANS_SERIF
            else -> Typeface.DEFAULT
        }
    }
}
