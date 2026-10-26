package com.pdfcraft.studio.core.session

import android.content.Context
import android.net.Uri
import java.io.File

object SessionStore {
    private const val PREFS = "pdfcraft_session"
    const val HOME = "home"
    const val EDITOR = "editor"
    const val VIEWER = "viewer"
    const val LIBRARY = "library"
    const val PROJECTS = "projects"

    data class Snapshot(
        val screen: String = HOME,
        val editorPath: String = "",
        val viewerUri: String = "",
        val editorIndex: Int = 0,
        val editorOffset: Int = 0,
        val viewerIndex: Int = 0,
        val viewerOffset: Int = 0,
        val openTool: String = "",
        val openToolMenu: String = "",
        val toolbarDialog: String = "",
        val editorMode: String = "",
        val focusedTextId: String = "",
        val overflowOpen: Boolean = false,
        val nameDialog: Boolean = false,
        val successDialog: Boolean = false,
        val nameField: String = "Project.pdf",
        val undoCount: Int = 0,
        val redoCount: Int = 0,
        val selSize: String = "",
        val selBg: String = "",
        val selDel: String = "",
        val selDup: String = "",
        val selNum: String = "",
        val dragPages: String = "",
        val deletePages: String = "",
        val dragImgs: String = "",
        val deleteImgs: String = "",
        val showDragImagePicker: Boolean = false,
        val showDeleteImagesImagePicker: Boolean = false,
        val showDeleteImagesModeDialog: Boolean = false,
        val orientationOpen: Boolean = false
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): Snapshot {
        val p = prefs(context)
        return Snapshot(
            screen = p.getString("screen", HOME) ?: HOME,
            editorPath = p.getString("editorPath", "") ?: "",
            viewerUri = p.getString("viewerUri", "") ?: "",
            editorIndex = p.getInt("editorIndex", 0),
            editorOffset = p.getInt("editorOffset", 0),
            viewerIndex = p.getInt("viewerIndex", 0),
            viewerOffset = p.getInt("viewerOffset", 0),
            openTool = p.getString("openTool", "") ?: "",
            openToolMenu = p.getString("openToolMenu", "") ?: "",
            toolbarDialog = p.getString("toolbarDialog", "") ?: "",
            editorMode = p.getString("editorMode", "") ?: "",
            focusedTextId = p.getString("focusedTextId", "") ?: "",
            overflowOpen = p.getBoolean("overflowOpen", false),
            nameDialog = p.getBoolean("nameDialog", false),
            successDialog = p.getBoolean("successDialog", false),
            nameField = p.getString("nameField", "Project.pdf") ?: "Project.pdf",
            undoCount = p.getInt("undoCount", 0),
            redoCount = p.getInt("redoCount", 0),
            selSize = p.getString("selSize", "") ?: "",
            selBg = p.getString("selBg", "") ?: "",
            selDel = p.getString("selDel", "") ?: "",
            selDup = p.getString("selDup", "") ?: "",
            selNum = p.getString("selNum", "") ?: "",
            dragPages = p.getString("dragPages", "") ?: "",
            deletePages = p.getString("deletePages", "") ?: "",
            dragImgs = p.getString("dragImgs", "") ?: "",
            deleteImgs = p.getString("deleteImgs", "") ?: "",
            showDragImagePicker = p.getBoolean("showDragImagePicker", false),
            showDeleteImagesImagePicker = p.getBoolean("showDeleteImagesImagePicker", false),
            showDeleteImagesModeDialog = p.getBoolean("showDeleteImagesModeDialog", false),
            orientationOpen = p.getBoolean("orientationOpen", false)
        )
    }

    private fun edit(context: Context) = prefs(context).edit()

    fun setHome(context: Context) { edit(context).putString("screen", HOME).apply() }
    fun setScreen(context: Context, screen: String) { edit(context).putString("screen", screen).apply() }
    fun setEditor(context: Context, path: String?) {
        edit(context).putString("screen", EDITOR).putString("editorPath", path.orEmpty()).apply()
    }
    fun setEditorScroll(context: Context, index: Int, offset: Int) {
        edit(context).putInt("editorIndex", index.coerceAtLeast(0)).putInt("editorOffset", offset.coerceAtLeast(0)).apply()
    }
    fun setViewer(context: Context, uri: String?) {
        edit(context).putString("screen", VIEWER).putString("viewerUri", uri.orEmpty()).apply()
    }
    fun setViewerScroll(context: Context, index: Int, offset: Int) {
        edit(context).putInt("viewerIndex", index.coerceAtLeast(0)).putInt("viewerOffset", offset.coerceAtLeast(0)).apply()
    }
    fun setOpenTool(context: Context, tool: String) { edit(context).putString("openTool", tool).apply() }
    fun setOpenToolMenu(context: Context, menu: String?) { edit(context).putString("openToolMenu", menu.orEmpty()).apply() }
    fun setToolbarDialog(context: Context, dialog: String) { edit(context).putString("toolbarDialog", dialog).apply() }
    fun setEditorMode(context: Context, mode: String) { edit(context).putString("editorMode", mode).apply() }
    fun setFocusedTextId(context: Context, id: String) { edit(context).putString("focusedTextId", id).apply() }
    fun setOverflow(context: Context, open: Boolean) { edit(context).putBoolean("overflowOpen", open).apply() }
    fun setNameDialog(context: Context, open: Boolean, name: String, success: Boolean) {
        edit(context).putBoolean("nameDialog", open).putString("nameField", name).putBoolean("successDialog", success).apply()
    }
    fun setHistory(context: Context, undo: Int, redo: Int) {
        edit(context).putInt("undoCount", undo).putInt("redoCount", redo).apply()
    }
    fun setPageSelections(context: Context, size: String, bg: String, del: String, dup: String, num: String) {
        edit(context).putString("selSize", size).putString("selBg", bg).putString("selDel", del)
            .putString("selDup", dup).putString("selNum", num).apply()
    }
    fun setPickerSets(context: Context, dragPages: String, deletePages: String, dragImgs: String, deleteImgs: String) {
        edit(context).putString("dragPages", dragPages).putString("deletePages", deletePages)
            .putString("dragImgs", dragImgs).putString("deleteImgs", deleteImgs).apply()
    }
    fun setExtraPickers(context: Context, dragImgs: Boolean, delImgs: Boolean, delMode: Boolean) {
        edit(context).putBoolean("showDragImagePicker", dragImgs)
            .putBoolean("showDeleteImagesImagePicker", delImgs)
            .putBoolean("showDeleteImagesModeDialog", delMode).apply()
    }
    fun setOrientationOpen(context: Context, open: Boolean) { edit(context).putBoolean("orientationOpen", open).apply() }

    fun parseInts(raw: String): List<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }

    fun csv(values: Collection<Int>): String = values.sorted().joinToString(",")
    fun csvIds(values: Collection<String>): String = values.joinToString("\u001f")
    fun parseIds(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split('\u001f').filter { it.isNotBlank() }

    fun clearEditorUi(context: Context) {
        edit(context)
            .putString("openTool", "").putString("openToolMenu", "")
            .putString("toolbarDialog", "").putString("editorMode", "")
            .putString("focusedTextId", "")
            .putInt("editorIndex", 0).putInt("editorOffset", 0)
            .putBoolean("overflowOpen", false).putBoolean("nameDialog", false)
            .putBoolean("successDialog", false).putString("nameField", "Project.pdf")
            .putInt("undoCount", 0).putInt("redoCount", 0)
            .putString("selSize", "").putString("selBg", "").putString("selDel", "")
            .putString("selDup", "").putString("selNum", "")
            .putString("dragPages", "").putString("deletePages", "")
            .putString("dragImgs", "").putString("deleteImgs", "")
            .putBoolean("showDragImagePicker", false)
            .putBoolean("showDeleteImagesImagePicker", false)
            .putBoolean("showDeleteImagesModeDialog", false)
            .putBoolean("orientationOpen", false)
            .apply()
    }

    fun uriReadable(context: Context, uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        return try {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(uriStr), "r")?.use { true } ?: false
        } catch (_: Exception) { false }
    }

    fun editorPathOpenable(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        if (path.startsWith("pdf|")) return uriReadable(context, path.substring(4))
        return try { File(path).isFile } catch (_: Exception) { false }
    }
}
