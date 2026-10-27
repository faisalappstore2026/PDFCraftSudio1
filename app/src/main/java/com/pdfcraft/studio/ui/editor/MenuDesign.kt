package com.pdfcraft.studio.ui.editor

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.ui.theme.BrandRed
import com.pdfcraft.studio.ui.theme.BrandRedDark

/**
 * fix35: design preview system for the Page / Text / Image Tools dropdown lists.
 * Pick 1..10 from the editor overflow menu; the choice is saved in SharedPreferences.
 */
enum class MenuDividerStyle { NONE, FULL, INSET }

data class MenuDesign(
    val name: String,
    // popup container
    val container: Color,
    val containerBrush: Brush? = null,
    val corner: Dp = 16.dp,
    val outline: Color? = null,
    // items
    val text: Color = Color(0xFF1F1F1F),
    val subText: Color = Color(0xFF8A8A8A),
    val accent: Color = BrandRed,
    val itemBg: Color = Color.Transparent,
    val itemCorner: Dp = 0.dp,
    val itemOutline: Color? = null,
    val itemMarginH: Dp = 0.dp,
    val itemMarginV: Dp = 0.dp,
    val itemPadH: Dp = 16.dp,
    val itemPadV: Dp = 7.dp,
    val pressed: Color = Color(0x1AE4293B),
    val accentBar: Boolean = false,
    val divider: MenuDividerStyle = MenuDividerStyle.NONE,
    val dividerColor: Color = Color(0xFFEEEEEE),
    val fontSize: TextUnit = 13.sp,
    val fontWeight: FontWeight = FontWeight.Medium,
    val alignRowBg: Color = Color.Transparent,
    // Page / Text / Image Tools header buttons
    val headerBg: Color = Color(0xFFF4F4F6),
    val headerBgOpen: Color = BrandRed,
    val headerBrushOpen: Brush? = null,
    val headerFg: Color = Color(0xFF2A2A2A),
    val headerFgOpen: Color = Color.White,
    val headerOutline: Color? = null,
    val headerOutlineOpen: Color? = null,
    val headerCorner: Dp = 12.dp,
    val headerWeight: FontWeight = FontWeight.SemiBold
)

val MENU_DESIGNS: List<MenuDesign> = listOf(
    // 1 Modern Clean (default)
    MenuDesign(
        name = "Modern Clean",
        container = Color.White,
        corner = 18.dp,
        itemCorner = 10.dp,
        itemMarginH = 6.dp,
        itemMarginV = 1.dp,
        itemPadH = 12.dp
    ),
    // 2 Brand Gradient
    MenuDesign(
        name = "Brand Gradient",
        container = Color.White,
        corner = 14.dp,
        accentBar = true,
        divider = MenuDividerStyle.INSET,
        dividerColor = Color(0xFFF1E3E5),
        headerBg = Color(0xFFFFF1F2),
        headerFg = BrandRedDark,
        headerBrushOpen = Brush.horizontalGradient(listOf(BrandRed, BrandRedDark)),
        headerCorner = 14.dp
    ),
    // 3 Midnight Dark
    MenuDesign(
        name = "Midnight Dark",
        container = Color(0xFF1E1F24),
        corner = 16.dp,
        text = Color(0xFFF2F2F5),
        subText = Color(0xFF9A9CA6),
        accent = Color(0xFFFF6B6B),
        itemCorner = 10.dp,
        itemMarginH = 6.dp,
        itemMarginV = 1.dp,
        itemPadH = 12.dp,
        pressed = Color(0x33FFFFFF),
        alignRowBg = Color(0xFFF2F2F5),
        headerBg = Color(0xFF2A2B31),
        headerFg = Color(0xFFF2F2F5),
        headerBgOpen = Color(0xFF111216),
        headerFgOpen = Color(0xFFFF6B6B),
        headerOutlineOpen = Color(0xFFFF6B6B)
    ),
    // 4 Soft Cards
    MenuDesign(
        name = "Soft Cards",
        container = Color(0xFFF1F2F6),
        corner = 18.dp,
        itemBg = Color.White,
        itemCorner = 12.dp,
        itemMarginH = 8.dp,
        itemMarginV = 2.dp,
        itemPadH = 12.dp,
        itemPadV = 6.dp,
        headerBg = Color.White,
        headerOutline = Color(0xFFE3E4EA),
        headerBgOpen = Color(0xFF2B2D42),
        headerCorner = 16.dp
    ),
    // 5 Outlined
    MenuDesign(
        name = "Outlined",
        container = Color.White,
        corner = 10.dp,
        outline = Color(0xFFDADCE0),
        itemCorner = 8.dp,
        itemOutline = Color(0xFFE8E8EC),
        itemMarginH = 6.dp,
        itemMarginV = 2.dp,
        itemPadH = 12.dp,
        itemPadV = 6.dp,
        fontWeight = FontWeight.Normal,
        headerBg = Color.White,
        headerOutline = Color(0xFFDADCE0),
        headerBgOpen = Color.White,
        headerFgOpen = BrandRed,
        headerOutlineOpen = BrandRed,
        headerCorner = 8.dp
    ),
    // 6 Rose Pills
    MenuDesign(
        name = "Rose Pills",
        container = Color(0xFFFFF4F5),
        corner = 22.dp,
        text = Color(0xFF3A1A1E),
        itemBg = Color.White,
        itemCorner = 50.dp,
        itemMarginH = 8.dp,
        itemMarginV = 2.dp,
        itemPadH = 14.dp,
        itemPadV = 6.dp,
        pressed = Color(0x33E4293B),
        headerBg = Color(0xFFFFF4F5),
        headerFg = BrandRedDark,
        headerOutline = Color(0xFFF7C9CE),
        headerCorner = 50.dp
    ),
    // 7 Accent Lines
    MenuDesign(
        name = "Accent Lines",
        container = Color.White,
        corner = 12.dp,
        accentBar = true,
        divider = MenuDividerStyle.FULL,
        dividerColor = Color(0xFFF0F0F0),
        fontWeight = FontWeight.Normal,
        headerBg = Color(0xFFF7F7F7),
        headerBgOpen = Color(0xFF263238),
        headerCorner = 6.dp
    ),
    // 8 Ocean Glass
    MenuDesign(
        name = "Ocean Glass",
        container = Color(0xFFF5F9FF),
        containerBrush = Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color(0xFFFFFFFF))),
        corner = 18.dp,
        outline = Color(0xFFD6E6FB),
        text = Color(0xFF13294B),
        accent = Color(0xFF1976D2),
        itemCorner = 10.dp,
        itemMarginH = 6.dp,
        itemMarginV = 1.dp,
        itemPadH = 12.dp,
        pressed = Color(0x261976D2),
        headerBg = Color(0xFFEAF3FF),
        headerFg = Color(0xFF13294B),
        headerBrushOpen = Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0))),
        headerCorner = 14.dp
    ),
    // 9 Warm Paper
    MenuDesign(
        name = "Warm Paper",
        container = Color(0xFFFFFBF2),
        corner = 8.dp,
        outline = Color(0xFFEFE3CF),
        text = Color(0xFF3E2723),
        subText = Color(0xFF8D7B6E),
        accent = BrandRedDark,
        divider = MenuDividerStyle.INSET,
        dividerColor = Color(0xFFEFE3CF),
        pressed = Color(0x22B31D2C),
        fontWeight = FontWeight.SemiBold,
        headerBg = Color(0xFFFFF6E5),
        headerFg = Color(0xFF3E2723),
        headerOutline = Color(0xFFEFE3CF),
        headerBgOpen = Color(0xFF3E2723),
        headerFgOpen = Color(0xFFFFF6E5),
        headerCorner = 6.dp
    ),
    // 10 Ruby Night
    MenuDesign(
        name = "Ruby Night",
        container = BrandRedDark,
        containerBrush = Brush.verticalGradient(listOf(BrandRed, BrandRedDark)),
        corner = 20.dp,
        text = Color.White,
        subText = Color(0xFFFFD6DA),
        accent = Color.White,
        itemCorner = 12.dp,
        itemMarginH = 6.dp,
        itemMarginV = 1.dp,
        itemPadH = 12.dp,
        pressed = Color(0x33FFFFFF),
        divider = MenuDividerStyle.INSET,
        dividerColor = Color(0x33FFFFFF),
        alignRowBg = Color(0xFFFFF1F2),
        headerBg = Color(0xFFFFF1F2),
        headerFg = BrandRedDark,
        headerBrushOpen = Brush.horizontalGradient(listOf(BrandRed, BrandRedDark)),
        headerCorner = 20.dp
    )
)

object MenuDesignState {
    private const val PREFS = "pdfcraft_menu_design"
    private const val KEY = "design_index"
    var index by mutableIntStateOf(0)
        private set
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        index = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, 0).coerceIn(0, MENU_DESIGNS.size - 1)
    }

    fun select(context: Context, i: Int) {
        loaded = true
        index = i.coerceIn(0, MENU_DESIGNS.size - 1)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY, index).apply()
    }
}

