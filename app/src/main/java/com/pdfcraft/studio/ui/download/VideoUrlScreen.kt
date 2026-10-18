package com.pdfcraft.studio.ui.download

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pdfcraft.studio.core.download.CompliantVideoProvider
import com.pdfcraft.studio.core.download.ProviderResult
import com.pdfcraft.studio.core.download.VideoSaver
import com.pdfcraft.studio.core.download.VideoUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.DownloadManager
import androidx.compose.ui.res.stringResource
import com.pdfcraft.studio.R

private sealed class UrlUi {
    object Idle : UrlUi()
    object Checking : UrlUi()
    class Unavailable(val url: String, val message: String) : UrlUi()
    class Working(val done: Long, val total: Long) : UrlUi()
    class Saved(val name: String) : UrlUi()
    class Failed(val message: String) : UrlUi()
    class Ready(val formats: List<com.pdfcraft.studio.core.download.VideoFormat>) : UrlUi()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUrlScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var ui by remember { mutableStateOf<UrlUi>(UrlUi.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }
    var downloadId by remember { mutableStateOf(-1L) }
    var preview by remember { mutableStateOf("") }
    var previewMime by remember { mutableStateOf("") }
    val busy = ui is UrlUi.Checking || ui is UrlUi.Working
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    fun openExternal(link: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        } catch (_: Throwable) {
            ui = UrlUi.Failed(context.getString(R.string.video_url_err_open))
        }
    }

    fun start(raw: String) {
        if (job?.isActive == true) return
        val check = VideoUrls.validate(raw)
        if (check is com.pdfcraft.studio.core.download.UrlCheck.Bad) {
            ui = UrlUi.Failed(check.message)
            return
        }
        if (android.os.Build.VERSION.SDK_INT <= 28 &&
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ui = UrlUi.Failed(context.getString(R.string.video_url_err_permission))
            return
        }
        ui = UrlUi.Checking
        job = scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompliantVideoProvider.lookup(context.applicationContext, check)
            }
            if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) return@launch
            when (result) {
                is ProviderResult.Failed -> ui = UrlUi.Failed(result.message)
                is ProviderResult.Unavailable -> ui = UrlUi.Unavailable(result.url, result.message)
                is ProviderResult.Formats -> {
                    val list = result.formats
                    if (list.isEmpty()) {
                        ui = UrlUi.Failed(context.getString(R.string.video_url_err_format))
                    } else {
                        preview = list[0].downloadUrl
                        previewMime = list[0].mime
                        ui = UrlUi.Ready(list)
                    }
                }
            }
        }
    }

    fun save(format: com.pdfcraft.studio.core.download.VideoFormat) {
        if (job?.isActive == true) return
        ui = UrlUi.Checking
        job = scope.launch {
            val id = try {
                withContext(Dispatchers.IO) {
                    VideoSaver.enqueue(context.applicationContext, format)
                }
            } catch (_: Throwable) {
                -1L
            }
            if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) {
                if (id >= 0L) VideoSaver.remove(context.applicationContext, id)
                return@launch
            }
            if (id < 0L) {
                ui = UrlUi.Failed(context.getString(R.string.video_url_err_start))
            } else {
                downloadId = id
                ui = UrlUi.Working(0L, format.bytes)
                watchDownload(context.applicationContext, id) { next ->
                    ui = next
                }
            }
        }
    }

    BackHandler {
        job?.cancel()
        // Back keeps download A
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_videos_url)) },
                navigationIcon = {
                    IconButton(onClick = {
                        job?.cancel()
                        // Back keeps download B
                        onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.video_url_back))
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focus.clearFocus() }
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.video_url_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.video_url_label)) },
                minLines = 3,
                maxLines = 6,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    start(url)
                })
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData? = cm.primaryClip
                        val text = if (clip != null && clip.itemCount > 0) {
                            clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
                        } else ""
                        if (text.isBlank()) ui = UrlUi.Failed(context.getString(R.string.video_url_clipboard_empty))
                        else url = text
                    }
                ) { Text(stringResource(R.string.video_url_paste)) }
                TextButton(
                    enabled = !busy,
                    onClick = {
                        url = ""
                        preview = ""
                        previewMime = ""
                        ui = UrlUi.Idle
                        focus.clearFocus()
                    }
                ) { Text(stringResource(R.string.video_url_clear)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    focus.clearFocus()
                    start(url)
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.video_url_ok)) }
            Spacer(modifier = Modifier.height(20.dp))
            if (preview.isNotBlank() && previewMime.startsWith("video/")) {
                androidx.compose.runtime.key(preview) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.widget.VideoView(ctx).apply {
                                setVideoURI(Uri.parse(preview))
                                setOnPreparedListener { player -> player.start() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            when (val state = ui) {
                is UrlUi.Idle -> {}
                is UrlUi.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.video_url_checking), modifier = Modifier.padding(start = 12.dp))
                    }
                }
                is UrlUi.Unavailable -> {
                    Text(state.message)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { openExternal(state.url) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.video_url_open_link))
                    }
                }
                is UrlUi.Working -> {
                    val total = state.total
                    if (total > 0L) {
                        LinearProgressIndicator(
                            progress = (state.done.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val pct = ((state.done * 100L) / total).coerceIn(0L, 100L)
                        Text("Download " + pct.toString() + "%  " + state.done.toString() + " / " + total.toString() + " bytes", modifier = Modifier.padding(top = 8.dp))
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.video_url_downloading), modifier = Modifier.padding(top = 8.dp))
                    }
                    TextButton(onClick = {
                        job?.cancel()
                        VideoSaver.remove(context, downloadId)
                        downloadId = -1L
                        ui = UrlUi.Failed(context.getString(R.string.video_url_cancelled))
                    }) { Text(stringResource(R.string.video_url_cancel)) }
                }
                is UrlUi.Saved -> {
                    Text(stringResource(R.string.video_url_saved_to))
                    Text(state.name, modifier = Modifier.padding(top = 6.dp))
                }
                is UrlUi.Ready -> {
                    Text("Real format returned by the file. Tap one to save it.")
                    Spacer(modifier = Modifier.height(8.dp))
                    state.formats.forEach { format ->
                        Button(
                            onClick = { save(format) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) { Text(format.label) }
                    }
                }
                is UrlUi.Failed -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { start(url) }) { Text(stringResource(R.string.video_url_retry)) }
                }
            }
        }
    }
}

private fun savedFileName(localUri: String?, fallback: String): String {
    val n = try { Uri.parse(localUri ?: "").lastPathSegment } catch (_: Throwable) { null }
    return if (n.isNullOrBlank()) fallback else n
}

private suspend fun watchDownload(context: Context, id: Long, onUpdate: (UrlUi) -> Unit) {
    var spins = 0
    while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false && spins < 900) {
        val p = withContext(Dispatchers.IO) { VideoSaver.query(context, id) }
        if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) return
        if (p == null) {
            onUpdate(UrlUi.Failed("Download disappeared."))
            return
        }
        when (p.status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                onUpdate(UrlUi.Saved(savedFileName(p.localUri, context.getString(R.string.video_url_file_fallback))))
                return
            }
            DownloadManager.STATUS_FAILED -> {
                onUpdate(UrlUi.Failed("Download failed (" + p.reason + ")."))
                return
            }
            else -> onUpdate(UrlUi.Working(p.done, p.total))
        }
        delay(500)
        spins++
    }
    if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
        onUpdate(UrlUi.Failed("Taking too long. The video may still finish in your Downloads notification. Tap Retry only if it fails."))
    }
}
