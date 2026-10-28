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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdfcraft.studio.R

/**
 * fix40: Menu Design picker = 30 dropdown list designs (5 per row) with a live preview.
 * Only the dropdown lists change (Page / Image / Text Tools and the three dots menu).
 * Tapping a number applies it at once and keeps the dialog open, Default = the lists
 * of the current window design (Favorite Design).
 */
@Composable
fun MenuDesignPickerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val window = currentMenuDesign()
    val shown = currentDropdownDesign()
    val active = MenuDesignState.dropdown
    val defaultWord = stringResource(R.string.menu_design_default)
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
                    text = stringResource(R.string.menu_design_title),
                    color = Color(0xFF1F1F1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.menu_design_hint),
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                DropdownPreview()
                Spacer(modifier = Modifier.height(14.dp))
                DROPDOWN_DESIGNS.chunked(5).forEachIndexed { row, rowDesigns ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDesigns.forEachIndexed { col, design ->
                            val number = row * 5 + col + 1
                            DropdownDesignChip(
                                design = design,
                                number = number,
                                selected = active == number,
                                onClick = { MenuDesignState.selectDropdown(ctx.applicationContext, number) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                val currentName =
                    if (active in 1..DROPDOWN_DESIGNS.size) active.toString() + " · " + shown.name
                    else defaultWord + " · " + window.name
                Text(
                    text = stringResource(R.string.menu_design_current) + " " + currentName,
                    color = Color(0xFF424242),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Row {
                    TextButton(onClick = { MenuDesignState.selectDropdown(ctx.applicationContext, 0) }) {
                        Text(defaultWord)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.menu_design_close))
                    }
                }
            }
        }
    }
}

/** fix40: small copy of a dropdown list drawn with the chosen dropdown design. */
@Composable
private fun DropdownPreview() {
    val d = currentDropdownDesign()
    val shape = RoundedCornerShape(d.corner)
    val outline = d.outline
    Column(
        modifier = Modifier
            .width(230.dp)
            .shadow(4.dp, shape)
            .clip(shape)
            .designBackground(d.container, d.containerBrush)
            .then(if (outline != null) Modifier.border(d.outlineWidth, outline, shape) else Modifier)
            .padding(vertical = 8.dp)
    ) {
        MenuItemFrame(onClick = {}) { MenuItemText(stringResource(R.string.save_project)) }
        MenuDesignDivider()
        MenuItemFrame(onClick = {}) { MenuItemText(stringResource(R.string.export_pdf)) }
        MenuDesignDivider()
        MenuItemFrame(onClick = {}) { MenuItemText(stringResource(R.string.menu_design_option)) }
        MenuDesignDivider()
        MenuItemFrame(onClick = {}) { MenuItemText(stringResource(R.string.favorite_design_option)) }
    }
}

/** fix40: one round number button = colors of that dropdown design. */
@Composable
private fun DropdownDesignChip(design: DropdownDesign, number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .designBackground(design.container, design.containerBrush)
            .border(
                if (selected) 3.dp else 1.5.dp,
                if (selected) Color(0xFF1F1F1F) else (design.outline ?: design.accent),
                CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = number.toString(),
            color = design.text,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** fix36: Favorite Design picker (whole editor window), numbers follow FAVORITE_DESIGNS order. */
@Composable
fun FavoriteDesignPickerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = currentMenuDesign()
    val active = MenuDesignState.favorite
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
                    text = stringResource(R.string.favorite_design_title),
                    color = Color(0xFF1F1F1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.favorite_design_hint),
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                FAVORITE_DESIGNS.chunked(5).forEachIndexed { row, rowDesigns ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDesigns.forEachIndexed { col, design ->
                            val i = row * 5 + col
                            val selected = active == i
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
                                        MenuDesignState.selectFavorite(ctx.applicationContext, i)
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
                    text = stringResource(R.string.menu_design_current) + " " + favWord + " " +
                        (active + 1).toString() + " · " + current.name,
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
