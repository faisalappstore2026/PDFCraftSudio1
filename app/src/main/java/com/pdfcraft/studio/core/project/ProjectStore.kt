package com.pdfcraft.studio.core.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.pdfcraft.studio.core.image.ImageSizeOption
import com.pdfcraft.studio.core.text.FontCatalog
import com.pdfcraft.studio.ui.editor.ColorRange
import com.pdfcraft.studio.ui.editor.FontRange
import com.pdfcraft.studio.ui.editor.EditorViewModel
import com.pdfcraft.studio.ui.editor.ImportedImage
import com.pdfcraft.studio.ui.editor.LinkRange
import com.pdfcraft.studio.ui.editor.ShadowRange
import com.pdfcraft.studio.ui.editor.TextElement
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ProjectStore {

    const val PROJECT_VERSION = 1
    const val EXTENSION = ".pdfcraft"

    data class SaveResult(
        val success: Boolean,
        val displayName: String = "",
        val path: String = "",
        val error: String? = null
    )

    data class ProjectListItem(
        val file: File,
        val name: String,
        val pageCount: Int,
        val lastModifiedMillis: Long,
        val readable: Boolean
    )

    data class LoadedProject(
        val name: String,
        val minPageCount: Int,
        val pageAspectRatio: Float,
        val isPageLandscape: Boolean,
        val pageMarginDp: Int,
        val pageBackgroundColor: Long,
        val pageNumberPosition: String,
        val pageNumberStyle: String,
        val pageAspectOverrides: Map<Int, Float>,
        val pageBackgroundColorOverrides: Map<Int, Long>,
        val pageBackgroundBitmap: Bitmap?,
        val pageBackgroundBitmapOverrides: Map<Int, Bitmap>,
        val pageBackgroundKind: String = "SOLID",
        val pageBackgroundColor2: Long = 0xFF90CAF9L,
        val pageBackgroundGradientDir: String = "TTB",
        val pageBackgroundKindOverrides: Map<Int, String> = emptyMap(),
        val pageBackgroundColor2Overrides: Map<Int, Long> = emptyMap(),
        val pageBackgroundGradientDirOverrides: Map<Int, String> = emptyMap(),
        val pdfPageRasters: Map<Int, Bitmap> = emptyMap(),
        val pageBackgroundPhoto: Bitmap? = null,
        val pageBgBlur: Float = 0f,
        val pageBgImgOpacity: Float = 100f,
        val pageBgBrightness: Float = 0f,
        val pageBgOverlayOn: Boolean = false,
        val pageBgOverlayColor: Long = 0xFFFFFFFFL,
        val pageBgOverlayOpacity: Float = 25f,
        val pageBgBlurType: String = "gaussian",
        val imagesPerRow: Int,
        val imageSpacingDp: Int,
        val imageCellAspectRatio: Float,
        val layoutCellAspectRatio: Float,
        val imageCornerRadiusPercent: Int,
        val selectedImageSizeOption: ImageSizeOption,
        val numberingAlpha: Float,
        val numberingSizeFrac: Float,
        val numberingXFrac: Float,
        val numberingYFrac: Float,
        val numberingBgArgb: Long,
        val numberingFgArgb: Long,
        val numberingWeight: Float,
        val images: List<ImportedImage>,
        val texts: List<TextElement>
    )

    data class LoadResult(
        val success: Boolean,
        val data: LoadedProject? = null,
        val warning: String? = null,
        val error: String? = null
    )

    fun save(
        context: Context,
        vm: EditorViewModel,
        displayName: String = "Untitled Project",
        targetDir: File? = null,
        overwriteFile: File? = null
    ): SaveResult {
        return try {
            val dir = targetDir ?: projectsDir(context)
            if (!dir.exists() && !dir.mkdirs()) {
                return SaveResult(false, error = "Could not create projects folder")
            }
            val existing = overwriteFile ?: vm.currentProjectFile
            val outFile = if (
                existing != null &&
                existing.parentFile?.canonicalPath == dir.canonicalPath &&
                !isDraftFile(existing)
            ) {
                existing
            } else if (
                overwriteFile != null &&
                overwriteFile.parentFile?.canonicalPath == dir.canonicalPath
            ) {
                overwriteFile
            } else {
                uniqueFile(dir, sanitizeFileName(displayName.ifBlank { "Untitled Project" }))
            }
            val json = JSONObject()
            json.put("projectVersion", PROJECT_VERSION)
            json.put("name", outFile.nameWithoutExtension)
            json.put("savedAt", System.currentTimeMillis())

            val page = JSONObject()
            page.put("minPageCount", vm.minPageCount)
            page.put("documentPageCount", vm.documentPageCount())
            page.put("pageAspectRatio", vm.pageAspectRatio.toDouble())
            page.put("isPageLandscape", vm.isPageLandscape)
            page.put("pageMarginDp", vm.pageMarginDp)
            page.put("pageBackgroundColor", vm.pageBackgroundColor)
            page.put("pageNumberPosition", vm.pageNumberPosition.name)
            page.put("pageNumberStyle", vm.pageNumberStyle.name)
            page.put("pageBackgroundImageUri", vm.pageBackgroundImageUri?.toString())
            page.put("pageAspectOverrides", mapToJson(vm.pageAspectOverrides) { it.toDouble() })
            page.put("pageBackgroundColorOverrides", mapToJson(vm.pageBackgroundColorOverrides) { it })
            page.put("pageBackgroundKind", vm.pageBackgroundKind)
            page.put("pageBackgroundColor2", vm.pageBackgroundColor2)
            page.put("pageBackgroundGradientDir", vm.pageBackgroundGradientDir)
            page.put("pageBackgroundKindOverrides", mapToJson(vm.pageBackgroundKindOverrides) { it })
            page.put("pageBackgroundColor2Overrides", mapToJson(vm.pageBackgroundColor2Overrides) { it })
            page.put("pageBackgroundGradientDirOverrides", mapToJson(vm.pageBackgroundGradientDirOverrides) { it })
            page.put("bgBlur", vm.pageBgBlur.toDouble())
            page.put("bgImgOpacity", vm.pageBgImgOpacity.toDouble())
            page.put("bgBrightness", vm.pageBgBrightness.toDouble())
            page.put("bgOverlayOn", vm.pageBgOverlayOn)
            page.put("bgOverlayColor", vm.pageBgOverlayColor)
            page.put("bgOverlayOpacity", vm.pageBgOverlayOpacity.toDouble())
            page.put("bgBlurType", vm.pageBgBlurType)
            json.put("page", page)

            val layout = JSONObject()
            layout.put("imagesPerRow", vm.imagesPerRow)
            layout.put("imageSpacingDp", vm.imageSpacingDp)
            layout.put("imageCellAspectRatio", vm.imageCellAspectRatio.toDouble())
            layout.put("layoutCellAspectRatio", vm.layoutCellAspectRatio.toDouble())
            layout.put("imageCornerRadiusPercent", vm.imageCornerRadiusPercent)
            layout.put("selectedImageSizeOption", imageSizeToJson(vm.selectedImageSizeOption))
            json.put("layout", layout)

            val numbering = JSONObject()
            numbering.put("alpha", vm.numberingAlpha.toDouble())
            numbering.put("sizeFrac", vm.numberingSizeFrac.toDouble())
            numbering.put("xFrac", vm.numberingXFrac.toDouble())
            numbering.put("yFrac", vm.numberingYFrac.toDouble())
            numbering.put("bgArgb", vm.numberingBgArgb)
            numbering.put("fgArgb", vm.numberingFgArgb)
            numbering.put("weight", vm.numberingWeight.toDouble())
            json.put("numberingDefaults", numbering)

            val tmp = File(outFile.parentFile, outFile.name + ".tmp")
            ZipOutputStream(FileOutputStream(tmp).buffered()).use { zip ->
                val imagesArr = JSONArray()
                vm.importedImages.toList().forEach { img ->
                    val obj = JSONObject()
                    obj.put("id", img.id)
                    obj.put("imageUri", img.imageUri?.toString())
                    obj.put("approxSizeBytes", img.approxSizeBytes ?: JSONObject.NULL)
                    obj.put("linkUrl", img.linkUrl)
                    obj.put("numberLabel", img.numberLabel ?: JSONObject.NULL)
                    obj.put("numberXFrac", img.numberXFrac.toDouble())
                    obj.put("numberYFrac", img.numberYFrac.toDouble())
                    obj.put("numberSizeFrac", img.numberSizeFrac.toDouble())
                    obj.put("numberAlpha", img.numberAlpha.toDouble())
                    obj.put("numberBgArgb", img.numberBgArgb)
                    obj.put("numberFgArgb", img.numberFgArgb)
                    obj.put("numberWeight", img.numberWeight.toDouble())
                    obj.put("dragOffsetXFrac", img.dragOffsetXFrac.toDouble())
                    obj.put("dragOffsetYFrac", img.dragOffsetYFrac.toDouble())
                    obj.put("cornerRadiusPercent", img.cornerRadiusPercent)
                    if (img.ownerPageIndex != null) obj.put("ownerPageIndex", img.ownerPageIndex)
                    obj.put("absXFrac", img.absXFrac.toDouble())
                    obj.put("absYFrac", img.absYFrac.toDouble())
                    obj.put("absWFrac", img.absWFrac.toDouble())
                    obj.put("absHFrac", img.absHFrac.toDouble())
                    if (img.aspectRatioOverride != null) {
                        obj.put("aspectRatioOverride", img.aspectRatioOverride.toDouble())
                    } else {
                        obj.put("aspectRatioOverride", JSONObject.NULL)
                    }
                    obj.put("file", putBitmap(zip, "images/${img.id}.jpg", img.bitmap))
                    imagesArr.put(obj)
                }
                json.put("images", imagesArr)

                json.getJSONObject("page").put(
                    "backgroundFile",
                    putBitmap(zip, "backgrounds/default.jpg", vm.pageBackgroundBitmap)
                )
                val bgFiles = JSONObject()
                vm.pageBackgroundBitmapOverrides.toMap().forEach { (pageIndex, bmp) ->
                    val f = putBitmap(zip, "backgrounds/page_$pageIndex.jpg", bmp)
                    if (f != null) bgFiles.put(pageIndex.toString(), f)
                }
                json.getJSONObject("page").put("backgroundFiles", bgFiles)
                val rasterFiles = JSONObject()
                vm.originalPdfRasters().forEach { (pageIndex, bmp) ->
                    val f = putBitmap(zip, "backgrounds/raster_$pageIndex.jpg", bmp)
                    if (f != null) rasterFiles.put(pageIndex.toString(), f)
                }
                json.getJSONObject("page").put(
                    "photoFile",
                    putBitmap(zip, "backgrounds/photo.jpg", vm.pageBackgroundPhoto)
                )
                json.getJSONObject("page").put("rasterFiles", rasterFiles)

                val usedFontIds = vm.textElements.map { it.fontId }.toSet()
                val fontsArr = JSONArray()
                vm.availableFonts.filter { it.isCustom && it.id in usedFontIds }.forEach { font ->
                    val src = font.filePath?.let { File(it) }
                    val zipPath = if (src != null && src.isFile) {
                        val dest = "fonts/${src.name}"
                        zip.putNextEntry(ZipEntry(dest))
                        src.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        dest
                    } else null
                    fontsArr.put(
                        JSONObject()
                            .put("id", font.id)
                            .put("displayName", font.displayName)
                            .put("file", zipPath)
                    )
                }
                json.put("customFonts", fontsArr)

                val textsArr = JSONArray()
                vm.textElements.toList().forEach { t -> textsArr.put(textToJson(t)) }
                json.put("texts", textsArr)

                zip.putNextEntry(ZipEntry("project.json"))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            if (outFile.exists() && !outFile.delete()) {
                tmp.delete()
                return SaveResult(false, error = "Could not overwrite project")
            }
            if (!tmp.renameTo(outFile)) {
                tmp.delete()
                return SaveResult(false, error = "Could not finish saving project")
            }
            SaveResult(success = true, displayName = outFile.name, path = outFile.absolutePath)
        } catch (e: Exception) {
            SaveResult(success = false, error = e.message ?: "Save failed")
        }
    }

    fun projectsDir(context: Context): File {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val base = ext ?: context.filesDir
        return File(base, "PDFCraftStudio/Projects")
    }

    fun draftsDir(context: Context): File {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val base = ext ?: context.filesDir
        return File(base, "PDFCraftStudio/Drafts")
    }

    fun latestDraftFile(context: Context): File? {
        val dir = draftsDir(context)
        if (!dir.exists()) return null
        return dir.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION, ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
    }

    fun isDraftFile(file: File?): Boolean {
        if (file == null) return false
        return try {
            val parent = file.parentFile?.canonicalPath ?: return false
            parent.endsWith("/PDFCraftStudio/Drafts") || parent.endsWith("\\PDFCraftStudio\\Drafts")
        } catch (_: Exception) {
            false
        }
    }

    fun nextDraftNumber(dir: File): Int {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION, ignoreCase = true) }
            ?: return 1
        var max = 0
        val re = Regex("^Draft Project (\\d+)$")
        files.forEach { f ->
            val m = re.matchEntire(f.nameWithoutExtension) ?: return@forEach
            val n = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (n > max) max = n
        }
        return max + 1
    }

    fun listDrafts(context: Context): List<ProjectListItem> {
        val dir = draftsDir(context)
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION, ignoreCase = true) }
            ?: return emptyList()
        return files.map { file ->
            try {
                ZipFile(file).use { zip ->
                    val entry = zip.getEntry("project.json")
                        ?: return@use ProjectListItem(file, file.nameWithoutExtension, 0, file.lastModified(), false)
                    val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                    val page = json.optJSONObject("page")
                    val pages = page?.optInt("documentPageCount", page.optInt("minPageCount", 1)) ?: 1
                    val savedAt = json.optLong("savedAt", file.lastModified())
                    ProjectListItem(
                        file = file,
                        name = file.nameWithoutExtension,
                        pageCount = pages.coerceAtLeast(1),
                        lastModifiedMillis = maxOf(savedAt, file.lastModified()),
                        readable = true
                    )
                }
            } catch (_: Exception) {
                ProjectListItem(file, file.nameWithoutExtension, 0, file.lastModified(), false)
            }
        }.sortedByDescending { it.lastModifiedMillis }
    }

    fun latestDraft(context: Context): ProjectListItem? {
        return listDrafts(context).firstOrNull { it.readable }
    }

    /** Copy a draft into My Projects, then delete it so the recover popup does not return. */
    fun promoteDraftToProjects(context: Context, draftPath: String): Boolean {
        val draftFile = File(draftPath)
        if (!draftFile.exists()) return false
        val dir = projectsDir(context)
        if (!dir.exists() && !dir.mkdirs()) return false
        val base = sanitizeFileName(draftFile.nameWithoutExtension.ifBlank { "Draft Project" })
        val dest = uniqueFile(dir, base)
        return try {
            draftFile.copyTo(dest, overwrite = false)
            draftFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun saveDraft(context: Context, vm: EditorViewModel, existingDraft: File?): SaveResult {
        val dir = draftsDir(context)
        if (!dir.exists() && !dir.mkdirs()) {
            return SaveResult(false, error = "Could not create drafts folder")
        }
        val outFile = if (existingDraft != null && isDraftFile(existingDraft)) {
            existingDraft
        } else {
            File(dir, "Draft Project ${nextDraftNumber(dir)}$EXTENSION")
        }
        return save(
            context = context,
            vm = vm,
            displayName = outFile.nameWithoutExtension,
            targetDir = dir,
            overwriteFile = outFile
        )
    }

    private fun uniqueFile(dir: File, base: String): File {
        var candidate = File(dir, "$base$EXTENSION")
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base ($n)$EXTENSION")
            n++
        }
        return candidate
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim()
        return cleaned.ifBlank { "Untitled Project" }
    }

    private fun imageSizeToJson(opt: ImageSizeOption): JSONObject = when (opt) {
        is ImageSizeOption.Default -> JSONObject().put("type", "original")
        is ImageSizeOption.Preset -> JSONObject().put("type", "preset").put("kb", opt.kb)
        is ImageSizeOption.Custom -> JSONObject().put("type", "custom").put("kb", opt.kb)
        is ImageSizeOption.CustomBytes -> JSONObject().put("type", "custom_bytes").put("bytes", opt.bytes)
    }

    private fun <T> mapToJson(map: Map<Int, T>, value: (T) -> Any): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), value(v)) }
        return obj
    }

    private fun textToJson(t: TextElement): JSONObject {
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("pageIndex", t.pageIndex)
        obj.put("text", t.text)
        obj.put("xFraction", t.xFraction.toDouble())
        obj.put("yFraction", t.yFraction.toDouble())
        obj.put("fontId", t.fontId)
        obj.put("fontRanges", fontRangesToJson(t.fontRanges))
        obj.put("fontSizeSp", t.fontSizeSp.toDouble())
        obj.put("rotationDeg", t.rotationDeg.toDouble())
        obj.put("textColorArgb", t.textColorArgb)
        obj.put("bgColorArgb", t.bgColorArgb ?: JSONObject.NULL)
        obj.put("shadowColorArgb", t.shadowColorArgb)
        obj.put("shadowOffsetXPx", t.shadowOffsetXPx.toDouble())
        obj.put("shadowOffsetYPx", t.shadowOffsetYPx.toDouble())
        obj.put("shadowBlurPx", t.shadowBlurPx.toDouble())
        obj.put("textAlign", t.textAlign)
        obj.put("boldRanges", rangesToJson(t.boldRanges))
        obj.put("italicRanges", rangesToJson(t.italicRanges))
        obj.put("underlineRanges", rangesToJson(t.underlineRanges))
        obj.put("strikeRanges", rangesToJson(t.strikeRanges))
        obj.put("quoteRanges", rangesToJson(t.quoteRanges))
        obj.put("colorRanges", colorRangesToJson(t.colorRanges))
        obj.put("bgColorRanges", colorRangesToJson(t.bgColorRanges))
        obj.put("linkRanges", linkRangesToJson(t.linkRanges))
        obj.put("shadowRanges", shadowRangesToJson(t.shadowRanges))
        return obj
    }

    private fun rangesToJson(ranges: List<IntRange>): JSONArray {
        val arr = JSONArray()
        ranges.forEach { r -> arr.put(JSONArray().put(r.first).put(r.last)) }
        return arr
    }

    private fun fontRangesToJson(ranges: List<FontRange>): JSONArray {
        val arr = JSONArray()
        ranges.forEach { fr ->
            arr.put(JSONObject().put("start", fr.range.first).put("end", fr.range.last).put("fontId", fr.fontId))
        }
        return arr
    }

    private fun fontRangesFromJson(arr: JSONArray?): List<FontRange> {
        if (arr == null) return emptyList()
        val out = mutableListOf<FontRange>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(FontRange(o.optInt("start")..o.optInt("end"), o.optString("fontId", FontCatalog.ID_DEFAULT)))
        }
        return out
    }

    private fun colorRangesToJson(ranges: List<ColorRange>): JSONArray {
        val arr = JSONArray()
        ranges.forEach { cr ->
            arr.put(JSONObject().put("start", cr.range.first).put("end", cr.range.last).put("colorArgb", cr.colorArgb))
        }
        return arr
    }

    private fun linkRangesToJson(ranges: List<LinkRange>): JSONArray {
        val arr = JSONArray()
        ranges.forEach { lr ->
            arr.put(JSONObject().put("start", lr.range.first).put("end", lr.range.last).put("url", lr.url))
        }
        return arr
    }

    private fun shadowRangesToJson(ranges: List<ShadowRange>): JSONArray {
        val arr = JSONArray()
        ranges.forEach { sr ->
            arr.put(
                JSONObject()
                    .put("start", sr.range.first)
                    .put("end", sr.range.last)
                    .put("colorArgb", sr.colorArgb)
                    .put("offsetXPx", sr.offsetXPx.toDouble())
                    .put("offsetYPx", sr.offsetYPx.toDouble())
                    .put("blurPx", sr.blurPx.toDouble())
            )
        }
        return arr
    }

    private fun putBitmap(zip: ZipOutputStream, zipPath: String, bitmap: Bitmap?): String? {
        if (bitmap == null || bitmap.isRecycled) return null
        val source = try {
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else bitmap
        } catch (_: Exception) {
            bitmap
        }
        val bytes = ByteArrayOutputStream()
        val ok = source.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        if (source !== bitmap && !source.isRecycled) source.recycle()
        if (!ok || bytes.size() == 0) return null
        zip.putNextEntry(ZipEntry(zipPath))
        zip.write(bytes.toByteArray())
        zip.closeEntry()
        return zipPath
    }

    fun listProjects(context: Context): List<ProjectListItem> {
        val dir = projectsDir(context)
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION, ignoreCase = true) }
            ?: return emptyList()
        return files.map { file ->
            try {
                ZipFile(file).use { zip ->
                    val entry = zip.getEntry("project.json")
                        ?: return@use ProjectListItem(file, file.nameWithoutExtension, 0, file.lastModified(), false)
                    val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                    val page = json.optJSONObject("page")
                    val pages = page?.optInt("documentPageCount", page.optInt("minPageCount", 1)) ?: 1
                    val savedAt = json.optLong("savedAt", file.lastModified())
                    ProjectListItem(
                        file = file,
                        name = file.nameWithoutExtension,
                        pageCount = pages.coerceAtLeast(1),
                        lastModifiedMillis = maxOf(savedAt, file.lastModified()),
                        readable = true
                    )
                }
            } catch (_: Exception) {
                ProjectListItem(file, file.nameWithoutExtension, 0, file.lastModified(), false)
            }
        }.sortedByDescending { it.lastModifiedMillis }
    }

    fun load(context: Context, file: File): LoadResult {
        if (!file.exists()) return LoadResult(false, error = "Project file not found")
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("project.json")
                    ?: return LoadResult(false, error = "Invalid project file")
                val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                val page = json.optJSONObject("page") ?: JSONObject()
                val layout = json.optJSONObject("layout") ?: JSONObject()
                val numbering = json.optJSONObject("numberingDefaults") ?: JSONObject()
                val missing = mutableListOf<String>()
                restoreCustomFonts(context, zip, json.optJSONArray("customFonts"))
                val images = mutableListOf<ImportedImage>()
                val imagesArr = json.optJSONArray("images")
                if (imagesArr != null) {
                    for (i in 0 until imagesArr.length()) {
                        val o = imagesArr.optJSONObject(i) ?: continue
                        val id = o.optString("id")
                        val rel = o.optString("file", "")
                        val bmp = if (rel.isNotBlank()) decodeZipBitmap(zip, rel) else null
                        if (rel.isNotBlank() && bmp == null && !id.startsWith("spacer_")) {
                            missing.add(id.ifBlank { "image ${i + 1}" })
                        }
                        val uriStr = o.optString("imageUri", "")
                        images.add(
                            ImportedImage(
                                id = id,
                                imageUri = uriStr.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) },
                                bitmap = bmp,
                                approxSizeBytes = if (o.has("approxSizeBytes") && !o.isNull("approxSizeBytes")) o.optInt("approxSizeBytes") else null,
                                linkUrl = o.optString("linkUrl", "").takeIf { it.isNotBlank() },
                                numberLabel = if (o.has("numberLabel") && !o.isNull("numberLabel")) o.optInt("numberLabel") else null,
                                numberXFrac = o.optDouble("numberXFrac", 0.5).toFloat(),
                                numberYFrac = o.optDouble("numberYFrac", 0.5).toFloat(),
                                numberSizeFrac = o.optDouble("numberSizeFrac", 0.18).toFloat(),
                                numberAlpha = o.optDouble("numberAlpha", 0.9).toFloat(),
                                numberBgArgb = o.optLong("numberBgArgb", 0xE6000000L),
                                numberFgArgb = o.optLong("numberFgArgb", 0xFFFFFFFFL),
                                numberWeight = o.optDouble("numberWeight", 0.85).toFloat(),
                                dragOffsetXFrac = o.optDouble("dragOffsetXFrac", 0.0).toFloat(),
                                dragOffsetYFrac = o.optDouble("dragOffsetYFrac", 0.0).toFloat(),
                                cornerRadiusPercent = o.optInt("cornerRadiusPercent", 0),
                                aspectRatioOverride = if (o.has("aspectRatioOverride") && !o.isNull("aspectRatioOverride"))
                                    o.optDouble("aspectRatioOverride").toFloat() else null,
                                ownerPageIndex = if (o.has("ownerPageIndex") && !o.isNull("ownerPageIndex")) o.optInt("ownerPageIndex") else null,
                                absXFrac = o.optDouble("absXFrac", 0.0).toFloat(),
                                absYFrac = o.optDouble("absYFrac", 0.0).toFloat(),
                                absWFrac = o.optDouble("absWFrac", 0.0).toFloat(),
                                absHFrac = o.optDouble("absHFrac", 0.0).toFloat()
                            )
                        )
                    }
                }
                val bgDefault = decodeZipBitmap(zip, page.optString("backgroundFile", "backgrounds/default.jpg"))
                val photoBmp = decodeZipBitmap(zip, page.optString("photoFile", "backgrounds/photo.jpg"))
                val bgOverrides = mutableMapOf<Int, Bitmap>()
                val bgFiles = page.optJSONObject("backgroundFiles")
                if (bgFiles != null) {
                    bgFiles.keys().forEach { key ->
                        val idx = key.toIntOrNull() ?: return@forEach
                        val rel = bgFiles.optString(key)
                        val bmp = decodeZipBitmap(zip, rel)
                        if (bmp != null) bgOverrides[idx] = bmp
                    }
                }
                val pdfRasters = mutableMapOf<Int, Bitmap>()
                val rasterFiles = page.optJSONObject("rasterFiles")
                if (rasterFiles != null) {
                    rasterFiles.keys().forEach { key ->
                        val idx = key.toIntOrNull() ?: return@forEach
                        val rel = rasterFiles.optString(key)
                        val bmp = decodeZipBitmap(zip, rel)
                        if (bmp != null) pdfRasters[idx] = bmp
                    }
                }
                val texts = mutableListOf<TextElement>()
                val textsArr = json.optJSONArray("texts")
                if (textsArr != null) {
                    for (i in 0 until textsArr.length()) {
                        val o = textsArr.optJSONObject(i) ?: continue
                        texts.add(textFromJson(o))
                    }
                }
                val warning = if (missing.isEmpty()) null else "Some images could not be restored (${missing.size})."
                LoadResult(
                    success = true,
                    data = LoadedProject(
                        name = file.nameWithoutExtension,
                        minPageCount = page.optInt("minPageCount", 1).coerceAtLeast(1),
                        pageAspectRatio = page.optDouble("pageAspectRatio", 0.589).toFloat(),
                        isPageLandscape = page.optBoolean("isPageLandscape", false),
                        pageMarginDp = page.optInt("pageMarginDp", 10),
                        pageBackgroundColor = page.optLong("pageBackgroundColor", 0xFFFFFFFFL),
                        pageNumberPosition = page.optString("pageNumberPosition", "NONE"),
                        pageNumberStyle = page.optString("pageNumberStyle", "ARABIC"),
                        pageAspectOverrides = floatMapFromJson(page.optJSONObject("pageAspectOverrides")),
                        pageBackgroundColorOverrides = longMapFromJson(page.optJSONObject("pageBackgroundColorOverrides")),
                        pageBackgroundBitmap = bgDefault,
                        pageBackgroundBitmapOverrides = bgOverrides,
                        pageBackgroundKind = page.optString("pageBackgroundKind", "SOLID"),
                        pageBackgroundColor2 = page.optLong("pageBackgroundColor2", 0xFF90CAF9L),
                        pageBackgroundGradientDir = page.optString("pageBackgroundGradientDir", "TTB"),
                        pageBackgroundKindOverrides = stringMapFromJson(page.optJSONObject("pageBackgroundKindOverrides")),
                        pageBackgroundColor2Overrides = longMapFromJson(page.optJSONObject("pageBackgroundColor2Overrides")),
                        pageBackgroundGradientDirOverrides = stringMapFromJson(page.optJSONObject("pageBackgroundGradientDirOverrides")),
                        pdfPageRasters = pdfRasters,
                        pageBackgroundPhoto = photoBmp,
                        pageBgBlur = page.optDouble("bgBlur", 0.0).toFloat(),
                        pageBgImgOpacity = page.optDouble("bgImgOpacity", 100.0).toFloat(),
                        pageBgBrightness = page.optDouble("bgBrightness", 0.0).toFloat(),
                        pageBgOverlayOn = page.optBoolean("bgOverlayOn", false),
                        pageBgOverlayColor = page.optLong("bgOverlayColor", 0xFFFFFFFFL),
                        pageBgOverlayOpacity = page.optDouble("bgOverlayOpacity", 25.0).toFloat(),
                        pageBgBlurType = page.optString("bgBlurType", "gaussian"),
                        imagesPerRow = layout.optInt("imagesPerRow", 3),
                        imageSpacingDp = layout.optInt("imageSpacingDp", 6),
                        imageCellAspectRatio = layout.optDouble("imageCellAspectRatio", 1.192929).toFloat(),
                        layoutCellAspectRatio = layout.optDouble("layoutCellAspectRatio", 1.192929).toFloat(),
                        imageCornerRadiusPercent = layout.optInt("imageCornerRadiusPercent", 0),
                        selectedImageSizeOption = imageSizeFromJson(layout.optJSONObject("selectedImageSizeOption")),
                        numberingAlpha = numbering.optDouble("alpha", 0.9).toFloat(),
                        numberingSizeFrac = numbering.optDouble("sizeFrac", 0.18).toFloat(),
                        numberingXFrac = numbering.optDouble("xFrac", 0.5).toFloat(),
                        numberingYFrac = numbering.optDouble("yFrac", 0.5).toFloat(),
                        numberingBgArgb = numbering.optLong("bgArgb", 0xFF7C4DFFL),
                        numberingFgArgb = numbering.optLong("fgArgb", 0xFFFFFFFFL),
                        numberingWeight = numbering.optDouble("weight", 0.67).toFloat(),
                        images = images,
                        texts = texts
                    ),
                    warning = warning
                )
            }
        } catch (e: Exception) {
            LoadResult(false, error = "This project file is damaged and cannot be opened.")
        }
    }

    fun deleteProject(file: File): Boolean {
        return try { file.exists() && file.delete() } catch (_: Exception) { false }
    }

    fun renameProject(file: File, newName: String): File? {
        return try {
            val dir = file.parentFile ?: return null
            val base = sanitizeFileName(newName)
            if (base.isBlank()) return null
            val dest = File(dir, "$base$EXTENSION")
            if (dest.canonicalPath == file.canonicalPath) return file
            if (dest.exists()) return null
            if (file.renameTo(dest)) dest else null
        } catch (_: Exception) { null }
    }

    private fun restoreCustomFonts(context: Context, zip: ZipFile, arr: JSONArray?) {
        if (arr == null) return
        val dir = FontCatalog.fontsDir(context)
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val rel = o.optString("file", "")
            if (rel.isBlank()) continue
            val entry = zip.getEntry(rel) ?: continue
            val dest = File(dir, File(rel).name)
            if (dest.exists()) continue
            try {
                zip.getInputStream(entry).use { input ->
                    java.io.FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun decodeZipBitmap(zip: ZipFile, path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val entry = zip.getEntry(path) ?: return null
        return try { zip.getInputStream(entry).use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
    }

    private fun imageSizeFromJson(obj: JSONObject?): ImageSizeOption {
        if (obj == null) return ImageSizeOption.Default
        return when (obj.optString("type")) {
            "preset" -> ImageSizeOption.Preset(obj.optInt("kb", 10))
            "custom" -> ImageSizeOption.Custom(obj.optInt("kb", 100))
            "custom_bytes" -> {
                val bytes = obj.optInt("bytes", 500)
                ImageSizeOption.CustomBytes(if (bytes > 0) bytes else 500)
            }
            else -> ImageSizeOption.Default
        }
    }

    private fun floatMapFromJson(obj: JSONObject?): Map<Int, Float> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<Int, Float>()
        obj.keys().forEach { key ->
            val idx = key.toIntOrNull() ?: return@forEach
            out[idx] = obj.optDouble(key).toFloat()
        }
        return out
    }

    private fun stringMapFromJson(obj: org.json.JSONObject?): Map<Int, String> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<Int, String>()
        obj.keys().forEach { k ->
            val idx = k.toIntOrNull() ?: return@forEach
            out[idx] = obj.optString(k)
        }
        return out
    }

    private fun longMapFromJson(obj: JSONObject?): Map<Int, Long> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<Int, Long>()
        obj.keys().forEach { key ->
            val idx = key.toIntOrNull() ?: return@forEach
            out[idx] = obj.optLong(key)
        }
        return out
    }

    private fun textFromJson(o: JSONObject): TextElement {
        return TextElement(
            id = o.optString("id"),
            pageIndex = o.optInt("pageIndex", 0),
            text = o.optString("text", ""),
            xFraction = o.optDouble("xFraction", 0.1).toFloat(),
            yFraction = o.optDouble("yFraction", 0.1).toFloat(),
            boldRanges = rangesFromJson(o.optJSONArray("boldRanges")),
            italicRanges = rangesFromJson(o.optJSONArray("italicRanges")),
            underlineRanges = rangesFromJson(o.optJSONArray("underlineRanges")),
            strikeRanges = rangesFromJson(o.optJSONArray("strikeRanges")),
            quoteRanges = rangesFromJson(o.optJSONArray("quoteRanges")),
            colorRanges = colorRangesFromJson(o.optJSONArray("colorRanges")),
            bgColorRanges = colorRangesFromJson(o.optJSONArray("bgColorRanges")),
            linkRanges = linkRangesFromJson(o.optJSONArray("linkRanges")),
            shadowRanges = shadowRangesFromJson(o.optJSONArray("shadowRanges")),
            fontId = o.optString("fontId", FontCatalog.ID_DEFAULT),
            fontRanges = fontRangesFromJson(o.optJSONArray("fontRanges")),
            fontSizeSp = o.optDouble("fontSizeSp", 16.0).toFloat(),
            rotationDeg = o.optDouble("rotationDeg", 0.0).toFloat(),
            textColorArgb = o.optLong("textColorArgb", 0xFF000000L),
            bgColorArgb = if (o.has("bgColorArgb") && !o.isNull("bgColorArgb")) o.optLong("bgColorArgb") else null,
            shadowColorArgb = o.optLong("shadowColorArgb", 0x80000000L),
            shadowOffsetXPx = o.optDouble("shadowOffsetXPx", 0.0).toFloat(),
            shadowOffsetYPx = o.optDouble("shadowOffsetYPx", 0.0).toFloat(),
            shadowBlurPx = o.optDouble("shadowBlurPx", 0.0).toFloat(),
            textAlign = o.optString("textAlign", "LEFT").ifBlank { "LEFT" }
        )
    }

    private fun rangesFromJson(arr: JSONArray?): List<IntRange> {
        if (arr == null) return emptyList()
        val out = mutableListOf<IntRange>()
        for (i in 0 until arr.length()) {
            val pair = arr.optJSONArray(i) ?: continue
            if (pair.length() < 2) continue
            out.add(pair.optInt(0)..pair.optInt(1))
        }
        return out
    }

    private fun colorRangesFromJson(arr: JSONArray?): List<ColorRange> {
        if (arr == null) return emptyList()
        val out = mutableListOf<ColorRange>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(ColorRange(o.optInt("start")..o.optInt("end"), o.optLong("colorArgb", 0xFF000000L)))
        }
        return out
    }

    private fun linkRangesFromJson(arr: JSONArray?): List<LinkRange> {
        if (arr == null) return emptyList()
        val out = mutableListOf<LinkRange>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(LinkRange(o.optInt("start")..o.optInt("end"), o.optString("url")))
        }
        return out
    }

    private fun shadowRangesFromJson(arr: JSONArray?): List<ShadowRange> {
        if (arr == null) return emptyList()
        val out = mutableListOf<ShadowRange>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                ShadowRange(
                    range = o.optInt("start")..o.optInt("end"),
                    colorArgb = o.optLong("colorArgb", 0x80000000L),
                    offsetXPx = o.optDouble("offsetXPx", 0.0).toFloat(),
                    offsetYPx = o.optDouble("offsetYPx", 0.0).toFloat(),
                    blurPx = o.optDouble("blurPx", 0.0).toFloat()
                )
            )
        }
        return out
    }

}
