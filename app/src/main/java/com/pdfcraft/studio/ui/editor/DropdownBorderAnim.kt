package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import java.util.Locale

/**
 * fix43: animated border for the Page / Image / Text Tools and the menu dropdowns.
 * 250 animations (fix45: 61..250 added); 0 = off. Saved in prefs "dropdown_border_anim".
 */
internal object DropdownBorderAnim {
    const val COUNT = 250 // fix45: 61..250 added
    const val DEFAULT = 29 // fix45, fix50
    /** fix45: page border design when the pages switch is off */
    const val PAGE_DEFAULT = 27
    private const val PREFS = "dropdown_border_anim"
    private var loaded = false

    var choice by mutableStateOf(DEFAULT)
        private set
    /** fix44: border line thickness multiplier (thin 0.4 .. thick 4.0) */
    const val MIN_THICK = 0.4f
    const val MAX_THICK = 4.0f
    var thickness by mutableStateOf(MIN_THICK) // fix45: default 0.4x
        private set
    /** fix45: ON = the selected animation is also drawn on the editor page borders */
    var pagesFollow by mutableStateOf(false)
        private set

    /** fix46: own page design, used only while the switch is ON (default 27, 0 = none). */
    var pageSel by mutableStateOf(PAGE_DEFAULT)
        private set

    /** fix45/fix46: design drawn on the pages: switch OFF = 27, ON = the separately saved page choice. */
    fun pageChoice(): Int = if (pagesFollow) pageSel else PAGE_DEFAULT

    /** fix46: number shown / highlighted in the panel. */
    fun panelChoice(): Int = if (pagesFollow) pageSel else choice

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("fix45_defaults", false)) {
            // fix45: start once from the new defaults (design 26, 0.4x)
            p.edit().putInt("choice", DEFAULT).putFloat("thickness", MIN_THICK).putBoolean("fix45_defaults", true).apply()
        }
        if (!p.getBoolean("fix50_defaults", false)) { // fix50: dropdowns 29 at 0.4x once
            p.edit().putInt("choice", DEFAULT).putFloat("thickness", MIN_THICK).putBoolean("fix50_defaults", true).apply()
        }
        choice = p.getInt("choice", DEFAULT).coerceIn(0, COUNT)
        thickness = p.getFloat("thickness", MIN_THICK).coerceIn(MIN_THICK, MAX_THICK)
        pagesFollow = p.getBoolean("pages", false)
        pageSel = p.getInt("page_choice", PAGE_DEFAULT).coerceIn(0, COUNT) // fix46
    }

    fun setPagesFollow(context: Context, on: Boolean) {
        pagesFollow = on
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("pages", on).apply()
    }

    fun setThickness(context: Context, v: Float) {
        thickness = v.coerceIn(MIN_THICK, MAX_THICK)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("thickness", thickness).apply()
    }

    fun select(context: Context, n: Int) {
        choice = n.coerceIn(0, COUNT)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("choice", choice).apply()
    }

    /** fix46: panel tap: switch ON = pages only, OFF = dropdowns only. */
    fun selectFromPanel(context: Context, n: Int) {
        StaticBorder.clearFor(context, pagesFollow) // fix48: an animation choice brings the animated border back
        if (!pagesFollow) {
            select(context, n)
            return
        }
        pageSel = n.coerceIn(0, COUNT)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("page_choice", pageSel).apply()
    }
}

private val ANIM_PALETTES: List<List<Color>> = listOf(
    listOf(Color(0xFFFF0000), Color(0xFFFFEB3B), Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9)),
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF), Color(0xFFFFFFFF)),
    listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFF8A65)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFFB2FF59)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFFE040FB)),
    listOf(Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFF8E1)),
    listOf(Color(0xFFEC008C), Color(0xFFFC6767), Color(0xFFFFCDD2)),
    listOf(Color(0xFF00E5FF), Color(0xFF1DE9B6), Color(0xFF2979FF)),
    listOf(Color(0xFFC62828), Color(0xFF212121), Color(0xFFFF5252)),
    listOf(Color(0xFF424242), Color(0xFFBDBDBD), Color(0xFFFFFFFF))
)

private class AnimSpec(val style: Int, val colors: List<Color>, val period: Int)

/** 1..60 = fix43 set; 61..250 (fix45) = 11 motions x generated colour sets x 3 speeds. */
private fun animSpec(n: Int): AnimSpec {
    if (n <= 60) {
        return AnimSpec((n - 1) % 6, ANIM_PALETTES[(n - 1) / 6], if (((n - 1) / 6) % 2 == 0) 2400 else 1600)
    }
    val k = n - 61
    val style = 6 + k % 11 // 6..16
    val p = k / 11
    val h = ((p * 47 + (k % 11) * 29) % 360).toFloat()
    val sat = if (p % 3 == 2) 0.55f else 0.85f
    val colors = listOf(
        Color.hsv(h, sat, 1f),
        Color.hsv((h + 40f) % 360f, sat, 0.95f),
        Color.hsv((h + 200f) % 360f, sat * 0.8f, 1f)
    )
    return AnimSpec(style, colors, listOf(2800, 2000, 1400)[k % 3])
}

/** Draws animation [n] (1..60) on top of the content; 0 or out of range = nothing. */
@Composable
internal fun Modifier.dropdownBorderAnim(n: Int, corner: Dp, thick: Float = DropdownBorderAnim.thickness): Modifier { // fix50
    if (n < 1 || n > DropdownBorderAnim.COUNT) return this
    val spec = animSpec(n)
    val style = spec.style
    val colors = spec.colors
    val period = spec.period
    val t by rememberInfiniteTransition(label = "dropdownBorder").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart),
        label = "dropdownBorderPhase"
    )
    return this.drawWithContent {
        drawContent()
        val base = 2.5.dp.toPx() * thick // fix44, fix50
        val r = corner.toPx()
        val w = size.width
        val h = size.height
        val perimeter = 2f * (w + h)
        fun stroke(brush: Brush, width: Float, effect: PathEffect? = null, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Butt) {
            val inset = width / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(inset, inset),
                size = Size(w - width, h - width),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                alpha = alpha,
                style = Stroke(width = width, cap = cap, pathEffect = effect)
            )
        }
        val wave = sin(t * 2f * PI.toFloat())
        when (style) {
            0 -> { // rotating gradient
                val a = t * 2f * PI.toFloat()
                val c = Offset(w / 2f, h / 2f)
                val d = Offset(cos(a) * w / 2f, sin(a) * h / 2f)
                stroke(Brush.linearGradient(colors + colors.first(), c - d, c + d), base)
            }
            1 -> { // marching dashes
                val on = base * 4f
                val off = base * 2.5f
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base,
                    PathEffect.dashPathEffect(floatArrayOf(on, off), -t * (on + off) * 3f)
                )
            }
            2 -> { // pulse
                stroke(Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base, alpha = 0.35f + 0.65f * abs(wave))
            }
            3 -> { // comet running around
                stroke(Brush.linearGradient(listOf(colors.last(), colors.last()), Offset.Zero, Offset(w, h)), base / 2f, alpha = 0.35f)
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base * 1.2f,
                    PathEffect.dashPathEffect(floatArrayOf(perimeter * 0.22f, perimeter * 0.78f), -t * perimeter)
                )
            }
            4 -> { // colour cycle
                val pos = t * colors.size
                val i = pos.toInt() % colors.size
                val c = lerp(colors[i], colors[(i + 1) % colors.size], pos - pos.toInt())
                stroke(Brush.linearGradient(listOf(c, c), Offset.Zero, Offset(w, h)), base)
            }
            6 -> { // marching round dots
                val gap = base * 3f
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base,
                    PathEffect.dashPathEffect(floatArrayOf(0.01f, gap), -t * gap * 4f), cap = StrokeCap.Round
                )
            }
            7 -> { // two comets
                stroke(Brush.linearGradient(listOf(colors[2], colors[2]), Offset.Zero, Offset(w, h)), base / 2f, alpha = 0.3f)
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base * 1.2f,
                    PathEffect.dashPathEffect(floatArrayOf(perimeter * 0.15f, perimeter * 0.35f), -t * perimeter), cap = StrokeCap.Round
                )
            }
            8 -> { // rotating gradient dashes
                val a = t * 2f * PI.toFloat()
                val c = Offset(w / 2f, h / 2f)
                val d = Offset(cos(a) * w / 2f, sin(a) * h / 2f)
                stroke(
                    Brush.linearGradient(colors + colors.first(), c - d, c + d), base,
                    PathEffect.dashPathEffect(floatArrayOf(base * 6f, base * 2f), t * base * 16f)
                )
            }
            9 -> { // heartbeat
                val beat = if (t < 0.12f) t / 0.12f else if (t < 0.24f) (0.24f - t) / 0.12f
                else if (t < 0.36f) (t - 0.24f) / 0.12f * 0.7f else if (t < 0.48f) (0.48f - t) / 0.12f * 0.7f else 0f
                stroke(Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base * (1f + beat), alpha = 0.35f + 0.65f * beat)
            }
            10 -> { // rainbow drift
                val rb = List(6) { i -> Color.hsv((t * 360f + i * 60f) % 360f, 0.85f, 1f) }
                stroke(Brush.linearGradient(rb, Offset.Zero, Offset(w, h)), base)
            }
            11 -> { // chasing long and short dashes
                val u = base * 3f
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base,
                    PathEffect.dashPathEffect(floatArrayOf(u * 3f, u, u, u), -t * u * 6f)
                )
            }
            12 -> { // glow pulse
                val g = 0.5f + 0.5f * wave
                stroke(Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base * 3f, alpha = 0.15f + 0.25f * g)
                stroke(Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base * 0.8f)
            }
            13 -> { // flashing alternate colours
                val c = if (t < 0.5f) colors[0] else colors[2]
                stroke(Brush.linearGradient(listOf(c, colors[1]), Offset.Zero, Offset(w, h)), base)
            }
            14 -> { // counter dashes (two lines, opposite directions)
                val u = base * 4f
                stroke(
                    Brush.linearGradient(listOf(colors[0], colors[0]), Offset.Zero, Offset(w, h)), base * 0.8f,
                    PathEffect.dashPathEffect(floatArrayOf(u, u), -t * u * 4f)
                )
                stroke(
                    Brush.linearGradient(listOf(colors[2], colors[2]), Offset.Zero, Offset(w, h)), base * 0.8f,
                    PathEffect.dashPathEffect(floatArrayOf(u, u), t * u * 4f + u)
                )
            }
            15 -> { // growing segment
                val len = perimeter * (0.1f + 0.85f * abs(wave))
                stroke(
                    Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), base,
                    PathEffect.dashPathEffect(floatArrayOf(len, perimeter - len + 0.01f), 0f), cap = StrokeCap.Round
                )
            }
            16 -> { // shimmer: sliding light band
                val x = -w + t * 3f * w
                stroke(
                    Brush.linearGradient(
                        listOf(colors[0], Color.White, colors[0]),
                        Offset(x, 0f), Offset(x + w * 0.6f, h)
                    ),
                    base
                )
            }
            else -> { // breathing double line
                val bw = base * (0.6f + 0.6f * abs(wave))
                stroke(Brush.linearGradient(colors, Offset.Zero, Offset(w, h)), bw)
                val inner = base * 2.4f
                drawRoundRect(
                    brush = Brush.linearGradient(colors.reversed(), Offset.Zero, Offset(w, h)),
                    topLeft = Offset(inner, inner),
                    size = Size(w - inner * 2f, h - inner * 2f),
                    cornerRadius = CornerRadius((r - inner).coerceAtLeast(0f)),
                    alpha = 1f - 0.6f * abs(wave),
                    style = Stroke(width = base / 2f)
                )
            }
        }
    }
}

/** fix43: draggable editor button that opens the 60 numbered animations. */
@Composable
internal fun DropdownBorderAnimButton(color: Color, fg: Color) {
    val ctx = LocalContext.current
    DropdownBorderAnim.ensureLoaded(ctx)
    var open by remember { mutableStateOf(false) }
    val n = DropdownBorderAnim.choice
    DraggableFab(
        key = "editor_border_anim",
        defaultX = 1f,
        defaultY = 0.45f,
        color = color,
        fg = fg,
        label = if (n == 0) "\u2726" else n.toString(),
        onClick = { open = true }
    )
    if (open) DropdownBorderAnimDialog(onDismiss = { open = false })
}

@Composable
private fun DropdownBorderAnimDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = DropdownBorderAnim.panelChoice() // fix46
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Text(
                stringResource(R.string.border_animations),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF212121)
            )
            Text(
                if (current == 0) stringResource(R.string.border_animation_off)
                else stringResource(R.string.selected_number, current),
                fontSize = 13.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            // fix45: also draw the selected animation on the page borders
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.border_on_pages), fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = DropdownBorderAnim.pagesFollow,
                    onCheckedChange = { DropdownBorderAnim.setPagesFollow(ctx, it) }
                )
            }
            // fix44: border line thickness
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.border_thickness), fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1fx", DropdownBorderAnim.thickness),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Slider(
                value = DropdownBorderAnim.thickness,
                onValueChange = { DropdownBorderAnim.setThickness(ctx, (it * 10f).roundToInt() / 10f) },
                valueRange = DropdownBorderAnim.MIN_THICK..DropdownBorderAnim.MAX_THICK,
                steps = 35
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DropdownBorderAnim.COUNT) { i ->
                    val n = i + 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(
                                if (n == current) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                                RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (n == current) Modifier.border(2.dp, Color(0xFF1976D2), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .dropdownBorderAnim(n, 10.dp)
                            .clickable { DropdownBorderAnim.selectFromPanel(ctx, n) } // fix46
                    ) {
                        Text(n.toString(), color = Color(0xFF212121), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { DropdownBorderAnim.selectFromPanel(ctx, 0) }) { // fix46
                    Text(stringResource(R.string.border_animation_off))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
