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

/**
 * fix35/fix36: editor design system. Each MenuDesign is a full editor theme:
 * dropdown lists, top bar, the Page / Image / Text Tools buttons and their row,
 * the pages area and the on-screen page frame. PDF output is never affected.
 * fix40: the window design comes from Favorite Design only (FAVORITE_DESIGNS,
 * default = Favorite 2 = old Menu Design 25 "Slate Blue"). Menu Design now picks
 * one of 30 DropdownDesign looks (MenuDesignPresets2.kt) that restyle ONLY the
 * dropdown lists: Page Tools, Image Tools, Text Tools and the three dots menu.
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

/** fix40: look of the dropdown lists only (Page / Image / Text Tools and the three dots menu). */
data class DropdownDesign(
    val name: String,
    val container: Color,
    val containerBrush: Brush? = null,
    val corner: Dp = 16.dp,
    val outline: Color? = null,
    val outlineWidth: Dp = 1.dp,
    val text: Color = Color(0xFF1F1F1F),
    val subText: Color = Color(0xFF8A8A8A),
    val accent: Color = BrandRed,
    val itemBg: Color = Color.Transparent,
    val itemBrush: Brush? = null,
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
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val alignRowBg: Color = Color.Transparent
)

/** fix40: the dropdown look a window design already has (Menu Design = Default). */
fun MenuDesign.dropdownLook(): DropdownDesign = DropdownDesign(
    name = name,
    container = container,
    containerBrush = containerBrush,
    corner = corner,
    outline = outline,
    text = text,
    subText = subText,
    accent = accent,
    itemBg = itemBg,
    itemCorner = itemCorner,
    itemOutline = itemOutline,
    itemMarginH = itemMarginH,
    itemMarginV = itemMarginV,
    itemPadH = itemPadH,
    itemPadV = itemPadV,
    pressed = pressed,
    accentBar = accentBar,
    divider = divider,
    dividerColor = dividerColor,
    fontSize = fontSize,
    fontWeight = fontWeight,
    alignRowBg = alignRowBg
)

/**
 * fix36: favorites in order 1, 2, 3, ... To add the next favorite, append ONE full
 * MenuDesign entry at the end of this list.
 * fix40: old Favorites 2 (Ocean Glass) and 3 (Warm Paper) removed,
 * Favorite 2 = DEFAULT_EDITOR_DESIGN (old Menu Design 25, MenuDesignPresets.kt).
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
    // Favorite 2 (fix36 Menu Design 25, the default editor design since fix40)
    DEFAULT_EDITOR_DESIGN,
)

/**
 * fix40: two saved choices.
 * favorite = index in FAVORITE_DESIGNS (the whole editor window), default 1 = Favorite 2.
 * dropdown = 0 (Default: the window design's own lists) or 1..30 (DROPDOWN_DESIGNS).
 * New keys, the fix36 kind / index save is moved once: Favorite 1 stays Favorite 1,
 * everything else (removed Menu Designs and Favorites 2, 3) becomes the new default.
 */
object MenuDesignState {
    private const val PREFS = "pdfcraft_editor_design"
    private const val KEY_OLD_KIND = "kind"
    private const val KEY_OLD_INDEX = "index"
    private const val KEY_FAVORITE = "window_fav_v2"
    private const val KEY_DROPDOWN = "dropdown_v1"
    const val DEFAULT_FAVORITE = 1
    var favorite by mutableIntStateOf(DEFAULT_FAVORITE)
        private set
    var dropdown by mutableIntStateOf(0)
        private set
    private var loaded = false

    /** the whole editor window design */
    fun active(): MenuDesign = FAVORITE_DESIGNS.getOrNull(favorite) ?: DEFAULT_EDITOR_DESIGN

    /** the dropdown lists design */
    fun activeDropdown(): DropdownDesign {
        val n = dropdown
        return if (n in 1..DROPDOWN_DESIGNS.size) DROPDOWN_DESIGNS[n - 1] else active().dropdownLook()
    }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var migrated = false
        val fav = try {
            if (p.contains(KEY_FAVORITE)) {
                p.getInt(KEY_FAVORITE, DEFAULT_FAVORITE)
            } else {
                migrated = true
                if (p.getInt(KEY_OLD_KIND, -1) == 1 && p.getInt(KEY_OLD_INDEX, -1) == 0) 0 else DEFAULT_FAVORITE
            }
        } catch (e: ClassCastException) {
            DEFAULT_FAVORITE
        }
        favorite = if (fav in FAVORITE_DESIGNS.indices) fav else DEFAULT_FAVORITE
        val dd = try {
            p.getInt(KEY_DROPDOWN, 0)
        } catch (e: ClassCastException) {
            0
        }
        dropdown = if (dd in 0..DROPDOWN_DESIGNS.size) dd else 0
        if (migrated) p.edit().putInt(KEY_FAVORITE, favorite).apply()
    }

    fun selectFavorite(context: Context, i: Int) {
        if (i !in FAVORITE_DESIGNS.indices) return
        ensureLoaded(context)
        favorite = i
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_FAVORITE, i).apply()
    }

    /** 0 = Default (window design lists), 1..30 = dropdown design number */
    fun selectDropdown(context: Context, n: Int) {
        if (n !in 0..DROPDOWN_DESIGNS.size) return
        ensureLoaded(context)
        dropdown = n
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DROPDOWN, n).apply()
    }
}
