package com.pdfcraft.studio.ui.editor

/* fix36: Menu Design themes 16..30 (builder designTheme is in MenuDesignPresets.kt). */
internal val MENU_DESIGNS_16_30: List<MenuDesign> = listOf(
    // 16
    designTheme("Sky Blue", 0xFFFFFFFF, 0xFF0D2B45, 0xFF1976D2, 0xFF2196F3, 0xFFFFFFFF,
        0xFFFFFFFF, 0xFFE3F2FD, 0xFF0D47A1, 0xFFE3F2FD, 0xFF90CAF9, style = 0, corner = 12,
        shadow = 2, shadowTint = 0xFF1976D2, glow2 = 0xFF64B5F6, label = 0xFF0D47A1),
    // 17
    designTheme("Cherry Blossom", 0xFFFFF5F8, 0xFF5A1832, 0xFFE91E63, 0xFFFFE4EC, 0xFF5A1832,
        0xFFFFF5F8, 0xFFFFE0EA, 0xFF8A1C45, 0xFFFFEEF3, 0xFFF8BBD0, style = 1, corner = 22,
        chipCorner = 50, pagesBrush = mdV(0xFFFFF0F5, 0xFFFCE4EC), shadow = 3,
        shadowTint = 0xFFE91E63, glow2 = 0xFFF48FB1),
    // 18
    designTheme("Aurora", 0xFFFBF9FF, 0xFF241B4B, 0xFF6C5CE7, 0xFF5B6CF0, 0xFFFFFFFF,
        0xFFFBF9FF, 0xFFEFEBFF, 0xFF3A2C8C, 0xFFEEEAFF, 0xFFC5B8FF, style = 5, corner = 20,
        chipCorner = 14, topBrush = mdH(0xFF4776E6, 0xFF8E54E9), openBrush = mdH(0xFF4776E6, 0xFF8E54E9),
        pagesBrush = mdV(0xFFEDE7FF, 0xFFE3F0FF), shadow = 5, shadowTint = 0xFF8E54E9,
        glow2 = 0xFF4776E6, label = 0xFF3A2C8C),
    // 19
    designTheme("Espresso", 0xFFFFF8F0, 0xFF3E2723, 0xFFA0522D, 0xFF3E2723, 0xFFFFE0B2,
        0xFFFFF8F0, 0xFFF3E5D8, 0xFF3E2723, 0xFFD7CCC8, 0xFFA1887F, style = 4, corner = 10,
        chipCorner = 8, onAccent = 0xFFFFF3E0, bold = true, shadow = 4,
        shadowTint = 0xFF3E2723, glow2 = 0xFFFFB74D, label = 0xFF4E342E),
    // 20
    designTheme("Monochrome", 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFFFFFF,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFFE0E0E0, 0xFF000000, style = 3, corner = 0,
        chipCorner = 0, chipLine = 0xFF000000, menuLine = 0xFF000000, bold = true,
        shadow = 0, glow2 = 0xFF9E9E9E, label = 0xFF000000),
    // 21
    designTheme("Electric Lime", 0xFF1A2012, 0xFFF1FFD6, 0xFFC6FF00, 0xFF151A0F, 0xFFC6FF00,
        0xFF151A0F, 0xFF242C18, 0xFFE8FFB0, 0xFF0F120B, 0xFF6E8F00, style = 2,
        onAccent = 0xFF1A2012, bold = true, shadow = 6, shadowTint = 0xFFC6FF00,
        glow2 = 0xFF76FF03, label = 0xFFC6FF00),
    // 22
    designTheme("Arctic", 0xFFFFFFFF, 0xFF0B3954, 0xFF0096C7, 0xFFF0F8FF, 0xFF0B3954,
        0xFFF7FBFF, 0xFFE3F2FB, 0xFF0B3954, 0xFFE6F3FA, 0xFFB9DDF0, style = 6, corner = 20,
        chipCorner = 16, pagesBrush = mdV(0xFFE0F4FF, 0xFFFFFFFF), shadow = 3,
        shadowTint = 0xFF0096C7, glow2 = 0xFF48CAE4),
    // 23
    designTheme("Ruby Night", 0xFF2A0E12, 0xFFFFE5E8, 0xFFFF6B7A, 0xFFB31D2C, 0xFFFFFFFF,
        0xFF3A1218, 0xFF52202A, 0xFFFFE5E8, 0xFF1E0A0D, 0xFF7A2A35, style = 4, shadow = 6,
        shadowTint = 0xFFFF6B7A, glow2 = 0xFFFFB3BC, label = 0xFFFFB3BC),
    // 24
    designTheme("Peach Cream", 0xFFFFFAF5, 0xFF5B2C0F, 0xFFFF8A50, 0xFFFFE5D0, 0xFF5B2C0F,
        0xFFFFFAF5, 0xFFFFEADB, 0xFF7A3B12, 0xFFFFF3EA, 0xFFFFC9A6, style = 5, corner = 18,
        chipCorner = 14, shadow = 2, shadowTint = 0xFFFF8A50, glow2 = 0xFFFFCC80),
    // 25
    designTheme("Slate Blue", 0xFFFFFFFF, 0xFF1E293B, 0xFF6366F1, 0xFF334155, 0xFFFFFFFF,
        0xFFF1F5F9, 0xFFE2E8F0, 0xFF1E293B, 0xFFCBD5E1, 0xFF94A3B8, style = 0, corner = 12,
        chipCorner = 10, shadow = 4, glow2 = 0xFFA5B4FC, label = 0xFF334155),
    // 26
    designTheme("Tropical", 0xFFF5FFFA, 0xFF0B3B2E, 0xFF11998E, 0xFF11998E, 0xFFFFFFFF,
        0xFFF5FFFA, 0xFFDDF7EC, 0xFF0B5E52, 0xFFE6FAF2, 0xFF8EE4C1, style = 1, corner = 20,
        chipCorner = 50, topBrush = mdH(0xFF11998E, 0xFF38EF7D), openBrush = mdH(0xFF11998E, 0xFF38EF7D),
        shadow = 3, shadowTint = 0xFF11998E, glow2 = 0xFF38EF7D, label = 0xFF0B5E52),
    // 27
    designTheme("Retro Pop", 0xFFFFFBEA, 0xFF1B1B1B, 0xFFEE4266, 0xFFFFD23F, 0xFF1B1B1B,
        0xFFFFFBEA, 0xFFFFFFFF, 0xFF1B1B1B, 0xFFFFF3C4, 0xFF1B1B1B, style = 3, corner = 6,
        chipCorner = 6, chipLine = 0xFF1B1B1B, menuLine = 0xFF1B1B1B, bold = true,
        shadow = 0, glow2 = 0xFF3BCEAC),
    // 28
    designTheme("Deep Space", 0xFF16232D, 0xFFE0F2F1, 0xFF4DD0E1, 0xFF0F2027, 0xFFFFFFFF,
        0xFF15262F, 0xFF203A43, 0xFFE0F7FA, 0xFF0F2027, 0xFF2C5364, style = 6, corner = 16,
        onAccent = 0xFF0F2027, topBrush = mdH(0xFF0F2027, 0xFF2C5364),
        pagesBrush = mdV(0xFF0F2027, 0xFF203A43), shadow = 7, shadowTint = 0xFF4DD0E1,
        glow2 = 0xFF80DEEA, label = 0xFF80DEEA),
    // 29
    designTheme("Classic Office", 0xFFFFFFFF, 0xFF202020, 0xFF2B579A, 0xFF2B579A, 0xFFFFFFFF,
        0xFFF3F3F3, 0xFFFFFFFF, 0xFF202020, 0xFFE6E6E6, 0xFFC8C8C8, style = 0, corner = 4,
        chipCorner = 4, chipLine = 0xFFD0D0D0, menuLine = 0xFFD0D0D0, shadow = 2,
        glow2 = 0xFF5B8BD6, label = 0xFF444444),
    // 30
    designTheme("Candy Neon", 0xFF1A1025, 0xFFFFE6F5, 0xFFFF4FD8, 0xFF7928CA, 0xFFFFFFFF,
        0xFF22142F, 0xFF35204A, 0xFFFFE6F5, 0xFF140B1C, 0xFF5A2D80, style = 1, corner = 20,
        chipCorner = 50, topBrush = mdH(0xFFFF0080, 0xFF7928CA), openBrush = mdH(0xFFFF0080, 0xFF7928CA),
        shadow = 7, shadowTint = 0xFFFF4FD8, glow2 = 0xFF7928CA, label = 0xFFFFA6EC)
)
