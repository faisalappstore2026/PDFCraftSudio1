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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

/**
 * fix43: animated border for the Page / Image / Text Tools and the menu dropdowns.
 * 60 animations = 10 colour sets x 6 motion styles; 0 = off. Saved in prefs "dropdown_border_anim".
 */
internal object DropdownBorderAnim {
    const val COUNT = 60
    const val DEFAULT = 1
    private const val PREFS = "dropdown_border_anim"
    private var loaded = false

    var choice by mutableStateOf(DEFAULT)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        choice = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("choice", DEFAULT).coerceIn(0, COUNT)
    }

    fun select(context: Context, n: Int) {
        choice = n.coerceIn(0, COUNT)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("choice", choice).apply()
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

/** Draws animation [n] (1..60) on top of the content; 0 or out of range = nothing. */
@Composable
internal fun Modifier.dropdownBorderAnim(n: Int, corner: Dp): Modifier {
    if (n < 1 || n > DropdownBorderAnim.COUNT) return this
    val style = (n - 1) % 6
    val colors = ANIM_PALETTES[(n - 1) / 6]
    val period = if (((n - 1) / 6) % 2 == 0) 2400 else 1600
    val t by rememberInfiniteTransition(label = "dropdownBorder").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart),
        label = "dropdownBorderPhase"
    )
    return this.drawWithContent {
        drawContent()
        val base = 2.5.dp.toPx()
        val r = corner.toPx()
        val w = size.width
        val h = size.height
        val perimeter = 2f * (w + h)
        fun stroke(brush: Brush, width: Float, effect: PathEffect? = null, alpha: Float = 1f) {
            val inset = width / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(inset, inset),
                size = Size(w - width, h - width),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                alpha = alpha,
                style = Stroke(width = width, pathEffect = effect)
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
    val current = DropdownBorderAnim.choice
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
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0 until DropdownBorderAnim.COUNT / 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 5) {
                            val n = row * 5 + col + 1
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
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
                                    .clickable { DropdownBorderAnim.select(ctx, n) }
                            ) {
                                Text(n.toString(), color = Color(0xFF212121), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { DropdownBorderAnim.select(ctx, 0) }) {
                    Text(stringResource(R.string.border_animation_off))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
