package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab
import java.util.Locale
import kotlin.math.roundToInt

/**
 * fix48: 300 static (not animated) colored border lines, prefs "static_border_lines".
 * Separate choices for the dropdown lists and for the pages (0 = none).
 * Rule: a static design (> 0) replaces the animated border on that target. Picking
 * "None" here, or picking an animation / Animation Off in the animation panel for
 * that target, clears the static design so the animated border is used again.
 */
internal object StaticBorder {
    const val COUNT = 300
    const val MIN_THICK = 0.4f
    const val MAX_THICK = 4.0f
    private const val PREFS = "static_border_lines"
    private var loaded = false
    var dropdown by mutableStateOf(0) // fix52, fix53: none, Border Animation shows
        private set
    var page by mutableStateOf(0)
        private set
    var thickness by mutableStateOf(0.8f) // fix52
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("fix52_defaults", false)) { // fix52: tool lists = static 282 at 0.8x once
            p.edit().putInt("dropdown", 282).putFloat("thickness", 0.8f).putBoolean("fix52_defaults", true).apply()
        }
        if (!p.getBoolean("fix53_defaults", false)) { // fix53: tool lists back to Border Animation once
            p.edit().putInt("dropdown", 0).putBoolean("fix53_defaults", true).apply()
        }
        dropdown = p.getInt("dropdown", 0).coerceIn(0, COUNT)
        page = p.getInt("page", 0).coerceIn(0, COUNT)
        thickness = p.getFloat("thickness", 1f).coerceIn(MIN_THICK, MAX_THICK)
    }

    fun select(context: Context, pages: Boolean, n: Int) {
        ensureLoaded(context)
        val v = n.coerceIn(0, COUNT)
        if (pages) page = v else dropdown = v
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(if (pages) "page" else "dropdown", v).apply()
    }

    /** called by the animation panel: its target goes back to the animated border */
    fun clearFor(context: Context, pages: Boolean) {
        ensureLoaded(context)
        if ((if (pages) page else dropdown) != 0) select(context, pages, 0)
    }

    fun setThickness(context: Context, v: Float) {
        thickness = v.coerceIn(MIN_THICK, MAX_THICK)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("thickness", thickness).apply()
    }
}

/** fix48: size of the triangle arrows on the Page / Image / Text Tools buttons, prefs "tool_arrow_size". */
internal object ToolArrowSize {
    const val MIN = 0.5f
    const val MAX = 3.0f
    private var loaded = false
    var scale by mutableStateOf(1.5f) // fix52
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        scale = 1.5f // fix52: fixed 1.5x, old saved value ignored
    }

    fun set(context: Context, v: Float) {
        scale = v.coerceIn(MIN, MAX)
        context.applicationContext.getSharedPreferences("tool_arrow_size", Context.MODE_PRIVATE)
            .edit().putFloat("scale", scale).apply()
    }
}

private fun staticColors(n: Int): List<Color> {
    val p = (n - 1) / 10
    val h = ((p * 37) % 360).toFloat()
    val sat = if (p % 3 == 1) 0.55f else 0.9f
    val v = if (p % 4 == 3) 0.7f else 1f
    return listOf(Color.hsv(h, sat, v), Color.hsv((h + 60f) % 360f, sat, v), Color.hsv((h + 180f) % 360f, sat * 0.8f, 1f))
}

/** fix48: draws static design [n] (1..300) over the content; 0 = nothing. 10 line styles x 30 colour sets. */
internal fun Modifier.staticBorderLine(n: Int, corner: Dp): Modifier {
    if (n < 1 || n > StaticBorder.COUNT) return this
    val style = (n - 1) % 10
    val cs = staticColors(n)
    return this.drawWithContent {
        drawContent()
        val base = 2.dp.toPx() * StaticBorder.thickness
        val r = corner.toPx()
        val w = size.width
        val h = size.height
        fun line(inset: Float, width: Float, b: Brush, effect: PathEffect? = null, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Butt) {
            drawRoundRect(
                brush = b,
                topLeft = Offset(inset, inset),
                size = Size((w - inset * 2f).coerceAtLeast(1f), (h - inset * 2f).coerceAtLeast(1f)),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                alpha = alpha,
                style = Stroke(width = width, cap = cap, pathEffect = effect)
            )
        }
        val c1 = SolidColor(cs[0])
        val c2 = SolidColor(cs[1])
        val lin = Brush.linearGradient(listOf(cs[0], cs[1], cs[2]), Offset.Zero, Offset(w, h))
        when (style) {
            0 -> line(base / 2f, base, c1) // solid
            1 -> { line(base / 2f, base, c1); line(base * 2.5f, base / 2f, c2) } // double
            2 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(base * 4f, base * 2f), 0f)) // dashed
            3 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(0.01f, base * 2.2f), 0f), cap = StrokeCap.Round) // dots
            4 -> line(base / 2f, base, c1, PathEffect.dashPathEffect(floatArrayOf(base * 5f, base * 1.5f, 0.01f, base * 1.5f), 0f), cap = StrokeCap.Round) // dash-dot
            5 -> line(base / 2f, base, lin) // gradient
            6 -> line(base / 2f, base, Brush.sweepGradient(listOf(cs[0], cs[1], cs[2], cs[0]), Offset(w / 2f, h / 2f))) // sweep
            7 -> { line(base * 0.8f, base * 1.6f, c1); line(base * 2.6f, base / 2f, c2) } // thick + thin
            8 -> line(base / 2f, base, lin, PathEffect.dashPathEffect(floatArrayOf(base * 8f, base * 3f), 0f)) // long dashes
            else -> { line(base * 1.5f, base * 3f, c1, alpha = 0.2f); line(base, base * 2f, c1, alpha = 0.45f); line(base / 2f, base, c1) } // glow
        }
    }
}

/** fix48: static design for the target if one is chosen, otherwise the fix43 animation [animN]. */
@Composable
internal fun Modifier.editorBorderLine(animN: Int, corner: Dp, page: Boolean): Modifier {
    StaticBorder.ensureLoaded(LocalContext.current)
    val s = if (page) StaticBorder.page else StaticBorder.dropdown
    return if (s > 0) this.staticBorderLine(s, corner) else this.dropdownBorderAnim(animN, corner)
}

/** fix48: draggable editor button that opens the 300 static border lines. */
@Composable
internal fun StaticBorderButton(color: Color, fg: Color) {
    val ctx = LocalContext.current
    StaticBorder.ensureLoaded(ctx)
    var open by remember { mutableStateOf(false) }
    DraggableFab(
        key = "editor_static_border",
        defaultX = 1f,
        defaultY = 0.60f,
        color = color,
        fg = fg,
        label = "\u25A2",
        onClick = { open = true }
    )
    if (open) StaticBorderDialog(onDismiss = { open = false })
}

@Composable
private fun StaticBorderDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var pages by remember { mutableStateOf(false) }
    val current = if (pages) StaticBorder.page else StaticBorder.dropdown
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Text(stringResource(R.string.static_border_lines), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF212121))
            Text(
                if (current == 0) stringResource(R.string.static_border_none)
                else stringResource(R.string.selected_number, current),
                fontSize = 13.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            // target tabs: dropdown lists / pages, each keeps its own design
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false, true).forEach { t ->
                    val on = t == pages
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(if (on) Color(0xFF1976D2) else Color(0xFFF1F3F4), RoundedCornerShape(10.dp))
                            .clickable { pages = t }
                    ) {
                        Text(
                            stringResource(if (t) R.string.static_target_pages else R.string.static_target_lists),
                            color = if (on) Color.White else Color(0xFF212121),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(stringResource(R.string.border_thickness), fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1fx", StaticBorder.thickness),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Slider(
                value = StaticBorder.thickness,
                onValueChange = { StaticBorder.setThickness(ctx, (it * 10f).roundToInt() / 10f) },
                valueRange = StaticBorder.MIN_THICK..StaticBorder.MAX_THICK,
                steps = 35
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StaticBorder.COUNT) { i ->
                    val n = i + 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(if (n == current) Color(0xFFE3F2FD) else Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                            .then(if (n == current) Modifier.border(2.dp, Color(0xFF1976D2), RoundedCornerShape(10.dp)) else Modifier)
                            .staticBorderLine(n, 10.dp)
                            .clickable { StaticBorder.select(ctx, pages, n) }
                    ) {
                        Text(n.toString(), color = Color(0xFF212121), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { StaticBorder.select(ctx, pages, 0) }) {
                    Text(stringResource(R.string.static_border_none))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}

/** fix48: bottom slider for the Page / Image / Text Tools arrow size, value shown on the right. */
@Composable
internal fun ToolArrowSizeBar() {
    val ctx = LocalContext.current
    ToolArrowSize.ensureLoaded(ctx)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xEEFFFFFF), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp)
        ) {
            Text(stringResource(R.string.tool_arrow_size), fontSize = 12.sp, color = Color(0xFF424242))
            Slider(
                value = ToolArrowSize.scale,
                onValueChange = { ToolArrowSize.set(ctx, (it * 10f).roundToInt() / 10f) },
                valueRange = ToolArrowSize.MIN..ToolArrowSize.MAX,
                steps = 24,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
            )
            Text(
                String.format(Locale.US, "%.1fx", ToolArrowSize.scale),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }
    }
}
