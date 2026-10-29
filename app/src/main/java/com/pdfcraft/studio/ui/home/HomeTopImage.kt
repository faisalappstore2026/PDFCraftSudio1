package com.pdfcraft.studio.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * fix41: defaults of the Home top image (res/drawable-nodpi/home_top_image.jpg = Grok1.jpg).
 * scale 1.00 = full width of the Home content, offsetX positive = right, offsetY positive = down (dp).
 * Send the "Copy Details" text to change these defaults.
 * fix43: bundled image = Grok2.jpg, corner 10, border design 1 (HomeTopImageBorders.kt).
 * fix42: crop, corner and Import Image (filesDir/home_top_image_custom.jpg, bundled image = fallback).
 */
object HomeTopImageFit {
    const val SCALE = 1.00f
    const val OFFSET_X = 0
    const val OFFSET_Y = 0
    const val MIN_SCALE = 0.20f
    const val MAX_SCALE = 2.00f
    /** 0.20..2.00 with a point every 0.05 */
    const val SCALE_STEPS = 35
    /** dp moved by one Up / Down / Left / Right press */
    const val MOVE_STEP = 4
    /** fix42: crop per side in percent (left + right / top + bottom trimmed equally) */
    const val CROP_X = 0
    const val CROP_Y = 0
    const val MAX_CROP = 45
    /** 0..45 with a point every 5 */
    const val CROP_STEPS = 8
    /** fix42: corner radius in dp */
    const val CORNER = 10 // fix43
    const val MAX_CORNER = 60
    /** 0..60 with a point every 2 */
    const val CORNER_STEPS = 29
    /** fix42: longest side of an imported image (px) */
    const val IMPORT_MAX_SIDE = 1600
    const val CUSTOM_FILE = "home_top_image_custom.jpg"
    /** fix43: border design 1..30 around the top image (0 = no border) */
    const val BORDER = 1
    const val BORDER_COUNT = 250 // fix45
}

/** fix41: saved Home top image size and position (SharedPreferences "home_top_image"). */
internal object HomeTopImageState {
    private const val PREFS = "home_top_image"
    private var loaded = false
    var scale by mutableStateOf(HomeTopImageFit.SCALE)
        private set
    var offsetX by mutableStateOf(HomeTopImageFit.OFFSET_X)
        private set
    var offsetY by mutableStateOf(HomeTopImageFit.OFFSET_Y)
        private set

    var cropX by mutableStateOf(HomeTopImageFit.CROP_X)
        private set
    var cropY by mutableStateOf(HomeTopImageFit.CROP_Y)
        private set
    var corner by mutableStateOf(HomeTopImageFit.CORNER)
        private set
    /** fix43: border design number (0 = none) */
    var border by mutableStateOf(HomeTopImageFit.BORDER)
        private set
    /** fix42: imported image (null = bundled home_top_image.jpg) */
    var customImage by mutableStateOf<ImageBitmap?>(null)
        private set
    private var customLoaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("fix43_defaults", false)) {
            // fix43: Grok2.jpg is the bundled image now, start once from the new defaults
            runCatching { customFile(context).delete() }
            p.edit().clear().putBoolean("fix43_defaults", true).apply()
        }
        scale = p.getFloat("scale", HomeTopImageFit.SCALE)
            .coerceIn(HomeTopImageFit.MIN_SCALE, HomeTopImageFit.MAX_SCALE)
        offsetX = p.getInt("offset_x", HomeTopImageFit.OFFSET_X)
        offsetY = p.getInt("offset_y", HomeTopImageFit.OFFSET_Y)
        cropX = p.getInt("crop_x", HomeTopImageFit.CROP_X).coerceIn(0, HomeTopImageFit.MAX_CROP)
        cropY = p.getInt("crop_y", HomeTopImageFit.CROP_Y).coerceIn(0, HomeTopImageFit.MAX_CROP)
        corner = p.getInt("corner", HomeTopImageFit.CORNER).coerceIn(0, HomeTopImageFit.MAX_CORNER)
        border = p.getInt("border", HomeTopImageFit.BORDER).coerceIn(0, HomeTopImageFit.BORDER_COUNT)
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("scale", scale)
            .putInt("offset_x", offsetX)
            .putInt("offset_y", offsetY)
            .putInt("crop_x", cropX)
            .putInt("crop_y", cropY)
            .putInt("corner", corner)
            .putInt("border", border)
            .putBoolean("fix43_defaults", true)
            .apply()
    }

    private fun customFile(context: Context) = File(context.applicationContext.filesDir, HomeTopImageFit.CUSTOM_FILE)

    /** fix42: reads the imported image once, off the main thread. */
    suspend fun loadCustom(context: Context) {
        if (customLoaded) return
        customLoaded = true
        val f = customFile(context)
        val img = withContext(Dispatchers.IO) {
            if (!f.isFile) null
            else runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()?.asImageBitmap()
        }
        if (img != null) customImage = img
    }

    /** fix42: Import Image: decode (downscaled, EXIF rotation), save as JPEG in filesDir, show it. */
    suspend fun importImage(context: Context, uri: Uri): Boolean {
        val app = context.applicationContext
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val r = app.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val side = maxOf(bounds.outWidth, bounds.outHeight)
                if (side <= 0) return@runCatching null
                var sample = 1
                while (side / (sample * 2) >= HomeTopImageFit.IMPORT_MAX_SIDE) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                var b: Bitmap = r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return@runCatching null
                val big = maxOf(b.width, b.height)
                if (big > HomeTopImageFit.IMPORT_MAX_SIDE) {
                    val k = HomeTopImageFit.IMPORT_MAX_SIDE.toFloat() / big
                    b = Bitmap.createScaledBitmap(
                        b, (b.width * k).roundToInt().coerceAtLeast(1), (b.height * k).roundToInt().coerceAtLeast(1), true
                    )
                }
                val deg = runCatching {
                    r.openInputStream(uri)?.use {
                        when (android.media.ExifInterface(it).getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL
                        )) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } ?: 0f
                }.getOrDefault(0f)
                if (deg != 0f) {
                    val m = Matrix().apply { postRotate(deg) }
                    b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
                }
                val tmp = File(app.filesDir, HomeTopImageFit.CUSTOM_FILE + ".tmp")
                tmp.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                if (!tmp.renameTo(customFile(app))) {
                    tmp.copyTo(customFile(app), overwrite = true)
                    tmp.delete()
                }
                b
            }.getOrNull()
        } ?: return false
        customLoaded = true
        customImage = bmp.asImageBitmap()
        return true
    }

    fun changeCropX(context: Context, value: Float) {
        cropX = value.roundToInt().coerceIn(0, HomeTopImageFit.MAX_CROP)
        save(context)
    }

    fun changeCropY(context: Context, value: Float) {
        cropY = value.roundToInt().coerceIn(0, HomeTopImageFit.MAX_CROP)
        save(context)
    }

    fun changeBorder(context: Context, n: Int) {
        border = n.coerceIn(0, HomeTopImageFit.BORDER_COUNT)
        save(context)
    }

    fun changeCorner(context: Context, value: Float) {
        corner = value.roundToInt().coerceIn(0, HomeTopImageFit.MAX_CORNER)
        save(context)
    }

    fun changeScale(context: Context, value: Float) {
        scale = ((value * 100f).roundToInt() / 100f)
            .coerceIn(HomeTopImageFit.MIN_SCALE, HomeTopImageFit.MAX_SCALE)
        save(context)
    }

    fun move(context: Context, dx: Int, dy: Int) {
        offsetX = (offsetX + dx).coerceIn(-600, 600)
        offsetY = (offsetY + dy).coerceIn(-600, 600)
        save(context)
    }

    fun details(): String =
        String.format(
            Locale.US,
            "HOME_TOP_IMAGE scale=%.2f offsetX=%d offsetY=%d cropX=%d cropY=%d corner=%d border=%d custom=%s",
            scale, offsetX, offsetY, cropX, cropY, corner, border, if (customImage != null) "yes" else "no"
        )
}

/** fix41: the Home top image with the saved size and position. */
@Composable
internal fun HomeTopImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    HomeTopImageState.ensureLoaded(context)
    LaunchedEffect(Unit) { HomeTopImageState.loadCustom(context) } // fix42
    val s = HomeTopImageState.scale
    val ox = HomeTopImageState.offsetX
    val oy = HomeTopImageState.offsetY
    val bundled = ImageBitmap.imageResource(R.drawable.home_top_image)
    val img = HomeTopImageState.customImage ?: bundled
    val cx = HomeTopImageState.cropX
    val cy = HomeTopImageState.cropY
    // fix42: crop equal parts from left + right and top + bottom
    val painter = remember(img, cx, cy) {
        val sx = (img.width * cx / 100).coerceIn(0, (img.width - 1) / 2)
        val sy = (img.height * cy / 100).coerceIn(0, (img.height - 1) / 2)
        BitmapPainter(
            img,
            IntOffset(sx, sy),
            IntSize((img.width - 2 * sx).coerceAtLeast(1), (img.height - 2 * sy).coerceAtLeast(1))
        )
    }
    val cornerShape = RoundedCornerShape(HomeTopImageState.corner.dp)
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(s.coerceAtMost(1f))
                .graphicsLayer {
                    val extra = if (s > 1f) s else 1f
                    scaleX = extra
                    scaleY = extra
                    translationX = ox.dp.toPx()
                    translationY = oy.dp.toPx()
                    shape = cornerShape // fix42: round corners
                    clip = true
                }
                .homeTopImageBorder(HomeTopImageState.border, cornerShape) // fix43
        )
    }
}

@Composable
private fun TopImageButton(text: String, accent: Color, label: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .background(accent.copy(alpha = 0.14f), shape)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Text(text = text, color = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** fix41: top image size slider, Up / Down / Left / Right buttons and Copy Details (below the icon size slider). */
@Composable
internal fun HomeTopImageControls(accent: Color, label: Color) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    HomeTopImageState.ensureLoaded(context)
    val step = HomeTopImageFit.MOVE_STEP
    val copiedMsg = stringResource(R.string.home_top_image_copied)
    val importFailedMsg = stringResource(R.string.home_top_image_import_failed)
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (!HomeTopImageState.importImage(context, uri)) {
                    Toast.makeText(context, importFailedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = accent.copy(alpha = 0.25f),
        activeTickColor = if (accent.luminance() > 0.6f) Color(0xAA000000) else Color(0xCCFFFFFF),
        inactiveTickColor = accent.copy(alpha = 0.85f)
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.home_top_image_size), color = label, fontSize = 13.sp)
        Slider(
            value = HomeTopImageState.scale,
            onValueChange = { HomeTopImageState.changeScale(context, it) },
            valueRange = HomeTopImageFit.MIN_SCALE..HomeTopImageFit.MAX_SCALE,
            steps = HomeTopImageFit.SCALE_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.25f),
                activeTickColor = if (accent.luminance() > 0.6f) Color(0xAA000000) else Color(0xCCFFFFFF),
                inactiveTickColor = accent.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopImageButton(stringResource(R.string.home_top_image_up), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, 0, -step)
            }
            TopImageButton(stringResource(R.string.home_top_image_down), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, 0, step)
            }
            TopImageButton(stringResource(R.string.home_top_image_left), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, -step, 0)
            }
            TopImageButton(stringResource(R.string.home_top_image_right), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, step, 0)
            }
        }
        TopImageButton(stringResource(R.string.home_top_image_copy), accent, label, Modifier.fillMaxWidth()) {
            clipboard.setText(AnnotatedString(HomeTopImageState.details()))
            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
        }
        // fix42: crop and corner sliders
        Text(text = stringResource(R.string.home_top_image_crop_x), color = label, fontSize = 13.sp)
        Slider(
            value = HomeTopImageState.cropX.toFloat(),
            onValueChange = { HomeTopImageState.changeCropX(context, it) },
            valueRange = 0f..HomeTopImageFit.MAX_CROP.toFloat(),
            steps = HomeTopImageFit.CROP_STEPS,
            colors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = stringResource(R.string.home_top_image_crop_y), color = label, fontSize = 13.sp)
        Slider(
            value = HomeTopImageState.cropY.toFloat(),
            onValueChange = { HomeTopImageState.changeCropY(context, it) },
            valueRange = 0f..HomeTopImageFit.MAX_CROP.toFloat(),
            steps = HomeTopImageFit.CROP_STEPS,
            colors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = stringResource(R.string.home_top_image_corner), color = label, fontSize = 13.sp)
        Slider(
            value = HomeTopImageState.corner.toFloat(),
            onValueChange = { HomeTopImageState.changeCorner(context, it) },
            valueRange = 0f..HomeTopImageFit.MAX_CORNER.toFloat(),
            steps = HomeTopImageFit.CORNER_STEPS,
            colors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )
        // fix42: replace the top image with one from the gallery
        TopImageButton(stringResource(R.string.home_top_image_import), accent, label, Modifier.fillMaxWidth()) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
