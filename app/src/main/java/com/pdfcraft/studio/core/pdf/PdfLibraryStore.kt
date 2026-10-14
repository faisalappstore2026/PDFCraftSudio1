package com.pdfcraft.studio.core.pdf

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object PdfLibraryStore {

    data class PdfItem(
        val uri: String,
        val name: String,
        val sizeBytes: Long,
        val folder: String,
        val lastModifiedMillis: Long,
        val location: String = folder
    )

    private const val PREFS = "pdf_library"
    private const val KEY_RECENT = "recent"
    private const val MAX_RECENT = 50

    fun scanDevicePdfs(context: Context): List<PdfItem> {
        val byKey = LinkedHashMap<String, PdfItem>()
        queryFilesUri(context, MediaStore.Files.getContentUri("external"), byKey)
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                queryFilesUri(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, byKey)
            } catch (_: Exception) {
            }
        }
        return byKey.values.sortedBy { it.name.lowercase(Locale.US) }
    }

    private fun queryFilesUri(
        context: Context,
        collection: android.net.Uri,
        into: LinkedHashMap<String, PdfItem>
    ) {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= 29) {
            projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projection.add(MediaStore.MediaColumns.DATA)
        }
        val mimeColName = MediaStore.MediaColumns.MIME_TYPE
        val nameColName = MediaStore.MediaColumns.DISPLAY_NAME
        val selection = mimeColName + "=? OR LOWER(" + nameColName + ") LIKE ?"
        val args = arrayOf("application/pdf", "%.pdf")
        val sort = MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
        try {
            context.contentResolver.query(
                collection, projection.toTypedArray(), selection, args, sort
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndex(nameColName)
                val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = c.getColumnIndex(mimeColName)
                val relCol = if (Build.VERSION.SDK_INT >= 29)
                    c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                val dataCol = if (Build.VERSION.SDK_INT < 29) {
                    @Suppress("DEPRECATION")
                    c.getColumnIndex(MediaStore.MediaColumns.DATA)
                } else -1
                while (c.moveToNext()) {
                    val name = if (nameCol >= 0) c.getString(nameCol).orEmpty() else ""
                    val mime = if (mimeCol >= 0) c.getString(mimeCol).orEmpty() else ""
                    val isPdf = mime.equals("application/pdf", true) || name.endsWith(".pdf", true)
                    if (!isPdf) continue
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val size = if (sizeCol >= 0) c.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val modified = if (dateCol >= 0) c.getLong(dateCol) * 1000L else 0L
                    val folder = when {
                        relCol >= 0 -> folderFromRelative(c.getString(relCol))
                        dataCol >= 0 -> folderFromPath(c.getString(dataCol))
                        else -> "Storage"
                    }
                    val key = name + "|" + size + "|" + folder
                    if (!into.containsKey(key) && !into.containsKey(uri)) {
                        val loc = when {
                        relCol >= 0 -> locationFromRelative(c.getString(relCol))
                        dataCol >= 0 -> locationFromPath(c.getString(dataCol))
                        else -> folder
                    }
                    into[uri] = PdfItem(uri, name.ifBlank { "PDF" }, size, folder, modified, loc)
                    }
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun folderFromRelative(rel: String?): String {
        val trimmed = rel?.trim()?.trim('/') ?: return "Storage"
        if (trimmed.isBlank()) return "Storage"
        return trimmed.substringAfterLast('/').ifBlank { trimmed }
    }

    private fun folderFromPath(path: String?): String {
        if (path.isNullOrBlank()) return "Storage"
        val parent = path.substringBeforeLast('/', "").substringAfterLast('/')
        return parent.ifBlank { "Storage" }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024L) return bytes.toString() + " B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) {
            val n = if (kb >= 100.0) kb.toInt().toString() else String.format(Locale.US, "%.1f", kb)
            return n + " KB"
        }
        val mb = kb / 1024.0
        val n = if (mb >= 100.0) mb.toInt().toString() else String.format(Locale.US, "%.1f", mb)
        return n + " MB"
    }

    fun listRecent(context: Context): List<PdfItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<PdfItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    PdfItem(
                        uri = o.optString("uri"),
                        name = o.optString("name", "PDF"),
                        sizeBytes = o.optLong("size"),
                        folder = o.optString("folder", "Storage"),
                        lastModifiedMillis = o.optLong("time")
                    )
                )
            }
            out.filter { it.uri.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecent(context: Context, item: PdfItem) {
        val existing = listRecent(context).filterNot { it.uri == item.uri }
        val next = listOf(item.copy(lastModifiedMillis = System.currentTimeMillis())) + existing
        saveRecent(context, next.take(MAX_RECENT))
    }

    private fun saveRecent(context: Context, items: List<PdfItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("uri", item.uri)
                    .put("name", item.name)
                    .put("size", item.sizeBytes)
                    .put("folder", item.folder)
                    .put("time", item.lastModifiedMillis)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, arr.toString())
            .apply()
    }

    fun removeRecent(context: Context, uri: String) {
        saveRecent(context, listRecent(context).filterNot { it.uri == uri })
    }

    fun renamePdf(context: Context, item: PdfItem, newName: String): PdfItem? {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return null
        val finalName = if (trimmed.endsWith(".pdf", true)) trimmed else trimmed + ".pdf"
        val uri = android.net.Uri.parse(item.uri)
        val values = android.content.ContentValues()
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        try {
            if (context.contentResolver.update(uri, values, null, null) > 0) {
                return item.copy(name = finalName)
            }
        } catch (_: Exception) {
        }
        return try {
            val out = android.provider.DocumentsContract.renameDocument(
                context.contentResolver, uri, finalName
            )
            if (out != null) item.copy(uri = out.toString(), name = finalName) else null
        } catch (_: Exception) {
            null
        }
    }

    fun deletePdf(context: Context, item: PdfItem): Boolean {
        return try {
            context.contentResolver.delete(android.net.Uri.parse(item.uri), null, null) > 0
        } catch (_: Exception) {
            false
        }
    }

    fun formatTime(millis: Long): String {
        if (millis <= 0L) return "-"
        return java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(java.util.Date(millis))
    }

    private fun locationFromRelative(rel: String?): String {
        val r = rel?.trim()?.trim('/') ?: ""
        return if (r.isEmpty()) "/storage/emulated/0/" else "/storage/emulated/0/" + r + "/"
    }

    private fun locationFromPath(path: String?): String {
        if (path.isNullOrBlank()) return "/storage/emulated/0/"
        val parent = path.substringBeforeLast('/')
        return if (parent.isBlank()) path else parent + "/"
    }

    fun sortPdfs(items: List<PdfItem>, mode: String): List<PdfItem> {
        return when (mode) {
            "name_za" -> items.sortedByDescending { it.name.lowercase(Locale.US) }
            "date_new" -> items.sortedByDescending { it.lastModifiedMillis }
            "date_old" -> items.sortedBy { it.lastModifiedMillis }
            "size_large" -> items.sortedByDescending { it.sizeBytes }
            "size_small" -> items.sortedBy { it.sizeBytes }
            else -> items.sortedBy { it.name.lowercase(Locale.US) }
        }
    }
}
