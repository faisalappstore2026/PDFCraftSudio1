package com.pdfcraft.studio.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** fix54: one document found on the device. */
internal data class DocFile(val path: String, val name: String, val size: Long, val modified: Long, val ext: String) {
    val file: File get() = File(path)
}

/** fix54: tabs of the Documents screen (PDF is the default). */
internal enum class DocKind(val label: String, val title: String, val exts: Set<String>) {
    ALL("All", "All Files", emptySet()),
    PDF("PDF", "PDF Files", setOf("pdf")),
    WORD("Word", "Word Files", setOf("doc", "docx", "rtf", "odt")),
    EXCEL("Excel", "Excel Files", setOf("xls", "xlsx", "csv", "ods")),
    PPT("PPT", "PPT Files", setOf("ppt", "pptx", "odp")),
    TEXT("Text", "Text Files", setOf("txt"));

    fun matches(ext: String): Boolean = if (this == ALL) ALL_EXTS.contains(ext) else exts.contains(ext)

    companion object {
        val ALL_EXTS: Set<String> by lazy { values().flatMap { it.exts }.toSet() }
        fun of(ext: String): DocKind = values().firstOrNull { it != ALL && it.exts.contains(ext) } ?: ALL
    }
}

internal object DocFiles {
    private const val PREFS = "documents_screen"

    fun hasAccess(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Recursive walk of the shared storage (hidden folders and Android/data skipped). */
    fun scan(): List<DocFile> {
        val root = Environment.getExternalStorageDirectory()
        val out = ArrayList<DocFile>()
        fun walk(dir: File, depth: Int) {
            if (depth > 14 || out.size >= 20000) return
            val list = try { dir.listFiles() } catch (_: Exception) { null } ?: return
            for (f in list) {
                val n = f.name
                if (n.startsWith(".")) continue
                if (f.isDirectory) {
                    if (depth == 1 && n == "Android") continue
                    walk(f, depth + 1)
                } else {
                    val ext = n.substringAfterLast('.', "").lowercase(Locale.US)
                    if (DocKind.ALL_EXTS.contains(ext)) out.add(DocFile(f.absolutePath, n, f.length(), f.lastModified(), ext))
                }
            }
        }
        walk(root, 1)
        return out
    }

    fun favorites(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("fav", emptySet())?.toSet() ?: emptySet()

    fun setFavorites(ctx: Context, s: Set<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("fav", HashSet(s)).apply()
    }

    fun sort(ctx: Context): Int = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("sort", 3)
    fun setSort(ctx: Context, v: Int) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("sort", v).apply() }

    /** 0 name A-Z, 1 name Z-A, 2 date old first, 3 date new first, 4 size small first, 5 size big first */
    fun sorted(list: List<DocFile>, s: Int): List<DocFile> = when (s) {
        0 -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
        1 -> list.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        2 -> list.sortedBy { it.modified }
        4 -> list.sortedBy { it.size }
        5 -> list.sortedByDescending { it.size }
        else -> list.sortedByDescending { it.modified }
    }

    fun sizeText(b: Long): String = when {
        b >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0))
        b >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", b / (1024.0 * 1024.0))
        b >= 1024L -> String.format(Locale.US, "%.0f KB", b / 1024.0)
        else -> b.toString() + " B"
    }

    fun dateText(t: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(t))

    fun mime(ext: String): String = when (ext) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "rtf" -> "application/rtf"
        "odt" -> "application/vnd.oasis.opendocument.text"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "odp" -> "application/vnd.oasis.opendocument.presentation"
        "txt" -> "text/plain"
        else -> "*/*"
    }

    private fun uri(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)

    private fun toast(ctx: Context, s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun open(ctx: Context, d: DocFile) {
        try {
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri(ctx, d.file), mime(d.ext))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(i, "Open with").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: Exception) {
            toast(ctx, "No app found to open this file")
        }
    }

    fun share(ctx: Context, docs: List<DocFile>) {
        if (docs.isEmpty()) return
        try {
            val i = if (docs.size == 1) {
                Intent(Intent.ACTION_SEND).setType(mime(docs[0].ext)).putExtra(Intent.EXTRA_STREAM, uri(ctx, docs[0].file))
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(docs.map { uri(ctx, it.file) }))
            }
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: Exception) {
            toast(ctx, "Could not share this file")
        }
    }

    private fun scanPaths(ctx: Context, vararg p: String) {
        try { MediaScannerConnection.scanFile(ctx.applicationContext, p, null, null) } catch (_: Exception) { }
    }

    fun delete(ctx: Context, docs: List<DocFile>): Int {
        var n = 0
        for (d in docs) if (d.file.delete()) { n++; scanPaths(ctx, d.path) }
        return n
    }

    /** New name keeps the old extension when the user leaves it out. */
    fun rename(ctx: Context, d: DocFile, newName: String): File? {
        var n = newName.trim().replace("/", "_")
        if (n.isEmpty()) return null
        if (!n.lowercase(Locale.US).endsWith("." + d.ext)) n = n + "." + d.ext
        val t = File(d.file.parentFile, n)
        if (t.exists()) { toast(ctx, "A file with this name already exists"); return null }
        return if (d.file.renameTo(t)) { scanPaths(ctx, d.path, t.absolutePath); t } else { toast(ctx, "Rename failed"); null }
    }

    fun moveTargets(): List<Pair<String, File>> {
        val r = Environment.getExternalStorageDirectory()
        return listOf(
            "Download" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Download / PDFCraft Studio" to File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PDFCraft Studio"),
            "Internal storage" to r
        )
    }

    /** Move = rename when possible, otherwise copy + delete. */
    fun move(ctx: Context, d: DocFile, dir: File): Boolean {
        if (d.file.parentFile?.absolutePath == dir.absolutePath) { toast(ctx, "Already in this folder"); return false }
        dir.mkdirs()
        var t = File(dir, d.name)
        var k = 1
        while (t.exists()) { t = File(dir, d.name.substringBeforeLast('.') + " (" + k + ")." + d.ext); k++ }
        val ok = d.file.renameTo(t) || try {
            FileInputStream(d.file).use { i -> FileOutputStream(t).use { o -> i.copyTo(o) } }
            if (t.length() == d.file.length()) d.file.delete() else { t.delete(); false }
        } catch (_: Exception) { t.delete(); false }
        if (ok) scanPaths(ctx, d.path, t.absolutePath) else toast(ctx, "Move failed")
        return ok
    }

    fun print(ctx: Context, d: DocFile) {
        if (d.ext != "pdf") { toast(ctx, "Only PDF files can be printed"); return }
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?,
                callback: PrintDocumentAdapter.LayoutResultCallback, extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(d.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true
                )
            }

            override fun onWrite(
                pages: Array<out PageRange>?, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?,
                callback: PrintDocumentAdapter.WriteResultCallback
            ) {
                try {
                    FileInputStream(d.file).use { i -> FileOutputStream(destination.fileDescriptor).use { o -> i.copyTo(o) } }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        try { pm.print(d.name, adapter, null) } catch (_: Exception) { toast(ctx, "Printing is not available") }
    }

    fun findActivity(ctx: Context): Activity? {
        var c: Context? = ctx
        while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
        return null
    }

    private var poll: Runnable? = null

    /** Opens "All files access" for this app and comes back by itself once it is allowed. */
    fun openAccessSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + activity.packageName))
            )
        } catch (_: Exception) {
            try { activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } catch (_: Exception) { return }
        }
        val h = Handler(Looper.getMainLooper())
        poll?.let { h.removeCallbacks(it) }
        val app = activity.applicationContext
        val cls = activity.javaClass
        val start = System.currentTimeMillis()
        val r = object : Runnable {
            override fun run() {
                if (Environment.isExternalStorageManager()) {
                    poll = null
                    try {
                        app.startActivity(
                            Intent(app, cls).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                        )
                    } catch (_: Exception) { }
                } else if (System.currentTimeMillis() - start < 120_000L) {
                    h.postDelayed(this, 500L)
                } else poll = null
            }
        }
        poll = r
        h.postDelayed(r, 500L)
    }
}
