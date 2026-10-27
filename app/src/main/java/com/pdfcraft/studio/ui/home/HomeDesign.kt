package com.pdfcraft.studio.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pdfcraft.studio.ui.theme.BrandRed

/**
 * fix37: Home screen design system. Design 0 = the original Home look.
 * Designs 1..30 = HOME_DESIGNS (HomeDesignPresets.kt + HomeDesignPresets2.kt).
 * Every design shows the same 4 options (Create PDF, View PDF, My Projects, Icon Center)
 * plus the logo size slider; only look and arrangement change.
 */
enum class HomeLayout { LIST, GRID2, HERO, ROWS, SHEET, CIRCLES, DOCK, BENTO, ZIGZAG, TIMELINE, CHIPS, ORBIT }

/** Logo + title + tagline: centered column, left aligned column, or one compact row. */
enum class HomeHeader { CENTER, LEFT, BAR }

/** Vertical placement: everything centered, everything at the top, or header top + options bottom. */
enum class HomePlacement { CENTER, TOP, SPREAD }

enum class HomeLogoFrame { CIRCLE, ROUNDED, RING, PLAIN }

enum class HomeIconBadge { CIRCLE, ROUNDED, OUTLINE, PLAIN }

data class HomeDesign(
    val name: String,
    val layout: HomeLayout,
    val header: HomeHeader = HomeHeader.CENTER,
    val placement: HomePlacement = HomePlacement.CENTER,
    // screen background
    val bg: Color = Color.White,
    val bgBrush: Brush? = null,
    val title: Color = Color(0xFF1F1F1F),
    val sub: Color = Color(0xFF8A8A8A),
    val titleSize: Int = 26,
    // logo
    val logoFrame: HomeLogoFrame = HomeLogoFrame.CIRCLE,
    val logoBg: Color = Color.White,
    val logoRing: Color = Color(0x22000000),
    // option cards
    val card: Color = Color.White,
    val cardBrush: Brush? = null,
    val cardFg: Color = Color(0xFF1F1F1F),
    val cardSub: Color = Color(0xFF8A8A8A),
    val corner: Dp = 20.dp,
    val outline: Color? = null,
    val elevation: Dp = 0.dp,
    val shadowColor: Color = Color.Black,
    // icon badge inside a card
    val badge: HomeIconBadge = HomeIconBadge.CIRCLE,
    val iconBg: Color = Color(0x1AE4293B),
    val iconFg: Color = BrandRed,
    /** Optional per option colors (badge background, or card background when colorCards = true). */
    val itemColors: List<Color> = emptyList(),
    val colorCards: Boolean = false,
    // slider, timeline line, hero accents
    val accent: Color = BrandRed,
    // panel behind options for SHEET and DOCK layouts
    val sheet: Color = Color.White,
    // floating design button
    val fab: Color = BrandRed,
    val fabFg: Color = Color.White,
    val spacing: Dp = 14.dp,
    val showSub: Boolean = true
)

val HOME_DESIGNS: List<HomeDesign> by lazy { HOME_DESIGNS_1 + HOME_DESIGNS_2 }

internal fun HomeDesign.cardColorFor(i: Int): Color? =
    if (colorCards && itemColors.isNotEmpty()) itemColors[i % itemColors.size] else null

internal fun HomeDesign.badgeColorFor(i: Int): Color =
    if (!colorCards && itemColors.isNotEmpty()) itemColors[i % itemColors.size] else iconBg

/** fix37: chosen Home design (0 = original) and the floating button position. */
object HomeDesignState {
    private const val PREFS = "pdfcraft_home_design"
    private const val KEY_DESIGN = "design"
    private const val KEY_FAB_X = "fab_x"
    private const val KEY_FAB_Y = "fab_y"
    var design by mutableIntStateOf(0)
        private set
    /** 0..1 of the free width / height, so the button always stays on screen. */
    var fabX by mutableFloatStateOf(1f)
        private set
    var fabY by mutableFloatStateOf(0.62f)
        private set
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = p.getInt(KEY_DESIGN, 0)
        design = if (d in 0..HOME_DESIGNS.size) d else 0
        fabX = p.getFloat(KEY_FAB_X, 1f).coerceIn(0f, 1f)
        fabY = p.getFloat(KEY_FAB_Y, 0.62f).coerceIn(0f, 1f)
    }

    fun active(): HomeDesign? =
        if (design in 1..HOME_DESIGNS.size) HOME_DESIGNS[design - 1] else null

    fun select(context: Context, number: Int) {
        if (number !in 0..HOME_DESIGNS.size) return
        loaded = true
        design = number
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DESIGN, number).apply()
    }

    fun moveFab(x: Float, y: Float) {
        fabX = x.coerceIn(0f, 1f)
        fabY = y.coerceIn(0f, 1f)
    }

    fun saveFab(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_FAB_X, fabX).putFloat(KEY_FAB_Y, fabY).apply()
    }
}
