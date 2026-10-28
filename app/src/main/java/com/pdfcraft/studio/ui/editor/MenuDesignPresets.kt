package com.pdfcraft.studio.ui.editor

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * fix36: window design builder. fix40: the 30 whole window Menu Designs were removed,
 * only design 25 is kept (DEFAULT_EDITOR_DESIGN). Dropdown designs: MenuDesignPresets2.kt.
 * Item styles: 0 lines, 1 pills, 2 accent bar, 3 outlined, 4 inset lines, 5 cards, 6 airy.
 */
private fun c(v: Long) = Color(v)
internal fun mdH(a: Long, b: Long) = Brush.horizontalGradient(listOf(Color(a), Color(b)))
internal fun mdV(a: Long, b: Long) = Brush.verticalGradient(listOf(Color(a), Color(b)))

internal fun designTheme(
    name: String,
    menu: Long, text: Long, accent: Long,
    top: Long, topFg: Long,
    bar: Long, chip: Long, chipFg: Long,
    pages: Long, page: Long,
    style: Int = 0,
    onAccent: Long = 0xFFFFFFFF,
    corner: Int = 14,
    chipCorner: Int = 12,
    shadow: Int = 2,
    shadowTint: Long = 0xFF000000,
    chipLine: Long = 0,
    menuLine: Long = 0,
    bold: Boolean = false,
    menuBrush: Brush? = null,
    openBrush: Brush? = null,
    topBrush: Brush? = null,
    pagesBrush: Brush? = null,
    glow2: Long = accent,
    label: Long = topFg
): MenuDesign {
    val tx = c(text)
    val ac = c(accent)
    val card = style == 1 || style == 3 || style == 5
    return MenuDesign(
        name = name,
        container = c(menu),
        containerBrush = menuBrush,
        corner = corner.dp,
        outline = if (menuLine != 0L) c(menuLine) else null,
        text = tx,
        subText = tx.copy(alpha = 0.55f),
        accent = ac,
        itemBg = when (style) {
            1 -> tx.copy(alpha = 0.05f)
            5 -> tx.copy(alpha = 0.07f)
            else -> Color.Transparent
        },
        itemCorner = when (style) { 1 -> 50.dp; 3 -> 8.dp; 5 -> 10.dp; else -> 0.dp },
        itemOutline = if (style == 3) tx.copy(alpha = 0.14f) else null,
        itemMarginH = if (card) 7.dp else 0.dp,
        itemMarginV = if (card) 2.dp else 0.dp,
        itemPadH = if (card) 12.dp else 16.dp,
        itemPadV = if (style == 6) 8.dp else if (card) 6.dp else 7.dp,
        pressed = ac.copy(alpha = 0.18f),
        accentBar = style == 2,
        divider = when (style) {
            0 -> MenuDividerStyle.FULL
            4 -> MenuDividerStyle.INSET
            else -> MenuDividerStyle.NONE
        },
        dividerColor = tx.copy(alpha = 0.10f),
        fontWeight = if (bold) FontWeight.SemiBold else if (style == 6) FontWeight.Normal else FontWeight.Medium,
        alignRowBg = tx.copy(alpha = 0.05f),
        headerBg = c(chip),
        headerBgOpen = ac,
        headerBrushOpen = openBrush,
        headerFg = c(chipFg),
        headerFgOpen = c(onAccent),
        headerOutline = if (chipLine != 0L) c(chipLine) else null,
        headerOutlineOpen = if (chipLine != 0L) ac else null,
        headerCorner = chipCorner.dp,
        headerWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
        topBarBg = c(top),
        topBarBrush = topBrush,
        topBarFg = c(topFg),
        badge = if (onAccent == 0xFFFFFFFF) ac else Color(0xFF1976D2),
        toolbarBg = c(bar),
        toolbarDivider = c(chipFg).copy(alpha = 0.12f),
        pagesBg = c(pages),
        pagesBrush = pagesBrush,
        pageBorder = c(page),
        pageShadow = shadow.dp,
        pageShadowColor = c(shadowTint),
        pageLine = c(page).copy(alpha = 1f),
        glow = ac,
        glow2 = c(glow2),
        pageLabel = c(label).copy(alpha = 0.7f)
    )
}

/** fix40: default editor window design = fix36 Menu Design 25 "Slate Blue", same values (also Favorite 2). */
val DEFAULT_EDITOR_DESIGN: MenuDesign =
    designTheme("Slate Blue", 0xFFFFFFFF, 0xFF1E293B, 0xFF6366F1, 0xFF334155, 0xFFFFFFFF,
        0xFFF1F5F9, 0xFFE2E8F0, 0xFF1E293B, 0xFFCBD5E1, 0xFF94A3B8, style = 0, corner = 12,
        chipCorner = 10, shadow = 4, glow2 = 0xFFA5B4FC, label = 0xFF334155)
