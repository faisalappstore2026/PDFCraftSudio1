package com.pdfcraft.studio.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.pdfcraft.studio.R
import kotlin.math.roundToInt

/** fix37: active Home design, null = original Home look. */
@Composable
internal fun currentHomeDesign(): HomeDesign? {
    val ctx = LocalContext.current
    remember { HomeDesignState.ensureLoaded(ctx.applicationContext) }
    return HomeDesignState.active()
}

/**
 * fix37: draggable round button. Drag = move it anywhere (kept inside the screen),
 * tap = open the 1..30 Home Design picker. Position is saved.
 */
@Composable
internal fun HomeDesignFloatingButton(fabColor: Color, fabFg: Color) {
    val ctx = LocalContext.current
    remember { HomeDesignState.ensureLoaded(ctx.applicationContext) }
    var pickerOpen by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSize = 54.dp
        val margin = 10.dp
        val marginPx = with(density) { margin.toPx() }
        val freeW = with(density) { (maxWidth - fabSize - margin * 2).toPx() }.coerceAtLeast(1f)
        val freeH = with(density) { (maxHeight - fabSize - margin * 2).toPx() }.coerceAtLeast(1f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (marginPx + HomeDesignState.fabX * freeW).roundToInt(),
                        (marginPx + HomeDesignState.fabY * freeH).roundToInt()
                    )
                }
                .size(fabSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(fabColor)
                .border(2.dp, fabFg.copy(alpha = 0.6f), CircleShape)
                .pointerInput(freeW, freeH) {
                    detectDragGestures(
                        onDragEnd = { HomeDesignState.saveFab(ctx.applicationContext) },
                        onDragCancel = { HomeDesignState.saveFab(ctx.applicationContext) }
                    ) { change, dragAmount ->
                        change.consume()
                        HomeDesignState.moveFab(
                            HomeDesignState.fabX + dragAmount.x / freeW,
                            HomeDesignState.fabY + dragAmount.y / freeH
                        )
                    }
                }
                .clickable { pickerOpen = true }
        ) {
            Text(text = "🎨", fontSize = 24.sp, color = fabFg)
        }
    }
    if (pickerOpen) {
        HomeDesignPickerDialog(onDismiss = { pickerOpen = false })
    }
}

/** fix37: Home Design picker, numbers 1..30 (5 per row) + Original. */
@Composable
internal fun HomeDesignPickerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val active = HomeDesignState.design
    val originalWord = stringResource(R.string.home_design_original)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.home_design_title),
                    color = Color(0xFF1F1F1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_design_hint),
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                HOME_DESIGNS.chunked(5).forEachIndexed { row, rowDesigns ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDesigns.forEachIndexed { col, design ->
                            val number = row * 5 + col + 1
                            val selected = active == number
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .homeBackground(design.bg, design.bgBrush)
                                    .border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) Color(0xFF1F1F1F) else Color(0x33000000),
                                        CircleShape
                                    )
                                    .clickable {
                                        HomeDesignState.select(ctx.applicationContext, number)
                                        onDismiss()
                                    }
                            ) {
                                Text(
                                    text = number.toString(),
                                    color = design.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                val currentName = HomeDesignState.active()?.let { active.toString() + " · " + it.name } ?: originalWord
                Text(
                    text = stringResource(R.string.menu_design_current) + " " + currentName,
                    color = Color(0xFF424242),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Row {
                    TextButton(onClick = {
                        HomeDesignState.select(ctx.applicationContext, 0)
                        onDismiss()
                    }) {
                        Text(originalWord)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.menu_design_close))
                    }
                }
            }
        }
    }
}

/**
 * fix37: last screen that set the status bar icons. A screen only resets the icons
 * when it leaves if no newer screen has set them (Home and Editor overlap while navigating).
 */
object StatusBarIconsOwner {
    private var token = 0
    fun claim(): Int {
        token += 1
        return token
    }
    fun isOwner(t: Int): Boolean = t == token
}

private fun Context.findHomeActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** fix37: light icons on dark Home designs; back to dark icons when Home leaves. */
@Composable
internal fun HomeStatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, darkIcons) {
        val window = if (view.isInEditMode) null else view.context.findHomeActivity()?.window
        val myToken = StatusBarIconsOwner.claim()
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
        onDispose {
            if (window != null && StatusBarIconsOwner.isOwner(myToken)) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }
}
