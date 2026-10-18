package com.pdfcraft.studio.ui.viewer

import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.pdf.PdfLinkExtractor
import com.pdfcraft.studio.ui.common.DocumentScrollBar
import com.pdfcraft.studio.ui.theme.BrandPageSurface
import com.pdfcraft.studio.core.pdf.PdfLinkRegion
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.distinctUntilChanged

private class PdfSession(
    val pfd: ParcelFileDescriptor,
    val renderer: PdfRenderer,
    val pageCount: Int,
    private val exec: java.util.concurrent.ExecutorService
) {
    @kotlin.jvm.Volatile var closed: Boolean = false
    private val closeLock = Any()

    private fun <T> onThread(timeoutSec: Long = 25L, block: () -> T?): T? {
        if (closed) return null
        val fut = try {
            exec.submit<T?> {
                if (closed) return@submit null
                try {
                    block()
                } catch (_: Throwable) {
                    null
                }
            }
        } catch (_: Throwable) {
            return null
        }
        return try {
            fut.get(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Throwable) {
            try { fut.cancel(true) } catch (_: Throwable) {}
            null
        }
    }

    fun readHeights(): List<Float>? = onThread(120L) {
        val heights = ArrayList<Float>(renderer.pageCount)
        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { heights.add(it.height.toFloat()) }
        }
        heights
    }

    fun readWidths(): List<Float>? = onThread(120L) {
        val widths = ArrayList<Float>(renderer.pageCount)
        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { widths.add(it.width.toFloat()) }
        }
        widths
    }

    fun renderPage(pageIndex: Int, targetWidthPx: Int): android.graphics.Bitmap? = onThread {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@onThread null
        renderer.openPage(pageIndex).use { page ->
            val scale = targetWidthPx.toFloat() / page.width.toFloat().coerceAtLeast(1f)
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val b = android.graphics.Bitmap.createBitmap(targetWidthPx, h, android.graphics.Bitmap.Config.ARGB_8888)
            b.eraseColor(android.graphics.Color.WHITE)
            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            b
        }
    }

    fun closeSafely() {
        synchronized(closeLock) {
            if (closed) return
            closed = true
        }
        val task = Runnable {
            try { renderer.close() } catch (_: Throwable) {}
            try { pfd.close() } catch (_: Throwable) {}
            try { exec.shutdown() } catch (_: Throwable) {}
        }
        try {
            exec.execute(task)
        } catch (_: Throwable) {
            Thread(task).start()
        }
    }

    companion object {
        fun start(pfd: ParcelFileDescriptor): PdfSession? {
            val exec = java.util.concurrent.Executors.newSingleThreadExecutor()
            return try {
                val opened = exec.submit<Pair<PdfRenderer, Int>> {
                    val renderer = PdfRenderer(pfd)
                    renderer to renderer.pageCount
                }.get(20, java.util.concurrent.TimeUnit.SECONDS)
                PdfSession(pfd, opened.first, opened.second, exec)
            } catch (_: Throwable) {
                try { pfd.close() } catch (_: Throwable) {}
                try { exec.shutdownNow() } catch (_: Throwable) {}
                null
            }
        }
    }
}

/** Process-wide page bitmaps. Not cleared when a LazyColumn item leaves the viewport. */
private object PdfPageBitmapCache {
    private const val MAX_BYTES = 48 * 1024 * 1024
    private val lock = Any()
    private val inflight = HashSet<String>()
    private val lru = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // B19: no recycle here, bitmap may still be on screen. GC frees it.
        }
    }

    fun key(docId: String, page: Int, width: Int): String = docId + "#" + page + "#" + width

    fun get(key: String): Bitmap? = synchronized(lock) {
        lru.get(key)?.takeIf { !it.isRecycled }
    }

    fun put(key: String, bmp: Bitmap) {
        synchronized(lock) { lru.put(key, bmp) }
    }

    fun tryBegin(key: String): Boolean = synchronized(lock) {
        if (get(key) != null) return false
        inflight.add(key)
    }

    fun end(key: String) {
        synchronized(lock) { inflight.remove(key) }
    }

    fun evictOtherDocs(keepDocId: String) {
        synchronized(lock) {
            val prefix = keepDocId + "#"
            lru.snapshot().keys.filter { !it.startsWith(prefix) }.forEach { lru.remove(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUriString: String,
    onBackClick: () -> Unit,
    initialIndex: Int = 0,
    initialOffset: Int = 0,
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val uri = remember(pdfUriString) { Uri.parse(pdfUriString) }
    val fileName = remember(pdfUriString) { queryDisplayName(context, uri) }

    var session by remember { mutableStateOf<PdfSession?>(null) }
    val sessionHolder = remember { arrayOfNulls<PdfSession>(1) }
    val backOnce = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    BackHandler {
        if (backOnce.compareAndSet(false, true)) {
            try {
                val s = sessionHolder[0]
                sessionHolder[0] = null
                session = null
                s?.closeSafely()
            } catch (_: Throwable) {}
            try { onBackClick() } catch (_: Throwable) {}
        }
    }
    var pageCount by remember { mutableStateOf(0) }
    var pageHeights by remember { mutableStateOf<List<Float>>(emptyList()) }
    var linkRegions by remember { mutableStateOf<List<PdfLinkRegion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfUriString) {
        PdfPageBitmapCache.evictOtherDocs(pdfUriString)
        isLoading = true
        errorMessage = null
        pageCount = 0
        linkRegions = emptyList()
        session = null
        val failed = context.getString(R.string.open_pdf_failed)
        val opened: Result<Pair<PdfSession, List<Float>>> = withContext(Dispatchers.IO) {
                var openedSession: PdfSession? = null
                try {
                    val pfd = openPdfDescriptor(context, uri)
                        ?: return@withContext Result.failure(IllegalStateException("pfd"))
                    val started = PdfSession.start(pfd)
                        ?: return@withContext Result.failure(IllegalStateException("renderer"))
                    openedSession = started
                    val heights = started.readHeights()
                    if (heights == null || heights.isEmpty()) {
                        started.closeSafely()
                        return@withContext Result.failure(IllegalStateException("empty"))
                    }
                    Result.success(openedSession to heights)
                } catch (e: Exception) {
                    try { openedSession?.closeSafely() } catch (_: Throwable) {}
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Result.failure(e)
                }
        }
        if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) {
            opened.getOrNull()?.first?.closeSafely()
            return@LaunchedEffect
        }
        opened.fold(
            onSuccess = { pair ->
                sessionHolder[0] = pair.first
                session = pair.first
                pageHeights = pair.second
                pageCount = pair.first.pageCount
                isLoading = false
                linkRegions = try {
                    withContext(Dispatchers.IO) {
                        PdfLinkExtractor.extract(context, uri, pair.second, pair.first.readWidths().orEmpty())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfViewerScreen", "Link extract failed", e)
                    emptyList()
                }
            },
            onFailure = { e ->
                android.util.Log.e("PdfViewerScreen", "Failed to open PDF", e)
                errorMessage = failed
                isLoading = false
            }
        )
    }

    DisposableEffect(pdfUriString) {
        onDispose {
            val s = sessionHolder[0]
            sessionHolder[0] = null
            session = null
            try { s?.closeSafely() } catch (_: Throwable) {}
        }
    }

    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            if (backOnce.compareAndSet(false, true)) {
                try { onBackClick() } catch (_: Throwable) {}
            }
        }
    }

    val targetWidthPx = with(density) { 720.dp.roundToPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> { }
                pageCount <= 0 -> {
                    Text(
                        text = stringResource(R.string.open_pdf_empty),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val current = session
                    val viewerListState = rememberLazyListState(initialIndex, initialOffset)
                    LaunchedEffect(viewerListState) {
                        snapshotFlow { viewerListState.firstVisibleItemIndex to viewerListState.firstVisibleItemScrollOffset }
                            .distinctUntilChanged()
                            .collect { onScrollChanged(it.first, it.second) }
                    }
                    val scaleState = remember { mutableStateOf(1f) }
                    val panState = remember { mutableStateOf(Offset.Zero) }
                    val zoomLayer = Modifier.graphicsLayer {
                        val s = scaleState.value
                        val p = panState.value
                        scaleX = s
                        scaleY = s
                        translationX = p.x
                        translationY = p.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    val pinchMod = Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var lastCx = Float.NaN
                            var lastCy = Float.NaN
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } >= 2) {
                                    val z = event.calculateZoom()
                                    val c = event.calculateCentroid()
                                    if (lastCx.isNaN()) {
                                        lastCx = c.x
                                        lastCy = c.y
                                    }
                                    val pan = Offset(c.x - lastCx, c.y - lastCy)
                                    lastCx = c.x
                                    lastCy = c.y
                                    val oldS = scaleState.value
                                    val newS = (oldS * z).coerceIn(1f, 4f)
                                    val r = newS / oldS.coerceAtLeast(0.01f)
                                    val oldP = panState.value
                                    val nx = c.x - (c.x - oldP.x) * r + pan.x
                                    val ny = c.y - (c.y - oldP.y) * r + pan.y
                                    val vw = size.width.toFloat().coerceAtLeast(1f)
                                    val vh = size.height.toFloat().coerceAtLeast(1f)
                                    scaleState.value = newS
                                    panState.value = Offset(
                                        nx.coerceIn(vw * (1f - newS), 0f),
                                        ny.coerceIn(vh * (1f - newS), 0f)
                                    )
                                    event.changes.forEach { it.consume() }
                                } else {
                                    lastCx = Float.NaN
                                    lastCy = Float.NaN
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    Box(Modifier.fillMaxSize().background(BrandPageSurface).clipToBounds().then(pinchMod)) {
                    LazyColumn(
                        state = viewerListState,
                        modifier = Modifier.fillMaxSize().then(zoomLayer),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(pageCount, key = { it }) { pageIndex ->
                            PdfPageLazy(
                                docId = pdfUriString,
                                session = current,
                                pageIndex = pageIndex,
                                targetWidthPx = targetWidthPx,
                                regions = linkRegions.filter { it.pageIndex == pageIndex },
                                onLinkClick = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        android.util.Log.e("PdfViewerScreen", "Open link failed", e)
                                    }
                                }
                            )
                        }
                    }
                    DocumentScrollBar(
                        listState = viewerListState,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageLazy(
    docId: String,
    session: PdfSession?,
    pageIndex: Int,
    targetWidthPx: Int,
    regions: List<PdfLinkRegion>,
    onLinkClick: (String) -> Unit
) {
    val cacheKey = PdfPageBitmapCache.key(docId, pageIndex, targetWidthPx)
    var bitmap by remember(cacheKey) {
        mutableStateOf(PdfPageBitmapCache.get(cacheKey))
    }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(cacheKey, session) {
        PdfPageBitmapCache.get(cacheKey)?.let { cached ->
            bitmap = cached
            return@LaunchedEffect
        }
        val s = session ?: return@LaunchedEffect
        val started = PdfPageBitmapCache.tryBegin(cacheKey)
        if (!started) {
            repeat(80) {
                delay(50)
                PdfPageBitmapCache.get(cacheKey)?.let {
                    bitmap = it
                    return@LaunchedEffect
                }
            }
            PdfPageBitmapCache.get(cacheKey)?.let { bitmap = it }
            return@LaunchedEffect
        }
        try {
            PdfPageBitmapCache.get(cacheKey)?.let {
                bitmap = it
                return@LaunchedEffect
            }
            val bmp = withContext(Dispatchers.IO) {
                if (s.closed) null else s.renderPage(pageIndex, targetWidthPx)
            }
            if (bmp != null) {
                PdfPageBitmapCache.put(cacheKey, bmp)
                bitmap = bmp
            }
        } finally {
            PdfPageBitmapCache.end(cacheKey)
        }
    }

    val bmp = bitmap
    if (bmp == null || bmp.isRecycled) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, androidx.compose.ui.graphics.Color(0x33000000), RoundedCornerShape(4.dp))
                .onSizeChanged { sizePx = it }
                .pointerInput(regions) {
                    detectTapGestures { offset ->
                        if (sizePx.width <= 0 || sizePx.height <= 0) return@detectTapGestures
                        val xFraction = offset.x / sizePx.width
                        val yFraction = offset.y / sizePx.height
                        val hit = regions.firstOrNull { region ->
                            xFraction in region.leftFraction..region.rightFraction &&
                                yFraction in region.topFraction..region.bottomFraction
                        }
                        if (hit != null) onLinkClick(hit.url)
                    }
                }
        )
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val n = c.getString(idx)
                        if (!n.isNullOrBlank()) return n
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
    val last = uri.lastPathSegment
    if (!last.isNullOrBlank()) return Uri.decode(last).substringAfterLast('/')
    return "PDF"
}

private fun openPdfDescriptor(
    context: android.content.Context,
    uri: Uri
): ParcelFileDescriptor? {
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.let { return it }
    } catch (e: Exception) {
        android.util.Log.w("PdfViewerScreen", "openFileDescriptor failed for $uri", e)
    }
    if (uri.scheme == "file") {
        val path = uri.path
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists()) {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }
    }
    return null
}
