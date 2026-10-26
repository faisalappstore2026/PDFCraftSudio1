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
    private const val KEY_PICKED = "picked"
    private const val MAX_PICKED = 100

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
        } catch (e: SecurityException) {
            android.util.Log.w("PdfLibraryStore", "No permission to scan PDFs", e)
        } catch (e: Exception) {
            android.util.Log.w("PdfLibraryStore", "PDF scan failed", e)
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

    fun listPicked(context: Context): List<PdfItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PICKED, "[]") ?: "[]"
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
                        folder = o.optString("folder", "Picked"),
                        lastModifiedMillis = o.optLong("time"),
                        location = o.optString("location", "Picked")
                    )
                )
            }
            out.filter { it.uri.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePicked(context: Context, items: List<PdfItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("uri", item.uri)
                    .put("name", item.name)
                    .put("size", item.sizeBytes)
                    .put("folder", item.folder)
                    .put("time", item.lastModifiedMillis)
                    .put("location", item.location)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PICKED, arr.toString())
            .apply()
    }

    fun addPicked(context: Context, item: PdfItem) {
        val existing = listPicked(context).filterNot { it.uri == item.uri }
        val next = listOf(item) + existing
        savePicked(context, next.take(MAX_PICKED))
    }

    fun removePicked(context: Context, uri: String) {
        savePicked(context, listPicked(context).filterNot { it.uri == uri })
    }

    /** Merge MediaStore scan with SAF-picked PDFs (and still-granted persistable URIs). */
    fun mergeWithPicked(context: Context, scanned: List<PdfItem>): List<PdfItem> {
        val byUri = LinkedHashMap<String, PdfItem>()
        for (item in scanned) {
            byUri[item.uri] = item
        }
        for (item in listPicked(context)) {
            if (item.uri.isNotBlank()) byUri.putIfAbsent(item.uri, item)
        }
        try {
            for (p in context.contentResolver.persistedUriPermissions) {
                if (!p.isReadPermission) continue
                val key = p.uri.toString()
                if (key.isBlank() || byUri.containsKey(key)) continue
                var name = p.uri.lastPathSegment
                    ?.substringAfterLast(':')
                    ?.substringAfterLast('/')
                    ?: "PDF"
                if (!name.endsWith(".pdf", true)) name = name + ".pdf"
                byUri[key] = PdfItem(key, name, 0L, "Picked", 0L, "Picked")
            }
        } catch (_: Exception) {
        }
        return byUri.values.sortedBy { it.name.lowercase(Locale.US) }
    }

    private fun rewriteUriLists(context: Context, oldUri: String, updated: PdfItem) {
        val pickedSrc = listPicked(context)
        if (pickedSrc.any { it.uri == oldUri || it.uri == updated.uri }) {
            val picked = pickedSrc
                .map { if (it.uri == oldUri) updated else it }
                .filterNot { it.uri == oldUri && updated.uri != oldUri }
                .distinctBy { it.uri }
            savePicked(context, picked.take(MAX_PICKED))
        }
        val recentSrc = listRecent(context)
        if (recentSrc.any { it.uri == oldUri || it.uri == updated.uri }) {
            val recent = recentSrc
                .map { if (it.uri == oldUri) updated else it }
                .filterNot { it.uri == oldUri && updated.uri != oldUri }
                .distinctBy { it.uri }
            saveRecent(context, recent.take(MAX_RECENT))
        }
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
                val updated = item.copy(name = finalName)
                rewriteUriLists(context, item.uri, updated)
                return updated
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfLibraryStore", "Rename via MediaStore failed", e)
        }
        return try {
            val out = android.provider.DocumentsContract.renameDocument(
                context.contentResolver, uri, finalName
            )
            if (out != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        out, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                val updated = item.copy(uri = out.toString(), name = finalName)
                rewriteUriLists(context, item.uri, updated)
                updated
            } else null
        } catch (e: Exception) {
            android.util.Log.w("PdfLibraryStore", "Rename failed", e)
            null
        }
    }

    fun deletePdf(context: Context, item: PdfItem): Boolean {
        return try {
            context.contentResolver.delete(android.net.Uri.parse(item.uri), null, null) > 0
        } catch (e: Exception) {
            android.util.Log.w("PdfLibraryStore", "Delete failed", e)
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
