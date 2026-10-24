package com.pdfcraft.studio.ui.common.colorpicker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class PageBgKind { SOLID, GRADIENT, IMAGE, NONE }

enum class GradientDir {
    LTR, RTL, TTB, BTT, TLBR, BLTR;

    fun label(): String = when (this) {
        LTR -> "Left → Right"
        RTL -> "Right → Left"
        TTB -> "Top → Bottom"
        BTT -> "Bottom → Top"
        TLBR -> "Top-Left → Bottom-Right"
        BLTR -> "Bottom-Left → Top-Right"
    }
}

data class PageBackgroundSpec(
    val kind: PageBgKind = PageBgKind.SOLID,
    val color: Long = 0xFFFFFFFFL,
    val color2: Long = 0xFF90CAF9L,
    val dir: GradientDir = GradientDir.LTR,
    val gx0: Float = 0f,
    val gy0: Float = 0.5f,
    val gx1: Float = 1f,
    val gy1: Float = 0.5f,
    val blur: Float = 0f,
    val imgOpacity: Float = 100f,
    val brightness: Float = 0f,
    val overlayOn: Boolean = false,
    val overlayColor: Long = 0xFFFFFFFFL,
    val overlayOpacity: Float = 25f,
    val blurType: String = "gaussian"
)

fun parseGradientLine(dir: String): FloatArray {
    if (dir.startsWith("LINE:")) {
        val p = dir.removePrefix("LINE:").split(",")
        if (p.size >= 4) {
            return floatArrayOf(
                p[0].toFloatOrNull() ?: 0.5f,
                p[1].toFloatOrNull() ?: 0f,
                p[2].toFloatOrNull() ?: 0.5f,
                p[3].toFloatOrNull() ?: 1f
            )
        }
    }
    return when (dir) {
        "LTR" -> floatArrayOf(0f, 0.5f, 1f, 0.5f)
        "RTL" -> floatArrayOf(1f, 0.5f, 0f, 0.5f)
        "BTT" -> floatArrayOf(0.5f, 1f, 0.5f, 0f)
        "TLBR" -> floatArrayOf(0f, 0f, 1f, 1f)
        "BLTR" -> floatArrayOf(0f, 1f, 1f, 0f)
        else -> floatArrayOf(0.5f, 0f, 0.5f, 1f)
    }
}

fun encodeGradientLine(x0: Float, y0: Float, x1: Float, y1: Float): String {
    var a = x0.coerceIn(0f, 1f)
    var b = y0.coerceIn(0f, 1f)
    var c = x1.coerceIn(0f, 1f)
    var d = y1.coerceIn(0f, 1f)
    val dx = c - a
    val dy = d - b
    if (dx * dx + dy * dy < 0.0004f) {
        d = (b + 0.25f).coerceAtMost(1f)
        if (d == b) d = (b - 0.25f).coerceAtLeast(0f)
    }
    return "LINE:$a,$b,$c,$d"
}

private class FracLinearGradientBrush(
    private val c1: Color,
    private val c2: Color,
    private val x0: Float,
    private val y0: Float,
    private val x1: Float,
    private val y1: Float
) : ShaderBrush() {
    override fun createShader(size: Size): android.graphics.Shader {
        return LinearGradientShader(
            Offset(x0 * size.width, y0 * size.height),
            Offset(x1 * size.width, y1 * size.height),
            listOf(c1, c2),
            tileMode = TileMode.Clamp
        )
    }
}

fun hsvToArgb(h: Float, s: Float, v: Float): Long {
    val hsv = floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    return (AColor.HSVToColor(hsv).toLong() and 0xFFFFFFFFL) or 0xFF000000L
}

fun argbToHsv(argb: Long): FloatArray {
    val hsv = FloatArray(3)
    AColor.colorToHSV(argb.toInt(), hsv)
    return hsv
}

fun argbToHex(argb: Long): String {
    val rgb = argb.toInt() and 0xFFFFFF
    return "#%06X".format(rgb)
}

fun hexToArgb(hex: String): Long? {
    val t = hex.trim().removePrefix("#")
    if (t.length != 6) return null
    return t.toLongOrNull(16)?.let { 0xFF000000L or it }
}

fun gradientBrush(c1: Long, c2: Long, x0: Float, y0: Float, x1: Float, y1: Float): Brush {
    return FracLinearGradientBrush(
        Color(c1.toInt()), Color(c2.toInt()), x0, y0, x1, y1
    )
}

fun gradientBrush(c1: Long, c2: Long, dir: String): Brush {
    val p = parseGradientLine(dir)
    return gradientBrush(c1, c2, p[0], p[1], p[2], p[3])
}

fun gradientBrush(c1: Long, c2: Long, dir: GradientDir): Brush {
    return gradientBrush(c1, c2, dir.name)
}

private val ConsumeVerticalScroll = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return Offset(0f, available.y)
    }
}

@Composable
fun AdvancedColorPicker(
    argb: Long,
    onArgbChange: (Long) -> Unit,
    currentArgb: Long? = argb,
    modifier: Modifier = Modifier
) {
    val start = remember { argbToHsv(argb) }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }
    var hexText by remember { mutableStateOf(argbToHex(argb)) }
    var lastEmitted by remember { mutableStateOf(argb) }

    fun emit(h: Float, s: Float, v: Float) {
        hue = h.coerceIn(0f, 360f)
        sat = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        val next = hsvToArgb(hue, sat, value)
        lastEmitted = next
        hexText = argbToHex(next)
        onArgbChange(next)
    }

    fun emitHue(h: Float) {
        val s = if (sat < 0.02f) 1f else sat
        val v = if (value < 0.02f) 1f else value
        emit(h, s, v)
    }

    LaunchedEffect(argb) {
        if (argb != lastEmitted) {
            val hsv = argbToHsv(argb)
            if (hsv[1] > 0.002f) hue = hsv[0]
            sat = hsv[1]
            value = hsv[2]
            hexText = argbToHex(argb)
            lastEmitted = argb
        }
    }

    val rgb = argb.toInt()
    val r = (rgb shr 16) and 0xFF
    val gCol = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF

    var showMore by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .nestedScroll(ConsumeVerticalScroll),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SvSquare(
                hue = hue,
                sat = sat,
                value = value,
                onSv = { s, v -> emit(hue, s, v) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
            )
            HueBar(
                hue = hue,
                onHue = { h -> emitHue(h) },
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clickable { showMore = !showMore }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (showMore) "Hide Options" else "Show More",
                color = Color(0xFF1976D2),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (showMore) "\u25B2" else "\u25BC",
                color = Color(0xFF1976D2),
                fontSize = 12.sp
            )
        }
        if (showMore) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = hexText,
                onValueChange = { raw ->
                    hexText = raw
                    hexToArgb(raw)?.let(onArgbChange)
                },
                label = { Text("HEX") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniIntField("R", r, 0, 255, Modifier.weight(1f)) { nr ->
                    onArgbChange(0xFF000000L or (nr.toLong() shl 16) or (gCol.toLong() shl 8) or b.toLong())
                }
                MiniIntField("G", gCol, 0, 255, Modifier.weight(1f)) { ng ->
                    onArgbChange(0xFF000000L or (r.toLong() shl 16) or (ng.toLong() shl 8) or b.toLong())
                }
                MiniIntField("B", b, 0, 255, Modifier.weight(1f)) { nb ->
                    onArgbChange(0xFF000000L or (r.toLong() shl 16) or (gCol.toLong() shl 8) or nb.toLong())
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniIntField("H", hue.roundToInt(), 0, 360, Modifier.weight(1f)) { nh ->
                    emitHue(nh.toFloat())
                }
                MiniIntField("S", (sat * 100f).roundToInt(), 0, 100, Modifier.weight(1f)) { ns ->
                    emit(hue, ns / 100f, value)
                }
                MiniIntField("V", (value * 100f).roundToInt(), 0, 100, Modifier.weight(1f)) { nv ->
                    emit(hue, sat, nv / 100f)
                }
            }
        }
    }
}

@Composable
fun GalleryColorPickSection(
    onColor: (Long) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var galleryBmp by remember { mutableStateOf<Bitmap?>(null) }
    var galleryH by remember { mutableStateOf(176f) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) galleryBmp = decodeGalleryBitmap(context, uri)
    }
    LaunchedEffect(galleryBmp) {
        if (galleryBmp != null && scrollState != null) {
            delay(80)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick Color From Gallery", color = Color.Black)
        }
        val bmp = galleryBmp
        if (bmp != null && !bmp.isRecycled) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Drag on the photo to pick a color",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            Spacer(Modifier.height(6.dp))
            GalleryEyedropper(
                bitmap = bmp,
                onColor = onColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(galleryH.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 28.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                galleryH = (galleryH + drag.y / density.density).coerceIn(120f, 360f)
                            }
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(36.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFB0B0B0))
                    )
                }
            }
            TextButton(onClick = { galleryBmp = null }) {
                Text("Remove photo")
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, argb: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(argb.toInt()))
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun MiniIntField(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onCommit: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() }.take(3)
            text.toIntOrNull()?.coerceIn(min, max)?.let(onCommit)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun SvSquare(
    hue: Float,
    sat: Float,
    value: Float,
    onSv: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = Color(hsvToArgb(hue, 1f, 1f).toInt())
    val density = LocalDensity.current
    val onSvState = remember { mutableStateOf(onSv) }
    onSvState.value = onSv
    var dragging by remember { mutableStateOf(false) }
    var dSat by remember { mutableStateOf(sat) }
    var dVal by remember { mutableStateOf(value) }
    val showSat = if (dragging) dSat else sat
    val showVal = if (dragging) dVal else value

    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                var downId: androidx.compose.ui.input.pointer.PointerId? = null
                while (downId == null) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.pressed } ?: continue
                    change.consume()
                    downId = change.id
                    dragging = true
                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    dSat = s
                    dVal = v
                    onSvState.value(s, v)
                }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    change.consume()
                    if (!change.pressed) break
                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    dSat = s
                    dVal = v
                    onSvState.value(s, v)
                }
                dragging = false
            }
        }
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (showSat * wPx - 8.dp.toPx()).roundToInt(),
                        ((1f - showVal) * hPx - 8.dp.toPx()).roundToInt()
                    )
                }
                .size(16.dp)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHue: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    }
    val density = LocalDensity.current
    val onHueState = remember { mutableStateOf(onHue) }
    onHueState.value = onHue
    var dragging by remember { mutableStateOf(false) }
    var dHue by remember { mutableStateOf(hue) }
    val showHue = if (dragging) dHue else hue

    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                var downId: androidx.compose.ui.input.pointer.PointerId? = null
                while (downId == null) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.pressed } ?: continue
                    change.consume()
                    downId = change.id
                    dragging = true
                    val h = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f) * 360f
                    dHue = h
                    onHueState.value(h)
                }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    change.consume()
                    if (!change.pressed) break
                    val h = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f) * 360f
                    dHue = h
                    onHueState.value(h)
                }
                dragging = false
            }
        }
    ) {
        val hPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(colors))
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(0, ((showHue / 360f) * hPx - 6.dp.toPx()).roundToInt()) }
                .fillMaxWidth()
                .height(12.dp)
                .border(2.dp, Color.White, RoundedCornerShape(6.dp))
        )
    }
}

private fun decodeGalleryBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val max = 1280
        var sample = 1
        val w = bounds.outWidth.coerceAtLeast(1)
        val h = bounds.outHeight.coerceAtLeast(1)
        while (w / sample > max || h / sample > max) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun GalleryEyedropper(
    bitmap: Bitmap,
    onColor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember(bitmap) { mutableStateOf(1f) }
    var pan by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var mark by remember { mutableStateOf<Offset?>(null) }
    var samplePt by remember { mutableStateOf<IntOffset?>(null) }
    var showLoupe by remember { mutableStateOf(false) }
    val onColorState = remember { mutableStateOf(onColor) }
    onColorState.value = onColor
    val zoomState = remember { mutableStateOf(zoom) }
    val panState = remember { mutableStateOf(pan) }
    zoomState.value = zoom
    panState.value = pan

    fun viewToBitmap(pos: Offset, vw: Float, vh: Float): IntOffset? {
        val z = zoomState.value.coerceAtLeast(0.01f)
        val p = panState.value
        val lx = (pos.x - p.x) / z
        val ly = (pos.y - p.y) / z
        val bw = bitmap.width
        val bh = bitmap.height
        if (bw <= 0 || bh <= 0 || vw <= 1f || vh <= 1f) return null
        val fit = min(vw / bw.toFloat(), vh / bh.toFloat())
        val dw = bw * fit
        val dh = bh * fit
        val left = (vw - dw) / 2f
        val top = (vh - dh) / 2f
        if (lx < left || ly < top || lx > left + dw || ly > top + dh) return null
        val x = ((lx - left) / dw * bw).toInt().coerceIn(0, bw - 1)
        val y = ((ly - top) / dh * bh).toInt().coerceIn(0, bh - 1)
        return IntOffset(x, y)
    }

    fun pick(pos: Offset, vw: Float, vh: Float) {
        val pt = viewToBitmap(pos, vw, vh) ?: return
        samplePt = pt
        mark = pos
        val px = bitmap.getPixel(pt.x, pt.y)
        onColorState.value((px.toLong() and 0xFFFFFFFFL) or 0xFF000000L)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = modifier
                .background(Color(0xFFF5F5F5))
                .clipToBounds()
                .onSizeChanged { viewSize = it }
                .pointerInput(bitmap) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var lastCx = Float.NaN
                        var lastCy = Float.NaN
                        showLoupe = false
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                showLoupe = false
                                mark = null
                                val z = event.calculateZoom()
                                val c = event.calculateCentroid()
                                val panDelta = event.calculatePan()
                                if (lastCx.isNaN()) {
                                    lastCx = c.x
                                    lastCy = c.y
                                }
                                val extra = Offset(c.x - lastCx, c.y - lastCy)
                                lastCx = c.x
                                lastCy = c.y
                                val oldS = zoom
                                val newS = (oldS * z).coerceIn(1f, 6f)
                                val r = newS / oldS.coerceAtLeast(0.01f)
                                val oldP = pan
                                var nx = c.x - (c.x - oldP.x) * r + extra.x + panDelta.x
                                var ny = c.y - (c.y - oldP.y) * r + extra.y + panDelta.y
                                val vw = size.width.toFloat().coerceAtLeast(1f)
                                val vh = size.height.toFloat().coerceAtLeast(1f)
                                nx = nx.coerceIn(vw * (1f - newS), 0f)
                                ny = ny.coerceIn(vh * (1f - newS), 0f)
                                if (newS <= 1.01f) {
                                    nx = 0f
                                    ny = 0f
                                }
                                zoom = newS
                                pan = Offset(nx, ny)
                                zoomState.value = zoom
                                panState.value = pan
                                event.changes.forEach { it.consume() }
                            } else {
                                lastCx = Float.NaN
                                lastCy = Float.NaN
                                val c = pressed.firstOrNull()
                                if (c != null) {
                                    showLoupe = true
                                    pick(c.position, size.width.toFloat(), size.height.toFloat())
                                    c.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        showLoupe = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = zoom
                        val p = pan
                        scaleX = s
                        scaleY = s
                        translationX = p.x
                        translationY = p.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            )
        }
        val sp = samplePt
        if (showLoupe && sp != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-96).dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .border(1.dp, Color(0xFF1976D2), CircleShape)
                    .background(Color.White)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val srcR = 14
                    val left = (sp.x - srcR).coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
                    val top = (sp.y - srcR).coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
                    val srcW = min(srcR * 2, bitmap.width - left).coerceAtLeast(1)
                    val srcH = min(srcR * 2, bitmap.height - top).coerceAtLeast(1)
                    val path = Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) }
                    clipPath(path) {
                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset(left, top),
                            srcSize = IntSize(srcW, srcH),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.None
                        )
                    }
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.White, Offset(cx, 8f), Offset(cx, size.height - 8f), 3f)
                    drawLine(Color.White, Offset(8f, cy), Offset(size.width - 8f, cy), 3f)
                    drawLine(Color(0xFF1976D2), Offset(cx, 8f), Offset(cx, size.height - 8f), 1.4f)
                    drawLine(Color(0xFF1976D2), Offset(8f, cy), Offset(size.width - 8f, cy), 1.4f)
                    drawCircle(Color.White, 5f, Offset(cx, cy), style = Stroke(width = 2.4f))
                }
            }
        }
    }
}

// fix19: OldColorSelectionDialog (old color selection window) removed.
