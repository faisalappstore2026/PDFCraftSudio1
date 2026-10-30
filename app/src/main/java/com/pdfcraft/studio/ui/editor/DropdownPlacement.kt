package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * fix44: own offset (dp) + size scale for each dropdown list.
 * 1 = Page Tools, 2 = Image Tools, 3 = Text Tools, 4 = three dots menu. 0 = not placed.
 * Saved in SharedPreferences "dropdown_placement".
 */
internal object DropdownPlacement {
    const val PAGE = 1
    const val IMAGE = 2
    const val TEXT = 3
    const val DOTS = 4
    const val MIN_SCALE = 0.60f
    const val MAX_SCALE = 1.60f
    const val SCALE_STEPS = 19 // 0.05
    const val MOVE_STEP = 4
    private const val PREFS = "dropdown_placement"
    private var loaded = false
    /** fix45 defaults: page 0,4 | image -4,4 | text 4,4 | dots -16,-12, all scale 1.05 */
    private val DEF_X = intArrayOf(0, 8, -4, -8, -16) // fix50: page 8,4 | image -4,4 | text -8,4 | dots -16,-12
    private val DEF_Y = intArrayOf(0, 4, 4, 4, -12)
    private const val DEF_S = 1.05f
    private val xs = List(5) { mutableStateOf(DEF_X[it]) }
    private val ys = List(5) { mutableStateOf(DEF_Y[it]) }
    private val ss = List(5) { mutableStateOf(if (it == 0) 1f else DEF_S) }

    fun x(id: Int): Int = if (id in 1..4) xs[id].value else 0
    fun y(id: Int): Int = if (id in 1..4) ys[id].value else 0
    fun scale(id: Int): Float = if (id in 1..4) ss[id].value else 1f

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("fix45_defaults", false)) {
            // fix45: start once from the new default placements
            p.edit().clear().putBoolean("fix45_defaults", true).apply()
        }
        if (!p.getBoolean("fix50_defaults", false)) { // fix50: new default placements once
            p.edit().clear().putBoolean("fix45_defaults", true).putBoolean("fix50_defaults", true).apply()
        }
        for (id in 1..4) {
            xs[id].value = p.getInt("m" + id + "_x", DEF_X[id]).coerceIn(-400, 400)
            ys[id].value = p.getInt("m" + id + "_y", DEF_Y[id]).coerceIn(-600, 600)
            ss[id].value = p.getFloat("m" + id + "_s", DEF_S).coerceIn(MIN_SCALE, MAX_SCALE)
        }
    }

    private fun save(context: Context, id: Int) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("m" + id + "_x", xs[id].value)
            .putInt("m" + id + "_y", ys[id].value)
            .putFloat("m" + id + "_s", ss[id].value)
            .apply()
    }

    fun move(context: Context, id: Int, dx: Int, dy: Int) {
        if (id !in 1..4) return
        xs[id].value = (xs[id].value + dx).coerceIn(-400, 400)
        ys[id].value = (ys[id].value + dy).coerceIn(-600, 600)
        save(context, id)
    }

    fun setScale(context: Context, id: Int, v: Float) {
        if (id !in 1..4) return
        ss[id].value = v.coerceIn(MIN_SCALE, MAX_SCALE)
        save(context, id)
    }

    fun reset(context: Context, id: Int) {
        if (id !in 1..4) return
        xs[id].value = DEF_X[id]
        ys[id].value = DEF_Y[id]
        ss[id].value = DEF_S
        save(context, id)
    }

    fun details(): String {
        val names = listOf("", "page", "image", "text", "dots")
        return "DROPDOWNS " + (1..4).joinToString(" | ") { id ->
            String.format(Locale.US, "%s: x=%d y=%d scale=%.2f", names[id], x(id), y(id), scale(id))
        }
    }
}

internal fun dropdownOffset(id: Int): DpOffset = DpOffset(DropdownPlacement.x(id).dp, DropdownPlacement.y(id).dp)

/** Lays the dropdown items out with a scaled density, so the list really gets smaller / bigger. */
@Composable
internal fun ColumnScope.DropdownScaledContent(id: Int, content: @Composable ColumnScope.() -> Unit) {
    val s = DropdownPlacement.scale(id)
    if (id == 0 || abs(s - 1f) < 0.001f) {
        content()
    } else {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale)) {
            content()
        }
    }
}

/** fix44: draggable editor button: move / resize the four dropdown lists. */
@Composable
internal fun DropdownPlacementButton(color: Color, fg: Color) {
    val ctx = LocalContext.current
    DropdownPlacement.ensureLoaded(ctx)
    var open by remember { mutableStateOf(false) }
    DraggableFab(
        key = "editor_dropdown_place",
        defaultX = 1f,
        defaultY = 0.30f,
        color = color,
        fg = fg,
        label = "\u2725",
        onClick = { open = true }
    )
    if (open) DropdownPlacementDialog(onDismiss = { open = false })
}

@Composable
private fun DropdownPlacementDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var sel by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val names = listOf(
        stringResource(R.string.dropdown_page_tools),
        stringResource(R.string.dropdown_image_tools),
        stringResource(R.string.dropdown_text_tools),
        stringResource(R.string.dropdown_dots_menu)
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.dropdown_placement_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF212121)
            )
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 2) {
                        val id = row * 2 + col + 1
                        val on = sel == id
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(if (on) Color(0xFFE3F2FD) else Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                                .border(if (on) 2.dp else 1.dp, if (on) Color(0xFF1976D2) else Color(0xFFBDBDBD), RoundedCornerShape(10.dp))
                                .clickable { sel = id }
                        ) {
                            Text(names[id - 1], fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        }
                    }
                }
            }
            if (sel in 1..4) {
                val id = sel
                val step = DropdownPlacement.MOVE_STEP
                Text(
                    String.format(Locale.US, "x=%d  y=%d  scale=%.2f", DropdownPlacement.x(id), DropdownPlacement.y(id), DropdownPlacement.scale(id)),
                    fontSize = 13.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MoveKey("\u2190") { DropdownPlacement.move(ctx, id, -step, 0) }
                    MoveKey("\u2191") { DropdownPlacement.move(ctx, id, 0, -step) }
                    MoveKey("\u2193") { DropdownPlacement.move(ctx, id, 0, step) }
                    MoveKey("\u2192") { DropdownPlacement.move(ctx, id, step, 0) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dropdown_size), fontSize = 13.sp, color = Color(0xFF424242))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { DropdownPlacement.reset(ctx, id) }) {
                        Text(stringResource(R.string.reset_label))
                    }
                }
                Slider(
                    value = DropdownPlacement.scale(id),
                    onValueChange = { DropdownPlacement.setScale(ctx, id, (it * 20f).roundToInt() / 20f) },
                    valueRange = DropdownPlacement.MIN_SCALE..DropdownPlacement.MAX_SCALE,
                    steps = DropdownPlacement.SCALE_STEPS
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (copied) Text(stringResource(R.string.copied_label), fontSize = 12.sp, color = Color(0xFF2E7D32))
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    clipboard.setText(AnnotatedString(DropdownPlacement.details()))
                    copied = true
                }) { Text(stringResource(R.string.home_top_image_copy)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}

@Composable
private fun MoveKey(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
