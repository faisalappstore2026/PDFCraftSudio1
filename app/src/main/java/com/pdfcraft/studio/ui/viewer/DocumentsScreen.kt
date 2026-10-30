package com.pdfcraft.studio.ui.viewer

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

private val SORT_NAMES = listOf("Name (A to Z)", "Name (Z to A)", "Date (oldest first)", "Date (newest first)", "Size (smallest first)", "Size (largest first)")

/** fix54: View PDF+Word+Excel+PPT+Text: all documents on the device in tabs. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(onBackClick: () -> Unit, onOpenPdf: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var access by remember { mutableStateOf(DocFiles.hasAccess(ctx)) }
    var sheet by remember { mutableStateOf(!access) }
    var loading by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf<List<DocFile>>(emptyList()) }
    var tab by remember { mutableStateOf(DocKind.PDF) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(DocFiles.sort(ctx)) }
    var favs by remember { mutableStateOf(DocFiles.favorites(ctx)) }
    var favOnly by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var topMenu by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<List<DocFile>?>(null) }
    var renameDoc by remember { mutableStateOf<DocFile?>(null) }
    var detailsDoc by remember { mutableStateOf<DocFile?>(null) }
    var moveDoc by remember { mutableStateOf<DocFile?>(null) }

    fun reload() {
        access = DocFiles.hasAccess(ctx)
        if (!access) return
        sheet = false
        loading = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { try { DocFiles.scan() } catch (_: Exception) { emptyList() } }
            all = r
            selected = selected.filter { p -> r.any { it.path == p } }.toSet()
            loading = false
        }
    }

    val readPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reload() }
    val enable: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DocFiles.findActivity(ctx)?.let { DocFiles.openAccessSettings(it) }
        } else readPerm.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    LaunchedEffect(Unit) { reload() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME && !access && DocFiles.hasAccess(ctx)) reload() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val shown = remember(all, tab, query, sort, favs, favOnly) {
        val q = query.trim().lowercase()
        DocFiles.sorted(all.filter { d ->
            tab.matches(d.ext) && (!favOnly || favs.contains(d.path)) && (q.isEmpty() || d.name.lowercase().contains(q))
        }, sort)
    }
    val selDocs = shown.filter { selected.contains(it.path) }

    fun toggleFav(d: DocFile) {
        favs = if (favs.contains(d.path)) favs - d.path else favs + d.path
        DocFiles.setFavorites(ctx, favs)
    }
    fun openDoc(d: DocFile) {
        if (d.ext == "pdf") onOpenPdf(Uri.fromFile(d.file).toString()) else DocFiles.open(ctx, d)
    }

    BackHandler(enabled = selecting || searching || favOnly || sheet) {
        when {
            selecting -> { selecting = false; selected = emptySet() }
            searching -> { searching = false; query = "" }
            sheet -> { sheet = false }
            else -> { favOnly = false }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF7F7F9),
        topBar = {
            Column(Modifier.background(Color.White)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                selecting -> { selecting = false; selected = emptySet() }
                                searching -> { searching = false; query = "" }
                                else -> onBackClick()
                            }
                        }) { Icon(if (selecting || searching) Icons.Filled.Close else Icons.Filled.ArrowBack, contentDescription = "Back", tint = DocInk) }
                    },
                    title = {
                        if (searching) {
                            TextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                placeholder = { Text("Search files") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = DocRed
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                when {
                                    selecting -> selDocs.size.toString() + " selected"
                                    favOnly -> "Favourite " + tab.title
                                    else -> tab.title
                                },
                                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DocInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (selecting) {
                            IconButton(onClick = { DocFiles.share(ctx, selDocs) }, enabled = selDocs.isNotEmpty()) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = if (selDocs.isNotEmpty()) DocInk else DocSub)
                            }
                            IconButton(onClick = { confirmDelete = selDocs }, enabled = selDocs.isNotEmpty()) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = if (selDocs.isNotEmpty()) DocRed else DocSub)
                            }
                        } else {
                            if (!searching) IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, contentDescription = "Search", tint = DocInk) }
                            Box {
                                IconButton(onClick = { topMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = DocInk) }
                                DropdownMenu(expanded = topMenu, onDismissRequest = { topMenu = false }) {
                                    DropdownMenuItem(text = { Text("Select") }, onClick = { topMenu = false; selecting = true; selected = emptySet() })
                                    DropdownMenuItem(text = { Text("Select All") }, onClick = { topMenu = false; selecting = true; selected = shown.map { it.path }.toSet() })
                                    DropdownMenuItem(text = { Text("Sort by") }, onClick = { topMenu = false; sortDialog = true })
                                    DropdownMenuItem(
                                        text = { Text(if (favOnly) "Show all files" else "Favourite") },
                                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = DocStar) },
                                        onClick = { topMenu = false; favOnly = !favOnly }
                                    )
                                }
                            }
                        }
                    }
                )
                ScrollableTabRow(selectedTabIndex = tab.ordinal, containerColor = Color.White, contentColor = DocRed, edgePadding = 8.dp) {
                    DocKind.values().forEach { k ->
                        Tab(
                            selected = tab == k, onClick = { tab = k },
                            text = { Text(k.label, fontWeight = if (tab == k) FontWeight.Bold else FontWeight.Normal) },
                            selectedContentColor = DocRed, unselectedContentColor = DocSub
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                if (selecting) {
                    val allOn = shown.isNotEmpty() && selDocs.size == shown.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(Color.White)
                            .clickable { selected = if (allOn) emptySet() else shown.map { it.path }.toSet() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Checkbox(checked = allOn, onCheckedChange = { selected = if (allOn) emptySet() else shown.map { it.path }.toSet() })
                        Text("Select all (" + shown.size + ")", color = DocInk, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                }
                when {
                    !access -> DocEmpty("Allow All files access to see your documents.", "Enable", enable)
                    loading && all.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DocRed) }
                    shown.isEmpty() -> DocEmpty(if (favOnly) "No favourite files yet. Tap the star of a file." else "No " + tab.title.lowercase() + " found.", null, null)
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(shown, key = { it.path }) { d ->
                            DocRow(
                                d = d, fav = favs.contains(d.path), selecting = selecting, checked = selected.contains(d.path),
                                onClick = {
                                    if (selecting) { selected = if (selected.contains(d.path)) selected - d.path else selected + d.path }
                                    else { openDoc(d) }
                                },
                                onLongClick = { selecting = true; selected = selected + d.path },
                                onStar = { toggleFav(d) },
                                onAction = { a ->
                                    when (a) {
                                        0 -> openDoc(d)
                                        1 -> DocFiles.share(ctx, listOf(d))
                                        2 -> { renameDoc = d }
                                        3 -> toggleFav(d)
                                        4 -> DocFiles.print(ctx, d)
                                        5 -> { detailsDoc = d }
                                        6 -> { moveDoc = d }
                                        else -> { confirmDelete = listOf(d) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (sheet && !access) AccessSheet(onEnable = enable, onClose = { sheet = false })
        }
    }

    if (sortDialog) AlertDialog(
        onDismissRequest = { sortDialog = false },
        title = { Text("Sort by") },
        text = {
            Column {
                SORT_NAMES.forEachIndexed { i, n ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { sort = i; DocFiles.setSort(ctx, i); sortDialog = false }.padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(if (sort == i) DocRed else Color(0xFFDDDDDD)))
                        Spacer(Modifier.width(12.dp))
                        Text(n, color = DocInk)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { sortDialog = false }) { Text("Close") } }
    )

    confirmDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (list.size == 1) "Delete file?" else "Delete " + list.size + " files?") },
            text = { Text(if (list.size == 1) list[0].name + " will be deleted from your device." else "The selected files will be deleted from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    DocFiles.delete(ctx, list)
                    confirmDelete = null
                    selecting = false
                    selected = emptySet()
                    reload()
                }) { Text("Delete", color = DocRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }

    renameDoc?.let { d ->
        var name by remember(d.path) { mutableStateOf(d.name.substringBeforeLast('.')) }
        AlertDialog(
            onDismissRequest = { renameDoc = null },
            title = { Text("Rename") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, suffix = { Text("." + d.ext) }) },
            confirmButton = {
                TextButton(onClick = {
                    val t = DocFiles.rename(ctx, d, name)
                    if (t != null) {
                        if (favs.contains(d.path)) { favs = favs - d.path + t.absolutePath; DocFiles.setFavorites(ctx, favs) }
                        renameDoc = null
                        reload()
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameDoc = null }) { Text("Cancel") } }
        )
    }

    detailsDoc?.let { d ->
        AlertDialog(
            onDismissRequest = { detailsDoc = null },
            title = { Text("Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Name", d.name)
                    DetailLine("Type", DocKind.of(d.ext).label + " (." + d.ext + ")")
                    DetailLine("Size", DocFiles.sizeText(d.size))
                    DetailLine("Modified", DocFiles.dateText(d.modified))
                    DetailLine("Path", d.path)
                }
            },
            confirmButton = { TextButton(onClick = { detailsDoc = null }) { Text("OK") } }
        )
    }

    moveDoc?.let { d ->
        AlertDialog(
            onDismissRequest = { moveDoc = null },
            title = { Text("Move to") },
            text = {
                Column {
                    DocFiles.moveTargets().forEach { (label, dir) ->
                        Text(
                            label, color = DocInk, fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                if (DocFiles.move(ctx, d, dir)) {
                                    if (favs.contains(d.path)) { favs = favs - d.path + java.io.File(dir, d.name).absolutePath; DocFiles.setFavorites(ctx, favs) }
                                    reload()
                                }
                                moveDoc = null
                            }.padding(vertical = 12.dp, horizontal = 6.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { moveDoc = null }) { Text("Cancel") } }
        )
    }
}

