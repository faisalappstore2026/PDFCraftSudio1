package com.pdfcraft.studio.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * fix43: small round floating button that can be dragged anywhere; its position
 * (0..1 of the free area) is saved in SharedPreferences "floating_buttons" under [key].
 */
@Composable
internal fun DraggableFab(
    key: String,
    defaultX: Float,
    defaultY: Float,
    color: Color,
    fg: Color,
    label: String,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current.applicationContext
    val prefs = remember { ctx.getSharedPreferences("floating_buttons", Context.MODE_PRIVATE) }
    var fx by remember { mutableStateOf(prefs.getFloat(key + "_x", defaultX).coerceIn(0f, 1f)) }
    var fy by remember { mutableStateOf(prefs.getFloat(key + "_y", defaultY).coerceIn(0f, 1f)) }
    fun savePos() {
        prefs.edit().putFloat(key + "_x", fx).putFloat(key + "_y", fy).apply()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSize = 50.dp
        val margin = 10.dp
        val marginPx = with(density) { margin.toPx() }
        val freeW = with(density) { (maxWidth - fabSize - margin * 2).toPx() }.coerceAtLeast(1f)
        val freeH = with(density) { (maxHeight - fabSize - margin * 2).toPx() }.coerceAtLeast(1f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset((marginPx + fx * freeW).roundToInt(), (marginPx + fy * freeH).roundToInt()) }
                .size(fabSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, fg.copy(alpha = 0.6f), CircleShape)
                .pointerInput(freeW, freeH) {
                    detectDragGestures(
                        onDragEnd = { savePos() },
                        onDragCancel = { savePos() }
                    ) { change, dragAmount ->
                        change.consume()
                        fx = (fx + dragAmount.x / freeW).coerceIn(0f, 1f)
                        fy = (fy + dragAmount.y / freeH).coerceIn(0f, 1f)
                    }
                }
                .clickable(onClick = onClick)
        ) {
            Text(text = label, fontSize = 22.sp, color = fg)
        }
    }
}
