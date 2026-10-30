package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import kotlin.math.roundToInt

/** fix52: size (sp) of the editor title text, prefs "editor_title_size". */
internal object EditorTitleSize {
    const val MIN = 12f
    const val MAX = 28f
    private var loaded = false
    var size by mutableStateOf(18f)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        size = context.applicationContext.getSharedPreferences("editor_title_size", Context.MODE_PRIVATE)
            .getFloat("sp", 18f).coerceIn(MIN, MAX)
    }

    fun set(context: Context, v: Float) {
        size = v.coerceIn(MIN, MAX)
        context.applicationContext.getSharedPreferences("editor_title_size", Context.MODE_PRIVATE)
            .edit().putFloat("sp", size).apply()
    }
}

@Composable
internal fun editorTitleSp(): Float {
    EditorTitleSize.ensureLoaded(LocalContext.current)
    return EditorTitleSize.size
}

/** fix52: slider window (three dots > Title Size), value shown. */
@Composable
internal fun EditorTitleSizeDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    EditorTitleSize.ensureLoaded(ctx)
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.title_size), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF212121))
                Spacer(Modifier.weight(1f))
                Text(EditorTitleSize.size.roundToInt().toString() + " sp", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            }
            Text(stringResource(R.string.editor_title), fontSize = EditorTitleSize.size.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121), maxLines = 1, modifier = Modifier.padding(vertical = 8.dp))
            Slider(
                value = EditorTitleSize.size,
                onValueChange = { EditorTitleSize.set(ctx, it.roundToInt().toFloat()) },
                valueRange = EditorTitleSize.MIN..EditorTitleSize.MAX,
                steps = 15
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = { EditorTitleSize.set(ctx, 18f) }) { Text("18 sp") }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
