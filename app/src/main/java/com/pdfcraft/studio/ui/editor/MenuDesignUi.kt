package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.theme.BrandRed

/** fix35: UI pieces that draw the dropdown lists with the chosen MenuDesign. */
@Composable
internal fun currentMenuDesign(): MenuDesign {
    val ctx = LocalContext.current
    remember { MenuDesignState.ensureLoaded(ctx.applicationContext) }
    return MENU_DESIGNS[MenuDesignState.index.coerceIn(0, MENU_DESIGNS.size - 1)]
}

internal fun MenuDesign.iconArgb(): Int = accent.toArgb()

/** Material3 1.2 DropdownMenu has no container params: theme its Surface instead. */
@Composable
internal fun DesignedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties,
    content: @Composable ColumnScope.() -> Unit
) {
    val d = currentMenuDesign()
    val scheme = MaterialTheme.colorScheme.copy(surface = d.container, surfaceTint = d.container)
    val shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(d.corner))
    MaterialTheme(colorScheme = scheme, shapes = shapes, typography = MaterialTheme.typography) {
        val brush = d.containerBrush
        val outline = d.outline
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .then(if (brush != null) Modifier.background(brush) else Modifier)
                .then(if (outline != null) Modifier.border(1.dp, outline, RoundedCornerShape(d.corner)) else Modifier),
            properties = properties,
            content = content
        )
    }
}

@Composable
internal fun MenuItemFrame(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val d = currentMenuDesign()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(d.itemCorner)
    val itemOutline = d.itemOutline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.itemMarginH, vertical = d.itemMarginV)
            .clip(shape)
            .background(if (pressed) d.pressed else d.itemBg, shape)
            .then(if (itemOutline != null) Modifier.border(1.dp, itemOutline, shape) else Modifier)
            .clickable(
                interactionSource = src,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = d.itemPadH, vertical = d.itemPadV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (d.accentBar) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(if (enabled) d.accent else d.subText, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        content()
    }
}

@Composable
internal fun MenuItemText(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false
) {
    val d = currentMenuDesign()
    Text(
        text = text,
        color = if (!enabled || secondary) d.subText else d.text,
        fontSize = if (secondary) (d.fontSize.value - 1f).sp else d.fontSize,
        fontWeight = if (secondary) FontWeight.Normal else d.fontWeight,
        lineHeight = (d.fontSize.value + 4f).sp,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

@Composable
internal fun MenuDesignDivider() {
    val d = currentMenuDesign()
    when (d.divider) {
        MenuDividerStyle.NONE -> Unit
        MenuDividerStyle.FULL -> HorizontalDivider(thickness = 0.5.dp, color = d.dividerColor)
        MenuDividerStyle.INSET -> HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = d.dividerColor
        )
    }
}

@Composable
internal fun MenuDesignHeader(text: String, expanded: Boolean, onClick: () -> Unit) {
    val d = currentMenuDesign()
    val shape = RoundedCornerShape(d.headerCorner)
    val openBrush = d.headerBrushOpen
    val bgMod = if (expanded && openBrush != null) Modifier.background(openBrush, shape)
    else Modifier.background(if (expanded) d.headerBgOpen else d.headerBg, shape)
    val outline = if (expanded) d.headerOutlineOpen else d.headerOutline
    val fg = if (expanded) d.headerFgOpen else d.headerFg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(bgMod)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = d.headerWeight,
            maxLines = 1
        )
        Text(
            text = if (expanded) " ▴" else " ▾",
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = d.headerWeight
        )
    }
}

/** Overflow menu chooser: 10 number chips, each tinted with its design colors. */
@Composable
fun MenuDesignPickerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = currentMenuDesign()
    val currentIndex = MenuDesignState.index
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
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
                Spacer(modifier = Modifier.height(14.dp))
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (col in 0 until 5) {
                            val i = row * 5 + col
                            val design = MENU_DESIGNS[i]
                            val selected = i == currentIndex
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(design.container, CircleShape)
                                    .border(
                                        if (selected) 2.5.dp else 1.dp,
                                        if (selected) BrandRed else Color(0xFFDDDDDD),
                                        CircleShape
                                    )
                                    .clickable {
                                        MenuDesignState.select(ctx.applicationContext, i)
                                        onDismiss()
                                    }
                            ) {
                                Text(
                                    text = (i + 1).toString(),
                                    color = design.text,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = stringResource(R.string.menu_design_current) + " " + (currentIndex + 1).toString() + " · " + current.name,
                    color = Color(0xFF424242),
                    fontSize = 12.sp
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.menu_design_close))
                }
            }
        }
    }
}
