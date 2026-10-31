package com.pdfcraft.studio.ui.viewer

// fix56: tab row with adjustable gap, Layout panel, new menu design, full-screen search page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** fix56: Documents layout prefs (tab gap in dp, file list size 0.6..1). */
internal object DocLayout {
    private const val P = "documents_layout"
    const val GAP_MIN = 0f
    const val GAP_MAX = 24f
    const val SCALE_MIN = 0.6f
    const val SCALE_MAX = 1f
    fun gap(c: Context): Float = c.getSharedPreferences(P, Context.MODE_PRIVATE).getFloat("gap", 6f).coerceIn(GAP_MIN, GAP_MAX)
    fun scale(c: Context): Float = c.getSharedPreferences(P, Context.MODE_PRIVATE).getFloat("scale", 1f).coerceIn(SCALE_MIN, SCALE_MAX)
    fun save(c: Context, gap: Float, scale: Float) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putFloat("gap", gap).putFloat("scale", scale).apply()
    }
}

/** fix56: compact pill tabs; [gap] = space between them in dp. */
@Composable
internal fun DocTabsRow(tab: DocKind, gap: Float, onTab: (DocKind) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        DocKind.values().forEach { k ->
            val on = tab == k
            Text(
                k.label,
                color = if (on) Color.White else DocInk,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (on) kindColor(k) else Color(0xFFF1F1F4))
                    .clickable { onTab(k) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFEDEDED))
}

@Composable
private fun MenuRow(icon: ImageVector, tint: Color, label: String, sub: String?, onClick: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
        },
        text = {
            Column {
                Text(label, color = DocInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (sub != null) Text(sub, color = DocSub, fontSize = 11.sp, maxLines = 1)
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

/** fix56: the redesigned ⋮ menu (rounded card, icons, groups). */
@Composable
internal fun DocTopMenu(
    expanded: Boolean, favOnly: Boolean, onDismiss: () -> Unit,
    onSelect: () -> Unit, onSelectAll: () -> Unit, onSort: () -> Unit, onFav: () -> Unit, onLayout: () -> Unit, onRefresh: () -> Unit
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(18.dp))) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(Color.White).width(250.dp)) {
            Text("Options", color = DocSub, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp))
            MenuRow(Icons.Filled.CheckCircle, Color(0xFF1E88E5), "Select", "Pick files one by one") { onDismiss(); onSelect() }
            MenuRow(Icons.Filled.Done, Color(0xFF43A047), "Select All", "All files in this tab") { onDismiss(); onSelectAll() }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFEEEEEE))
            MenuRow(Icons.Filled.List, Color(0xFF8E24AA), "Sort by", "Name, date or size") { onDismiss(); onSort() }
            MenuRow(Icons.Filled.Star, DocStar, if (favOnly) "Show all files" else "Favourite", if (favOnly) "Leave favourites" else "Only starred files") { onDismiss(); onFav() }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFEEEEEE))
            MenuRow(Icons.Filled.Settings, Color(0xFFFB8C00), "Layout", "Tab spacing and list size") { onDismiss(); onLayout() }
            MenuRow(Icons.Filled.Refresh, Color(0xFF00897B), "Refresh", "Scan the device again") { onDismiss(); onRefresh() }
        }
    }
}

@Composable
private fun LabeledSlider(title: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = DocInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(
            value, color = DocRed, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFEBEE)).padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
    Slider(
        value = v, onValueChange = onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = DocRed, activeTrackColor = DocRed, inactiveTrackColor = Color(0xFFF3D0D0))
    )
}

/** fix56: Layout panel at the bottom; the tabs and list stay visible above it while sliding. */
@Composable
internal fun DocLayoutPanel(
    gap: Float, scale: Float, onGap: (Float) -> Unit, onScale: (Float) -> Unit, onReset: () -> Unit, onClose: () -> Unit, modifier: Modifier
) {
    Column(
        modifier.fillMaxWidth().padding(10.dp).shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)).background(Color.White).navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Layout", color = DocInk, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onReset) { Text("Reset", color = DocSub) }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = DocInk) }
        }
        LabeledSlider("Tab spacing", gap.toInt().toString() + " dp", gap, DocLayout.GAP_MIN..DocLayout.GAP_MAX, onGap)
        Spacer(Modifier.height(4.dp))
        LabeledSlider("File list size", (scale * 100).toInt().toString() + " %", scale, DocLayout.SCALE_MIN..DocLayout.SCALE_MAX, onScale)
    }
}

private val SBg = Color(0xFF121212)
private val SField = Color(0xFF232323)
private val SText = Color(0xFFEDEDED)
private val SHint = Color(0xFF8A8A8A)

/** fix56: full-screen dark search page (keyboard opens, results of every file type while typing). */
@Composable
internal fun DocSearchPage(all: List<DocFile>, query: String, onQuery: (String) -> Unit, onBack: () -> Unit, onOpen: (DocFile) -> Unit) {
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val fr = remember { FocusRequester() }
        val kb = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) { try { fr.requestFocus(); kb?.show() } catch (e: Exception) { } }
        val q = query.trim().lowercase()
        val res = remember(all, q) {
            if (q.isEmpty()) emptyList() else all.filter { DocKind.ALL.matches(it.ext) && it.name.lowercase().contains(q) }.sortedBy { it.name.lowercase() }
        }
        Column(Modifier.fillMaxSize().background(SBg)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SText) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(SField).padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = SHint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search documents", color = SHint, fontSize = 16.sp)
                        BasicTextField(
                            value = query, onValueChange = onQuery, singleLine = true,
                            textStyle = TextStyle(color = SText, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color(0xFF8AB4F8)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { kb?.hide() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(fr)
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = SHint, modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQuery("") })
                    }
                }
            }
            if (q.isNotEmpty()) {
                Text(
                    if (res.isEmpty()) "No files match \"" + query.trim() + "\"" else res.size.toString() + (if (res.size == 1) " file found" else " files found"),
                    color = SHint, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(res, key = { it.path }) { d ->
                    val k = DocKind.of(d.ext)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { kb?.hide(); onOpen(d) }.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(kindColor(k)), contentAlignment = Alignment.Center) {
                            Text(d.ext.uppercase().take(4), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val name = d.name
                            val i = name.lowercase().indexOf(q)
                            Text(
                                buildAnnotatedString {
                                    if (i < 0 || i + q.length > name.length) append(name) else {
                                        append(name.substring(0, i))
                                        withStyle(SpanStyle(color = Color(0xFF8AB4F8), fontWeight = FontWeight.Bold)) { append(name.substring(i, i + q.length)) }
                                        append(name.substring(i + q.length))
                                    }
                                },
                                color = SText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(k.label + "  \u2022  " + DocFiles.sizeText(d.size) + "  \u2022  " + DocFiles.dateText(d.modified), color = SHint, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
