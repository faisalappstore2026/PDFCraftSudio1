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
 * fix35/fix36: editor design system. Each MenuDesign is a full editor theme:
 * dropdown lists, top bar, the Page / Image / Text Tools buttons and their row,
 * the pages area and the on-screen page frame. PDF output is never affected.
 * Menu Design = MENU_DESIGNS (30, in MenuDesignPresets.kt).
 * Favorite Design = FAVORITE_DESIGNS (append a new entry to add Favorite 4, 5, ...).
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
    val headerWeight: FontWeight = FontWeight.SemiBold,
    // fix36: top bar (back / title / undo / redo / three dots)
    val topBarBg: Color = Color.White,
    val topBarBrush: Brush? = null,
    val topBarFg: Color = Color(0xFF1F1F1F),
    val badge: Color = Color(0xFF1976D2),
    // fix36: row behind the Page / Image / Text Tools buttons
    val toolbarBg: Color = Color.White,
    val toolbarBrush: Brush? = null,
    val toolbarDivider: Color = Color(0xFFEEEEEE),
    // fix36: pages area and on-screen page frame (not the PDF)
    val pagesBg: Color = Color(0xFFEDEDED),
    val pagesBrush: Brush? = null,
    val pageBorder: Color = Color(0x33000000),
    val pageShadow: Dp = 2.dp,
    val pageShadowColor: Color = Color.Black,
    val pageLine: Color = Color(0xFF4A4A4A),
    val glow: Color = Color(0xFF00D9FF),
    val glow2: Color = Color(0xFF4285FF),
    val pageLabel: Color = Color(0x99000000)
)

/**
 * fix36: favorites in order 1, 2, 3, ... To add the next favorite, append ONE
 * entry at the end of this list, for example  MENU_DESIGNS[11],  (= Menu Design 12).
 */
val FAVORITE_DESIGNS: List<MenuDesign> = listOf(
    // Favorite 1 (fix35 design 5)
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
        headerCorner = 8.dp,
        // fix36 full editor theme
        topBarBg = Color.White,
        topBarFg = Color(0xFF202124),
        badge = BrandRed,
        toolbarBg = Color.White,
        toolbarDivider = Color(0xFFDADCE0),
        pagesBg = Color(0xFFF1F3F4),
        pageBorder = Color(0xFFDADCE0),
        pageShadow = 1.dp,
        pageLine = Color(0xFFBDC1C6),
        glow = BrandRed,
        glow2 = Color(0xFFFF8A80),
        pageLabel = Color(0xFF5F6368)
    ),
    // Favorite 2 (fix35 design 8)
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
        headerCorner = 14.dp,
        // fix36 full editor theme
        topBarBg = Color(0xFFEAF3FF),
        topBarBrush = Brush.verticalGradient(listOf(Color(0xFFDCEBFF), Color(0xFFF5F9FF))),
        topBarFg = Color(0xFF13294B),
        badge = Color(0xFF1976D2),
        toolbarBg = Color(0xFFF5F9FF),
        toolbarDivider = Color(0xFFD6E6FB),
        pagesBg = Color(0xFFE3EEFB),
        pagesBrush = Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color(0xFFD3E5FA))),
        pageBorder = Color(0xFFB3D1F5),
        pageShadow = 4.dp,
        pageShadowColor = Color(0xFF1976D2),
        pageLine = Color(0xFF90B8E6),
        glow = Color(0xFF42A5F5),
        glow2 = Color(0xFF1565C0),
        pageLabel = Color(0xFF3D5A80)
    ),
    // Favorite 3 (fix35 design 9)
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
        headerCorner = 6.dp,
        // fix36 full editor theme
        topBarBg = Color(0xFFFFF6E5),
        topBarFg = Color(0xFF3E2723),
        badge = BrandRedDark,
        toolbarBg = Color(0xFFFFFBF2),
        toolbarDivider = Color(0xFFEFE3CF),
        pagesBg = Color(0xFFF3E9D8),
        pageBorder = Color(0xFFD9C7A8),
        pageShadow = 3.dp,
        pageShadowColor = Color(0xFF5D4037),
        pageLine = Color(0xFF8D7B6E),
        glow = BrandRedDark,
        glow2 = Color(0xFFD84315),
        pageLabel = Color(0xFF8D7B6E)
    ),
)

/** fix36: one active selection = kind (menu / favorite) + index. Default = Favorite 1. */
object MenuDesignState {
    const val KIND_MENU = 0
    const val KIND_FAVORITE = 1
    // new prefs file: the old fix35 file (pdfcraft_menu_design / design_index) is ignored
    private const val PREFS = "pdfcraft_editor_design"
    private const val KEY_KIND = "kind"
    private const val KEY_INDEX = "index"
    var kind by mutableIntStateOf(KIND_FAVORITE)
        private set
    var index by mutableIntStateOf(0)
        private set
    private var loaded = false

    private fun listFor(k: Int): List<MenuDesign> =
        if (k == KIND_MENU) MENU_DESIGNS else FAVORITE_DESIGNS

    fun active(): MenuDesign {
        val list = listFor(kind)
        return if (index in list.indices) list[index] else FAVORITE_DESIGNS[0]
    }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = p.getInt(KEY_KIND, KIND_FAVORITE)
        val i = p.getInt(KEY_INDEX, 0)
        if ((k == KIND_MENU || k == KIND_FAVORITE) && i in listFor(k).indices) {
            kind = k
            index = i
        } else {
            kind = KIND_FAVORITE
            index = 0
        }
    }

    fun select(context: Context, k: Int, i: Int) {
        if (k != KIND_MENU && k != KIND_FAVORITE) return
        if (i !in listFor(k).indices) return
        loaded = true
        kind = k
        index = i
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_KIND, k).putInt(KEY_INDEX, i).apply()
    }
}
