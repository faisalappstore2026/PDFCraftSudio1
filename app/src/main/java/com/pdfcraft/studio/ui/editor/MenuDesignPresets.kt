package com.pdfcraft.studio.ui.editor

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * fix36: Menu Design themes 1..15 (16..30 are in MenuDesignPresets2.kt).
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

/** fix36: all 30 Menu Design themes; picker numbers 1..30 follow this order. */
val MENU_DESIGNS: List<MenuDesign> by lazy { MENU_DESIGNS_1_15 + MENU_DESIGNS_16_30 }

private val MENU_DESIGNS_1_15: List<MenuDesign> = listOf(
    // 1
    designTheme("Snow White", 0xFFFFFFFF, 0xFF1F1F1F, 0xFFE4293B, 0xFFFFFFFF, 0xFF1F1F1F,
        0xFFFFFFFF, 0xFFF5F5F7, 0xFF2A2A2A, 0xFFF4F4F6, 0xFFE0E0E0, style = 6, corner = 18,
        chipCorner = 14, shadow = 1, label = 0xFF6B6B6B),
    // 2
    designTheme("Brand Red", 0xFFFFFFFF, 0xFF1F1F1F, 0xFFE4293B, 0xFFE4293B, 0xFFFFFFFF,
        0xFFFFFFFF, 0xFFFFEEF0, 0xFFB31D2C, 0xFFF7ECEE, 0xFFF2B8BF, style = 2, bold = true,
        shadow = 3, shadowTint = 0xFFB31D2C, glow2 = 0xFFFF8A80, label = 0xFFB31D2C),
    // 3
    designTheme("Midnight", 0xFF1E1F24, 0xFFECECEC, 0xFFFF5A6A, 0xFF121318, 0xFFFFFFFF,
        0xFF1B1C22, 0xFF2A2B33, 0xFFE6E6EA, 0xFF0E0F13, 0xFF3A3B44, style = 4, shadow = 6,
        glow2 = 0xFF8E9BFF, label = 0xFFB0B0B8),
    // 4
    designTheme("Ocean Breeze", 0xFFF4FBFE, 0xFF0B3A4A, 0xFF0083B0, 0xFF0093C4, 0xFFFFFFFF,
        0xFFF4FBFE, 0xFFE0F4FB, 0xFF0B3A4A, 0xFFE3F4FA, 0xFF9ED8EA, style = 5, corner = 18,
        topBrush = mdH(0xFF00B4DB, 0xFF0083B0), openBrush = mdH(0xFF00B4DB, 0xFF0083B0),
        pagesBrush = mdV(0xFFE6F7FC, 0xFFCDEBF5), shadow = 4, shadowTint = 0xFF0083B0,
        glow2 = 0xFF00B4DB, label = 0xFF0B3A4A),
    // 5
    designTheme("Sunset Glow", 0xFFFFF8F2, 0xFF4A2A1A, 0xFFFF6B4A, 0xFFFF8A65, 0xFFFFFFFF,
        0xFFFFF8F2, 0xFFFFEBDD, 0xFF7A3A1E, 0xFFFFEFE5, 0xFFFFC7A8, style = 1, corner = 20,
        chipCorner = 50, topBrush = mdH(0xFFFF7E5F, 0xFFFEB47B), openBrush = mdH(0xFFFF7E5F, 0xFFFEB47B),
        shadow = 3, shadowTint = 0xFFFF7E5F, glow2 = 0xFFFEB47B, label = 0xFF7A3A1E),
    // 6
    designTheme("Mint Fresh", 0xFFF3FCF8, 0xFF0B3D2E, 0xFF10B981, 0xFFE8F8F1, 0xFF0B3D2E,
        0xFFF3FCF8, 0xFFDDF5EA, 0xFF0B3D2E, 0xFFE6F4EE, 0xFFA7E3C9, style = 1, corner = 18,
        chipCorner = 50, glow2 = 0xFF34D399),
    // 7
    designTheme("Lavender Dream", 0xFFFAF7FF, 0xFF2E1065, 0xFF7C3AED, 0xFFF1ECFF, 0xFF2E1065,
        0xFFFAF7FF, 0xFFEDE5FF, 0xFF3B1A80, 0xFFEFE9FB, 0xFFCDB9F5, style = 5, corner = 20,
        chipCorner = 16, pagesBrush = mdV(0xFFF3EEFF, 0xFFE4DAFA), shadow = 4,
        shadowTint = 0xFF7C3AED, glow2 = 0xFFC084FC),
    // 8
    designTheme("Neon Cyber", 0xFF11111C, 0xFFE0FFF8, 0xFF00FFC6, 0xFF0A0A12, 0xFF00FFC6,
        0xFF0D0D18, 0xFF0D0D18, 0xFF00FFC6, 0xFF07070D, 0xFF00FFC6, style = 3,
        onAccent = 0xFF00201A, chipLine = 0xFF00FFC6, menuLine = 0xFF00FFC6, shadow = 8,
        shadowTint = 0xFF00FFC6, glow2 = 0xFFFF2BD6, bold = true, label = 0xFF7FFFE3),
    // 9
    designTheme("Royal Gold", 0xFFFFFDF5, 0xFF2A2440, 0xFFD4AF37, 0xFF1C1A2E, 0xFFF5D67B,
        0xFF24223A, 0xFF33304F, 0xFFF5D67B, 0xFF2B2944, 0xFFD4AF37, style = 4,
        onAccent = 0xFF1C1A2E, bold = true, menuLine = 0xFFE8D9A8, shadow = 6,
        shadowTint = 0xFFD4AF37, glow2 = 0xFFFFF1B8, label = 0xFFF5D67B),
    // 10
    designTheme("Forest", 0xFFFFFFFF, 0xFF1B2E24, 0xFF2D6A4F, 0xFF1B4332, 0xFFE9F5EC,
        0xFFF1F7F3, 0xFFE1EEE6, 0xFF1B4332, 0xFFDDE8E0, 0xFF95BFA6, style = 2, shadow = 3,
        shadowTint = 0xFF1B4332, glow2 = 0xFF74C69D, label = 0xFF1B4332),
    // 11
    designTheme("Paper Ink", 0xFFFAF7F0, 0xFF222222, 0xFF222222, 0xFFFAF7F0, 0xFF222222,
        0xFFFAF7F0, 0xFFF0EBE0, 0xFF222222, 0xFFE9E4D8, 0xFFCFC6B3, style = 4, corner = 4,
        chipCorner = 4, menuLine = 0xFFCFC6B3, chipLine = 0xFFCFC6B3, shadow = 1,
        glow2 = 0xFF8C7B5E),
    // 12
    designTheme("Glass Frost", 0xFFF7F9FC, 0xFF1E2A3B, 0xFF3B82F6, 0xFFFFFFFF, 0xFF1E2A3B,
        0xFFF7F9FC, 0xFFFFFFFF, 0xFF1E2A3B, 0xFFEAF0F7, 0xFFD3DDEA, style = 5, corner = 22,
        chipCorner = 16, chipLine = 0xFFDCE4EF, menuLine = 0xFFDCE4EF,
        menuBrush = mdV(0xFFFFFFFF, 0xFFEFF4FA), topBrush = mdV(0xFFFFFFFF, 0xFFF0F4FA),
        pagesBrush = mdV(0xFFE3E9F3, 0xFFF4F7FB), shadow = 6, shadowTint = 0xFF3B82F6,
        glow2 = 0xFF93C5FD),
    // 13
    designTheme("Material You", 0xFFF3EDF7, 0xFF1D1B20, 0xFF6750A4, 0xFFFEF7FF, 0xFF1D1B20,
        0xFFF7F2FA, 0xFFE8DEF8, 0xFF1D192B, 0xFFEDE7F3, 0xFFCAC4D0, style = 1, corner = 16,
        chipCorner = 20, shadow = 2, shadowTint = 0xFF6750A4, glow2 = 0xFFD0BCFF),
    // 14
    designTheme("Charcoal Pro", 0xFF2B2D31, 0xFFE3E3E3, 0xFF5865F2, 0xFF2B2D31, 0xFFFFFFFF,
        0xFF35373C, 0xFF45474D, 0xFFFFFFFF, 0xFF1E1F22, 0xFF4E5058, style = 6, corner = 12,
        chipCorner = 10, shadow = 5, glow2 = 0xFF9AA3FF, label = 0xFFB5BAC1),
    // 15
    designTheme("Coral Pop", 0xFFFFFFFF, 0xFF3A1F1C, 0xFFFF6F61, 0xFFFF6F61, 0xFFFFFFFF,
        0xFFFFFFFF, 0xFFFFF0EE, 0xFFC0392B, 0xFFFFE9E6, 0xFFFFB3AA, style = 1, corner = 20,
        chipCorner = 50, bold = true, shadow = 3, shadowTint = 0xFFFF6F61,
        glow2 = 0xFFFFC371, label = 0xFFC0392B)
)
