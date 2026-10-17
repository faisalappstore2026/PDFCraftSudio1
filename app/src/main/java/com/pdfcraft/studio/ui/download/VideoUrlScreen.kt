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

private sealed class UrlUi {
    object Idle : UrlUi()
    object Checking : UrlUi()
    class Unavailable(val url: String, val message: String) : UrlUi()
    class Working(val done: Long, val total: Long) : UrlUi()
    class Saved(val name: String) : UrlUi()
    class Failed(val message: String) : UrlUi()
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
    val busy = ui is UrlUi.Checking || ui is UrlUi.Working

    fun openExternal(link: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        } catch (_: Throwable) {
            ui = UrlUi.Failed("Could not open this link.")
        }
    }

    fun start(raw: String) {
        if (job?.isActive == true) return
        val check = VideoUrls.validate(raw)
        if (check is com.pdfcraft.studio.core.download.UrlCheck.Bad) {
            ui = UrlUi.Failed(check.message)
            return
        }
        ui = UrlUi.Checking
        job = scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompliantVideoProvider.lookup(context.applicationContext, check)
            }
            if (!isActive) return@launch
            when (result) {
                is ProviderResult.Failed -> ui = UrlUi.Failed(result.message)
                is ProviderResult.Unavailable -> ui = UrlUi.Unavailable(result.url, result.message)
                is ProviderResult.Formats -> {
                    val format = result.formats.firstOrNull()
                    if (format == null) {
                        ui = UrlUi.Failed("No download format was returned.")
                    } else {
                        val id = try {
                            withContext(Dispatchers.IO) {
                                VideoSaver.enqueue(context.applicationContext, format)
                            }
                        } catch (_: Throwable) {
                            -1L
                        }
                        if (!isActive) {
                            if (id >= 0L) VideoSaver.remove(context.applicationContext, id)
                            return@launch
                        }
                        if (id < 0L) {
                            ui = UrlUi.Failed("Could not start the download.")
                        } else {
                            downloadId = id
                            ui = UrlUi.Working(0L, format.bytes)
                            watchDownload(context.applicationContext, id) { next ->
                                ui = next
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler {
        job?.cancel()
        if (downloadId >= 0L && ui is UrlUi.Working) {
            VideoSaver.remove(context, downloadId)
        }
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Videos Through URL") },
                navigationIcon = {
                    IconButton(onClick = {
                        job?.cancel()
                        if (downloadId >= 0L && ui is UrlUi.Working) VideoSaver.remove(context, downloadId)
                        onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
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
                text = "Paste an HTTPS link. YouTube, Facebook, and TikTok video links are checked. A direct video file can be saved. A valid link does not prove the video is available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Video URL") },
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
                        if (text.isBlank()) ui = UrlUi.Failed("Clipboard is empty.")
                        else url = text
                    }
                ) { Text("Paste") }
                TextButton(
                    enabled = !busy,
                    onClick = {
                        url = ""
                        ui = UrlUi.Idle
                        focus.clearFocus()
                    }
                ) { Text("Clear") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    focus.clearFocus()
                    start(url)
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("OK") }
            Spacer(modifier = Modifier.height(20.dp))
            when (val state = ui) {
                is UrlUi.Idle -> {}
                is UrlUi.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Text("Checking link...", modifier = Modifier.padding(start = 12.dp))
                    }
                }
                is UrlUi.Unavailable -> {
                    Text(state.message)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { openExternal(state.url) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open link")
                    }
                }
                is UrlUi.Working -> {
                    val total = state.total
                    if (total > 0L) {
                        LinearProgressIndicator(
                            progress = (state.done.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(state.done.toString() + " / " + total.toString() + " bytes", modifier = Modifier.padding(top = 8.dp))
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Downloading...", modifier = Modifier.padding(top = 8.dp))
                    }
                    TextButton(onClick = {
                        job?.cancel()
                        VideoSaver.remove(context, downloadId)
                        downloadId = -1L
                        ui = UrlUi.Failed("Download cancelled.")
                    }) { Text("Cancel") }
                }
                is UrlUi.Saved -> {
                    Text("Saved to Downloads / PDFCraftStudio")
                    Text(state.name, modifier = Modifier.padding(top = 6.dp))
                }
                is UrlUi.Failed -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { start(url) }) { Text("Retry") }
                }
            }
        }
    }
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
                onUpdate(UrlUi.Saved("Video file"))
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
}
