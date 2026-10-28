package com.pdfcraft.studio.ui.editor

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * fix40: Menu Design = 30 dropdown list designs. They restyle ONLY the dropdown lists
 * (Page Tools, Image Tools, Text Tools and the three dots menu: Save Project, Save PDF,
 * Menu Design, Favorite Design). The editor window itself follows Favorite Design.
 * Item styles: 0 lines, 1 pills, 2 accent bar, 3 outlined, 4 inset lines, 5 cards,
 * 6 airy, 7 gradient items, 8 terminal (caps spacing + accent lines).
 */
private fun dc(v: Long) = Color(v)

internal fun dropdownTheme(
    name: String,
    menu: Long, text: Long, accent: Long,
    style: Int = 0,
    corner: Int = 14,
    line: Long = 0,
    lineWidth: Int = 1,
    bold: Boolean = false,
    menuBrush: Brush? = null,
    itemBrush: Brush? = null,
    size: Int = 13,
    tracking: Float = 0f
): DropdownDesign {
    val tx = dc(text)
    val ac = dc(accent)
    val card = style == 1 || style == 3 || style == 5 || style == 7
    return DropdownDesign(
        name = name,
        container = dc(menu),
        containerBrush = menuBrush,
        corner = corner.dp,
        outline = if (line != 0L) dc(line) else null,
        outlineWidth = lineWidth.dp,
        text = tx,
        subText = tx.copy(alpha = 0.55f),
        accent = ac,
        itemBg = when (style) {
            1 -> tx.copy(alpha = 0.06f)
            5 -> tx.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        itemBrush = if (style == 7) itemBrush else null,
        itemCorner = when (style) { 1 -> 50.dp; 3 -> 8.dp; 5 -> 10.dp; 7 -> 12.dp; else -> 0.dp },
        itemOutline = if (style == 3) ac.copy(alpha = 0.45f) else null,
        itemMarginH = if (card) 7.dp else 0.dp,
        itemMarginV = if (card) 2.dp else 0.dp,
        itemPadH = if (card) 12.dp else 16.dp,
        itemPadV = if (style == 6) 9.dp else if (card) 6.dp else 7.dp,
        pressed = ac.copy(alpha = 0.20f),
        accentBar = style == 2,
        divider = when (style) {
            0, 8 -> MenuDividerStyle.FULL
            4 -> MenuDividerStyle.INSET
            else -> MenuDividerStyle.NONE
        },
        dividerColor = if (style == 8) ac.copy(alpha = 0.35f) else tx.copy(alpha = 0.10f),
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.SemiBold else if (style == 6) FontWeight.Normal else FontWeight.Medium,
        letterSpacing = if (tracking != 0f) tracking.sp else TextUnit.Unspecified,
        alignRowBg = tx.copy(alpha = 0.06f)
    )
}

/** fix40: picker numbers 1..30 follow this order. */
val DROPDOWN_DESIGNS: List<DropdownDesign> by lazy {
    listOf(
        // 1
        dropdownTheme("Neon Cyan", 0xFF0B1220, 0xFFE0FBFF, 0xFF00E5FF, style = 3, corner = 12,
            line = 0xFF00E5FF, bold = true),
        // 2
        dropdownTheme("Glass White", 0xFFFFFFFF, 0xFF1E293B, 0xFF3B82F6, style = 5, corner = 20,
            line = 0xFFDCE4EF, menuBrush = mdV(0xFFFFFFFF, 0xFFF1F5FB)),
        // 3
        dropdownTheme("Hologram", 0xFF101A33, 0xFFFFFFFF, 0xFF7DF9FF, style = 1, corner = 22,
            line = 0x667DF9FF, menuBrush = mdV(0xFF13204A, 0xFF0E3B4A)),
        // 4
        dropdownTheme("Matrix", 0xFF000000, 0xFF00FF41, 0xFF00FF41, style = 8, corner = 4,
            line = 0xFF00802A, size = 12, tracking = 1f),
        // 5
        dropdownTheme("Plasma", 0xFF1A0626, 0xFFFFE6FA, 0xFFFF2BD6, style = 7, corner = 20,
            itemBrush = mdH(0xFF3A0A52, 0xFF5A0B6E)),
        // 6
        dropdownTheme("Arctic Lines", 0xFFF7FBFF, 0xFF0B3954, 0xFF0096C7, style = 0, corner = 12),
        // 7
        dropdownTheme("Carbon Bar", 0xFF1C1C1E, 0xFFF2F2F7, 0xFFFF9F0A, style = 2, corner = 10,
            bold = true),
        // 8
        dropdownTheme("Circuit Green", 0xFF04140E, 0xFFCFFFE8, 0xFF1DE9B6, style = 4, corner = 8,
            line = 0xFF1DE9B6),
        // 9
        dropdownTheme("Solar Flare", 0xFFFFF8F0, 0xFF4A1C00, 0xFFFF6D00, style = 7, corner = 18,
            itemBrush = mdH(0xFFFFE0C2, 0xFFFFF3E6)),
        // 10
        dropdownTheme("Space HUD", 0xFF020617, 0xFF7DD3FC, 0xFF38BDF8, style = 8, corner = 2,
            line = 0xFF38BDF8, bold = true, size = 12, tracking = 1.2f),
        // 11
        dropdownTheme("Chrome", 0xFFF2F4F7, 0xFF1C2430, 0xFF3A86FF, style = 3, corner = 14,
            line = 0xFFC9D1DC, menuBrush = mdV(0xFFFFFFFF, 0xFFDDE2E9)),
        // 12
        dropdownTheme("Laser Pink", 0xFF12000A, 0xFFFFE3F1, 0xFFFF1F8E, style = 2, corner = 14,
            line = 0xFFFF1F8E, bold = true),
        // 13
        dropdownTheme("Quantum Violet", 0xFF0E0624, 0xFFF1E9FF, 0xFFB388FF, style = 5, corner = 18,
            menuBrush = mdV(0xFF1A0B3A, 0xFF0E0624)),
        // 14
        dropdownTheme("Mint Air", 0xFFF0FFF8, 0xFF053B2B, 0xFF00BFA5, style = 6, corner = 22),
        // 15
        dropdownTheme("Volt", 0xFF0D0D0D, 0xFFFFF59D, 0xFFFFEA00, style = 3, corner = 0,
            line = 0xFFFFEA00, lineWidth = 2, bold = true),
        // 16
        dropdownTheme("Ocean Depth", 0xFF061325, 0xFFE6F0FF, 0xFF4FC3F7, style = 7, corner = 16,
            itemBrush = mdH(0xFF0C1F3A, 0xFF10335C)),
        // 17
        dropdownTheme("Paper Tech", 0xFFFFFFFF, 0xFF111827, 0xFF111827, style = 4, corner = 6,
            line = 0xFFE5E7EB),
        // 18
        dropdownTheme("Aurora", 0xFF00241F, 0xFFE0FFF7, 0xFF00F5A0, style = 1, corner = 20,
            menuBrush = mdV(0xFF00241F, 0xFF0A1A3F)),
        // 19
        dropdownTheme("Ruby Core", 0xFF1A0005, 0xFFFFE6EC, 0xFFFF1744, style = 0, corner = 12,
            line = 0x66FF1744),
        // 20
        dropdownTheme("Sky Glass", 0xFFEAF6FF, 0xFF0B2545, 0xFF2979FF, style = 1, corner = 24,
            line = 0xFFCFE3F7, menuBrush = mdV(0xFFF5FBFF, 0xFFDDEFFF)),
        // 21
        dropdownTheme("Terminal Amber", 0xFF140C00, 0xFFFFB000, 0xFFFFB000, style = 8, corner = 0,
            line = 0xFF805800, size = 12, tracking = 0.8f),
        // 22
        dropdownTheme("Lunar", 0xFFF2F2F4, 0xFF16181D, 0xFF3D5AFE, style = 5, corner = 16),
        // 23
        dropdownTheme("Cyber Grape", 0xFF14001F, 0xFFF5E1FF, 0xFFE040FB, style = 3, corner = 18,
            line = 0xFFE040FB),
        // 24
        dropdownTheme("Ion Blue", 0xFF001B4D, 0xFFFFFFFF, 0xFF00E5FF, style = 7, corner = 14,
            bold = true, itemBrush = mdH(0xFF2962FF, 0xFF00B8D4)),
        // 25
        dropdownTheme("Steel Lines", 0xFF263238, 0xFFECEFF1, 0xFF80CBC4, style = 4, corner = 10),
        // 26
        dropdownTheme("Holo Pearl", 0xFFFDFBFF, 0xFF2E1065, 0xFF8B5CF6, style = 6, corner = 20,
            line = 0xFFE9DDFF, menuBrush = mdH(0xFFFFF0FB, 0xFFEAF4FF)),
        // 27
        dropdownTheme("Toxic Lime", 0xFF0F1405, 0xFFEFFFD0, 0xFFC6FF00, style = 2, corner = 8),
        // 28
        dropdownTheme("Sunrise", 0xFFFFF4EF, 0xFF3A1F1C, 0xFFFF5E62, style = 1, corner = 24,
            line = 0xFFFFD3C4),
        // 29
        dropdownTheme("Deep Navy", 0xFF0A1931, 0xFFE3EAF5, 0xFF5B8DEF, style = 6, corner = 12,
            line = 0xFF1F3A63),
        // 30
        dropdownTheme("Prism", 0xFF0F0F14, 0xFFFFFFFF, 0xFFF72585, style = 5, corner = 22,
            line = 0x33FFFFFF, bold = true, menuBrush = mdV(0xFF1C1C28, 0xFF0F0F14))
    )
}
