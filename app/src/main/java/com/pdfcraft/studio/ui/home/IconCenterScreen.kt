package com.pdfcraft.studio.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconCenterScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var copied by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // Off the main thread, downscaled, never throws.
            val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                decodeIconSafely(context, uri)
            }
            if (bmp != null) {
                bitmap = bmp
                offsetX = 0f
                offsetY = 0f
                scale = 1f
                copied = false
            }
        }
    }
    val report = buildIconReport(offsetX, offsetY, scale)
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Icon Center") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This circle matches the real round app icon. Scale 1.00 is the same size as the phone icon. Copy the numbers and send them in chat.",
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { picker.launch("image/*") }) {
                Text("Import image")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECECEC))
                    .border(2.dp, Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val shot = bitmap
                if (shot != null) {
                    Image(
                        bitmap = shot.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val launcherScale = 1.5f * scale
                                scaleX = launcherScale
                                scaleY = launcherScale
                                translationX = offsetX * size.width
                                translationY = offsetY * size.height
                            }
                    )
                } else {
                    Text("No image", color = Color(0xFF757575))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Left / Right   " + trimNum(offsetX),
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = offsetX,
                onValueChange = { value ->
                    offsetX = value
                    copied = false
                },
                valueRange = -0.50f..0.50f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Up / Down   " + trimNum(offsetY),
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = offsetY,
                onValueChange = { value ->
                    offsetY = value
                    copied = false
                },
                valueRange = -0.50f..0.50f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Zoom   " + trimNum(scale),
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = scale,
                onValueChange = { value ->
                    scale = value
                    copied = false
                },
                valueRange = 0.40f..3.00f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                clipboard.setText(AnnotatedString(report))
                copied = true
            }) {
                Text(if (copied) "Copied" else "Copy details")
            }
        }
    }
}

private fun decodeIconSafely(context: android.content.Context, uri: Uri): android.graphics.Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest > 0 && longest / (sample * 2) >= 1024) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Throwable) {
        android.util.Log.w("IconCenter", "Image decode failed", e)
        null
    }
}

private fun buildIconReport(offsetX: Float, offsetY: Float, scale: Float): String {
    val sideX = if (offsetX > 0.001f) "right" else if (offsetX < -0.001f) "left" else "center"
    val sideY = if (offsetY > 0.001f) "down" else if (offsetY < -0.001f) "up" else "center"
    return "ICON_FIT\noffsetX=" + trimNum(offsetX) + "\noffsetY=" + trimNum(offsetY) + "\nscale=" + trimNum(scale) + "\nhorizontal=" + sideX + "\nvertical=" + sideY + "\nmeaning=offsetX positive is right. offsetY positive is down. scale 1.00 matches the real round app icon."
}

private fun trimNum(v: Float): String {
    val n = kotlin.math.round(v * 100f) / 100f
    val neg = n < 0f
    val a = kotlin.math.abs(n)
    val whole = a.toInt()
    val frac = kotlin.math.round((a - whole) * 100f).toInt().coerceIn(0, 99)
    val body = whole.toString() + "." + frac.toString().padStart(2, '0')
    return if (neg) "-" + body else body
}
