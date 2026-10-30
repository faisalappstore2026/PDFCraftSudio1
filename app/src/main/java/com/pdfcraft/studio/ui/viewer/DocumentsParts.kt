package com.pdfcraft.studio.ui.viewer

// fix54: rows, empty text and the access sheet of DocumentsScreen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val DocRed = Color(0xFFE53935)
internal val DocInk = Color(0xFF212121)
internal val DocSub = Color(0xFF757575)
internal val DocStar = Color(0xFFFFB300)

internal fun kindColor(k: DocKind): Color = when (k) {
    DocKind.PDF -> Color(0xFFE53935)
    DocKind.WORD -> Color(0xFF1E88E5)
    DocKind.EXCEL -> Color(0xFF43A047)
    DocKind.PPT -> Color(0xFFFB8C00)
    DocKind.TEXT -> Color(0xFF607D8B)
    DocKind.ALL -> Color(0xFF8E24AA)
}

@Composable
internal fun DetailLine(k: String, v: String) {
    Column {
        Text(k, fontSize = 12.sp, color = DocSub)
        Text(v, fontSize = 14.sp, color = DocInk)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DocRow(
    d: DocFile, fav: Boolean, selecting: Boolean, checked: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, onStar: () -> Unit, onAction: (Int) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val k = DocKind.of(d.ext)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(if (checked) Color(0xFFFFEBEE) else Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 2.dp, top = 10.dp, bottom = 10.dp)
    ) {
        if (selecting) Checkbox(checked = checked, onCheckedChange = { onClick() })
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(kindColor(k))
        ) {
            Text(d.ext.uppercase().take(4), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, color = DocInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(DocFiles.sizeText(d.size) + "  \u2022  " + DocFiles.dateText(d.modified), color = DocSub, fontSize = 12.sp, maxLines = 1)
        }
        if (!selecting) {
            IconButton(onClick = onStar) {
                Icon(Icons.Filled.Star, contentDescription = "Favourite", tint = if (fav) DocStar else Color(0xFFCFCFCF))
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = DocSub) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("Open", "Share", "Rename", if (fav) "Remove favourite" else "Favorite", "Print", "Details", "Move", "Delete")
                        .forEachIndexed { i, n ->
                            DropdownMenuItem(
                                text = { Text(n, color = if (i == 7) DocRed else DocInk) },
                                onClick = { menu = false; onAction(i) }
                            )
                        }
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

@Composable
internal fun DocEmpty(text: String, button: String?, onButton: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(text, color = DocSub, fontSize = 15.sp)
        if (button != null && onButton != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onButton, colors = ButtonDefaults.buttonColors(containerColor = DocRed)) { Text(button) }
        }
    }
}

/** Bottom sheet (up to half the screen) asking for All files access. */
@Composable
internal fun AccessSheet(onEnable: () -> Unit, onClose: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0x80000000)).clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.5f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White)
                .pointerInput(Unit) { detectTapGestures { } }
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Box(Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color(0xFFDDDDDD)))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFEBEE)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = DocRed, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Allow access to your documents", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = DocInk)
            Spacer(Modifier.height(10.dp))
            Text(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    "To show your PDF, Word, Excel, PPT and Text files, PDFCraft Studio needs All files access.\n\n" +
                        "1. Tap Enable.\n2. Turn on \"Allow access to manage all files\".\n3. The app opens again by itself and shows your files."
                else "To show your PDF, Word, Excel, PPT and Text files, PDFCraft Studio needs permission to read your storage. Tap Enable and then Allow.",
                fontSize = 14.sp, color = DocSub, lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onEnable, colors = ButtonDefaults.buttonColors(containerColor = DocRed),
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)
            ) { Text("Enable", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            TextButton(onClick = onClose) { Text("Not now", color = DocSub) }
        }
    }
}
