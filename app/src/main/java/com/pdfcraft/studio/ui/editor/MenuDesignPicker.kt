package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R

/** fix36: Menu Design picker, numbers 1..30 (5 per row). */
@Composable
fun MenuDesignPickerDialog(onDismiss: () -> Unit) {
    DesignGridDialog(
        title = stringResource(R.string.menu_design_title),
        hint = stringResource(R.string.menu_design_hint),
        designs = MENU_DESIGNS,
        kind = MenuDesignState.KIND_MENU,
        onDismiss = onDismiss
    )
}

/** fix36: Favorite Design picker, numbers follow FAVORITE_DESIGNS order (1, 2, 3, ...). */
@Composable
fun FavoriteDesignPickerDialog(onDismiss: () -> Unit) {
    DesignGridDialog(
        title = stringResource(R.string.favorite_design_title),
        hint = stringResource(R.string.favorite_design_hint),
        designs = FAVORITE_DESIGNS,
        kind = MenuDesignState.KIND_FAVORITE,
        onDismiss = onDismiss
    )
}

@Composable
private fun DesignGridDialog(
    title: String,
    hint: String,
    designs: List<MenuDesign>,
    kind: Int,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val current = currentMenuDesign()
    val activeKind = MenuDesignState.kind
    val activeIndex = MenuDesignState.index
    val favWord = stringResource(R.string.favorite_design_short)
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
                    text = title,
                    color = Color(0xFF1F1F1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                designs.chunked(5).forEachIndexed { row, rowDesigns ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDesigns.forEachIndexed { col, design ->
                            val i = row * 5 + col
                            val selected = activeKind == kind && activeIndex == i
                            val chipBrush = design.headerBrushOpen ?: design.topBarBrush
                            val chipColor = if (design.headerBrushOpen != null) design.headerBgOpen
                            else if (design.topBarBrush != null) design.topBarBg else design.headerBgOpen
                            val numColor = if (design.headerBrushOpen != null || design.topBarBrush == null)
                                design.headerFgOpen else design.topBarFg
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .designBackground(chipColor, chipBrush)
                                    .border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) Color(0xFF1F1F1F) else Color(0x33000000),
                                        CircleShape
                                    )
                                    .clickable {
                                        MenuDesignState.select(ctx.applicationContext, kind, i)
                                        onDismiss()
                                    }
                            ) {
                                Text(
                                    text = (i + 1).toString(),
                                    color = numColor,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = stringResource(R.string.menu_design_current) + " " +
                        (if (activeKind == MenuDesignState.KIND_FAVORITE) favWord + " " else "") +
                        (activeIndex + 1).toString() + " · " + current.name,
                    color = Color(0xFF424242),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.menu_design_close))
                }
            }
        }
    }
}
