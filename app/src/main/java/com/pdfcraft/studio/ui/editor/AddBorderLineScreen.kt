package com.pdfcraft.studio.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pdfcraft.studio.R

internal val BlAccent = Color(0xFF3D5AFE)
internal val BlBg = Color(0xFFF4F5F9)

/** fix55: shows the Add Border Line window when Image Tools / Text Tools opened it. */
@Composable
internal fun AddBorderLineHost(vm: EditorViewModel) {
    val mode = BorderLineTool.mode
    if (mode == 0) return
    AddBorderLineScreen(vm, image = mode == 1, onClose = { BorderLineTool.mode = 0 })
}

@Composable
private fun frameStyleName(style: Int): String = stringResource(
    when (style) {
        0 -> R.string.bl_cat_solid
        1 -> R.string.bl_cat_double
        2 -> R.string.bl_cat_dashed
        3 -> R.string.bl_cat_dotted
        4 -> R.string.bl_cat_dash_dot
        5 -> R.string.bl_cat_gradient
        6 -> R.string.bl_cat_rainbow
        7 -> R.string.bl_cat_thick_thin
        8 -> R.string.bl_cat_long_dash
        else -> R.string.bl_cat_glow
    }
)

private fun Modifier.framePreview(design: Int, thick: Float, inset: Float, corner: Dp): Modifier =
    this.drawWithContent {
        drawContent()
        drawFrameDesign(design, 2.dp.toPx() * thick, inset.dp.toPx(), corner.toPx(), size.width, size.height)
    }

@Composable
private fun AddBorderLineScreen(vm: EditorViewModel, image: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    ElementBorders.ensureLoaded(ctx)
    var step by remember { mutableIntStateOf(0) } // 0 main, 1 pages, 2 elements
    val selected = remember { mutableStateListOf<String>() }
    val pages = remember { mutableStateListOf<Int>() }
    var shown by remember { mutableIntStateOf(0) }
    var design by remember { mutableIntStateOf(0) }
    var thick by remember { mutableFloatStateOf(1f) }
    var inset by remember { mutableFloatStateOf(0f) }
    var category by remember { mutableIntStateOf(0) }
    val pageCount = if (image) vm.documentPageCount().coerceAtLeast(1)
        else maxOf(vm.documentPageCount(), (vm.textElements.maxOfOrNull { it.pageIndex } ?: 0) + 1).coerceAtLeast(1)
    fun idsOnPage(p: Int): List<String> =
        if (image) vm.imagesOnPage(p).filter { !it.id.startsWith("spacer_") }.map { it.id }
        else vm.textElements.filter { it.pageIndex == p }.map { it.id }

    Dialog(
        onDismissRequest = { if (step > 0) step -= 1 else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(enabled = step > 0) { step -= 1 }
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().background(BlBg).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (step > 0) step -= 1 else onClose() }) {
                    Icon(if (step > 0) Icons.Filled.KeyboardArrowLeft else Icons.Filled.Close, contentDescription = null, tint = Color.Black)
                }
                Text(
                    stringResource(
                        when (step) {
                            1 -> R.string.bl_select_pages
                            2 -> if (image) R.string.bl_select_images else R.string.bl_select_texts
                            else -> R.string.bl_title
                        }
                    ),
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (step == 0) {
                    TextButton(
                        enabled = selected.isNotEmpty(),
                        onClick = {
                            ElementBorders.apply(ctx, selected.toList(), if (design > 0) ElementFrame(design, thick, inset) else null)
                            onClose()
                        }
                    ) { Text(stringResource(R.string.bl_ok), fontWeight = FontWeight.Bold) }
                }
            }
            HorizontalDivider()
            when (step) {
                1 -> PagePickStep(pageCount, pages, { p -> idsOnPage(p).size }, image) {
                    step = 2
                }
                2 -> ElementPickStep(vm, image, pages.sorted(), selected, ::idsOnPage) {
                    shown = 0
                    val first = selected.firstOrNull()?.let { ElementBorders.get(it) }
                    if (first != null) {
                        design = first.design; thick = first.thickness; inset = first.insetDp
                        category = (first.design - 1) % FRAME_STYLES
                    }
                    step = 0
                }
                else -> Column(Modifier.fillMaxSize()) {
                    // Fixed live preview (does not scroll)
                    Box(
                        Modifier.fillMaxWidth().height(230.dp).background(BlBg).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ids = selected.toList()
                        val idx = shown.coerceIn(0, (ids.size - 1).coerceAtLeast(0))
                        val id = ids.getOrNull(idx)
                        val pd = if (design > 0) design else 0
                        if (id == null) {
                            Box(
                                Modifier.fillMaxWidth(0.86f).aspectRatio(16f / 9f).background(Color.White)
                                    .framePreview(if (pd > 0) pd else 3, if (pd > 0) thick else 0.8f, if (pd > 0) inset else 0f, 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = { pages.clear(); step = 1 }) {
                                    Text(stringResource(if (image) R.string.bl_add_images else R.string.bl_add_texts))
                                }
                            }
                        } else if (image) {
                            val img = vm.getImage(id)
                            val bmp = img?.bitmap
                            BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.Center) {
                                val a = if (bmp != null && bmp.height > 0) (bmp.width.toFloat() / bmp.height).coerceIn(0.2f, 5f) else 16f / 9f
                                val bw: Dp
                                val bh: Dp
                                if (a > maxWidth / maxHeight) { bw = maxWidth; bh = maxWidth / a } else { bh = maxHeight; bw = maxHeight * a }
                                val cr = minOf(bw, bh) * ((img?.cornerRadiusPercent ?: 0).coerceIn(0, 100) / 100f) * 0.5f
                                Box(Modifier.size(bw, bh).framePreview(pd, thick, inset, cr)) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cr))
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize().background(Color.White))
                                    }
                                }
                            }
                        } else {
                            val te = vm.textElements.firstOrNull { it.id == id }
                            Box(Modifier.padding(bottom = 30.dp).background(Color.White).framePreview(pd, thick, inset, 0.dp).padding(14.dp)) {
                                Text(
                                    (te?.text ?: "").ifBlank { "Text" },
                                    color = Color(((te?.textColorArgb) ?: 0xFF000000L).toInt()),
                                    fontSize = ((te?.fontSizeSp ?: 16f).coerceIn(10f, 28f)).sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (id != null) {
                            Row(Modifier.align(Alignment.BottomCenter), verticalAlignment = Alignment.CenterVertically) {
                                if (ids.size > 1) {
                                    IconButton(onClick = { shown = (idx - 1 + ids.size) % ids.size }) { Icon(Icons.Filled.KeyboardArrowLeft, null, tint = Color.Black) }
                                    Text((idx + 1).toString() + " / " + ids.size, color = Color.Black, fontSize = 13.sp)
                                    IconButton(onClick = { shown = (idx + 1) % ids.size }) { Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.Black) }
                                }
                                TextButton(onClick = { pages.clear(); step = 1 }) { Text(stringResource(R.string.bl_change)) }
                            }
                        }
                    }
                    // Sliders with values
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(stringResource(R.string.bl_thickness) + ": " + String.format("%.1f", thick) + "x", color = Color.Black, fontSize = 13.sp)
                        Slider(value = thick, onValueChange = { thick = it }, valueRange = ElementBorders.MIN_THICK..ElementBorders.MAX_THICK)
                        Text(stringResource(R.string.bl_inset) + ": " + inset.toInt() + " dp  (" + stringResource(R.string.bl_inset_hint) + ")", color = Color.Black, fontSize = 13.sp)
                        Slider(value = inset, onValueChange = { inset = it }, valueRange = ElementBorders.MIN_INSET..ElementBorders.MAX_INSET)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { thick = 1f; inset = 0f }) { Text(stringResource(R.string.bl_reset)) }
                            TextButton(onClick = { design = 0 }) { Text(stringResource(R.string.bl_none)) }
                        }
                    }
                    // Categories
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        for (s in 0 until FRAME_STYLES) {
                            val on = s == category
                            Text(
                                frameStyleName(s),
                                color = if (on) Color.White else Color.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(50))
                                    .background(if (on) BlAccent else BlBg)
                                    .clickable { category = s }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    // Designs of the category (scrolls, preview stays)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(76.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items((0 until StaticBorder.COUNT / FRAME_STYLES).toList()) { k ->
                            val n = frameDesignFor(category, k)
                            val on = n == design
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(8.dp))
                                    .background(if (on) Color(0xFFE3E8FF) else Color.White)
                                    .border(if (on) 2.dp else 1.dp, if (on) BlAccent else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                    .clickable { design = n }
                                    .padding(8.dp)
                                    .framePreview(n, 1.2f, 0f, 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (on) Icon(Icons.Filled.Check, null, tint = BlAccent, modifier = Modifier.size(20.dp))
                                else Text(n.toString(), color = Color(0xFF999999), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
