package com.pdfcraft.studio.ui.common.colorpicker

import android.graphics.Bitmap
import com.pdfcraft.studio.core.image.BackgroundFx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val GradientScrollLock = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return Offset(0f, available.y)
    }
}

@Composable
fun PageBackgroundEditorDialog(
    initial: PageBackgroundSpec,
    initialBitmap: Bitmap?,
    onPickImage: () -> Unit,
    pendingBitmap: Bitmap?,
    previewPages: List<TemplatePreviewPage> = emptyList(),
    initialPreviewPageIndex: Int = -1,
    onCancel: () -> Unit,
    onOk: (PageBackgroundSpec, Bitmap?) -> Unit
) {
    var tab by remember {
        mutableStateOf(
            when (initial.kind) {
                PageBgKind.IMAGE -> "IMAGE"
                PageBgKind.GRADIENT -> "GRADIENT"
                else -> "TEMPLATES"
            }
        )
    }
    var kind by remember { mutableStateOf(initial.kind) }
    var color by remember { mutableStateOf(initial.color) }
    var color2 by remember { mutableStateOf(initial.color2) }
    var dir by remember { mutableStateOf(initial.dir) }
    var gx0 by remember {
        mutableStateOf(if (initial.kind == PageBgKind.GRADIENT) initial.gx0 else 0f)
    }
    var gy0 by remember {
        mutableStateOf(if (initial.kind == PageBgKind.GRADIENT) initial.gy0 else 0.5f)
    }
    var gx1 by remember {
        mutableStateOf(if (initial.kind == PageBgKind.GRADIENT) initial.gx1 else 1f)
    }
    var gy1 by remember {
        mutableStateOf(if (initial.kind == PageBgKind.GRADIENT) initial.gy1 else 0.5f)
    }
    var previewPageIndex by remember { mutableStateOf(initialPreviewPageIndex) }
    var editingStop by remember { mutableStateOf(1) }
    var localBmp by remember { mutableStateOf(pendingBitmap ?: initialBitmap) }
    var blur by remember { mutableStateOf(initial.blur) }
    var imgOpacity by remember { mutableStateOf(initial.imgOpacity) }
    var brightness by remember { mutableStateOf(initial.brightness) }
    var overlayOn by remember { mutableStateOf(initial.overlayOn) }
    var overlayColor by remember { mutableStateOf(initial.overlayColor) }
    var overlayOpacity by remember { mutableStateOf(initial.overlayOpacity) }
    var blurType by remember { mutableStateOf(initial.blurType.ifBlank { "gaussian" }) }
    var colorOpacity by remember {
        val a = ((initial.color ushr 24) and 0xFFL).toFloat() / 255f * 100f
        mutableStateOf(if (a < 1f) 100f else a.coerceIn(0f, 100f))
    }
    var colorAmount by remember { mutableStateOf(100f) }
    fun tuned(c: Long) = tuneArgb(c, colorOpacity, colorAmount)
    LaunchedEffect(pendingBitmap) {
        if (pendingBitmap != null) {
            localBmp = pendingBitmap
            kind = PageBgKind.IMAGE
            tab = "IMAGE"
            overlayOn = false
            blur = 0f
            imgOpacity = 100f
            brightness = 0f
            overlayOpacity = 25f
            blurType = "gaussian"
        }
    }
    val shownBmp = localBmp
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(shownBmp, blur, blurType) {
        val src = shownBmp
        if (src == null || src.isRecycled) {
            previewBmp = null
            return@LaunchedEffect
        }
        if (blur < 1f) {
            previewBmp = null
            return@LaunchedEffect
        }
        val baked = withContext(Dispatchers.Default) {
            BackgroundFx.process(
                src = src,
                blurPct = blur,
                opacityPct = 100f,
                brightness = 0f,
                overlayOn = false,
                overlayColor = 0xFFFFFFFFL,
                overlayOpacityPct = 0f,
                maxSide = 220,
                blurType = blurType
            )
        }
        previewBmp = baked
    }
    val liveFill = if (shownBmp != null) (previewBmp ?: shownBmp) else null
    val liveC1 = tuned(color)
    val liveC2 = if (tab == "GRADIENT") tuned(color2) else null

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text("Page Background", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            var previewAspect by remember { mutableStateOf(16f / 9f) }
            ColorTemplatePreview(
                selectedArgb = liveC1,
                pages = previewPages,
                color2Argb = liveC2,
                fillBitmap = liveFill,
                fillAlpha = imgOpacity / 100f,
                fillBrightness = brightness,
                fillTint = if (shownBmp != null && tab != "IMAGE" && tab != "GRADIENT") liveC1 else null,
                usePageRaster = true,
                gx0 = gx0,
                gy0 = gy0,
                gx1 = gx1,
                gy1 = gy1,
                initialPageIndex = previewPageIndex,
                onPageIndexChange = { previewPageIndex = it },
                onAspectChange = { previewAspect = it }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                KindChip("Color Templates", tab == "TEMPLATES", Modifier.weight(1f)) {
                    tab = "TEMPLATES"
                    kind = PageBgKind.SOLID
                }
                KindChip("Solid Color", tab == "SOLID", Modifier.weight(1f)) {
                    tab = "SOLID"
                    kind = PageBgKind.SOLID
                }
                KindChip("Gradient", tab == "GRADIENT", Modifier.weight(1f)) {
                    tab = "GRADIENT"
                    kind = PageBgKind.GRADIENT
                }
                KindChip("Image", tab == "IMAGE", Modifier.weight(1f)) {
                    tab = "IMAGE"
                    kind = PageBgKind.IMAGE
                }
            }
            Spacer(Modifier.height(10.dp))
            val bodyScroll = rememberScrollState()
            LaunchedEffect(tab) { bodyScroll.scrollTo(0) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(bodyScroll)
            ) {
                if (tab != "IMAGE") {
                    FxSlider("Strength", colorAmount, 0f, 100f, { "${it.toInt()}%" }) { colorAmount = it }
                    Spacer(Modifier.height(8.dp))
                }
                when (tab) {
                    "TEMPLATES" -> ColorTemplatesGrid(
                        selectedArgb = color,
                        onSelect = { color = it }
                    )
                    "SOLID" -> AdvancedColorPicker(
                        argb = color,
                        currentArgb = initial.color,
                        onArgbChange = { color = it }
                    )
                    "IMAGE" -> ImageGalleryDropZone(
                        bitmap = shownBmp,
                        onPick = onPickImage,
                        onRemove = {
                            localBmp = null
                            previewBmp = null
                            blur = 0f
                            imgOpacity = 100f
                            brightness = 0f
                            overlayOn = false
                            overlayOpacity = 25f
                            blurType = "gaussian"
                        },
                        blur = blur,
                        onBlur = { blur = it },
                        imgOpacity = imgOpacity,
                        onOpacity = { imgOpacity = it },
                        brightness = brightness,
                        onBrightness = { brightness = it },
                        overlayOn = overlayOn,
                        onOverlayOn = { overlayOn = it },
                        overlayColor = overlayColor,
                        onOverlayColor = { overlayColor = it },
                        overlayOpacity = overlayOpacity,
                        onOverlayOpacity = { overlayOpacity = it },
                        blurType = blurType,
                        onBlurType = { blurType = it }
                    )
                    "GRADIENT" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GradientStopButton("Color 1", color, editingStop == 1) { editingStop = 1 }
                            Spacer(Modifier.width(12.dp))
                            GradientStopButton("Color 2", color2, editingStop == 2) { editingStop = 2 }
                        }
                        Spacer(Modifier.height(12.dp))
                        AdvancedColorPicker(
                            argb = if (editingStop == 1) color else color2,
                            currentArgb = if (editingStop == 1) initial.color else initial.color2,
                            onArgbChange = { if (editingStop == 1) color = it else color2 = it }
                        )
                        Spacer(Modifier.height(10.dp))
                        GradientDirectionCanvas(
                            color1 = color,
                            color2 = color2,
                            aspect = previewAspect,
                            x0 = gx0,
                            y0 = gy0,
                            x1 = gx1,
                            y1 = gy1,
                            onLineChange = { a, b, c, d ->
                                gx0 = a; gy0 = b; gx1 = c; gy1 = d
                            }
                        )
                    }
                }
                if (tab != "IMAGE") {
                    Spacer(Modifier.height(14.dp))
                    GalleryColorPickSection(
                        onColor = { picked ->
                            if (tab == "GRADIENT" && editingStop == 2) color2 = picked
                            else color = picked
                        },
                        scrollState = bodyScroll
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onOk(
                            run {
                                val photo = shownBmp
                                val colorize = photo != null && tab != "IMAGE"
                                val outColor = if (photo != null && !colorize) 0xFFFFFFFFL else tuned(color)
                                val outColor2 = tuned(color2)
                                val outKind = when {
                                    photo != null -> PageBgKind.IMAGE
                                    tab == "GRADIENT" -> PageBgKind.GRADIENT
                                    else -> PageBgKind.SOLID
                                }
                                PageBackgroundSpec(
                                    kind = outKind,
                                    color = outColor,
                                    color2 = outColor2,
                                    dir = dir,
                                    gx0 = gx0, gy0 = gy0, gx1 = gx1, gy1 = gy1,
                                    blur = if (photo != null) blur else 0f,
                                    imgOpacity = if (photo != null) imgOpacity else 100f,
                                    brightness = if (photo != null) brightness else 0f,
                                    overlayOn = colorize,
                                    overlayColor = outColor,
                                    overlayOpacity = 100f,
                                    blurType = blurType
                                )
                            },
                            shownBmp
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { Text("OK") }
            }
        }
    }
}

data class TemplatePreviewImage(
    val bitmap: Bitmap,
    val xFrac: Float,
    val yFrac: Float,
    val wFrac: Float,
    val hFrac: Float
)

data class TemplatePreviewText(
    val text: String,
    val xFrac: Float,
    val yFrac: Float,
    val colorArgb: Long,
    val sizeSp: Float
)

data class TemplatePreviewPage(
    val index: Int,
    val bitmap: Bitmap? = null,
    val images: List<TemplatePreviewImage> = emptyList(),
    val texts: List<TemplatePreviewText> = emptyList(),
    val aspectRatio: Float = 16f / 9f
)

@Composable
private fun ColorTemplatePreview(
    selectedArgb: Long,
    pages: List<TemplatePreviewPage>,
    color2Argb: Long? = null,
    gx0: Float = 0f,
    gy0: Float = 0.5f,
    gx1: Float = 1f,
    gy1: Float = 0.5f,
    fillBitmap: Bitmap? = null,
    fillAlpha: Float = 1f,
    fillBrightness: Float = 0f,
    fillTint: Long? = null,
    usePageRaster: Boolean = true,
    initialPageIndex: Int = -1,
    onPageIndexChange: (Int) -> Unit = {},
    onAspectChange: (Float) -> Unit = {}
) {
    val ratios = listOf(9f / 16f, 16f / 9f, 1f)
    var ratioIndex by remember { mutableStateOf(1) }
    var previewH by remember { mutableStateOf(132f) }
    var pageIndex by remember {
        mutableStateOf(
            when {
                initialPageIndex >= 0 -> initialPageIndex
                pages.any { it.bitmap != null } -> 0
                else -> -1
            }
        )
    }
    var menuOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val page = pages.firstOrNull { it.index == pageIndex }
    val ar = (page?.aspectRatio?.takeIf { it > 0.05f }) ?: ratios[ratioIndex]
    LaunchedEffect(ar) { onAspectChange(ar) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewH.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDEDED))
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val maxW = maxWidth
                    val maxH = maxHeight
                    var w = maxH * ar
                    var h = maxH
                    if (w > maxW) {
                        w = maxW
                        h = w / ar
                    }
                    val wf = with(density) { w.toPx() }
                    val hf = with(density) { h.toPx() }
                    val pageBg = if (color2Argb != null) {
                        gradientBrush(selectedArgb, color2Argb, gx0, gy0, gx1, gy1)
                    } else {
                        null
                    }
                    val bmp = if (usePageRaster) page?.bitmap?.takeIf { !it.isRecycled } else null
                    val safeFill = fillBitmap?.takeIf { !it.isRecycled }
                    Box(
                        modifier = Modifier
                            .size(w, h)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .then(
                                if (pageBg != null) Modifier.background(pageBg)
                                else Modifier.background(Color(selectedArgb.toInt()))
                            )
                            .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
                    ) {
                        if (safeFill != null) {
                            Image(
                                bitmap = safeFill.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = livePreviewFilter(fillBrightness, fillTint),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = fillAlpha.coerceIn(0f, 1f) }
                            )
                        }
                        if (bmp != null) {
                            val pageImg = bmp.asImageBitmap()
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = pageImg,
                                    dstSize = androidx.compose.ui.unit.IntSize(
                                        size.width.toInt().coerceAtLeast(1),
                                        size.height.toInt().coerceAtLeast(1)
                                    ),
                                    blendMode = BlendMode.Multiply
                                )
                            }
                        }
                        page?.images?.forEach { img ->
                            Image(
                                bitmap = img.bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset {
                                        IntOffset((img.xFrac * wf).toInt(), (img.yFrac * hf).toInt())
                                    }
                                    .size(
                                        with(density) { (img.wFrac * wf).toDp() },
                                        with(density) { (img.hFrac * hf).toDp() }
                                    )
                            )
                        }
                        page?.texts?.forEach { te ->
                            Text(
                                text = te.text,
                                color = Color(te.colorArgb.toInt()),
                                fontSize = (te.sizeSp * 0.4f).coerceIn(5f, 11f).sp,
                                maxLines = 2,
                                modifier = Modifier
                                    .offset {
                                        IntOffset((te.xFrac * wf).toInt(), (te.yFrac * hf).toInt())
                                    }
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
                if (page == null) Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(36.dp)
                        .shadow(2.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable { ratioIndex = (ratioIndex + 1) % ratios.size },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val s = Stroke(width = 1.8.dp.toPx())
                        val blue = Color(0xFF1976D2)
                        drawRoundRect(
                            color = blue,
                            topLeft = Offset(size.width * 0.28f, size.height * 0.08f),
                            size = Size(size.width * 0.44f, size.height * 0.84f),
                            cornerRadius = CornerRadius(2.2.dp.toPx()),
                            style = s
                        )
                        drawRoundRect(
                            color = blue,
                            topLeft = Offset(size.width * 0.08f, size.height * 0.30f),
                            size = Size(size.width * 0.84f, size.height * 0.40f),
                            cornerRadius = CornerRadius(2.2.dp.toPx()),
                            style = s
                        )
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Pages",
                        tint = Color(0xFF333333)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "None",
                                fontWeight = if (pageIndex < 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (pageIndex < 0) Color(0xFF1976D2) else Color.Black
                            )
                        },
                        onClick = {
                            pageIndex = -1
                            onPageIndexChange(-1)
                            menuOpen = false
                        }
                    )
                    pages.forEach { pg ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Page ${pg.index + 1}",
                                        fontWeight = if (pg.index == pageIndex) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (pg.index == pageIndex) Color(0xFF1976D2) else Color.Black
                                    )
                                },
                                onClick = {
                                    pageIndex = pg.index
                                    onPageIndexChange(pg.index)
                                    menuOpen = false
                                }
                            )
                        }
                }
            }
        }
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
                            previewH = (previewH + drag.y / density.density).coerceIn(128f, 360f)
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
    }
}


@Composable
private fun GradientDirectionCanvas(
    color1: Long,
    color2: Long,
    aspect: Float,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    onLineChange: (Float, Float, Float, Float) -> Unit
) {
    val maxHdp = (LocalConfiguration.current.screenHeightDp * 0.30f).coerceIn(150f, 260f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Drag across the page to set the gradient direction.",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val ar = aspect.coerceIn(0.2f, 5f)
            val maxW = maxWidth.value
            var wDp = maxW
            var hDp = wDp / ar
            if (hDp > maxHdp) {
                hDp = maxHdp
                wDp = hDp * ar
            }
            val pts = remember { FloatArray(4) }
            pts[0] = x0; pts[1] = y0; pts[2] = x1; pts[3] = y1
            Box(
                modifier = Modifier
                    .width(wDp.dp)
                    .height(hDp.dp)
                    .shadow(3.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
                    .background(gradientBrush(color1, color2, x0, y0, x1, y1))
                    .nestedScroll(GradientScrollLock)
                    .pointerInput(aspect, wDp, hDp) {
                        var mode = 2
                        var ox = pts[0]
                        var oy = pts[1]
                        var o0x = pts[0]
                        var o0y = pts[1]
                        var o1x = pts[2]
                        var o1y = pts[3]
                        var grabX = 0f
                        var grabY = 0f
                        val scope = this
                        val hit = 48f
                        detectDragGestures(
                            onDragStart = { pos ->
                                val w = scope.size.width.toFloat().coerceAtLeast(1f)
                                val h = scope.size.height.toFloat().coerceAtLeast(1f)
                                val sx = pts[0] * w
                                val sy = pts[1] * h
                                val ex = pts[2] * w
                                val ey = pts[3] * h
                                val d0 = kotlin.math.hypot(pos.x - sx, pos.y - sy)
                                val d1 = kotlin.math.hypot(pos.x - ex, pos.y - ey)
                                val dSeg = distToSegment(pos.x, pos.y, sx, sy, ex, ey)
                                mode = when {
                                    d0 <= hit && d0 <= d1 -> 0
                                    d1 <= hit -> 1
                                    dSeg <= 28f -> 3
                                    else -> 2
                                }
                                val nx = (pos.x / w).coerceIn(0f, 1f)
                                val ny = (pos.y / h).coerceIn(0f, 1f)
                                grabX = nx
                                grabY = ny
                                o0x = pts[0]; o0y = pts[1]; o1x = pts[2]; o1y = pts[3]
                                when (mode) {
                                    0 -> onLineChange(nx, ny, pts[2], pts[3])
                                    1 -> onLineChange(pts[0], pts[1], nx, ny)
                                    2 -> {
                                        ox = nx
                                        oy = ny
                                        onLineChange(nx, ny, nx, ny)
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = scope.size.width.toFloat().coerceAtLeast(1f)
                                val h = scope.size.height.toFloat().coerceAtLeast(1f)
                                val nx = (change.position.x / w).coerceIn(0f, 1f)
                                val ny = (change.position.y / h).coerceIn(0f, 1f)
                                when (mode) {
                                    0 -> onLineChange(nx, ny, pts[2], pts[3])
                                    1 -> onLineChange(pts[0], pts[1], nx, ny)
                                    2 -> onLineChange(ox, oy, nx, ny)
                                    3 -> {
                                        var dx = nx - grabX
                                        var dy = ny - grabY
                                        val minX = kotlin.math.min(o0x, o1x)
                                        val maxX = kotlin.math.max(o0x, o1x)
                                        val minY = kotlin.math.min(o0y, o1y)
                                        val maxY = kotlin.math.max(o0y, o1y)
                                        dx = dx.coerceIn(-minX, 1f - maxX)
                                        dy = dy.coerceIn(-minY, 1f - maxY)
                                        onLineChange(o0x + dx, o0y + dy, o1x + dx, o1y + dy)
                                    }
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p0 = Offset(x0 * size.width, y0 * size.height)
                    val p1 = Offset(x1 * size.width, y1 * size.height)
                    drawLine(Color.White, p0, p1, strokeWidth = 6f)
                    drawLine(Color(0xFF1976D2), p0, p1, strokeWidth = 3f)
                    drawCircle(Color.White, 12f, p0)
                    drawCircle(Color(0xFF1976D2), 8f, p0)
                    drawCircle(Color.White, 12f, p1)
                    drawCircle(Color(0xFFFF9800), 8f, p1)
                }
            }
        }
    }
}

@Composable
private fun ColorTemplatesGrid(
    selectedArgb: Long,
    onSelect: (Long) -> Unit
) {
    val colors = remember {
        listOf(
            0xFFFFFFFFL, 0xFFFAFAFAL, 0xFFF5F5F5L, 0xFFEEEEEE,
            0xFFFFFDE7L, 0xFFFFF8E1L, 0xFFFFF3E0L, 0xFFFFF8F0L,
            0xFFF5F5DCL, 0xFFFFF9C4L, 0xFFFFE0B2L, 0xFFD7CCC8L,
            0xFFE3F2FDL, 0xFFBBDEFBL, 0xFFE8F5E9L, 0xFFC8E6C9L,
            0xFFF3E5F5L, 0xFFFCE4ECL, 0xFFE0F2F1L, 0xFFFFCCBCL,
            0xFFF48FB1L, 0xFFF06292L, 0xFFEC407AL, 0xFFE91E63L,
            0xFF90CAF9L, 0xFF42A5F5L, 0xFF1E88E5L, 0xFF1565C0L,
            0xFFEF9A9AL, 0xFFEF5350L, 0xFFE53935L, 0xFFC62828L,
            0xFFA5D6A7L, 0xFF66BB6AL, 0xFF43A047L, 0xFF2E7D32L,
            0xFFFFCC80L, 0xFFFFA726L, 0xFFFB8C00L, 0xFFEF6C00L,
            0xFFFFF176L, 0xFFFFEE58L, 0xFFFFD54FL, 0xFFFFCA28L,
            0xFFCE93D8L, 0xFFAB47BCL, 0xFF8E24AAL, 0xFF6A1B9AL,
            0xFFBCAAA4L, 0xFFA1887FL, 0xFF8D6E63L, 0xFF6D4C41L,
            0xFF80DEEAL, 0xFF26C6DAL, 0xFF26A69AL, 0xFF00897BL,
            0xFF9FA8DAL, 0xFF5C6BC0L, 0xFF3F51B5L, 0xFF283593L,
            0xFFBDBDBDL, 0xFF9E9E9EL, 0xFF616161L, 0xFF212121L
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.chunked(8).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowColors.forEach { argb ->
                    val selected = argb.toInt() == selectedArgb.toInt()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(argb.toInt()))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelect(argb) }
                    )
                }
                repeat(8 - rowColors.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


private fun distToSegment(
    px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float
): Float {
    val vx = x1 - x0
    val vy = y1 - y0
    val l2 = vx * vx + vy * vy
    if (l2 < 0.0001f) return kotlin.math.hypot(px - x0, py - y0)
    val t = (((px - x0) * vx + (py - y0) * vy) / l2).coerceIn(0f, 1f)
    return kotlin.math.hypot(px - (x0 + t * vx), py - (y0 + t * vy))
}

@Composable
private fun ImageGalleryDropZone(
    bitmap: Bitmap?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    blur: Float,
    onBlur: (Float) -> Unit,
    imgOpacity: Float,
    onOpacity: (Float) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    overlayOn: Boolean,
    onOverlayOn: (Boolean) -> Unit,
    overlayColor: Long,
    onOverlayColor: (Long) -> Unit,
    overlayOpacity: Float,
    onOverlayOpacity: (Float) -> Unit,
    blurType: String,
    onBlurType: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (bitmap == null || bitmap.isRecycled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F7FA))
                    .drawBehind {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                        drawRoundRect(
                            color = Color(0xFF90A4AE),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = stroke
                        )
                    }
                    .clickable(onClick = onPick),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Choose From Gallery",
                        color = Color(0xFF546E7A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to choose a page background",
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
            ) { Text("Remove Photo", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))
            Text("Photo effects", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Text("Blur type", fontSize = 13.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            val types = listOf(
                "gaussian" to "Gaussian",
                "box" to "Box",
                "motion" to "Motion",
                "radial" to "Radial",
                "zoom" to "Zoom",
                "spin" to "Spin",
                "lens" to "Lens",
                "surface" to "Surface",
                "directional" to "Directional",
                "average" to "Average",
                "median" to "Median",
                "pixelate" to "Pixelate",
                "mosaic" to "Mosaic",
                "defocus" to "Defocus",
                "tiltshift" to "Tilt-Shift",
                "selective" to "Selective",
                "edge" to "Edge",
                "background" to "Background",
                "bokeh" to "Bokeh",
                "depth" to "Depth"
            )
            types.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (key, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (blurType == key) Color(0xFF1976D2) else Color(0xFFF0F0F0))
                                .clickable { onBlurType(key) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (blurType == key) Color.White else Color.Black,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }
            FxSlider("Blur", blur, 0f, 100f, { "${it.toInt()}%" }, onBlur)
            FxSlider("Opacity", imgOpacity, 0f, 100f, { "${it.toInt()}%" }, onOpacity)
            FxSlider("Brightness", brightness, -100f, 100f, {
                val n = it.toInt()
                if (n > 0) "+$n" else n.toString()
            }, onBrightness)
        }
    }
}

@Composable
private fun OverlayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF1976D2) else Color(0xFFF0F0F0))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FxSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
            Text(format(value), fontSize = 13.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1976D2),
                activeTrackColor = Color(0xFF1976D2)
            )
        )
    }
}

@Composable
private fun GradientStopButton(
    label: String,
    argb: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF1976D2) else Color.White
    val fg = if (selected) Color.White else Color.Black
    Box(
        modifier = Modifier
            .width(148.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(argb.toInt()))
                    .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
            )
            Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KindChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF1976D2) else Color.White
    val fg = if (selected) Color.White else Color.Black
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, Color(0xFF1976D2), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


private fun tuneArgb(argb: Long, opacityPct: Float, amountPct: Float): Long {
    val r = ((argb shr 16) and 0xFFL).toInt()
    val g = ((argb shr 8) and 0xFFL).toInt()
    val b = (argb and 0xFFL).toInt()
    val amt = (amountPct / 100f).coerceIn(0f, 1f)
    val rr = (r + ((255 - r) * (1f - amt))).toInt().coerceIn(0, 255)
    val gg = (g + ((255 - g) * (1f - amt))).toInt().coerceIn(0, 255)
    val bb = (b + ((255 - b) * (1f - amt))).toInt().coerceIn(0, 255)
    val aa = ((opacityPct / 100f) * 255f).toInt().coerceIn(0, 255)
    return (aa.toLong() shl 24) or (rr.toLong() shl 16) or (gg.toLong() shl 8) or bb.toLong()
}

private fun livePreviewFilter(brightness: Float, tint: Long?): ColorFilter? {
    val add = (brightness.coerceIn(-100f, 100f) / 100f) * 255f
    val needsB = kotlin.math.abs(add) > 1f
    val needsT = tint != null && (tint and 0x00FFFFFFL) != 0x00FFFFFFL
    if (!needsB && !needsT) return null
    val arr = floatArrayOf(
        1f, 0f, 0f, 0f, if (needsB) add else 0f,
        0f, 1f, 0f, 0f, if (needsB) add else 0f,
        0f, 0f, 1f, 0f, if (needsB) add else 0f,
        0f, 0f, 0f, 1f, 0f
    )
    if (needsT) {
        val c = tint ?: 0x00FFFFFFL
        val tr = ((c shr 16) and 0xFFL).toFloat() / 255f
        val tg = ((c shr 8) and 0xFFL).toFloat() / 255f
        val tb = (c and 0xFFL).toFloat() / 255f
        arr[0] *= tr
        arr[6] *= tg
        arr[12] *= tb
    }
    return ColorFilter.colorMatrix(ColorMatrix(arr))
}
