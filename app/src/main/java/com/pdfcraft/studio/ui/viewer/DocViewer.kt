package com.pdfcraft.studio.ui.viewer

// fix56: in-app viewer for Text, Word (.docx), Excel (.xlsx, .csv) and PPT (.pptx)

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun docxSpans(paras: List<DocPara>): CharSequence {
    val sb = SpannableStringBuilder()
    for (p in paras) {
        val a = sb.length
        sb.append(p.text)
        val b = sb.length
        if (p.heading > 0 && b > a) {
            sb.setSpan(StyleSpan(Typeface.BOLD), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan((1.7f - 0.12f * p.heading).coerceAtLeast(1.05f)), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        for (r in p.bold) if (r.last + 1 <= p.text.length && r.first >= 0) sb.setSpan(StyleSpan(Typeface.BOLD), a + r.first, a + r.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        for (r in p.italic) if (r.last + 1 <= p.text.length && r.first >= 0) sb.setSpan(StyleSpan(Typeface.ITALIC), a + r.first, a + r.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(if (p.heading > 0) "\n" else "\n\n")
    }
    return sb
}

private class TvHolder { var tv: TextView? = null }

@Composable
internal fun DocViewerScreen(d: DocFile, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val ext = d.ext.lowercase()
    val kind = when (ext) { "txt" -> 1; "docx" -> 2; "xlsx", "csv" -> 3; "pptx" -> 4; else -> 0 }
    val speakable = kind == 1 || kind == 2
    var loading by remember { mutableStateOf(kind != 0) }
    var error by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf<CharSequence>("") }
    var sheets by remember { mutableStateOf<List<DocSheet>>(emptyList()) }
    var slides by remember { mutableStateOf<List<DocSlide>>(emptyList()) }
    var fontSp by remember { mutableFloatStateOf(16f) }
    var ttsOpen by remember { mutableStateOf(false) }
    var selOnly by remember { mutableStateOf<String?>(null) }
    val holder = remember { TvHolder() }
    val tts = remember { if (speakable) DocTts(ctx) else null }
    var lastVoice by remember { mutableStateOf<android.speech.tts.Voice?>(null) }
    var lastStyle by remember { mutableStateOf(TTS_STYLES[0]) }
    DisposableEffect(Unit) { onDispose { tts?.release() } }

    LaunchedEffect(d.path) {
        if (kind == 0) return@LaunchedEffect
        try {
            withContext(Dispatchers.IO) {
                when (kind) {
                    1 -> { text = DocParse.readText(d.file) }
                    2 -> { text = docxSpans(DocParse.docx(d.file)) }
                    3 -> { sheets = if (ext == "csv") listOf(DocParse.csv(d.file)) else DocParse.xlsx(d.file) }
                    else -> { slides = DocParse.pptx(d.file) }
                }
                Unit
            }
        } catch (t: Throwable) {
            error = "This file could not be read. It may be damaged or protected."
        }
        loading = false
    }

    fun selectedText(): String? {
        val tv = holder.tv ?: return null
        val a = minOf(tv.selectionStart, tv.selectionEnd)
        val b = maxOf(tv.selectionStart, tv.selectionEnd)
        if (a < 0 || b <= a || b > tv.text.length) return null
        return tv.text.subSequence(a, b).toString().takeIf { it.isNotBlank() }
    }

    Dialog(onDismissRequest = { if (ttsOpen) ttsOpen = false else onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF7F7F9))) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 6.dp)) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DocInk) }
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(kindColor(DocKind.of(ext))), contentAlignment = Alignment.Center) {
                        Text(ext.uppercase().take(4), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = DocInk, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(DocFiles.sizeText(d.size), color = DocSub, fontSize = 12.sp, maxLines = 1)
                    }
                    if (kind == 1 || kind == 2) {
                        Text("A\u2212", color = DocInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { fontSp = (fontSp - 2f).coerceAtLeast(10f) }.padding(8.dp))
                        Text("A+", color = DocInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { fontSp = (fontSp + 2f).coerceAtMost(34f) }.padding(8.dp))
                    }
                    if (tts != null) {
                        IconButton(onClick = { selOnly = selectedText(); ttsOpen = true }) { SpeakerIcon(DocInk) }
                    }
                    IconButton(onClick = { DocFiles.open(ctx, d) }) { Icon(Icons.Filled.ExitToApp, contentDescription = "Open with", tint = DocInk) }
                }
                HorizontalDivider(color = Color(0xFFEDEDED))
                val err = error
                when {
                    kind == 0 -> Unsupported(d, "." + ext + " files can't be shown inside the app yet. You can open this file with another app.")
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DocRed) }
                    err != null -> Unsupported(d, err)
                    kind == 1 || kind == 2 -> AndroidView(
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        factory = { c ->
                            val tv = TextView(c)
                            tv.setTextIsSelectable(true)
                            tv.setTextColor(0xFF212121.toInt())
                            tv.setLineSpacing(0f, 1.25f)
                            val pad = (18 * c.resources.displayMetrics.density).toInt()
                            tv.setPadding(pad, pad, pad, pad * 3)
                            tv.customSelectionActionModeCallback = object : ActionMode.Callback {
                                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean { menu?.add(0, 56001, 0, "Read aloud"); return true }
                                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                                    if (item?.itemId != 56001) return false
                                    val s = selectedText()
                                    if (s != null && tts != null) tts.speak(s, lastVoice, lastStyle)
                                    mode?.finish()
                                    return true
                                }
                                override fun onDestroyActionMode(mode: ActionMode?) {}
                            }
                            holder.tv = tv
                            ScrollView(c).apply { addView(tv) }
                        },
                        update = {
                            val tv = holder.tv
                            if (tv != null) {
                                if (tv.text !== text) tv.text = text
                                tv.textSize = fontSp
                            }
                        }
                    )
                    kind == 3 -> SheetsView(sheets)
                    else -> SlidesView(slides)
                }
            }
            if (ttsOpen && tts != null) {
                TtsSheet(tts, selection = selOnly != null, onPick = { v, s ->
                    lastVoice = v; lastStyle = s
                    val t = selOnly ?: text.toString()
                    tts.speak(t, v, s)
                }, onClose = { ttsOpen = false })
            } else if (tts != null && (tts.speaking || tts.paused)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(RoundedCornerShape(50)).background(DocInk).padding(horizontal = 6.dp)
                ) {
                    Text(if (tts.paused) "\u25B6" else "\u275A\u275A", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { if (tts.paused) tts.resume() else tts.pause() }.padding(12.dp))
                    Text(if (tts.paused) "Paused" else "Reading\u2026", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { ttsOpen = true }.padding(vertical = 12.dp))
                    Text("\u25A0", color = Color(0xFFFF8A80), fontSize = 16.sp, modifier = Modifier.clickable { tts.stop() }.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Unsupported(d: DocFile, msg: String) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(kindColor(DocKind.of(d.ext))), contentAlignment = Alignment.Center) {
            Text(d.ext.uppercase().take(4), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("Can't show this file here", color = DocInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(msg, color = DocSub, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { DocFiles.open(ctx, d) }, colors = ButtonDefaults.buttonColors(containerColor = DocRed), shape = RoundedCornerShape(24.dp)) {
            Text("Open with another app", fontWeight = FontWeight.Bold)
        }
    }
}

private fun colName(i: Int): String {
    var n = i + 1
    val sb = StringBuilder()
    while (n > 0) { val r = (n - 1) % 26; sb.insert(0, ('A' + r)); n = (n - 1) / 26 }
    return sb.toString()
}

@Composable
private fun SheetsView(sheets: List<DocSheet>) {
    var cur by remember { mutableIntStateOf(0) }
    val sh = sheets.getOrNull(cur)
    Column(Modifier.fillMaxSize()) {
        if (sh == null || sh.rows.all { it.isEmpty() }) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("This sheet is empty.", color = DocSub) }
        } else {
            val cols = (sh.rows.maxOf { it.size }).coerceIn(1, DocParse.MAX_COLS)
            val cw = 112.dp
            Box(Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState())) {
                LazyColumn(Modifier.width(44.dp + cw * cols).fillMaxHeight().background(Color.White)) {
                    item {
                        Row(Modifier.background(Color(0xFFE8F5E9))) {
                            Box(Modifier.width(44.dp).border(0.5.dp, Color(0xFFD5D5D5)).padding(6.dp)) { Text("", fontSize = 12.sp) }
                            for (c in 0 until cols) Box(Modifier.width(cw).border(0.5.dp, Color(0xFFD5D5D5)).padding(6.dp), contentAlignment = Alignment.Center) {
                                Text(colName(c), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                    items(sh.rows.size) { r ->
                        val row = sh.rows[r]
                        Row(Modifier.heightIn(min = 30.dp)) {
                            Box(Modifier.width(44.dp).background(Color(0xFFF3F3F3)).border(0.5.dp, Color(0xFFD5D5D5)).padding(6.dp), contentAlignment = Alignment.Center) {
                                Text((r + 1).toString(), fontSize = 11.sp, color = DocSub)
                            }
                            for (c in 0 until cols) Box(Modifier.width(cw).border(0.5.dp, Color(0xFFE2E2E2)).padding(6.dp)) {
                                Text(row.getOrElse(c) { "" }, fontSize = 13.sp, color = DocInk, maxLines = 3, overflow = TextOverflow.Ellipsis, fontWeight = if (r == 0) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
        if (sheets.size > 1) {
            Row(Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sheets.forEachIndexed { i, s ->
                    val on = i == cur
                    Text(
                        s.name, fontSize = 13.sp, color = if (on) Color.White else DocInk, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) Color(0xFF43A047) else Color(0xFFF0F0F0)).clickable { cur = i }.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlidesView(slides: List<DocSlide>) {
    if (slides.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No slides found.", color = DocSub) }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(slides, key = { it.number }) { s ->
            Column(
                Modifier.fillMaxWidth().heightIn(min = 170.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(18.dp)).padding(16.dp)
            ) {
                Text(
                    "Slide " + s.number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFB8C00)).padding(horizontal = 10.dp, vertical = 3.dp)
                )
                Spacer(Modifier.height(10.dp))
                if (s.lines.isEmpty()) Text("(no text on this slide)", color = DocSub, fontSize = 13.sp)
                s.lines.forEachIndexed { i, l ->
                    if (i == 0) Text(l, color = DocInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    else Text("\u2022  " + l, color = Color(0xFF424242), fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
