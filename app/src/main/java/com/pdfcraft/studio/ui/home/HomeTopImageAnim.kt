package com.pdfcraft.studio.ui.home

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.DraggableFab
import com.pdfcraft.studio.ui.editor.DropdownBorderAnim
import com.pdfcraft.studio.ui.editor.dropdownBorderAnim
import java.util.Locale
import kotlin.math.roundToInt

/**
 * fix50: animated border for the Home top image (same 250 animations as the dropdowns),
 * prefs "home_top_anim". choice 0 = off: the static Top Image Border design is drawn.
 * choice > 0: the animation is drawn instead of the static design, in a ring outside the image.
 */
internal object HomeTopAnim {
    const val MIN_THICK = 0.4f
    const val MAX_THICK = 4.0f
    private var loaded = false
    var choice by mutableStateOf(0)
        private set
    var thickness by mutableStateOf(1f)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences("home_top_anim", Context.MODE_PRIVATE)
        choice = p.getInt("choice", 0).coerceIn(0, DropdownBorderAnim.COUNT)
        thickness = p.getFloat("thickness", 1f).coerceIn(MIN_THICK, MAX_THICK)
    }

    fun select(context: Context, n: Int) {
        choice = n.coerceIn(0, DropdownBorderAnim.COUNT)
        context.applicationContext.getSharedPreferences("home_top_anim", Context.MODE_PRIVATE)
            .edit().putInt("choice", choice).apply()
    }

    fun setThickness(context: Context, v: Float) {
        thickness = v.coerceIn(MIN_THICK, MAX_THICK)
        context.applicationContext.getSharedPreferences("home_top_anim", Context.MODE_PRIVATE)
            .edit().putFloat("thickness", thickness).apply()
    }
}

/** fix50: the chosen animation in a ring outside the image, otherwise the static design [n]. */
@Composable
internal fun Modifier.homeTopBorderOrAnim(n: Int, shape: RoundedCornerShape, cornerDp: Int): Modifier {
    HomeTopAnim.ensureLoaded(LocalContext.current)
    val a = HomeTopAnim.choice
    if (a < 1) return this.homeTopImageBorder(n, shape)
    val ring = (2.5f * 3f * HomeTopAnim.thickness).dp
    return this.dropdownBorderAnim(a, cornerDp.dp + ring, HomeTopAnim.thickness).padding(ring)
}

/** fix50: Home floating button with the border animations for the top image. */
@Composable
internal fun HomeTopImageAnimButton(fabColor: Color, fabFg: Color) {
    val ctx = LocalContext.current
    HomeTopAnim.ensureLoaded(ctx)
    var open by remember { mutableStateOf(false) }
    val n = HomeTopAnim.choice
    DraggableFab(
        key = "home_top_anim",
        defaultX = 1f,
        defaultY = 0.65f,
        color = fabColor,
        fg = fabFg,
        label = if (n == 0) "\u2726" else n.toString(),
        onClick = { open = true }
    )
    if (open) HomeTopImageAnimDialog(onDismiss = { open = false })
}

@Composable
private fun HomeTopImageAnimDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = HomeTopAnim.choice
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp)) {
            Text(stringResource(R.string.home_top_anim_title), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF212121))
            Text(
                if (current == 0) stringResource(R.string.border_animation_off) else stringResource(R.string.selected_number, current),
                fontSize = 13.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.border_thickness), fontSize = 13.sp, color = Color(0xFF424242))
                Spacer(Modifier.weight(1f))
                Text(String.format(Locale.US, "%.1fx", HomeTopAnim.thickness), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            }
            Slider(
                value = HomeTopAnim.thickness,
                onValueChange = { HomeTopAnim.setThickness(ctx, (it * 10f).roundToInt() / 10f) },
                valueRange = HomeTopAnim.MIN_THICK..HomeTopAnim.MAX_THICK,
                steps = 35
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DropdownBorderAnim.COUNT) { i ->
                    val k = i + 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(if (k == current) Color(0xFFE3F2FD) else Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                            .then(if (k == current) Modifier.border(2.dp, Color(0xFF1976D2), RoundedCornerShape(10.dp)) else Modifier)
                            .dropdownBorderAnim(k, 10.dp, HomeTopAnim.thickness)
                            .clickable { HomeTopAnim.select(ctx, k) }
                    ) {
                        Text(k.toString(), color = Color(0xFF212121), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { HomeTopAnim.select(ctx, 0) }) { Text(stringResource(R.string.border_animation_off)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_label)) }
            }
        }
    }
}
