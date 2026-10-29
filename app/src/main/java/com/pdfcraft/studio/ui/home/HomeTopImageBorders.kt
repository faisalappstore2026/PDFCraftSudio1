package com.pdfcraft.studio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab

/** fix43: 30 border designs for the Home top image = 10 colour sets x 3 line styles. */
private val BORDER_PALETTES: List<List<Color>> = listOf(
    listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0)),
    listOf(Color(0xFF212121), Color(0xFF616161)),
    listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFFEC008C), Color(0xFFFC6767)),
    listOf(Color(0xFFFFD700), Color(0xFFB8860B)),
    listOf(Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF))
)

/** style 0 = gradient line, 1 = double line, 2 = dashed line */
internal fun Modifier.homeTopImageBorder(n: Int, shape: RoundedCornerShape): Modifier {
    if (n < 1 || n > HomeTopImageFit.BORDER_COUNT) return this
    val colors = BORDER_PALETTES[(n - 1) % 10]
    val style = (n - 1) / 10
    return this.drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height))
        val w = 3.dp.toPx()
        val r = shape.topStart.toPx(size, this).coerceAtLeast(0f)
        fun line(inset: Float, width: Float, effect: PathEffect?) {
            drawRoundRect(
                brush = brush,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                style = Stroke(width = width, pathEffect = effect)
            )
        }
        when (style) {
            0 -> line(w / 2, w, null)
            1 -> {
                line(w / 3, w / 1.5f, null)
                line(w * 1.9f, w / 1.5f, null)
            }
            else -> line(w / 2, w, PathEffect.dashPathEffect(floatArrayOf(w * 4, w * 2.5f), 0f))
        }
    }
}

/** fix43: second floating button on Home, opens the 30 border designs. */
@Composable
internal fun HomeTopImageBorderButton(fabColor: Color, fabFg: Color) {
    var open by remember { mutableStateOf(false) }
    DraggableFab(
        key = "home_top_border",
        defaultX = 1f,
        defaultY = 0.80f,
        color = fabColor,
        fg = fabFg,
        label = "\u25A3",
        onClick = { open = true }
    )
    if (open) HomeTopImageBorderDialog(onDismiss = { open = false })
}

@Composable
private fun HomeTopImageBorderDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val current = HomeTopImageState.border
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Text(
                stringResource(R.string.home_top_image_borders),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF212121)
            )
            Text(
                stringResource(R.string.selected_number, current),
                fontSize = 13.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0 until 6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 5) {
                            val n = row * 5 + col + 1
                            val shape = RoundedCornerShape(10.dp)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .background(Color(0xFF9E9E9E), shape)
                                    .homeTopImageBorder(n, shape)
                                    .then(
                                        if (n == current) Modifier.border(2.dp, Color(0xFF1976D2), RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .clickable { HomeTopImageState.changeBorder(ctx, n) }
                            ) {
                                Text(n.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { HomeTopImageState.changeBorder(ctx, 0) }) {
                    Text(stringResource(R.string.none_label))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
