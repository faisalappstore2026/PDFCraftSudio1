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

/** fix55: page and element pickers of the Add Border Line window. */
@Composable
internal fun PagePickStep(pageCount: Int, pages: MutableList<Int>, countOn: (Int) -> Int, image: Boolean, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val all = pages.size == pageCount
            Checkbox(checked = all, onCheckedChange = { pages.clear(); if (it) pages.addAll(0 until pageCount) })
            Text(stringResource(R.string.bl_select_all), color = Color.Black)
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items((0 until pageCount).toList()) { p ->
                val on = p in pages
                Row(
                    Modifier.fillMaxWidth().clickable { if (on) pages.remove(p) else pages.add(p) }.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = on, onCheckedChange = { if (on) pages.remove(p) else pages.add(p) })
                    Text(stringResource(R.string.bl_page) + " " + (p + 1), color = Color.Black, modifier = Modifier.weight(1f))
                    Text(
                        "(" + countOn(p) + " " + stringResource(if (image) R.string.bl_images else R.string.bl_texts) + ")",
                        color = Color(0xFF888888), fontSize = 13.sp
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
        Button(onClick = onNext, enabled = pages.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.bl_next))
        }
    }
}

@Composable
internal fun ElementPickStep(
    vm: EditorViewModel,
    image: Boolean,
    pages: List<Int>,
    selected: MutableList<String>,
    idsOnPage: (Int) -> List<String>,
    onDone: () -> Unit
) {
    val groups = pages.map { it to idsOnPage(it) }
    val allIds = groups.flatMap { it.second }.distinct()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val all = allIds.isNotEmpty() && selected.containsAll(allIds)
            Checkbox(checked = all, onCheckedChange = { selected.removeAll(allIds); if (it) selected.addAll(allIds) })
            Text(stringResource(R.string.bl_select_all), color = Color.Black)
        }
        HorizontalDivider()
        if (allIds.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(if (image) R.string.bl_no_images else R.string.bl_no_texts), color = Color(0xFF888888), textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (image) 3 else 1),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((p, ids) in groups) {
                    if (ids.isEmpty()) continue
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.bl_page) + " " + (p + 1), color = Color.Black, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            val allP = selected.containsAll(ids)
                            TextButton(onClick = { selected.removeAll(ids); if (!allP) selected.addAll(ids) }) {
                                Text(stringResource(if (allP) R.string.bl_clear else R.string.bl_select_all))
                            }
                        }
                    }
                    items(ids) { id ->
                        val on = id in selected
                        val toggle = { if (on) selected.remove(id) else selected.add(id); Unit }
                        if (image) {
                            val bmp = vm.getImage(id)?.bitmap
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(BlBg)
                                    .border(if (on) 3.dp else 1.dp, if (on) BlAccent else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                    .clickable { toggle() }
                                    .padding(6.dp)
                            ) {
                                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                Checkbox(checked = on, onCheckedChange = { toggle() }, modifier = Modifier.align(Alignment.TopEnd))
                            }
                        } else {
                            val te = vm.textElements.firstOrNull { it.id == id }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .border(if (on) 2.dp else 1.dp, if (on) BlAccent else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                                    .clickable { toggle() }.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = on, onCheckedChange = { toggle() })
                                Text((te?.text ?: "").ifBlank { "Text" }, color = Color.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = onDone, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.bl_done) + " (" + selected.size + ")")
        }
    }
}
