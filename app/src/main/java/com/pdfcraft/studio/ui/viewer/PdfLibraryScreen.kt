package com.pdfcraft.studio.ui.viewer

import android.Manifest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.rememberLazyListState
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.pdf.PdfLibraryStore
import com.pdfcraft.studio.ui.common.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab { All, Folders, Recent }
private val Accent = Color(0xFF1976D2)

class PdfLibraryVm : ViewModel() {
    var allPdfs by mutableStateOf(emptyList<PdfLibraryStore.PdfItem>())
    var recent by mutableStateOf(emptyList<PdfLibraryStore.PdfItem>())
    var tab by mutableStateOf(LibraryTab.All)
    var query by mutableStateOf("")
    var selectedFolder by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)
    var permissionDenied by mutableStateOf(false)
    var askedPermission by mutableStateOf(false)
    var sortMode by mutableStateOf("date_new")
    var selectMode by mutableStateOf(false)
    var selectedUris by mutableStateOf(setOf<String>())
    var showSortMenu by mutableStateOf(false)
    var showTopMenu by mutableStateOf(false)
    // Device scan bookkeeping: resume does not rescan the whole phone every time.
    var lastScanAt: Long = 0L
    var scanRunning: Boolean = false
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfLibraryScreen(
    onBackClick: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onEditPdf: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: PdfLibraryVm = viewModel()
    var tab by vm::tab
    var query by vm::query
    var loading by vm::loading
    var allPdfs by vm::allPdfs
    var recent by vm::recent
    var selectedFolder by vm::selectedFolder
    var permissionDenied by vm::permissionDenied
    var askedPermission by vm::askedPermission
    var sortMode by vm::sortMode
    var selectMode by vm::selectMode
    var selectedUris by vm::selectedUris
    var showSortMenu by vm::showSortMenu
    var showTopMenu by vm::showTopMenu
    var showMultiDelete by remember { mutableStateOf(false) }
    val pdfListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    var menuFor by remember { mutableStateOf<PdfLibraryStore.PdfItem?>(null) }
    var renameFor by remember { mutableStateOf<PdfLibraryStore.PdfItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteFor by remember { mutableStateOf<PdfLibraryStore.PdfItem?>(null) }
    var detailsFor by remember { mutableStateOf<PdfLibraryStore.PdfItem?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    fun hasStorageAccess(): Boolean {
        // API 33+: no all-files / READ for PDFs. MediaStore + SAF picker instead.
        return if (Build.VERSION.SDK_INT >= 33) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun pdfItemFromUri(uri: Uri): PdfLibraryStore.PdfItem {
        var name = "PDF"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni).orEmpty().ifBlank { name }
                    if (si >= 0) size = c.getLong(si).coerceAtLeast(0L)
                }
            }
        } catch (_: Exception) {
        }
        if (!name.endsWith(".pdf", true)) name = name + ".pdf"
        return PdfLibraryStore.PdfItem(
            uri = uri.toString(),
            name = name,
            sizeBytes = size,
            folder = "Picked",
            lastModifiedMillis = System.currentTimeMillis(),
            location = "Picked"
        )
    }

    fun reload(showSpinner: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        val skipScan = !showSpinner && allPdfs.isNotEmpty() &&
            (vm.scanRunning || now - vm.lastScanAt < 30_000L)
        scope.launch {
            if (skipScan) {
                // Recent list is cheap and may have changed (a PDF was just opened).
                recent = withContext(Dispatchers.IO) { PdfLibraryStore.listRecent(context) }
                return@launch
            }
            vm.scanRunning = true
            try {
                if (showSpinner || allPdfs.isEmpty()) loading = true
                val list = withContext(Dispatchers.IO) {
                    PdfLibraryStore.mergeWithPicked(
                        context,
                        PdfLibraryStore.scanDevicePdfs(context)
                    )
                }
                val rec = withContext(Dispatchers.IO) { PdfLibraryStore.listRecent(context) }
                allPdfs = list
                recent = rec
                vm.lastScanAt = android.os.SystemClock.elapsedRealtime()
            } finally {
                vm.scanRunning = false
                loading = false
            }
        }
    }

    // System back first leaves select mode / an open folder, like the top-bar arrow.
    androidx.activity.compose.BackHandler(
        enabled = selectMode || (tab == LibraryTab.Folders && selectedFolder != null)
    ) {
        if (selectMode) {
            selectMode = false
            selectedUris = emptySet()
        } else {
            selectedFolder = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) {
            permissionDenied = false
            reload(true)
        } else {
            loading = false
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        askedPermission = true
        val added = ArrayList<PdfLibraryStore.PdfItem>()
        for (uri in uris) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val item = pdfItemFromUri(uri)
            PdfLibraryStore.addRecent(context, item)
            PdfLibraryStore.addPicked(context, item)
            added.add(item)
        }
        if (added.isNotEmpty()) {
            val byUri = LinkedHashMap<String, PdfLibraryStore.PdfItem>()
            (added + allPdfs).forEach { byUri[it.uri] = it }
            allPdfs = byUri.values.toList()
            recent = PdfLibraryStore.listRecent(context)
            permissionDenied = false
            loading = false
            actionMessage = context.getString(R.string.pdf_library_added_count, added.size)
        }
    }

    fun openPdfPicker() {
        try {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        } catch (_: Exception) {
            try {
                pdfPickerLauncher.launch(arrayOf("*/*"))
            } catch (_: Exception) {
            }
        }
    }

    fun openSystemAccess() {
        askedPermission = true
        if (Build.VERSION.SDK_INT >= 33) {
            openPdfPicker()
            loading = false
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) {
        if (hasStorageAccess()) {
            permissionDenied = false
            reload()
        } else {
            loading = false
            permissionDenied = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasStorageAccess()) {
                    permissionDenied = false
                    reload(false)
                } else if (askedPermission) {
                    permissionDenied = true
                    loading = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val q = query.trim()
    fun matches(item: PdfLibraryStore.PdfItem): Boolean {
        return q.isEmpty() || item.name.contains(q, ignoreCase = true)
    }
    val visiblePdfs = PdfLibraryStore.sortPdfs(
        when (tab) {
            LibraryTab.All -> allPdfs.filter(::matches)
            LibraryTab.Recent -> recent.filter(::matches)
            LibraryTab.Folders -> {
                val folder = selectedFolder
                if (folder == null) emptyList()
                else allPdfs.filter { it.folder == folder }.filter(::matches)
            }
        },
        sortMode
    )
    val folders = allPdfs.groupBy { it.folder }.toList()
        .filter { pair ->
            q.isEmpty() ||
                pair.first.contains(q, ignoreCase = true) ||
                pair.second.any { it.name.contains(q, ignoreCase = true) }
        }
        .sortedBy { it.first.lowercase() }
    val emptyMsg = if (q.isNotEmpty()) {
        stringResource(R.string.pdf_library_no_match)
    } else {
        stringResource(R.string.pdf_library_empty)
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (tab == LibraryTab.Folders && selectedFolder != null) selectedFolder.orEmpty()
                        else stringResource(R.string.view_pdf),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (tab == LibraryTab.Folders && selectedFolder != null) {
                            selectedFolder = null
                        } else onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                actions = {
                    if (selectMode) {
                        IconButton(onClick = {
                            if (selectedUris.isNotEmpty()) showMultiDelete = true
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.pdf_library_delete), tint = Color.Black)
                        }
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.pdf_library_more), tint = Color.Black)
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pdf_library_add_pdfs)) },
                                onClick = {
                                    showTopMenu = false
                                    openPdfPicker()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pdf_library_select)) },
                                onClick = {
                                    showTopMenu = false
                                    selectMode = !selectMode
                                    if (!selectMode) selectedUris = emptySet()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pdf_library_sort)) },
                                onClick = {
                                    showTopMenu = false
                                    showSortMenu = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pdf_library_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabChip(stringResource(R.string.pdf_library_all), tab == LibraryTab.All) {
                    tab = LibraryTab.All
                    selectedFolder = null
                }
                TabChip(stringResource(R.string.pdf_library_folders), tab == LibraryTab.Folders) {
                    tab = LibraryTab.Folders
                }
                TabChip(stringResource(R.string.pdf_library_recent), tab == LibraryTab.Recent) {
                    tab = LibraryTab.Recent
                    selectedFolder = null
                }
            }
            when {
                loading -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Accent)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.pdf_library_finding), color = Color(0xFF8A8A8A))
                }
                permissionDenied && allPdfs.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(
                                if (Build.VERSION.SDK_INT >= 33) R.string.pdf_library_pick_hint
                                else R.string.pdf_library_access_needed
                            ),
                            color = Color(0xFF8A8A8A),
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { openSystemAccess() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                stringResource(
                                    if (Build.VERSION.SDK_INT >= 33) R.string.pdf_library_add_pdfs
                                    else R.string.pdf_library_allow_permission
                                ),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                tab == LibraryTab.Folders && selectedFolder == null && q.isEmpty() -> {
                    if (folders.isEmpty()) EmptyHint(emptyMsg)
                    else LazyColumn(Modifier.fillMaxSize(), state = folderListState) {
                        items(folders, key = { it.first }) { pair ->
                            FolderRow(pair.first, pair.second.size) { selectedFolder = pair.first }
                        }
                    }
                }
                (if (q.isNotEmpty()) allPdfs.filter(::matches) else visiblePdfs).isEmpty() -> EmptyHint(emptyMsg)
                else -> LazyColumn(Modifier.fillMaxSize(), state = pdfListState) {
                    val rows = PdfLibraryStore.sortPdfs(if (q.isNotEmpty()) allPdfs.filter(::matches) else visiblePdfs, sortMode)
                    items(rows, key = { it.uri }) { item ->
                        PdfRow(
                            item = item,
                            menuOpen = menuFor?.uri == item.uri,
                            onOpen = {
                                PdfLibraryStore.addRecent(context, item)
                                onOpenPdf(item.uri)
                            },
                            onEdit = { onEditPdf(item.uri) },
                            onMenu = { menuFor = if (menuFor?.uri == item.uri) null else item },
                            onDismissMenu = { menuFor = null },
                            onMenuOpen = {
                                menuFor = null
                                PdfLibraryStore.addRecent(context, item)
                                onOpenPdf(item.uri)
                            },
                            onMenuRename = {
                                menuFor = null
                                renameText = item.name.removeSuffix(".pdf").removeSuffix(".PDF")
                                renameFor = item
                            },
                            onMenuDelete = {
                                menuFor = null
                                deleteFor = item
                            },
                            onMenuShare = {
                                menuFor = null
                                try {
                                    val shareUri = Uri.parse(item.uri)
                                    val send = Intent(Intent.ACTION_SEND)
                                    send.type = "application/pdf"
                                    send.putExtra(Intent.EXTRA_STREAM, shareUri)
                                    send.clipData = android.content.ClipData.newRawUri(item.name, shareUri)
                                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(Intent.createChooser(send, item.name))
                                } catch (_: Exception) {
                                }
                            },
                            onMenuDetails = {
                                menuFor = null
                                detailsFor = item
                            },
                            selectMode = selectMode,
                            selected = item.uri in selectedUris,
                            onToggleSelect = {
                                selectedUris = if (item.uri in selectedUris) selectedUris - item.uri else selectedUris + item.uri
                            }
                        )
                    }
                }
            }
        }
    }

    if (actionMessage != null) {
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            confirmButton = {
                TextButton(onClick = { actionMessage = null }) { Text(stringResource(R.string.ok)) }
            },
            text = { Text(actionMessage ?: "") }
        )
    }
    renameFor?.let { target ->
        AlertDialog(
            onDismissRequest = { renameFor = null },
            title = { Text(stringResource(R.string.pdf_library_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val updated = withContext(Dispatchers.IO) {
                            PdfLibraryStore.renamePdf(context, target, renameText)
                        }
                        if (updated == null) {
                            actionMessage = context.getString(R.string.pdf_library_rename_failed)
                        } else {
                            allPdfs = allPdfs.map { if (it.uri == target.uri) updated else it }
                            recent = recent.map { if (it.uri == target.uri) updated else it }
                        }
                        renameFor = null
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { renameFor = null }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
    deleteFor?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.pdf_library_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            PdfLibraryStore.deletePdf(context, target)
                        }
                        if (ok) {
                            allPdfs = allPdfs.filterNot { it.uri == target.uri }
                            recent = recent.filterNot { it.uri == target.uri }
                            PdfLibraryStore.removeRecent(context, target.uri)
                            PdfLibraryStore.removePicked(context, target.uri)
                        } else {
                            actionMessage = context.getString(R.string.pdf_library_delete_failed)
                        }
                        deleteFor = null
                    }
                }) { Text(stringResource(R.string.pdf_library_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
    detailsFor?.let { item ->
        AlertDialog(
            onDismissRequest = { detailsFor = null },
            title = { Text(stringResource(R.string.pdf_library_details_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.pdf_library_details_name), fontWeight = FontWeight.SemiBold)
                    Text(item.name)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.pdf_library_details_size), fontWeight = FontWeight.SemiBold)
                    Text(PdfLibraryStore.formatSize(item.sizeBytes))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.pdf_library_details_location), fontWeight = FontWeight.SemiBold)
                    Text(item.location)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.pdf_library_details_modified), fontWeight = FontWeight.SemiBold)
                    Text(PdfLibraryStore.formatTime(item.lastModifiedMillis))
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsFor = null }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text(stringResource(R.string.pdf_library_sort_title)) },
            text = {
                Column {
                    val options = listOf(
                        "name_az" to R.string.pdf_library_sort_name_az,
                        "name_za" to R.string.pdf_library_sort_name_za,
                        "date_new" to R.string.pdf_library_sort_date_new,
                        "date_old" to R.string.pdf_library_sort_date_old,
                        "size_large" to R.string.pdf_library_sort_size_large,
                        "size_small" to R.string.pdf_library_sort_size_small
                    )
                    options.forEach { pair ->
                        val key = pair.first
                        val res = pair.second
                        Text(
                            text = (if (sortMode == key) "✓  " else "     ") + stringResource(res),
                            modifier = Modifier.fillMaxWidth().clickable {
                                sortMode = key
                                showSortMenu = false
                            }.padding(vertical = 10.dp),
                            color = Color.Black
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenu = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
    if (showMultiDelete) {
        AlertDialog(
            onDismissRequest = { showMultiDelete = false },
            title = { Text(stringResource(R.string.pdf_library_delete_selected)) },
            text = { Text(stringResource(R.string.pdf_library_delete_selected_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val targets = (allPdfs + recent).filter { it.uri in selectedUris }.distinctBy { it.uri }
                    var failed = false
                    targets.forEach { item ->
                        val ok = PdfLibraryStore.deletePdf(context, item)
                        if (ok) {
                            allPdfs = allPdfs.filterNot { it.uri == item.uri }
                            recent = recent.filterNot { it.uri == item.uri }
                            PdfLibraryStore.removeRecent(context, item.uri)
                        } else failed = true
                    }
                    selectedUris = emptySet()
                    selectMode = false
                    showMultiDelete = false
                    if (failed) actionMessage = context.getString(R.string.pdf_library_delete_failed)
                }) { Text(stringResource(R.string.pdf_library_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDelete = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Accent else Color(0xFF666666),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.height(2.dp).width(if (selected) 36.dp else 0.dp).background(Accent, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun PdfRow(
    item: PdfLibraryStore.PdfItem,
    menuOpen: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuRename: () -> Unit,
    onMenuDelete: () -> Unit,
    onMenuShare: () -> Unit,
    onMenuDetails: () -> Unit,
    selectMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { if (selectMode) onToggleSelect() else onOpen() }).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectMode) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        }
        Icon(AppIcons.FileDocument, null, tint = Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = Color.Black, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(PdfLibraryStore.formatSize(item.sizeBytes), color = Color(0xFF8A8A8A), fontSize = 13.sp)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.pdf_library_edit), tint = Color(0xFF444444))
        }
        Box {
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.pdf_library_more), tint = Color(0xFF444444))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(text = { Text(stringResource(R.string.pdf_library_open)) }, onClick = onMenuOpen)
                DropdownMenuItem(text = { Text(stringResource(R.string.pdf_library_rename)) }, onClick = onMenuRename)
                DropdownMenuItem(text = { Text(stringResource(R.string.pdf_library_delete)) }, onClick = onMenuDelete)
                DropdownMenuItem(text = { Text(stringResource(R.string.pdf_library_share)) }, onClick = onMenuShare)
                DropdownMenuItem(text = { Text(stringResource(R.string.pdf_library_details)) }, onClick = onMenuDetails)
            }
        }
    }
}

@Composable
private fun FolderRow(name: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Folder, null, tint = Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.Black, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                count.toString() + if (count == 1) " PDF" else " PDFs",
                color = Color(0xFF8A8A8A),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF8A8A8A), modifier = Modifier.padding(32.dp))
    }
}
