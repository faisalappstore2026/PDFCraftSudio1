package com.pdfcraft.studio.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pdfcraft.studio.core.image.BackgroundFx
import com.pdfcraft.studio.ui.common.colorpicker.PageBackgroundSpec
import com.pdfcraft.studio.ui.common.colorpicker.PageBgKind
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * fix41: editor window backgrounds from the three dots menu.
 * Area 1 (Color Picker 1) = area where the pages are shown.
 * Area 2 (Color Picker 2) = top bar (title, undo / redo, three dots).
 * Saved in SharedPreferences "editor_area_bg", images in filesDir/editor_area_bg_N.png.
 * Not set = the editor design colors (default).
 */
internal object EditorAreaBg {
    const val PAGES = 1
    const val TOP_BAR = 2
    /** fix43: Color Picker 3 = undo/redo number badge */
    const val BADGE = 3
    private const val PREFS = "editor_area_bg"
    private var loaded = false

    private class AreaLook(
        val kind: PageBgKind,
        val color: Long,
        val color2: Long,
        val line: FloatArray,
        val image: ImageBitmap?
    )

    private var pagesLook by mutableStateOf<AreaLook?>(null)
    private var topBarLook by mutableStateOf<AreaLook?>(null)
    private var badgeLook by mutableStateOf<AreaLook?>(null)

    /** fix43: defaults when nothing is saved (Color Templates row 1 col 4 / row 4 col 8) */
    private val DEFAULT_PAGES = AreaLook(PageBgKind.SOLID, 0xFFEEEEEEL, 0xFFEEEEEEL, floatArrayOf(0f, 0f, 1f, 1f), null)
    private val DEFAULT_TOP_BAR = AreaLook(PageBgKind.SOLID, 0xFFC62828L, 0xFFC62828L, floatArrayOf(0f, 0f, 1f, 1f), null)

    private fun get(area: Int): AreaLook? = when (area) {
        PAGES -> pagesLook ?: DEFAULT_PAGES
        TOP_BAR -> topBarLook ?: DEFAULT_TOP_BAR
        else -> badgeLook
    }
    private fun put(area: Int, look: AreaLook?) {
        when (area) {
            PAGES -> pagesLook = look
            TOP_BAR -> topBarLook = look
            else -> badgeLook = look
        }
    }

    private fun imageFile(context: Context, area: Int) = File(context.filesDir, "editor_area_bg_" + area + ".png")

    fun isSet(area: Int): Boolean = get(area) != null

    /** Starting values for the picker window (an image starts on the color tabs). */
    fun spec(area: Int): PageBackgroundSpec {
        val l = get(area) ?: return PageBackgroundSpec()
        if (l.kind == PageBgKind.GRADIENT) {
            return PageBackgroundSpec(
                kind = PageBgKind.GRADIENT,
                color = l.color,
                color2 = l.color2,
                gx0 = l.line[0], gy0 = l.line[1], gx1 = l.line[2], gy1 = l.line[3]
            )
        }
        return PageBackgroundSpec(kind = PageBgKind.SOLID, color = l.color)
    }

    suspend fun load(context: Context) {
        if (loaded) return
        loaded = true
        val app = context.applicationContext
        for (area in listOf(PAGES, TOP_BAR, BADGE)) {
            val look = withContext(Dispatchers.IO) { readLook(app, area) }
            put(area, look)
        }
    }

    private fun readLook(context: Context, area: Int): AreaLook? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = "a" + area + "_"
        val kindName = p.getString(k + "kind", null) ?: return null
        val kind = runCatching { PageBgKind.valueOf(kindName) }.getOrNull() ?: return null
        val line = floatArrayOf(
            p.getFloat(k + "gx0", 0f), p.getFloat(k + "gy0", 0.5f),
            p.getFloat(k + "gx1", 1f), p.getFloat(k + "gy1", 0.5f)
        )
        val image = if (kind == PageBgKind.IMAGE) {
            val f = imageFile(context, area)
            if (!f.isFile) return null
            runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()?.asImageBitmap() ?: return null
        } else null
        return AreaLook(kind, p.getLong(k + "color", 0xFFFFFFFFL), p.getLong(k + "color2", 0xFF90CAF9L), line, image)
    }

    /** OK in the picker window: bake the image (blur, opacity, brightness, overlay) and save. */
    suspend fun apply(context: Context, area: Int, spec: PageBackgroundSpec, bitmap: Bitmap?) {
        val app = context.applicationContext
        val photo = bitmap?.takeIf { !it.isRecycled }
        val kind = if (photo != null) PageBgKind.IMAGE else if (spec.kind == PageBgKind.GRADIENT) PageBgKind.GRADIENT else PageBgKind.SOLID
        val baked: Bitmap? = if (photo != null) withContext(Dispatchers.Default) {
            runCatching {
                BackgroundFx.process(
                    src = photo,
                    blurPct = spec.blur,
                    opacityPct = spec.imgOpacity,
                    brightness = spec.brightness,
                    overlayOn = spec.overlayOn,
                    overlayColor = spec.overlayColor,
                    overlayOpacityPct = spec.overlayOpacity,
                    maxSide = 1400,
                    blurType = spec.blurType
                )
            }.getOrNull()
        } else null
        if (kind == PageBgKind.IMAGE && baked == null) return
        val saved = withContext(Dispatchers.IO) {
            runCatching {
                if (baked != null) {
                    imageFile(app, area).outputStream().use { baked.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } else {
                    imageFile(app, area).delete()
                }
                val k = "a" + area + "_"
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(k + "kind", kind.name)
                    .putLong(k + "color", spec.color)
                    .putLong(k + "color2", spec.color2)
                    .putFloat(k + "gx0", spec.gx0)
                    .putFloat(k + "gy0", spec.gy0)
                    .putFloat(k + "gx1", spec.gx1)
                    .putFloat(k + "gy1", spec.gy1)
                    .apply()
                true
            }.getOrDefault(false)
        }
        if (!saved) return
        loaded = true
        put(
            area,
            AreaLook(kind, spec.color, spec.color2, floatArrayOf(spec.gx0, spec.gy0, spec.gx1, spec.gy1), baked?.asImageBitmap())
        )
    }

    /** "None" in the picker window = back to the editor design colors (default). */
    fun reset(context: Context, area: Int) {
        val app = context.applicationContext
        put(area, null)
        val k = "a" + area + "_"
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(k + "kind").remove(k + "color").remove(k + "color2")
            .remove(k + "gx0").remove(k + "gy0").remove(k + "gx1").remove(k + "gy1")
            .apply()
        runCatching { imageFile(app, area).delete() }
    }

    /** Draws the chosen background of an area (nothing when not set). */
    fun draw(m: Modifier, area: Int): Modifier {
        val l = get(area) ?: return m
        return m.drawBehind {
            val w = size.width
            val h = size.height
            when (l.kind) {
                PageBgKind.GRADIENT -> drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color(l.color), Color(l.color2)),
                        start = Offset(l.line[0] * w, l.line[1] * h),
                        end = Offset(l.line[2] * w, l.line[3] * h)
                    )
                )
                PageBgKind.IMAGE -> {
                    val img = l.image
                    if (img != null && w > 0f && h > 0f) {
                        // center crop
                        val iw = img.width.toFloat()
                        val ih = img.height.toFloat()
                        val k = maxOf(w / iw, h / ih)
                        val sw = (w / k).coerceAtMost(iw)
                        val sh = (h / k).coerceAtMost(ih)
                        val sx = ((iw - sw) / 2f).toInt().coerceIn(0, img.width - 1)
                        val sy = ((ih - sh) / 2f).toInt().coerceIn(0, img.height - 1)
                        drawImage(
                            image = img,
                            srcOffset = IntOffset(sx, sy),
                            srcSize = IntSize(
                                sw.toInt().coerceIn(1, img.width - sx),
                                sh.toInt().coerceIn(1, img.height - sy)
                            ),
                            dstSize = IntSize(w.roundToInt(), h.roundToInt())
                        )
                    }
                }
                PageBgKind.SOLID, PageBgKind.NONE -> drawRect(Color(l.color))
            }
        }
    }
}

/** fix41: Color Picker 1 / 2 background of an editor area (nothing when not set). */
internal fun Modifier.editorAreaBackground(area: Int): Modifier = EditorAreaBg.draw(this, area)
