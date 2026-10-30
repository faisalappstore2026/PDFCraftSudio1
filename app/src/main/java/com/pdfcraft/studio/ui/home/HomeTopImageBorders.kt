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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab
import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

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

/** fix45: border line thickness (0.4x .. 4.0x), prefs "home_top_border". */
internal object HomeTopBorderThickness {
    const val MIN = 0.4f
    const val MAX = 4.0f
    private var loaded = false
    var value by mutableStateOf(1f)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        value = context.applicationContext.getSharedPreferences("home_top_border", Context.MODE_PRIVATE)
            .getFloat("thickness", 1f).coerceIn(MIN, MAX)
    }

    fun set(context: Context, v: Float) {
        value = v.coerceIn(MIN, MAX)
        context.applicationContext.getSharedPreferences("home_top_border", Context.MODE_PRIVATE)
            .edit().putFloat("thickness", value).apply()
    }
}

/** 1..30 = fix43 (style 0 gradient, 1 double, 2 dashed); 31..250 = fix45 styles 3..12 with generated colours. */
private fun borderColors(n: Int): List<Color> {
    if (n <= 30) return BORDER_PALETTES[(n - 1) % 10]
    val k = n - 31
    val p = k / 10
    val h = ((p * 37 + (k % 10) * 23) % 360).toFloat()
    val sat = if (p % 4 == 3) 0.45f else 0.85f
    val v = if (p % 5 == 4) 0.6f else 1f
    return listOf(Color.hsv(h, sat, v), Color.hsv((h + 50f) % 360f, sat, v), Color.hsv((h + 180f) % 360f, sat * 0.7f, 1f))
}

private fun borderStyle(n: Int): Int = if (n <= 30) (n - 1) / 10 else 3 + (n - 31) % 10

/** fix46: how far each style reaches from the outer edge, in units of the line width w. */
private val BORDER_EXTENT = floatArrayOf(1f, 2.23f, 1f, 1f, 1.97f, 1.9f, 1f, 1f, 2.4f, 1.73f, 2.15f, 1.6f, 1.23f)

/**
 * fix46: the border is drawn OUTSIDE the content: this pads the content by the border extent and
 * draws the lines in that ring, so no image pixel is covered. Outer radius = image radius + extent.
 * Put a clip for the image AFTER this modifier.
 */
internal fun Modifier.homeTopImageBorder(n: Int, shape: RoundedCornerShape): Modifier {
    if (n < 1 || n > HomeTopImageFit.BORDER_COUNT) return this
    val colors = borderColors(n)
    val style = borderStyle(n)
    val extentDp = (3f * HomeTopBorderThickness.value * BORDER_EXTENT[style.coerceIn(0, BORDER_EXTENT.size - 1)]).dp
    return this.drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height))
        val w = 3.dp.toPx() * HomeTopBorderThickness.value
        val ext = extentDp.toPx()
        val inner = Size((size.width - 2 * ext).coerceAtLeast(1f), (size.height - 2 * ext).coerceAtLeast(1f))
        val r = shape.topStart.toPx(inner, this).coerceAtLeast(0f) + ext
        fun line(inset: Float, width: Float, effect: PathEffect?, b: Brush = brush, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Butt) {
            drawRoundRect(
                brush = b,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                alpha = alpha,
                style = Stroke(width = width, cap = cap, pathEffect = effect)
            )
        }
        when (style) {
            0 -> line(w / 2, w, null)
            1 -> {
                line(w / 3, w / 1.5f, null)
                line(w * 1.9f, w / 1.5f, null)
            }
            2 -> line(w / 2, w, PathEffect.dashPathEffect(floatArrayOf(w * 4, w * 2.5f), 0f))
            3 -> line(w / 2, w, PathEffect.dashPathEffect(floatArrayOf(0.01f, w * 2.2f), 0f), cap = StrokeCap.Round) // dots
            4 -> { // triple thin
                line(w / 6, w / 3, null)
                line(w, w / 3, null)
                line(w * 1.8f, w / 3, null)
            }
            5 -> { // thick inner + thin outer
                line(w / 6, w / 3, null)
                line(w * 1.3f, w * 1.2f, null)
            }
            6 -> line(w / 2, w, PathEffect.dashPathEffect(floatArrayOf(w * 9, w * 3), 0f)) // long dashes
            7 -> line(w / 2, w, PathEffect.dashPathEffect(floatArrayOf(w * 5, w * 1.5f, 0.01f, w * 1.5f), 0f), cap = StrokeCap.Round) // dash-dot
            8 -> { // glow
                line(w * 1.2f, w * 2.4f, null, alpha = 0.3f)
                line(w / 2, w * 0.7f, null)
            }
            9 -> { // two-colour double
                line(w / 3, w / 1.5f, null, Brush.linearGradient(listOf(colors[0], colors[0]), Offset.Zero, Offset(size.width, size.height)))
                line(w * 1.4f, w / 1.5f, null, Brush.linearGradient(listOf(colors[2], colors[2]), Offset.Zero, Offset(size.width, size.height)))
            }
            10 -> { // solid + dashed inside
                line(w / 2, w, null)
                line(w * 1.9f, w / 2, PathEffect.dashPathEffect(floatArrayOf(w * 2, w * 2), 0f))
            }
            11 -> line(w * 0.8f, w * 1.6f, null) // bold
            else -> { // hairline pair
                line(w / 4, w / 2, null)
                line(w * 1.1f, w / 4, null, alpha = 0.7f)
            }
        }
    }.padding(extentDp)
}

/** fix43: second floating button on Home, opens the 30 border designs. */
@Composable
internal fun HomeTopImageBorderButton(fabColor: Color, fabFg: Color) {
    var open by remember { mutableStateOf(false) }
    HomeTopBorderThickness.ensureLoaded(LocalContext.current) // fix45
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
            // fix45: border thickness
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.border_thickness), fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1fx", HomeTopBorderThickness.value),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Slider(
                value = HomeTopBorderThickness.value,
                onValueChange = { HomeTopBorderThickness.set(ctx, (it * 10f).roundToInt() / 10f) },
                valueRange = HomeTopBorderThickness.MIN..HomeTopBorderThickness.MAX,
                steps = 35
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HomeTopImageFit.BORDER_COUNT) { i ->
                    val n = i + 1
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
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
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { HomeTopImageState.changeBorder(ctx, 0) }) {
                    Text(stringResource(R.string.none_label))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
