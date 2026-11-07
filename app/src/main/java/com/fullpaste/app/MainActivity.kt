package com.fullpaste.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Page() }
    }
}

private fun readClip(ctx: android.content.Context): String? {
    val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0).coerceToText(ctx)?.toString().orEmpty()
    return text.ifBlank { null }
}

private fun saveDownloads(ctx: android.content.Context, text: String): String {
    val name = "FullPaste-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
    if (Build.VERSION.SDK_INT >= 29) {
        val values = ContentValues()
        values.put(MediaStore.Downloads.DISPLAY_NAME, name)
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        values.put(MediaStore.Downloads.IS_PENDING, 1)
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the text file")
        ctx.contentResolver.openOutputStream(uri).use { it?.write(text.toByteArray(Charsets.UTF_8)) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        ctx.contentResolver.update(uri, values, null, null)
        return name
    }
    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!dir.exists()) dir.mkdirs()
    File(dir, name).writeText(text)
    return name
}

private fun shareFile(ctx: android.content.Context, text: String) {
    val dir = File(ctx.cacheDir, "paste")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "FullPaste.txt")
    file.writeText(text)
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND)
    send.type = "text/plain"
    send.putExtra(Intent.EXTRA_STREAM, uri)
    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri("FullPaste.txt", uri)
    ctx.startActivity(Intent.createChooser(send, "Save text file"))
}

@Composable
private fun Page() {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Copy the answer, then tap Paste full text.") }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0F5C56))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF3EFE6)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Full Paste", style = MaterialTheme.typography.headlineMedium)
                Text(note)
                Text(text.length.toString() + " characters")
                Button(onClick = {
                    val full = readClip(ctx)
                    if (full == null) note = "Clipboard is empty. Copy the answer again."
                    else { text = full; note = "Full text kept. Now save the text file." }
                }, modifier = Modifier.fillMaxWidth()) { Text("Paste full text") }
                AndroidView(
                    factory = { context ->
                        object : EditText(context) {
                            override fun onTextContextMenuItem(id: Int): Boolean {
                                if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
                                    val full = readClip(context)
                                    if (full != null) {
                                        val a = minOf(selectionStart, selectionEnd).coerceAtLeast(0)
                                        val b = maxOf(selectionStart, selectionEnd).coerceAtLeast(0)
                                        this.text.replace(a, b, full)
                                        return true
                                    }
                                }
                                return super.onTextContextMenuItem(id)
                            }
                        }.apply {
                            hint = "Full text stays here"
                            gravity = android.view.Gravity.TOP
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) { text = s?.toString().orEmpty() }
                            })
                        }
                    },
                    update = { edit ->
                        if (edit.text.toString() != text) {
                            edit.setText(text)
                            edit.setSelection(edit.text.length)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (text.isBlank()) { note = "Paste the text first."; return@Button }
                        if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(ctx as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 21)
                            note = "Allow storage, then tap Save text file again."
                            return@Button
                        }
                        try {
                            val saved = saveDownloads(ctx, text)
                            note = "Saved in Downloads: " + saved
                            Toast.makeText(ctx, note, Toast.LENGTH_LONG).show()
                        } catch (t: Throwable) { note = "Save failed: " + (t.message ?: "error") }
                    }, modifier = Modifier.weight(1f)) { Text("Save text file") }
                    OutlinedButton(onClick = {
                        if (text.isBlank()) note = "Paste the text first." else shareFile(ctx, text)
                    }, modifier = Modifier.weight(1f)) { Text("Share file") }
                }
                    Button(onClick = {
                        ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
                        note = "Turn on Full Paste Keyboard, then choose it inside ChatGPT."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Enable keyboard") }
                    OutlinedButton(onClick = {
                        ctx.getSystemService(android.view.inputmethod.InputMethodManager::class.java)?.showInputMethodPicker()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Choose keyboard") }
            }
        }
    }
}
